package app.aaps.pump.tandem.mobi.ui.overview

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import app.aaps.core.interfaces.insulin.ConcentrationHelper
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.pump.PumpInsulin
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.ui.compose.StatusLevel
import androidx.compose.material.icons.Icons
import app.aaps.core.ui.compose.pump.ActionCategory
import app.aaps.core.ui.compose.pump.PumpAction
import app.aaps.core.ui.compose.pump.PumpInfoRow
import app.aaps.core.ui.compose.pump.PumpCommunicationStatus
import app.aaps.core.ui.compose.pump.PumpOverviewUiState
import app.aaps.core.ui.compose.pump.StatusBanner
import app.aaps.core.ui.compose.pump.tickerFlow

import android.content.Context
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.mutableStateOf
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.pump.PumpRate
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.ui.compose.pump.PumpInfoGroup
import app.aaps.core.ui.compose.pump.PumpInfoInterface
import app.aaps.pump.common.defs.BasalProfileStatus
import app.aaps.pump.common.defs.BolusData
import app.aaps.pump.common.defs.PumpDriverMode
import app.aaps.pump.common.defs.PumpDriverState
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.common.defs.TempBasalPair
import app.aaps.pump.common.driver.connector.defs.PumpCommandType
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.data.SemaphoreInfoDto
import app.aaps.pump.tandem.common.data.defs.TandemPumpApiVersion
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.def.TandemCustomCommand
import app.aaps.pump.tandem.common.keys.TandemBooleanPreferenceKey
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import app.aaps.pump.tandem.mobi.TandemMobiPumpPlugin
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

import app.aaps.core.ui.R as Rco
import app.aaps.pump.common.R as Rc
import app.aaps.core.interfaces.R as Rci


sealed class MobiOverviewEvent {
    data object StartData : MobiOverviewEvent()
    data object StartActions : MobiOverviewEvent()
    data object OpenNotification : MobiOverviewEvent()
    data object OpenEvents : MobiOverviewEvent()
    data object OpenHistory : MobiOverviewEvent()
    data object StartPairing: MobiOverviewEvent()
}

