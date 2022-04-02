package info.nightscout.androidaps.plugins.pump.ypsopump.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import info.nightscout.androidaps.activities.NoSplashAppCompatActivity
import info.nightscout.androidaps.interfaces.ActivePlugin
import info.nightscout.androidaps.plugins.pump.common.ble.BlePreCheck
import info.nightscout.androidaps.plugins.pump.common.events.EventPumpChanged
import info.nightscout.androidaps.plugins.pump.ypsopump.R
import info.nightscout.androidaps.plugins.pump.ypsopump.databinding.YpsopumpBleConfigActivityBinding
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpConst
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.shared.logging.LTag
import info.nightscout.shared.sharedPreferences.SP
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject

// IMPORTANT: This activity needs to be called from RileyLinkSelectPreference (see pref_medtronic.xml as example)
@SuppressLint("MissingPermission")
class YpsoPumpBLEConfigActivity2 : NoSplashAppCompatActivity() {

    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var activePlugin: ActivePlugin
    @Inject lateinit var sp: SP
    @Inject lateinit var blePreCheck: BlePreCheck
    @Inject lateinit var context: Context
    //@Inject lateinit var rxBus: RxBus

    private lateinit var binding: YpsopumpBleConfigActivityBinding

    private var settings: ScanSettings? = null
    private val filters: List<ScanFilter>? = null

