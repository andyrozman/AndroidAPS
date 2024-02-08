package info.nightscout.androidaps.plugins.pump.tandem.comm.ble.operations

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import info.nightscout.androidaps.plugins.pump.tandem.comm.ble.defs.GattStatus
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
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
    @SuppressLint("MissingPermission")
    abstract fun execute(/*comm: YpsoPumpBLE*/)


    open fun gattOperationCompletionCallback(uuid: UUID, value: ByteArray?, gattStatus: GattStatus) {
        if (characteristic!!.uuid != uuid) {
            aapsLogger.error(
                LTag.PUMPBTCOMM, String.format(
                    "Completion callback: UUID does not match! out of sequence? Found: %s, should be %s",
                    characteristic!!.uuid, uuid))
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