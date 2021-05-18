package info.nightscout.androidaps.plugins.pump.ypsopump

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import info.nightscout.androidaps.events.EventExtendedBolusChange
import info.nightscout.androidaps.events.EventTempBasalChange
import info.nightscout.androidaps.interfaces.ActivePlugin
import info.nightscout.androidaps.interfaces.CommandQueueProvider
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.defs.PumpUpdateFragmentType
import info.nightscout.androidaps.plugins.pump.common.events.EventPumpFragmentValuesChanged
import info.nightscout.androidaps.plugins.pump.common.events.EventRefreshButtonState
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpStatusChanged
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.queue.events.EventQueueChanged
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.T
import info.nightscout.androidaps.utils.WarnColors
import info.nightscout.androidaps.utils.alertDialogs.OKDialog
import info.nightscout.androidaps.utils.resources.ResourceHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.ypsopump_fragment.*
import javax.inject.Inject

class YpsoPumpFragment : DaggerFragment() {
    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var fabricPrivacy: FabricPrivacy
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var rxBus: RxBusWrapper
    @Inject lateinit var commandQueue: CommandQueueProvider
    @Inject lateinit var activePlugin: ActivePlugin
    @Inject lateinit var warnColors: WarnColors
    @Inject lateinit var pumpUtil: YpsoPumpUtil
    @Inject lateinit var pumpStatus: YpsopumpPumpStatus
    @Inject lateinit var dateUtil: DateUtil

    private var disposable: CompositeDisposable = CompositeDisposable()

    private val loopHandler = Handler()
    private lateinit var refreshLoop: Runnable

    init {
        refreshLoop = Runnable {
            activity?.runOnUiThread { updateGUI(PumpUpdateFragmentType.Full) }
            loopHandler.postDelayed(refreshLoop, T.mins(1).msecs())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.ypsopump_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO fix
        // medtronic_pumpstatus.setBackgroundColor(resourceHelper.gc(R.color.colorInitializingBorder))
        //
        // medtronic_rl_status.text = resourceHelper.gs(RileyLinkServiceState.NotStarted.resourceId)
        //
        // medtronic_pump_status.setTextColor(Color.WHITE)
        // medtronic_pump_status.text = "{fa-bed}"
        //
        // medtronic_history.setOnClickListener {
        //     if (medtronicPumpPlugin.rileyLinkService?.verifyConfiguration() == true) {
        //         startActivity(Intent(context, MedtronicHistoryActivity::class.java))
        //     } else {
        //         displayNotConfiguredDialog()
        //     }
        // }
        //
        // medtronic_refresh.setOnClickListener {
        //     if (medtronicPumpPlugin.rileyLinkService?.verifyConfiguration() != true) {
        //         displayNotConfiguredDialog()
        //     } else {
        //         medtronic_refresh.isEnabled = false
        //         medtronicPumpPlugin.resetStatusState()
        //         commandQueue.readStatus("Clicked refresh", object : Callback() {
        //             override fun run() {
        //                 activity?.runOnUiThread { medtronic_refresh?.isEnabled = true }
        //             }
        //         })
        //     }
        // }

        // medtronic_stats.setOnClickListener {
        //     if (medtronicPumpPlugin.rileyLinkService?.verifyConfiguration() == true) {
        //         startActivity(Intent(context, RileyLinkStatusActivity::class.java))
        //     } else {
        //         displayNotConfiguredDialog()
        //     }
        // }
    }

    @Synchronized
    override fun onResume() {
        // TODO fix
        super.onResume()
        loopHandler.postDelayed(refreshLoop, T.mins(1).msecs())
        disposable += rxBus
            .toObservable(EventRefreshButtonState::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ pump_refresh.isEnabled = it.newState }, { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventPumpStatusChanged::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ updatePumpStatus(it.driverStatus) }, { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventExtendedBolusChange::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ updateGUI(PumpUpdateFragmentType.TreatmentValues) }, { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventTempBasalChange::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ updateGUI(PumpUpdateFragmentType.TreatmentValues) }, { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventPumpFragmentValuesChanged::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ updateGUI(it.updateType) }, { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventQueueChanged::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ updateGUI(PumpUpdateFragmentType.Queue) }, { fabricPrivacy.logException(it) })
        // disposable += rxBus
        //     .toObservable(EventOtherPumpValuesChanged::class.java)  // ?
        //     .observeOn(AndroidSchedulers.mainThread())
        //     .subscribe({ updateGUI(UpdateGui.OtherValues) }, { fabricPrivacy.logException(it) })
        // disposable += rxBus
        //     .toObservable(EventPumpValuesChanged::class.java)
        //     .observeOn(AndroidSchedulers.mainThread())
        //     .subscribe({ updateGUI(UpdateGui.Full) }, { fabricPrivacy.logException(it) })

        updateGUI(PumpUpdateFragmentType.Full)
    }

