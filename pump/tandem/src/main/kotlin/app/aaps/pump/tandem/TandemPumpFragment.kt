package app.aaps.pump.tandem

//import kotlinx.android.synthetic.main.ypsopump_fragment.*
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.aaps.core.data.time.T
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.pump.WarnColors
import app.aaps.core.interfaces.queue.Callback
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventExtendedBolusChange
import app.aaps.core.interfaces.rx.events.EventQueueChanged
import app.aaps.core.interfaces.rx.events.EventRefreshButtonState
import app.aaps.core.interfaces.rx.events.EventTempBasalChange
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy
import app.aaps.core.ui.dialogs.OKDialog
import app.aaps.pump.tandem.databinding.TandemFragmentBinding
import dagger.android.support.DaggerFragment

import info.nightscout.pump.common.defs.PumpDriverState
import info.nightscout.aaps.pump.common.defs.PumpUpdateFragmentType

import info.nightscout.aaps.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.aaps.pump.tandem.driver.TandemPumpStatus
import info.nightscout.aaps.pump.tandem.data.events.EventPumpDriverStateChanged
import info.nightscout.aaps.pump.tandem.util.TandemPumpUtil

import info.nightscout.pump.common.events.EventPumpFragmentValuesChanged


import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import javax.inject.Inject

class TandemPumpFragment : DaggerFragment() {

    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var fabricPrivacy: FabricPrivacy
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var commandQueue: CommandQueue
    @Inject lateinit var activePlugin: ActivePlugin
    @Inject lateinit var warnColors: WarnColors
    @Inject lateinit var pumpUtil: TandemPumpUtil
    @Inject lateinit var pumpStatus: TandemPumpStatus
    @Inject lateinit var dateUtil: DateUtil
    @Inject lateinit var pumpSync: PumpSync
    @Inject lateinit var tandemPumpPlugin: TandemPumpPlugin
    @Inject lateinit var aapsSchedulers: AapsSchedulers

    private var disposable: CompositeDisposable = CompositeDisposable()

    private val loopHandler = Handler(Looper.getMainLooper())
    private lateinit var refreshLoop: Runnable

    private var _binding: TandemFragmentBinding? = null

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
        _binding = TandemFragmentBinding.inflate(inflater, container, false)
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
            tandemPumpPlugin.resetStatusState()
            commandQueue.readStatus("Clicked refresh", object : Callback() {
                override fun run() {
                    activity?.runOnUiThread { binding.pumpRefresh.isEnabled = true }
                }
            })
        }

        binding.pumpHistory.setOnClickListener {
            //startActivity(Intent(context, PumpHistoryActivity::class.java))
        }

    }

    @Synchronized
    override fun onResume() {
        super.onResume()

        loopHandler.postDelayed(refreshLoop, T.mins(1).msecs())
        disposable += rxBus
            .toObservable(EventRefreshButtonState::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({ binding.pumpRefresh.isEnabled = it.newState }, { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventPumpDriverStateChanged::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({ updatePumpStatus(it.driverStatus) }, { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventExtendedBolusChange::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({ updateGUI(PumpUpdateFragmentType.TreatmentValues) }, { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventTempBasalChange::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({ updateGUI(PumpUpdateFragmentType.TreatmentValues) }, { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventPumpFragmentValuesChanged::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({ updateGUI(it.updateType) }, { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventQueueChanged::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({ updateGUI(PumpUpdateFragmentType.Queue) }, { fabricPrivacy.logException(it) })

        updateGUI(PumpUpdateFragmentType.Full)
    }

    @Synchronized
    override fun onPause() {
        super.onPause()
        disposable.clear()
        loopHandler.removeCallbacks(refreshLoop)
    }

    private fun displayNotConfiguredDialog() {
        context?.let {
            OKDialog.show(it, resourceHelper.gs(R.string.medtronic_warning),
                          resourceHelper.gs(R.string.medtronic_error_operation_not_possible_no_configuration), null)
        }
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
                    binding.pumpLastConnection.text = resourceHelper.gs(app.aaps.core.interfaces.R.string.minago, min)
                } else if (min < 1440) {
                    val h = (min / 60).toInt()
                    binding.pumpLastConnection.text = resourceHelper.gs(app.aaps.core.interfaces.R.string.hoursago, h, h)
                } else {
                    val h = (min / 60).toInt()
                    val d = h / 24
                    // h = h - (d * 24);
                    binding.pumpLastConnection.text = resourceHelper.gs(app.aaps.core.interfaces.R.string.days_ago, d, d)
                }
                binding.pumpLastConnection.setTextColor(Color.RED)
            } else {
                binding.pumpLastConnection.text = minAgo
                binding.pumpLastConnection.setTextColor(Color.WHITE)
            }
        }

        if (updateType == PumpUpdateFragmentType.PumpStatus || updateType == PumpUpdateFragmentType.Full) {
            // Pump Status (Error)
            val pumpDriverState: PumpDriverState = pumpUtil.driverStatus

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
                val unit = resourceHelper.gs(app.aaps.core.ui.R.string.insulin_unit_shortname)
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
                + resourceHelper.gs(app.aaps.core.ui.R.string.pump_base_basal_rate, pumpStatus.baseBasalRate))

            //TBR TODO
            // binding.pumpTempBasal.text = activePlugin.activeTreatments.getTempBasalFromHistory(System.currentTimeMillis())?.toStringFull()
            //     ?: ""
        }

        if (updateType == PumpUpdateFragmentType.Configuration || updateType == PumpUpdateFragmentType.Full) {
            // Firmware, Errors
//            if (pumpStatus.ypsopumpFirmware != null) {
            if (pumpStatus.tandemPumpFirmware.isClosedLoopPossible) {
                binding.pumpFirmware.text = pumpStatus.tandemPumpFirmware.description
            } else {
                binding.pumpFirmware.text = resourceHelper.gs(R.string.pump_firmware_open_loop_only, pumpStatus.tandemPumpFirmware.description)
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
            warnColors.setColorInverse(binding.pumpBattery, pumpStatus.batteryRemaining.toDouble(), 25, 10)

            // reservoir
            binding.pumpReservoir.text = resourceHelper.gs(app.aaps.core.ui.R.string.reservoir_value, pumpStatus.reservoirRemainingUnits, pumpStatus.reservoirFullUnits)
            warnColors.setColorInverse(binding.pumpReservoir, pumpStatus.reservoirRemainingUnits, 50, 20)
        }

    }

    private fun updatePumpStatus(pumpDriverState: PumpDriverState?) {
        when (pumpDriverState) {
            null,
            PumpDriverState.Ready,
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
                var commandType: PumpCommandType? = pumpUtil.currentCommand
                if (commandType == null) {
                    binding.pumpStatus.text = "{fa-bed}   "
                } else {
                    binding.pumpStatus.text = "{fa-bluetooth-b}   " + resourceHelper.gs(commandType.resourceId)
                }
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
