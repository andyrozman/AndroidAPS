package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import info.nightscout.shared.logging.LTag
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.GattStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.shared.logging.AAPSLogger
import java.util.*
import java.util.concurrent.Semaphore

abstract class BLECommOperation(var aapsLogger: AAPSLogger,
                                var gatt: BluetoothGatt,
                                var characteristic: BluetoothGattCharacteristic? = null) {

    var timedOut = false
    var interrupted = false

    var value: ByteArray? = null

    protected var gattStatus: GattStatus? = null

    protected var operationComplete = Semaphore(0, true)

    // This is to be run on the main thread
    abstract fun execute(comm: YpsoPumpBLE)


    open fun gattOperationCompletionCallback(uuid: UUID, value: ByteArray?, gattStatus: GattStatus) {
        if (characteristic!!.uuid != uuid) {
            aapsLogger.error(LTag.PUMPBTCOMM, String.format(
                    "Completion callback: UUID does not match! out of sequence? Found: %s, should be %s",
                    YpsoGattCharacteristic.lookup(characteristic!!.uuid), YpsoGattCharacteristic.lookup(uuid)))
        }
        checkIfCorrectGattStatus(gattStatus)
    }


    val gattOperationTimeout_ms: Int
        get() = 22000


    fun checkIfCorrectGattStatus(gattStatus: GattStatus) {
        this.gattStatus = gattStatus
        if (gattStatus != GattStatus.GATT_SUCCESS) {
            aapsLogger.error(LTag.PUMPBTCOMM, "Gatt communication was not successful! status=" + gattStatus.name)
        }
    }

}