    @Synchronized
    override fun onPause() {
        super.onPause()
        disposable.clear()
        loopHandler.removeCallbacks(refreshLoop)
    }

    @Synchronized
    private fun setDeviceStatus() {
        // TODO
        // val resourceId = rileyLinkServiceData.rileyLinkServiceState.resourceId
        // val rileyLinkError = medtronicPumpPlugin.rileyLinkService?.error
        // medtronic_rl_status.text =
        //     when {
        //         rileyLinkServiceData.rileyLinkServiceState == RileyLinkServiceState.NotStarted   -> resourceHelper.gs(resourceId)
        //         rileyLinkServiceData.rileyLinkServiceState.isConnecting                          -> "{fa-bluetooth-b spin}   " + resourceHelper.gs(resourceId)
        //         rileyLinkServiceData.rileyLinkServiceState.isError && rileyLinkError == null     -> "{fa-bluetooth-b}   " + resourceHelper.gs(resourceId)
        //         rileyLinkServiceData.rileyLinkServiceState.isError && rileyLinkError != null     -> "{fa-bluetooth-b}   " + resourceHelper.gs(rileyLinkError.getResourceId(RileyLinkTargetDevice.MedtronicPump))
        //         else                                                                             -> "{fa-bluetooth-b}   " + resourceHelper.gs(resourceId)
        //     }
        // medtronic_rl_status.setTextColor(if (rileyLinkError != null) Color.RED else Color.WHITE)
        //
        // medtronic_errors.text =
        //     rileyLinkServiceData.rileyLinkError?.let {
        //         resourceHelper.gs(it.getResourceId(RileyLinkTargetDevice.MedtronicPump))
        //     } ?: "-"
        //
        // when (medtronicPumpStatus.pumpDeviceState) {
        //     null,
        //     PumpDeviceState.Sleeping             -> medtronic_pump_status.text = "{fa-bed}   " // + pumpStatus.pumpDeviceState.name());
        //     PumpDeviceState.NeverContacted,
        //     PumpDeviceState.WakingUp,
        //     PumpDeviceState.PumpUnreachable,
        //     PumpDeviceState.ErrorWhenCommunicating,
        //     PumpDeviceState.TimeoutWhenCommunicating,
        //     PumpDeviceState.InvalidConfiguration -> medtronic_pump_status.text = " " + resourceHelper.gs(medtronicPumpStatus.pumpDeviceState.resourceId)
        //
        //     PumpDeviceState.Active               -> {
        //         val cmd = medtronicUtil.currentCommand
        //         if (cmd == null)
        //             medtronic_pump_status.text = " " + resourceHelper.gs(medtronicPumpStatus.pumpDeviceState.resourceId)
        //         else {
        //             aapsLogger.debug(LTag.PUMP, "Command: " + cmd)
        //             val cmdResourceId = cmd.resourceId
        //             if (cmd == MedtronicCommandType.GetHistoryData) {
        //                 medtronic_pump_status.text = medtronicUtil.frameNumber?.let {
        //                     resourceHelper.gs(cmdResourceId, medtronicUtil.pageNumber, medtronicUtil.frameNumber)
        //                 }
        //                     ?: resourceHelper.gs(R.string.medtronic_cmd_desc_get_history_request, medtronicUtil.pageNumber)
        //             } else {
        //                 medtronic_pump_status.text = " " + (cmdResourceId?.let { resourceHelper.gs(it) }
        //                     ?: cmd.getCommandDescription())
        //             }
        //         }
        //     }
        //
        //     else   -> aapsLogger.warn(LTag.PUMP, "Unknown pump state: " + medtronicPumpStatus.pumpDeviceState)
        // }
        //
        // val status = commandQueue.spannedStatus()
        // if (status.toString() == "") {
        //     medtronic_queue.visibility = View.GONE
        // } else {
        //     medtronic_queue.visibility = View.VISIBLE
        //     medtronic_queue.text = status
        // }
    }