    //private var currentlySelectedBTDeviceName: TextView? = null
    //private var currentlySelectedBTDeviceAddress: TextView? = null
    //private var buttonRemoveBTDevice: Button? = null
    //private var buttonStartScan: Button? = null
    //private var buttonStopScan: Button? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var deviceListAdapter: LeDeviceListAdapter? = null
    private var handler: Handler? = null
    var scanning = false
    private val devicesMap: MutableMap<String, BluetoothDevice> = HashMap()
    private val stopScanAfterTimeoutRunnable = Runnable {
        if (scanning) {
            stopLeDeviceScan()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = YpsopumpBleConfigActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!blePreCheck.prerequisitesCheck(this)) {
            aapsLogger.error(TAG, "prerequisitesCheck failed.")
            return;
        }

        // Initializes Bluetooth adapter.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        deviceListAdapter = LeDeviceListAdapter()
        handler = Handler()
        // currentlySelectedBTDeviceName = findViewById(R.id.ypsopump_ble_config_currently_selected_pump_name)
        // currentlySelectedBTDeviceAddress = findViewById(R.id.ypsopump_ble_config_currently_selected_pump_address)
        // buttonRemoveBTDevice = findViewById(R.id.ypsopump_ble_config_button_remove)
        // buttonStartScan = findViewById(R.id.ypsopump_ble_config_scan_start)
        // buttonStopScan = findViewById(R.id.ypsopump_ble_config_button_scan_stop)
        val deviceList = binding.ypsopumpBleConfigScanDeviceList //   findViewById<ListView>(R.id.ypsopump_ble_config_scan_device_list)
        deviceList.adapter = deviceListAdapter
        deviceList.onItemClickListener = OnItemClickListener { parent: AdapterView<*>?, view: View, position: Int, id: Long ->
            // stop scanning if still active
            if (scanning) {
                stopLeDeviceScan()
            }
            val bleAddress = (view.findViewById<View>(R.id.ypsopump_ble_config_scan_item_device_address) as TextView).text.toString()
            val deviceName = (view.findViewById<View>(R.id.ypsopump_ble_config_scan_item_device_name) as TextView).text.toString()

//            sp.putString(YpsoPumpConst.Prefs.PumpAddress, bleAddress);
//            sp.putString(YpsoPumpConst.Prefs.PumpName, deviceName);
            aapsLogger!!.debug(TAG, "Device selected: $bleAddress")
            if (devicesMap.containsKey(bleAddress)) {
                aapsLogger!!.debug(TAG, "Device FOUND in deviceMap: $bleAddress")
                val bluetoothDevice = devicesMap[bleAddress]
                val bondState = bluetoothDevice!!.bondState
                aapsLogger!!.debug(TAG, "Device bonding status: " + bluetoothDevice.bondState + " desc: " + getBondingStatusDescription(bluetoothDevice.bondState))

                // if we are not bonded, bonding is started
                if (bondState != 12) {
                    // TODO create bond
                    bluetoothDevice.createBond()
                }
                var addressChanged = false

                // set pump address
                if (setSystemParameterForBT(YpsoPumpConst.Prefs.PumpAddress, bleAddress)) {
                    addressChanged = true
                }
                setSystemParameterForBT(YpsoPumpConst.Prefs.PumpName, deviceName)
                var name = bluetoothDevice.name
                if (name.contains("_")) {
                    name = name.substring(name.indexOf("_") + 1)
                    setSystemParameterForBT(YpsoPumpConst.Prefs.PumpSerial, name)
                }
                if (addressChanged) {
                    // TODO send notification to pumpSync
                    rxBus.send(EventPumpChanged(name, bleAddress, null))
                }
            } else {
                aapsLogger.debug(TAG, "Device NOT found in deviceMap: $bleAddress")
            }
            finish()
        }
        binding.ypsopumpBleConfigScanStart.setOnClickListener({ view: View? -> startLeDeviceScan() })
        binding.ypsopumpBleConfigButtonScanStop.setOnClickListener { view: View? ->
            if (scanning) {
                stopLeDeviceScan()
            }
        }

        binding.ypsopumpBleConfigButtonRemove.setOnClickListener(View.OnClickListener { view: View? ->
            AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.ypsopump_ble_config_remove_riley_link_confirmation_title))
                .setMessage(getString(R.string.ypsopump_ble_config_remove_riley_link_confirmation))
                .setPositiveButton(getString(R.string.yes)) { dialog: DialogInterface?, which: Int ->
                    val device = binding.ypsopumpBleConfigCurrentlySelectedPumpAddress.text.toString()
                    aapsLogger!!.debug(TAG, "Removing device as selected: $device")
                    if (devicesMap.containsKey(device)) {
                        val bluetoothDevice = devicesMap[device]
                        aapsLogger!!.debug(TAG, "Device can be detected near, so trying to remove bond if possible.")
                        if (bluetoothDevice!!.bondState == 12) {
                            // TODO remove bond
                            //bluetoothDevice.removeBond();
                        }
                    }
                    sp!!.remove(YpsoPumpConst.Prefs.PumpAddress)
                    sp!!.remove(YpsoPumpConst.Prefs.PumpName)
                    updateCurrentlySelectedBTDevice()
                }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        })
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
            sp!!.putString(parameter, newValue)
        }
        return false
    }

    private fun getBondingStatusDescription(state: Int): String {
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

    private fun updateCurrentlySelectedBTDevice() {
        val address = sp.getString(YpsoPumpConst.Prefs.PumpAddress, "")
        if (StringUtils.isEmpty(address)) {
            binding.ypsopumpBleConfigCurrentlySelectedPumpName.setText(R.string.ypsopump_ble_config_no_ypsopump_selected)
            binding.ypsopumpBleConfigCurrentlySelectedPumpAddress.visibility = View.GONE
            binding.ypsopumpBleConfigButtonRemove.visibility = View.GONE
        } else {
            binding.ypsopumpBleConfigCurrentlySelectedPumpAddress.visibility = View.VISIBLE
            binding.ypsopumpBleConfigButtonRemove.visibility = View.VISIBLE
            binding.ypsopumpBleConfigCurrentlySelectedPumpName.text = sp.getString(YpsoPumpConst.Prefs.PumpName, "YpsoPump (?)")
            binding.ypsopumpBleConfigCurrentlySelectedPumpAddress.text = address
        }
    }

    override fun onResume() {
        super.onResume()
        prepareForScanning()
        updateCurrentlySelectedBTDevice()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (scanning) {
            stopLeDeviceScan()
        }
    }

    private fun prepareForScanning() {
        //val checkOK = blePrecheck!!.prerequisitesCheck(this)
        //if (checkOK) {
        bleScanner = bluetoothAdapter!!.bluetoothLeScanner
        settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        //            filters = Collections.singletonList(new ScanFilter.Builder().setServiceUuid(
//                    ParcelUuid.fromString(YpsoGattService.SERVICE_YPSO_1.getUuid())).build());
        //}
    }

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, scanRecord: ScanResult) {
            aapsLogger!!.debug(TAG, scanRecord.toString())
            //runOnUiThread { if (addDevice(scanRecord)) deviceListAdapter!!.notifyDataSetChanged() }
            if (addDevice(scanRecord)) {
                Handler(Looper.getMainLooper()).post { deviceListAdapter?.notifyDataSetChanged() }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            runOnUiThread {
                var added = false
                for (result in results) {
                    aapsLogger!!.debug(TAG, "SCAN: " + result.advertisingSid + " name=" + result.device.address)
                    if (addDevice(result)) added = true
                }
                if (added) deviceListAdapter!!.notifyDataSetChanged()
            }
        }

        private fun addDevice(result: ScanResult): Boolean {
            val device = result.device

            // we can either check with beggining MAC address or by name, which would be something like YpsoPump_10000001
            if (!device.address.startsWith("EC:2A:F0")) {
                return false
            }
            aapsLogger.info(TAG, "Found YpsoPump with address: " + device.address)

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
            deviceListAdapter!!.addDevice(result)
            if (!devicesMap.containsKey(device.address)) {
                devicesMap[device.address] = device
            }
            return true
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("Scan Failed", "Error Code: $errorCode")
            Toast.makeText(
                this@YpsoPumpBLEConfigActivity2, resourceHelper!!.gs(R.string.ble_config_scan_error, errorCode),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startLeDeviceScan() {
        if (bleScanner == null) {
            aapsLogger.error(LTag.PUMPBTCOMM, "startLeDeviceScan failed: bleScanner is null")
            return
        }
        deviceListAdapter!!.clear()
        deviceListAdapter!!.notifyDataSetChanged()
        handler!!.postDelayed(stopScanAfterTimeoutRunnable, SCAN_PERIOD_MILLIS)
        binding.ypsopumpBleConfigScanStart.isEnabled = false
        binding.ypsopumpBleConfigButtonScanStop.visibility = View.VISIBLE
        scanning = true
        bleScanner!!.startScan(filters, settings, bleScanCallback)
        aapsLogger!!.debug(LTag.PUMPBTCOMM, "startLeDeviceScan: Scanning Start")
        Toast.makeText(this@YpsoPumpBLEConfigActivity2, R.string.ble_config_scan_scanning, Toast.LENGTH_SHORT).show()
    }

    private fun stopLeDeviceScan() {
        if (scanning) {
            scanning = false
            bleScanner!!.stopScan(bleScanCallback)
            aapsLogger!!.debug(LTag.PUMPBTCOMM, "stopLeDeviceScan: Scanning Stop")
            Toast.makeText(this, R.string.ble_config_scan_finished, Toast.LENGTH_SHORT).show()
            handler!!.removeCallbacks(stopScanAfterTimeoutRunnable)
        }
        binding.ypsopumpBleConfigScanStart.isEnabled = true
        binding.ypsopumpBleConfigButtonScanStop.visibility = View.GONE
    }

    private inner class LeDeviceListAdapter : BaseAdapter() {

        private var mLeDevices: ArrayList<BluetoothDevice> = arrayListOf()
        private var rileyLinkDevices: MutableMap<BluetoothDevice, Int> = mutableMapOf()

        //private val mInflator: LayoutInflater
        fun addDevice(result: ScanResult) {
            if (!mLeDevices.contains(result.device)) {
                mLeDevices.add(result.device)
            }
            rileyLinkDevices[result.device] = result.rssi
            notifyDataSetChanged()
        }

        fun clear() {
            mLeDevices.clear()
            rileyLinkDevices.clear()
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        // override fun getView(i: Int, convertView: View?, parent: ViewGroup?): View {
        //     var v = convertView
        //     val holder: ViewHolder
        //     if (v == null) {
        //         v = View.inflate(applicationContext, R.layout.danars_blescanner_item, null)
        //         holder = ViewHolder(v)
        //         v.tag = holder
        //     } else {
        //         // reuse view if already exists
        //         holder = v.tag as ViewHolder
        //     }
        //     val item = getItem(i)
        //     holder.setData(item)
        //     return v!!
        // }

        override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup?): View {

            var v = convertView
            val holder: ViewHolder
            if (v == null) {
                v = View.inflate(applicationContext, R.layout.ypsopump_ble_config_scan_item, null)
                holder = ViewHolder()
                holder.deviceAddress = v.findViewById(R.id.ypsopump_ble_config_scan_item_device_address)
                holder.deviceName = v.findViewById(R.id.ypsopump_ble_config_scan_item_device_name)
                v.tag = holder
            } else {
                // reuse view if already exists
                holder = v.tag as ViewHolder
            }
            // val item = getItem(i)
            // holder.setData(item)
            // return v!!

            val device = mLeDevices[i]
            var deviceName = device.name
            if (StringUtils.isBlank(deviceName)) {
                deviceName = "Ypsopump (?)"
            }
            deviceName += " [" + rileyLinkDevices[device] + "]"
            val currentlySelectedAddress = sp.getString(YpsoPumpConst.Prefs.PumpAddress, "")
            if (currentlySelectedAddress == device.address) {
                deviceName += " (" + resources.getString(R.string.ble_config_scan_selected) + ")"
            }
            holder.deviceName!!.text = deviceName
            holder.deviceAddress!!.text = device.address
            return v!!

            //
            //
            //
            // //var view = view
            // val viewHolder: ViewHolder
            // // General ListView optimization code.
            // if (view.tag == null) {
            //     //view = mInflator.inflate(R.layout.ypsopump_ble_config_scan_item, null)
            //     viewHolder = ViewHolder()
            //     viewHolder.deviceAddress = view.findViewById(R.id.ypsopump_ble_config_scan_item_device_address)
            //     viewHolder.deviceName = view.findViewById(R.id.ypsopump_ble_config_scan_item_device_name)
            //     view.tag = viewHolder
            // } else {
            //     viewHolder = view.tag as ViewHolder
            // }
            // //viewHolder = view.tag as ViewHolder
            // val device = mLeDevices[i]
            // var deviceName = device.name
            // if (StringUtils.isBlank(deviceName)) {
            //     deviceName = "Ypsopump (?)"
            // }
            // deviceName += " [" + rileyLinkDevices[device] + "]"
            // val currentlySelectedAddress = sp.getString(YpsoPumpConst.Prefs.PumpAddress, "")
            // if (currentlySelectedAddress == device.address) {
            //     deviceName += " (" + resources.getString(R.string.ble_config_scan_selected) + ")"
            // }
            // viewHolder.deviceName!!.text = deviceName
            // viewHolder.deviceAddress!!.text = device.address
            // return view
        }

        init {
            mLeDevices = ArrayList()
            rileyLinkDevices = HashMap()
            //mInflator = this@YpsoPumpBLEConfigActivity.layoutInflater
        }
    }

    internal class ViewHolder {

        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }

    companion object {

        //@Inject lateinit var blePreCheck: BlePreCheck;
        //private static final String TAG = "YpsoPumpBLEConfigActivity";
        private val TAG = LTag.PUMPBTCOMM
        private const val SCAN_PERIOD_MILLIS: Long = 15000
    }
}