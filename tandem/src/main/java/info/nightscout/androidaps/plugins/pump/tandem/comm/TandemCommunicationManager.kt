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
import info.nightscout.androidaps.plugins.pump.common.data.PumpTimeDifferenceDto
import info.nightscout.androidaps.plugins.pump.common.defs.PumpErrorType
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemNotificationType
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpApiVersion
import info.nightscout.androidaps.plugins.pump.tandem.driver.TandemPumpStatus
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpConst
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import info.nightscout.shared.sharedPreferences.SP
import org.joda.time.DateTime
import java.util.*

class TandemCommunicationManager constructor(
    var context: Context,
    var aapsLogger: AAPSLogger,
    var sp: SP,
    var pumpUtil: TandemPumpUtil,
    var pumpStatus: TandemPumpStatus,
    var btAddress: String
) : TandemPump(context, Optional.of(btAddress)) {

    lateinit var peripheral: BluetoothPeripheral
    var connected = false
    var inConnectMode = false
    var errorConnecting = false
    var commandRequestModeRunning = false

    var commandRequest: Message? = null
    var commandResponse: Message? = null
    var responses: MutableMap<Int, Message> = mutableMapOf()

    var bluetoothHandler: TandemBluetoothHandler? = null

    var TAG = LTag.PUMPBTCOMM


    fun connect(): Boolean {
        aapsLogger.info(TAG, "TANDEMDBG: connect() ")

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
        aapsLogger.info(TAG, "TANDEMDBG: disconnect ")

        if (bluetoothHandler!=null) {
            bluetoothHandler!!.stop()
        }
        connected = false
        inConnectMode = false
    }


    private fun createBluetoothHandler(): TandemBluetoothHandler? {
        if (bluetoothHandler != null) {
            return bluetoothHandler
        }
        aapsLogger.info(TAG, "TANDEMDBG: createBluetoothHandler ")
        bluetoothHandler = TandemBluetoothHandler.getInstance(context, this)
        aapsLogger.info(TAG, "TANDEMDBG: createBluetoothHandler ${bluetoothHandler}")

        return bluetoothHandler
    }


    override fun onInitialPumpConnection(peripheral: BluetoothPeripheral)  {
        aapsLogger.info(TAG, "TANDEMDBG: createBluetoothHandler ")

        this.peripheral = peripheral
        super.onInitialPumpConnection(peripheral)
    }


    fun sendCommand(request: Message): Message? {
        this.commandRequestModeRunning = true
        this.commandRequest = request

        aapsLogger.info(LTag.PUMPCOMM, "TANDEMDBG: Sending Request: ${request.opCode()} - ${request.javaClass.name} ")

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
        aapsLogger.info(LTag.PUMPBTCOMM, "TANDEMDBG: Received Response: ${message.opCode()} - ${message.javaClass.name} ")

        if (inConnectMode)  {

            if (message is ApiVersionResponse) {

                val apiVersionResponse = message
                val apiVersion = TandemPumpApiVersion.getApiVersionFromResponse(apiVersionResponse)

                aapsLogger.info(LTag.PUMPCOMM, "Api Version: ${apiVersionResponse.majorVersion}.${apiVersionResponse.minorVersion} : ${apiVersion.name} ")

                // TODO check if PumpApiVersion changed
                //sp.putString(TandemPumpConst.Prefs.PumpApiVersion, apiVersion.name)
            } else if (message is TimeSinceResetResponse) {

                aapsLogger.info(LTag.PUMPCOMM, "TimeSinceResetResponse: ${message}")

                val dtPump = DateTime().withMillis(message.pumpTimeSeconds * 1000L)

                val pumpTimeDifference = PumpTimeDifferenceDto(DateTime.now(), dtPump)
                pumpStatus.pumpTime = pumpTimeDifference

                // TODO check Pump Serial

                this.connected = true
            }
        } else {

            if (message.opCode() == commandRequest!!.opCode()) {
                this.commandResponse = message
            }

        }
    }


    override fun onWaitingForPairingCode(peripheral: BluetoothPeripheral?, centralChallenge: CentralChallengeResponse?) {
        aapsLogger.info(TAG, "TANDEMDBG: onWaitingForPairingCode ")

        val pairingCode = sp.getStringOrNull(TandemPumpConst.Prefs.PumpPairCode, null)

        aapsLogger.info(TAG, "TANDEMDBG: onWaitingForPairingCode. Pairing Code: ${pairingCode} ")

        if (pairingCode.isNullOrBlank()) {
            aapsLogger.error(LTag.PUMPCOMM, "TandemPump: onWaitingForPairingCode. It seems you Pairing code was not saved.")
            sendInvalidPairingCodeError()
            return
        }

        pair(peripheral, centralChallenge, pairingCode)
    }


    override fun onInvalidPairingCode(peripheral: BluetoothPeripheral?, resp: PumpChallengeResponse?) {
        aapsLogger.error(TAG, "TANDEMDBG: onInvalidPairingCode() - PairingCode seems to be no longer valid.")
        sendInvalidPairingCodeError()
    }


    fun sendInvalidPairingCodeError() {
        sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, -2)
        pumpUtil.errorType = PumpErrorType.PumpPairInvalidPairCode

        pumpUtil.sendNotification(TandemNotificationType.InvalidPairingCodeReconfigure)

        this.errorConnecting = true
    }


}