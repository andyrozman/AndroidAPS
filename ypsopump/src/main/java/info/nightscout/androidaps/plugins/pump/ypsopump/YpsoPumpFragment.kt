package info.nightscout.androidaps.plugins.pump.ypsopump

//import kotlinx.android.synthetic.main.ypsopump_fragment.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import info.nightscout.androidaps.events.EventExtendedBolusChange
import info.nightscout.androidaps.events.EventTempBasalChange
import info.nightscout.androidaps.interfaces.ActivePlugin
import info.nightscout.androidaps.interfaces.CommandQueue
import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.defs.PumpUpdateFragmentType
import info.nightscout.androidaps.plugins.pump.common.events.EventPumpFragmentValuesChanged
import info.nightscout.androidaps.plugins.pump.common.events.EventRefreshButtonState
import info.nightscout.androidaps.plugins.pump.ypsopump.databinding.YpsopumpFragmentBinding
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType
import info.nightscout.androidaps.plugins.pump.ypsopump.dialog.YpsoPumpHistoryActivity
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpStatusChanged
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.queue.Callback
import info.nightscout.androidaps.queue.events.EventQueueChanged
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.T
import info.nightscout.androidaps.utils.WarnColors
import info.nightscout.androidaps.utils.alertDialogs.OKDialog
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.shared.logging.AAPSLogger
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import javax.inject.Inject

class YpsoPumpFragment : DaggerFragment() {

    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var fabricPrivacy: FabricPrivacy
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var commandQueue: CommandQueue
    @Inject lateinit var activePlugin: ActivePlugin
    @Inject lateinit var warnColors: WarnColors
    @Inject lateinit var pumpUtil: YpsoPumpUtil
    @Inject lateinit var pumpStatus: YpsopumpPumpStatus
    @Inject lateinit var dateUtil: DateUtil
    @Inject lateinit var pumpSync: PumpSync
    @Inject lateinit var ypsopumpPumpPlugin: YpsopumpPumpPlugin

    private var disposable: CompositeDisposable = CompositeDisposable()

    private val loopHandler = Handler(Looper.getMainLooper())
    private lateinit var refreshLoop: Runnable

    private var _binding: YpsopumpFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    init {
        refreshLoop = Runnable {
            activity?.runOnUiThread { updateGUI(PumpUpdateFragmentType.Full) }
            loopHandler.postDelayed(refreshLoop, T.mins(1).msecs())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = YpsopumpFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    //private var _binding: YpsoPumpFragmentBinding? = null

    //private var _bind: YpsoPumpFragment? = null

    // TODO re-add
    // private var _binding: YpsoPumpFragmentBinding? = null
    //
    // // This property is only valid between onCreateView and
    // // onDestroyView.
    // private val binding get() = _binding!!
    //
    // override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    //     MedtronicFragmentBinding.inflate(inflater, container, false).also { _binding = it }.root
    //
    // override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    //     return inflater.inflate(R.layout.ypsopump_fragment, container, false)
    // }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pumpRefresh.setOnClickListener {
            binding.pumpRefresh.isEnabled = false
            ypsopumpPumpPlugin.resetStatusState()
            commandQueue.readStatus("Clicked refresh", object : Callback() {
                override fun run() {
                    activity?.runOnUiThread { binding.pumpRefresh.isEnabled = true }
                }
            })
        }

        binding.pumpHistory.setOnClickListener {
            startActivity(Intent(context, YpsoPumpHistoryActivity::class.java))
        }

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
            .subscribe({ binding.pumpRefresh.isEnabled = it.newState }, { fabricPrivacy.logException(it) })
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

        val pumpState = pumpSync.expectedPumpState()

        // last connection
        if (pumpStatus.lastConnection != 0L) {
            val minAgo = dateUtil.minAgo(resourceHelper, pumpStatus.lastConnection)
            val min = (System.currentTimeMillis() - pumpStatus.lastConnection) / 1000 / 60
            if (pumpStatus.lastConnection + 60 * 1000 > System.currentTimeMillis()) {
                binding.pumpLastConnection.setText(R.string.medtronic_pump_connected_now)
                binding.pumpLastConnection.setTextColor(Color.WHITE)
            } else if (pumpStatus.lastConnection + 30 * 60 * 1000 < System.currentTimeMillis()) {

                if (min < 60) {
                    binding.pumpLastConnection.text = resourceHelper.gs(R.string.minago, min)
                } else if (min < 1440) {
                    val h = (min / 60).toInt()
                    binding.pumpLastConnection.text = (resourceHelper.gq(R.plurals.duration_hours, h, h) + " "
                        + resourceHelper.gs(R.string.ago))
                } else {
                    val h = (min / 60).toInt()
                    val d = h / 24
                    // h = h - (d * 24);
                    binding.pumpLastConnection.text = (resourceHelper.gq(R.plurals.duration_days, d, d) + " "
                        + resourceHelper.gs(R.string.ago))
                }
                binding.pumpLastConnection.setTextColor(Color.RED)
            } else {
                binding.pumpLastConnection.text = minAgo
                binding.pumpLastConnection.setTextColor(Color.WHITE)
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
                binding.pumpQueue.visibility = View.GONE
            } else {
                binding.pumpQueue.visibility = View.VISIBLE
                binding.pumpQueue.text = status
            }
        }

        if (updateType == PumpUpdateFragmentType.TreatmentValues || updateType == PumpUpdateFragmentType.Full) {
            // Last Bolus, TBR (Profile Change)

            val bolusState: PumpSync.PumpState.Bolus? = pumpState.bolus
            
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
                binding.pumpLastBolus.text = resourceHelper.gs(R.string.pump_last_bolus, bolus, unit, ago)
            } else {
                binding.pumpLastBolus.text = ""
            }

            // base basal rate
            binding.pumpBaseBasalRate.text = ("(" + pumpStatus.activeProfileName + ")  "
                + resourceHelper.gs(R.string.pump_basebasalrate, pumpStatus.baseBasalRate))

            //TBR TODO
            // binding.pumpTempBasal.text = activePlugin.activeTreatments.getTempBasalFromHistory(System.currentTimeMillis())?.toStringFull()
            //     ?: ""
        }

