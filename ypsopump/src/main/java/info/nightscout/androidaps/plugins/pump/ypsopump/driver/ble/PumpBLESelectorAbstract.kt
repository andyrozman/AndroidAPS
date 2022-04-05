package info.nightscout.androidaps.plugins.pump.ypsopump.driver.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.pump.common.driver.PumpBLESelectorInterface
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import info.nightscout.shared.sharedPreferences.SP

abstract class PumpBLESelectorAbstract constructor(
    var resourceHelper: ResourceHelper,
    var aapsLogger: AAPSLogger,
    var sp: SP,
    var rxBus: RxBus,
    var context: Context
) : PumpBLESelectorInterface {

    // @Inject protected lateinit var resourceHelper: ResourceHelper
    // @Inject protected lateinit var aapsLogger: AAPSLogger
    // @Inject protected lateinit var sp: SP
    // @Inject protected lateinit var rxBus: RxBus
    // @Inject protected lateinit var context: Context

    protected val TAG = LTag.PUMPBTCOMM

    override fun getScanSettings(): ScanSettings? {
        return ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    }

    override fun filterDevice(device: BluetoothDevice): BluetoothDevice? {
        return device
    }

    protected fun removeBond(bluetoothDevice: BluetoothDevice): Boolean {
        return try {
            val method = bluetoothDevice.javaClass.getMethod("removeBond", null)
            val resultObject = method.invoke(bluetoothDevice, null as Array<Any?>?)
            if (resultObject == null) {
                aapsLogger.error(TAG, "ERROR: result object is null")
                false
            } else {
                val result = resultObject as Boolean
                if (result) {
                    aapsLogger.info(TAG, "Successfully removed bond")
                } else {
                    aapsLogger.warn(TAG, "Bond was not removed")
                }
                result
            }
        } catch (e: Exception) {
            aapsLogger.error(TAG, "ERROR: could not remove bond")
            e.printStackTrace()
            false
        }
    }

    protected fun getBondingStatusDescription(state: Int): String {
        return if (state == 10) {
            "BOND_NONE"
        } else if (state == 11) {
            "BOND_BONDING"
        } else if (state == 12) {
            "BOND_BONDED"
        } else {
            "UNKNOWN BOND STATUS ($state)"
        }
    }

    override fun getScanFilters(): MutableList<ScanFilter>? {
        return null
    }
    
}