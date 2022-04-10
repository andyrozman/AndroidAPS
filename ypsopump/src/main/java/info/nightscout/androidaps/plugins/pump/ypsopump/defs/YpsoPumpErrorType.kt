package info.nightscout.androidaps.plugins.pump.ypsopump.defs

import androidx.annotation.StringRes
import info.nightscout.androidaps.plugins.pump.ypsopump.R

/**
 * Created by andy on 14/05/2018.
 */
enum class YpsoPumpErrorType(@StringRes var resourceId: Int) {

    NoBluetoothAdapter(R.string.ble_error_no_bt_adapter),  //
    BluetoothDisabled(R.string.ble_error_bt_disabled),  //
    FailedToConnectToBleDevice(R.string.ble_error_failed_to_conn_to_ble_device),
    ConfiguredPumpNotFound(R.string.ble_error_configured_pump_not_found),  //
    PumpFoundUnbonded(R.string.ble_error_pump_found_unbonded),
    PumpUnreachable(R.string.ble_error_pump_unreachable), //
    DeviceIsNotPump(R.string.ypsopump_error_not_correct_pump),  //
    EncryptionFailed(R.string.ble_error_encryption_failed)
    ;

}