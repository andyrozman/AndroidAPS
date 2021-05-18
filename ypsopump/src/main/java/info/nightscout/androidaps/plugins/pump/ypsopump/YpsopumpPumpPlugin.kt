package info.nightscout.androidaps.plugins.pump.ypsopump

import android.content.Context
import android.os.SystemClock
import androidx.preference.Preference
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.data.PumpEnactResult
import info.nightscout.androidaps.db.Source
import info.nightscout.androidaps.db.TemporaryBasal
import info.nightscout.androidaps.events.EventRefreshOverview
import info.nightscout.androidaps.interfaces.*
import info.nightscout.androidaps.interfaces.PumpSync.TemporaryBasalType
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.plugins.pump.common.PumpPluginAbstract
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType
import info.nightscout.androidaps.plugins.pump.common.defs.PumpUpdateFragmentType
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.events.EventPumpFragmentValuesChanged
import info.nightscout.androidaps.plugins.pump.common.events.EventRefreshButtonState
import info.nightscout.androidaps.plugins.pump.common.sync.PumpSyncStorage
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpConnectionManager
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpStatusRefreshType
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpConfigurationChanged
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpConst
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.TimeChangeType
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.androidaps.utils.rx.AapsSchedulers
import info.nightscout.androidaps.utils.sharedPreferences.SP
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
    rxBus: RxBusWrapper,
    context: Context,
    resourceHelper: ResourceHelper,
    activePlugin: ActivePlugin,
    sp: SP,
    commandQueue: CommandQueueProvider,
    fabricPrivacy: FabricPrivacy,
    val ypsopumpUtil: YpsoPumpUtil,
    private val ypsopumpPumpStatus: YpsopumpPumpStatus,
    dateUtil: DateUtil,
    connectionManager: YpsoPumpConnectionManager,
    aapsSchedulers: AapsSchedulers,
    pumpSync: PumpSync,
    pumpSyncStorage: PumpSyncStorage
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

    private val pumpStatus: YpsopumpPumpStatus
    private val pumpConnectionManager: YpsoPumpConnectionManager

    // variables for handling statuses and history
    private var firstRun = true
    private var isRefresh = false
    private val statusRefreshMap: MutableMap<YpsoPumpStatusRefreshType?, Long?> = HashMap()
    private var isInitialized = false
    private var hasTimeDateOrTimeZoneChanged = false

    override fun onStart() {
        aapsLogger.debug(LTag.PUMP, deviceID() + " started.")
        super.onStart()
    }

    override fun onStartCustomActions() {}

    override fun updatePreferenceSummary(pref: Preference) {
        super.updatePreferenceSummary(pref)
        if (pref.key == resourceHelper.gs(R.string.key_ypsopump_address)) {
            val value: String? = sp.getStringOrNull(R.string.key_ypsopump_address, null)
            pref.summary = value ?: resourceHelper.gs(R.string.not_set_short)
        }
    }

    private val logPrefix: String
        private get() = "YpsopumpPumpPlugin::"

    override fun initPumpStatusData() {
        pumpStatus.lastConnection = sp.getLong(YpsoPumpConst.Statistics.LastGoodPumpCommunicationTime, 0L)
        pumpStatus.lastDataTime = pumpStatus.lastConnection
        pumpStatus.previousConnection = pumpStatus.lastConnection
        aapsLogger.debug(LTag.PUMP, "initPumpStatusData: " + pumpStatus)

        // this is only thing that can change, by being configured
        //pumpDescription.maxTempAbsolute = (pumpStatus.maxBasal != null) ? pumpStatus.maxBasal : 35.0d;
        aapsLogger.debug(LTag.PUMP, "pumpDescription: " + this.pumpDescription)

        // set first YpsoPump Pump Start
        if (!sp.contains(YpsoPumpConst.Statistics.FirstPumpStart)) {
            sp.putLong(YpsoPumpConst.Statistics.FirstPumpStart, System.currentTimeMillis())
        }
    }

    fun onStartScheduledPumpActions() {

        // TODO fix me
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
        Thread(Runnable {
            do {
                SystemClock.sleep(60000)
                if (this.isInitialized) {
                    val statusRefresh = workWithStatusRefresh(
                        StatusRefreshAction.GetData, null, null)
                    if (doWeHaveAnyStatusNeededRefereshing(statusRefresh)) {
                        if (!commandQueue.statusInQueue()) {
                            commandQueue.readStatus("Scheduled Status Refresh", null)
                        }
                    }
                }
            } while (serviceRunning)
        }).start()
    }

    override val serviceClass: Class<*>?
        get() = null

    override val pumpStatusData: PumpStatus
        get() = pumpStatus

    override fun deviceID(): String {
        return "YpsoPump"
    }

    override fun isInitialized(): Boolean {
        aapsLogger.debug(LTag.PUMP, "isInitialized - ")
        return true
        //return pumpStatus.ypsopumpFirmware != null;
    }

    override fun isSuspended(): Boolean {
        aapsLogger.debug(LTag.PUMP, "isSuspended - ")
        return false
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

    // Pump Plugin
    // TODO
    //    @Override
    //    public boolean hasService() {
    //        return false;
    //    }
    //    @Override
    //    public long getLastConnectionTimeMillis() {
    //        return ypsopumpPumpStatus.lastConnection;
    //    }
    //
    //    @Override
    //    public void setLastCommunicationToNow() {
    //        ypsopumpPumpStatus.setLastCommunicationToNow();
    //    }
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

                    YpsoPumpStatusRefreshType.BatteryStatus, YpsoPumpStatusRefreshType.RemainingInsulin -> {
                        pumpConnectionManager.remainingInsulin
                        refreshTypesNeededToReschedule.add(key)
                        resetDisplay = true
                        resetTime = true
                    }

                    YpsoPumpStatusRefreshType.Configuration                                             -> {
                        pumpConnectionManager.configuration // TODO this might not be needed
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
        if (resetTime) pumpStatus.setLastCommunicationToNow()
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
        setRefreshButtonEnabled(false)
        pumpState = PumpDriverState.Connected

        // time (1h)
        checkTimeAndOptionallySetTime()
        readPumpHistory()

        // remaining insulin (>50 = 4h; 50-20 = 1h; 15m)
        pumpConnectionManager.remainingInsulin
        scheduleNextRefresh(YpsoPumpStatusRefreshType.RemainingInsulin, 10)

        // remaining power (1h)
        pumpConnectionManager.batteryStatus
        scheduleNextRefresh(YpsoPumpStatusRefreshType.BatteryStatus, 20)

        // configuration (once and then if history shows config changes)
        pumpConnectionManager.configuration

        // read profile (once, later its controlled by isThisProfileSet method)
        basalProfiles
        pumpStatus.setLastCommunicationToNow()
        setRefreshButtonEnabled(true)
        if (!isRefresh) {
            pumpState = PumpDriverState.Initialized
        }
        isInitialized = true
        firstRun = false
        return true
    }

    private val basalProfiles: Unit
        private get() {
            if (!pumpConnectionManager.basalProfile) {
                pumpConnectionManager.basalProfile
            }
        }

    var profile: Profile? = null

    // TODO not implemented
    override fun isThisProfileSet(profile: Profile): Boolean {
        aapsLogger.debug(LTag.PUMP, "isThisProfileSet: ")

        // TODO implement
        this.profile = profile
        return true

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
        rxBus.send(EventPumpConfigurationChanged())
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

        // TODO implement
        return false

        //setRefreshButtonEnabled(false);

//        if (isPumpNotReachable()) {
//            aapsLogger.debug(LTag.PUMP, "MedtronicPumpPlugin::checkTimeAndOptionallySetTime - Pump Unreachable.");
//            setRefreshButtonEnabled(true);
//            return;
//        }
//
//        ypsopumpUtil.dismissNotification(MedtronicNotificationType.PumpUnreachable, rxBus);
//
//        ypsoPumpService.getMedtronicUIComm().executeCommand(MedtronicCommandType.GetRealTimeClock);
//
//        ClockDTO clock = ypsopumpUtil.getPumpTime();
//
//        if (clock == null) { // retry
//            ypsoPumpService.getMedtronicUIComm().executeCommand(MedtronicCommandType.GetRealTimeClock);
//
//            clock = ypsopumpUtil.getPumpTime();
//        }
//
//        if (clock == null)
//            return;
//
//        int timeDiff = Math.abs(clock.timeDifference);
//
//        if (timeDiff > 20) {
//
//            if ((clock.localDeviceTime.getYear() <= 2015) || (timeDiff <= 24 * 60 * 60)) {
//
//                aapsLogger.info(LTag.PUMP, "MedtronicPumpPlugin::checkTimeAndOptionallySetTime - Time difference is {} s. Set time on pump.", timeDiff);
//
//                ypsoPumpService.getMedtronicUIComm().executeCommand(MedtronicCommandType.SetRealTimeClock);
//
//                if (clock.timeDifference == 0) {
//                    Notification notification = new Notification(Notification.INSIGHT_DATE_TIME_UPDATED, getResourceHelper().gs(R.string.pump_time_updated), Notification.INFO, 60);
//                    rxBus.send(new EventNewNotification(notification));
//                }
//            } else {
//                if ((clock.localDeviceTime.getYear() > 2015)) {
//                    aapsLogger.error("MedtronicPumpPlugin::checkTimeAndOptionallySetTime - Time difference over 24h requested [diff={} s]. Doing nothing.", timeDiff);
//                    ypsopumpUtil.sendNotification(MedtronicNotificationType.TimeChangeOver24h, getResourceHelper(), rxBus);
//                }
//            }
//
//        } else {
//            aapsLogger.info(LTag.PUMP, "MedtronicPumpPlugin::checkTimeAndOptionallySetTime - Time difference is {} s. Do nothing.", timeDiff);
//        }
//
//        scheduleNextRefresh(YpsoPumpStatusRefreshType.PumpTime, 0);
    }

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

//                detailedBolusInfo.date = now;
//                detailedBolusInfo.deliverAt = now; // not sure about that one

                // TODO pumpSync
                activePlugin.activeTreatments.addToHistoryTreatment(detailedBolusInfo, true)

                // we subtract insulin, exact amount will be visible with next remainingInsulin update.
                pumpStatus.reservoirRemainingUnits -= detailedBolusInfo.insulin

//                incrementStatistics(detailedBolusInfo.isSMB ? YpsoPumpConst.Statistics.SMBBoluses
//                        : YpsoPumpConst.Statistics.StandardBoluses);
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
        if (isLoggingEnabled) aapsLogger.warn(LTag.PUMP, "MedtronicPumpPlugin::deliverBolus - Stop Bolus Delivery.")
    }

    private val isLoggingEnabled: Boolean
        private get() = true

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
                val tempStart: TemporaryBasal = TemporaryBasal(injector) //
                    .date(System.currentTimeMillis()) //
                    .duration(durationInMinutes) //
                    .percent(percent) //
                    .source(Source.USER)

                // TODO pumpSync
