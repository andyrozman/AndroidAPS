package info.nightscout.androidaps.plugins.pump.tandem.comm

import android.content.Context
import com.jwoglom.pumpx2.pump.bluetooth.TandemBluetoothHandler
import com.jwoglom.pumpx2.pump.bluetooth.TandemPump
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.response.authentication.CentralChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.authentication.PumpChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ApiVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TimeSinceResetResponse
import com.welie.blessed.BluetoothPeripheral
import info.nightscout.androidaps.plugins.pump.common.defs.PumpErrorType
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpNotificationType
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpApiVersion
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpConst
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import info.nightscout.shared.sharedPreferences.SP
import java.util.*

class TandemCommunicationManager constructor(
    var context: Context,
    var aapsLogger: AAPSLogger,
    var sp: SP,
    var pumpUtil: TandemPumpUtil,
    var btAddress: String
) : TandemPump(context, Optional.of(btAddress)) {

    lateinit var peripheral: BluetoothPeripheral
    var connected = false
    var inConnectMode = false
    var errorConnecting = false

    var commandRequestModeRunning = false

    var responses: MutableMap<Int, Message> = mutableMapOf()

    var bluetoothHandler: TandemBluetoothHandler? = null

    fun connect(): Boolean {
        if (bluetoothHandler==null) {
            createBluetoothHandler()
        }

        connected = false
        inConnectMode = true
        bluetoothHandler!!.startScan()

        while (inConnectMode) {
            Thread.sleep(2000)

            if (connected || errorConnecting) {
                inConnectMode = false
                //return connected;
            }
        }

        return connected
    }


    fun disconnect() {
        if (bluetoothHandler!=null) {
            bluetoothHandler!!.stop()
        }
        connected = false
        inConnectMode = false
    }


    fun createBluetoothHandler(): TandemBluetoothHandler? {
        if (bluetoothHandler != null) {
            return bluetoothHandler
        }
        //tandemEventCallback = PumpX2TandemPump(getApplicationContext())
        bluetoothHandler = TandemBluetoothHandler.getInstance(context, this)
        return bluetoothHandler
    }

    override fun onInitialPumpConnection(peripheral: BluetoothPeripheral)  {
        this.peripheral = peripheral
        super.onInitialPumpConnection(peripheral)
    }

    var commandRequest: Message? = null
    var commandResponse: Message? = null


    fun sendCommand(request: Message): Message? {
        this.commandRequestModeRunning = true
        this.commandRequest = request

        aapsLogger.info(LTag.PUMPCOMM, "Sending Request: ${request.opCode()} - ${request.javaClass.name} ")

        sendCommand(peripheral, request)

        while(commandRequestModeRunning) {

            if (commandResponse!=null) {
                this.commandRequestModeRunning = false
                return commandResponse
            }

            pumpUtil.sleep(1000)
        }

        return null
    }



    override fun onReceiveMessage(peripheral: BluetoothPeripheral, message: Message) {
        aapsLogger.info(LTag.PUMPCOMM, "Received Response: ${message.opCode()} - ${message.javaClass.name} ")

        if (inConnectMode)  {

            if (message is ApiVersionResponse) {
                // sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 80)
                // sp.putString(TandemPumpConst.Prefs.PumpAddress, peripheral.address)
                // sp.putString(TandemPumpConst.Prefs.PumpName, peripheral.name)

                val apiVersionResponse = message
                val apiVersion = TandemPumpApiVersion.getApiVersionFromResponse(apiVersionResponse)

                aapsLogger.info(LTag.PUMPCOMM, "Api Version: ${apiVersionResponse.majorVersion}.${apiVersionResponse.minorVersion} : ${apiVersion.name} ")

                // TODO check if PumpApiVersion changed
                //sp.putString(TandemPumpConst.Prefs.PumpApiVersion, apiVersion.name)
            } else if (message is TimeSinceResetResponse) {
                sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 90)
                val timeSinceResponse = message
                aapsLogger.info(LTag.PUMPCOMM, "TimeSinceResetResponse: ${timeSinceResponse}")

                this.connected = true
            }
        } else {

            if (message.opCode() == commandRequest!!.opCode()) {
                this.commandResponse = message
            }

        }

    }

    override fun onWaitingForPairingCode(peripheral: BluetoothPeripheral?, centralChallenge: CentralChallengeResponse?) {
        val pairingCode = sp.getStringOrNull(TandemPumpConst.Prefs.PumpPairCode, null)

        if (pairingCode.isNullOrBlank()) {
            aapsLogger.error(LTag.PUMPCOMM, "TandemPump: onWaitingForPairingCode. It seems you Pairing code was not saved.")
            sendInvalidPairingCodeError()
            return
        }

        pair(peripheral, centralChallenge, pairingCode)
    }

    override fun onInvalidPairingCode(peripheral: BluetoothPeripheral?, resp: PumpChallengeResponse?) {
        aapsLogger.error(LTag.PUMPCOMM, "onInvalidPairingCode() - PairingCode seems to be no longer valid.")
        sendInvalidPairingCodeError()
    }

    fun sendInvalidPairingCodeError() {
        sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, -2)
        pumpUtil.errorType = PumpErrorType.PumpPairInvalidPairCode

        pumpUtil.sendNotification(TandemPumpNotificationType.InvalidPairingCodeReconfigure)

        this.errorConnecting = true
    }


}