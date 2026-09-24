package app.aaps.pump.tandem.common.comm.maint

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.keys.interfaces.Preferences
//import app.aaps.core.ui.except for the UIdialogs.AlertDialogHelper
import app.aaps.core.ui.extensions.runOnUiThread
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
//import com.jwoglom.pumpx2.pump.TandemError
import com.jwoglom.pumpx2.pump.bluetooth.TandemBluetoothHandler
import com.jwoglom.pumpx2.pump.bluetooth.TandemPump
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.ApiVersionRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.PumpVersionRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.TimeSinceResetRequest
import com.jwoglom.pumpx2.pump.messages.response.authentication.CentralChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ApiVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TimeSinceResetResponse
//import com.jwoglom.pumpx2.util.timber.LConfigurator
import com.welie.blessed.BluetoothPeripheral
import app.aaps.pump.common.defs.PumpUpdateFragmentType
import app.aaps.pump.common.events.EventPumpConnectionParametersChanged
import app.aaps.pump.common.events.EventPumpFragmentValuesChanged

import app.aaps.pump.tandem.common.data.defs.TandemPumpApiVersion
import app.aaps.pump.tandem.common.keys.TandemIntPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.tandem.common.util.PumpX2L

import app.aaps.pump.tandem.common.util.TandemPumpUtil
import com.jwoglom.pumpx2.pump.PumpState
import com.jwoglom.pumpx2.pump.messages.models.PairingCodeType
import com.jwoglom.pumpx2.pump.messages.response.authentication.AbstractCentralChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.qualifyingEvent.QualifyingEvent
import com.jwoglom.pumpx2.util.timber.LConfigurator

import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.*
import app.aaps.pump.tandem.common.events.EventTandemPairingStatus
import app.aaps.pump.tandem.common.events.PairingError
import com.jwoglom.pumpx2.pump.messages.builders.JpakeAuthBuilder

/**
 * This is low-level driver that does pairing with pump
 */
