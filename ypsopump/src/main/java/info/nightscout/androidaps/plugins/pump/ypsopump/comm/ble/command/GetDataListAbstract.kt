package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil

abstract class GetDataListAbstract<T>(hasAndroidInjector: HasAndroidInjector) : AbstractBLECommand<List<T>>(hasAndroidInjector) {

    override fun executeInternal(pumpBle: YpsoPumpBLE): Boolean {

        var lastCountSaved = 0  // this needs to read saved value so that we don't read whole list always, but for testing this is ok

        var data: ByteArray? = readFromDevice(getCountUuid(), pumpBle)

        if (data == null) {
            aapsLogger.error(LTag.PUMPBTCOMM, "Coudn't read last count.")
            return false
        }

        var lastCount = getResultAsInt(data)

        var lastCountGLB: Int = YpsoPumpUtil.getValueFromeGLB_SAFE_VAR(data)

        aapsLogger.debug(LTag.PUMPBTCOMM, "Count: " + lastCount + ", lastCountGLB: " + lastCountGLB)

        if (lastCountSaved == lastCount) {
            aapsLogger.debug(LTag.PUMPBTCOMM, "No new entries found for " + getEntryType() + " [lastCountSaved=$lastCountSaved, lastCountOnPump=$lastCount")
        } else {
            aapsLogger.debug(LTag.PUMPBTCOMM, "Count for " + getEntryType() + " $lastCount items.")
        }

        //lastCount = 10   // TODO remove just for testing

        val outList: MutableList<T> = mutableListOf()

        for (i in lastCountSaved..(lastCount - 1)) {

            aapsLogger.debug(LTag.PUMPBTCOMM, "Trying to read " + getEntryType() + " with index " + i)

            if (writeToDevice(i, getIndexUuid(), pumpBle)) {

                val data = readFromDevice(getValueUuid(), pumpBle)

                if (data != null) {
                    var decodedObject: T? = decodeEntry(data)

                    if (decodedObject != null) {
                        aapsLogger.debug(LTag.PUMPBTCOMM, "Decoded object: " + decodedObject)
                        outList.add(decodedObject)
                    }
                } else
                    break;

            } else {
                break;
            }
        }

        aapsLogger.debug(LTag.PUMPBTCOMM, "Found " + outList.size + " entries.")
        this.commandResponse = outList

        return true
    }

    abstract fun getIndexUuid(): YpsoGattCharacteristic

    abstract fun getCountUuid(): YpsoGattCharacteristic

    abstract fun getValueUuid(): YpsoGattCharacteristic

    abstract fun decodeEntry(data: ByteArray): T?

    abstract fun getEntryType(): String

    fun writeToDevice(valueToWrite: Int, characteristic: YpsoGattCharacteristic, pumpBle: YpsoPumpBLE): Boolean {
        val bleCommOperationResult = executeBLEWriteCommandWithRetry(
            characteristic,
            //YpsoPumpUtil.getSettingIdAsArray(valueToWrite),
            YpsoPumpUtil.getBytesFromIntArray2(valueToWrite), // TODO fix
            pumpBle)
        if (!bleCommOperationResult!!.isSuccessful) {
            this.bleCommOperationResult = bleCommOperationResult
            return false
        } else {
            return true
        }
    }

    fun readFromDevice(characteristic: YpsoGattCharacteristic, pumpBle: YpsoPumpBLE): ByteArray? {
        val bleCommOperationResultRead = executeBLEReadCommandWithRetry(
            characteristic,
            pumpBle)
        if (!bleCommOperationResultRead!!.isSuccessful) {
            this.bleCommOperationResult = bleCommOperationResultRead
            return null
        }
        val data = bleCommOperationResultRead.value
        return data
    }

}