package info.nightscout.androidaps.plugins.pump.common.driver

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context

interface PumpBLESelectorInterface {

    fun onResume()
    fun onDestroy()

    /**
     * This method is called when device is being removed (it can be empty if you don't need to do any special action, but if you
     * have to unbound (for example), then this is method where to call it. For unbounding removeBond is available
     */
    fun removeDevice(device: BluetoothDevice)

    /**
     * Cleanup method after device was removed
     */
    fun cleanupAfterDeviceRemoved()

    fun onScanFailed(context: Context, errorCode: Int)
    fun onStartLeDeviceScan(context: Context)
    fun onStopLeDeviceScan(context: Context)
    fun getScanFilters(): List<ScanFilter>?
    fun getScanSettings(): ScanSettings?
    fun filterDevice(device: BluetoothDevice): BluetoothDevice?
    fun onDeviceSelected(bluetoothDevice: BluetoothDevice, bleAddress: String, deviceName: String)

    /**
     * If pump has no name, this name will be used
     */
    fun getUnknownPumpName(): String

    /**
     * get Address of Currently selected pump, empty string if none
     */
    fun currentlySelectedAddress(): String

}