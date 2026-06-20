package app.aaps.pump.tandem.common.comm

import android.content.Context
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.ui.extensions.runOnUiThread
import app.aaps.pump.common.data.PumpTimeDifferenceDto
import app.aaps.pump.common.defs.PumpDriverState
import app.aaps.pump.common.defs.PumpErrorType
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.common.defs.PumpUpdateFragmentType
import app.aaps.pump.common.events.EventPumpFragmentValuesChanged
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.comm.data.CommunicationListener
import app.aaps.pump.tandem.common.comm.data.DisconnectDataDto
import app.aaps.pump.tandem.common.comm.maint.TandemConnectionFixer
import app.aaps.pump.tandem.common.comm.ui.TandemUiStateWriter
import app.aaps.pump.tandem.common.data.defs.TandemNotificationType
import app.aaps.pump.tandem.common.data.defs.TandemPumpApiVersion
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.tandemDataStore
import app.aaps.pump.tandem.common.events.EventHandleQualifyingEvent
import app.aaps.pump.tandem.common.keys.TandemIntPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.tandem.common.util.PumpX2L
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import com.jwoglom.pumpx2.pump.PumpState
import com.jwoglom.pumpx2.pump.TandemError
import com.jwoglom.pumpx2.pump.bluetooth.TandemBluetoothHandler
import com.jwoglom.pumpx2.pump.bluetooth.TandemConfig
import com.jwoglom.pumpx2.pump.bluetooth.TandemPump
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.response.authentication.AbstractCentralChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ApiVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TimeSinceResetResponse
import com.jwoglom.pumpx2.pump.messages.response.qualifyingEvent.QualifyingEvent
import com.welie.blessed.ConnectionState
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.HciStatus
import org.joda.time.DateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * This is low-level driver that does all communication with pump, with exception of pairing.
 */