    private fun displayNotConfiguredDialog() {
        context?.let {
            OKDialog.show(it, resourceHelper.gs(R.string.medtronic_warning),
                resourceHelper.gs(R.string.medtronic_error_operation_not_possible_no_configuration), null)
        }
    }

    // GUI functions
    @Synchronized
    fun updateGUI_() {

        // TODO fix
        // if (medtronic_rl_status == null) return
        //
        // setDeviceStatus()
        //
        // // last connection
        // if (medtronicPumpStatus.lastConnection != 0L) {
        //     val minAgo = DateUtil.minAgo(resourceHelper, medtronicPumpStatus.lastConnection)
        //     val min = (System.currentTimeMillis() - medtronicPumpStatus.lastConnection) / 1000 / 60
        //     if (medtronicPumpStatus.lastConnection + 60 * 1000 > System.currentTimeMillis()) {
        //         medtronic_lastconnection.setText(R.string.medtronic_pump_connected_now)
        //         medtronic_lastconnection.setTextColor(Color.WHITE)
        //     } else if (medtronicPumpStatus.lastConnection + 30 * 60 * 1000 < System.currentTimeMillis()) {
        //
        //         if (min < 60) {
        //             medtronic_lastconnection.text = resourceHelper.gs(R.string.minago, min)
        //         } else if (min < 1440) {
        //             val h = (min / 60).toInt()
        //             medtronic_lastconnection.text = (resourceHelper.gq(R.plurals.duration_hours, h, h) + " "
        //                 + resourceHelper.gs(R.string.ago))
        //         } else {
        //             val h = (min / 60).toInt()
        //             val d = h / 24
        //             // h = h - (d * 24);
        //             medtronic_lastconnection.text = (resourceHelper.gq(R.plurals.duration_days, d, d) + " "
        //                 + resourceHelper.gs(R.string.ago))
        //         }
        //         medtronic_lastconnection.setTextColor(Color.RED)
        //     } else {
        //         medtronic_lastconnection.text = minAgo
        //         medtronic_lastconnection.setTextColor(Color.WHITE)
        //     }
        // }
        //
        // // last bolus
        // val bolus = medtronicPumpStatus.lastBolusAmount
        // val bolusTime = medtronicPumpStatus.lastBolusTime
        // if (bolus != null && bolusTime != null) {
        //     val agoMsc = System.currentTimeMillis() - medtronicPumpStatus.lastBolusTime.time
        //     val bolusMinAgo = agoMsc.toDouble() / 60.0 / 1000.0
        //     val unit = resourceHelper.gs(R.string.insulin_unit_shortname)
        //     val ago: String
        //     if (agoMsc < 60 * 1000) {
        //         ago = resourceHelper.gs(R.string.medtronic_pump_connected_now)
        //     } else if (bolusMinAgo < 60) {
        //         ago = DateUtil.minAgo(resourceHelper, medtronicPumpStatus.lastBolusTime.time)
        //     } else {
        //         ago = DateUtil.hourAgo(medtronicPumpStatus.lastBolusTime.time, resourceHelper)
        //     }
        //     medtronic_lastbolus.text = resourceHelper.gs(R.string.mdt_last_bolus, bolus, unit, ago)
        // } else {
        //     medtronic_lastbolus.text = ""
        // }
        //
        // // base basal rate
        // medtronic_basabasalrate.text = ("(" + medtronicPumpStatus.activeProfileName + ")  "
        //     + resourceHelper.gs(R.string.pump_basebasalrate, medtronicPumpPlugin.baseBasalRate))
        //
        // medtronic_tempbasal.text = activePlugin.activeTreatments.getTempBasalFromHistory(System.currentTimeMillis())?.toStringFull()
        //     ?: ""
        //
        // // battery
        // if (medtronicPumpStatus.batteryType == BatteryType.None || medtronicPumpStatus.batteryVoltage == null) {
        //     medtronic_pumpstate_battery.text = "{fa-battery-" + medtronicPumpStatus.batteryRemaining / 25 + "}  "
        // } else {
        //     medtronic_pumpstate_battery.text = "{fa-battery-" + medtronicPumpStatus.batteryRemaining / 25 + "}  " + medtronicPumpStatus.batteryRemaining + "%" + String.format("  (%.2f V)", medtronicPumpStatus.batteryVoltage)
        // }
        // warnColors.setColorInverse(medtronic_pumpstate_battery, medtronicPumpStatus.batteryRemaining.toDouble(), 25.0, 10.0)
        //
        // // reservoir
        // medtronic_reservoir.text = resourceHelper.gs(R.string.reservoirvalue, medtronicPumpStatus.reservoirRemainingUnits, medtronicPumpStatus.reservoirFullUnits)
        // warnColors.setColorInverse(medtronic_reservoir, medtronicPumpStatus.reservoirRemainingUnits, 50.0, 20.0)
        //
        // medtronicPumpPlugin.rileyLinkService?.verifyConfiguration()
        // medtronic_errors.text = medtronicPumpStatus.errorInfo
    }