// TODO TandemPairingManager maybe add some more dialogs in case of success, error  N-5
//  Create Wizard with 2 steps:
//     1 - Eneter pin number
//     2 - Show status of bonding (in case of error show error)
// TODO(jwoglom): rename to TandemPumpPairingManager for consistency with parent class
// TODO is this still used? PumpBLEConfigActivity was removed Andy Jwolglom?
class TandemPairingManager constructor(
    context: Context,
    var aapsLogger: AAPSLogger,
    var preferences: Preferences,
    var tandemPumpUtil: TandemPumpUtil,
    var btAddress: String,
    var resourceHelper: ResourceHelper,
    var rxBus: RxBus,
    //var sp: SP,
    var pumpStatus: TandemPumpStatus,
    var pumpSync: PumpSync,
    // var activity: PumpBLEConfigActivity,
    var pumpX2L: PumpX2L,
    var aapsSchedulers: AapsSchedulers
) : TandemPump(context, Optional.of(btAddress)) {

    @Suppress("PropertyName")
    val TAG: LTag = LTag.PUMPBTCOMM

    var bluetoothHandler: TandemBluetoothHandler? = null

    private var pairingCodeToUse: String? = null
    private var pairingStartTime: Long = 0

    //private var disposable: CompositeDisposable = CompositeDisposable()

    fun startPairing() {
        aapsLogger.info(TAG, "start Pairing")

        aapsLogger.info(TAG, "TANDEM-PAIR-DBG: start Pairing")

        createBluetoothHandler()

        bluetoothHandler!!.startScan()
    }

    /**
     * Start pairing with a specific pairing code provided by the user
     * This is the recommended entry point for wizard-based pairing
     */
    fun startPairingWithCode(pairingCode: String) {
        aapsLogger.info(TAG, "start Pairing with code")
        this.pairingCodeToUse = pairingCode
        this.pairingStartTime = System.currentTimeMillis()

        rxBus.send(EventTandemPairingStatus.PairingStarted)
        startPairing()
    }

    fun shutdownPairingManager() {
        stopBluetoothHandler()
    }

    fun showToast(text:String) {

        runOnUiThread {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show() // TODO TandemPairingManager N-5
        }

    }


    fun createBluetoothHandler(): TandemBluetoothHandler? {
        aapsLogger.info(TAG, "TANDEM-PAIR-DBG: createBluetoothHandler pairing")

        PumpState.pairingCodeType = PairingCodeType.SHORT_6CHAR;

        if (bluetoothHandler != null) {
            return bluetoothHandler
        }

        LConfigurator.enableTimber()  // TODO not sure about this
        bluetoothHandler = TandemBluetoothHandler.getInstance(context, this, pumpX2L)
        return bluetoothHandler
    }

    fun stopBluetoothHandler() {
        aapsLogger.info(TAG, "TANDEM-PAIR-DBG: stopBluetoothHandler pairing")
        tandemPumpUtil.forceResetBluetoothHandler(bluetoothHandler)
        bluetoothHandler = null
    }

    // Pair Status

    override fun onReceiveMessage(peripheral: BluetoothPeripheral, message: Message) {
        aapsLogger.info(TAG, "TANDEM-PAIR-DBG: received message: opCode=${message.opCode()}")

        if (message is ApiVersionResponse) {
            aapsLogger.info(TAG, "TANDEM-PAIR-DBG: got ApiVersionResponse")

            //sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 80)
            preferences.put(TandemIntPreferenceKey.PumpPairStatus, 80)

            //sp.putString(TandemPumpConst.Prefs.PumpAddress, peripheral.address)
            preferences.put(TandemStringPreferenceKey.PumpAddress, peripheral.address)

            //sp.putString(TandemPumpConst.Prefs.PumpName, peripheral.name)
            preferences.put(TandemStringPreferenceKey.PumpName, peripheral.name)

            val apiVersionResponse = message
            val apiVersion = TandemPumpApiVersion.getApiVersionFromResponse(apiVersionResponse)

            aapsLogger.info(TAG, "Api Version: ${apiVersionResponse.majorVersion}.${apiVersionResponse.minorVersion} : ${apiVersion.name} ")

            pumpStatus.tandemPumpFirmware = apiVersion

            //sp.putString(TandemPumpConst.Prefs.PumpApiVersion, apiVersion.name)
            preferences.put(TandemStringPreferenceKey.PumpApiVersion, apiVersion.name)

            rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Configuration))

        } else if (message is TimeSinceResetResponse) {
            aapsLogger.info(TAG, "TANDEM-PAIR-DBG: got TimeSinceResetResponse")

            preferences.put(TandemIntPreferenceKey.PumpPairStatus, 90)
            val timeSinceResponse = message
            aapsLogger.info(TAG, "TimeSinceResetResponse: ${timeSinceResponse}")

        } else if (message is PumpVersionResponse) {
            aapsLogger.info(TAG, "TANDEM-PAIR-DBG: got PumpVersionResponse")

            val pumpVersionResponse = message
            aapsLogger.info(TAG, "PumpVersionResponse: ${pumpVersionResponse}")

            // Persist pump identity first, but do NOT flip PumpPairStatus to 100
            // or fire EventPumpConnectionParametersChanged yet — those trigger
            // TandemService.connect() which would race the still-alive pairing
            // BluetoothHandler singleton. We tear that down first, then notify.
            preferences.put(TandemStringPreferenceKey.PumpSerial, "" + pumpVersionResponse.serialNum)
            preferences.put(TandemStringPreferenceKey.PumpVersionResponse, tandemPumpUtil.gson.toJson(message))
            pumpStatus.serialNumber = pumpVersionResponse.serialNum.toLong()

            // Hop off the BLE callback thread before tearing down the central
            // (stop() closes the BluetoothCentralManager currently dispatching us).
            Handler(Looper.getMainLooper()).post {
                stopBluetoothHandler()

                preferences.put(TandemIntPreferenceKey.PumpPairStatus, 100)

                pumpSync.connectNewPump()
                finalPairingStatus()
                rxBus.send(EventPumpConnectionParametersChanged())

                val pumpName = preferences.get(TandemStringPreferenceKey.PumpName)
                rxBus.send(EventTandemPairingStatus.PairingSuccess(
                    pumpSerial = pumpVersionResponse.serialNum.toString(),
                    pumpName = pumpName,
                    pumpApiVersion = pumpStatus.tandemPumpFirmware
                ))
            }
        }

    }

    override fun onReceiveQualifyingEvent(peripheral: BluetoothPeripheral?, events: MutableSet<QualifyingEvent>?) {
        aapsLogger.info(TAG, "TANDEM-PAIR-DBG: onReceiveQualifyingEvent: %s", events)
    }

    override fun onWaitingForPairingCode(peripheral: BluetoothPeripheral?, centralChallenge: AbstractCentralChallengeResponse?) {

        aapsLogger.info(TAG, "TANDEM-PAIR-DBG: onWaitingForPairingCode:")

        rxBus.send(EventTandemPairingStatus.WaitingForCode)

        // Use the pairing code provided via startPairingWithCode, or fall back to empty string
        val code = pairingCodeToUse ?: ""

        preferences.put(TandemStringPreferenceKey.PumpPairCode, code)
        preferences.put(TandemIntPreferenceKey.PumpPairStatus, 40)

        aapsLogger.info(TAG, "Using pairing code for pump pairing")

        pair(peripheral, centralChallenge, code)
        preferences.put(TandemIntPreferenceKey.PumpPairStatus, 50)
        rxBus.send(EventTandemPairingStatus.PairingInProgress(0, "Starting"))
    }

    // private fun hasPairingCode(peripheral: BluetoothPeripheral?, btAddress: String, challenge: AbstractCentralChallengeResponse?, pairingCode: String) {
    //     aapsLogger.info(LTag.PUMPBTCOMM, "Device ${btAddress} hasPairingCode: ${pairingCode}")
    //     sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 50)
    //     sp.putString(TandemPumpConst.Prefs.PumpPairCode, pairingCode)
    //     pair(peripheral, challenge, pairingCode)
    // }


    // override fun onInvalidPairingCode(peripheral: BluetoothPeripheral, resp: PumpChallengeResponse?) {
    //
    //     aapsLogger.info(TAG, "TANDEMDBG: onInvalidPairingCode")
    //
    //     sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, -1)
    //     pumpUtil.errorType = PumpErrorType.PumpPairInvalidPairCode
    //
    //     aapsLogger.error(TAG, "PairingCode WAS INVALID.")
    //
    //     showToast("PairingCode WAS INVALID. Try again.")
    //
    //     //PumpState.failedPumpConnectionAttempts++
    //
    //     rxBus.send(EventPumpDriverStateChanged(PumpDriverState.ErrorCommunicatingWithPump))
    //
    //     if (finishActivity) {
    //         activity.finish()
    //     }
    //
    //     // AlertDialog.Builder(context)
    //     //     .setTitle("Pump Connection")
    //     //     .setMessage("The pump rejected the pairing code. You need to unpair and re-pair the device in Bluetooth Settings. Press OK to enter the new code.")
    //     //     .setPositiveButton(R.string.yes) { dialog, which ->
    //     //         val intent = Intent(PUMP_CONNECTED_STAGE1_INTENT)
    //     //         intent.putExtra("address", peripheral!!.address)
    //     //         intent.putExtra("name", peripheral!!.name)
    //     //         context.sendBroadcast(intent)
    //     //     }
    //     //     .setNegativeButton(R.string.no, null)
    //     //     .setIcon(R.drawable.ic_dialog_alert)
    //     //     .show()
    //
    //     // rxBus.send()
    // }

    override fun onJpakeProgress(jpakeStep: JpakeAuthBuilder.JpakeStep) {
        rxBus.send(EventTandemPairingStatus.PairingInProgress(jpakeStep.progressPercent, jpakeStep.name))
    }

    private fun triggerPairDialog(peripheral: BluetoothPeripheral, btAddress: String, challenge: CentralChallengeResponse) {

        aapsLogger.info(LTag.PUMPCOMM, "TANDEM-PAIR-DBG: triggerPairDialog")

        val btName = peripheral.name
        val builder = AlertDialog.Builder(this.context)
        builder.setTitle("Enter pairing code (case-sensitive)")
        builder.setMessage("Enter the pairing code from Bluetooth Settings > Pair Device to connect to:\n\n$btName ($btAddress)")

        // Set up the input
        val input = EditText(this.context)
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL

        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("OK") { _, _ ->
            val pairingCode = input.text.toString()
            //Timber.i("pairing code inputted: %s", pairingCode)
            //triggerImmediatePair(peripheral, pairingCode, challenge)
            //sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 50)
            preferences.put(TandemIntPreferenceKey.PumpPairStatus, 50)
            //sp.putString(TandemPumpConst.Prefs.PumpPairCode, pairingCode)
            preferences.put(TandemStringPreferenceKey.PumpPairCode, pairingCode)

            aapsLogger.info(LTag.PUMPCOMM, "PairingCode Accepted: ${pairingCode}")

            pair(peripheral, challenge, pairingCode)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }


    private fun triggerAAPSPairDialog(peripheral: BluetoothPeripheral, btAddress: String, challenge: CentralChallengeResponse) {

        aapsLogger.info(LTag.PUMPCOMM, "TANDEM-PAIR-DBG: triggerAAPSPairDialog")

        val btName = peripheral.name
        var okClicked = false

        // Set up the input
        val input = EditText(this.context)
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL

        MaterialAlertDialogBuilder(context)
            .setTitle(resourceHelper.gs(R.string.tandem_ble_config_pairing_title))
            .setMessage(resourceHelper.gs(R.string.tandem_ble_config_pairing_message, btName, btAddress))
            .setView(input)
            .setPositiveButton(context.getString(app.aaps.core.ui.R.string.ok)) { dialog: DialogInterface, _: Int ->
                if (okClicked) return@setPositiveButton
                else {
                    okClicked = true
                    dialog.dismiss()
                    SystemClock.sleep(100)
                    val pairingCode = input.text.toString()
                    //sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 50)
                    preferences.put(TandemIntPreferenceKey.PumpPairStatus, 50)
                    //sp.putString(TandemPumpConst.Prefs.PumpPairCode, pairingCode)
                    preferences.put(TandemStringPreferenceKey.PumpPairCode, pairingCode)

                    aapsLogger.info(LTag.PUMPCOMM, "PairingCode Accepted: ${pairingCode}")

                    pair(peripheral, challenge, pairingCode)
                }
            }
            .setNegativeButton(context.getString(app.aaps.core.ui.R.string.cancel)) { dialog: DialogInterface, _: Int ->
                if (okClicked) return@setNegativeButton
                else {
                    okClicked = true
                    dialog.dismiss()
                    SystemClock.sleep(100)
                }
            }
            .show()
            .setCanceledOnTouchOutside(false)

    }


    // override fun onPumpCriticalError(peripheral: BluetoothPeripheral?, reason: TandemError?) {
    //     super.onPumpCriticalError(peripheral, reason)
    //     aapsLogger.error(TAG, "TANDEMDBG: CRITICAL ERROR: ${reason}")
    //     ToastUtils.showToastInUiThread(context, "Tandem error: ${reason}")
    // }

    //  40 = onWaitingForPairingCode
    //  50 pairing code set
    //  -1 = onInvalidPairingCode
    //  70 = connected;
    //  80 = ApiVersionResponse;
    //  90 = TimeSinceResetResponse;
    //  100 = PumpVersionResponse

    override fun onPumpConnected(peripheral: BluetoothPeripheral?) {
        aapsLogger.info(LTag.PUMPCOMM, "TANDEMDBG: onPumpConnected")

        //sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 70)
        preferences.put(TandemIntPreferenceKey.PumpPairStatus, 70)

        sendCommand(peripheral, ApiVersionRequest())
        sendCommand(peripheral, TimeSinceResetRequest())
        sendCommand(peripheral, PumpVersionRequest())
    }


    override fun sendCommand(peripheral: BluetoothPeripheral?, message: Message?) {
        try {
            super.sendCommand(peripheral, message)
        } catch (ex: Exception) {
            aapsLogger.error(TAG, "TANDEM-PAIR-DBG: Problem sending command to the pump. Ex: ${ex.message}", ex)

        }
    }


    fun finalPairingStatus() {

        val stringBuilder = StringBuilder()

        stringBuilder.append("PAIRING STATUS\n")
        stringBuilder.append("-------------------\n")

        stringBuilder.append("Pump Pair Status: ${tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.PumpPairStatus, null)}\n")
        stringBuilder.append("PumpPairCode: ${getStringPreference(TandemStringPreferenceKey.PumpPairCode, null)}\n")
        stringBuilder.append("PumpAddress: ${getStringPreference(TandemStringPreferenceKey.PumpAddress, "-")}\n")
        stringBuilder.append("PumpName: ${getStringPreference(TandemStringPreferenceKey.PumpName, "-")}\n")
        stringBuilder.append("PumpSerial: ${getStringPreference(TandemStringPreferenceKey.PumpSerial, "-")}\n")
        stringBuilder.append("Pump Version Response: ${tandemPumpUtil.getStringPreferenceOrDefault(TandemStringPreferenceKey.PumpVersionResponse, "-")}\n")

        // stringBuilder.append("Pump Pair Status: ${sp.getInt(TandemPumpConst.Prefs.PumpPairStatus, -100)} \n")
        // stringBuilder.append("PumpPairCode: ${sp.getString(TandemPumpConst.Prefs.PumpPairCode, "-")}\n")
        // stringBuilder.append("PumpAddress: ${sp.getString(TandemPumpConst.Prefs.PumpAddress, "-")}\n")
        // stringBuilder.append("PumpName: ${sp.getString(TandemPumpConst.Prefs.PumpName, "-")}\n")
        // stringBuilder.append("PumpSerial: ${sp.getString(TandemPumpConst.Prefs.PumpSerial, "-")}\n")
        // stringBuilder.append("Pump Version Response: ${sp.getString(TandemPumpConst.Prefs.PumpVersionResponse, "-")} \n")

        aapsLogger.info(LTag.PUMPCOMM, "TANDEM-PAIR-DBG: onPairingStatus: \n ${stringBuilder.toString()}") // TODO remove this TandemPairingManager N-5

    }

    fun getStringPreference(key: TandemStringPreferenceKey, defaultValue: String?): String {
        return tandemPumpUtil
            .getStringPreferenceOrDefault(key, defaultValue)
    }

    /**
     * Check if pairing has timed out and send appropriate error event
     * Returns true if timeout occurred
     */
    fun checkPairingTimeout(): Boolean {
        val elapsed = System.currentTimeMillis() - pairingStartTime
        val status = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.PumpPairStatus, -1)

        if (elapsed > 30000 && status < 100) { // 30 second timeout
            aapsLogger.error(TAG, "Pairing timeout: status=$status, elapsed=${elapsed}ms")

            val error = when (status) {
                40, 50 -> PairingError.IncorrectPIN  // Stuck at waiting/entered code
                70 -> PairingError.ConnectionTimeout  // Connected but no response
                else -> PairingError.BluetoothError
            }

            rxBus.send(EventTandemPairingStatus.PairingFailed(error))
            stopBluetoothHandler()
            return true
        }
        return false
    }

    /**
     * Clear all pairing data to allow re-pairing with a pump
     */
    fun clearPairingData() {
        aapsLogger.info(TAG, "Clearing pairing data for re-pairing")

        // NOTE: deliberately do NOT stop the BLE handler here. Retry / rescan
        // flows run on the same TandemPairingManager (same TandemPump identity),
        // so the existing handler can be reused. Tearing it down and creating
        // a fresh one leaves orphan handlers with pending reconnect runnables
        // that compete for the pump after pairing finishes.

        // Clear preferences
        preferences.put(TandemIntPreferenceKey.PumpPairStatus, -1)
        preferences.put(TandemStringPreferenceKey.PumpAddress, "")
        preferences.put(TandemStringPreferenceKey.PumpPairCode, "")
        preferences.put(TandemStringPreferenceKey.PumpSerial, "")
        preferences.put(TandemStringPreferenceKey.PumpName, "")
        preferences.put(TandemStringPreferenceKey.PumpVersionResponse, "")
        preferences.put(TandemStringPreferenceKey.PumpApiVersion, "")

        // Clear PumpX2 library state
        try {
            PumpState.resetState(context)
            aapsLogger.info(TAG, "PumpState cleared successfully")
        } catch (e: Exception) {
            aapsLogger.error(TAG, "Error clearing PumpState", e)
        }

        // Reset pump status
        pumpStatus.serialNumber = 0L
        pumpStatus.errorDescription = ""

        // Reset instance variables
        pairingCodeToUse = null
        pairingStartTime = 0

        // Notify UI
        rxBus.send(EventPumpConnectionParametersChanged())
    }


}
