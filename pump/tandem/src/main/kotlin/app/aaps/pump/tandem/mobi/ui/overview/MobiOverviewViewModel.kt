package app.aaps.pump.tandem.mobi.ui.overview

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.data.model.EB
import app.aaps.core.data.model.TB
import app.aaps.core.data.ue.Action
import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.insulin.ConcentrationHelper
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.logging.UserEntryLogger
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.pump.PumpInsulin
import app.aaps.core.interfaces.pump.PumpRate
import app.aaps.core.interfaces.queue.Callback
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventInitializationChanged
import app.aaps.core.interfaces.rx.events.EventQueueChanged
import app.aaps.core.interfaces.rx.events.EventRefreshButtonState
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.ui.compose.StatusLevel
import app.aaps.core.ui.compose.icons.IcLoopClosed
import app.aaps.core.ui.compose.pump.ActionCategory
import app.aaps.core.ui.compose.pump.PumpAction
import app.aaps.core.ui.compose.pump.PumpCommunicationStatus
import app.aaps.core.ui.compose.pump.PumpInfoGroup
import app.aaps.core.ui.compose.pump.PumpInfoInterface
import app.aaps.core.ui.compose.pump.PumpInfoRow

import app.aaps.core.ui.compose.pump.PumpOverviewUiState
import app.aaps.core.ui.compose.pump.tickerFlow
import app.aaps.pump.common.data.PumpStatus
import app.aaps.pump.common.defs.PumpDriverMode
import app.aaps.pump.common.defs.PumpDriverState
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.common.defs.PumpUpdateFragmentType
import app.aaps.pump.common.driver.connector.defs.PumpCommandType
import app.aaps.pump.common.events.EventPumpDriverStateChanged
import app.aaps.pump.common.events.EventPumpFragmentValuesChanged
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.def.TandemCustomCommand
import app.aaps.pump.tandem.common.keys.TandemBooleanPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import app.aaps.pump.tandem.mobi.TandemMobiPluginVersion
import app.aaps.pump.tandem.mobi.TandemMobiPumpPlugin
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import app.aaps.core.ui.R as Rco
import app.aaps.pump.common.R as Rc
import app.aaps.core.interfaces.R as Rci

sealed class MobiOverviewEvent {
    data object StartData : MobiOverviewEvent()
    data object StartActions : MobiOverviewEvent()
    data object OpenNotification : MobiOverviewEvent()
    data object OpenEvents : MobiOverviewEvent()
    data object OpenHistory : MobiOverviewEvent()
}

