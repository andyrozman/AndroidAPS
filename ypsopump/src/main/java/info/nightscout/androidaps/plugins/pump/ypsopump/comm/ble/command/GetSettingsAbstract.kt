package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoSettingId
import info.nightscout.shared.logging.LTag
import java.util.*

abstract class GetSettingsAbstract<T>(hasAndroidInjector: HasAndroidInjector) : AbstractBLECommand<T>(hasAndroidInjector) {

    fun readSettingsValue(settingsId: YpsoSettingId, pumpBle: YpsoPumpBLE?): ByteArray? {
        return readSettingsValue(settingsId, 0, pumpBle)
    }

    fun readSettingsValue(settingsId: YpsoSettingId, index: Int, pumpBle: YpsoPumpBLE?): ByteArray? {
        val bleCommOperationResult = executeBLEWriteCommandWithRetry(
            YpsoGattCharacteristic.SETTINGS_ID,
            ypsoPumpUtil.getSettingIdAsArray(settingsId.id + index), pumpBle!!)
        if (!bleCommOperationResult!!.isSuccessful) {
            this.bleCommOperationResult = bleCommOperationResult
            return null
        }
        val bleCommOperationResultRead = executeBLEReadCommandWithRetry(
            YpsoGattCharacteristic.SETTINGS_VALUE,
            pumpBle)
        if (!bleCommOperationResultRead!!.isSuccessful) {
            this.bleCommOperationResult = bleCommOperationResultRead
            return null
        }
        val data = bleCommOperationResultRead.value
        this.bleCommOperationResult = bleCommOperationResultRead

        aapsLogger.debug(LTag.PUMPBTCOMM, String.format(Locale.ENGLISH, "Setting read [name=%s, index=%d, result=%s]", settingsId.name, index, ByteUtil.getHex(data)))
        return data
    }

}