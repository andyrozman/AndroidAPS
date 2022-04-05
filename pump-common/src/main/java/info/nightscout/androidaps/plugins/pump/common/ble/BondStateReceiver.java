package info.nightscout.androidaps.plugins.pump.common.ble;

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.ERROR;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.StringRes;

import com.google.gson.Gson;

import javax.inject.Inject;

import dagger.android.DaggerBroadcastReceiver;
import info.nightscout.androidaps.utils.resources.ResourceHelper;
import info.nightscout.shared.logging.AAPSLogger;
import info.nightscout.shared.sharedPreferences.SP;

public class BondStateReceiver extends DaggerBroadcastReceiver {

    @Inject SP sp;
    @Inject Context context;
    @Inject ResourceHelper rh;
    @Inject AAPSLogger aapsLogger;

    public String TAG = "PUMPCOMM";
    Gson gson = new Gson();

    @StringRes int deviceAddress;
    @StringRes int bondedFlag;
    int targetState;
    String targetDevice;

    Context applicationContext;

    public BondStateReceiver(@StringRes int deviceAddress, @StringRes int bondedFlag,
                             String targetDevice, int targetState) {
        this.deviceAddress = deviceAddress;
        this.bondedFlag = bondedFlag;
        this.targetDevice = targetDevice;
        this.targetState = targetState;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        final String action = intent.getAction();
        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        Log.i(TAG, "in onReceive:  INTENT" + gson.toJson(intent));

        if (device == null) {
            Log.e(TAG, "onReceive. Device is null. Exiting.");
            return;
        } else {
            if (!device.getAddress().equals(this.targetDevice)) {
                Log.e(TAG, "onReceive. Device is not the same as targetDevice. Exiting.");
                return;
            }
        }

        // Check if action is valid
        if (action == null) return;

        // Take action depending on new bond state
        if (action.equals(ACTION_BOND_STATE_CHANGED)) {
            final int bondState = intent.getIntExtra(EXTRA_BOND_STATE, ERROR);
            final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

            Log.i(TAG, "in onReceive:  bondState=" + bondState);
            Log.i(TAG, "in onReceive: previousBondState=" + previousBondState);

            if (bondState == targetState) {
                Log.i(TAG, "onReceive:  found targeted state: " + targetState);

                String currentDeviceSettings = sp.getString(deviceAddress, "");

                if (currentDeviceSettings.equals(targetDevice)) {
                    if (targetState == 12) {
                        sp.putBoolean(this.bondedFlag, true);
                    } else if (targetState == 10) {
                        sp.putBoolean(this.bondedFlag, false);
                    }
                    context.unregisterReceiver(this);
                } else {
                    Log.e(TAG, "onReceive:  Device stored in SP is not the same as target device, process interrupted");
                }

            } else {
                Log.i(TAG, "onReceive:  currentBondState=" + bondState + ", targetBondState=" + targetState);
            }
        }
    }


}