@HiltViewModel
@Stable
open class MobiOverviewViewModel @Inject constructor(
    private val aapsLogger: AAPSLogger,
    protected val rh: ResourceHelper,
    rxBus: RxBus,
    aapsSchedulers: AapsSchedulers,
    private val commandQueue: CommandQueue,
    private val dateUtil: DateUtil,
    private val tandemPump: TandemMobiPumpPlugin,
    private val tandemPumpStatus: TandemPumpStatus,
    private val activePlugin: ActivePlugin,
    private val ch: ConcentrationHelper,
    private val persistenceLayer: PersistenceLayer,
    private val tandemUtil: TandemPumpUtil,
    protected val preferences: Preferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val disposable = CompositeDisposable()

    private val _events = MutableSharedFlow<MobiOverviewEvent>(extraBufferCapacity = 5)
    val events: SharedFlow<MobiOverviewEvent> = _events

    private val communicationStatus = PumpCommunicationStatus(rxBus, commandQueue, context, viewModelScope)

    // RxBus events converted to a flow trigger for recomposition
    protected val rxTrigger = MutableStateFlow(0L)

    companion object {
        private const val PLACEHOLDER = "-"
    }

    var displayDriver = true
    var guiInitialized = false

    var currentActivity = mutableStateOf<String>("")
    var buttonsEnabled = mutableStateOf<Boolean>(true)
    var pumpRunningState = mutableStateOf(rh.gs(Rc.string.pump_status_running))
    var pumpRunningStateLevel = mutableStateOf(StatusLevel.NORMAL)
    var pumpErrors = mutableStateOf("")
    var baseBasalRate = mutableStateOf(PLACEHOLDER)
    var pumpTempBasal = mutableStateOf(PLACEHOLDER)
    var lastBolusValue = mutableStateOf(PLACEHOLDER)
    var semaphoreTexts = mutableStateOf("")

    var pumpFirmware = mutableStateOf(PLACEHOLDER)
    var pumpSerialNo = mutableStateOf(PLACEHOLDER)
    var pumpAddress = mutableStateOf(PLACEHOLDER)

    var lastConnectionText = mutableStateOf(PLACEHOLDER)
    var lastConnectionStatus = mutableStateOf(StatusLevel.NORMAL)

    var batteryText = mutableStateOf(PLACEHOLDER)
    var batteryStatus = mutableStateOf(StatusLevel.NORMAL)

    val reservoirText = mutableStateOf(PLACEHOLDER)
    val reservoirLevel = mutableStateOf(StatusLevel.NORMAL)

    var mapSemaphore = mapOf(
        Pair("NOTIFICATIONS", rh.gs(R.string.pump_data_status_notification)),
        Pair("EVENTS", rh.gs(R.string.pump_data_status_events)),
        Pair("HISTORY", rh.gs(R.string.pump_data_status_history))
    )

    //val iconBack = Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Rco.string.back))

    init {

        // TODO refresh driver display

        displayDriver = preferences.get(TandemBooleanPreferenceKey.DisplayDriverVersion)

        disposable += rxBus
            .toObservable(EventRefreshButtonState::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({
                            buttonsEnabled.value = it.newState
                            rxTrigger.value = System.currentTimeMillis()
                       },
                       { aapsLogger.error(LTag.PUMP, "Error: ${it.message}", it) })
        disposable += rxBus
            .toObservable(EventPumpDriverStateChanged::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({
                            updateCurrentActivity(it.driverStatus)
                            rxTrigger.value = System.currentTimeMillis()
                       },
                       { aapsLogger.error(LTag.PUMP, "Error: ${it.message}", it) })
        disposable += rxBus
            .toObservable(EventPumpFragmentValuesChanged::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({
                            updateGUI(it.updateType)
                            rxTrigger.value = System.currentTimeMillis()
                       },
                       { aapsLogger.error(LTag.PUMP, "Error: ${it.message}", it) })


        updateCurrentActivity(tandemUtil.driverStatus)


        // disposable += rxBus
        //     .toObservable(EventDanaRNewStatus::class.java)
        //     .observeOn(aapsSchedulers.io)
        //     .subscribe({ rxTrigger.value = System.currentTimeMillis() }, { aapsLogger.error(LTag.PUMP, "Error", it) })
        // disposable += rxBus
        //     .toObservable(EventInitializationChanged::class.java)
        //     .observeOn(aapsSchedulers.io)
        //     .subscribe({ rxTrigger.value = System.currentTimeMillis() }, { aapsLogger.error(LTag.PUMP, "Error", it) })
        //
        // // Observe EB/TB database changes for immediate UI updates
        // persistenceLayer.observeChanges(EB::class.java)
        //     .onEach { rxTrigger.value = System.currentTimeMillis() }
        //     .launchIn(viewModelScope)
        // persistenceLayer.observeChanges(TB::class.java)
        //     .onEach { rxTrigger.value = System.currentTimeMillis() }
        //     .launchIn(viewModelScope)
    }



    private fun updateGUI(updateType: PumpUpdateFragmentType) {

        // TODO remove
        //updateCurrentActivity(tandemUtil.driverStatus)

        // TODO need to reimplement the old logic

        //val pumpState = pumpSync.expectedPumpState()

        // NOTE: Custom_2 is used for updating 3 indicators;
        //       Custom_1 is used for updating Last Pump Event (used for debug purposes only, labels are disabled programaticaly)

        //currentTextColor = binding.pumpBaseBasalRate.currentTextColor // we need color from item, in case we are not running in dark mode

        // last connection
        updateLastConnection()
        // if (pumpStatus.lastConnection != 0L) {
        //
        //     val min = (System.currentTimeMillis() - pumpStatus.lastConnection) / 1000 / 60
        //     if (pumpStatus.lastConnection + 60 * 1000 > System.currentTimeMillis()) {
        //         binding.pumpLastConnection.setText(app.aaps.core.interfaces.R.string.now)
        //         binding.pumpLastConnection.setTextColor(currentTextColor)
        //     } else if (pumpStatus.lastConnection + 30 * 60 * 1000 < System.currentTimeMillis()) {
        //
        //         if (min < 60) {
        //             binding.pumpLastConnection.text = rh.gs(app.aaps.core.interfaces.R.string.minago, min)
        //         } else if (min < 1440) {
        //             val h = min / 60.0f
        //             binding.pumpLastConnection.text = rh.gs(app.aaps.core.interfaces.R.string.hoursago, h)
        //         } else {
        //             val h = min / 60.0f
        //             val d = h / 24.0f
        //             // h = h - (d * 24);
        //             binding.pumpLastConnection.text = rh.gs(app.aaps.core.interfaces.R.string.days_ago, d)
        //         }
        //         binding.pumpLastConnection.setTextColor(Color.RED)
        //     } else {
        //         val minAgo = dateUtil.minAgo(resourceHelper, pumpStatus.lastConnection)
        //         binding.pumpLastConnection.text = minAgo
        //         binding.pumpLastConnection.setTextColor(currentTextColor)
        //     }
        // }


        if (updateType == PumpUpdateFragmentType.PumpStatus || updateType == PumpUpdateFragmentType.Full) {
            // Pump Status (Error)
            val pumpDriverState: PumpDriverState = tandemUtil.driverStatus

            updateCurrentActivity(pumpDriverState)
        }


        if (updateType == PumpUpdateFragmentType.Bolus ||
            updateType == PumpUpdateFragmentType.TreatmentValues ||
            updateType == PumpUpdateFragmentType.Full) {

            // last bolus
            updateLastBolus()
        }

        if (updateType == PumpUpdateFragmentType.TBR ||
            updateType == PumpUpdateFragmentType.TreatmentValues ||
            updateType == PumpUpdateFragmentType.Full) {

            // base basal rate
            baseBasalRate.value = ch.basalRateString(tandemPump.baseBasalRate, true)

            // tbr (always saved on pumpStatus)
            updateTBR()
        }

        if (updateType == PumpUpdateFragmentType.Configuration ||
            updateType == PumpUpdateFragmentType.Full) {
            // Firmware, Errors

            if (tandemPumpStatus.pumpDriverMode == PumpDriverMode.Demo) {
                pumpFirmware.value = rh.gs(R.string.pump_firmware_demo)
            } else {
                if (tandemPumpStatus.tandemPumpFirmware.isClosedLoopPossible) {
                    pumpFirmware.value = tandemPumpStatus.tandemPumpFirmware.description
                } else {
                    pumpFirmware.value = rh.gs(R.string.pump_firmware_open_loop_only, tandemPumpStatus.tandemPumpFirmware.description)
                }
            }

            pumpSerialNo.value = tandemUtil.getStringPreferenceOrDefault(TandemStringPreferenceKey.PumpSerial, PLACEHOLDER)
            pumpAddress.value = tandemUtil.getStringPreferenceOrDefault(TandemStringPreferenceKey.PumpAddress, PLACEHOLDER)

        }

        if (updateType == PumpUpdateFragmentType.Battery ||
            updateType == PumpUpdateFragmentType.OtherValues ||
            updateType == PumpUpdateFragmentType.Full) {
            updateBattery()
        }

        if (updateType == PumpUpdateFragmentType.Reservoir ||
            updateType == PumpUpdateFragmentType.OtherValues ||
            updateType == PumpUpdateFragmentType.Full) {
            updateReservoir()
        }


        if (updateType == PumpUpdateFragmentType.Custom_2 ||
            updateType == PumpUpdateFragmentType.Full) {
            updateDataSemaphore()
        }

        updatePumpErrors()
        //setVisibilityOfDriverVersion()

        guiInitialized = true

    }


    private fun updateDataSemaphore() {
        val sb = StringBuilder()

        if (tandemPumpStatus.semaphoreNotifications) {
            sb.append("   ")
            sb.append(mapSemaphore.get("NOTIFICATIONS"))
        }

        if (tandemPumpStatus.semaphoreEvents) {
            sb.append("   ")
            sb.append(mapSemaphore.get("EVENTS"))
        }

        if (tandemPumpStatus.semaphoreHistory) {
            sb.append("   ")
            sb.append(mapSemaphore.get("HISTORY"))
        }

        semaphoreTexts.value = sb.toString().trim()

        // updateLabelColor(binding.pumpDataStatusNotification, pumpStatus.semaphoreNotifications, Color.RED)
        // updateLabelColor(binding.pumpDataStatusEvents, pumpStatus.semaphoreEvents, Color.GREEN)
        // updateLabelColor(binding.pumpDataStatusHistory, pumpStatus.semaphoreHistory, Color.BLUE)
    }


    private fun updatePumpErrors() {
        if (tandemPumpStatus.errorDescription != null) {
            pumpErrors.value = tandemPumpStatus.errorDescription!!
        } else {
            if (tandemPumpStatus.disconnectData!=null && TandemMobiPluginVersion.connectionFixerEnabled) {
                // REC
                pumpErrors.value = "Lost Connection to Pump."
            } else {
                pumpErrors.value = ""
            }
        }
    }




    // private val pumpDataFlow = combine(
    //     tandemPumpStatus.lastConnectionFlow,
    //     tandemPumpStatus.reservoirRemainingUnitsFlow,
    //     tandemPumpStatus.batteryRemainingFlow,
    //     tandemPumpStatus.lastBolusTimeFlow,
    //     tandemPumpStatus.lastBolusAmountFlow,
    //     tandemPumpStatus.pumpRunningStateFlow
    // ) { lastConnection, reservoir, battery, lastBolusTime, lastBolusAmount, pumpRunningState ->
    //     PumpSnapshot(lastConnectionTime = lastConnection,
    //                  reservoir = reservoir,
    //                  battery = battery,
    //                  lastBolusTime = lastBolusTime,
    //                  lastBolusAmount = lastBolusAmount,
    //                  pumpRunningState = pumpRunningState)
    // }


    private val refreshFlow = combine(
        rxTrigger,
        communicationStatus.refreshTrigger,
        tickerFlow(60_000L)
    ) { _, _, _ -> }


    val uiState: StateFlow<PumpOverviewUiState> = combine(
        communicationStatus.refreshTrigger,
        tickerFlow(60_000L)
    ) { _, _ -> buildUiState() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = buildUiState()
    )


    private fun buildUiState(): PumpOverviewUiState {
        return PumpOverviewUiState(
            statusBanner = communicationStatus.statusBanner(),
            infoRows = buildInfoRows(),
            primaryActions = buildPrimaryActions(),
            //managementActions = buildManagementActions(),
            queueStatus = communicationStatus.queueStatus()
        )
    }


    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }


    fun onRefreshClick() {
        aapsLogger.debug(LTag.PUMP, "Clicked connect to pump")

        setButtonState(false)
        tandemPump.resetStatusState()
        commandQueue.readStatus("Clicked refresh", object : Callback() {
            override fun run() {
                setButtonState(true)
            }
        })
    }

    private fun setButtonState(enabled: Boolean) {
        this.buttonsEnabled.value = enabled
    }

    fun onDataClick() {
        _events.tryEmit(MobiOverviewEvent.StartData)
    }

    fun onActionClick() {
        _events.tryEmit(MobiOverviewEvent.StartActions)
    }

    fun onOpenEventsClick() {
        _events.tryEmit(MobiOverviewEvent.OpenEvents)
    }

    fun onOpenHistoryClick() {
        _events.tryEmit(MobiOverviewEvent.OpenHistory)
    }

    fun onOpenNotificationClick() {
        _events.tryEmit(MobiOverviewEvent.OpenNotification)
    }


    private fun buildPrimaryActions(): List<PumpAction> {
        if (primaryActions==null || primaryActions.isEmpty()) {
            primaryActions = listOf(
                PumpAction(
                    label = rh.gs(app.aaps.core.ui.R.string.refresh),
                    //iconRes = app.aaps.core.ui.R.drawable.ic_refresh,
                    icon = IcLoopClosed, // TODO dev4
                    category = ActionCategory.PRIMARY,
                    visible = buttonsEnabled.value,
                    onClick = { onRefreshClick() }
                ),
                PumpAction(
                    label = rh.gs(R.string.pump_data),
                    icon = Icons.AutoMirrored.Filled.List,
                    category = ActionCategory.PRIMARY,
                    visible = buttonsEnabled.value,
                    onClick = { onDataClick() }
                ),
                PumpAction(
                    label = rh.gs(R.string.pump_actions),
                    icon = Icons.AutoMirrored.Filled.List,
                    category = ActionCategory.PRIMARY,
                    visible = buttonsEnabled.value,
                    onClick = { onActionClick() }
                )
            )

        }

        return primaryActions;
    }


    var primaryActions = listOf<PumpAction>()



    @Synchronized
    private fun updateCurrentActivity(pumpDriverState: PumpDriverState?) {
        val resActivity = Rc.string.pump_current_activity

        //aapsLogger.info(LTag.PUMP, "DUB Update Current activity: ${pumpDriverState!!.name}")

        when (pumpDriverState) {
            //null,
            PumpDriverState.Ready,
            PumpDriverState.Sleeping                   -> {
                currentActivity.value = rh.gs(pumpDriverState.resourceId)
                // icon {fa-bed}
            }
            PumpDriverState.Connecting,
            PumpDriverState.Handshaking,
            PumpDriverState.Disconnecting              ->  {
                currentActivity.value = rh.gs(pumpDriverState.resourceId)
                // {fa-bluetooth spin}
            }
            PumpDriverState.Connected,
            PumpDriverState.Disconnected               -> {
                currentActivity.value = rh.gs(pumpDriverState.resourceId)
                // {fa-bluetooth}
            }

            PumpDriverState.ErrorCommunicatingWithPump -> {
                currentActivity.value = "Error ???"
                // fa-bed
                val errorType = tandemUtil.errorType

                pumpErrors.value = if (errorType != null) errorType.name else ""
                //aapsLogger.warn(LTag.PUMP, "Errors are not supported.")
            }

            PumpDriverState.ExecutingCommand           -> {
                val commandType: PumpCommandType? = tandemUtil.currentCommand
                val customCommandTypeInterface : TandemCustomCommand? = tandemUtil.customCommandType as TandemCustomCommand?
                // {fa-bluetooth}
                if (commandType == null) {
                    currentActivity.value = rh.gs(pumpDriverState.resourceId)
                } else {
                    if (commandType == PumpCommandType.CustomCommand) {
                        if (customCommandTypeInterface==null) {
                            currentActivity.value = rh.gs(commandType.resourceId)
                        } else {
                            currentActivity.value = customCommandTypeInterface.getDescription()
                        }
                    } else {
                        if (commandType == PumpCommandType.GetHistoryWithParameters) {
                            val progress: String = tandemUtil.historyProgress.orEmpty()
                            currentActivity.value = rh.gs(commandType.resourceId, progress)
                        } else {
                            currentActivity.value = rh.gs(commandType.resourceId)
                        }
                    }
                }
            }

            else                                       -> {
                currentActivity.value = rh.gs(pumpDriverState!!.resourceId)
            }
        }

        updatePumpStatus()
    }

    private fun updatePumpStatus() {
        pumpRunningState.value = when(tandemPumpStatus.pumpRunningState) {
            PumpRunningState.Unknown   -> rh.gs(Rc.string.pump_status_unknown)
            PumpRunningState.Running   -> rh.gs(Rc.string.pump_status_running)
            PumpRunningState.Suspended -> rh.gs(Rc.string.pump_status_suspended)
        }

        pumpRunningStateLevel.value = if (tandemPumpStatus.pumpRunningState == PumpRunningState.Suspended)
            StatusLevel.CRITICAL else StatusLevel.NORMAL
    }



    // val primaryActions = listOf(
    //     PumpAction(
    //         label = rh.gs(app.aaps.core.ui.R.string.refresh),
    //         iconRes = app.aaps.core.ui.R.drawable.ic_refresh,
    //         category = ActionCategory.PRIMARY,
    //         visible = true, // TODO
    //         onClick = { onRefreshClick() }
    //     ),
    //     PumpAction(
    //         label = rh.gs(R.string.pump_data),
    //         icon = Icons.AutoMirrored.Filled.List,
    //         category = ActionCategory.PRIMARY,
    //         visible = true, // TODO
    //         onClick = { onDataClick() }
    //     ),
    //     PumpAction(
    //         label = rh.gs(R.string.pump_actions),
    //         icon = Icons.AutoMirrored.Filled.List,
    //         category = ActionCategory.PRIMARY,
    //         visible = true, // TODO
    //         onClick = { onActionClick() }
    //     )
    // )

    // Battery warn level





    /**
     * Override in subclasses for variant-specific unpair logic (e.g., BLE bond removal for DanaRS).
     * Default: clears stored device name, resets pump state, logs user entry.
     */
    // open fun performUnpair() {
    //     uel.log(Action.CLEAR_PAIRING_KEYS, Sources.Dana)
    //     preferences.remove(DanaStringNonKey.RName)
    //     danaPump.reset()
    //     rxTrigger.value = System.currentTimeMillis()
    // }

    /**
     * Override in subclasses to add variant-specific management actions (e.g., BLE pair/unpair for DanaRS).
     */
    // protected open fun buildManagementActions(pump: TandemPumpStatus, isInitialized: Boolean, isConfigured: Boolean): List<PumpAction> = buildList {
    //     // User Settings (only for non-Korean, non-legacy-DanaR firmware)
    //     // if (pump.hwModel != 1 && pump.protocol != 0x00) {
    //     //     add(
    //     //         PumpAction(
    //     //             label = rh.gs(R.string.danar_user_options),
    //     //             icon = Icons.Filled.Settings,
    //     //             category = ActionCategory.MANAGEMENT,
    //     //             visible = isInitialized,
    //     //             onClick = { onUserSettingsClick() }
    //     //         )
    //     //     )
    //     // }
    //
    //     add(
    //         PumpAction(
    //             label = rh.gs(Rco.string.refresh),
    //             icon = Icons.Filled.Bluetooth,
    //             category = ActionCategory.MANAGEMENT,
    //             onClick = { onRefreshClick() }
    //         )
    //     )
    //
    //     add(
    //         PumpAction(
    //             label = rh.gs(Rco.string.refresh),
    //             icon = Icons.Filled.Bluetooth,
    //             category = ActionCategory.MANAGEMENT,
    //             onClick = { onRefreshClick() }
    //         )
    //     )
    //
    //     add(
    //         PumpAction(
    //             label = rh.gs(Rco.string.refresh),
    //             icon = Icons.Filled.Bluetooth,
    //             category = ActionCategory.MANAGEMENT,
    //             onClick = { onRefreshClick() }
    //         )
    //     )
    //
    //
    //     // Pair / Unpair
    //     // if (isConfigured) {
    //     //     add(
    //     //         PumpAction(
    //     //             label = rh.gs(app.aaps.core.ui.R.string.pump_unpair),
    //     //             icon = Icons.Filled.Bluetooth,
    //     //             category = ActionCategory.MANAGEMENT,
    //     //             onClick = { onUnpairClick() }
    //     //         )
    //     //     )
    //     // } else {
    //     //     add(
    //     //         PumpAction(
    //     //             label = rh.gs(app.aaps.core.ui.R.string.pairing),
    //     //             icon = Icons.Filled.Bluetooth,
    //     //             category = ActionCategory.MANAGEMENT,
    //     //             onClick = { onPairClick() }
    //     //         )
    //     //     )
    //     // }
    // }


    private fun buildInfoRows(): List<PumpInfoInterface> = buildList {

        // TODO split data creation (in updateGUI)

        var infoGroup = PumpInfoGroup()


        //  1. Pump Firmware
        infoGroup.list.add(PumpInfoRow(label = rh.gs(R.string.pump_firmware_label), value = pumpFirmware.value))

        //  2. Serial Nr
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.serial_number), value = pumpSerialNo.value))

        //  3. BT Address
        infoGroup.list.add(PumpInfoRow(label = rh.gs(R.string.pump_address_label), value = pumpAddress.value))

        // Driver version
        if (displayDriver) {
            infoGroup.list.add(PumpInfoRow(label = rh.gs(R.string.driver_version), value = tandemPump.version))
        }

        add(infoGroup)

        infoGroup = PumpInfoGroup()

        //  4. BT State
        infoGroup.list.add(PumpInfoRow(label = rh.gs(R.string.pump_bt_state_label), value = currentActivity.value))

        // X 5. Queue

        //  6. Pump Status
        infoGroup.list.add(PumpInfoRow(label = rh.gs(R.string.pump_status_label), value = pumpRunningState.value, level = pumpRunningStateLevel.value))

        add(infoGroup)

        infoGroup = PumpInfoGroup()

        //  7. Battery
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.battery_label), value = batteryText.value, level = batteryStatus.value))

        //  8. Reservoir
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.reservoir_label), value = reservoirText.value, level = reservoirLevel.value))

        //  9. Last connect
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.last_connection_label), value = lastConnectionText.value,
                                       level = lastConnectionStatus.value))

        add(infoGroup)

        infoGroup = PumpInfoGroup()

        //  10. Last Bolus
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.last_bolus_label), value = lastBolusValue.value))

        //  11. Base basal rate
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.base_basal_rate_label), value = baseBasalRate.value))

        //  12. Temp Basal
        infoGroup.list.add(PumpInfoRow(label = rh.gs(Rco.string.tempbasal_label), value = pumpTempBasal.value))

        add(infoGroup)

        // 13. Error

        if (!pumpErrors.value.isEmpty()) {
            add(PumpInfoRow(label = rh.gs(R.string.pump_driver_errors), value = pumpErrors.value))
        }

        updateDataSemaphore()
        add(PumpInfoRow(label = "     ", value = semaphoreTexts.value))

        // TODO   Notification   Events    History

        // TODO   Pictures

    }


    private fun updateReservoir() {
        val remaining = PumpInsulin(tandemPumpStatus.reservoirRemainingUnits)
        //val full = tandemPumpStatus.reservoirFullUnits
        reservoirText.value = ch.insulinAmountString(remaining)
        reservoirLevel.value = when {
            ch.fromPump(remaining) <= 20.0 -> StatusLevel.CRITICAL
            ch.fromPump(remaining) <= 50.0 -> StatusLevel.WARNING
            else              -> StatusLevel.NORMAL
        }
    }


    private fun updateLastConnection() {
        val lastConnection = tandemPumpStatus.lastConnection

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
                rh.gs(Rci.string.days_ago, d)
            }

            lastConnectionStatus.value = StatusLevel.CRITICAL
        } else {
            lastConnectionText.value = dateUtil.minAgo(rh, lastConnection)
        }
    }


    private fun updateBattery() {
        val remaining = tandemPumpStatus.batteryRemaining
        batteryText.value = "${remaining}%"
        batteryStatus.value = when {
            remaining == null -> StatusLevel.NORMAL
            remaining <= 20   -> StatusLevel.CRITICAL
            remaining <= 30   -> StatusLevel.WARNING
            else              -> StatusLevel.NORMAL
        }
    }


    private fun updateLastBolus() {
        val bolus = tandemPumpStatus.tandemLastBolus

        lastBolusValue.value = if (bolus != null) {
            ch.insulinAmountAgoString(PumpInsulin(bolus.amountImmediateDelivered!!), bolus.timestamp) ?:PLACEHOLDER
        } else {
            PLACEHOLDER
        }
    }


    private fun updateTBR() {
        if (tandemPumpStatus.currentTempBasal==null || System.currentTimeMillis() > tandemPumpStatus.currentTempBasalEstimatedEnd!!) {
            tandemPumpStatus.clearTbr()
            pumpTempBasal.value = PLACEHOLDER
        } else {
            val rate = PumpRate(tandemPumpStatus.currentTempBasal!!.insulinRate)
            val startTime = tandemPumpStatus.currentTempBasal!!.start!!
            val durationInMin = tandemPumpStatus.currentTempBasal!!.durationMinutes
            pumpTempBasal.value = ch.basalTbrString(rate, startTime, durationInMin = durationInMin, isAbsolute = false)
        }
    }


}
