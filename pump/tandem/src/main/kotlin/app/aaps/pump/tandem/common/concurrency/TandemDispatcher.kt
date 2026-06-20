package app.aaps.pump.tandem.common.concurrency

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.pump.PumpEnactResult
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.implementation.pump.PumpEnactResultObject
import app.aaps.pump.tandem.common.comm.ui.TandemUICommunication
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnectionManager
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The single chokepoint for pump-talking work. All callers go through one of the four `submit*`
 * helpers; the lambda runs with [PumpDispatcherScope] as receiver, which is the only way to
 * reach the underlying [TandemPumpConnectionManager] / [TandemUICommunication]. Outside the
 * lambda the symbols are unresolvable.
 *
 * Wraps [PumpOpQueue] so the priority / availability / rate-limit / comm-suspend semantics
 * established in Phases A–B are preserved.
 */
@Singleton
class TandemDispatcher @Inject constructor(
    private val pumpOps: PumpOpQueue,
    private val rh: ResourceHelper,
    private val logger: AAPSLogger,
    pumpConnectionManager: TandemPumpConnectionManager,
    tandemUICommunication: TandemUICommunication
) {

    private val scope: PumpDispatcherScope = object : PumpDispatcherScope {
        override val pumpConnectionManager = pumpConnectionManager
        override val tandemUICommunication = tandemUICommunication
    }

    /**
     * The blocking submit* helpers must never be called from the queue thread: runBlocking would
     * wait for an op only that same thread can run → deadlock. Fail fast instead.
     */
    private fun assertNotOnQueueThread(name: String) {
        check(Thread.currentThread().name != PumpOpQueue.THREAD_NAME) {
            "$name called on the ${PumpOpQueue.THREAD_NAME} thread; runBlocking would deadlock the queue. " +
                "Do not call submit* from inside a pump op block."
        }
    }

    /**
     * AAPS-side mutating ops (bolus, TBR, profile). Routes via [Priority.DEFAULT] with
     * `requiresDeliveryEnabled = true`. On [PumpUnavailableException] (delivery gated by the
     * pump being suspended / in cartridge change), produces a descriptive failed
     * [PumpEnactResult] via [unavailable] so AAPS can re-request next cycle.
     */
    internal fun <T : PumpEnactResult> submitMutating(
        name: String,
        maxDuration: Duration = 2.minutes,
        unavailable: (PumpUnavailableException) -> T,
        block: PumpDispatcherScope.() -> T
    ): T = runBlocking {
        assertNotOnQueueThread(name)
        try {
            pumpOps.submit(
                BlockingPumpOp(name, maxDuration, requiresDeliveryEnabled = true) { scope.block() },
                Priority.DEFAULT
            ).await()
        } catch (e: PumpUnavailableException) {
            logger.warn(LTag.PUMP, "$name: ${e.message}")
            unavailable(e)
        }
    }

    /**
     * Reads / config writes that don't require delivery to be enabled. Routes via
     * [Priority.DEFAULT].
     */
    internal fun <T> submitDefault(
        name: String,
        maxDuration: Duration = 30.seconds,
        block: PumpDispatcherScope.() -> T
    ): T = runBlocking {
        assertNotOnQueueThread(name)
        pumpOps.submit(
            BlockingPumpOp(name, maxDuration, requiresDeliveryEnabled = false) { scope.block() },
            Priority.DEFAULT
        ).await()
    }

    /**
     * UI-driven sends — taps, refresh, settings change. Fire-and-forget at
     * [Priority.USER_INITIATED]; jumps ahead of background AAPS work so the user's tap doesn't
     * wait. Response (if any) arrives via the existing listener path.
     */
    internal fun submitUser(
        name: String,
        maxDuration: Duration = 10.seconds,
        block: PumpDispatcherScope.() -> Unit
    ) {
        pumpOps.submit(
            BlockingPumpOp(name, maxDuration, requiresDeliveryEnabled = false) { scope.block() },
            Priority.USER_INITIATED
        )
    }

    /**
     * Emergency overrides — bolus cancel, alarm acknowledgement. Front of the queue and bypass
     * the availability gate (typically running *during* the workflow that disabled delivery).
     */
    internal fun submitCritical(
        name: String,
        maxDuration: Duration = 30.seconds,
        block: PumpDispatcherScope.() -> Unit
    ) {
        runBlocking {
            assertNotOnQueueThread(name)
            try {
                pumpOps.submit(
                    BlockingPumpOp(name, maxDuration, requiresDeliveryEnabled = false) { scope.block() },
                    Priority.CRITICAL
                ).await()
            } catch (t: Throwable) {
                logger.error(LTag.PUMP, "$name failed: ${t.message}", t)
            }
        }
    }

    /**
     * Lowest-priority maintenance work (history log fetches). Fire-and-forget at
     * [Priority.BACKGROUND] — subject to the queue's token-bucket rate limit so a long sync
     * cannot saturate the wire.
     */
    internal fun submitBackground(
        name: String,
        maxDuration: Duration = 10.seconds,
        block: PumpDispatcherScope.() -> Unit
    ) {
        pumpOps.submit(
            BlockingPumpOp(name, maxDuration, requiresDeliveryEnabled = false) { scope.block() },
            Priority.BACKGROUND
        )
    }

    /** Surface for callers (e.g. plugin's isBusy()) to query queue activity. */
    fun isBusy(): Boolean = pumpOps.isBusy()

    @Suppress("unused") private val unused = rh // reserved for future PumpEnactResult message wiring
}
