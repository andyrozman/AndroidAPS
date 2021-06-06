package info.nightscout.androidaps.plugins.pump.ypsopump.defs;

/**
 * Created by andy on 6/28/18.
 */

public enum YpsoPumpStatusRefreshType {

    PumpHistory(5, null), //
    //Configuration(0, null), //
    RemainingInsulin(-1, null /*MedtronicCommandType.GetRemainingInsulin*/), //
    BatteryStatus(55, null /*MedtronicCommandType.GetBatteryStatus*/), //
    PumpTime(60, null /*MedtronicCommandType.GetRealTimeClock*/) //
    ;

    private final int refreshTime;
    private final YpsoPumpCommandType commandType;


    YpsoPumpStatusRefreshType(int refreshTime, YpsoPumpCommandType commandType) {
        this.refreshTime = refreshTime;
        this.commandType = commandType;
    }


    public int getRefreshTime() {
        return refreshTime;
    }


    public YpsoPumpCommandType getCommandType(YpsoPumpFirmware medtronicDeviceType) {
//        if (this == Configuration) {
//            return MedtronicCommandType.getSettings(medtronicDeviceType);
//        } else
            return commandType;
    }
}