    @Synchronized
    fun updateGUI(updateType: PumpUpdateFragmentType) {

        // last connection
        if (pumpStatus.lastConnection != 0L) {
            val minAgo = dateUtil.minAgo(resourceHelper, pumpStatus.lastConnection)
            val min = (System.currentTimeMillis() - pumpStatus.lastConnection) / 1000 / 60
            if (pumpStatus.lastConnection + 60 * 1000 > System.currentTimeMillis()) {
                pump_lastconnection.setText(R.string.medtronic_pump_connected_now)
                pump_lastconnection.setTextColor(Color.WHITE)
            } else if (pumpStatus.lastConnection + 30 * 60 * 1000 < System.currentTimeMillis()) {

                if (min < 60) {
                    pump_lastconnection.text = resourceHelper.gs(R.string.minago, min)
                } else if (min < 1440) {
                    val h = (min / 60).toInt()
                    pump_lastconnection.text = (resourceHelper.gq(R.plurals.duration_hours, h, h) + " "
                        + resourceHelper.gs(R.string.ago))
                } else {
                    val h = (min / 60).toInt()
                    val d = h / 24
                    // h = h - (d * 24);
                    pump_lastconnection.text = (resourceHelper.gq(R.plurals.duration_days, d, d) + " "
                        + resourceHelper.gs(R.string.ago))
                }
                pump_lastconnection.setTextColor(Color.RED)
            } else {
                pump_lastconnection.text = minAgo
                pump_lastconnection.setTextColor(Color.WHITE)
            }
        }

        if (updateType == PumpUpdateFragmentType.PumpStatus || updateType == PumpUpdateFragmentType.Full) {
            // Pump Status (Error)

            //if (pumpUtil.driverStatus
            // TODO error handling

            var pumpDriverState: PumpDriverState? = pumpUtil.driverStatus

            updatePumpStatus(pumpDriverState)

        }

        if (updateType == PumpUpdateFragmentType.Queue || updateType == PumpUpdateFragmentType.Full) {
            // Queue
            val status = commandQueue.spannedStatus()
            if (status.toString() == "") {
                pump_queue.visibility = View.GONE
            } else {
                pump_queue.visibility = View.VISIBLE
                pump_queue.text = status
            }
        }

        if (updateType == PumpUpdateFragmentType.TreatmentValues || updateType == PumpUpdateFragmentType.Full) {
            // Last Bolus, TBR (Profile Change)

            // last bolus
            val bolus = pumpStatus.lastBolusAmount
            val bolusTime = pumpStatus.lastBolusTime
            if (bolus != null && bolusTime != null) {
                val agoMsc = System.currentTimeMillis() - pumpStatus.lastBolusTime!!.time
                val bolusMinAgo = agoMsc.toDouble() / 60.0 / 1000.0
                val unit = resourceHelper.gs(R.string.insulin_unit_shortname)
                val ago: String
                if (agoMsc < 60 * 1000) {
                    ago = resourceHelper.gs(R.string.medtronic_pump_connected_now)
                } else if (bolusMinAgo < 60) {
                    ago = dateUtil.minAgo(resourceHelper, pumpStatus.lastBolusTime!!.time)
                } else {
                    ago = dateUtil.hourAgo(pumpStatus.lastBolusTime!!.time, resourceHelper)
                }
                pump_lastbolus.text = resourceHelper.gs(R.string.pump_last_bolus, bolus, unit, ago)
            } else {
                pump_lastbolus.text = ""
            }

            // base basal rate
            pump_basabasalrate.text = ("(" + pumpStatus.activeProfileName + ")  "
                + resourceHelper.gs(R.string.pump_basebasalrate, pumpStatus.baseBasalRate))

            // TBR TODO
            // pump_tempbasal.text = activePlugin.activeTreatments.getTempBasalFromHistory(System.currentTimeMillis())?.toStringFull()
            //     ?: ""
        }

        if (updateType == PumpUpdateFragmentType.Configuration || updateType == PumpUpdateFragmentType.Full) {
            // Firmware, Errors
            if (pumpStatus.ypsopumpFirmware != null) {
                if (pumpStatus.ypsopumpFirmware!!.isClosedLoopPossible) {
                    pump_firmware.text = pumpStatus.ypsopumpFirmware!!.description
                } else {
                    pump_firmware.text = resourceHelper.gs(R.string.pump_firmware_open_loop_only, pumpStatus.ypsopumpFirmware!!.description)
                }
            } else {
                pump_firmware.text = "Unknown"
            }

            pump_errors.text = if (pumpStatus.errorDescription != null) pumpStatus.errorDescription else ""
        }

        if (updateType == PumpUpdateFragmentType.OtherValues || updateType == PumpUpdateFragmentType.Full) {
            // Battery, Reservoir

            // battery
            pump_battery.text = "{fa-battery-" + pumpStatus.batteryRemaining / 25 + "}  " + pumpStatus.batteryRemaining + "%"
            warnColors.setColorInverse(pump_battery, pumpStatus.batteryRemaining.toDouble(), 25.0, 10.0)

            // reservoir
            pump_reservoir.text = resourceHelper.gs(R.string.reservoirvalue, pumpStatus.reservoirRemainingUnits, pumpStatus.reservoirFullUnits)
            warnColors.setColorInverse(pump_reservoir, pumpStatus.reservoirRemainingUnits, 50.0, 20.0)
        }

    }

