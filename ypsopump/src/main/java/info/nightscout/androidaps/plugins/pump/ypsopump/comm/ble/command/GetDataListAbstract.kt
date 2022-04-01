package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.shared.logging.LTag

abstract class GetDataListAbstract<T>(hasAndroidInjector: HasAndroidInjector,
                                      var targetDate: Long?,
                                      var eventSequenceNumber: Int?,
                                      var includeEventSequence: Boolean = false) : AbstractBLECommand<MutableList<T>>(hasAndroidInjector) {

    var cancelProcessing: Boolean = false

    // TODO support for targetDate or eventSequenceNumber
    override fun executeInternal(pumpBle: YpsoPumpBLE): Boolean {

        //var lastCountSaved = 0  // this needs to read saved value so that we don't read whole list always, but for testing this is ok

        var data: ByteArray? = readFromDevice(getCountUuid(), pumpBle)

        if (data == null) {
            aapsLogger.error(LTag.PUMPCOMM, "Coudn't read last count.")
            return false
        }

        val lastCount = getResultAsInt(data)
        val lastCountGLB: Int = ypsoPumpUtil.getValueFromeGLB_SAFE_VAR(data)

        aapsLogger.debug(LTag.PUMPCOMM, "Count: " + lastCount + ", lastCountGLB: " + lastCountGLB)

        if (targetDate == null && eventSequenceNumber == null) {
            aapsLogger.debug(LTag.PUMPCOMM, "Retrieving whole " + getEntryType() + " history, no limits imposed.")
        } else if (targetDate != null) {
            aapsLogger.debug(LTag.PUMPCOMM, "Retrieving " + getEntryType() + " with targetDate $targetDate as cutoff.")
        } else {
            aapsLogger.debug(LTag.PUMPCOMM, "Retrieving " + getEntryType() + " with eventSequenceNumber $eventSequenceNumber as cutoff.")
        }

        //lastCount = 10   // TODO remove just for testing

        val outList: MutableList<T> = mutableListOf()

        for (i in 0 until (lastCount - 1)) {

            aapsLogger.debug(LTag.PUMPCOMM, "Trying to read " + getEntryType() + " with index " + i)

            if (writeToDevice(i, getIndexUuid(), pumpBle)) {

                data = readFromDevice(getValueUuid(), pumpBle)

                if (data != null) {
                    val decodedObject: T? = decodeEntry(data)

                    if (decodedObject != null) {
                        aapsLogger.debug(LTag.PUMPCOMM, "Decoded object: " + decodedObject)

                        if (targetDate == null && eventSequenceNumber == null) {
                            outList.add(decodedObject)
                        } else {
                            if (isEntryInRange(decodedObject)) {
                                outList.add(decodedObject)
                            } else {
                                break;
                            }
                        }

                    } // decoded object

                } else
                    break;

            } else {
                break;
            } // writeToDevice

            if (cancelProcessing)
                break;

        }

        aapsLogger.debug(LTag.PUMPBTCOMM, "Found " + outList.size + " new entries.")
        this.commandResponse = outList

        return true
    } // for

    abstract fun isEntryInRange(event: T): Boolean

    abstract fun getIndexUuid(): YpsoGattCharacteristic

    abstract fun getCountUuid(): YpsoGattCharacteristic

    abstract fun getValueUuid(): YpsoGattCharacteristic

    abstract fun decodeEntry(data: ByteArray): T?

    abstract fun getEntryType(): String

    fun writeToDevice(valueToWrite: Int, characteristic: YpsoGattCharacteristic, pumpBle: YpsoPumpBLE): Boolean {
        val bleCommOperationResult = executeBLEWriteCommandWithRetry(
            characteristic,
            //YpsoPumpUtil.getSettingIdAsArray(valueToWrite),
            ypsoPumpUtil.getBytesFromIntArray2(valueToWrite), // TODO fix
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