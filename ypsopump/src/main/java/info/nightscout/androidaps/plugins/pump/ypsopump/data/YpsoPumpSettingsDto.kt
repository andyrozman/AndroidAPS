package info.nightscout.androidaps.plugins.pump.ypsopump.data

import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoBasalProfileType

class YpsoPumpSettingsDto {

    var basalProfileType: YpsoBasalProfileType? = null
    var bolusIncStep: Double? = null
    var remainingLifetime = 0

    // 1.1 or 1.2 version
    var bolusAmoutLimit: Double? = null
    var basalRateLimit: Double? = null

    // base
    // var pumpIsOn = false
    // var lastEventBasalRate: Event? = null
    // var lastBasalRateEventId: Guid? = null

    // // 11
    // var lastEventCounterNumber: Short = 0
    // var lastAlertCounterNumber: Short = 0
    // var lastEventCount = 0
    // var lastAlertCount = 0
    //
    // // 15
    // var lastEventSequenceNumber = 0
    // var lastAlertSequenceNumber = 0
    // var lastSystemSequenceNumber = 0

    //    var lastEventCount = 0
//    var lastAlertCount = 0
//     var lastSystemCount = 0

    override fun toString(): String {
        return "YpsoPumpSettingsDto [basalProfileType=$basalProfileType, bolusIncStep=$bolusIncStep, " +
            "remainingLifetime=$remainingLifetime, bolusAmoutCAP=$bolusAmoutLimit, " +
            "basalRateCAP=$basalRateLimit)"
    }

}