    private fun updatePumpStatus(pumpDriverState: PumpDriverState?) {
        when (pumpDriverState) {
            null,
            PumpDriverState.Sleeping                   -> pump_status.text = "{fa-bed}   "
            PumpDriverState.Connecting,
            PumpDriverState.Disconnecting              -> pump_status.text = "{fa-bluetooth-b spin}   " + resourceHelper.gs(pumpDriverState.resourceId)
            PumpDriverState.Connected,
            PumpDriverState.Disconnected               -> pump_status.text = "{fa-bluetooth-b}   " + resourceHelper.gs(pumpDriverState.resourceId)

            PumpDriverState.ErrorCommunicatingWithPump -> {
                pump_status.text = "{fa-bed}   " + "Error ???"
                aapsLogger.warn(LTag.PUMP, "Errors are not supported.")
            }

            PumpDriverState.ExecutingCommand           -> {
                var commandType: YpsoPumpCommandType = pumpUtil.currentCommand
                pump_status.text = "{fa-bluetooth-b}   " + resourceHelper.gs(commandType.resourceId)
            }

            else                                       -> {
                pump_status.text = " " + resourceHelper.gs(pumpDriverState.resourceId)
            }
        }
    }

    enum class UpdateGui {
        Status, // Pump Status (Error)
        Queue, // Queue
        TreatmentValues, // Last Bolus, TBR, Profile Change
        Full,
        Configuration,  // Firmware, Errors
        OtherValues // Battery, Reservoir
    }

}