//                activePlugin.getActiveTreatments().addToHistoryTempBasal(tempStart);
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

    override fun setTempBasalAbsolute(absoluteRate: Double, durationInMinutes: Int, profile: Profile,
                                      enforceNew: Boolean, tbrType: TemporaryBasalType): PumpEnactResult {
        aapsLogger.debug(LTag.PUMP, "setTempBasalAbsolute called with a rate of $absoluteRate for $durationInMinutes min.")
        val unroundedPercentage = java.lang.Double.valueOf(absoluteRate / baseBasalRate * 100).toInt()
        return this.setTempBasalPercent(unroundedPercentage, durationInMinutes, profile, enforceNew, tbrType)
    }

    //@Override
    fun setTempBasalPercent_xxx(percent: Int, durationInMinutes: Int, profile: Profile?,
                                enforceNew: Boolean): PumpEnactResult {
        aapsLogger.warn(LTag.PUMP, logPrefix + "setTempBasalPercent: [percent=" + percent + ", duration=" + durationInMinutes + ", enforceNew=" + enforceNew + "]")
        return try {

            // TODO fixme look at old absolute to see what is happening, but it depends on what pump supports
            val commandResponse = pumpConnectionManager.setTemporaryBasal(percent, durationInMinutes)
            if (commandResponse!=null && commandResponse.isSuccess) {
                pumpStatus.tempBasalStart = System.currentTimeMillis()
                pumpStatus.tempBasalPercent = percent
                pumpStatus.tempBasalDuration = durationInMinutes
                val tempStart: TemporaryBasal = TemporaryBasal(injector) //
                    .date(System.currentTimeMillis()) //
                    .duration(durationInMinutes) //
                    .percent(percent) //
                    .source(Source.USER)
                // TODO
                //activePlugin.activeTreatments.addToHistoryTempBasal(tempStart)
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

//        if (percent == 0) {
//            return setTempBasalAbsolute(0.0d, durationInMinutes, profile, enforceNew);
//        } else {
//            double absoluteValue = profile.getBasal() * (percent / 100.0d);
//            absoluteValue = pumpDescription.pumpType.determineCorrectBasalSize(absoluteValue);
//            aapsLogger.warn(LTag.PUMP, "setTempBasalPercent [MedtronicPumpPlugin] - You are trying to use setTempBasalPercent with percent other then 0% (" + percent + "). This will start setTempBasalAbsolute, with calculated value (" + absoluteValue + "). Result might not be 100% correct.");
//            return setTempBasalAbsolute(absoluteValue, durationInMinutes, profile, enforceNew);
//        }
    }

    private fun finishAction(overviewKey: String?) {
        if (overviewKey != null) rxBus.send(EventRefreshOverview(overviewKey, false))
        triggerUIChange()
        setRefreshButtonEnabled(true)
    }

    private fun readPumpHistory() {
        aapsLogger.warn(LTag.PUMP, logPrefix + "readPumpHistory N/A.")
        pumpConnectionManager.pumpHistory

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
    private fun scheduleNextRefresh(refreshType: YpsoPumpStatusRefreshType?, additionalTimeInMinutes: Int = 0) {
        when (refreshType) {
            YpsoPumpStatusRefreshType.RemainingInsulin                                                                                                                  -> {
                val remaining = pumpStatus.reservoirRemainingUnits
                val min: Int
                min = if (remaining > 50) 4 * 60 else if (remaining > 20) 60 else 15
                workWithStatusRefresh(StatusRefreshAction.Add, refreshType, getTimeInFutureFromMinutes(min))
            }

            YpsoPumpStatusRefreshType.PumpTime, YpsoPumpStatusRefreshType.Configuration, YpsoPumpStatusRefreshType.BatteryStatus, YpsoPumpStatusRefreshType.PumpHistory -> {
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
        val temporaryBasalResponse = pumpConnectionManager.temporaryBasal
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
                val tempBasal: TemporaryBasal = TemporaryBasal(injector) //
                    .date(System.currentTimeMillis()) //
                    .duration(0) //
                    .source(Source.USER)

                // TODO pumpSync
//                activePlugin.getActiveTreatments().addToHistoryTempBasal(tempBasal);
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
        return pumpStatus.serialNumber!!
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

//        String profileInvalid = isProfileValid(basalProfile);
//
//        if (profileInvalid != null) {
//            return new PumpEnactResult(getInjector()) //
//                    .success(false) //
//                    .enacted(false) //
//                    .comment(getResourceHelper().gs(R.string.medtronic_cmd_set_profile_pattern_overflow, profileInvalid));
//        }
            val commandResponse = pumpConnectionManager.setBasalProfile(profile)
            aapsLogger.info(LTag.PUMP, logPrefix + "Basal Profile was set: " + commandResponse)
            if (commandResponse!=null && commandResponse.isSuccess) {
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
    override fun timezoneOrDSTChanged(changeType: TimeChangeType) {
        aapsLogger.warn(LTag.PUMP, logPrefix + "Time or TimeZone changed. ")
        hasTimeDateOrTimeZoneChanged = true
    }

    override fun generateTempId(dataObject: Any?): Long {
        TODO("Not yet implemented")
    }

    init {
        this.sp = sp
        pumpStatus = ypsopumpPumpStatus
        pumpConnectionManager = connectionManager
        displayConnectionMessages = true
    }
}