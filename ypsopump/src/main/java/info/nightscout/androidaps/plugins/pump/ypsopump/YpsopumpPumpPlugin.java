package info.nightscout.androidaps.plugins.pump.ypsopump;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.db.Source;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.events.EventRefreshOverview;
import info.nightscout.androidaps.interfaces.ActivePluginProvider;
import info.nightscout.androidaps.interfaces.CommandQueueProvider;
import info.nightscout.androidaps.interfaces.PluginDescription;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.logging.LTag;
import info.nightscout.androidaps.plugins.bus.RxBusWrapper;
import info.nightscout.androidaps.plugins.pump.common.PumpPluginAbstract;
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus;
import info.nightscout.androidaps.plugins.pump.common.data.TempBasalPair;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType;
import info.nightscout.androidaps.plugins.pump.common.events.EventRefreshButtonState;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpConnectionManager;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.TemporaryBasalResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoDriverStatus;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpStatusRefreshType;
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus;
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpConfigurationChanged;
import info.nightscout.androidaps.plugins.pump.ypsopump.service.YpsoPumpService;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpConst;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.utils.FabricPrivacy;
import info.nightscout.androidaps.utils.TimeChangeType;
import info.nightscout.androidaps.utils.resources.ResourceHelper;
import info.nightscout.androidaps.utils.sharedPreferences.SP;


/**
 * Created by andy on 14.01.2021.
 *
 * @author Andy Rozman (andy.rozman@gmail.com)
 */
@Singleton
public class YpsopumpPumpPlugin extends PumpPluginAbstract implements PumpInterface {

    private final SP sp;
    private final YpsoPumpUtil ypsopumpUtil;
    private final YpsopumpPumpStatus pumpStatus;
    private final YpsoPumpConnectionManager pumpConnectionManager;

    // variables for handling statuses and history
    private boolean firstRun = true;
    private boolean isRefresh = false;
    private final Map<YpsoPumpStatusRefreshType, Long> statusRefreshMap = new HashMap<>();
    private boolean isInitialized = false;
    //private PumpHistoryEntry lastPumpHistoryEntry;

    public static boolean isBusy = false;
    private final List<Long> busyTimestamps = new ArrayList<>();
    private boolean hasTimeDateOrTimeZoneChanged = false;


    @Inject
    public YpsopumpPumpPlugin(
            HasAndroidInjector injector,
            AAPSLogger aapsLogger,
            RxBusWrapper rxBus,
            Context context,
            ResourceHelper resourceHelper,
            ActivePluginProvider activePlugin,
            SP sp,
            CommandQueueProvider commandQueue,
            FabricPrivacy fabricPrivacy,
            YpsoPumpUtil ypsopumpUtil,
            YpsopumpPumpStatus ypsopumpPumpStatus,
            DateUtil dateUtil,
            YpsoPumpConnectionManager connectionManager
//            MedtronicHistoryData medtronicHistoryData,
    ) {

        super(new PluginDescription() //
                        .mainType(PluginType.PUMP) //
                        .fragmentClass(YpsoPumpFragment.class.getName()) //
                        .pluginIcon(R.drawable.ic_ypsopump128)
                        .pluginName(R.string.ypsopump_name) //
                        .shortName(R.string.ypsopump_name_short) //
                        .preferencesId(R.xml.pref_ypsopump)
                        .description(R.string.description_pump_ypsopump), //
                PumpType.YpsoPump,
                injector, resourceHelper, aapsLogger, commandQueue, rxBus, activePlugin, sp, context, fabricPrivacy, dateUtil
        );

        this.ypsopumpUtil = ypsopumpUtil;
        this.sp = sp;
        this.pumpStatus = ypsopumpPumpStatus;
        this.pumpConnectionManager = connectionManager;

        displayConnectionMessages = true;
    }


    @Override
    protected void onStart() {
        aapsLogger.debug(LTag.PUMP, this.deviceID() + " started.");
        super.onStart();
    }

