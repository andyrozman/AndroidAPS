package app.aaps.pump.tandem.common.comm.maint

import android.content.Context
import android.os.Handler
import android.os.Looper
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import com.jwoglom.pumpx2.pump.bluetooth.TandemBluetoothHandler
import com.jwoglom.pumpx2.pump.bluetooth.TandemPump
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.ApiVersionRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.PumpVersionRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.TimeSinceResetRequest
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ApiVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TimeSinceResetResponse
import com.welie.blessed.BluetoothPeripheral

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

import java.util.*
import app.aaps.pump.tandem.common.events.EventTandemPairingStatus
import com.jwoglom.pumpx2.pump.messages.builders.JpakeAuthBuilder

/**
 * This is low-level driver that does pairing with pump
 */
class TandemPairingManager(
    context: Context,
    var aapsLogger: AAPSLogger,
    var preferences: Preferences,
    var tandemPumpUtil: TandemPumpUtil,
    var btAddress: String,
    var resourceHelper: ResourceHelper,
    var rxBus: RxBus,
    var pumpStatus: TandemPumpStatus,
    var pumpSync: PumpSync,
    var pumpX2L: PumpX2L,
) : TandemPump(context, Optional.of(btAddress)) {

    @Suppress("PropertyName")
    val TAG: LTag = LTag.PUMPBTCOMM

    var bluetoothHandler: TandemBluetoothHandler? = null

    private var pairingCodeToUse: String? = null
    private var pairingStartTime: Long = 0

    //private var disposable: CompositeDisposable = CompositeDisposable()

    fun startPairing() {
        aapsLogger.info(TAG, "start Pairing")

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


    fun createBluetoothHandler(): TandemBluetoothHandler? {
        aapsLogger.info(TAG, "createBluetoothHandler pairing")

        PumpState.pairingCodeType = TandemPumpUtil.PAIRING_CODE_TYPE

        if (bluetoothHandler != null) {
            return bluetoothHandler
        }

        LConfigurator.enableTimber()
        bluetoothHandler = TandemBluetoothHandler.getInstance(context, this, pumpX2L)
        return bluetoothHandler
    }

    fun stopBluetoothHandler() {
        aapsLogger.info(TAG, "stopBluetoothHandler pairing")
        val handler = bluetoothHandler
        if (handler == null) {
            // Nothing owned by the pairing manager. Calling forceReset here would still set
            // preventReconnect=true and reset the pumpx2 singleton - and it ran AFTER the wizard's
            // onFinish cleared that flag, so the flag stayed latched until an app restart. The main
            // TandemPumpCommunicationManager may also own a handler by now; do not touch it.
            aapsLogger.info(TAG, "stopBluetoothHandler: no handler owned by pairing manager - nothing to tear down")
            return
        }
        bluetoothHandler = null
        tandemPumpUtil.forceResetBluetoothHandler(handler)
    }

    // Pair Status

    override fun onReceiveMessage(peripheral: BluetoothPeripheral, message: Message) {
        aapsLogger.info(TAG, "received message: opCode=${message.opCode()}")

        if (message is ApiVersionResponse) {
            aapsLogger.info(TAG, "got ApiVersionResponse")

            preferences.put(TandemIntPreferenceKey.PumpPairStatus, 80)
            preferences.put(TandemStringPreferenceKey.PumpAddress, peripheral.address)
            preferences.put(TandemStringPreferenceKey.PumpName, peripheral.name)

            val apiVersion = TandemPumpApiVersion.getApiVersionFromResponse(message)

            aapsLogger.info(TAG, "Api Version: ${message.majorVersion}.${message.minorVersion} : ${apiVersion.name} ")

            pumpStatus.tandemPumpFirmware = apiVersion

            preferences.put(TandemStringPreferenceKey.PumpApiVersion, apiVersion.name)

        } else if (message is TimeSinceResetResponse) {

            preferences.put(TandemIntPreferenceKey.PumpPairStatus, 90)
            aapsLogger.info(TAG, "TimeSinceResetResponse: $message")

        } else if (message is PumpVersionResponse) {
            aapsLogger.info(TAG, "PumpVersionResponse: $message")

            // Persist pump identity first, but do NOT flip PumpPairStatus to 100
            // or fire EventPumpConnectionParameters Changed yet — those trigger
            // TandemService.connect() which would race the still-alive pairing
            // BluetoothHandler singleton. We tear that down first, then notify.
            preferences.put(TandemStringPreferenceKey.PumpSerial, "" + message.serialNum)
            preferences.put(TandemStringPreferenceKey.PumpVersionResponse, tandemPumpUtil.gson.toJson(message))
            pumpStatus.serialNumber = message.serialNum

            // Hop off the BLE callback thread before tearing down the central
            // (stop() closes the BluetoothCentralManager currently dispatching us).
            Handler(Looper.getMainLooper()).post {
                stopBluetoothHandler()

                preferences.put(TandemIntPreferenceKey.PumpPairStatus, 100)

                pumpSync.connectNewPump()
                finalPairingStatus()

                val pumpName = preferences.get(TandemStringPreferenceKey.PumpName)
                rxBus.send(EventTandemPairingStatus.PairingSuccess(
                    pumpSerial = message.serialNum.toString(),
                    pumpName = pumpName,
                    pumpApiVersion = pumpStatus.tandemPumpFirmware
                ))
            }
        }

    }

    override fun onReceiveQualifyingEvent(peripheral: BluetoothPeripheral?, events: MutableSet<QualifyingEvent>?) {
        aapsLogger.info(TAG, "onReceiveQualifyingEvent: %s", events)
    }

    override fun onWaitingForPairingCode(peripheral: BluetoothPeripheral?, centralChallenge: AbstractCentralChallengeResponse?) {

        aapsLogger.info(TAG, "onWaitingForPairingCode:")

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


    override fun onJpakeProgress(jpakeStep: JpakeAuthBuilder.JpakeStep) {
        rxBus.send(EventTandemPairingStatus.PairingInProgress(jpakeStep.progressPercent, jpakeStep.name))
    }


    //  40 = onWaitingForPairingCode
    //  50 pairing code set
    //  -1 = onInvalidPairingCode
    //  70 = connected;
    //  80 = ApiVersionResponse;
    //  90 = TimeSinceResetResponse;
    //  100 = PumpVersionResponse

    override fun onPumpConnected(peripheral: BluetoothPeripheral?) {
        aapsLogger.info(LTag.PUMPCOMM, "onPumpConnected")

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

        aapsLogger.info(LTag.PUMPCOMM, "onPairingStatus: \n $stringBuilder")

    }

    fun getStringPreference(key: TandemStringPreferenceKey, defaultValue: String?): String {
        return tandemPumpUtil
            .getStringPreferenceOrDefault(key, defaultValue)
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
        //pumpStatus.errorDescription = ""

        // Reset instance variables
        pairingCodeToUse = null
        pairingStartTime = 0

    }

}
