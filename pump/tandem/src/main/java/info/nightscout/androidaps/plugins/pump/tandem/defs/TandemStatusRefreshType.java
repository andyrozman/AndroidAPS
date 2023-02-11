package info.nightscout.androidaps.plugins.pump.tandem.defs;

import info.nightscout.pump.common.driver.connector.defs.PumpCommandType;

/**
 * Created by andy on 04.07.2022.
 */

public enum TandemStatusRefreshType {

    PumpHistory(5, PumpCommandType.GetHistory), //
    //Configuration(0, null), //
    RemainingInsulin(-1, PumpCommandType.GetRemainingInsulin), //
    BatteryStatus(55, PumpCommandType.GetBatteryStatus), //
    PumpTime(60, PumpCommandType.GetTime) //
    ;

    private final int refreshTime;
    private final PumpCommandType commandType;


    TandemStatusRefreshType(int refreshTime, PumpCommandType commandType) {
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