    @Override
    public void updatePreferenceSummary(@NotNull Preference pref) {
        super.updatePreferenceSummary(pref);

        if (pref.getKey().equals(getResourceHelper().gs(R.string.key_ypsopump_address))) {
            String value = sp.getStringOrNull(R.string.key_ypsopump_address, null);
            pref.setSummary(value == null ? getResourceHelper().gs(R.string.not_set_short) : value);
        }
    }

    private String getLogPrefix() {
        return "YpsopumpPumpPlugin::";
    }

    @Override
    public void initPumpStatusData() {

        pumpStatus.lastConnection = sp.getLong(YpsoPumpConst.Statistics.LastGoodPumpCommunicationTime, 0L);
        pumpStatus.lastDataTime = pumpStatus.lastConnection;
        pumpStatus.previousConnection = pumpStatus.lastConnection;

        aapsLogger.debug(LTag.PUMP, "initPumpStatusData: " + this.pumpStatus);

        // this is only thing that can change, by being configured
        //pumpDescription.maxTempAbsolute = (pumpStatus.maxBasal != null) ? pumpStatus.maxBasal : 35.0d;

        aapsLogger.debug(LTag.PUMP, "pumpDescription: " + this.pumpDescription);

        // set first YpsoPump Pump Start
        if (!sp.contains(YpsoPumpConst.Statistics.FirstPumpStart)) {
            sp.putLong(YpsoPumpConst.Statistics.FirstPumpStart, System.currentTimeMillis());
        }
    }


    public void onStartCustomActions() {

        // check status every minute (if any status needs refresh we send readStatus command)
        new Thread(() -> {

            do {
                SystemClock.sleep(60000);

                if (this.isInitialized) {

                    Map<YpsoPumpStatusRefreshType, Long> statusRefresh = workWithStatusRefresh(
                            StatusRefreshAction.GetData, null, null);

                    if (doWeHaveAnyStatusNeededRefereshing(statusRefresh)) {
                        if (!getCommandQueue().statusInQueue()) {
                            getCommandQueue().readStatus("Scheduled Status Refresh", null);
                        }
                    }

                    clearBusyQueue();
                }

            } while (serviceRunning);

        }).start();
    }


    public Class getServiceClass() {
        return YpsoPumpService.class;
    }

    @Override
    public PumpStatus getPumpStatusData() {
        return pumpStatus;
    }

    @Override
    public String deviceID() {
        return "YpsoPump";
    }

    @Override
    public boolean isInitialized() {
        aapsLogger.debug(LTag.PUMP, "isInitialized - ");

        return true;
        //return pumpStatus.ypsopumpFirmware != null;
    }

    public boolean isSuspended() {
        aapsLogger.debug(LTag.PUMP, "isSuspended - ");

        return false;
    }


    public boolean isConnected() {
        YpsoDriverStatus driverStatus = ypsopumpUtil.getDriverStatus();
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "isConnected - " + driverStatus.name());
        return driverStatus == YpsoDriverStatus.Connected || driverStatus == YpsoDriverStatus.ExecutingCommand;
    }


    public boolean isConnecting() {
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "isConnecting - " + ypsopumpUtil.getDriverStatus().name());
        //return pumpState == PumpDriverState.Connecting;
        return ypsopumpUtil.getDriverStatus() == YpsoDriverStatus.Connecting;
    }


    public void connect(String reason) {
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "connect (reason=" + reason + ").");

        pumpConnectionManager.connectToPump();
    }


    public void disconnect(String reason) {
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "disconnect (reason=" + reason + ").");

        pumpConnectionManager.disconnectFromPump();
    }


    public void stopConnecting() {
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "stopConnecting [PumpPluginAbstract] - default (empty) implementation.");
    }


    @Override
    public boolean isHandshakeInProgress() {
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "isHandshakeInProgress - " + ypsopumpUtil.getDriverStatus().name());
        return ypsopumpUtil.getDriverStatus() == YpsoDriverStatus.Connecting;
    }


    @Override
    public void finishHandshaking() {
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "finishHandshaking [PumpPluginAbstract] - default (empty) implementation.");
    }


    @Override
    public boolean isFakingTempsByExtendedBoluses() {
        return false;
    }


    @Override
    public boolean canHandleDST() {
        return false;
    }


    // Pump Plugin

    private boolean isServiceSet() {
        return true;
    }


    @Override
    public boolean hasService() {
        return false;
    }


