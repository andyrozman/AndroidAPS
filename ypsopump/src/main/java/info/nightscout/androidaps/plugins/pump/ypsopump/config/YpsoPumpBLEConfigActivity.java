package info.nightscout.androidaps.plugins.pump.ypsopump.config;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import info.nightscout.androidaps.activities.NoSplashAppCompatActivity;
import info.nightscout.androidaps.interfaces.ActivePlugin;
import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.logging.LTag;
import info.nightscout.androidaps.plugins.bus.RxBusWrapper;
import info.nightscout.androidaps.plugins.pump.common.ble.BlePreCheck;
import info.nightscout.androidaps.plugins.pump.ypsopump.R;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic;
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpConfigurationChanged;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpConst;
import info.nightscout.androidaps.utils.resources.ResourceHelper;
import info.nightscout.androidaps.utils.sharedPreferences.SP;

// IMPORTANT: This activity needs to be called from RileyLinkSelectPreference (see pref_medtronic.xml as example)
public class YpsoPumpBLEConfigActivity extends NoSplashAppCompatActivity {

    @Inject AAPSLogger aapsLogger;
    @Inject SP sp;
    @Inject ResourceHelper resourceHelper;
    @Inject BlePreCheck blePrecheck;
    @Inject ActivePlugin activePlugin;
    @Inject RxBusWrapper rxBus;

    private static final String TAG = "RileyLinkBLEConfigActivity";
    private static final long SCAN_PERIOD_MILLIS = 15_000;

    private ScanSettings settings;
    private List<ScanFilter> filters;
    private TextView currentlySelectedRileyLinkName;
    private TextView currentlySelectedRileyLinkAddress;
    private Button buttonRemoveRileyLink;
    private Button buttonStartScan;
    private Button buttonStopScan;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private LeDeviceListAdapter deviceListAdapter;
    private Handler handler;
    public boolean scanning;

