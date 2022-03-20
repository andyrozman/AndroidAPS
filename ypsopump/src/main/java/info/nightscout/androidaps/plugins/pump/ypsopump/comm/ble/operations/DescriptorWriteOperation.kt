package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import android.os.SystemClock
import info.nightscout.shared.logging.LTag
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.GattStatus
import info.nightscout.shared.logging.AAPSLogger
import java.util.*
import java.util.concurrent.TimeUnit

class DescriptorWriteOperation(aapsLogger: AAPSLogger,
                               gatt: BluetoothGatt,
                               var descriptor: BluetoothGattDescriptor,
                               var valueToSet: ByteArray?) :
        BLECommOperation(aapsLogger, gatt, null) {

    override fun gattOperationCompletionCallback(uuid: UUID, value: ByteArray?, gattStatus: GattStatus) {
        //super.gattOperationCompletionCallback(uuid, value);
        operationComplete.release()
    }

    override fun execute(comm: YpsoPumpBLE) {
        descriptor.value = valueToSet
        gatt.writeDescriptor(descriptor)
        // wait here for callback to notify us that value was read.
        try {
            val didAcquire = operationComplete.tryAcquire(gattOperationTimeout_ms.toLong(), TimeUnit.MILLISECONDS)
            if (didAcquire) {
                SystemClock.sleep(1) // This is to allow the IBinder thread to exit before we continue, allowing easier
                // understanding of the sequence of events.
                // success
            } else {
                aapsLogger.error(LTag.PUMPBTCOMM, "Timeout waiting for descriptor write operation to complete")
                timedOut = true
            }
        } catch (e: InterruptedException) {
            aapsLogger.error(LTag.PUMPBTCOMM, "Interrupted while waiting for descriptor write operation to complete")
            interrupted = true
        }
    }

}