package info.nightscout.androidaps.plugins.pump.tandem.comm.ble.operations

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.SystemClock
import info.nightscout.androidaps.plugins.pump.tandem.comm.ble.defs.GattStatus
import info.nightscout.rx.logging.AAPSLogger
import info.nightscout.rx.logging.LTag
import java.util.*
import java.util.concurrent.TimeUnit

class CharacteristicReadOperation(aapsLogger: AAPSLogger, gatt: BluetoothGatt, chara: BluetoothGattCharacteristic) :
        BLECommOperation(aapsLogger, gatt, chara) {

    @SuppressLint("MissingPermission")
    override fun execute(/*comm: YpsoPumpBLE*/) {

        gatt.readCharacteristic(characteristic)

        // wait here for callback to notify us that value was read.
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
        value = characteristic!!.value
    }

    override fun gattOperationCompletionCallback(uuid: UUID, value: ByteArray?, gattStatus: GattStatus) {
        super.gattOperationCompletionCallback(uuid, value, gattStatus)
        operationComplete.release()
    }


}