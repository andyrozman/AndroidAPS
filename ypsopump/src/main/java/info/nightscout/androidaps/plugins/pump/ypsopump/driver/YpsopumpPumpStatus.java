package info.nightscout.androidaps.plugins.pump.ypsopump.driver;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.inject.Inject;
import javax.inject.Singleton;

import info.nightscout.androidaps.plugins.bus.RxBusWrapper;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDeviceState;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpConst;
import info.nightscout.androidaps.utils.resources.ResourceHelper;
import info.nightscout.androidaps.utils.sharedPreferences.SP;


/**
 * Created by andy on 1/4/21.
 */

@Singleton
public class YpsopumpPumpStatus extends info.nightscout.androidaps.plugins.pump.common.data.PumpStatus  {

    private final ResourceHelper resourceHelper;
    private final SP sp;
    private final RxBusWrapper rxBus;

    public String errorDescription = null;
    public YpsoPumpFirmware ypsopumpFirmware;
    public double baseBasalRate = 0.0d;


    public String serialNumber;
    public String pumpFrequency = null;
    public Double maxBolus;
    public Double maxBasal;

    // statuses
    private PumpDeviceState pumpDeviceState = PumpDeviceState.NeverContacted;
    public Date tempBasalStart;
    public Double tempBasalAmount = 0.0d;

    // fixme
    public Integer tempBasalLength = 0;

//    public BasalProfileStatus basalProfileStatus = BasalProfileStatus.NotInitialized;
//    public BatteryType batteryType = BatteryType.None;




    @Inject
    public YpsopumpPumpStatus(ResourceHelper resourceHelper,
                              SP sp,
                              RxBusWrapper rxBus
    ) {
        super(PumpType.YpsoPump);
        this.resourceHelper = resourceHelper;
        this.sp = sp;
        this.rxBus = rxBus;
        initSettings();
    }


    public void initSettings() {
        this.activeProfileName = "A";
        this.reservoirRemainingUnits = 75d;
        this.reservoirFullUnits = 160;
        this.batteryRemaining = 75;
        this.lastConnection = sp.getLong(YpsoPumpConst.Statistics.LastGoodPumpCommunicationTime, 0L);
        this.lastDataTime = this.lastConnection;
    }


    public double getBasalProfileForHour() {
        if (basalsByHour != null) {
            GregorianCalendar c = new GregorianCalendar();
            int hour = c.get(Calendar.HOUR_OF_DAY);

            return basalsByHour[hour];
        }

        return 0;
    }


    @NotNull
    public String getErrorInfo() {
        return (errorDescription == null) ? "-" : errorDescription;
    }


    public PumpDeviceState getPumpDeviceState() {
        return pumpDeviceState;
    }


    public void setPumpDeviceState(PumpDeviceState pumpDeviceState) {
        this.pumpDeviceState = pumpDeviceState;

//        rileyLinkUtil.getRileyLinkHistory().add(new RLHistoryItem(pumpDeviceState, RileyLinkTargetDevice.MedtronicPump));
//
//        rxBus.send(new EventRileyLinkDeviceStatusChange(pumpDeviceState));
    }
}
