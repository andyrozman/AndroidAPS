package info.nightscout.androidaps.plugins.pump.tandem.comm.ble.operations

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.SystemClock
import info.nightscout.androidaps.plugins.pump.tandem.comm.ble.defs.GattStatus
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import java.util.*
import java.util.concurrent.TimeUnit

class CharacteristicWriteOperation(aapsLogger: AAPSLogger,
                                   gatt: BluetoothGatt,
                                   chara: BluetoothGattCharacteristic,
                                   var valueToSet: ByteArray?) :
        BLECommOperation(aapsLogger, gatt, chara) {

    @SuppressLint("MissingPermission")
    override fun execute(/*comm: YpsoPumpBLE*/) {
        characteristic!!.value = valueToSet
        gatt.writeCharacteristic(characteristic)
        // wait here for callback to notify us that value was written.
        try {
            val didAcquire = operationComplete.tryAcquire(gattOperationTimeout_ms.toLong(), TimeUnit.MILLISECONDS)
            if (didAcquire) {
                SystemClock.sleep(1) // This is to allow the IBinder thread to exit before we continue, allowing easier
                // understanding of the sequence of events.
                // success
            } else {
                aapsLogger.error(LTag.PUMPBTCOMM, "Timeout waiting for gatt write operation to complete")
                timedOut = true
            }
        } catch (e: InterruptedException) {
            aapsLogger.error(LTag.PUMPBTCOMM, "Interrupted while waiting for gatt write operation to complete")
            interrupted = true
        }
    }

    // This will be run on the IBinder thread
    override fun gattOperationCompletionCallback(uuid: UUID, value: ByteArray?, gattStatus: GattStatus) {
        super.gattOperationCompletionCallback(uuid, value, gattStatus)
        if (gattStatus != GattStatus.GATT_SUCCESS) {

        }
        operationComplete.release()
    }

}