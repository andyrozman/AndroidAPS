package app.aaps.pump.tandem.common.concurrency

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps [PumpAvailabilityState] in sync with the pump-side state the AAPS Loop itself maintains.
 *
 * The pump is the source of truth for "can I accept a bolus / TBR / profile right now?". The Loop's
 * own status reads ([TandemPumpConnector.getPumpStatus]) and the connection lifecycle
 * ([TandemPumpCommunicationManager]) update the backend flows [TandemPumpStatus.pumpRunningStateFlow]
 * and [TandemPumpStatus.pumpConnectedFlow]. This component observes those two flows and projects
 * their combined state onto [PumpAvailabilityState].
 *
 * It deliberately reads backend flows, NOT the `tandemDataStore` UI LiveData: the LiveData is only
 * populated when a UI screen is driving the pump, so a headless Loop would never see it move off its
 * initial value and every mutating op would fast-fail. Backend (non-UI) code must never read UI state.
 *
 * Mapping (highest-priority match wins):
 *   - not connected                  → Unknown
 *   - pumpRunningState == Suspended  → DeliveryDisabled (also covers cartridge change, which the
 *                                       pump reports as Suspended)
 *   - pumpRunningState == Running    → DeliveryEnabled
 *   - everything else                → Unknown
 */
@Singleton
class PumpAvailabilitySync @Inject constructor(
    private val availability: PumpAvailabilityState,
    private val pumpStatus: TandemPumpStatus,
    private val logger: AAPSLogger
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch {
            combine(pumpStatus.pumpConnectedFlow, pumpStatus.pumpRunningStateFlow) { connected, running ->
                connected to running
            }.distinctUntilChanged().collect { (connected, running) ->
                recompute(connected, running)
            }
        }
        logger.debug(LTag.PUMP, "PumpAvailabilitySync: started observing backend pump-state flows")
    }

    private fun recompute(connected: Boolean, running: PumpRunningState) {
        when {
            !connected -> availability.markUnknown("not connected")
            running == PumpRunningState.Suspended -> availability.markDisabled("pump suspended")
            running == PumpRunningState.Running -> availability.markEnabled("pump running")
            else -> availability.markUnknown("running state=$running")
        }
    }
}