//    @Override
//    public long getLastConnectionTimeMillis() {
//        return ypsopumpPumpStatus.lastConnection;
//    }
//
//    @Override
//    public void setLastCommunicationToNow() {
//        ypsopumpPumpStatus.setLastCommunicationToNow();
//    }


    //@Override
    public void setBusy(boolean busy) {
        isBusy = busy;
    }

    @Override
    public boolean isBusy() {
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, getLogPrefix() + "isBusy");

        if (isServiceSet()) {

            if (isBusy)
                return true;

            if (busyTimestamps.size() > 0) {

                clearBusyQueue();

                return busyTimestamps.size() > 0;
            }
        }

        return false;
    }


    private synchronized void clearBusyQueue() {

        if (busyTimestamps.size() == 0) {
            return;
        }

        Set<Long> deleteFromQueue = new HashSet<>();

        for (Long busyTimestamp : busyTimestamps) {

            if (System.currentTimeMillis() > busyTimestamp) {
                deleteFromQueue.add(busyTimestamp);
            }
        }

        if (deleteFromQueue.size() == busyTimestamps.size()) {
            busyTimestamps.clear();
            //setEnableCustomAction(MedtronicCustomActionType.ClearBolusBlock, false);
        }

        if (deleteFromQueue.size() > 0) {
            busyTimestamps.removeAll(deleteFromQueue);
        }

    }


    @Override
    public void getPumpStatus(String reason) {
        boolean needRefresh = true;

        if (firstRun) {
            needRefresh = initializePump(!isRefresh);
        } else {
            refreshAnyStatusThatNeedsToBeRefreshed();
        }

//        if (needRefresh)
//            rxBus.send(new EventMedtronicPumpValuesChanged());
    }


    void resetStatusState() {
        firstRun = true;
        isRefresh = true;
    }


    private void refreshAnyStatusThatNeedsToBeRefreshed() {

        Map<YpsoPumpStatusRefreshType, Long> statusRefresh = workWithStatusRefresh(StatusRefreshAction.GetData, null,
                null);

        if (!doWeHaveAnyStatusNeededRefereshing(statusRefresh)) {
            return;
        }

        boolean resetTime = false;

        if (hasTimeDateOrTimeZoneChanged) {
            checkTimeAndOptionallySetTime();

            // read time if changed, set new time
            hasTimeDateOrTimeZoneChanged = false;
        }


        // execute
        Set<YpsoPumpStatusRefreshType> refreshTypesNeededToReschedule = new HashSet<>();

        for (Map.Entry<YpsoPumpStatusRefreshType, Long> refreshType : statusRefresh.entrySet()) {

            if (refreshType.getValue() > 0 && System.currentTimeMillis() > refreshType.getValue()) {

                switch (refreshType.getKey()) {
                    case PumpHistory: {
                        readPumpHistory();
                    }
                    break;

                    case PumpTime: {
                        checkTimeAndOptionallySetTime();
                        refreshTypesNeededToReschedule.add(refreshType.getKey());
                        resetTime = true;
                    }
                    break;

                    case BatteryStatus:
                    case RemainingInsulin: {
                        pumpConnectionManager.getRemainingInsulin();
                        refreshTypesNeededToReschedule.add(refreshType.getKey());
                        resetTime = true;
                    }
                    break;

                    case Configuration: {
                        pumpConnectionManager.getConfiguration();
                        resetTime = true;
                    }
                    break;
                }
            }

            // reschedule
            for (YpsoPumpStatusRefreshType refreshType2 : refreshTypesNeededToReschedule) {
                scheduleNextRefresh(refreshType2);
            }

        }

        if (resetTime)
            pumpStatus.setLastCommunicationToNow();

    }


    private boolean doWeHaveAnyStatusNeededRefereshing(Map<YpsoPumpStatusRefreshType, Long> statusRefresh) {

        for (Map.Entry<YpsoPumpStatusRefreshType, Long> refreshType : statusRefresh.entrySet()) {

            if (refreshType.getValue() > 0 && System.currentTimeMillis() > refreshType.getValue()) {
                return true;
            }
        }

        return hasTimeDateOrTimeZoneChanged;
    }


    private void setRefreshButtonEnabled(boolean enabled) {
        rxBus.send(new EventRefreshButtonState(enabled));
    }

    // TODO not implement
    private boolean initializePump(boolean realInit) {

        if (isInitialized)
            return false;

        aapsLogger.info(LTag.PUMP, getLogPrefix() + "initializePump - start");

        setRefreshButtonEnabled(false);

        this.pumpState = PumpDriverState.Connected;

        // time (1h)
        checkTimeAndOptionallySetTime();

        readPumpHistory();

        // remaining insulin (>50 = 4h; 50-20 = 1h; 15m)
        pumpConnectionManager.getRemainingInsulin();
        scheduleNextRefresh(YpsoPumpStatusRefreshType.RemainingInsulin, 10);

        // remaining power (1h)
        pumpConnectionManager.getBatteryStatus();
        scheduleNextRefresh(YpsoPumpStatusRefreshType.BatteryStatus, 20);

        // configuration (once and then if history shows config changes)
        pumpConnectionManager.getConfiguration();

        // read profile (once, later its controlled by isThisProfileSet method)
        getBasalProfiles();

        pumpStatus.setLastCommunicationToNow();
        setRefreshButtonEnabled(true);

        if (!isRefresh) {
            pumpState = PumpDriverState.Initialized;
        }


        isInitialized = true;
        // this.pumpState = PumpDriverState.Initialized;

        this.firstRun = false;

        return true;
    }

    // TODO not implemented
    private void getBasalProfiles() {

//        MedtronicUITask medtronicUITask = ypsoPumpService.getMedtronicUIComm().executeCommand(MedtronicCommandType.GetBasalProfileSTD);
//
//        if (medtronicUITask.getResponseType() == MedtronicUIResponseType.Error) {
//            ypsoPumpService.getMedtronicUIComm().executeCommand(MedtronicCommandType.GetBasalProfileSTD);
//        }
    }

    Profile profile;

    // TODO not implemented
    @Override
    public boolean isThisProfileSet(Profile profile) {
        aapsLogger.debug(LTag.PUMP, "isThisProfileSet: ");

        this.profile = profile;

//        aapsLogger.debug(LTag.PUMP, "isThisProfileSet: basalInitalized=" + medtronicPumpStatus.basalProfileStatus);
//
//        if (!isInitialized)
//            return true;
//
//        if (medtronicPumpStatus.basalProfileStatus == BasalProfileStatus.NotInitialized) {
//            // this shouldn't happen, but if there was problem we try again
//            getBasalProfiles();
//            return isProfileSame(profile);
//        } else if (medtronicPumpStatus.basalProfileStatus == BasalProfileStatus.ProfileChanged) {
//            return false;
//        }
//
//        return (medtronicPumpStatus.basalProfileStatus != BasalProfileStatus.ProfileOK) || isProfileSame(profile);
        return true;
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


    @Override
    public long lastDataTime() {

        if (pumpStatus.lastConnection != 0) {
            return pumpStatus.lastConnection;
        }

        return System.currentTimeMillis();
    }


    @Override
    public double getBaseBasalRate() {
        // TODO fix this
        if (profile == null) {
            aapsLogger.debug("Profile is not set: ");
            pumpStatus.baseBasalRate = 0.0d;

            return 0.0d;
        } else {
            GregorianCalendar gc = new GregorianCalendar();

            long time = (gc.get(Calendar.HOUR_OF_DAY) * 60 * 60) + (gc.get(Calendar.MINUTE) * 60);

            double basal = profile.getBasal(time);
            aapsLogger.debug("Basal for this hour is: " + basal);
            pumpStatus.baseBasalRate = basal;

            return basal;
        }


        //return pumpStatus.getBasalProfileForHour();
    }


    @Override
    public double getReservoirLevel() {
        return pumpStatus.reservoirRemainingUnits;
    }


    @Override
    public int getBatteryLevel() {
        return pumpStatus.batteryRemaining;
    }

    protected void triggerUIChange() {
        rxBus.send(new EventPumpConfigurationChanged());
    }

    private BolusDeliveryType bolusDeliveryType = BolusDeliveryType.Idle;

    private enum BolusDeliveryType {
        Idle, //
        DeliveryPrepared, //
        Delivering, //
        CancelDelivery
    }


    private void checkTimeAndOptionallySetTime() {

        aapsLogger.info(LTag.PUMP, getLogPrefix() + "checkTimeAndOptionallySetTime - Start");

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


    @NonNull
    protected PumpEnactResult deliverBolus(final DetailedBolusInfo detailedBolusInfo) {

        aapsLogger.info(LTag.PUMP, getLogPrefix() + "deliverBolus - " + BolusDeliveryType.DeliveryPrepared);

        if (detailedBolusInfo.insulin > pumpStatus.reservoirRemainingUnits) {
            return new PumpEnactResult(getInjector()) //
                    .success(false) //
                    .enacted(false) //
                    .comment(getResourceHelper().gs(R.string.ypsopump_cmd_bolus_could_not_be_delivered_no_insulin,
                            pumpStatus.reservoirRemainingUnits,
                            detailedBolusInfo.insulin));
        }

        try {

            setRefreshButtonEnabled(false);

            CommandResponse commandResponse = pumpConnectionManager.deliverBolus(detailedBolusInfo);

            if (commandResponse.isSuccess()) {

                long now = System.currentTimeMillis();

                detailedBolusInfo.date = now;
                detailedBolusInfo.deliverAt = now; // not sure about that one

                activePlugin.getActiveTreatments().addToHistoryTreatment(detailedBolusInfo, true);

                // we subtract insulin, exact amount will be visible with next remainingInsulin update.
                pumpStatus.reservoirRemainingUnits -= detailedBolusInfo.insulin;

                incrementStatistics(detailedBolusInfo.isSMB ? YpsoPumpConst.Statistics.SMBBoluses
                        : YpsoPumpConst.Statistics.StandardBoluses);

                return new PumpEnactResult(getInjector()).success(true) //
                        .enacted(true) //
                        .bolusDelivered(detailedBolusInfo.insulin) //
                        .carbsDelivered(detailedBolusInfo.carbs);
            } else {
                return new PumpEnactResult(getInjector()) //
                        .success(false) //
                        .enacted(false) //
                        .comment(getResourceHelper().gs(R.string.ypsopump_cmd_bolus_could_not_be_delivered));
            }

        } finally {
            finishAction("Bolus");
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


    public void stopBolusDelivering() {

        this.bolusDeliveryType = BolusDeliveryType.CancelDelivery;

        if (isLoggingEnabled())
            aapsLogger.warn(LTag.PUMP, "MedtronicPumpPlugin::deliverBolus - Stop Bolus Delivery.");
    }

    private boolean isLoggingEnabled() {
        return true;
    }


    private void incrementStatistics(String statsKey) {
        long currentCount = sp.getLong(statsKey, 0L);
        currentCount++;
        sp.putLong(statsKey, currentCount);
    }


    // if enforceNew===true current temp basal is canceled and new TBR set (duration is prolonged),
    // if false and the same rate is requested enacted=false and success=true is returned and TBR is not changed
    @NonNull
    @Override
    public PumpEnactResult setTempBasalPercent(Integer percent, Integer durationInMinutes, Profile profile,
                                               boolean enforceNew) {
        setRefreshButtonEnabled(false);

        try {

            aapsLogger.info(LTag.PUMP, getLogPrefix() + "setTempBasalPercent: rate: " + percent + "%, duration=" + durationInMinutes);

            // read current TBR
            TempBasalPair tbrCurrent = readTBR();

            if (tbrCurrent == null) {
                aapsLogger.warn(LTag.PUMP, getLogPrefix() + "setTempBasalPercent - Could not read current TBR, canceling operation.");
                return new PumpEnactResult(getInjector()).success(false).enacted(false)
                        .comment(getResourceHelper().gs(R.string.ypsopump_cmd_cant_read_tbr));
            } else {
                aapsLogger.info(LTag.PUMP, getLogPrefix() + "setTempBasalPercent: Current Basal: duration: " + tbrCurrent.getDurationMinutes() + " min, rate=" + tbrCurrent.getInsulinRate());
            }

            if (!enforceNew) {

                if (YpsoPumpUtil.isSame(tbrCurrent.getInsulinRate(), percent)) {

                    boolean sameRate = true;
                    if (YpsoPumpUtil.isSame(0.0d, percent) && durationInMinutes > 0) {
                        // if rate is 0.0 and duration>0 then the rate is not the same
                        sameRate = false;
                    }

                    if (sameRate) {
                        aapsLogger.info(LTag.PUMP, getLogPrefix() + "setTempBasalPercent - No enforceNew and same rate. Exiting.");
                        return new PumpEnactResult(getInjector()).success(true).enacted(false);
                    }
                }
                // if not the same rate, we cancel and start new
            }

            // if TBR is running we will cancel it.
            if (tbrCurrent.getInsulinRate() != 0.0f && tbrCurrent.getDurationMinutes() > 0) {
                aapsLogger.info(LTag.PUMP, getLogPrefix() + "setTempBasalPercent - TBR running - so canceling it.");

                // CANCEL

                CommandResponse commandResponse = pumpConnectionManager.cancelTemporaryBasal();

                if (commandResponse.isSuccess()) {
                    aapsLogger.info(LTag.PUMP, getLogPrefix() + "setTempBasalPercent - Current TBR cancelled.");
                } else {
                    aapsLogger.error(getLogPrefix() + "setTempBasalPercent - Cancel TBR failed.");

                    return new PumpEnactResult(getInjector()).success(false).enacted(false)
                            .comment(getResourceHelper().gs(R.string.ypsopump_cmd_cant_cancel_tbr_stop_op));
                }
            }

            // now start new TBR
            CommandResponse commandResponse = pumpConnectionManager.setTemporaryBasal(percent, durationInMinutes);

            aapsLogger.info(LTag.PUMP, getLogPrefix() + "setTempBasalPercent - setTBR. Response: " + commandResponse);

            if (commandResponse.isSuccess()) {

                pumpStatus.tempBasalStart = new Date();
                pumpStatus.tempBasalPercent = percent;
                pumpStatus.tempBasalDuration = durationInMinutes;
                pumpStatus.tempBasalEnd = System.currentTimeMillis() + (durationInMinutes * 60 * 1000);

                TemporaryBasal tempStart = new TemporaryBasal(getInjector()) //
                        .date(System.currentTimeMillis()) //
                        .duration(durationInMinutes) //
                        .percent(percent) //
                        .source(Source.USER);

                activePlugin.getActiveTreatments().addToHistoryTempBasal(tempStart);

                incrementStatistics(YpsoPumpConst.Statistics.TBRsSet);

                return new PumpEnactResult(getInjector()).success(true).enacted(true) //
                        .percent(percent).duration(durationInMinutes);

            } else {
                return new PumpEnactResult(getInjector()).success(false).enacted(false) //
                        .comment(getResourceHelper().gs(R.string.ypsopump_cmd_tbr_could_not_be_delivered));
            }
        } finally {
            finishAction("TBR");
        }

    }

    @NonNull @Override
    public PumpEnactResult setTempBasalAbsolute(Double absoluteRate, Integer durationInMinutes, Profile profile,
                                                boolean enforceNew) {
        getAapsLogger().debug(LTag.PUMP, "setTempBasalAbsolute called with a rate of " + absoluteRate + " for " + durationInMinutes + " min.");
        int unroundedPercentage = Double.valueOf(absoluteRate / getBaseBasalRate() * 100).intValue();

        return setTempBasalPercent(unroundedPercentage, durationInMinutes, profile, enforceNew);
    }


    @NonNull //@Override
    public PumpEnactResult setTempBasalPercent_xxx(Integer percent, Integer durationInMinutes, Profile profile,
                                                   boolean enforceNew) {

        aapsLogger.warn(LTag.PUMP, getLogPrefix() + "setTempBasalPercent: [percent=" + percent + ", duration=" + durationInMinutes + ", enforceNew=" + enforceNew + "]");

        try {

            // TODO fixme look at old absolute to see what is happening, but it depends on what pump supports
            CommandResponse commandResponse = pumpConnectionManager.setTemporaryBasal(percent, durationInMinutes);

            if (commandResponse.isSuccess()) {
                pumpStatus.tempBasalStart = new Date();
                pumpStatus.tempBasalPercent = percent;
                pumpStatus.tempBasalDuration = durationInMinutes;

                TemporaryBasal tempStart = new TemporaryBasal(getInjector()) //
                        .date(System.currentTimeMillis()) //
                        .duration(durationInMinutes) //
                        .percent(percent) //
                        .source(Source.USER);

                activePlugin.getActiveTreatments().addToHistoryTempBasal(tempStart);

                incrementStatistics(YpsoPumpConst.Statistics.TBRsSet);

                return new PumpEnactResult(getInjector()).success(true).enacted(true) //
                        .percent(percent).duration(durationInMinutes);

            } else {

                return new PumpEnactResult(getInjector()).success(false).enacted(false) //
                        .comment(getResourceHelper().gs(R.string.ypsopump_cmd_tbr_could_not_be_delivered));
            }
        } finally {
            finishAction("TBR");
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


    private void finishAction(String overviewKey) {

        if (overviewKey != null)
            rxBus.send(new EventRefreshOverview(overviewKey, false));

        triggerUIChange();

        setRefreshButtonEnabled(true);
    }


    private void readPumpHistory() {

        aapsLogger.warn(LTag.PUMP, getLogPrefix() + "readPumpHistory N/A.");

        pumpConnectionManager.getPumpHistory();


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


    private void scheduleNextRefresh(YpsoPumpStatusRefreshType refreshType) {
        scheduleNextRefresh(refreshType, 0);
    }


    private void scheduleNextRefresh(YpsoPumpStatusRefreshType refreshType, int additionalTimeInMinutes) {
        switch (refreshType) {

            case RemainingInsulin: {
                double remaining = pumpStatus.reservoirRemainingUnits;
                int min;
                if (remaining > 50)
                    min = 4 * 60;
                else if (remaining > 20)
                    min = 60;
                else
                    min = 15;

                workWithStatusRefresh(StatusRefreshAction.Add, refreshType, getTimeInFutureFromMinutes(min));
            }
            break;

            case PumpTime:
            case Configuration:
            case BatteryStatus:
            case PumpHistory: {
                workWithStatusRefresh(StatusRefreshAction.Add, refreshType,
                        getTimeInFutureFromMinutes(refreshType.getRefreshTime() + additionalTimeInMinutes));
            }
            break;
        }
    }

    private enum StatusRefreshAction {
        Add, //
        GetData
    }


    private synchronized Map<YpsoPumpStatusRefreshType, Long> workWithStatusRefresh(StatusRefreshAction action, //
                                                                                    YpsoPumpStatusRefreshType statusRefreshType, //
                                                                                    Long time) {

        switch (action) {

            case Add: {
                statusRefreshMap.put(statusRefreshType, time);
                return null;
            }

            case GetData: {
                return new HashMap<>(statusRefreshMap);
            }

            default:
                return null;

        }

    }


    private long getTimeInFutureFromMinutes(int minutes) {
        return System.currentTimeMillis() + getTimeInMs(minutes);
    }


    private long getTimeInMs(int minutes) {
        return minutes * 60 * 1000L;
    }


    private TempBasalPair readTBR() {

        TemporaryBasalResponse temporaryBasalResponse = pumpConnectionManager.getTemporaryBasal();

        if (temporaryBasalResponse.isSuccess()) {
            TempBasalPair tbr = (TempBasalPair) temporaryBasalResponse.getTempBasalPair();

            // we sometimes get rate returned even if TBR is no longer running
            if (tbr.getDurationMinutes() == 0) {
                tbr.setInsulinRate(0.0d);
            }

            return tbr;
        } else {
            return null;
        }
    }


    @NonNull @Override
    public PumpEnactResult cancelTempBasal(boolean enforceNew) {

        try {

            aapsLogger.info(LTag.PUMP, getLogPrefix() + "cancelTempBasal - started");

            setRefreshButtonEnabled(false);

            TempBasalPair tbrCurrent = readTBR();

            if (tbrCurrent != null) {
                if (tbrCurrent.getInsulinRate() == 0.0f && tbrCurrent.getDurationMinutes() == 0) {
                    aapsLogger.info(LTag.PUMP, getLogPrefix() + "cancelTempBasal - TBR already canceled.");
                    finishAction("TBR");
                    return new PumpEnactResult(getInjector()).success(true).enacted(false);
                }
            } else {
                aapsLogger.warn(LTag.PUMP, getLogPrefix() + "cancelTempBasal - Could not read currect TBR, canceling operation.");
                finishAction("TBR");
                return new PumpEnactResult(getInjector()).success(false).enacted(false)
                        .comment(getResourceHelper().gs(R.string.ypsopump_cmd_cant_read_tbr));
            }

            CommandResponse commandResponse = pumpConnectionManager.cancelTemporaryBasal();

            if (commandResponse.isSuccess()) {
                aapsLogger.info(LTag.PUMP, getLogPrefix() + "cancelTempBasal - Cancel TBR successful.");

                TemporaryBasal tempBasal = new TemporaryBasal(getInjector()) //
                        .date(System.currentTimeMillis()) //
                        .duration(0) //
                        .source(Source.USER);

                activePlugin.getActiveTreatments().addToHistoryTempBasal(tempBasal);

                return new PumpEnactResult(getInjector()).success(true).enacted(true) //
                        .isTempCancel(true);
            } else {
                aapsLogger.info(LTag.PUMP, getLogPrefix() + "cancelTempBasal - Cancel TBR failed.");

                return new PumpEnactResult(getInjector()).success(false).enacted(false) //
                        .comment(getResourceHelper().gs(R.string.ypsopump_cmd_cant_cancel_tbr));
            }

        } finally {
            finishAction("TBR");
        }
    }

    @NonNull @Override
    public String serialNumber() {
        return pumpStatus.serialNumber;
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


    @NonNull @Override
    public PumpEnactResult setNewBasalProfile(Profile profile) {
        aapsLogger.info(LTag.PUMP, getLogPrefix() + "setNewBasalProfile");

        try {
            setRefreshButtonEnabled(false);

//        String profileInvalid = isProfileValid(basalProfile);
//
//        if (profileInvalid != null) {
//            return new PumpEnactResult(getInjector()) //
//                    .success(false) //
//                    .enacted(false) //
//                    .comment(getResourceHelper().gs(R.string.medtronic_cmd_set_profile_pattern_overflow, profileInvalid));
//        }

            CommandResponse commandResponse = pumpConnectionManager.setBasalProfile(profile);

            aapsLogger.info(LTag.PUMP, getLogPrefix() + "Basal Profile was set: " + commandResponse);

            if (commandResponse.isSuccess()) {
                return new PumpEnactResult(getInjector()).success(true).enacted(true);
            } else {
                return new PumpEnactResult(getInjector()).success(false).enacted(false) //
                        .comment(getResourceHelper().gs(R.string.ypsopump_cmd_basal_profile_could_not_be_set));
            }
        } finally {
            finishAction("Set Basal Profile");
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


    @Override
    public void timezoneOrDSTChanged(TimeChangeType changeType) {
        aapsLogger.warn(LTag.PUMP, getLogPrefix() + "Time or TimeZone changed. ");

        this.hasTimeDateOrTimeZoneChanged = true;
    }

}
