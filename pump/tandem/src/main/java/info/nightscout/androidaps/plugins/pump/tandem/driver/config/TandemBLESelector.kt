package info.nightscout.androidaps.plugins.pump.tandem.driver.config

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.annotation.StringRes
import com.jwoglom.pumpx2.pump.messages.bluetooth.ServiceUUID
import info.nightscout.rx.bus.RxBus

import info.nightscout.pump.common.driver.ble.PumpBLESelectorAbstract
import info.nightscout.pump.common.events.EventPumpConnectionParametersChanged
import info.nightscout.androidaps.plugins.pump.tandem.R
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpConst
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.pump.common.ui.PumpBLEConfigActivity
import info.nightscout.androidaps.plugins.pump.tandem.comm.TandemPairingManager
import info.nightscout.androidaps.plugins.pump.tandem.driver.TandemPumpStatus
import info.nightscout.interfaces.pump.PumpSync
import info.nightscout.pump.common.driver.ble.PumpBLESelector
import info.nightscout.pump.common.driver.ble.PumpBLESelectorText
import info.nightscout.rx.logging.AAPSLogger
import info.nightscout.rx.logging.LTag
import info.nightscout.shared.interfaces.ResourceHelper
import info.nightscout.shared.sharedPreferences.SP
import javax.inject.Inject

// TODO this needs to be refactored
@SuppressLint("MissingPermission")
class TandemBLESelector @Inject constructor(
    resourceHelper: ResourceHelper,
    aapsLogger: AAPSLogger,
    sp: SP,
    rxBus: RxBus,
    context: Context,
    var tandemPumpUtil: TandemPumpUtil,
    var pumpSync: PumpSync,
    var pumpStatus: TandemPumpStatus
) : PumpBLESelectorAbstract(resourceHelper, aapsLogger, sp, rxBus, context), PumpBLESelector {

    var startingAddress: String? = null
    var tandemPairingManager: TandemPairingManager? = null

    override fun onResume() {
        tandemPumpUtil.preventConnect = true
        rxBus.send(EventPumpConnectionParametersChanged())
        startingAddress = currentlySelectedPumpAddress();
    }

    override fun onDestroy() {
        tandemPumpUtil.preventConnect = false
        rxBus.send(EventPumpConnectionParametersChanged())

        // val selectedAddress = currentlySelectedPumpAddress()
        //
        // // TODO this might need to be moved to PairingManager
        // if (!startingAddress.equals(selectedAddress) &&
        //     selectedAddress.length > 0
        // ) {
        //     pumpSync.connectNewPump()
        // }
    }

    override fun removeDevice(device: BluetoothDevice) {
    }

    override fun getScanSettings(): ScanSettings? {
        val scanSettingBuilder = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)

        return scanSettingBuilder.build();
    }

    override fun getScanFilters(): MutableList<ScanFilter>? {
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(ServiceUUID.PUMP_SERVICE_UUID))
            .build()

        return mutableListOf(filter);
    }

    override fun cleanupAfterDeviceRemoved() {
        sp.remove(TandemPumpConst.Prefs.PumpAddress)
        cleanSP()
        rxBus.send(EventPumpConnectionParametersChanged())
    }

    override fun onDeviceSelected(bluetoothDevice: BluetoothDevice, bleAddress: String, deviceName: String, activity: PumpBLEConfigActivity) {

        aapsLogger.debug(TAG, "TANDEMDBG: onDeviceSelected: ${bleAddress} ")

        //var addressChanged = false

        // set pump address
        //if (setSystemParameterForBT(TandemPumpConst.Prefs.PumpAddress, bleAddress)) {
            //addressChanged = true
            cleanSP()

            rxBus.send(EventPumpConnectionParametersChanged())

            aapsLogger.debug(TAG, "TANDEMDBG: Create TandemPairingManager")

            try {
                tandemPairingManager = TandemPairingManager(
                    context = context,
                    aapsLogger = aapsLogger,
                    sp = sp,
                    pumpUtil = tandemPumpUtil,
                    btAddress = bleAddress,
                    rxBus = rxBus,
                    resourceHelper = resourceHelper,
                    pumpStatus = pumpStatus,
                    pumpSync = pumpSync,
                    activity = activity
                )
                tandemPairingManager!!.startPairing()
            } catch(ex: Exception) {
                aapsLogger.error(LTag.PUMPCOMM, "Caught exception trying to pair. Ex: ${ex.message}", ex)
            }
        //}

    }

    override fun onDeviceSelectedClosesActivity(): Boolean {
        return false
    }

    private fun cleanSP() {
        sp.remove(TandemPumpConst.Prefs.PumpPairStatus)
        sp.remove(TandemPumpConst.Prefs.PumpPairCode)
        sp.remove(TandemPumpConst.Prefs.PumpName)
        sp.remove(TandemPumpConst.Prefs.PumpSerial)
        sp.remove(TandemPumpConst.Prefs.PumpVersionResponse)
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

    override fun getUnknownPumpName(): String = "Tandem (?)"

    override fun currentlySelectedPumpAddress(): String = sp.getString(TandemPumpConst.Prefs.PumpAddress, "")

    override fun currentlySelectedPumpName(): String = sp.getString(TandemPumpConst.Prefs.PumpName, getUnknownPumpName())

    override fun getText(key: PumpBLESelectorText): String {
        var stringId: Int = R.string.tandem_ble_config_scan_title

        when (key) {
            PumpBLESelectorText.SCAN_TITLE          -> stringId = R.string.tandem_ble_config_scan_title
            PumpBLESelectorText.SELECTED_PUMP_TITLE -> stringId = R.string.tandem_ble_config_selected_pump_title
            PumpBLESelectorText.REMOVE_TITLE        -> stringId = R.string.tandem_ble_config_remove_pump_title
            PumpBLESelectorText.REMOVE_TEXT         -> stringId = R.string.tandem_ble_config_remove_pump_confirmation
            PumpBLESelectorText.NO_SELECTED_PUMP    -> stringId = R.string.tandem_ble_config_no_pump_selected
            PumpBLESelectorText.PUMP_CONFIGURATION  -> stringId = R.string.tandem_configuration
        }

        return resourceHelper.gs(stringId)
    }

    override fun getAdditionalPermissions(): List<String> {
        return arrayListOf()
        // return listOf(
        //     Manifest.permission.BLUETOOTH,
        //     Manifest.permission.BLUETOOTH_ADMIN)
    }

}