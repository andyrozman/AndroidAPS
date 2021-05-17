package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware

class GetFirmwareVersion(hasAndroidInjector: HasAndroidInjector) : AbstractBLECommand<YpsoPumpFirmware?>(hasAndroidInjector) {

    override fun executeInternal(pumpBle: YpsoPumpBLE): Boolean {

        val bleCommOperationResultDate = executeBLEReadCommandWithRetry(YpsoGattCharacteristic.MASTER_SOFTWARE_VERSION,
                pumpBle)

        bleCommOperationResult = bleCommOperationResultDate

        if (bleCommOperationResultDate!!.isSuccessful) {
            commandResponse = ypsoPumpDataConverter.decodeMasterSoftwareVersion(bleCommOperationResultDate.value!!)
            return true
        }

        return false
    }

}