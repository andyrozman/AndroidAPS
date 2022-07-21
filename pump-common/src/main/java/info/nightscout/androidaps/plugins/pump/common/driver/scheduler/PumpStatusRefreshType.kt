package info.nightscout.androidaps.plugins.pump.common.driver.scheduler

/**
 * Created by andy on 6/28/18.
 */
enum class PumpStatusRefreshType() {

    PumpHistory,
    Configuration ,
    RemainingInsulin,
    BatteryStatus,
    PumpTime;

    // PumpHistory, //(5),  //
    // Configuration //(0),  //
    // RemainingInsulin(-1, MedtronicCommandType.GetRemainingInsulin),  //
    // BatteryStatus(55, MedtronicCommandType.GetBatteryStatus),  //
    // PumpTime(60, MedtronicCommandType.GetRealTimeClock //
    // );

    // fun getCommandType(medtronicDeviceType: MedtronicDeviceType): MedtronicCommandType? {
    //     return if (this == Configuration) {
    //         MedtronicCommandType.getSettings(medtronicDeviceType)
    //     } else
    //         commandType
    // }

}