    private final Runnable stopScanAfterTimeoutRunnable = () -> {
        if (scanning) {
            stopLeDeviceScan();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ypsopump_ble_config_activity);

        // Initializes Bluetooth adapter.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceListAdapter = new LeDeviceListAdapter();
        handler = new Handler();
        currentlySelectedRileyLinkName = findViewById(R.id.ypsopump_ble_config_currently_selected_pump_name);
        currentlySelectedRileyLinkAddress = findViewById(R.id.ypsopump_ble_config_currently_selected_pump_address);
        buttonRemoveRileyLink = findViewById(R.id.ypsopump_ble_config_button_remove);
        buttonStartScan = findViewById(R.id.ypsopump_ble_config_scan_start);
        buttonStopScan = findViewById(R.id.ypsopump_ble_config_button_scan_stop);
        ListView deviceList = findViewById(R.id.ypsopump_ble_config_scan_device_list);
        deviceList.setAdapter(deviceListAdapter);
        deviceList.setOnItemClickListener((parent, view, position, id) -> {
            // stop scanning if still active
            if (scanning) {
                stopLeDeviceScan();
            }

            String bleAddress = ((TextView) view.findViewById(R.id.ypsopump_ble_config_scan_item_device_address)).getText().toString();
            String deviceName = ((TextView) view.findViewById(R.id.ypsopump_ble_config_scan_item_device_name)).getText().toString();

            sp.putString(YpsoPumpConst.Prefs.PumpAddress, bleAddress);
            sp.putString(YpsoPumpConst.Prefs.PumpName, deviceName);

            // we need to trigger event for reconfiguration
//            RileyLinkPumpDevice rileyLinkPump = (RileyLinkPumpDevice) activePlugin.getActivePump();
//            rileyLinkPump.getRileyLinkService().verifyConfiguration(true); // force reloading of address to assure that the RL gets reconnected (even if the address didn't change)
//            rileyLinkPump.triggerPumpConfigurationChangedEvent();

            // TODO when device is selected, we need to Bond it, and if there is any other pump Bonded, it needs to be unbonded
            rxBus.send(new EventPumpConfigurationChanged());

            finish();
        });

        buttonStartScan.setOnClickListener(view -> {
            startLeDeviceScan();
        });

        buttonStopScan.setOnClickListener(view -> {
            if (scanning) {
                stopLeDeviceScan();
            }
        });

        buttonRemoveRileyLink.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.ypsopump_ble_config_remove_riley_link_confirmation_title))
                .setMessage(getString(R.string.ypsopump_ble_config_remove_riley_link_confirmation))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    sp.remove(YpsoPumpConst.Prefs.PumpAddress);
                    sp.remove(YpsoPumpConst.Prefs.PumpName);
                    updateCurrentlySelectedRileyLink();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show());
    }

    private void updateCurrentlySelectedRileyLink() {
        String address = sp.getString(YpsoPumpConst.Prefs.PumpAddress, "");
        if (StringUtils.isEmpty(address)) {
            currentlySelectedRileyLinkName.setText(R.string.ypsopump_ble_config_no_ypsopump_selected);
            currentlySelectedRileyLinkAddress.setVisibility(View.GONE);
            buttonRemoveRileyLink.setVisibility(View.GONE);
        } else {
            currentlySelectedRileyLinkAddress.setVisibility(View.VISIBLE);
            buttonRemoveRileyLink.setVisibility(View.VISIBLE);

            currentlySelectedRileyLinkName.setText(sp.getString(YpsoPumpConst.Prefs.PumpName, "YpsoPump (?)"));
            currentlySelectedRileyLinkAddress.setText(address);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        prepareForScanning();

        updateCurrentlySelectedRileyLink();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (scanning) {
            stopLeDeviceScan();
        }
    }

    private void prepareForScanning() {
        boolean checkOK = blePrecheck.prerequisitesCheck(this);

        if (checkOK) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            filters = Collections.singletonList(new ScanFilter.Builder().setServiceUuid(
                    ParcelUuid.fromString(YpsoGattCharacteristic.PUMP_BASE_SERVICE_VERSION.getUuid())).build());
        }
    }

    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult scanRecord) {
            Log.d(TAG, scanRecord.toString());

            runOnUiThread(() -> {
                if (addDevice(scanRecord))
                    deviceListAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            runOnUiThread(() -> {
                boolean added = false;

                for (ScanResult result : results) {
                    if (addDevice(result))
                        added = true;
                }

                if (added)
                    deviceListAdapter.notifyDataSetChanged();
            });
        }

        private boolean addDevice(ScanResult result) {
            BluetoothDevice device = result.getDevice();

            List<ParcelUuid> serviceUuids = result.getScanRecord().getServiceUuids();

            if (serviceUuids == null || serviceUuids.size() == 0) {
                Log.v(TAG, "Device " + device.getAddress() + " has no serviceUuids (Not YpsoPump).");
            } else if (serviceUuids.size() > 1) {
                Log.v(TAG, "Device " + device.getAddress() + " has too many serviceUuids (Not YpsoPump).");
            } else {
                String uuid = serviceUuids.get(0).getUuid().toString().toLowerCase();
                // TODO this might not work correctly

                if (uuid.equals(YpsoGattCharacteristic.PUMP_BASE_SERVICE_VERSION.getUuid())) {
                    Log.i(TAG, "Found YpsoPump with address: " + device.getAddress());
                    deviceListAdapter.addDevice(result);
                    return true;
                } else {
                    Log.v(TAG, "Device " + device.getAddress() + " has incorrect uuid (Not YpsoPump).");
                }
            }

            return false;
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
            Toast.makeText(YpsoPumpBLEConfigActivity.this, resourceHelper.gs(R.string.ble_config_scan_error, errorCode),
                    Toast.LENGTH_LONG).show();
        }

    };

    private void startLeDeviceScan() {
        if (bleScanner == null) {
            aapsLogger.error(LTag.PUMPBTCOMM, "startLeDeviceScan failed: bleScanner is null");
            return;
        }

        deviceListAdapter.clear();
        deviceListAdapter.notifyDataSetChanged();

        handler.postDelayed(stopScanAfterTimeoutRunnable, SCAN_PERIOD_MILLIS);

        buttonStartScan.setEnabled(false);
        buttonStopScan.setVisibility(View.VISIBLE);

        scanning = true;
        bleScanner.startScan(filters, settings, bleScanCallback);
        aapsLogger.debug(LTag.PUMPBTCOMM, "startLeDeviceScan: Scanning Start");
        Toast.makeText(YpsoPumpBLEConfigActivity.this, R.string.ble_config_scan_scanning, Toast.LENGTH_SHORT).show();
    }

    private void stopLeDeviceScan() {
        if (scanning) {
            scanning = false;

            bleScanner.stopScan(bleScanCallback);

            aapsLogger.debug(LTag.PUMPBTCOMM, "stopLeDeviceScan: Scanning Stop");
            Toast.makeText(this, R.string.ble_config_scan_finished, Toast.LENGTH_SHORT).show();
            handler.removeCallbacks(stopScanAfterTimeoutRunnable);
        }

        buttonStartScan.setEnabled(true);
        buttonStopScan.setVisibility(View.GONE);
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private final ArrayList<BluetoothDevice> mLeDevices;
        private final Map<BluetoothDevice, Integer> rileyLinkDevices;
        private final LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            rileyLinkDevices = new HashMap<>();
            mInflator = YpsoPumpBLEConfigActivity.this.getLayoutInflater();
        }

        public void addDevice(ScanResult result) {
            if (!mLeDevices.contains(result.getDevice())) {
                mLeDevices.add(result.getDevice());
            }
            rileyLinkDevices.put(result.getDevice(), result.getRssi());
            notifyDataSetChanged();
        }

        public void clear() {
            mLeDevices.clear();
            rileyLinkDevices.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.ypsopump_ble_config_scan_item, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.ypsopump_ble_config_scan_item_device_address);
                viewHolder.deviceName = view.findViewById(R.id.ypsopump_ble_config_scan_item_device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            String deviceName = device.getName();

            if (StringUtils.isBlank(deviceName)) {
                deviceName = "RileyLink (?)";
            }

            deviceName += " [" + rileyLinkDevices.get(device) + "]";

            String currentlySelectedAddress = sp.getString(YpsoPumpConst.Prefs.PumpAddress, "");

            if (currentlySelectedAddress.equals(device.getAddress())) {
                deviceName += " (" + getResources().getString(R.string.ble_config_scan_selected) + ")";
            }

            viewHolder.deviceName.setText(deviceName);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

}
