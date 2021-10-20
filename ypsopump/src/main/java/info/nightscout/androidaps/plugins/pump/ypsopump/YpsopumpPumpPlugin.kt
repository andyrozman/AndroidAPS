package info.nightscout.androidaps.plugins.pump.ypsopump

import android.content.Context
import android.content.DialogInterface
import android.os.SystemClock
import androidx.preference.Preference
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.data.PumpEnactResult
import info.nightscout.androidaps.events.EventPreferenceChange
import info.nightscout.androidaps.events.EventRefreshOverview
import info.nightscout.androidaps.interfaces.*
import info.nightscout.androidaps.interfaces.PumpSync.TemporaryBasalType
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification
import info.nightscout.androidaps.plugins.pump.common.PumpPluginAbstract
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus
import info.nightscout.androidaps.plugins.pump.common.data.PumpTimeDifferenceDto
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.defs.PumpRunningState
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType
import info.nightscout.androidaps.plugins.pump.common.defs.PumpUpdateFragmentType
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.events.EventPumpFragmentValuesChanged
import info.nightscout.androidaps.plugins.pump.common.events.EventRefreshButtonState
import info.nightscout.androidaps.plugins.pump.common.sync.PumpSyncStorage
import info.nightscout.androidaps.plugins.pump.common.utils.ProfileUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpConnectionManager
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoDriverMode
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpStatusRefreshType
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.handlers.YpsoPumpHistoryHandler
import info.nightscout.androidaps.plugins.pump.ypsopump.handlers.YpsoPumpStatusHandler
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpConst
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.TimeChangeType
import info.nightscout.androidaps.utils.alertDialogs.OKDialog
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.rx.AapsSchedulers
import info.nightscout.androidaps.utils.sharedPreferences.SP
import org.joda.time.DateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by andy on 14.01.2021.
 *
 * @author Andy Rozman (andy.rozman@gmail.com)
 */
