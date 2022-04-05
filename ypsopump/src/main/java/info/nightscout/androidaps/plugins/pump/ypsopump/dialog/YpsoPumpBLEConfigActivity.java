package info.nightscout.androidaps.plugins.pump.ypsopump.dialog;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import info.nightscout.androidaps.activities.NoSplashAppCompatActivity;
import info.nightscout.androidaps.interfaces.ActivePlugin;
import info.nightscout.androidaps.interfaces.Pump;
import info.nightscout.androidaps.plugins.bus.RxBus;
import info.nightscout.androidaps.plugins.pump.common.ble.BlePreCheck;
import info.nightscout.androidaps.plugins.pump.common.driver.PumpBLESelectorInterface;
import info.nightscout.androidaps.plugins.pump.common.driver.PumpBLESelectorText;
import info.nightscout.androidaps.plugins.pump.common.driver.PumpDriverConfigurationCapable;
import info.nightscout.androidaps.plugins.pump.ypsopump.R;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpConst;
import info.nightscout.androidaps.utils.alertDialogs.OKDialog;
import info.nightscout.androidaps.utils.resources.ResourceHelper;
import info.nightscout.shared.logging.AAPSLogger;
import info.nightscout.shared.logging.LTag;
import info.nightscout.shared.sharedPreferences.SP;

// IMPORTANT: This activity needs to be called from RileyLinkSelectPreference (see pref_medtronic.xml as example)
@SuppressLint("MissingPermission")
public class YpsoPumpBLEConfigActivity extends NoSplashAppCompatActivity {

    @Inject AAPSLogger aapsLogger;
    @Inject SP sp;
    @Inject ResourceHelper resourceHelper;
    @Inject BlePreCheck blePrecheck;
    @Inject ActivePlugin activePlugin;
    @Inject RxBus rxBus;
    //@Inject YpsoPumpUtil ypsoPumpUtil;
    //@Inject HasAndroidInjector androidInjector;

    //private static final String TAG = "YpsoPumpBLEConfigActivity";
    private static final LTag TAG = LTag.PUMPBTCOMM;
    private static final long SCAN_PERIOD_MILLIS = 15_000;

    //YpsoPumpBLESelector bleSelector = new YpsoPumpBLESelector(); // TODO refactor this

    private ScanSettings settings;
    private List<ScanFilter> filters;
    private TextView currentlySelectedBTDeviceName;
    private TextView currentlySelectedBTDeviceAddress;
    private Button buttonRemoveBTDevice;
    private Button buttonStartScan;
    private Button buttonStopScan;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private LeDeviceListAdapter deviceListAdapter;
    private Handler handler;
    public boolean scanning;

    PumpBLESelectorInterface bleSelector;

    private Map<String,BluetoothDevice> devicesMap = new HashMap<>();

    private final Runnable stopScanAfterTimeoutRunnable = () -> {
        if (scanning) {
            stopLeDeviceScan();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ypsopump_ble_config_activity);

        if (!blePrecheck.prerequisitesCheck(this)) {
            aapsLogger.error(TAG, "Prerequisites failed. Exiting.");
            return;
        }

        // Configuration
        Pump activePump = activePlugin.getActivePump();

        if (activePump instanceof PumpDriverConfigurationCapable) {
            PumpDriverConfigurationCapable pumpDriverConfigurationCapable = (PumpDriverConfigurationCapable) activePump;
            bleSelector = pumpDriverConfigurationCapable.getPumpDriverConfiguration().getPumpBLESelector();
        } else {
            throw new RuntimeException("YpsoPumpBLEConfigActivity can be used only with PumpDriverConfigurationCapable pump driver.");
        }

        // Initializes Bluetooth adapter.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceListAdapter = new LeDeviceListAdapter();
        handler = new Handler();
        currentlySelectedBTDeviceName = findViewById(R.id.ypsopump_ble_config_currently_selected_pump_name);
        currentlySelectedBTDeviceAddress = findViewById(R.id.ypsopump_ble_config_currently_selected_pump_address);
        buttonRemoveBTDevice = findViewById(R.id.ypsopump_ble_config_button_remove);
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

            aapsLogger.debug(TAG, "Device selected: " + bleAddress);

            if (devicesMap.containsKey(bleAddress)) {
                aapsLogger.debug(TAG, "Device FOUND in deviceMap: " + bleAddress);
                BluetoothDevice bluetoothDevice = devicesMap.get(bleAddress);
                bleSelector.onDeviceSelected(bluetoothDevice, bleAddress, deviceName);
            } else {
                aapsLogger.debug(TAG, "Device NOT found in deviceMap: " + bleAddress);
            }

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

        buttonRemoveBTDevice.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {

                OKDialog.INSTANCE.showConfirmation(YpsoPumpBLEConfigActivity.this,
                        bleSelector.getText(PumpBLESelectorText.REMOVE_TEXT),
                        new Runnable() {
                            @Override public void run() {
                                String device = currentlySelectedBTDeviceAddress.getText().toString();
                                aapsLogger.debug(TAG, "Removing device as selected: " + device);
                                if (devicesMap.containsKey(device)) {
                                    BluetoothDevice bluetoothDevice = devicesMap.get(device);
                                    aapsLogger.debug(TAG, "Device can be detected near, so trying to remove bond if possible.");
                                    bleSelector.removeDevice(bluetoothDevice);
                                }

                                bleSelector.cleanupAfterDeviceRemoved();
                                updateCurrentlySelectedBTDevice();

                            }
                        });
//                        new DialogInterface.OnClickListener() -> {
//                    @Override public void run() {
//                        String device = currentlySelectedBTDeviceAddress.getText().toString();
//                        aapsLogger.debug(TAG, "Removing device as selected: " + device);
//                        if (devicesMap.containsKey(device)) {
//                            BluetoothDevice bluetoothDevice = devicesMap.get(device);
//                            aapsLogger.debug(TAG, "Device can be detected near, so trying to remove bond if possible.");
//                            bleSelector.removeDevice(bluetoothDevice);
//                        }
//
//                        bleSelector.cleanupAfterDeviceRemoved();
//                        updateCurrentlySelectedBTDevice();
//                    }
//                });


            }
        });
    }

