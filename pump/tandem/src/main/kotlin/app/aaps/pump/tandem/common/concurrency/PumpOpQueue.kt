package app.aaps.pump.tandem.common.concurrency

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import com.jwoglom.pumpx2.pump.messages.Message
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.ArrayDeque
import java.util.EnumMap
import java.util.concurrent.Executors

/** Thrown into a fast-failing op's Deferred when delivery is gated by [PumpAvailability]. */
class PumpUnavailableException(val availability: PumpAvailability, val opName: String) :
    RuntimeException("Pump op '$opName' rejected: availability=$availability")

/**
 * Single-dispatcher pump op queue with four-tier priority and per-tier rate limiting.
 *
 * Architecture: one dedicated single-thread executor owns BLE I/O. Submission [Priority]
 * determines insertion point — CRITICAL > USER_INITIATED > DEFAULT > BACKGROUND,
 * FIFO within each tier. Status reads with a [PumpOp.coalesceKey] are deduped at submit time.
 *
 * Each tier may have an optional [RateLimit]; by default only [Priority.BACKGROUND] is rate
 * limited (see [defaultRateLimits]) so history-log-style maintenance work cannot saturate the
 * wire. When the head-of-queue tier is rate-gated the dispatcher sleeps until either a token
 * becomes available or a higher-priority op is submitted (which preempts via [wakeup]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PumpOpQueue(
    private val logger: AAPSLogger,
    private val availability: PumpAvailabilityState,
    private val commSuspend: CommSuspendGate,
    private val sender: Sender? = null,
    rateLimits: Map<Priority, RateLimit> = defaultRateLimits()
) {

    companion object {
        /** Name of the single thread all ops run on. Callers must never block on it (deadlock). */
        const val THREAD_NAME = "TandemPumpOpQueue"

        /** Default: BACKGROUND throttled at 1 msg/s, burst 2. Other tiers unrate-limited. */
        fun defaultRateLimits(): Map<Priority, RateLimit> = mapOf(
            Priority.BACKGROUND to RateLimit(rps = 1.0, burst = 2)
        )
    }

    /**
     * Wire sender abstraction. In production this delegates to TandemCommunicationManager.
     * Kept narrow so the queue itself has no BLE knowledge.
     */
    fun interface Sender {
        fun sendOnWire(request: Message, forceSend: Boolean): Message?
    }

    private class Entry<T>(
        val op: PumpOp<T>,
        val priority: Priority,
        val deferred: CompletableDeferred<T>
    )

    private val lock = Any()
    private val queues: EnumMap<Priority, ArrayDeque<Entry<*>>> =
        EnumMap<Priority, ArrayDeque<Entry<*>>>(Priority::class.java).also { map ->
            Priority.values().forEach { map[it] = ArrayDeque() }
        }
    private val limiters: Map<Priority, TokenBucket> = rateLimits.mapValues { TokenBucket(it.value) }
    /** coalesceKey -> Deferred of the pending op, so duplicate submits can share its result. */
    private val pendingByKey = HashMap<String, CompletableDeferred<*>>()
    private var inFlight: Entry<*>? = null

    private val wakeup = Channel<Unit>(capacity = Channel.CONFLATED)

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, THREAD_NAME).apply { isDaemon = true }
    }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    /** Single mutex around the wire so awaitable comm-suspend gating composes naturally. */
    private val wireMutex = Mutex()

    init {
        scope.launch { dispatchLoop() }
    }

    fun isBusy(): Boolean = synchronized(lock) {
        inFlight != null || queues.values.any { it.isNotEmpty() }
    }

    fun <T> submit(op: PumpOp<T>, priority: Priority): Deferred<T> {
        val key = op.coalesceKey
        synchronized(lock) {
            if (key != null) {
                @Suppress("UNCHECKED_CAST")
                val existing = pendingByKey[key] as CompletableDeferred<T>?
                if (existing != null) {
                    logger.debug(LTag.PUMP, "PumpOpQueue: coalescing '${op.name}' onto existing key=$key")
                    return existing
                }
            }
            val deferred = CompletableDeferred<T>()
            val entry = Entry(op, priority, deferred)
            queues[priority]!!.addLast(entry)
            if (key != null) pendingByKey[key] = deferred
            logger.debug(LTag.PUMP, "PumpOpQueue: submitted '${op.name}' priority=$priority ${snapshotSizes()}")
            wakeup.trySend(Unit)
            return deferred
        }
    }

    private fun snapshotSizes(): String =
        queues.entries.joinToString(", ", "(", ")") { "${it.key}=${it.value.size}" }

    private sealed class Pick {
        data class Ready(val entry: Entry<*>) : Pick()
        /** Head-of-queue tier is rate-limited; wait this many ms (or shorter, on wakeup). */
        data class WaitMs(val ms: Long) : Pick()
        /** All queues empty. */
        data object Idle : Pick()
    }

    private suspend fun dispatchLoop() {
        while (true) {
            when (val pick = pickNext()) {
                is Pick.Ready  -> runEntry(pick.entry)
                is Pick.WaitMs -> withTimeoutOrNull(pick.ms) { wakeup.receive() }
                is Pick.Idle   -> wakeup.receive()
            }
        }
    }

    private fun pickNext(): Pick = synchronized(lock) {
        // Whole-queue pause when the pump signals comm-suspended. Applies regardless of priority
        // — even CRITICAL ops can't usefully send while the pump's BT buffer is full.
        val pauseRemaining = commSuspend.remainingPauseMs()
        if (pauseRemaining > 0 && queues.values.any { it.isNotEmpty() }) {
            return Pick.WaitMs(pauseRemaining)
        }
        for (p in Priority.values()) {
            val q = queues[p]!!
            if (q.isEmpty()) continue
            val limiter = limiters[p]
            if (limiter == null) {
                val e = q.pollFirst()
                inFlight = e
                return Pick.Ready(e)
            }
            val waitMs = limiter.tryAcquire()
            if (waitMs == 0L) {
                val e = q.pollFirst()
                inFlight = e
                return Pick.Ready(e)
            }
            // Head priority is rate-gated; do NOT fall through to lower priorities (would invert
            // priority ordering). Wait for either a token or a higher-priority submission.
            return Pick.WaitMs(waitMs)
        }
        return Pick.Idle
    }

    private suspend fun <T> runEntry(entry: Entry<T>) {
        val op = entry.op
        val deferred = entry.deferred
        try {
            if (op.requiresDeliveryEnabled && !availability.current.allowsDelivery) {
                logger.warn(
                    LTag.PUMP,
                    "PumpOpQueue: fast-failing '${op.name}' — availability=${availability.current}"
                )
                deferred.completeExceptionally(PumpUnavailableException(availability.current, op.name))
                return
            }
            val ctx = Ctx(op)
            val result = withTimeout(op.maxDuration) { op.run(ctx) }
            deferred.complete(result)
        } catch (t: TimeoutCancellationException) {
            logger.error(LTag.PUMP, "PumpOpQueue: '${op.name}' timed out after ${op.maxDuration}")
            deferred.completeExceptionally(t)
        } catch (t: Throwable) {
            logger.error(LTag.PUMP, "PumpOpQueue: '${op.name}' threw: ${t.message}", t)
            deferred.completeExceptionally(t)
        } finally {
            synchronized(lock) {
                inFlight = null
                op.coalesceKey?.let { pendingByKey.remove(it) }
            }
        }
    }

    /** Per-op context. Created fresh per op so the [PumpOp.name] is captured for logs. */
    private inner class Ctx(@Suppress("unused") private val op: PumpOp<*>) : PumpOpContext {
        override suspend fun send(request: Message, forceSend: Boolean): Message? {
            val s = sender ?: error("PumpOpQueue.Sender not configured; ctx.send() unavailable")
            return wireMutex.withLock {
                commSuspend.await()
                s.sendOnWire(request, forceSend)
            }
        }
    }

    fun shutdown() {
        scope.cancel()
        executor.shutdown()
    }
}