@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Stable
@Inject
class MobiOverviewViewModel(
    private val aapsLogger: AAPSLogger,
    private val rh: ResourceHelper,
    private val commandQueue: CommandQueue,
    private val rxBus: RxBus,
    private val dateUtil: DateUtil,
    private val tandemPlugin: TandemMobiPumpPlugin,
    val tandemPumpStatus: TandemPumpStatus,
    private val ch: ConcentrationHelper,
    private val tandemUtil: TandemPumpUtil,
    protected val preferences: Preferences,
    private val context: Context
) : ViewModel() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val communicationStatus = PumpCommunicationStatus(rxBus, commandQueue, rh, scope)
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _events = MutableSharedFlow<MobiOverviewEvent>(extraBufferCapacity = 6)
    val events: SharedFlow<MobiOverviewEvent> = _events

    var displayDriver = true
    var useSharedConnection = true
    var hidePumpPairButton = true


    val buttonsEnabledFlow = MutableStateFlow<Boolean>(true)
    var buttonsEnabled: Boolean
        get() = buttonsEnabledFlow.value
        set(value) {
            buttonsEnabledFlow.value = value
        }

    val pairButtonEnabledFlow = MutableStateFlow<Boolean>(false)
    var pairButtonEnabled: Boolean
        get() = pairButtonEnabledFlow.value
        set(value) {
            if (pairButtonEnabledFlow.value != value) {
                pairButtonEnabledFlow.value = value
            }
        }


    var mapSemaphore = mapOf(
        Pair("NOTIFICATIONS", rh.gs(R.string.pump_data_status_notification)),
        Pair("EVENTS", rh.gs(R.string.pump_data_status_events)),
        Pair("HISTORY", rh.gs(R.string.pump_data_status_history))
    )

    companion object {
        private const val PLACEHOLDER = "-"
    }

    var lastConnectionText = mutableStateOf(PLACEHOLDER)
    var lastConnectionStatus = mutableStateOf(StatusLevel.NORMAL)

    var batteryText = mutableStateOf(PLACEHOLDER)
    var batteryStatus = mutableStateOf(StatusLevel.NORMAL)

    val reservoirText = mutableStateOf(PLACEHOLDER)
    val reservoirLevel = mutableStateOf(StatusLevel.NORMAL)

    protected val rxTrigger = MutableStateFlow(0L)


    val pumpErrorFlow = MutableStateFlow<String?>(null)
    var pumpError: String?
        get() = pumpErrorFlow.value
        set(value) {
            pumpErrorFlow.value = value
        }

    val uiState: StateFlow<PumpOverviewUiState> = combine(
        tandemUtil.currentActivityFlow,
        tandemPumpStatus.pumpRunningStateFlow,
        tandemPlugin.baseBasalRateFlow,
        tandemPumpStatus.lastBolusDataFlow,
        tandemPumpStatus.currentTempBasalFlow,
        tandemPumpStatus.tandemPumpFirmwareFlow,
        tandemPumpStatus.serialNumberFlow,
        tandemPumpStatus.pumpAddressFlow,
        tandemPumpStatus.reservoirRemainingUnitsFlow,
        tandemPumpStatus.batteryRemainingFlow,
        tandemPumpStatus.activeBolusDataFlow,
        tandemPumpStatus.lastConnectionFlow,
        pumpErrorFlow,
        tandemPumpStatus.semaphoreInfoFlow,
        buttonsEnabledFlow,
        pairButtonEnabledFlow,
        communicationStatus.refreshTrigger,
        tickerFlow(60_000L)
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val currentActivity = values[0] as String
        val pumpRunningState = values[1] as PumpRunningState
        val baseBasalRate = values[2] as PumpRate
        val lastBolus = values[3] as BolusData?
        val tempBasal = values[4] as TempBasalPair?
        val pumpFirmware = values[5] as TandemPumpApiVersion
        val pumpSerialNo = values[6] as Long
        val pumpAddress = values[7] as String
        val reservoir = values[8] as Double?
        val batteryPercent = values[9] as Int?
        val activeBolus= values[10] as BolusData?
        val lastConnectionTime = values[11] as Long
        val pumpError = values[12] as String?
        val semaphoreInfo = values[13] as SemaphoreInfoDto
        val buttonsEnabledLocal = values[14] as Boolean
        val pairButtonEnabledLocal = values[15] as Boolean

        buildUiState(
            currentActivity = currentActivity,
            pumpRunningState = pumpRunningState,
            baseBasalRate = baseBasalRate,
            lastBolus = lastBolus,
            tempBasal= tempBasal,
            pumpFirmware = pumpFirmware,
            pumpSerialNo = pumpSerialNo,
            pumpAddress = pumpAddress,
            reservoir = reservoir,
            batteryPercent = batteryPercent,
            activeBolusData = activeBolus,
            lastConnectionTime = lastConnectionTime,
            pumpError = pumpError,
            semaphoreInfo = semaphoreInfo,
            buttonsEnabledLocal = buttonsEnabledLocal,
            pairButtonEnabledLocal = pairButtonEnabledLocal
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), buildInitialState())


    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }


    init {
        displayDriver = preferences.get(TandemBooleanPreferenceKey.DisplayDriverVersion)
        useSharedConnection = preferences.get(TandemBooleanPreferenceKey.UseSharedConnection)
        hidePumpPairButton = preferences.get(TandemBooleanPreferenceKey.HidePumpPairButton)

        preferences.observe(TandemBooleanPreferenceKey.DisplayDriverVersion).drop(1).onEach {
            displayDriver = preferences.get(TandemBooleanPreferenceKey.DisplayDriverVersion)
            aapsLogger.error(LTag.PUMP, "Display Driver Version changed: $displayDriver")
        }.launchIn(ioScope)

        preferences.observe(TandemBooleanPreferenceKey.UseSharedConnection).drop(1).onEach {
            useSharedConnection = preferences.get(TandemBooleanPreferenceKey.UseSharedConnection)
            aapsLogger.error(LTag.PUMP, "Use Shared Connection changed: $useSharedConnection")
            setPairButtonStatus()
        }.launchIn(ioScope)

        preferences.observe(TandemBooleanPreferenceKey.HidePumpPairButton).drop(1).onEach {
            hidePumpPairButton = preferences.get(TandemBooleanPreferenceKey.HidePumpPairButton)
            aapsLogger.error(LTag.PUMP, "Hide Pair Pump Button changed: $hidePumpPairButton")
            setPairButtonStatus()
        }.launchIn(ioScope)

    }

    private fun setPairButtonStatus() {
        if (useSharedConnection) {
            pairButtonEnabled = false
        } else {
            pairButtonEnabled = !hidePumpPairButton
        }
    }


    suspend fun onRefreshClick() {
        tandemPlugin.resetStatusState()
        commandQueue.readStatus(rh.gs(Rc.string.requested_by_user))
    }

    private fun setButtonState(enabled: Boolean) {
        this.buttonsEnabled = enabled
    }

    fun onDataClick() {
        _events.tryEmit(MobiOverviewEvent.StartData)
    }

    fun onActionClick() {
        _events.tryEmit(MobiOverviewEvent.StartActions)
    }

    fun onPairingClick() {
        _events.tryEmit(MobiOverviewEvent.StartPairing)
    }


    private fun buildInitialState(): PumpOverviewUiState {
        return buildUiState(
            currentActivity = tandemUtil.currentActivity,
            pumpRunningState = tandemPumpStatus.pumpRunningState,
            baseBasalRate = tandemPlugin.baseBasalRate,
            lastBolus = tandemPumpStatus.lastBolusData,
            tempBasal = tandemPumpStatus.currentTempBasal,
            pumpFirmware = tandemPumpStatus.tandemPumpFirmware,
            pumpSerialNo = tandemPumpStatus.serialNumber,
            pumpAddress = tandemPumpStatus.pumpAddress,
            reservoir = tandemPumpStatus.reservoirRemainingUnits,
            batteryPercent = tandemPumpStatus.batteryRemaining,
            activeBolusData = tandemPumpStatus.activeBolusData,
            lastConnectionTime = tandemPumpStatus.lastConnection,
            pumpError = pumpError,
            semaphoreInfo = tandemPumpStatus.semaphoreInfo,
            buttonsEnabledLocal = buttonsEnabled,
            pairButtonEnabledLocal = pairButtonEnabled
        )
    }


    private fun buildUiState(
        currentActivity: String,
        pumpRunningState: PumpRunningState,
        baseBasalRate: PumpRate,
        lastBolus: BolusData?,
        tempBasal: TempBasalPair?,
        pumpFirmware: TandemPumpApiVersion,
        pumpSerialNo: Long,
        pumpAddress: String,
        reservoir: Double?,
        batteryPercent: Int?,
        activeBolusData: BolusData?,
        lastConnectionTime: Long,
        pumpError: String?,
        semaphoreInfo: SemaphoreInfoDto,
        buttonsEnabledLocal: Boolean,
        pairButtonEnabledLocal: Boolean
    ): PumpOverviewUiState {

        // Status banner: communication status from shared helper, or pump-specific warning
        val statusBanner = buildStatusBanner(pumpState = pumpRunningState,
                                             statusBanner = communicationStatus.statusBanner())
        val queueStatus = communicationStatus.queueStatus()

        // Last bolus
        val lastBolus = if (lastBolus != null) {
            ch.insulinAmountAgoString(
                PumpInsulin(lastBolus.amountImmediateDelivered!!),
                lastBolus.timestamp
            )
        } else null

        // Active bolus
        val activeBolusText = if (activeBolusData!=null) {
            ch.insulinDeliveryAgoString(
                amount = PumpInsulin(activeBolusData.amountImmediateDelivered!!),
                totalAmount = PumpInsulin(activeBolusData.amountImmediateRequested!!),
                startTime = activeBolusData.timestamp
            )
        } else null


        var pumpRows: ArrayList<PumpInfoInterface> = arrayListOf()

        var infoGroup = PumpInfoGroup()

        val firmwareString = if (tandemPumpStatus.pumpDriverMode == PumpDriverMode.Demo) {
            rh.gs(R.string.pump_firmware_demo)
        } else {
            if (tandemPumpStatus.tandemPumpFirmware.isClosedLoopPossible) {
                tandemPumpStatus.tandemPumpFirmware.description
            } else {
                if (tandemPumpStatus.tandemPumpFirmware==TandemPumpApiVersion.Unknown)
                    tandemPumpStatus.tandemPumpFirmware.description
                else
                    rh.gs(R.string.pump_firmware_open_loop_only, tandemPumpStatus.tandemPumpFirmware.description)
            }
        }


        //  1. Pump Firmware
        infoGroup.list.add(PumpInfoRow(label = rh.gs(app.aaps.pump.tandem.R.string.pump_firmware_label),
                                       value = firmwareString))

        //  2. Serial Nr
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.serial_number),
                                       value = pumpSerialNo.toString()))

        //  3. BT Address
        infoGroup.list.add(PumpInfoRow(label = rh.gs(R.string.pump_address_label),
                                       value = pumpAddress))

        // Driver version
        if (displayDriver) {
            infoGroup.list.add(PumpInfoRow(label = rh.gs(R.string.driver_version),
                                           value = tandemPlugin.version))
        }

        pumpRows.add(infoGroup)

        infoGroup = PumpInfoGroup()

        //  4. BT State
        infoGroup.list.add(PumpInfoRow(label = rh.gs(R.string.pump_bt_state_label),
                                       value = currentActivity))

        pumpRows.add(infoGroup)

        if (pumpRunningState == PumpRunningState.Unknown) {
            return PumpOverviewUiState(
                statusBanner = statusBanner,
                queueStatus = queueStatus,
                infoRows = pumpRows,
                primaryActions = buildPrimaryActions(pumpRunningState = pumpRunningState,
                                                     buttonsEnabledLocal = false,
                                                     statusBanner = communicationStatus.statusBanner()),
                //managementActions = managementActions
            )
        }
        // else {
        //     if (previousState == PumpRunningState.Unknown) {
        //         buttonsEnabled.value = true
        //         previousState = pumpRunningState
        //     }
        // }

        infoGroup = PumpInfoGroup()

        //  7. Battery
        updateBattery(batteryPercent)
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.battery_label),
                                       value = batteryText.value,
                                       level = batteryStatus.value))

        //  8. Reservoir
        updateReservoir(reservoir)
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.reservoir_label),
                                       value = reservoirText.value,
                                       level = reservoirLevel.value))

        //  9. Last connect
        updateLastConnection(lastConnectionTime)
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.last_connection_label),
                                       value = lastConnectionText.value,
                                       level = lastConnectionStatus.value))

        pumpRows.add(infoGroup)

        infoGroup = PumpInfoGroup()

        //  10. Last Bolus
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.last_bolus_label), value = lastBolus ?: PLACEHOLDER))

        // Active bolus
        activeBolusText?.let {
            infoGroup.list.add(PumpInfoRow(label = rh.gs(Rc.string.active_bolus_label), value = it))
        }

        //  11. Base basal rate
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.base_basal_rate_label),
                                       value =  if (tandemPumpStatus.basalProfileStatus!=BasalProfileStatus.NotInitialized && pumpRunningState== PumpRunningState.Running)
                                                    ch.basalRateString(baseBasalRate, true)
                                                else
                                                    PLACEHOLDER))

        val tempBasalValue = if (tempBasal!=null ) {
            ch.basalTbrString(rate = PumpRate(tempBasal.insulinRate),
                              startTime = tempBasal.start!!,
                              durationInMin = tempBasal.durationMinutes,
                              isAbsolute = false)
        } else
            PLACEHOLDER

        //  12. Temp Basal
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.tempbasal_label),
                                       value = tempBasalValue))

        pumpRows.add(infoGroup)

        // 13. Error
        if (pumpError!=null) {
            pumpRows.add(PumpInfoRow(label = rh.gs(R.string.pump_driver_errors),
                                     value = pumpError))
        }

        // 14 semaphore
        pumpRows.add(MobiSemaphorePumpInfoRow(_events, semaphoreInfo, mapSemaphore))

        return PumpOverviewUiState(
            statusBanner = statusBanner,
            queueStatus = queueStatus,
            infoRows = pumpRows,
            primaryActions = buildPrimaryActions(pumpRunningState = pumpRunningState,
                                                 buttonsEnabledLocal = buttonsEnabledLocal,
                                                 statusBanner = communicationStatus.statusBanner()),
            managementActions = buildManagementActions(statusBanner = communicationStatus.statusBanner(), pairButtonEnabledLocal)
        )
    }


    private fun updateLastConnection(lastConnection: Long) {

        lastConnectionStatus.value = StatusLevel.NORMAL

        if (lastConnection == 0L) {
            lastConnectionText.value = PLACEHOLDER
        }

        val min = (System.currentTimeMillis() - lastConnection) / 1000 / 60
        if (lastConnection + 60 * 1000 > System.currentTimeMillis()) {
            lastConnectionText.value = rh.gs(Rci.string.now)
        } else if (lastConnection + 30 * 60 * 1000 < System.currentTimeMillis()) {
            lastConnectionText.value = if (min < 60) {
                rh.gs(Rci.string.minago, min)
            } else if (min < 1440) {
                val h = min / 60.0f
                rh.gs(Rci.string.hoursago, h)
            } else {
                val h = min / 60.0f
                val d = h / 24.0f

                if (d>7)
                    PLACEHOLDER
                else
                    rh.gs(Rci.string.days_ago, d)
            }

            lastConnectionStatus.value = StatusLevel.CRITICAL
        } else {
            lastConnectionText.value = dateUtil.minAgo(rh, lastConnection)
        }
    }


    private fun updateBattery(batteryPercent: Int?) {
        if (batteryPercent==null || batteryPercent==0)
            batteryText.value = PLACEHOLDER
        else
            batteryText.value = "${batteryPercent}%"

        batteryText.value = "${batteryPercent}%"
        batteryStatus.value = when {
            batteryPercent == null -> StatusLevel.NORMAL
            batteryPercent <= 20   -> StatusLevel.CRITICAL
            batteryPercent <= 30   -> StatusLevel.WARNING
            else              -> StatusLevel.NORMAL
        }
    }


    private fun updateReservoir(remaining: Double?) {
        reservoirText.value = if (remaining!=null && remaining >= 0.0) ch.insulinAmountString(PumpInsulin(remaining)) else PLACEHOLDER
        reservoirLevel.value = when {
            remaining == null -> StatusLevel.NORMAL
            remaining <= 20.0 -> StatusLevel.CRITICAL
            remaining <= 50.0 -> StatusLevel.WARNING
            else              -> StatusLevel.NORMAL
        }
    }





    private fun buildPrimaryActions(pumpRunningState: PumpRunningState, buttonsEnabledLocal: Boolean, statusBanner: StatusBanner?): List<PumpAction> {
        if (primaryActionsEnabled==null || primaryActionsEnabled.isEmpty()) {
            primaryActionsEnabled = listOf(
                PumpAction(
                    label = rh.gs(app.aaps.core.ui.R.string.refresh),
                    icon = Icons.Filled.Refresh,
                    category = ActionCategory.PRIMARY,
                    enabled = true,
                    visible = true,
                    onClick = {
                                scope.launch {
                                    onRefreshClick()
                                }
                        }
                ),
                PumpAction(
                    label = rh.gs(R.string.pump_data),
                    icon = Icons.AutoMirrored.Filled.List,
                    category = ActionCategory.PRIMARY,
                    enabled = true,
                    visible = true,
                    onClick = { onDataClick() }
                ),
                PumpAction(
                    label = rh.gs(R.string.pump_actions),
                    icon = Icons.AutoMirrored.Filled.List,
                    category = ActionCategory.PRIMARY,
                    enabled = true,
                    visible = true,
                    onClick = { onActionClick() }
                )
            )
        }

        if (primaryActionsDisabled==null || primaryActionsDisabled.isEmpty()) {
            primaryActionsDisabled = listOf(
                PumpAction(
                    label = rh.gs(app.aaps.core.ui.R.string.refresh),
                    icon = Icons.Filled.Refresh,
                    category = ActionCategory.PRIMARY,
                    enabled = false,
                    visible = true,
                    onClick = {
                        scope.launch {
                            onRefreshClick()
                        }
                    }
                ),
                PumpAction(
                    label = rh.gs(R.string.pump_data),
                    icon = Icons.AutoMirrored.Filled.List,
                    category = ActionCategory.PRIMARY,
                    enabled = false,
                    visible = true,
                    onClick = { onDataClick() }
                ),
                PumpAction(
                    label = rh.gs(R.string.pump_actions),
                    icon = Icons.AutoMirrored.Filled.List,
                    category = ActionCategory.PRIMARY,
                    enabled = false,
                    visible = true,
                    onClick = { onActionClick() }
                )
            )
        }

        // TOOD testing different solutions
        return if (statusBanner==null) {
            if (buttonsEnabledLocal) {
                primaryActionsEnabled
            } else {
                primaryActionsDisabled
            }
        }
        else
            primaryActionsDisabled


        // return when(pumpRunningState) {
        //     PumpRunningState.Unknown   -> primaryActionsDisabled
        //     PumpRunningState.Suspended -> primaryActionsEnabled
        //     PumpRunningState.Running   -> {
        //         if (buttonsEnabledLocal)
        //             primaryActionsEnabled
        //         else
        //             primaryActionsDisabled
        //     }
        // }
    }

    private fun buildManagementActions(statusBanner: StatusBanner?, pairButtonEnabledLocal: Boolean): List<PumpAction> {

        if (managementActionsEnabled==null || managementActionsEnabled.isEmpty()) {
            managementActionsEnabled = listOf(
                PumpAction(
                    label = "Pair Pump",    //rh.gs(R.string.carelevo_overview_pump_discard_btn_label),
                    icon = Icons.Filled.Delete,
                    enabled = true,
                    category = ActionCategory.MANAGEMENT,
                    onClick = { onPairingClick() }
                )
            )
        }

        if (managementActionsDisabled==null || managementActionsDisabled.isEmpty()) {
            managementActionsDisabled = listOf(
                PumpAction(
                    label = "Pair Pump",    //rh.gs(R.string.carelevo_overview_pump_discard_btn_label),
                    icon = Icons.Filled.Delete,
                    enabled = false,
                    category = ActionCategory.MANAGEMENT,
                    onClick = { onPairingClick() }
                )
            )
        }

        return if (pairButtonEnabledLocal) {
            if (statusBanner==null) {
                managementActionsEnabled
            } else {
                managementActionsDisabled
            }
        } else {
            listOf()
        }
    }


    var primaryActionsEnabled = listOf<PumpAction>()
    var primaryActionsDisabled = listOf<PumpAction>()
    var managementActionsEnabled = listOf<PumpAction>()
    var managementActionsDisabled = listOf<PumpAction>()


    private fun buildStatusBanner(pumpState: PumpRunningState, statusBanner: StatusBanner?): StatusBanner? {

        // when display aaps status, with exception on when we are in suspended state
        if (statusBanner!=null && pumpState != PumpRunningState.Suspended) {
            return statusBanner
        }

        return when(pumpState) {
            PumpRunningState.Unknown   -> StatusBanner(
                        text = rh.gs(R.string.pump_state_unknown_in_overview),
                        level = StatusLevel.CRITICAL
                    )
            PumpRunningState.Suspended -> StatusBanner(
                        text = rh.gs(R.string.pump_state_suspended_in_overview),
                        level = StatusLevel.WARNING
                    )
            else                       -> null
        }
    }


}
