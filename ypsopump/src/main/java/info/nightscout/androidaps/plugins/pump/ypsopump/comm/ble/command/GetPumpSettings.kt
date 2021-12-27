package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoBasalProfileType
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoSettingId
import info.nightscout.androidaps.plugins.pump.ypsopump.data.YpsoPumpSettingsDto

class GetPumpSettings(hasAndroidInjector: HasAndroidInjector?) : GetSettingsAbstract<YpsoPumpSettingsDto?>(hasAndroidInjector!!) {

    override fun executeInternal(pumpBle: YpsoPumpBLE): Boolean {
        val settingsDto = YpsoPumpSettingsDto()

        this.commandResponse = settingsDto

        // basal profile
        var data: ByteArray? = readSettingsValue(YpsoSettingId.BasalProfileSelection, pumpBle)
            ?: return false

        var value = getResultAsInt(data!!)
        when (value) {
            10   -> settingsDto.basalProfileType = YpsoBasalProfileType.BASAL_PROFILE_B
            3    -> settingsDto.basalProfileType = YpsoBasalProfileType.BASAL_PROFILE_A

            else -> {
                settingsDto.basalProfileType = YpsoBasalProfileType.UNKNOWN
                aapsLogger.error(LTag.PUMPBTCOMM, "Unknown Basal Profile code received: $value")
            }
        }

        aapsLogger.debug(LTag.PUMPBTCOMM, "Basal Profile selected: " + settingsDto.basalProfileType)

//        // remaining lifetime
//        data = readSettingsValue(YpsoSettingId.RemainingLifetime, pumpBle)
//                ?: return false
//
//        value = getResultAsInt(data)
//        settingsDto.remainingLifetime = value
//
//        aapsLogger.debug(LTag.PUMPBTCOMM, "Remaining Lifetime: " + value)

        // bolus inc
        data = readSettingsValue(YpsoSettingId.BolusIncrementStepSelection, pumpBle)
            ?: return false

        value = getResultAsInt(data)
        val step = value / 100.0
        settingsDto.bolusIncStep = step

        aapsLogger.debug(LTag.PUMPBTCOMM, "Bolus Step: " + step)

        // SETTING_SERVICE_VERSION

        if (ypsopumpPumpStatus.settingsServiceVersion == null) {

            val bleCommOperationResultRead = executeBLEReadCommandWithRetry(
                YpsoGattCharacteristic.SETTING_SERVICE_VERSION,
                pumpBle)

            if (!bleCommOperationResultRead!!.isSuccessful) {
                this.bleCommOperationResult = bleCommOperationResultRead
                return false
            }

            data = bleCommOperationResultRead.value

            val version = ByteUtil.getString(data)

            ypsopumpPumpStatus.settingsServiceVersion = version

            aapsLogger.debug(LTag.PUMPBTCOMM, "Settings Service Version: " + version)
        }

        if (ypsopumpPumpStatus.settingsServiceVersion.equals("1.1") ||
            ypsopumpPumpStatus.settingsServiceVersion.equals("1.2")) {

            // basalRateLimit
            data = readSettingsValue(YpsoSettingId.BasalRateLimit, pumpBle)
                ?: return false

            value = getResultAsInt(data)
            settingsDto.basalRateLimit = value / 100.0

            aapsLogger.debug(LTag.PUMPBTCOMM, "Basal Rate Limit: " + settingsDto.basalRateLimit)

            // bolus rate limit
            data = readSettingsValue(YpsoSettingId.BolusAmountLimit, pumpBle)
                ?: return false

            value = getResultAsInt(data)
            settingsDto.bolusAmoutLimit = value / 100.0

            aapsLogger.debug(LTag.PUMPBTCOMM, "Bolus Amount Limit: " + settingsDto.bolusAmoutLimit)

        }

        this.commandResponse = settingsDto

        // lifetime: 32766 - 13.5. - 15:18

        return true
    }
}