package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.DateTimeDto

class GetDate(hasAndroidInjector: HasAndroidInjector?) : AbstractBLECommand<DateTimeDto?>(hasAndroidInjector!!) {

    override fun executeInternal(pumpBle: YpsoPumpBLE): Boolean {
        //return pumpBle.readCharacteristicWithUuid(YpsoGattCharacteristic.MASTER_SOFTWARE_VERSION);
        return false
    }

    override fun toString(): String {
        val sb = StringBuilder("GetDate{")
        sb.append("commandResponse=").append(commandResponse)
        //sb.append(", commandResponseStatus=").append(commandResponseStatus);
        sb.append('}')
        return sb.toString()
    }
}