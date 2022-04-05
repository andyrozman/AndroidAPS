package info.nightscout.androidaps.plugins.pump.ypsopump.driver.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.IntentFilter
import android.widget.Toast
import androidx.annotation.StringRes
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.pump.common.ble.BondStateReceiver
import info.nightscout.androidaps.plugins.pump.common.driver.PumpBLESelectorInterface
import info.nightscout.androidaps.plugins.pump.common.driver.PumpBLESelectorText
import info.nightscout.androidaps.plugins.pump.common.events.EventBondChanged
import info.nightscout.androidaps.plugins.pump.common.events.EventPumpChanged
import info.nightscout.androidaps.plugins.pump.ypsopump.R
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpConst
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.sharedPreferences.SP
import javax.inject.Inject

@SuppressLint("MissingPermission")
class YpsoPumpBLESelector @Inject constructor(
    resourceHelper: ResourceHelper,
    aapsLogger: AAPSLogger,
    sp: SP,
    rxBus: RxBus,
    context: Context,
    var ypsoPumpUtil: YpsoPumpUtil
) : PumpBLESelectorAbstract(resourceHelper, aapsLogger, sp, rxBus, context), PumpBLESelectorInterface {

    //@Inject lateinit var ypsoPumpUtil: YpsoPumpUtil

    override fun onResume() {
        ypsoPumpUtil.preventConnect = true
    }

    override fun onDestroy() {
        ypsoPumpUtil.preventConnect = false
    }

    override fun onScanFailed(context: Context, errorCode: Int) {
        Toast.makeText(
            context, resourceHelper.gs(R.string.ble_config_scan_error, errorCode),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onStartLeDeviceScan(context: Context) {
        Toast.makeText(context, R.string.ble_config_scan_scanning, Toast.LENGTH_SHORT).show()
    }

    override fun onStopLeDeviceScan(context: Context) {
        Toast.makeText(context, R.string.ble_config_scan_finished, Toast.LENGTH_SHORT).show()
    }

    override fun removeDevice(device: BluetoothDevice) {
        if (device.getBondState() == 12) {
            // TODO remove bond
            //bluetoothDevice.removeBond();
            removeBond(device)
        }
    }

    override fun getScanSettings(): ScanSettings? {
        //return ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        val scanSettingBuilder = ScanSettings.Builder()
            //.setLegacy(true)
            //.setCallbackType(DEFAULT_KEYS_SEARCH_GLOBAL)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)

        return scanSettingBuilder.build();
    }

    override fun getScanFilters(): MutableList<ScanFilter>? {

        // val scanFilterList: MutableList<ScanFilter> = mutableListOf()
        // val scanFilterBuilder = ScanFilter.Builder()
        // //val fromString = ParcelUuid.fromString(YpsoGattService.SERVICE_YPSO_1.uuid)
        // scanFilterBuilder.setServiceUuid(ParcelUuid.fromString(YpsoGattService.SERVICE_YPSO_1.uuid))
        //
        // scanFilterBuilder.se
        // scanFilterList.add(scanFilterBuilder.build())
        // return scanFilterList

        return null

        // TODO Ypso only
        //return null
    }

    override fun filterDevice(device: BluetoothDevice): BluetoothDevice? {
        if (device.name != null && device.name.contains("Ypso")) {
            return device
        }

        return null
    }

    override fun cleanupAfterDeviceRemoved() {
        sp.remove(YpsoPumpConst.Prefs.PumpAddress)
        sp.remove(YpsoPumpConst.Prefs.PumpName)
        sp.putBoolean(YpsoPumpConst.Prefs.PumpBonded, false)
    }

    override fun onDeviceSelected(bluetoothDevice: BluetoothDevice, bleAddress: String, deviceName: String) {

        val bondState: Int = bluetoothDevice.getBondState()

        aapsLogger.debug(TAG, "Device bonding status: " + bluetoothDevice.getBondState() + " desc: " + getBondingStatusDescription(bluetoothDevice.getBondState()))

        val bonded = bondState != 12

        // if we are not bonded, bonding is started

        // if we are not bonded, bonding is started
        if (bondState != 12) {
            aapsLogger.debug(TAG, "Start bonding")
            context.registerReceiver(
                BondStateReceiver(
                    R.string.key_ypsopump_address,
                    R.string.key_ypsopump_bonded,
                    bleAddress, 12
                ),
                IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            )
            bluetoothDevice.createBond()
        }

        sp.putBoolean(YpsoPumpConst.Prefs.PumpBonded, bonded)
        rxBus.send(EventBondChanged(bleAddress, bonded))

        var addressChanged = false

        // set pump address
        if (setSystemParameterForBT(YpsoPumpConst.Prefs.PumpAddress, bleAddress)) {
            addressChanged = true
        }

        setSystemParameterForBT(YpsoPumpConst.Prefs.PumpName, deviceName)

        var name: String = bluetoothDevice.getName()

        if (name.contains("_")) {
            name = name.substring(name.indexOf("_") + 1)
            setSystemParameterForBT(YpsoPumpConst.Prefs.PumpSerial, name)
        }

        if (addressChanged) {
            // TODO send notification to pumpSync
            rxBus.send(EventPumpChanged(name, bleAddress, null))
        }

    }

    private fun setSystemParameterForBT(@StringRes parameter: Int, newValue: String): Boolean {
        if (sp.contains(parameter)) {
            val current = sp.getStringOrNull(parameter, null)
            if (current == null) {
                sp.putString(parameter, newValue)
            } else if (current != newValue) {
                sp.putString(parameter, newValue)
                return true
            }
        } else {
            sp.putString(parameter, newValue)
        }
        return false
    }

    override fun getUnknownPumpName(): String {
        return "YpsoPump (?)"
    }

    override fun currentlySelectedPumpAddress(): String {
        return sp.getString(YpsoPumpConst.Prefs.PumpAddress, "")
    }

    override fun currentlySelectedPumpName(): String {
        return sp.getString(YpsoPumpConst.Prefs.PumpName, getUnknownPumpName())
    }

    override fun getText(key: PumpBLESelectorText): String {
        var stringId: Int = R.string.ypsopump_ble_config_scan_title

        when (key) {
            PumpBLESelectorText.SCAN_TITLE          -> stringId = R.string.ypsopump_ble_config_scan_title
            PumpBLESelectorText.SELECTED_PUMP_TITLE -> stringId = R.string.ypsopump_ble_config_selected_pump_title
            PumpBLESelectorText.REMOVE_TITLE        -> stringId = R.string.ypsopump_ble_config_remove_pump_title
            PumpBLESelectorText.REMOVE_TEXT         -> stringId = R.string.ypsopump_ble_config_remove_pump_confirmation
            PumpBLESelectorText.NO_SELECTED_PUMP    -> stringId = R.string.ypsopump_ble_config_no_pump_selected
            PumpBLESelectorText.PUMP_CONFIGURATION  -> stringId = R.string.ypsopump_configuration
        }

        return resourceHelper.gs(stringId)
    }

}