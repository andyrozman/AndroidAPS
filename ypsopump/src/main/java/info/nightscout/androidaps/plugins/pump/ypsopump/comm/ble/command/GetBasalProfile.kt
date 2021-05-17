package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoSettingId
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.BasalProfileDto
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil

class GetBasalProfile(hasAndroidInjector: HasAndroidInjector?) : GetSettingsAbstract<BasalProfileDto?>(hasAndroidInjector!!) {

    override fun executeInternal(pumpBle: YpsoPumpBLE): Boolean {

        val patterns = DoubleArray(24)
        for (i in 0..23) {
            val data = readSettingsValue(YpsoSettingId.BasalPatternA, i, pumpBle)
                    ?: return false

            val i2 = getResultAsInt(data)
            patterns[i] = i2 / 100.0
        }
        commandResponse = BasalProfileDto(patterns)
        return true

    }

    fun getSettingIdAsArray(settingId: Int): ByteArray {
        return YpsoPumpUtil.getSettingIdAsArray(settingId)
    }
}