package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations.BLECommOperationResult
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.DateTimeDto

class GetDateTime(hasAndroidInjector: HasAndroidInjector?) : AbstractBLECommand<DateTimeDto?>(hasAndroidInjector!!) {

    override fun executeInternal(pumpBle: YpsoPumpBLE): Boolean {
        var date: DateTimeDto?
        val bleCommOperationResultDate = executeBLEReadCommandWithRetry(YpsoGattCharacteristic.SYSTEM_DATE,
                pumpBle)

        bleCommOperationResult = bleCommOperationResultDate

        date = if (bleCommOperationResultDate!!.isSuccessful) {
            ypsoPumpDataConverter.decodeDate(bleCommOperationResultDate.value!!)
        } else {
            return false
        }

        var time: DateTimeDto? = null
        val bleCommOperationResultTime: BLECommOperationResult?

        bleCommOperationResultTime = executeBLEReadCommandWithRetry(YpsoGattCharacteristic.SYSTEM_TIME, pumpBle)

        bleCommOperationResult = bleCommOperationResultTime

        if (bleCommOperationResultTime!!.isSuccessful) {
            time = ypsoPumpDataConverter.decodeTime(bleCommOperationResultTime.value!!)
        }

        if (time != null) {
            date.hour = time.hour
            date.minute = time.minute
            date.second = time.second
            commandResponse = date
            return true
        } else {
            return false
        }
    }
}