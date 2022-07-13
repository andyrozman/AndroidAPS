package info.nightscout.androidaps.plugins.pump.tandem.defs;

import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType;

/**
 * Created by andy on 04.07.2022.
 */

public enum YpsoPumpStatusRefreshType {

    PumpHistory(5, null), //
    //Configuration(0, null), //
    RemainingInsulin(-1, null /*MedtronicCommandType.GetRemainingInsulin*/), //
    BatteryStatus(55, null /*MedtronicCommandType.GetBatteryStatus*/), //
    PumpTime(60, null /*MedtronicCommandType.GetRealTimeClock*/) //
    ;

    private final int refreshTime;
    private final PumpCommandType commandType;


    YpsoPumpStatusRefreshType(int refreshTime, PumpCommandType commandType) {
        this.refreshTime = refreshTime;
        this.commandType = commandType;
    }


    public int getRefreshTime() {
        return refreshTime;
    }


//    public PumpCommandType getCommandType(YpsoPumpFirmware medtronicDeviceType) {
////        if (this == Configuration) {
////            return MedtronicCommandType.getSettings(medtronicDeviceType);
////        } else
//            return commandType;
//    }
}
