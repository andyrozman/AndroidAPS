package info.nightscout.androidaps.plugins.pump.ypsopump.data

import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoBasalProfileType

class YpsoPumpSettingsDto {

    var basalProfileType: YpsoBasalProfileType? = null
    var bolusIncStep: Double? = null
    var remainingLifetime = 0

    // 1.1 or 1.2 version
    var bolusAmoutLimit: Double? = null
    var basalRateLimit: Double? = null


    override fun toString(): String {
        return "YpsoPumpSettingsDto [basalProfileType=$basalProfileType, bolusIncStep=$bolusIncStep, " +
            "remainingLifetime=$remainingLifetime, bolusAmoutCAP=$bolusAmoutLimit, " +
            "basalRateCAP=$basalRateLimit)"
    }

}