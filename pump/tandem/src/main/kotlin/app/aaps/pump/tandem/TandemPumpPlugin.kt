package app.aaps.pump.tandem

import android.content.Context
import android.content.DialogInterface
import android.os.SystemClock
import androidx.preference.Preference
import app.aaps.core.data.model.BS
import app.aaps.core.data.plugin.PluginDescription
import app.aaps.core.data.plugin.PluginType
import app.aaps.core.data.pump.defs.PumpType
import app.aaps.core.interfaces.constraints.Constraint
import app.aaps.core.interfaces.constraints.PluginConstraints
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.objects.Instantiator
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.profile.Profile
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.pump.Pump
import app.aaps.core.interfaces.pump.PumpEnactResult
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventRefreshButtonState
import app.aaps.core.interfaces.rx.events.EventRefreshOverview
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.interfaces.utils.DecimalFormatter
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy
import app.aaps.core.ui.dialogs.OKDialog
import app.aaps.implementation.pump.PumpEnactResultObject
import dagger.android.HasAndroidInjector
import info.nightscout.pump.common.PumpPluginAbstract
import info.nightscout.pump.common.data.PumpStatus
import info.nightscout.aaps.pump.common.events.EventPumpConnectionParametersChanged
import info.nightscout.pump.common.sync.PumpSyncStorage
import info.nightscout.pump.common.utils.ProfileUtil
import info.nightscout.aaps.pump.tandem.driver.connector.TandemPumpConnectionManager
import info.nightscout.aaps.pump.tandem.driver.TandemPumpStatus
import info.nightscout.aaps.pump.tandem.driver.config.TandemPumpDriverConfiguration
import info.nightscout.aaps.pump.tandem.util.TandemPumpConst
import info.nightscout.aaps.pump.tandem.util.TandemPumpUtil

import info.nightscout.pump.common.defs.*
import info.nightscout.aaps.pump.tandem.comm.AAPSTimberTree
import info.nightscout.aaps.pump.common.defs.PumpDriverMode
import info.nightscout.aaps.pump.common.defs.PumpRunningState
import info.nightscout.aaps.pump.common.defs.PumpUpdateFragmentType
import info.nightscout.aaps.pump.common.driver.connector.commands.data.AdditionalResponseDataInterface
import info.nightscout.aaps.pump.common.driver.connector.commands.response.DataCommandResponse
import info.nightscout.aaps.pump.common.driver.connector.commands.response.ResultCommandResponse
import info.nightscout.aaps.pump.tandem.data.defs.TandemStatusRefreshType
import info.nightscout.pump.common.events.EventPumpFragmentValuesChanged

import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by andy on 04.07.2022.
 *
 * @author Andy Rozman (andy.rozman@gmail.com)
 */