//        buttonRemoveBTDevice.setOnClickListener(view -> new AlertDialog.Builder(this)
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .setTitle(getString(R.string.ypsopump_ble_config_remove_riley_link_confirmation_title))
//                .setMessage(getString(R.string.ypsopump_ble_config_remove_riley_link_confirmation))
//                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
//                    String device = this.currentlySelectedBTDeviceAddress.getText().toString();
//                    aapsLogger.debug(TAG, "Removing device as selected: " + device);
//                    if (devicesMap.containsKey(device)) {
//                        BluetoothDevice bluetoothDevice = devicesMap.get(device);
//                        aapsLogger.debug(TAG, "Device can be detected near, so trying to remove bond if possible.");
//                        bleSelector.removeDevice(bluetoothDevice);
//                    }
//
//                    bleSelector.cleanupAfterDeviceRemoved();
//                    updateCurrentlySelectedBTDevice();
//                })
//                .setNegativeButton(getString(R.string.no), null)
//                .show());
//    }


    private void updateCurrentlySelectedBTDevice() {
        String address = sp.getString(YpsoPumpConst.Prefs.PumpAddress, "");
        if (StringUtils.isEmpty(address)) {
            currentlySelectedBTDeviceName.setText(bleSelector.getText(PumpBLESelectorText.NO_SELECTED_PUMP));
            currentlySelectedBTDeviceAddress.setVisibility(View.GONE);
            buttonRemoveBTDevice.setVisibility(View.GONE);
        } else {
            currentlySelectedBTDeviceAddress.setVisibility(View.VISIBLE);
            buttonRemoveBTDevice.setVisibility(View.VISIBLE);

            currentlySelectedBTDeviceName.setText(sp.getString(YpsoPumpConst.Prefs.PumpName, bleSelector.getUnknownPumpName()));
            currentlySelectedBTDeviceAddress.setText(address);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bleSelector.onResume();

        prepareForScanning();

        updateCurrentlySelectedBTDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanning) {
            stopLeDeviceScan();
        }
        bleSelector.onDestroy();
    }

    private void prepareForScanning() {
        //boolean checkOK = blePrecheck.prerequisitesCheck(this);

        //if (checkOK) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            settings = bleSelector.getScanSettings();
            filters = bleSelector.getScanFilters();
        //}
    }

    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult scanRecord) {
            aapsLogger.debug(TAG, scanRecord.toString());

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
                    aapsLogger.debug(TAG, "SCAN: " + result.getAdvertisingSid() + " name=" + result.getDevice().getAddress());
                    if (addDevice(result))
                        added = true;
                }

                if (added)
                    deviceListAdapter.notifyDataSetChanged();
            });
        }

        private boolean addDevice(ScanResult result) {
            BluetoothDevice device = result.getDevice();

            // TODO remove
            // we can either check with beggining MAC address or by name, which would be something like YpsoPump_10000001
            if (!device.getAddress().startsWith("EC:2A:F0")) {
                return false;
            }

            aapsLogger.info(TAG, "Found YpsoPump with address: " + device.getAddress());

            device = bleSelector.filterDevice(device);

            if (device==null) {
                return false;
            }

//            List<ParcelUuid> serviceUuids = result.getScanRecord().getServiceUuids();
//
//            if (serviceUuids == null || serviceUuids.size() == 0) {
//                Log.v(TAG, "Device " + device.getAddress() + " has no serviceUuids (Not YpsoPump).");
//            } else if (serviceUuids.size() > 1) {
//                Log.v(TAG, "Device " + device.getAddress() + " has too many serviceUuids (Not YpsoPump).");
//            } else {
//                String uuid = serviceUuids.get(0).getUuid().toString().toLowerCase();
//                // TODO this might not work correctly
//
//                if (uuid.equals(YpsoGattCharacteristic.PUMP_BASE_SERVICE_VERSION.getUuid())) {
//                    Log.i(TAG, "Found YpsoPump with address: " + device.getAddress());
//                    deviceListAdapter.addDevice(result);
//                    return true;
//                } else {
//                    Log.v(TAG, "Device " + device.getAddress() + " has incorrect uuid (Not YpsoPump).");
//                }
//            }



            deviceListAdapter.addDevice(result);

            if (!devicesMap.containsKey(device.getAddress())) {
                devicesMap.put(device.getAddress(), device);
            }

            return true;
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
            bleSelector.onScanFailed(YpsoPumpBLEConfigActivity.this, errorCode);
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
        bleSelector.onStartLeDeviceScan(YpsoPumpBLEConfigActivity.this);
    }

    private void stopLeDeviceScan() {
        if (scanning) {
            scanning = false;

            bleScanner.stopScan(bleScanCallback);

            aapsLogger.debug(LTag.PUMPBTCOMM, "stopLeDeviceScan: Scanning Stop");
            bleSelector.onStopLeDeviceScan(YpsoPumpBLEConfigActivity.this);
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
                deviceName =  bleSelector.getUnknownPumpName();
            }

            deviceName += " [" + rileyLinkDevices.get(device) + "]";

            String currentlySelectedAddress = bleSelector.currentlySelectedPumpAddress();

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