        if (updateType == PumpUpdateFragmentType.Configuration || updateType == PumpUpdateFragmentType.Full) {
            // Firmware, Errors
//            if (pumpStatus.ypsopumpFirmware != null) {
            if (pumpStatus.ypsopumpFirmware.isClosedLoopPossible) {
                binding.pumpFirmware.text = pumpStatus.ypsopumpFirmware.description
            } else {
                binding.pumpFirmware.text = resourceHelper.gs(R.string.pump_firmware_open_loop_only, pumpStatus.ypsopumpFirmware.description)
            }
            // }
            // else {
            //     binding.pumpFirmware.text = "Unknown"
            // }

            //pump_errors.text = if (pumpStatus.errorDescription != null) pumpStatus.errorDescription else ""
        }

        if (updateType == PumpUpdateFragmentType.OtherValues || updateType == PumpUpdateFragmentType.Full) {
            // Battery, Reservoir

            // battery
            binding.pumpBattery.text = "{fa-battery-" + pumpStatus.batteryRemaining / 25 + "}  " + pumpStatus.batteryRemaining + "%"
            warnColors.setColorInverse(binding.pumpBattery, pumpStatus.batteryRemaining.toDouble(), 25.0, 10.0)

            // reservoir
            binding.pumpReservoir.text = resourceHelper.gs(R.string.reservoirvalue, pumpStatus.reservoirRemainingUnits, pumpStatus.reservoirFullUnits)
            warnColors.setColorInverse(binding.pumpReservoir, pumpStatus.reservoirRemainingUnits, 50.0, 20.0)
        }

    }

    private fun updatePumpStatus(pumpDriverState: PumpDriverState?) {
        when (pumpDriverState) {
            null,
            PumpDriverState.Sleeping                   -> binding.pumpStatus.text = "{fa-bed}   "
            PumpDriverState.Connecting,
            PumpDriverState.Disconnecting              -> binding.pumpStatus.text = "{fa-bluetooth-b spin}   " + resourceHelper.gs(pumpDriverState.resourceId)
            PumpDriverState.Connected,
            PumpDriverState.Disconnected               -> binding.pumpStatus.text = "{fa-bluetooth-b}   " + resourceHelper.gs(pumpDriverState.resourceId)

            PumpDriverState.ErrorCommunicatingWithPump -> {
                binding.pumpStatus.text = "{fa-bed}   " + "Error ???"
                val errorType = pumpUtil.errorType

                binding.pumpErrors.text = if (errorType != null) errorType.name else ""
                //aapsLogger.warn(LTag.PUMP, "Errors are not supported.")
            }

            PumpDriverState.ExecutingCommand           -> {
                var commandType: YpsoPumpCommandType = pumpUtil.currentCommand
                binding.pumpStatus.text = "{fa-bluetooth-b}   " + resourceHelper.gs(commandType.resourceId)
            }

            else                                       -> {
                binding.pumpStatus.text = " " + resourceHelper.gs(pumpDriverState.resourceId)
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
