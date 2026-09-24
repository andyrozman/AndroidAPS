package app.aaps.pump.tandem.mobi.ui.wizard

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.pump.tandem.common.comm.maint.TandemPairingManager
import app.aaps.pump.tandem.common.events.EventTandemPairingStatus
import app.aaps.pump.tandem.common.events.PairingError
import app.aaps.pump.tandem.common.keys.TandemIntPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import com.jwoglom.pumpx2.pump.messages.bluetooth.ServiceUUID
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import dev.zacsweers.metro.Inject

/**
 * ViewModel for the Tandem Mobi connection wizard
 */
class TandemMobiConnectionWizardViewModel @Inject constructor(
    private val aapsLogger: AAPSLogger,
    private val rxBus: RxBus,
    private val aapsSchedulers: AapsSchedulers,
    private val preferences: Preferences,
    private val tandemPumpUtil: TandemPumpUtil
) : ViewModel() {

    private val _state = MutableStateFlow(TandemMobiWizardState())
    val state: StateFlow<TandemMobiWizardState> = _state.asStateFlow()

    private val disposable = CompositeDisposable()
    private var pairingManager: TandemPairingManager? = null

    // BLE scanning properties
    private var bleScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private val scannedDevicesMap = ConcurrentHashMap<String, ScannedDevice>()

    init {
        // TODO Metro
        // Subscribe to pairing status events
        // disposable += rxBus
        //     .toObservable(EventTandemPairingStatus::class.java)
        //     .observeOn(aapsSchedulers.main)
        //     .subscribe({ event ->
        //         handlePairingEvent(event)
        //     }, { throwable ->
        //         aapsLogger.error(LTag.PUMP, "Error receiving pairing event", throwable)
        //     })
    }

    fun setPairingManager(manager: TandemPairingManager) {
        this.pairingManager = manager
    }

    // Initialize BLE scanner
    fun initializeBLEScanner(bluetoothAdapter: BluetoothAdapter?) {
        bleScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    // Start BLE device scanning
    fun startDeviceScan() {
        aapsLogger.info(LTag.PUMP, "Starting BLE device scan")
        // Clear previous results
        scannedDevicesMap.clear()
        _state.update { it.copy(isScanning = true, scannedDevices = emptyList(), scanError = null) }

        // Setup scan callback
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleScanResult(result)
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { handleScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                aapsLogger.error(LTag.PUMP, "BLE scan failed with error code: $errorCode")
                _state.update { it.copy(
                    isScanning = false,
                    scanError = "Scan failed: $errorCode"
                )}
            }
        }
        scanCallback = callback

        // Create filters (ServiceUUID.PUMP_SERVICE_UUID)
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(ServiceUUID.PUMP_SERVICE_UUID))
            .build()

        // Create settings
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)
            .build()

        val scanner = bleScanner
        if (scanner == null) {
            aapsLogger.error(LTag.PUMP, "BLE scanner unavailable (Bluetooth off or not initialized)")
            _state.update { it.copy(
                isScanning = false,
                scanError = "Bluetooth is unavailable. Please enable Bluetooth and try again."
            )}
            return
        }

        // Start scan with 15-second timeout
        try {
            scanner.startScan(listOf(filter), settings, callback)
        } catch (e: SecurityException) {
            aapsLogger.error(LTag.PUMP, "Missing BLUETOOTH_SCAN permission for BLE scan", e)
            _state.update { it.copy(
                isScanning = false,
                scanError = "Missing BLUETOOTH_SCAN permission. Please grant Bluetooth permissions and try again."
            )}
            return
        }

        viewModelScope.launch {
            delay(15000) // 15 second timeout
            if (_state.value.isScanning) {
                aapsLogger.info(LTag.PUMP, "BLE scan timeout reached, stopping scan")
                stopDeviceScan()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val scannedDevice = ScannedDevice(
            address = device.address,
            name = device.name ?: "Tandem (?)",
            rssi = result.rssi,
            device = device,
            isCurrentlySelected = device.address == tandemPumpUtil.getStringPreferenceOrDefault(TandemStringPreferenceKey.PumpAddress, "")
        )

        scannedDevicesMap[device.address] = scannedDevice

        // Update state with sorted list (by RSSI, strongest first)
        _state.update { it.copy(
            scannedDevices = scannedDevicesMap.values.sortedByDescending { it.rssi }
        )}
    }

    fun onBlePermissionDenied() {
        aapsLogger.error(LTag.PUMP, "Bluetooth scan permission denied by user")
        _state.update { it.copy(
            isScanning = false,
            scanError = "Bluetooth permissions are required to scan for pumps. Please grant them in app settings."
        )}
    }

    fun stopDeviceScan() {
        aapsLogger.info(LTag.PUMP, "Stopping BLE device scan")
        try {
            scanCallback?.let { bleScanner?.stopScan(it) }
        } catch (e: SecurityException) {
            aapsLogger.error(LTag.PUMP, "Missing BLUETOOTH_SCAN permission for stopScan", e)
        }
        _state.update { it.copy(isScanning = false) }
    }

    fun onDeviceSelectedFromList(scannedDevice: ScannedDevice) {
        aapsLogger.info(LTag.PUMP, "Device selected from list: ${scannedDevice.name} (${scannedDevice.address})")
        // Save to preferences
        preferences.put(TandemStringPreferenceKey.PumpAddress, scannedDevice.address)
        preferences.put(TandemStringPreferenceKey.PumpName, scannedDevice.name)

        // Update state (navigation handled externally)
        _state.update { it.copy(
            deviceAddress = scannedDevice.address,
            deviceName = scannedDevice.name
        )}
    }

    fun onBluetoothPermissionsGranted() {
        _state.update { it.copy(hasBluetoothPermission = true) }
    }

    fun checkBluetoothEnabled(adapter: BluetoothAdapter?): Boolean {
        val enabled = adapter?.isEnabled == true
        _state.update { it.copy(isBluetoothEnabled = enabled) }
        return enabled
    }

    fun onPINChanged(pin: String) {
        // Only allow numeric input, max 6 digits
        val filtered = pin.filter { it.isDigit() }.take(6)
        _state.update { it.copy(enteredPIN = filtered) }
    }

    fun startPairingWithCode(pin: String) {
        if (pin.length == 6) {
            aapsLogger.info(LTag.PUMP, "PIN entered, starting pairing")
            startPairing(pin)
        }
    }

    private fun startPairing(pin: String) {
        viewModelScope.launch {
            pairingManager?.startPairingWithCode(pin)
                ?: run {
                    aapsLogger.error(LTag.PUMP, "PairingManager not set!")
                    _state.update {
                        it.copy(pairingError = PairingError.UnknownError)
                    }
                    return@launch
                }

            // Monitor pairing timeout
            monitorPairingTimeout()
        }
    }

    private suspend fun monitorPairingTimeout() {
        var elapsedTime = 0L
        val checkInterval = 1000L // Check every second
        val timeout = 30000L // 30 second timeout

        while (elapsedTime < timeout && _state.value.pairingStatus < 100 && _state.value.pairingError == null) {
            delay(checkInterval)
            elapsedTime += checkInterval

            // Update pairing status from preferences
            val status = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.PumpPairStatus, -1)
            _state.update { it.copy(pairingStatus = status) }

            if (status == 100) {
                // Success - event will be handled in handlePairingEvent
                break
            }
        }

        // If we've reached timeout and pairing hasn't completed, send timeout error
        if (elapsedTime >= timeout && _state.value.pairingStatus < 100 && _state.value.pairingError == null) {
            aapsLogger.error(LTag.PUMP, "Pairing timeout in ViewModel after ${timeout}ms")
            _state.update { it.copy(pairingError = PairingError.ConnectionTimeout) }
        }
    }

    private fun handlePairingEvent(event: EventTandemPairingStatus) {
        aapsLogger.info(LTag.PUMP, "Received pairing event: ${event.javaClass.simpleName}")

        when (event) {
            is EventTandemPairingStatus.PairingStarted -> {
                _state.update { it.copy(pairingStatus = 0) }
            }
            is EventTandemPairingStatus.WaitingForCode -> {
                _state.update { it.copy(pairingStatus = 40) }
            }
            is EventTandemPairingStatus.PairingInProgress -> {
                _state.update { it.copy(
                    pairingStatus = 50 + (event.progressPercent / 2),
                    pairingLabel = event.progressLabel
                ) }
            }
            is EventTandemPairingStatus.PairingSuccess -> {
                aapsLogger.info(LTag.PUMP, "Pairing successful: ${event.pumpSerial}")
                _state.update { it.copy(
                    pairingStatus = 100,
                    pairedPumpSerial = event.pumpSerial,
                    pairedPumpName = event.pumpName,
                    pairedPumpApiVersion = event.pumpApiVersion,
                )}
            }
            is EventTandemPairingStatus.PairingFailed -> {
                aapsLogger.error(LTag.PUMP, "Pairing failed: ${event.error}")
                _state.update {
                    it.copy(
                        pairingError = event.error,
                        retryCount = it.retryCount + 1
                    )
                }
            }
        }
    }

    fun onRetryPairing() {
        aapsLogger.info(LTag.PUMP, "Retrying pairing with same PIN")
        pairingManager?.clearPairingData()
        _state.update { it.copy(pairingError = null) }
        startPairing(_state.value.enteredPIN)
    }

    fun onEditPIN() {
        aapsLogger.info(LTag.PUMP, "User wants to edit PIN")
        _state.update { it.copy(pairingError = null) }
    }

    fun onCancelAndRescan() {
        aapsLogger.info(LTag.PUMP, "User wants to rescan for devices")
        pairingManager?.clearPairingData()
        _state.update { it.copy(
            enteredPIN = "",
            pairingError = null,
            deviceAddress = "",
            deviceName = ""
        )}
    }

    fun startRePairing() {
        aapsLogger.info(LTag.PUMP, "Starting re-pairing flow")
        pairingManager?.clearPairingData()
        _state.update {
            TandemMobiWizardState(isRePairing = true)
        }
    }

    /**
     * Check if there's already a paired pump
     */
    fun hasExistingPairing(): Boolean {
        val pairStatus = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.PumpPairStatus, -1)
        val pumpSerial = tandemPumpUtil.getStringPreferenceOrDefault(TandemStringPreferenceKey.PumpSerial, "")
        return pairStatus == 100 && pumpSerial.isNotEmpty()
    }

    /**
     * Get the currently paired pump information
     */
    fun getExistingPumpInfo(): Triple<String, String, String> {
        val pumpName = tandemPumpUtil.getStringPreferenceOrDefault(TandemStringPreferenceKey.PumpName, "Unknown")
        val pumpSerial = tandemPumpUtil.getStringPreferenceOrDefault(TandemStringPreferenceKey.PumpSerial, "Unknown")
        val pumpAddress = tandemPumpUtil.getStringPreferenceOrDefault(TandemStringPreferenceKey.PumpAddress, "Unknown")
        return Triple(pumpName, pumpSerial, pumpAddress)
    }

    /**
     * Populate state with existing pump information for display
     */
    fun loadExistingPumpInfo() {
        val (pumpName, pumpSerial, pumpAddress) = getExistingPumpInfo()
        _state.update { it.copy(
            existingPumpName = pumpName,
            existingPumpSerial = pumpSerial,
            existingPumpAddress = pumpAddress
        )}
    }

    /**
     * User confirmed they want to remove existing pump and continue.
     * Resets state for fresh pairing (not re-pairing since we cleared the data).
     */
    fun onConfirmRemoveExistingPump() {
        aapsLogger.info(LTag.PUMP, "User confirmed removal of existing pump, resetting to fresh pairing state")
        _state.update {
            TandemMobiWizardState(isRePairing = false)
        }
    }

    /**
     * User cancelled removing existing pump
     */
    fun onCancelRemoveExistingPump() {
        aapsLogger.info(LTag.PUMP, "User cancelled removal of existing pump")
        // Activity will handle finish
    }

    override fun onCleared() {
        super.onCleared()
        stopDeviceScan()
        disposable.clear()
    }
}