class TandemPumpCommunicationManager(
    context: Context,
    var resourceHelper: ResourceHelper,
    var aapsLogger: AAPSLogger,
    var rxBus: RxBus,
    var preferences: Preferences,
    var pumpUtil: TandemPumpUtil,
    var pumpStatus: TandemPumpStatus,
    var pumpConfig: TandemConfig,
    var timberTree: PumpX2L,
    var tandemConnectionFixer: TandemConnectionFixer
) : TandemPump(context, pumpConfig) {

    lateinit var peripheral: BluetoothPeripheral
    @Volatile var connected = false
    @Volatile var errorConnecting = false
    var commandRequestModeRunning: Boolean = false
        get() { return inFlightRequests.isNotEmpty() }

    var dataStore: TandemUiStateWriter = tandemDataStore

    var communicationListener : CommunicationListener? = null
        set(value) {  if (value==null)
            operationMode=OperationMode.StandardOperation
        else
            operationMode=OperationMode.ExternalListenerOperation
            field = value
        }

    //var responses: MutableMap<Int, Message> = mutableMapOf()

    var bluetoothHandler: TandemBluetoothHandler? = null



    @Volatile var operationMode: OperationMode = OperationMode.None

    companion object {
        val TAG = LTag.PUMPBTCOMM
        val COMMAND_TIMEOUT = 5 * 1000  // 5s (in ms) timeout for receiving pump command response
        val HANDSHAKE_TIMEOUT = 30 * 1000L  // 30s (in ms) timeout for handshake (pairing) connecting flow
        val CONNECT_TIMEOUT = 60 * 1000L // 60s (in ms) timeout for complete connecting flow
    }


    fun connect(): Boolean {

        aapsLogger.info(TAG, "connect() ")

        if (bluetoothHandler==null) {
            createBluetoothHandler()
        }

        connected = false
        val connectStartTime = System.currentTimeMillis()
        operationMode = OperationMode.ConnectionMode
        bluetoothHandler!!.startScan()

        while (operationMode == OperationMode.ConnectionMode) {

            Thread.sleep(500)

            if (connected || errorConnecting) {
                aapsLogger.info(TAG, "connected: $connected error: $errorConnecting")
                operationMode = OperationMode.StandardOperation
            } else if (handshakingStartTime > 0 &&
                       pumpUtil.driverStatus == PumpDriverState.Handshaking &&
                       System.currentTimeMillis() - handshakingStartTime > HANDSHAKE_TIMEOUT) {
                aapsLogger.error(TAG, "Handshake timeout after ${HANDSHAKE_TIMEOUT / 1000}s, forcing disconnect")
                handshakingStartTime = 0L
                errorConnecting = true
                forceDisconnect(onDisconnect = false, tandemError = null)
            } else if (handshakingStartTime > 0 &&
                    pumpUtil.driverStatus == PumpDriverState.Connecting &&
                    System.currentTimeMillis() - connectStartTime > CONNECT_TIMEOUT) {
                aapsLogger.error(TAG, "Connection timeout after ${CONNECT_TIMEOUT / 1000}s, forcing disconnect")
                errorConnecting = true
                forceDisconnect(onDisconnect = false, tandemError = null)
            }
        }

        this.pumpStatus.disconnectData = null

        // Conservative until the first status read resolves running-state: set Unknown before
        // flipping connected true so the availability gate never sees a stale (connected, Running).
        pumpStatus.pumpRunningState = PumpRunningState.Unknown
        pumpStatus.pumpConnectedFlow.value = connected
        tandemDataStore.postPumpConnected(connected)


        return connected
    }

    /** Publishes the disconnected delivery state: running-state Unknown + not connected. */
    private fun publishDisconnectedState() {
        pumpStatus.pumpRunningState = PumpRunningState.Unknown
        pumpStatus.pumpConnectedFlow.value = false
        tandemDataStore.postPumpConnected(false)
    }


    fun disconnect(): Boolean {

        aapsLogger.info(TAG, "disconnect()")

        // Fully tear down the BLE handler: cancel any live peripheral
        // connection, stop the central, and null the pumpx2 singleton so the
        // next connect() builds a fresh handler. blessed's close() alone does
        // not disconnect already-connected peripherals.
        pumpUtil.forceResetBluetoothHandler(bluetoothHandler)
        bluetoothHandler = null

        connected = false
        operationMode = OperationMode.None

        publishDisconnectedState()

        return connected
    }


    private fun createBluetoothHandler(): TandemBluetoothHandler? {
        if (bluetoothHandler != null) {
            return bluetoothHandler
        }
        aapsLogger.info(TAG, "createBluetoothHandler for Communication")

        runOnUiThread {
            bluetoothHandler = TandemBluetoothHandler.getInstance(context, this, timberTree)
        }

        while (bluetoothHandler == null) {
            aapsLogger.debug(TAG, "Waiting for bluetoothHandler on ui thread")
            pumpUtil.sleep(500)
        }

        return bluetoothHandler
    }


    private var handshakingStartTime: Long = 0L

    override fun onInitialPumpConnection(peripheral: BluetoothPeripheral)  {
        aapsLogger.info(TAG, "onInitialPumpConnection: $peripheral")

        this.peripheral = peripheral
        connected = false
        errorConnecting = false
        operationMode = OperationMode.ConnectionMode
        handshakingStartTime = System.currentTimeMillis()
        pumpUtil.driverStatus = PumpDriverState.Handshaking
        super.onInitialPumpConnection(peripheral)
    }


    override fun onPumpConnected(peripheral: BluetoothPeripheral?) {
        aapsLogger.info(TAG, "onPumpConnected: $peripheral")

        super.onPumpConnected(peripheral)
    }


    // Thread-safe: added/removed on the TandemPumpOpQueue thread, read/added on the BLE callback
    // (main) thread. newKeySet gives weakly-consistent iteration so find() can't throw CME.
    val inFlightRequests: MutableSet<Message> = ConcurrentHashMap.newKeySet()
    val inFlightResponses: MutableSet<Message> = ConcurrentHashMap.newKeySet()

    /**
     * Sends command to the pump, if driver is in preventConnect mode any messages will be ignored,
     * unless we specify forceSend. Force send should be used only for ChangeFillManager
     */
    @Synchronized
    fun sendCommand(request: Message, forceSend: Boolean = false): Message? {

        aapsLogger.info(TAG, "sendCommand: ${request.javaClass.simpleName}")

        operationMode = OperationMode.StandardOperation

        if (!initializePump(request)) {
            return null
        }

        if (!isPumpStillConnected()) {
            aapsLogger.error(TAG, "It seems Pump is no longer connected.")
            if (!tryToReconnectToPump()) {
                aapsLogger.error(TAG, "Couldn't re-connect to the Pump.")
                this.operationMode = OperationMode.None;
                return null
            }
        }

        this.inFlightRequests.add(request)
        sendCommand(peripheral, request)
        aapsLogger.info(LTag.PUMPCOMM, "Sending Request: [code=${request.opCode()},class=${request::class.simpleName}]")

        val timeoutTime = System.currentTimeMillis() + COMMAND_TIMEOUT;

        while (commandRequestModeRunning) {

            val commandResponse = this.inFlightResponses.find { it.opCode() == request.responseOpCode }
            if (commandResponse != null) {
                this.inFlightRequests.remove(request)
                this.inFlightResponses.remove(commandResponse)
                return commandResponse
            }

            pumpUtil.sleep(100)

            if (System.currentTimeMillis()>=timeoutTime) {
                aapsLogger.error(TAG, "Timeout for command ${request.javaClass.name} returning with null.")
                break
            }
        }

        this.inFlightRequests.remove(request)
        return null
    }


    private fun isPumpStillConnected(): Boolean {
        val bleConnected = ::peripheral.isInitialized &&
            peripheral.state == ConnectionState.CONNECTED
        if (!bleConnected && connected) {
            aapsLogger.warn(TAG, "BLE no longer connected; updating state.")
            connected = false
            pumpUtil.driverStatus = PumpDriverState.Disconnected
            publishDisconnectedState()
        }
        return bleConnected && connected
    }


    private fun tryToReconnectToPump(): Boolean {
        aapsLogger.warn(TAG, "Attempting to reconnect to pump.")

        pumpUtil.driverStatus = PumpDriverState.Connecting
        publishDisconnectedState()

        errorConnecting = false
        connected = false

        pumpUtil.forceResetBluetoothHandler(bluetoothHandler)
        bluetoothHandler = null
        operationMode = OperationMode.None

        val reconnectResult = connect()
        if (!reconnectResult) {
            aapsLogger.error(TAG, "Reconnect attempt failed.")
            pumpUtil.driverStatus = PumpDriverState.Disconnected
            publishDisconnectedState()
        }

        return reconnectResult
    }


    private fun initializePump(request: Message): Boolean {
        var times = 0;
        while (!::peripheral.isInitialized && times < 10) {
            aapsLogger.warn(LTag.PUMPCOMM, "Waiting for peripheral for sendCommand with ${request.opCode()} - ${request.javaClass.name}")
            pumpUtil.sleep(1000)
            times++
        }

        if (!::peripheral.isInitialized) {
            aapsLogger.warn(LTag.PUMPCOMM, "Failed sendCommand, no peripheral with ${request.opCode()} - ${request.javaClass.name}")
            return false
        }
        return true;
    }


    fun sendCommandWithListener(request: Message): Boolean {

        operationMode = OperationMode.ExternalListenerOperation

        aapsLogger.warn(LTag.PUMPCOMM, "STM: sendCommandWithListener sendCommand with [request_code=${request.opCode()},request_class=${request.javaClass.simpleName},connected=$connected]")

        if (!initializePump(request)) {
            return false
        }

        aapsLogger.info(LTag.PUMPCOMM, "STM: Sending Request: [code=${request.opCode()},class=${request::class.simpleName}]")

        sendCommand(peripheral, request)

        return true
    }


    var apiVersionResponseReceived = false


    override fun onReceiveMessage(peripheral: BluetoothPeripheral, message: Message) {
        aapsLogger.info(LTag.PUMPBTCOMM, "Received Response: ${message.opCode()} - ${message.javaClass.simpleName} - Mode: $operationMode")

        when(operationMode) {
            OperationMode.ConnectionMode            -> receiveMessageInConnectMode(message)
            OperationMode.StandardOperation         -> receiveMessageInStandardMode(message)
            OperationMode.ExternalListenerOperation -> communicationListener!!.onReceiveMessage(message)
            else -> {
                aapsLogger.error("We are in None operation mode and we received Pump Message.")
            }
        }
    }

    lateinit var apiVersionResponse: ApiVersionResponse

    fun receiveMessageInConnectMode(message: Message) {

        if (message is ApiVersionResponse) {

            apiVersionResponse = message

            val apiVersion = TandemPumpApiVersion.getApiVersionFromResponse(apiVersionResponse)

            aapsLogger.info(LTag.PUMPCOMM, "Api Version: ${apiVersionResponse.majorVersion}.${apiVersionResponse.minorVersion} : ${apiVersion.name} ")

            pumpStatus.tandemPumpFirmware = apiVersion
            pumpStatus.apiVersionResponse = message

            //sp.putString(TandemPumpConst.Prefs.PumpApiVersion, apiVersion.name)
            preferences.put(TandemStringPreferenceKey.PumpApiVersion, apiVersion.name)

            dataStore.postApiVersionResponse(message)

            rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Configuration))

        } else if (message is TimeSinceResetResponse) {

            val timeSince : TimeSinceResetResponse = message

            aapsLogger.info(LTag.PUMPCOMM, "TimeSinceResetResponse: ${message}")

            val dtPump = DateTime(timeSince.currentTimeInstant.toEpochMilli())

            val pumpTimeDifference = PumpTimeDifferenceDto(DateTime.now(), dtPump)
            pumpStatus.pumpTime = pumpTimeDifference

            pumpUtil.driverStatus = PumpDriverState.Connected

            this.connected = true
            this.operationMode = OperationMode.StandardOperation

        } else if (message is PumpVersionResponse) {
            dataStore.postPumpVersionResponse(message)
        }
    }

    fun receiveMessageInStandardMode(message: Message) {
        if (!this.commandRequestModeRunning) {
            aapsLogger.error(LTag.PUMPCOMM, "No Command Requested, but received message [code=${message.opCode()}]")
        } else {
            val matchingRequest = inFlightRequests.find { it.responseOpCode == message.opCode() }
            if (matchingRequest != null) {
                aapsLogger.info(LTag.PUMPCOMM, "Response received [code=${message.opCode()},class=${message::class.simpleName}]")
                this.inFlightResponses.add(message)
            } else {
                if (message is ApiVersionResponse) {
                    this.apiVersionResponseReceived = true
                    aapsLogger.error(LTag.PUMPCOMM, "Received ApiVersionResponse - problem with communication.")
                } else {
                    aapsLogger.info(TAG, "Discarding Message [code=${message.opCode()},class=${message.javaClass.simpleName}]")
                }
            }
        }

    }


    override fun onReceiveQualifyingEvent(peripheral: BluetoothPeripheral, events: Set<QualifyingEvent>) {
        aapsLogger.info(TAG, "QE: onReceiveQualifyingEvent: %s (creating AAPS event)", events)
        rxBus.send(EventHandleQualifyingEvent(events = events, dateTime = System.currentTimeMillis()))
    }


    override fun onWaitingForPairingCode(peripheral: BluetoothPeripheral?, centralChallenge: AbstractCentralChallengeResponse?) {
        aapsLogger.info(TAG, "TandemCommMgr: onWaitingForPairingCode ")

        val pairingCodePS = PumpState.getPairingCode(context)

        if (pairingCodePS==null) {
            aapsLogger.info(TAG, "TandemCommMgr: PumpState doesn't have PairCode, reading from local configuration.")
            val pairingCode = pumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.PumpPairCode, null)
            //sp.getStringOrNull(TandemPumpConst.Prefs.PumpPairCode, null)
            //aapsLogger.info(TAG, "TandemCommMgr: onWaitingForPairingCode. Pairing Code: ${pairingCode} ")

            if (pairingCode.isNullOrBlank()) {
                aapsLogger.error(LTag.PUMPCOMM, "TandemCommMgr: onWaitingForPairingCode. It seems your Pairing code was not saved.")
                sendInvalidPairingCodeError()
                return
            }

            pair(peripheral, centralChallenge, pairingCode)
        } else {
            aapsLogger.info(TAG, "TandemCommMgr: Taking PairCode from PumpState.")
            pair(peripheral, centralChallenge, pairingCodePS)
        }
    }

    override fun onPumpCriticalError(peripheral: BluetoothPeripheral?, reason: TandemError?) {
        aapsLogger.error(TAG, "CF: Pump Critical Error: ${reason}")
        dataStore.postDebugLastTandemError(reason)

        // When a status response message has code non-zero
        // This can occur just because a precondition isn't met
        // (e.g., trying to fill tubing when haven't stopped insulin delivery)
        if (reason == TandemError.ERROR_RESPONSE) {
            pumpStatus.errorDescription = resourceHelper.gs(
                R.string.tandem_error_pump_error_response,
                reason.extra
            )
            pumpUtil.errorType = PumpErrorType.PumpUnreachable
            rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.None))
        } else {

            pumpStatus.errorDescription = resourceHelper.gs(
                R.string.tandem_error_pump_critical_error,
                if (reason == null) "Unknown" else reason.message
            )
            pumpUtil.errorType = PumpErrorType.PumpUnreachable
            rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.None))

            // we currently look only for BT_CONNECTION_FAILED, might need to extend it
            if (reason != null && reason == TandemError.BT_CONNECTION_FAILED) {
                forceDisconnect(
                    onDisconnect = false,
                    tandemError = reason
                )
            }
        }

        //tandemConnectionFixer.startConnectionFix()

        super.onPumpCriticalError(peripheral, reason)
    }

    override fun onPumpDisconnected(peripheral: BluetoothPeripheral?, status: HciStatus?): Boolean {
        aapsLogger.error(TAG, "Pump Disconnected: $status")
        forceDisconnect(onDisconnect = true,
                        hciStatus = status)
        return super.onPumpDisconnected(peripheral, status)
    }

    fun forceDisconnect(onDisconnect: Boolean, hciStatus: HciStatus? = null,  tandemError: TandemError? = null) {
        aapsLogger.error(TAG, "forceDisconnect: onDisconnect=${onDisconnect}" +
            ", hciStatus=${hciStatus}, tandemError=${tandemError}")
        this.pumpStatus.disconnectData = DisconnectDataDto(onDisconnect = onDisconnect,
                                                           hciStatus = hciStatus,
                                                           tandemError = tandemError)
        pumpUtil.driverStatus = PumpDriverState.Disconnected
        pumpStatus.pumpRunningState = PumpRunningState.Unknown
        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.PumpStatus))
        tandemConnectionFixer.startConnectionFix()
    }


    fun sendInvalidPairingCodeError() {
        preferences.put(TandemIntPreferenceKey.PumpPairStatus, -2)
        pumpUtil.errorType = PumpErrorType.PumpPairInvalidPairCode

        pumpUtil.sendNotification(TandemNotificationType.InvalidPairingCodeReconfigure)

        this.errorConnecting = true
    }


    fun isPumpFullyConnected() : Boolean {
        return operationMode==OperationMode.StandardOperation ||
            operationMode==OperationMode.ExternalListenerOperation
    }

    fun isListenerEnabled(): Boolean {
        return this.communicationListener!=null
    }

    enum class OperationMode {
        None,
        ConnectionMode,
        StandardOperation,
        ExternalListenerOperation,
    }


}