@Singleton
class YpsopumpPumpPlugin @Inject constructor(
    injector: HasAndroidInjector,
    aapsLogger: AAPSLogger,
    rxBus: RxBus,
    context: Context,
    resourceHelper: ResourceHelper,
    activePlugin: ActivePlugin,
    sp: SP,
    commandQueue: CommandQueueProvider,
    fabricPrivacy: FabricPrivacy,
    val ypsopumpUtil: YpsoPumpUtil,
    val pumpStatus: YpsopumpPumpStatus,
    dateUtil: DateUtil,
    val pumpConnectionManager: YpsoPumpConnectionManager,
    aapsSchedulers: AapsSchedulers,
    pumpSync: PumpSync,
    pumpSyncStorage: PumpSyncStorage,
    var ypsoPumpStatusHandler: YpsoPumpStatusHandler,
    var ypsoPumpHistoryHandler: YpsoPumpHistoryHandler
) : PumpPluginAbstract(PluginDescription() //
    .mainType(PluginType.PUMP) //
    .fragmentClass(YpsoPumpFragment::class.java.name) //
    .pluginIcon(R.drawable.ic_ypsopump128)
    .pluginName(R.string.ypsopump_name) //
    .shortName(R.string.ypsopump_name_short) //
    .preferencesId(R.xml.pref_ypsopump)
    .description(R.string.description_pump_ypsopump),  //
    PumpType.YPSOPUMP,
    injector, resourceHelper, aapsLogger, commandQueue, rxBus, activePlugin, sp, context, fabricPrivacy, dateUtil, aapsSchedulers, pumpSync, pumpSyncStorage
), Pump {

    // variables for handling statuses and history
    private var firstRun = true
    private var isRefresh = false
    private val statusRefreshMap: MutableMap<YpsoPumpStatusRefreshType?, Long?> = mutableMapOf()
    private var isInitialized = false
    private var hasTimeDateOrTimeZoneChanged = false
    private var driverMode = YpsoDriverMode.Faked // TODO when implementation fully done, default should be automatic

    override fun onStart() {
        aapsLogger.debug(LTag.PUMP, model().model + " started.")
        super.onStart()
    }

    override fun updatePreferenceSummary(pref: Preference) {
        super.updatePreferenceSummary(pref)
        if (pref.key == resourceHelper.gs(R.string.key_ypsopump_address)) {
            val value: String? = sp.getStringOrNull(R.string.key_ypsopump_address, null)
            pref.summary = value ?: resourceHelper.gs(R.string.not_set_short)
        }
    }

    private val logPrefix: String
        get() = "YpsopumpPumpPlugin::"

    override fun initPumpStatusData() {
        pumpStatus.lastConnection = sp.getLong(YpsoPumpConst.Statistics.LastGoodPumpCommunicationTime, 0L)
        pumpStatus.lastDataTime = pumpStatus.lastConnection
        pumpStatus.previousConnection = pumpStatus.lastConnection
        aapsLogger.debug(LTag.PUMP, "initPumpStatusData: " + pumpStatus)

        // this is only thing that can change, by being configured
        // TODO pumpDescription.maxTempAbsolute = (pumpStatus.maxBasal != null) ? pumpStatus.maxBasal : 35.0d;
        aapsLogger.debug(LTag.PUMP, "pumpDescription: " + this.pumpDescription)

        pumpStatus.pumpDescription = this.pumpDescription

        // set first YpsoPump Pump Start
        if (!sp.contains(YpsoPumpConst.Statistics.FirstPumpStart)) {
            sp.putLong(YpsoPumpConst.Statistics.FirstPumpStart, System.currentTimeMillis())
        }
    }

    override fun onStartScheduledPumpActions() {

        disposable.add(rxBus
            .toObservable(EventPreferenceChange::class.java)
            .observeOn(aapsSchedulers.io)
            .subscribe({ event: EventPreferenceChange ->
                if (event.isChanged(resourceHelper, YpsoPumpConst.Prefs.PumpSerial)) {
                    ypsoPumpStatusHandler.switchPumpData()
                    resetStatusState()
                }
            }) { throwable: Throwable? -> fabricPrivacy.logException(throwable!!) })

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

        // val serialNumber = sp.getLong(YpsoPumpConst.Prefs.PumpSerial, 0L)
        //
        // sp.remove(YpsoPumpConst.Prefs.PumpSerial)
        //
        // if (serialNumber > 0) {
        //     sp.putString(YpsoPumpConst.Prefs.PumpSerial, "" + serialNumber)
        // }

        ypsoPumpStatusHandler.loadYpsoPumpStatusList()
    }

    override val serviceClass: Class<*>?
        get() = null

    override val pumpStatusData: PumpStatus
        get() = pumpStatus


    override fun isInitialized(): Boolean {
        aapsLogger.debug(LTag.PUMP, "isInitialized - true (always)")
        return true
        //return pumpStatus.ypsopumpFirmware != null;
    }

    override fun isSuspended(): Boolean {
        aapsLogger.debug(LTag.PUMP, "isSuspended - false (always)")
        return pumpStatus.pumpRunningState == PumpRunningState.Suspended
    }

    override fun isConnected(): Boolean {
        val driverStatus = ypsopumpUtil.driverStatus
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "isConnected - " + driverStatus.name)
        return driverStatus == PumpDriverState.Ready || driverStatus == PumpDriverState.ExecutingCommand
    }

    override fun isConnecting(): Boolean {
        val driverStatus = ypsopumpUtil.driverStatus
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "isConnecting - " + driverStatus.name)
        //return pumpState == PumpDriverState.Connecting;
        return driverStatus == PumpDriverState.Connecting || driverStatus == PumpDriverState.Connected || driverStatus == PumpDriverState.EncryptCommunication
    }

    override fun connect(reason: String) {
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "connect (reason=$reason).")
        pumpConnectionManager.connectToPump()
    }

    override fun disconnect(reason: String) {
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "disconnect (reason=$reason).")
        pumpConnectionManager.disconnectFromPump()
    }

    override fun stopConnecting() {
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "stopConnecting [PumpPluginAbstract] - default (empty) implementation.")
    }

    override fun isHandshakeInProgress(): Boolean {
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "isHandshakeInProgress - " + ypsopumpUtil.driverStatus.name)
        return ypsopumpUtil.driverStatus === PumpDriverState.Connecting
    }

    override fun finishHandshaking() {
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "finishHandshaking [PumpPluginAbstract] - default (empty) implementation.")
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
        val statusRefresh = workWithStatusRefresh(StatusRefreshAction.GetData, null,
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
        val refreshTypesNeededToReschedule: MutableSet<YpsoPumpStatusRefreshType?> = HashSet()
        for ((key, value) in statusRefresh!!) {
            if (value!! > 0 && System.currentTimeMillis() > value) {
                when (key) {
                    YpsoPumpStatusRefreshType.PumpHistory                                               -> {
                        readPumpHistory()
                    }

                    YpsoPumpStatusRefreshType.PumpTime                                                  -> {
                        if (checkTimeAndOptionallySetTime()) {
                            resetDisplay = true
                        }
                        refreshTypesNeededToReschedule.add(key)
                        resetTime = true
                    }

                    YpsoPumpStatusRefreshType.BatteryStatus,
                    YpsoPumpStatusRefreshType.RemainingInsulin                                          -> {
                        pumpConnectionManager.getRemainingInsulin()
                        refreshTypesNeededToReschedule.add(key)
                        resetDisplay = true
                        resetTime = true
                    }
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

    private fun doWeHaveAnyStatusNeededRefereshing(statusRefresh: Map<YpsoPumpStatusRefreshType?, Long?>?): Boolean {
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

        if (pumpStatus.serialNumber == null) {
            aapsLogger.info(LTag.PUMP, logPrefix + "initializePump - serial Number is null, initialization stopped.")
            return false
        }

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
        ypsoPumpHistoryHandler.readCurrentStatusOfPump()

        // TODO readPumpHistory
        //readPumpHistory()

        // TODO remaining insulin (>50 = 4h; 50-20 = 1h; 15m) - pumpConnectionManager.remainingInsulin (command not available)
        //scheduleNextRefresh(YpsoPumpStatusRefreshType.RemainingInsulin, 10)

        // TODO remaining power (1h) - pumpConnectionManager.batteryStatus (command not available)
        //scheduleNextRefresh(YpsoPumpStatusRefreshType.BatteryStatus, 20)

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

        var commandResponse: CommandResponse? = null


        Thread(Runnable {
            SystemClock.sleep(500)
            //ErrorHelperActivity.runAlarm(context, resourceHelper.gs(R.string.medtronic_cmd_cancel_bolus_not_supported), resourceHelper.gs(R.string.medtronic_warning), R.raw.boluserror)

            OKDialog.showConfirmation(context = context,
                title = resourceHelper.gs(R.string.ypsopump_cmd_exec_title_set_profile),
                message = resourceHelper.gs(R.string.ypsopump_cmd_exec_desc_set_profile, "Unknown"),
                { _: DialogInterface?, _: Int ->
                    //commandResponse = CommandResponse.builder().success(true).build()
                    aapsLogger.debug(LTag.PUMP, "UI Test -> Success")
                }, { _: DialogInterface?, _: Int ->
                    //commandResponse = CommandResponse.builder().success(true).build()
                    aapsLogger.debug(LTag.PUMP, "UI Test -> Cancel")
                })

        }).start()

        //Looper.prepare()

        aapsLogger.debug(LTag.PUMP, "After UI Test: $commandResponse")
    }

    private fun setDriverMode() {
        if (pumpStatus.isFirmwareSet) {
            if (pumpStatus.ypsopumpFirmware.isClosedLoopPossible) {
                // TODO
            } else {
                this.driverMode = YpsoDriverMode.ForcedOpenLoop
            }
        } else
            this.driverMode = YpsoDriverMode.Faked
    }

    // private val basalProfiles: Unit
    //     get() {
    //         if (!pumpConnectionManager.basalProfile) {
    //             pumpConnectionManager.basalProfile
    //         }
    //     }

    var profile: Profile? = null

    // TODO not implemented
    override fun isThisProfileSet(profile: Profile): Boolean {
        aapsLogger.debug(LTag.PUMP, "isThisProfileSet ")

        this.profile = profile  // TODO remove this later

        if (driverMode == YpsoDriverMode.Faked) {
            aapsLogger.debug(LTag.PUMP, "  Faked mode: returning true")
            return true
        } else {
            if (pumpStatus.basalsByHour == null) {
                aapsLogger.debug(LTag.PUMP, "  Pump Profile:     null, returning true")
                return true
            } else {
                val profileAsString = ProfileUtil.getBasalProfilesDisplayableAsStringOfArray(profile, PumpType.YPSOPUMP)
                val profileDriver = ProfileUtil.getProfilesByHourToString(pumpStatus.basalsByHour)

                aapsLogger.debug(LTag.PUMP, "AAPS Profile:     $profileAsString")
                aapsLogger.debug(LTag.PUMP, "Pump Profile:     $profileDriver")

                return profileAsString.equals(profileDriver)
            }
        }

//        if (this.profile!=null && this.profile.equals(profile))
//        this.profile = profile;

//        aapsLogger.debug(LTag.PUMP, "isThisProfileSet: basalInitalized=" + pumpStatus.basalProfileStatus);
//
//        if (!isInitialized)
//            return true;
//
//        if (pumpStatus.basalProfileStatus == BasalProfileStatus.NotInitialized) {
//            // this shouldn't happen, but if there was problem we try again
//            getBasalProfiles();
//            return isProfileSame(profile);
//        } else if (pumpStatus.basalProfileStatus == BasalProfileStatus.ProfileChanged) {
//            return false;
//        }
//
//        return (pumpStatus.basalProfileStatus != BasalProfileStatus.ProfileOK) || isProfileSame(profile);
    }

    //    private boolean isProfileSame(Profile profile) {
    //
    //        boolean invalid = false;
    //        Double[] basalsByHour = medtronicPumpStatus.basalsByHour;
    //
    //        aapsLogger.debug(LTag.PUMP, "Current Basals (h):   "
    //                + (basalsByHour == null ? "null" : BasalProfile.getProfilesByHourToString(basalsByHour)));
    //
    //        // int index = 0;
    //
    //        if (basalsByHour == null)
    //            return true; // we don't want to set profile again, unless we are sure
    //
    //        StringBuilder stringBuilder = new StringBuilder("Requested Basals (h): ");
    //
    //        for (Profile.ProfileValue basalValue : profile.getBasalValues()) {
    //
    //            double basalValueValue = pumpDescription.pumpType.determineCorrectBasalSize(basalValue.value);
    //
    //            int hour = basalValue.timeAsSeconds / (60 * 60);
    //
    //            if (!MedtronicUtil.isSame(basalsByHour[hour], basalValueValue)) {
    //                invalid = true;
    //            }
    //
    //            stringBuilder.append(String.format(Locale.ENGLISH, "%.3f", basalValueValue));
    //            stringBuilder.append(" ");
    //        }
    //
    //        aapsLogger.debug(LTag.PUMP, stringBuilder.toString());
    //
    //        if (!invalid) {
    //            aapsLogger.debug(LTag.PUMP, "Basal profile is same as AAPS one.");
    //        } else {
    //            aapsLogger.debug(LTag.PUMP, "Basal profile on Pump is different than the AAPS one.");
    //        }
    //
    //        return (!invalid);
    //    }

    override fun lastDataTime(): Long {
        return if (pumpStatus.lastConnection != 0L) {
            pumpStatus.lastConnection
        } else System.currentTimeMillis()
    }

    // TODO fix this

    //return pumpStatus.getBasalProfileForHour();
    override val baseBasalRate: Double
        get() =// TODO fix this
            if (profile == null) {
                aapsLogger.debug("Profile is not set: ")
                pumpStatus.baseBasalRate = 0.0
                0.0
            } else {
                val gc = GregorianCalendar()
                val time = gc[Calendar.HOUR_OF_DAY] * 60 * 60 + (gc[Calendar.MINUTE] * 60).toLong()
                val basal = profile!!.getBasal(time)
                aapsLogger.debug("Basal for this hour is: $basal")
                pumpStatus.baseBasalRate = basal
                basal
            }

    //return pumpStatus.getBasalProfileForHour();

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

        val clock = pumpConnectionManager.getTime()

        if (clock != null) {
            // TODO check if this works, migneed to use LocalDateTime...
            pumpStatus.pumpTime = PumpTimeDifferenceDto(DateTime.now(), clock.toLocalDateTime())
            val diff = Math.abs(pumpStatus.pumpTime!!.timeDifference)

            if (diff > 60) {
                aapsLogger.error(LTag.PUMP, "Time difference between phone and pump is more than 60s ($diff)")

                if (!pumpStatus.ypsopumpFirmware.isClosedLoopPossible) {
                    val notification = Notification(Notification.PUMP_PHONE_TIME_DIFF_TOO_BIG, resourceHelper.gs(R.string.time_difference_too_big, 60, diff), Notification.INFO, 60)
                    rxBus.send(EventNewNotification(notification))
                } else {

                    // TODO setNewTime, different notification
                    val time = pumpConnectionManager.setTime()

                    if (time != null) {
                        pumpStatus.pumpTime = PumpTimeDifferenceDto(DateTime.now(), time.toLocalDateTime())

                        val newTimeDiff = Math.abs(pumpStatus.pumpTime!!.timeDifference)

                        if (newTimeDiff < 60) {
                            val notification = Notification(Notification.INSIGHT_DATE_TIME_UPDATED,
                                resourceHelper.gs(R.string.pump_time_updated),
                                Notification.INFO, 60)
                            rxBus.send(EventNewNotification(notification))
                        } else {
                            aapsLogger.error(LTag.PUMP, "Setting time on pump failed.")
                        }
                    } else {
                        aapsLogger.error(LTag.PUMP, "Setting time on pump failed.")
                    }
                }
            }
        }

        setRefreshButtonEnabled(false)
        scheduleNextRefresh(YpsoPumpStatusRefreshType.PumpTime, 0)

        return true
    }

    // TODO progress bar
    override fun deliverBolus(detailedBolusInfo: DetailedBolusInfo): PumpEnactResult {
        aapsLogger.info(LTag.PUMP, logPrefix + "deliverBolus - " + BolusDeliveryType.DeliveryPrepared)
        return if (detailedBolusInfo.insulin > pumpStatus.reservoirRemainingUnits) {
            PumpEnactResult(injector) //
                .success(false) //
                .enacted(false) //
                .comment(resourceHelper.gs(R.string.ypsopump_cmd_bolus_could_not_be_delivered_no_insulin,
                    pumpStatus.reservoirRemainingUnits,
                    detailedBolusInfo.insulin))
        } else try {
            setRefreshButtonEnabled(false)

            val commandResponse = pumpConnectionManager.deliverBolus(detailedBolusInfo)

            if (commandResponse!=null && commandResponse.isSuccess) {
                val now = System.currentTimeMillis()

                detailedBolusInfo.bolusTimestamp = now

                // we subtract insulin, exact amount will be visible with next remainingInsulin update.
                //pumpStatus.reservoirRemainingUnits -= detailedBolusInfo.insulin

                incrementStatistics(if (detailedBolusInfo.bolusType == DetailedBolusInfo.BolusType.SMB)
                    YpsoPumpConst.Statistics.SMBBoluses
                else
                    YpsoPumpConst.Statistics.StandardBoluses)

                if (detailedBolusInfo.carbs > 0.0) {
                    pumpSync.syncCarbsWithTimestamp(
                        timestamp = now,
                        amount = detailedBolusInfo.carbs,
                        pumpId = null,
                        pumpType = PumpType.YPSOPUMP,
                        pumpSerial = serialNumber()
                    )
                }

                readPumpHistoryAfterAction(bolusInfo = detailedBolusInfo)

                PumpEnactResult(injector).success(true) //
                    .enacted(true) //
                    .bolusDelivered(detailedBolusInfo.insulin) //
                    .carbsDelivered(detailedBolusInfo.carbs)
            } else {
                PumpEnactResult(injector) //
                    .success(false) //
                    .enacted(false) //
                    .comment(resourceHelper.gs(R.string.ypsopump_cmd_bolus_could_not_be_delivered))
            }
        } finally {
            finishAction("Bolus")
        }

//        bolusDeliveryType = BolusDeliveryType.DeliveryPrepared;
//
//        if (isPumpNotReachable()) {
//            aapsLogger.debug(LTag.PUMP, "MedtronicPumpPlugin::deliverBolus - Pump Unreachable.");
//            return setNotReachable(true, false);
//        }
//
//        ypsopumpUtil.dismissNotification(MedtronicNotificationType.PumpUnreachable, rxBus);
//
//        if (bolusDeliveryType == BolusDeliveryType.CancelDelivery) {
//            // LOG.debug("MedtronicPumpPlugin::deliverBolus - Delivery Canceled.");
//            return setNotReachable(true, true);
//        }
//
//        // LOG.debug("MedtronicPumpPlugin::deliverBolus - Starting wait period.");
//
//        int sleepTime = sp.getInt(YpsoPumpConst.Prefs.BolusDelay, 10) * 1000;
//
//        SystemClock.sleep(sleepTime);
//
//        if (bolusDeliveryType == BolusDeliveryType.CancelDelivery) {
//            // LOG.debug("MedtronicPumpPlugin::deliverBolus - Delivery Canceled, before wait period.");
//            return setNotReachable(true, true);
//        }
//
//        // LOG.debug("MedtronicPumpPlugin::deliverBolus - End wait period. Start delivery");
//
//        try {
//
//            bolusDeliveryType = BolusDeliveryType.Delivering;
//
//            // LOG.debug("MedtronicPumpPlugin::deliverBolus - Start delivery");
//
//            MedtronicUITask responseTask = ypsoPumpService.getMedtronicUIComm().executeCommand(MedtronicCommandType.SetBolus,
//                    detailedBolusInfo.insulin);
//
//            Boolean response = (Boolean) responseTask.returnData;
//
//            setRefreshButtonEnabled(true);
//
//            // LOG.debug("MedtronicPumpPlugin::deliverBolus - Response: {}", response);
//
//            if (response) {
//
//                if (bolusDeliveryType == BolusDeliveryType.CancelDelivery) {
//                    // LOG.debug("MedtronicPumpPlugin::deliverBolus - Delivery Canceled after Bolus started.");
//
//                    new Thread(() -> {
//                        // Looper.prepare();
//                        // LOG.debug("MedtronicPumpPlugin::deliverBolus - Show dialog - before");
//                        SystemClock.sleep(2000);
//                        // LOG.debug("MedtronicPumpPlugin::deliverBolus - Show dialog. Context: "
//                        // + MainApp.instance().getApplicationContext());
//
//                        Intent i = new Intent(context, ErrorHelperActivity.class);
//                        i.putExtra("soundid", R.raw.boluserror);
//                        i.putExtra("status", getResourceHelper().gs(R.string.medtronic_cmd_cancel_bolus_not_supported));
//                        i.putExtra("title", getResourceHelper().gs(R.string.medtronic_warning));
//                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        context.startActivity(i);
//
//                    }).start();
//                }
//
//                long now = System.currentTimeMillis();
//
//                detailedBolusInfo.date = now;
//                detailedBolusInfo.deliverAt = now; // not sure about that one
//
//                activePlugin.getActiveTreatments().addToHistoryTreatment(detailedBolusInfo, true);
//
//                // we subtract insulin, exact amount will be visible with next remainingInsulin update.
//                pumpStatus.reservoirRemainingUnits -= detailedBolusInfo.insulin;
//
//                incrementStatistics(detailedBolusInfo.isSMB ? YpsoPumpConst.Statistics.SMBBoluses
//                        : YpsoPumpConst.Statistics.StandardBoluses);
//
//
//                // calculate time for bolus and set driver to busy for that time
//                int bolusTime = (int) (detailedBolusInfo.insulin * 42.0d);
//                long time = now + (bolusTime * 1000);
//
//                this.busyTimestamps.add(time);
//                //setEnableCustomAction(MedtronicCustomActionType.ClearBolusBlock, true);
//
//                return new PumpEnactResult(getInjector()).success(true) //
//                        .enacted(true) //
//                        .bolusDelivered(detailedBolusInfo.insulin) //
//                        .carbsDelivered(detailedBolusInfo.carbs);
//
//            } else {
//                return new PumpEnactResult(getInjector()) //
//                        .success(bolusDeliveryType == BolusDeliveryType.CancelDelivery) //
//                        .enacted(false) //
//                        .comment(getResourceHelper().gs(R.string.medtronic_cmd_bolus_could_not_be_delivered));
//            }
//
//        } finally {
//            finishAction("Bolus");
//            this.bolusDeliveryType = BolusDeliveryType.Idle;
//        }
//        return new PumpEnactResult(getInjector()).success(true) //
//                .enacted(true) //
//                .bolusDelivered(detailedBolusInfo.insulin) //
//                .carbsDelivered(detailedBolusInfo.carbs);
    }

    override fun stopBolusDelivering() {
        bolusDeliveryType = BolusDeliveryType.CancelDelivery
        // TODO if there is command
        if (isLoggingEnabled) aapsLogger.warn(LTag.PUMP, "MedtronicPumpPlugin::deliverBolus - Stop Bolus Delivery.")
    }

    private val isLoggingEnabled: Boolean
        get() = true

    private fun incrementStatistics(statsKey: String) {
        var currentCount: Long = sp.getLong(statsKey, 0L)
        currentCount++
        sp.putLong(statsKey, currentCount)
    }

    // if enforceNew===true current temp basal is canceled and new TBR set (duration is prolonged),
    // if false and the same rate is requested enacted=false and success=true is returned and TBR is not changed
    override fun setTempBasalPercent(percent: Int, durationInMinutes: Int, profile: Profile,
                                     enforceNew: Boolean, tbrType: TemporaryBasalType): PumpEnactResult {
        setRefreshButtonEnabled(false)
        return try {
            aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent: rate: " + percent + "%, duration=" + durationInMinutes)

            // read current TBR
            val tbrCurrent = readTBR()
            if (tbrCurrent == null) {
                aapsLogger.warn(LTag.PUMP, logPrefix + "setTempBasalPercent - Could not read current TBR, canceling operation.")
                return PumpEnactResult(injector).success(false).enacted(false)
                    .comment(resourceHelper.gs(R.string.ypsopump_cmd_cant_read_tbr))
            } else {
                aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent: Current Basal: duration: " + tbrCurrent.durationMinutes + " min, rate=" + tbrCurrent.insulinRate)
            }
            if (!enforceNew) {
                if (ypsopumpUtil.isSame(tbrCurrent.insulinRate, percent)) {
                    var sameRate = true
                    if (ypsopumpUtil.isSame(0.0, percent) && durationInMinutes > 0) {
                        // if rate is 0.0 and duration>0 then the rate is not the same
                        sameRate = false
                    }
                    if (sameRate) {
                        aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent - No enforceNew and same rate. Exiting.")
                        return PumpEnactResult(injector).success(true).enacted(false)
                    }
                }
                // if not the same rate, we cancel and start new
            }

            // if TBR is running we will cancel it.
            if (tbrCurrent.insulinRate != 0.0 && tbrCurrent.durationMinutes > 0) {
                aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent - TBR running - so canceling it.")

                // CANCEL
                val commandResponse = pumpConnectionManager.cancelTemporaryBasal()
                if (commandResponse!=null && commandResponse.isSuccess) {
                    aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent - Current TBR cancelled.")
                } else {
                    aapsLogger.error(logPrefix + "setTempBasalPercent - Cancel TBR failed.")
                    return PumpEnactResult(injector).success(false).enacted(false)
                        .comment(resourceHelper.gs(R.string.ypsopump_cmd_cant_cancel_tbr_stop_op))
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

                incrementStatistics(YpsoPumpConst.Statistics.TBRsSet)
                PumpEnactResult(injector).success(true).enacted(true) //
                    .percent(percent).duration(durationInMinutes)
            } else {
                PumpEnactResult(injector).success(false).enacted(false) //
                    .comment(resourceHelper.gs(R.string.ypsopump_cmd_tbr_could_not_be_delivered))
            }
        } finally {
            finishAction("TBR")
        }
    }

    // TODO setTempBasalAbsolute
    override fun setTempBasalAbsolute(absoluteRate: Double, durationInMinutes: Int, profile: Profile,
                                      enforceNew: Boolean, tbrType: TemporaryBasalType): PumpEnactResult {
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
        aapsLogger.warn(LTag.PUMP, logPrefix + "readPumpHistory N/A.")

        ypsoPumpHistoryHandler.getPumpHistory()

        //        pumpConnectionManager.getPumpHistory()

        scheduleNextRefresh(YpsoPumpStatusRefreshType.PumpHistory)



        // read last event history
        // read 10 minutes in past

//        if (isLoggingEnabled())
//            aapsLogger.warn(LTag.PUMP, getLogPrefix() + "readPumpHistory WIP.");
//
//        readPumpHistoryLogic();
//
//        scheduleNextRefresh(YpsoPumpStatusRefreshType.PumpHistory);
//
//        if (medtronicHistoryData.hasRelevantConfigurationChanged()) {
//            scheduleNextRefresh(YpsoPumpStatusRefreshType.Configuration, -1);
//        }
//
//        if (medtronicHistoryData.hasPumpTimeChanged()) {
//            scheduleNextRefresh(YpsoPumpStatusRefreshType.PumpTime, -1);
//        }
//
//        if (this.ypsopumpPumpStatus.basalProfileStatus != BasalProfileStatus.NotInitialized
//                && medtronicHistoryData.hasBasalProfileChanged()) {
//            medtronicHistoryData.processLastBasalProfileChange(pumpDescription.pumpType, ypsopumpPumpStatus);
//        }
//
//        PumpDriverState previousState = this.pumpState;
//
//        if (medtronicHistoryData.isPumpSuspended()) {
//            this.pumpState = PumpDriverState.Suspended;
//            aapsLogger.debug(LTag.PUMP, getLogPrefix() + "isPumpSuspended: true");
//        } else {
//            if (previousState == PumpDriverState.Suspended) {
//                this.pumpState = PumpDriverState.Ready;
//            }
//            aapsLogger.debug(LTag.PUMP, getLogPrefix() + "isPumpSuspended: false");
//        }
//
//        medtronicHistoryData.processNewHistoryData();
//
//        this.medtronicHistoryData.finalizeNewHistoryRecords();
        // this.medtronicHistoryData.setLastHistoryRecordTime(this.lastPumpHistoryEntry.atechDateTime);
    }

    //    private void readPumpHistoryLogic() {
    //
    //        boolean debugHistory = false;
    //
    //        LocalDateTime targetDate = null;
    //
    //        if (lastPumpHistoryEntry == null) {
    //
    //            if (debugHistory)
    //                aapsLogger.debug(LTag.PUMP, getLogPrefix() + "readPumpHistoryLogic(): lastPumpHistoryEntry: null");
    //
    //            Long lastPumpHistoryEntryTime = getLastPumpEntryTime();
    //
    //            LocalDateTime timeMinus36h = new LocalDateTime();
    //            timeMinus36h = timeMinus36h.minusHours(36);
    //            medtronicHistoryData.setIsInInit(true);
    //
    //            if (lastPumpHistoryEntryTime == 0L) {
    //                if (debugHistory)
    //                    aapsLogger.debug(LTag.PUMP, getLogPrefix() + "readPumpHistoryLogic(): lastPumpHistoryEntryTime: 0L - targetDate: "
    //                            + targetDate);
    //                targetDate = timeMinus36h;
    //            } else {
    //                // LocalDateTime lastHistoryRecordTime = DateTimeUtil.toLocalDateTime(lastPumpHistoryEntryTime);
    //
    //                if (debugHistory)
    //                    aapsLogger.debug(LTag.PUMP, getLogPrefix() + "readPumpHistoryLogic(): lastPumpHistoryEntryTime: " + lastPumpHistoryEntryTime + " - targetDate: " + targetDate);
    //
    //                medtronicHistoryData.setLastHistoryRecordTime(lastPumpHistoryEntryTime);
    //
    //                LocalDateTime lastHistoryRecordTime = DateTimeUtil.toLocalDateTime(lastPumpHistoryEntryTime);
    //
    //                lastHistoryRecordTime = lastHistoryRecordTime.minusHours(12); // we get last 12 hours of history to
    //                // determine pump state
    //                // (we don't process that data), we process only
    //
    //                if (timeMinus36h.isAfter(lastHistoryRecordTime)) {
    //                    targetDate = timeMinus36h;
    //                }
    //
    //                targetDate = (timeMinus36h.isAfter(lastHistoryRecordTime) ? timeMinus36h : lastHistoryRecordTime);
    //
    //                if (debugHistory)
    //                    aapsLogger.debug(LTag.PUMP, getLogPrefix() + "readPumpHistoryLogic(): targetDate: " + targetDate);
    //            }
    //        } else {
    //            if (debugHistory)
    //                aapsLogger.debug(LTag.PUMP, getLogPrefix() + "readPumpHistoryLogic(): lastPumpHistoryEntry: not null - " + ypsopumpUtil.gsonInstance.toJson(lastPumpHistoryEntry));
    //            medtronicHistoryData.setIsInInit(false);
    //            // medtronicHistoryData.setLastHistoryRecordTime(lastPumpHistoryEntry.atechDateTime);
    //
    //            // targetDate = lastPumpHistoryEntry.atechDateTime;
    //        }
    //
    //        //aapsLogger.debug(LTag.PUMP, "HST: Target Date: " + targetDate);
    //
    //        MedtronicUITask responseTask2 = ypsoPumpService.getMedtronicUIComm().executeCommand(MedtronicCommandType.GetHistoryData,
    //                lastPumpHistoryEntry, targetDate);
    //
    //        if (debugHistory)
    //            aapsLogger.debug(LTag.PUMP, "HST: After task");
    //
    //        PumpHistoryResult historyResult = (PumpHistoryResult) responseTask2.returnData;
    //
    //        if (debugHistory)
    //            aapsLogger.debug(LTag.PUMP, "HST: History Result: " + historyResult.toString());
    //
    //        PumpHistoryEntry latestEntry = historyResult.getLatestEntry();
    //
    //        if (debugHistory)
    //            aapsLogger.debug(LTag.PUMP, getLogPrefix() + "Last entry: " + latestEntry);
    //
    //        if (latestEntry == null) // no new history to read
    //            return;
    //
    //        this.lastPumpHistoryEntry = latestEntry;
    //        sp.putLong(YpsoPumpConst.Statistics.LastPumpHistoryEntry, latestEntry.atechDateTime);
    //
    //        if (debugHistory)
    //            aapsLogger.debug(LTag.PUMP, "HST: History: valid=" + historyResult.validEntries.size() + ", unprocessed=" + historyResult.unprocessedEntries.size());
    //
    //        this.medtronicHistoryData.addNewHistory(historyResult);
    //        this.medtronicHistoryData.filterNewEntries();
    //
    //        // determine if first run, if yes detrmine how much of update do we need
    //        // first run:
    //        // get last hiostory entry, if not there download 1.5 days of data
    //        // - there: check if last entry is older than 1.5 days
    //        // - yes: download 1.5 days
    //        // - no: download with last entry
    //        // - not there: download 1.5 days
    //        //
    //        // upload all new entries to NightScout (TBR, Bolus)
    //        // determine pump status
    //        //
    //        // save last entry
    //        //
    //        // not first run:
    //        // update to last entry
    //        // - save
    //        // - determine pump status
    //    }
    //    private Long getLastPumpEntryTime() {
    //        Long lastPumpEntryTime = sp.getLong(YpsoPumpConst.Statistics.LastPumpHistoryEntry, 0L);
    //
    //        try {
    //            LocalDateTime localDateTime = DateTimeUtil.toLocalDateTime(lastPumpEntryTime);
    //
    //            if (localDateTime.getYear() != (new GregorianCalendar().get(Calendar.YEAR))) {
    //                aapsLogger.warn(LTag.PUMP, "Saved LastPumpHistoryEntry was invalid. Year was not the same.");
    //                return 0L;
    //            }
    //
    //            return lastPumpEntryTime;
    //
    //        } catch (Exception ex) {
    //            aapsLogger.warn(LTag.PUMP, "Saved LastPumpHistoryEntry was invalid.");
    //            return 0L;
    //        }
    //
    //    }

    private fun readPumpHistoryAfterAction(bolusInfo: DetailedBolusInfo? = null,
                                           tempBasalInfo: TempBasalPair? = null,
                                           profile: Profile? = null) {
        // TODO
        if (true)
            return
        aapsLogger.warn(LTag.PUMP, logPrefix + "readPumpHistoryAfterAction N/A.")
        ypsoPumpHistoryHandler.getLastEventAndSendItToPumpSync(
            bolusInfo = bolusInfo,
            tempBasalInfo = tempBasalInfo,
            profile = profile)
    }

    private fun scheduleNextRefresh(refreshType: YpsoPumpStatusRefreshType?, additionalTimeInMinutes: Int = 0) {
        when (refreshType) {
            YpsoPumpStatusRefreshType.RemainingInsulin -> {
                val remaining = pumpStatus.reservoirRemainingUnits
                val min: Int
                min = if (remaining > 50) 4 * 60 else if (remaining > 20) 60 else 15
                workWithStatusRefresh(StatusRefreshAction.Add, refreshType, getTimeInFutureFromMinutes(min))
            }

            YpsoPumpStatusRefreshType.PumpTime,
                //YpsoPumpStatusRefreshType.Configuration,
            YpsoPumpStatusRefreshType.BatteryStatus,
            YpsoPumpStatusRefreshType.PumpHistory      -> {
                workWithStatusRefresh(StatusRefreshAction.Add, refreshType,
                    getTimeInFutureFromMinutes(refreshType.refreshTime + additionalTimeInMinutes))
            }
        }
    }

    private enum class StatusRefreshAction {
        Add,  //
        GetData
    }

    @Synchronized
    private fun workWithStatusRefresh(action: StatusRefreshAction,  //
                                      statusRefreshType: YpsoPumpStatusRefreshType?,  //
                                      time: Long?): Map<YpsoPumpStatusRefreshType?, Long?>? {
        return when (action) {
            StatusRefreshAction.Add     -> {
                statusRefreshMap[statusRefreshType] = time
                null
            }

            StatusRefreshAction.GetData -> {
                HashMap(statusRefreshMap)
            }

            else                        -> null
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
        return if (temporaryBasalResponse!=null && temporaryBasalResponse.isSuccess) {
            val tbr = temporaryBasalResponse.tempBasalPair as TempBasalPair

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
                    return PumpEnactResult(injector).success(true).enacted(false)
                }
            } else {
                aapsLogger.warn(LTag.PUMP, logPrefix + "cancelTempBasal - Could not read currect TBR, canceling operation.")
                finishAction("TBR")
                return PumpEnactResult(injector).success(false).enacted(false)
                    .comment(resourceHelper.gs(R.string.ypsopump_cmd_cant_read_tbr))
            }
            val commandResponse = pumpConnectionManager.cancelTemporaryBasal()
            if (commandResponse!!.isSuccess) {
                aapsLogger.info(LTag.PUMP, logPrefix + "cancelTempBasal - Cancel TBR successful.")

                readPumpHistoryAfterAction(tempBasalInfo = TempBasalPair(0.0, true, 0))

                PumpEnactResult(injector).success(true).enacted(true) //
                    .isTempCancel(true)
            } else {
                aapsLogger.info(LTag.PUMP, logPrefix + "cancelTempBasal - Cancel TBR failed.")
                PumpEnactResult(injector).success(false).enacted(false) //
                    .comment(resourceHelper.gs(R.string.ypsopump_cmd_cant_cancel_tbr))
            }
        } finally {
            finishAction("TBR")
        }
    }

    override fun serialNumber(): String {
        return if (pumpStatus.serialNumber == null) "" else pumpStatus.serialNumber!!.toString()
    }

    //    @NotNull @Override
    //    public Constraint<Boolean> isClosedLoopAllowed(@NotNull Constraint<Boolean> value) {
    ////        if(pumpStatus.ypsopumpFirmware==null) {
    ////            return value.set(aapsLogger,false, resourceHelper.gs(R.string.some_reason), this);
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
    //                    .comment(getResourceHelper().gs(R.string.ypsopump_cmd_basal_profile_could_not_be_set));
    //        }

    override fun setNewBasalProfile(profile: Profile): PumpEnactResult {
        aapsLogger.info(LTag.PUMP, logPrefix + "setNewBasalProfile")
        return try {
            setRefreshButtonEnabled(false)
            var commandResponse: CommandResponse? = null
            var driverModeCurrent = driverMode

            if (driverModeCurrent == YpsoDriverMode.Faked) {
                pumpConnectionManager.sendFakeCommand(YpsoPumpCommandType.SetBasalProfile)
            } else if (driverModeCurrent == YpsoDriverMode.ForcedOpenLoop) {

                aapsLogger.error(LTag.PUMP, "Forced Open Loop: setNewBasalProfile")
                // Looper.prepare()
                // OKDialog.showConfirmation(context = context,
                //     title = resourceHelper.gs(R.string.ypsopump_cmd_exec_title_set_profile),
                //     message = resourceHelper.gs(R.string.ypsopump_cmd_exec_desc_set_profile, "Unknown"),
                //     { _: DialogInterface?, _: Int ->
                //         commandResponse = CommandResponse.builder().success(true).build()
                //     }, null)

                // TODO
                commandResponse = pumpConnectionManager.setBasalProfile(profile)

            } else {
                commandResponse = pumpConnectionManager.setBasalProfile(profile)
            }

            readPumpHistoryAfterAction(profile = profile)

//        String profileInvalid = isProfileValid(basalProfile);
//
//        if (profileInvalid != null) {
//            return new PumpEnactResult(getInjector()) //
//                    .success(false) //
//                    .enacted(false) //
//                    .comment(getResourceHelper().gs(R.string.medtronic_cmd_set_profile_pattern_overflow, profileInvalid));
//        }

            // TODO setProfile 1.5 (Ui) and 1.6 send command
            //val commandResponse = pumpConnectionManager.setBasalProfile(profile)
            aapsLogger.info(LTag.PUMP, logPrefix + "Basal Profile was set: " + commandResponse)
            if (commandResponse != null && commandResponse.isSuccess) {
                PumpEnactResult(injector).success(true).enacted(true)
            } else {
                PumpEnactResult(injector).success(false).enacted(false) //
                    .comment(resourceHelper.gs(R.string.ypsopump_cmd_basal_profile_could_not_be_set))
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
    override fun timezoneOrDSTChanged(timeChangeType: TimeChangeType) {
        aapsLogger.warn(LTag.PUMP, logPrefix + "Time or TimeZone changed. ")
        hasTimeDateOrTimeZoneChanged = true
    }

    override fun generateTempId(dataObject: Any?): Long {
        return 0L
    }

    init {
        displayConnectionMessages = true
    }
}