@Singleton
class TandemPumpPlugin @Inject constructor(
    injector: HasAndroidInjector,
    aapsLogger: AAPSLogger,
    rxBus: RxBus,
    context: Context,
    rh: ResourceHelper,
    activePlugin: ActivePlugin,
    sp: SP,
    commandQueue: CommandQueue,
    fabricPrivacy: FabricPrivacy,
    val tandemUtil: TandemPumpUtil,
    val pumpStatus: TandemPumpStatus,
    dateUtil: DateUtil,
    val pumpConnectionManager: TandemPumpConnectionManager,
    aapsSchedulers: AapsSchedulers,
    pumpSync: PumpSync,
    pumpSyncStorage: PumpSyncStorage,
    pumpDriverConfiguration: TandemPumpDriverConfiguration,
    decimalFormatter: DecimalFormatter,
    instantiator: Instantiator
) : PumpPluginAbstract(
    PluginDescription() //
        .mainType(PluginType.PUMP) //
        .fragmentClass(TandemPumpFragment::class.java.name) //
        .pluginIcon(R.drawable.ic_tslim_128)
        .pluginName(R.string.tandem_name) //
        .shortName(R.string.tandem_name_short) //
        .preferencesId(R.xml.pref_tandem)
        .description(R.string.description_pump_tandem),  //
    PumpType.TANDEM_T_SLIM_X2_BT,
    rh, aapsLogger, commandQueue, rxBus, activePlugin, sp, context, fabricPrivacy, dateUtil, aapsSchedulers, pumpSync, pumpSyncStorage, pumpDriverConfiguration, decimalFormatter, instantiator
), Pump, PluginConstraints /*, PumpConstraints , PumpDriverConfigurationCapable*/ {

    // variables for handling statuses and history
    private var firstRun = true
    private var isRefresh = false
    private val statusRefreshMap: MutableMap<TandemStatusRefreshType?, Long?> = mutableMapOf()
    private var isInitialized = false
    private var hasTimeDateOrTimeZoneChanged = false
    private var driverMode = PumpDriverMode.Faked // TODO when implementation fully done, default should be automatic

    private var driverInitialized = false
    private var pumpAddress: String = ""
    private var pumpBonded: Boolean = false
    private var aapsTimberTree = AAPSTimberTree(aapsLogger)

    private var pumpX2Version = com.jwoglom.pumpx2.BuildConfig.PUMPX2_VERSION
    private var tandemVersion = "v0.2.6"

    override fun onStart() {
        aapsLogger.debug(LTag.PUMP, model().model + " started - ${tandemVersion} (pumpX2 ${pumpX2Version})")
        super.onStart()
    }

    override fun updatePreferenceSummary(pref: Preference) {
        super.updatePreferenceSummary(pref)
        if (pref.key == rh.gs(R.string.key_tandem_address)) {
            val value: String? = sp.getStringOrNull(R.string.key_tandem_address, null)
            pref.summary = value ?: rh.gs(app.aaps.core.ui.R.string.not_set_short)
        } else if (pref.key == rh.gs(R.string.key_tandem_serial)) {
            val value: String? = sp.getStringOrNull(R.string.key_tandem_serial, null)
            pref.summary = value ?: rh.gs(app.aaps.core.ui.R.string.not_set_short)
        }
        aapsLogger.info(LTag.PUMP, "Preference: $pref")
    }

    //override var logPrefix: String = "TandemPumpPlugin::"

    // PumpAbstract implementations
    override fun initPumpStatusData() {
        pumpStatus.lastConnection = sp.getLong(TandemPumpConst.Statistics.LastGoodPumpCommunicationTime, 0L)
        pumpStatus.lastDataTime = pumpStatus.lastConnection
        pumpStatus.previousConnection = pumpStatus.lastConnection
        aapsLogger.debug(LTag.PUMP, "initPumpStatusData: " + pumpStatus)

        // this is only thing that can change, by being configured
        // TODO pumpDescription.maxTempAbsolute = (pumpStatus.maxBasal != null) ? pumpStatus.maxBasal : 35.0d;
        aapsLogger.debug(LTag.PUMP, "pumpDescription: " + this.pumpDescription)

        pumpStatus.pumpDescription = this.pumpDescription

        // set first Tandem Pump Start
        if (!sp.contains(TandemPumpConst.Statistics.FirstPumpStart)) {
            sp.putLong(TandemPumpConst.Statistics.FirstPumpStart, System.currentTimeMillis())
        }

        pumpStatus.serialNumber = sp.getLong(TandemPumpConst.Prefs.PumpSerial, 0)
    }

    override fun onStartScheduledPumpActions() {

        // Enable logging in jwoglom's X2 library
        //Timber.plant(aapsTimberTree)

        // disposable.add(rxBus
        //                    .toObservable(EventPreferenceChange::class.java)
        //                    .observeOn(aapsSchedulers.io)
        //                    .subscribe({ event: EventPreferenceChange ->
        //                                   if (event.isChanged(rh, TandemPumpConst.Prefs.PumpSerial)) {
        //                                       ypsoPumpStatusHandler.switchPumpData()
        //                                       resetStatusState()
        //                                   }
        //                               }) { throwable: Throwable? -> fabricPrivacy.logException(throwable!!) })



        disposable.add(rxBus
                           .toObservable(EventPumpConnectionParametersChanged::class.java)
                           .observeOn(aapsSchedulers.io)
                           .subscribe({ _ ->
                                          checkInitializationState()
                                      }) { throwable: Throwable? -> fabricPrivacy.logException(throwable!!) })

        rxBus.send(EventPumpConnectionParametersChanged())


        // TODO fix me repetable start with RxJava
//        Observable c = Observable.fromCallable(() -> {
//            //calls.getAndIncrement();
//            int o = 33;
//            return o;
//        })
//                .subscribeOn(aapsSchedulers.getIo())
//                .repeatWhen(z -> z.delay(1, TimeUnit.MINUTES))
//                .doOnError(it -> aapsLogger.error(it.getMessage(), it));

        //disposable += c;

//        disposable += rxBus
//                .toObservable(EventPumpStatusChanged::class.java)
//
//            .observeOn(AndroidSchedulers.mainThread())
//                .subscribe({ updateGUI(UpdateGui.Status) }, { fabricPrivacy.logException(it) })

        //this.disposable += c;

//        this.disposable = this.disposable + Completable.fromCallable({
//        if (this.isInitialized) {
//
//        }
//        }
//        .subscribeOn(schedulerProvider.io)
//                .repeatWhen {
//            it.delay(1, TimeUnit.MINUTES)
//        }
//.subscribeBy(
//                onComplete = {/* ignore, will not be called */},
//                onError = aapsLogger::e
//        )
//;;

        // check status every minute (if any status needs refresh we send readStatus command)
        Thread {
            do {
                SystemClock.sleep(60000)
                if (this.isInitialized) {
                    val statusRefresh = workWithStatusRefresh(
                        StatusRefreshAction.GetData, null, null
                    )
                    if (doWeHaveAnyStatusNeededRefereshing(statusRefresh)) {
                        if (!commandQueue.statusInQueue()) {
                            commandQueue.readStatus("Scheduled Status Refresh", null)
                        }
                    }
                }
            } while (serviceRunning)
        }.start()

        checkInitializationState()

    }

    private fun checkInitializationState() {
        pumpAddress = sp.getString(TandemPumpConst.Prefs.PumpAddress, "")

        val pumpBondStatus = sp.getInt(TandemPumpConst.Prefs.PumpPairStatus, -1)

        driverInitialized = (!pumpAddress.isEmpty() &&
            pumpBondStatus == 100 &&
            !tandemUtil.preventConnect)

        aapsLogger.info(LTag.PUMP, "TANDEMDBG: initialization status: $driverInitialized");
    }

    override val serviceClass: Class<*>?
        get() = null

    override val pumpStatusData: PumpStatus
        get() = pumpStatus

    // TODO: At the moment X2 doesn't support Closed Loop, future pump (t:mobi and t:slim X3) will support it and
    //  maybe also X2 at later time
    // Constraints interface
    override fun isClosedLoopAllowed(value: Constraint<Boolean>): Constraint<Boolean> {
        if (value.value()) {
            value.set(
                //aapsLogger,
                false, //driverMode == PumpDriverMode.Automatic,
                rh.gs(R.string.tandem_fol_closed_loop_not_allowed_x2),
                this
            )
        }
        return value
    }

    override fun isSMBModeEnabled(value: Constraint<Boolean>): Constraint<Boolean> {
        if (value.value()) {
            value.set(
                //aapsLogger,
                driverMode == PumpDriverMode.Automatic, // TODO
                rh.gs(R.string.tandem_fol_smb_not_allowed),
                this
            )
        }
        return value
    }

    // override fun getPumpDriverConfiguration(): PumpDriverConfiguration {
    //     return pumpDriverConfiguration
    // }


    // Pump Interface
    override fun isInitialized(): Boolean {
        aapsLogger.debug(LTag.PUMP, "isInitialized - driverInit=$driverInitialized, preventConnect=${tandemUtil.preventConnect}")
        return driverInitialized //&& !tandemUtil.preventConnect
        //return pumpStatus.ypsopumpFirmware != null;
    }

    override fun isSuspended(): Boolean {
        val suspended = (pumpStatus.pumpRunningState == PumpRunningState.Suspended)
        aapsLogger.debug(LTag.PUMP, "isSuspended - $suspended")
        return suspended
    }

    override fun isConnected(): Boolean {
        if (!driverInitialized)
            return false;
        val driverStatus = tandemUtil.driverStatus
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "isConnected - " + driverStatus.name)
        return driverStatus == PumpDriverState.Ready || driverStatus == PumpDriverState.ExecutingCommand
    }

    override fun isConnecting(): Boolean {
        if (!driverInitialized)
            return false

        val driverStatus = tandemUtil.driverStatus
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "isConnecting - " + driverStatus.name)
        //return pumpState == PumpDriverState.Connecting;
        return driverStatus == PumpDriverState.Connecting || /*driverStatus == PumpDriverState.Connected ||*/ driverStatus == PumpDriverState.EncryptCommunication
    }

    override fun connect(reason: String) {
        if (!driverInitialized)
            return;
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "connect (reason=$reason).")
        pumpConnectionManager.connectToPump() //deviceMac = pumpAddress, deviceBonded = pumpBonded)
    }

    override fun disconnect(reason: String) {
        if (!driverInitialized)
            return;

        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "disconnect (reason=$reason).")
        pumpConnectionManager.disconnectFromPump()
    }

    override fun stopConnecting() {
        if (!driverInitialized)
            return;
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "stopConnecting [PumpPluginAbstract] - default (empty) implementation.")
    }

    override fun isHandshakeInProgress(): Boolean {
        if (!driverInitialized)
            return false;

        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "isHandshakeInProgress - " + tandemUtil.driverStatus.name)
        return tandemUtil.driverStatus === PumpDriverState.Connecting
    }

    override fun finishHandshaking() {
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "finishHandshaking [PumpPluginAbstract] - default (empty) implementation.")
    }

    override val isFakingTempsByExtendedBoluses: Boolean
        get() = false

    override fun canHandleDST(): Boolean {
        return false
    }

    override fun isBusy(): Boolean {
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, logPrefix + "isBusy")
        return false
    }

    override fun getPumpStatus(reason: String) {
        var needRefresh = true
        if (firstRun) {
            needRefresh = initializePump(!isRefresh)
        } else {
            refreshAnyStatusThatNeedsToBeRefreshed()
        }
        if (needRefresh) rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Full))
    }

    fun resetStatusState() {
        firstRun = true
        isRefresh = true
    }

    private fun refreshAnyStatusThatNeedsToBeRefreshed(): Boolean {
        val statusRefresh = workWithStatusRefresh(
            StatusRefreshAction.GetData, null,
            null)
        if (!doWeHaveAnyStatusNeededRefereshing(statusRefresh)) {
            return false
        }
        var resetTime = false
        var resetDisplay = false
        if (hasTimeDateOrTimeZoneChanged) {
            checkTimeAndOptionallySetTime()

            // read time if changed, set new time
            hasTimeDateOrTimeZoneChanged = false
        }

        // execute
        val refreshTypesNeededToReschedule: MutableSet<TandemStatusRefreshType> = HashSet()
        for ((key, value) in statusRefresh!!) {
            if (value!! > 0 && System.currentTimeMillis() > value) {
                when (key) {
                    TandemStatusRefreshType.PumpHistory      -> {
                        readPumpHistory()
                    }

                    TandemStatusRefreshType.PumpTime         -> {
                        if (checkTimeAndOptionallySetTime()) {
                            resetDisplay = true
                        }
                        refreshTypesNeededToReschedule.add(key)
                        resetTime = true
                    }

                    TandemStatusRefreshType.BatteryStatus,
                    TandemStatusRefreshType.RemainingInsulin -> {
                        pumpConnectionManager.getRemainingInsulin()
                        refreshTypesNeededToReschedule.add(key)
                        resetDisplay = true
                        resetTime = true
                    }

                    null                                                                                -> TODO()
                }
            }

            // reschedule
            for (refreshType2 in refreshTypesNeededToReschedule) {
                scheduleNextRefresh(refreshType2)
            }
        }

        if (resetTime)
            pumpStatus.setLastCommunicationToNow()

        return resetDisplay
    }

    private fun doWeHaveAnyStatusNeededRefereshing(statusRefresh: Map<TandemStatusRefreshType?, Long?>?): Boolean {
        for ((_, value) in statusRefresh!!) {
            if (value!! > 0 && System.currentTimeMillis() > value) {
                return true
            }
        }
        return hasTimeDateOrTimeZoneChanged
    }

    private fun setRefreshButtonEnabled(enabled: Boolean) {
        rxBus.send(EventRefreshButtonState(enabled))
    }

    // TODO not implement
    private fun initializePump(realInit: Boolean): Boolean {
        if (isInitialized) return false
        aapsLogger.info(LTag.PUMP, logPrefix + "initializePump - start")

        // TODO Tandem handle this
        // if (pumpStatus.serialNumber == null) {
        //     aapsLogger.info(LTag.PUMP, logPrefix + "initializePump - serial Number is null, initialization stopped.")
        //     return false
        // }

        setRefreshButtonEnabled(false)
        pumpState = PumpDriverState.Connected

        // TODO remove
        //startUITest()

        // firmware version
        pumpConnectionManager.determineFirmwareVersion()
        setDriverMode()

        // TODO time (1h) - setting time command not available
        checkTimeAndOptionallySetTime()

        // TODO read status of pump from Db
        //ypsoPumpHistoryHandler.readCurrentStatusOfPump()

        // TODO readPumpHistory
        readPumpHistory()

        // TODO remaining insulin (>50 = 4h; 50-20 = 1h; 15m) -
        pumpConnectionManager.getRemainingInsulin() // (command not available)
        scheduleNextRefresh(TandemStatusRefreshType.RemainingInsulin, 10)

        // TODO remaining power (1h) -
        pumpConnectionManager.getBatteryLevel()
        scheduleNextRefresh(TandemStatusRefreshType.BatteryStatus, 20)

        // configuration (once and then if history shows config changes)
        pumpConnectionManager.getConfiguration()

        // TODO read profile (once, later its controlled by isThisProfileSet method)
        pumpConnectionManager.getBasalProfile()

        pumpStatus.setLastCommunicationToNow()
        setRefreshButtonEnabled(true)

        if (!isRefresh) {
            pumpState = PumpDriverState.Initialized
        }

        isInitialized = true
        firstRun = false

        return true
    }

    private fun startUITest() {
        aapsLogger.debug(LTag.PUMP, "Before UI Test")

        var resultCommandResponse: ResultCommandResponse? = null


        Thread(Runnable {
            SystemClock.sleep(500)
            //ErrorHelperActivity.runAlarm(context, rh.gs(R.string.medtronic_cmd_cancel_bolus_not_supported), rh.gs(R.string.medtronic_warning), R.raw.boluserror)

            OKDialog.showConfirmation(context = context,
                                      title = rh.gs(R.string.ypsopump_cmd_exec_title_set_profile),
                                      message = rh.gs(R.string.ypsopump_cmd_exec_desc_set_profile, "Unknown"),
                                      { _: DialogInterface?, _: Int ->
                                          //commandResponse = CommandResponse.builder().success(true).build()
                                          aapsLogger.debug(LTag.PUMP, "UI Test -> Success")
                                      }, { _: DialogInterface?, _: Int ->
                                          //commandResponse = CommandResponse.builder().success(true).build()
                                          aapsLogger.debug(LTag.PUMP, "UI Test -> Cancel")
                                      })

        }).start()

        //Looper.prepare()

        aapsLogger.debug(LTag.PUMP, "After UI Test: $resultCommandResponse")
    }

    private fun setDriverMode() {
        // TODO history refresh data
        // if (pumpStatus.isFirmwareSet) {
        //     if (pumpStatus.ypsopumpFirmware.isClosedLoopPossible) {
        //         // TODO
        //     } else {
        //         this.driverMode = YpsoDriverMode.ForcedOpenLoop
        //     }
        // } else
            this.driverMode = PumpDriverMode.Faked
    }

    // private val basalProfiles: Unit
    //     get() {
    //         if (!pumpConnectionManager.basalProfile) {
    //             pumpConnectionManager.basalProfile
    //         }
    //     }

    var profile: Profile? = null

    override fun isThisProfileSet(profile: Profile): Boolean {
        aapsLogger.debug(LTag.PUMP, "isThisProfileSet ")

        if (!isInitialized)
            return true

        // if (driverMode == PumpDriverMode.Faked) {
        //     //this.profile = profile  // TODO remove this later
        //     pumpStatus.basalProfile = profile
        // }

        // if (driverMode == PumpDriverMode.Faked) {
        //     aapsLogger.debug(LTag.PUMP, "  Faked mode: returning true")
        //     return true
        // } else {

        if (pumpStatus.basalProfile == null) {
            aapsLogger.debug(LTag.PUMP, "  Pump Profile:     null, returning false")
            return false
        } else {
            val profileAsString = ProfileUtil.getBasalProfilesDisplayableAsStringOfArray(profile, PumpType.TANDEM_T_SLIM_X2)
            //val profileDriver = ProfileUtil.getProfilesByHourToString(pumpStatus.basalProfilePump!!.basalPatterns)
            val profileDriver = ProfileUtil.getBasalProfilesDisplayableAsStringOfArray(pumpStatus.basalProfile!!, PumpType.TANDEM_T_SLIM_X2)

            aapsLogger.debug(LTag.PUMP, "AAPS Profile:     $profileAsString")
            aapsLogger.debug(LTag.PUMP, "Pump Profile:     $profileDriver")

            val areTheySame = profileAsString.equals(profileDriver)

            aapsLogger.debug(LTag.PUMP, "Pump Profile is the same: $areTheySame")

            return areTheySame
        }
        //}
    }

    override fun lastDataTime(): Long {
        return if (pumpStatus.lastConnection != 0L) {
            pumpStatus.lastConnection
        } else System.currentTimeMillis()
    }


    override val baseBasalRate: Double
        get() =// TODO fix this
            if (pumpStatus.basalProfile == null) {
                aapsLogger.debug("Profile is not set: ")
                pumpStatus.baseBasalRate = 0.0
                0.0
            } else {
                val gc = GregorianCalendar()
                val time = gc[Calendar.HOUR_OF_DAY] * 60 * 60 + (gc[Calendar.MINUTE] * 60).toLong()
                val basal = pumpStatus.basalProfile!!.getBasal(time)
                aapsLogger.debug("Basal for this hour is: $basal")
                pumpStatus.baseBasalRate = basal
                basal
            }

    override val reservoirLevel: Double
        get() = pumpStatus.reservoirRemainingUnits

    override val batteryLevel: Int
        get() = pumpStatus.batteryRemaining

    override fun triggerUIChange() {
        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.TreatmentValues))
    }

    override fun hasService(): Boolean {
        return false
    }

    private var bolusDeliveryType = BolusDeliveryType.Idle

    private enum class BolusDeliveryType {
        Idle,  //
        DeliveryPrepared,  //
        Delivering,  //
        CancelDelivery
    }

    private fun checkTimeAndOptionallySetTime(): Boolean {
        aapsLogger.info(LTag.PUMP, logPrefix + "checkTimeAndOptionallySetTime - Start")

        // try {
        //
        //     val clock = pumpConnectionManager.getTime()
        //
        //     if (clock != null) {
        //         // TODO check if this works, migneed to use LocalDateTime...
        //         //pumpStatus.pumpTime = PumpTimeDifferenceDto(DateTime.now(), clock.toLocalDateTime())
        //         val diff = Math.abs(pumpStatus.pumpTime!!.timeDifference)
        //
        //         if (diff > 60) {
        //             aapsLogger.error(LTag.PUMP, "Time difference between phone and pump is more than 60s ($diff)")
        //
        //             // TODO fix this
        //
        //             // if (!pumpStatus.ypsopumpFirmware.isClosedLoopPossible) {
        //             //     val notification = Notification(Notification.PUMP_PHONE_TIME_DIFF_TOO_BIG, rh.gs(R.string.time_difference_too_big, 60, diff), Notification.INFO, 60)
        //             //     rxBus.send(EventNewNotification(notification))
        //             // } else {
        //             //
        //             //     // TODO setNewTime, different notification
        //             //     val time = pumpConnectionManager.setTime()
        //             //
        //             //     if (time != null) {
        //             //         pumpStatus.pumpTime = PumpTimeDifferenceDto(DateTime.now(), time.toLocalDateTime())
        //             //
        //             //         val newTimeDiff = Math.abs(pumpStatus.pumpTime!!.timeDifference)
        //             //
        //             //         if (newTimeDiff < 60) {
        //             //             val notification = Notification(
        //             //                 Notification.INSIGHT_DATE_TIME_UPDATED,
        //             //                 rh.gs(R.string.pump_time_updated),
        //             //                 Notification.INFO, 60
        //             //             )
        //             //             rxBus.send(EventNewNotification(notification))
        //             //         } else {
        //             //             aapsLogger.error(LTag.PUMP, "Setting time on pump failed.")
        //             //         }
        //             //     } else {
        //             //         aapsLogger.error(LTag.PUMP, "Setting time on pump failed.")
        //             //     }
        //             // }
        //         }
        //     }
        // } catch (ex: Exception) {
        //     aapsLogger.error(LTag.PUMP, "Setting time on pump failed.")
        // } finally {
        //     setRefreshButtonEnabled(false)
        //     scheduleNextRefresh(TandemStatusRefreshType.PumpTime, 0)
        // }

        return true
    }

    // TODO progress bar
    override fun deliverBolus(detailedBolusInfo: DetailedBolusInfo): PumpEnactResult {
        aapsLogger.info(LTag.PUMP, logPrefix + "deliverBolus - " + BolusDeliveryType.DeliveryPrepared)
        return if (detailedBolusInfo.insulin > pumpStatus.reservoirRemainingUnits) {
            PumpEnactResultObject(rh) //
                .success(false) //
                .enacted(false) //
                .comment(
                    rh.gs(
                        R.string.ypsopump_cmd_bolus_could_not_be_delivered_no_insulin,
                        pumpStatus.reservoirRemainingUnits,
                        detailedBolusInfo.insulin
                    )
                )
        } else try {
            setRefreshButtonEnabled(false)

            val commandResponse = pumpConnectionManager.deliverBolus(detailedBolusInfo)

            if (commandResponse!=null && commandResponse.isSuccess) {
                val now = System.currentTimeMillis()

                detailedBolusInfo.bolusTimestamp = now

                // we subtract insulin, exact amount will be visible with next remainingInsulin update.
                //pumpStatus.reservoirRemainingUnits -= detailedBolusInfo.insulin

                incrementStatistics(if (detailedBolusInfo.bolusType == BS.Type.SMB)
                    TandemPumpConst.Statistics.SMBBoluses
                else
                    TandemPumpConst.Statistics.StandardBoluses)

                if (detailedBolusInfo.carbs > 0.0) {
                    pumpSync.syncCarbsWithTimestamp(
                        timestamp = now,
                        amount = detailedBolusInfo.carbs,
                        pumpId = null,
                        pumpType = PumpType.TANDEM_T_SLIM_X2_BT,
                        pumpSerial = serialNumber()
                    )
                }

                readPumpHistoryAfterAction(bolusInfo = detailedBolusInfo)

                PumpEnactResultObject(rh).success(true) //
                    .enacted(true) //
                    .bolusDelivered(detailedBolusInfo.insulin) //
                    //.carbsDelivered(detailedBolusInfo.carbs)   // TODO
            } else {
                PumpEnactResultObject(rh) //
                    .success(false) //
                    .enacted(false) //
                    .comment(rh.gs(R.string.ypsopump_cmd_bolus_could_not_be_delivered))
            }
        } finally {
            finishAction("Bolus")
        }
    }

    override fun stopBolusDelivering() {
        bolusDeliveryType = BolusDeliveryType.CancelDelivery
        // TODO if there is command
        if (isLoggingEnabled) aapsLogger.warn(LTag.PUMP, "TandemPumpPlugin::deliverBolus - Stop Bolus Delivery.")
    }

    private val isLoggingEnabled: Boolean
        get() = true


    // if enforceNew===true current temp basal is canceled and new TBR set (duration is prolonged),
    // if false and the same rate is requested enacted=false and success=true is returned and TBR is not changed
    @Synchronized
    override fun setTempBasalPercent(percent: Int, durationInMinutes: Int, profile: Profile,
                                     enforceNew: Boolean, tbrType: PumpSync.TemporaryBasalType
    ): PumpEnactResult {
        setRefreshButtonEnabled(false)
        return try {
            aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent: rate: " + percent + "%, duration=" + durationInMinutes)

            // read current TBR
            val tbrCurrent = readTBR()
            if (tbrCurrent == null) {
                aapsLogger.warn(LTag.PUMP, logPrefix + "setTempBasalPercent - Could not read current TBR, canceling operation.")
                return PumpEnactResultObject(rh).success(false).enacted(false)
                    .comment(rh.gs(R.string.ypsopump_cmd_cant_read_tbr))
            } else {
                aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent: Current Basal: duration: " + tbrCurrent.durationMinutes + " min, rate=" + tbrCurrent.insulinRate)
            }
            if (!enforceNew) {
                if (tandemUtil.isSame(tbrCurrent.insulinRate, percent)) {
                    var sameRate = true
                    if (tandemUtil.isSame(0.0, percent) && durationInMinutes > 0) {
                        // if rate is 0.0 and duration>0 then the rate is not the same
                        sameRate = false
                    }
                    if (sameRate) {
                        aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent - No enforceNew and same rate. Exiting.")
                        return PumpEnactResultObject(rh).success(true).enacted(false)
                    }
                }
                // if not the same rate, we cancel and start new
            }

            // if TBR is running we will cancel it.
            if (tbrCurrent.insulinRate != 0.0 && tbrCurrent.durationMinutes > 0) {
                aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent - TBR running - so canceling it.")

                // CANCEL
                val commandResponse = pumpConnectionManager.cancelTemporaryBasal()
                if (commandResponse.isSuccess) {
                    aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent - Current TBR cancelled.")
                } else {
                    aapsLogger.error(logPrefix + "setTempBasalPercent - Cancel TBR failed.")
                    return PumpEnactResultObject(rh).success(false).enacted(false)
                        .comment(rh.gs(R.string.ypsopump_cmd_cant_cancel_tbr_stop_op))
                }
            }

            // now start new TBR
            val commandResponse = pumpConnectionManager.setTemporaryBasal(percent, durationInMinutes)
            aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent - setTBR. Response: " + commandResponse)
            if (commandResponse!=null && commandResponse.isSuccess) {
                pumpStatus.tempBasalStart = System.currentTimeMillis()
                pumpStatus.tempBasalPercent = percent
                pumpStatus.tempBasalDuration = durationInMinutes
                pumpStatus.tempBasalEnd = System.currentTimeMillis() + durationInMinutes * 60 * 1000

                readPumpHistoryAfterAction(tempBasalInfo = TempBasalPair(
                    insulinRate = percent.toDouble(),
                    isPercent = true,
                    durationMinutes = durationInMinutes))

                incrementStatistics(TandemPumpConst.Statistics.TBRsSet)
                PumpEnactResultObject(rh).success(true).enacted(true) //
                    .percent(percent).duration(durationInMinutes)
            } else {
                PumpEnactResultObject(rh).success(false).enacted(false) //
                    .comment(rh.gs(R.string.ypsopump_cmd_tbr_could_not_be_delivered))
            }
        } finally {
            finishAction("TBR")
        }
    }

    // TODO setTempBasalAbsolute
    override fun setTempBasalAbsolute(absoluteRate: Double, durationInMinutes: Int, profile: Profile,
                                      enforceNew: Boolean, tbrType: PumpSync.TemporaryBasalType
    ): PumpEnactResult {
        aapsLogger.debug(LTag.PUMP, "setTempBasalAbsolute called with a rate of $absoluteRate for $durationInMinutes min.")
        val unroundedPercentage = java.lang.Double.valueOf(absoluteRate / baseBasalRate * 100).toInt()
        // TODO unroundedPercentage needs to be rounded correctly
        return this.setTempBasalPercent(unroundedPercentage, durationInMinutes, profile, enforceNew, tbrType)
    }


    private fun finishAction(overviewKey: String?) {
        if (overviewKey != null) rxBus.send(EventRefreshOverview(overviewKey, false))
        triggerUIChange()
        setRefreshButtonEnabled(true)
    }

    // TODO we might want to return data
    private fun readPumpHistory() {
        aapsLogger.warn(LTag.PUMP, logPrefix + "readPumpHistory N/A WIP.")

        //ypsoPumpHistoryHandler.getFullPumpHistory()

        //        pumpConnectionManager.getPumpHistory()

        scheduleNextRefresh(TandemStatusRefreshType.PumpHistory)

    }


    private fun readPumpHistoryAfterAction(bolusInfo: DetailedBolusInfo? = null,
                                           tempBasalInfo: TempBasalPair? = null,
                                           profile: Profile? = null) {
        // TODO
        // if (true)
        //     return
        aapsLogger.warn(LTag.PUMP, logPrefix + "readPumpHistoryAfterAction N/A.")
        // ypsoPumpHistoryHandler.getLastEventAndSendItToPumpSync(
        //     bolusInfo = bolusInfo,
        //     tempBasalInfo = tempBasalInfo,
        //     profile = profile
        // )
    }

    private fun scheduleNextRefresh(refreshType: TandemStatusRefreshType, additionalTimeInMinutes: Int = 0) {
        when (refreshType) {
            TandemStatusRefreshType.RemainingInsulin -> {
                val remaining = pumpStatus.reservoirRemainingUnits
                val min: Int
                min = if (remaining > 50) 4 * 60 else if (remaining > 20) 60 else 15
                workWithStatusRefresh(StatusRefreshAction.Add, refreshType, getTimeInFutureFromMinutes(min))
            }

            TandemStatusRefreshType.PumpTime,
                //YpsoPumpStatusRefreshType.Configuration,
            TandemStatusRefreshType.BatteryStatus,
            TandemStatusRefreshType.PumpHistory      -> {
                workWithStatusRefresh(
                    StatusRefreshAction.Add, refreshType,
                    getTimeInFutureFromMinutes(getHistoryRefreshTime() + additionalTimeInMinutes)
                )
            }

        }
    }

    private fun getHistoryRefreshTime(): Int {
        if (this.driverMode != PumpDriverMode.Automatic) {
            return 15 // TODO use settings
        } else {
            return 5
        }
    }

    private enum class StatusRefreshAction {
        Add,  //
        GetData
    }

    @Synchronized
    private fun workWithStatusRefresh(
        action: StatusRefreshAction,  //
        statusRefreshType: TandemStatusRefreshType?,  //
        time: Long?
    ): Map<TandemStatusRefreshType?, Long?>? {
        return when (action) {
            StatusRefreshAction.Add     -> {
                statusRefreshMap[statusRefreshType] = time
                null
            }

            StatusRefreshAction.GetData -> {
                HashMap(statusRefreshMap)
            }

            //else                        -> null
        }
    }


    private fun getTimeInFutureFromMinutes(minutes: Int): Long {
        return System.currentTimeMillis() + getTimeInMs(minutes)
    }

    private fun getTimeInMs(minutes: Int): Long {
        return minutes * 60 * 1000L
    }

    private fun readTBR(): TempBasalPair? {
        val temporaryBasalResponse = pumpConnectionManager.getTemporaryBasal()
        return if (temporaryBasalResponse.value!=null) {
            val tbr = temporaryBasalResponse.value!!

            // we sometimes get rate returned even if TBR is no longer running
            if (tbr.durationMinutes == 0) {
                tbr.insulinRate = 0.0
            }
            tbr
        } else {
            null
        }
    }

    override fun cancelTempBasal(enforceNew: Boolean): PumpEnactResult {
        return try {
            aapsLogger.info(LTag.PUMP, logPrefix + "cancelTempBasal - started")
            setRefreshButtonEnabled(false)
            val tbrCurrent = readTBR()
            if (tbrCurrent != null) {
                if (tbrCurrent.insulinRate == 0.0 && tbrCurrent.durationMinutes == 0) {
                    aapsLogger.info(LTag.PUMP, logPrefix + "cancelTempBasal - TBR already canceled.")
                    finishAction("TBR")
                    return PumpEnactResultObject(rh).success(true).enacted(false)
                }
            } else {
                aapsLogger.warn(LTag.PUMP, logPrefix + "cancelTempBasal - Could not read currect TBR, canceling operation.")
                finishAction("TBR")
                return PumpEnactResultObject(rh).success(false).enacted(false)
                    .comment(rh.gs(R.string.ypsopump_cmd_cant_read_tbr))
            }
            val commandResponse = pumpConnectionManager.cancelTemporaryBasal()
            if (commandResponse.isSuccess) {
                aapsLogger.info(LTag.PUMP, logPrefix + "cancelTempBasal - Cancel TBR successful.")

                readPumpHistoryAfterAction(tempBasalInfo = TempBasalPair(0.0, true, 0))

                PumpEnactResultObject(rh).success(true).enacted(true) //
                    .isTempCancel(true)
            } else {
                aapsLogger.info(LTag.PUMP, logPrefix + "cancelTempBasal - Cancel TBR failed.")
                PumpEnactResultObject(rh).success(false).enacted(false) //
                    .comment(rh.gs(R.string.ypsopump_cmd_cant_cancel_tbr))
            }
        } finally {
            finishAction("TBR")
        }
    }

    override fun serialNumber(): String {
        return if (pumpStatus.serialNumber == null) "" else pumpStatus.serialNumber.toString()
    }

    override fun generateTempId(objectA: Any): Long {
        TODO("Not yet implemented")
    }

    //    @NotNull @Override
    //    public Constraint<Boolean> isClosedLoopAllowed(@NotNull Constraint<Boolean> value) {
    ////        if(pumpStatus.ypsopumpFirmware==null) {
    ////            return value.set(aapsLogger,false, rh.gs(R.string.some_reason), this);
    ////        } else {
    ////            return value;
    ////        }
    //
    //        return new Constraint<Boolean>(true);
    //    }

    //    @NonNull @Override
    //    public PumpEnactResult setNewBasalProfile(Profile profile) {
    //        aapsLogger.info(LTag.PUMP, getLogPrefix() + "setNewBasalProfile");
    //        setRefreshButtonEnabled(false);
    //
    //        // basalsByHour
    //        CommandResponse commandResponse = pumpConnectionManager.setBasalProfile(profile);
    //
    //        if (commandResponse.isSuccess()) {
    //            return new PumpEnactResult(getInjector()).success(true).enacted(true);
    //        } else {
    //            return new PumpEnactResult(getInjector()).success(false).enacted(false) //
    //                    .comment(getrh().gs(R.string.ypsopump_cmd_basal_profile_could_not_be_set));
    //        }

    override fun setNewBasalProfile(profile: Profile): PumpEnactResult {
        aapsLogger.info(LTag.PUMP, logPrefix + "setNewBasalProfile")
        return try {
            setRefreshButtonEnabled(false)
            var resultCommandResponse: DataCommandResponse<AdditionalResponseDataInterface?>
            var driverModeCurrent = driverMode

            if (driverModeCurrent == PumpDriverMode.Faked) {
                resultCommandResponse = pumpConnectionManager.setBasalProfile(profile)
            } else if (driverModeCurrent == PumpDriverMode.ForcedOpenLoop) {

                aapsLogger.error(LTag.PUMP, "Forced Open Loop: setNewBasalProfile")
                // Looper.prepare()
                // OKDialog.showConfirmation(context = context,
                //     title = rh.gs(R.string.ypsopump_cmd_exec_title_set_profile),
                //     message = rh.gs(R.string.ypsopump_cmd_exec_desc_set_profile, "Unknown"),
                //     { _: DialogInterface?, _: Int ->
                //         commandResponse = CommandResponse.builder().success(true).build()
                //     }, null)

                // TODO
                resultCommandResponse = pumpConnectionManager.setBasalProfile(profile)

            } else {
                resultCommandResponse = pumpConnectionManager.setBasalProfile(profile)
            }

            readPumpHistoryAfterAction(profile = profile)

//        String profileInvalid = isProfileValid(basalProfile);
//
//        if (profileInvalid != null) {
//            return new PumpEnactResult(getInjector()) //
//                    .success(false) //
//                    .enacted(false) //
//                    .comment(getrh().gs(R.string.medtronic_cmd_set_profile_pattern_overflow, profileInvalid));
//        }

            // TODO setProfile 1.5 (Ui) and 1.6 send command
            //val commandResponse = pumpConnectionManager.setBasalProfile(profile)
            aapsLogger.info(LTag.PUMP, logPrefix + "Basal Profile was set: " + resultCommandResponse)
            if (resultCommandResponse != null && resultCommandResponse.isSuccess) {
                pumpStatus.basalProfile = profile
                PumpEnactResultObject(rh).success(true).enacted(true)
            } else {
                PumpEnactResultObject(rh).success(false).enacted(false) //
                    .comment(rh.gs(R.string.ypsopump_cmd_basal_profile_could_not_be_set))
            }
        } finally {
            finishAction("Set Basal Profile")
        }
    }

    //    private String isProfileValid(BasalProfile basalProfile) {
    //
    //        StringBuilder stringBuilder = new StringBuilder();
    //
    //        if (ypsopumpPumpStatus.maxBasal == null)
    //            return null;
    //
    //        for (BasalProfileEntry profileEntry : basalProfile.getEntries()) {
    //
    //            if (profileEntry.rate > ypsopumpPumpStatus.maxBasal) {
    //                stringBuilder.append(profileEntry.startTime.toString("HH:mm"));
    //                stringBuilder.append("=");
    //                stringBuilder.append(profileEntry.rate);
    //            }
    //        }
    //
    //        return stringBuilder.length() == 0 ? null : stringBuilder.toString();
    //    }
    //
    //
    //    @NonNull
    //    private BasalProfile convertProfileToMedtronicProfile(Profile profile) {
    //
    //        BasalProfile basalProfile = new BasalProfile(aapsLogger);
    //
    //        for (int i = 0; i < 24; i++) {
    //            double rate = profile.getBasalTimeFromMidnight(i * 60 * 60);
    //
    //            double v = pumpDescription.pumpType.determineCorrectBasalSize(rate);
    //
    //            BasalProfileEntry basalEntry = new BasalProfileEntry(v, i, 0);
    //            basalProfile.addEntry(basalEntry);
    //
    //        }
    //
    //        basalProfile.generateRawDataFromEntries();
    //
    //        return basalProfile;
    //    }


    // OPERATIONS not supported by Pump or Plugin


    // override fun generateTempId(dataObject: Any?): Long {
    //     return 0L
    // }

    init {
        displayConnectionMessages = true
    }
}