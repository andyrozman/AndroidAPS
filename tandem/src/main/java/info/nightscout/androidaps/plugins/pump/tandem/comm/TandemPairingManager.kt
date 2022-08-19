package info.nightscout.androidaps.plugins.pump.tandem.comm

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.SystemClock
import android.text.InputType
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jwoglom.pumpx2.pump.bluetooth.TandemBluetoothHandler
import com.jwoglom.pumpx2.pump.bluetooth.TandemPump
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.ApiVersionRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.PumpVersionRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.TimeSinceResetRequest
import com.jwoglom.pumpx2.pump.messages.response.authentication.CentralChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.authentication.PumpChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ApiVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TimeSinceResetResponse
import com.welie.blessed.BluetoothPeripheral
import info.nightscout.androidaps.extensions.runOnUiThread
import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.interfaces.ResourceHelper
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.defs.PumpErrorType
import info.nightscout.androidaps.plugins.pump.common.events.EventPumpConnectionParametersChanged
import info.nightscout.androidaps.plugins.pump.tandem.R
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpApiVersion
import info.nightscout.androidaps.plugins.pump.tandem.driver.TandemPumpStatus
import info.nightscout.androidaps.plugins.pump.tandem.event.EventPumpStatusChanged
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpConst
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.androidaps.utils.alertDialogs.AlertDialogHelper
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import info.nightscout.shared.sharedPreferences.SP
import java.util.*
import javax.inject.Inject

class TandemPairingManager constructor(
    var context: Context,
    var aapsLogger: AAPSLogger,
    var sp: SP,
    var pumpUtil: TandemPumpUtil,
    var btAddress: String,
    var resourceHelper: ResourceHelper,
    var rxBus: RxBus,
    var pumpStatus: TandemPumpStatus,
    var pumpSync: PumpSync
) : TandemPump(context, Optional.of(btAddress)) {

    var bluetoothHandler: TandemBluetoothHandler? = null

    fun startPairing() {
        aapsLogger.info(LTag.PUMPCOMM, "start Pairing")
        createBluetoothHandler()

        bluetoothHandler!!.startScan()
    }

    fun shutdownPairingManager() {
        stopBluetoothHandler()
    }


    fun createBluetoothHandler(): TandemBluetoothHandler? {
        if (bluetoothHandler != null) {
            return bluetoothHandler
        }

        bluetoothHandler = TandemBluetoothHandler.getInstance(context, this)
        return bluetoothHandler
    }

    fun stopBluetoothHandler() {
        bluetoothHandler!!.stop()
        bluetoothHandler = null
        //return getBluetoothHandler()
    }

    // Pair Status

    override fun onReceiveMessage(peripheral: BluetoothPeripheral, message: Message) {
        aapsLogger.info(LTag.PUMPCOMM, "received message: opCode=${message.opCode()}")

        if (message is ApiVersionResponse) {
            sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 80)
            sp.putString(TandemPumpConst.Prefs.PumpAddress, peripheral.address)
            sp.putString(TandemPumpConst.Prefs.PumpName, peripheral.name)

            val apiVersionResponse = message
            val apiVersion = TandemPumpApiVersion.getApiVersionFromResponse(apiVersionResponse)

            aapsLogger.info(LTag.PUMPCOMM, "Api Version: ${apiVersionResponse.majorVersion}.${apiVersionResponse.minorVersion} : ${apiVersion.name} ")

            sp.putString(TandemPumpConst.Prefs.PumpApiVersion, apiVersion.name)
        } else if (message is TimeSinceResetResponse) {
            sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 90)
            val timeSinceResponse = message
            aapsLogger.info(LTag.PUMPCOMM, "TimeSinceResetResponse: ${timeSinceResponse}")
        } else if (message is PumpVersionResponse) {
            sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 100)
            val pumpVersionResponse = message

            aapsLogger.info(LTag.PUMPCOMM, "PumpVersionResponse: ${pumpVersionResponse}")

            sp.putString(TandemPumpConst.Prefs.PumpSerial, "" + pumpVersionResponse.serialNum)
            sp.putString(TandemPumpConst.Prefs.PumpVersionResponse, pumpUtil.gson.toJson(message))

            pumpStatus.serialNumber = pumpVersionResponse.serialNum

            pumpSync.connectNewPump()

            finalPairingStatus()

            rxBus.send(EventPumpConnectionParametersChanged())
        }

    }

    override fun onWaitingForPairingCode(peripheral: BluetoothPeripheral, centralChallenge: CentralChallengeResponse) {

        aapsLogger.info(LTag.PUMPCOMM, "onWaitingForPairingCode:")

        sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 40)

        triggerPairDialog(peripheral, btAddress, centralChallenge)

        // if (success) {
        //     sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 2)
        //     pair(peripheral, centralChallenge, pairCode)
        // }


        //TODO("Not yet implemented")
    }

    override fun onInvalidPairingCode(peripheral: BluetoothPeripheral, resp: PumpChallengeResponse) {
        sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, -1)
        pumpUtil.errorType = PumpErrorType.PumpPairInvalidPairCode

        aapsLogger.error(LTag.PUMPCOMM, "PairingCode WAS INVALID.")


        //PumpState.failedPumpConnectionAttempts++

        rxBus.send(EventPumpStatusChanged(PumpDriverState.ErrorCommunicatingWithPump))

        // AlertDialog.Builder(context)
        //     .setTitle("Pump Connection")
        //     .setMessage("The pump rejected the pairing code. You need to unpair and re-pair the device in Bluetooth Settings. Press OK to enter the new code.")
        //     .setPositiveButton(R.string.yes) { dialog, which ->
        //         val intent = Intent(PUMP_CONNECTED_STAGE1_INTENT)
        //         intent.putExtra("address", peripheral!!.address)
        //         intent.putExtra("name", peripheral!!.name)
        //         context.sendBroadcast(intent)
        //     }
        //     .setNegativeButton(R.string.no, null)
        //     .setIcon(R.drawable.ic_dialog_alert)
        //     .show()

        // rxBus.send()
    }

    private fun triggerPairDialog(peripheral: BluetoothPeripheral, btAddress: String, challenge: CentralChallengeResponse) {
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
            sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 50)
            sp.putString(TandemPumpConst.Prefs.PumpPairCode, pairingCode)

            aapsLogger.info(LTag.PUMPCOMM, "PairingCode Accepted: ${pairingCode}")

            pair(peripheral, challenge, pairingCode)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }


    private fun triggerAAPSPairDialog(peripheral: BluetoothPeripheral, btAddress: String, challenge: CentralChallengeResponse) {

        val btName = peripheral.name
        var okClicked = false

        // Set up the input
        val input = EditText(this.context)
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL

        MaterialAlertDialogBuilder(context, R.style.DialogTheme)
            .setCustomTitle(AlertDialogHelper.buildCustomTitle(context, resourceHelper.gs(R.string.tandem_ble_config_pairing_title)))
            .setMessage(resourceHelper.gs(R.string.tandem_ble_config_pairing_message, btName, btAddress))
            .setView(input)
            .setPositiveButton(context.getString(R.string.ok)) { dialog: DialogInterface, _: Int ->
                if (okClicked) return@setPositiveButton
                else {
                    okClicked = true
                    dialog.dismiss()
                    SystemClock.sleep(100)
                    val pairingCode = input.text.toString()
                    sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 50)
                    sp.putString(TandemPumpConst.Prefs.PumpPairCode, pairingCode)

                    aapsLogger.info(LTag.PUMPCOMM, "PairingCode Accepted: ${pairingCode}")

                    pair(peripheral, challenge, pairingCode)
                }
            }
            .setNegativeButton(context.getString(R.string.cancel)) { dialog: DialogInterface, _: Int ->
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



    //  40 = onWaitingForPairingCode
    //  50 pairing code set
    //  -1 = onInvalidPairingCode
    //  70 = connected;
    //  80 = ApiVersionResponse;
    //  90 = TimeSinceResetResponse;
    //  100 = PumpVersionResponse

    override fun onPumpConnected(peripheral: BluetoothPeripheral?) {
        aapsLogger.info(LTag.PUMPCOMM, "onPumpConnected")
        sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 70)

        sendCommand(peripheral, ApiVersionRequest())
        sendCommand(peripheral, TimeSinceResetRequest())
        sendCommand(peripheral, PumpVersionRequest())
    }


    fun finalPairingStatus() {

        val stringBuilder = StringBuilder()

        stringBuilder.append("PAIRING STATUS\n")
        stringBuilder.append("-------------------\n")
        stringBuilder.append("Pump Pair Status: ${sp.getInt(TandemPumpConst.Prefs.PumpPairStatus, -100)} \n")
        stringBuilder.append("PumpPairCode: ${sp.getString(TandemPumpConst.Prefs.PumpPairCode, "-")}\n")
        stringBuilder.append("PumpAddress: ${sp.getString(TandemPumpConst.Prefs.PumpAddress, "-")}\n")
        stringBuilder.append("PumpName: ${sp.getString(TandemPumpConst.Prefs.PumpName, "-")}\n")
        stringBuilder.append("PumpSerial: ${sp.getString(TandemPumpConst.Prefs.PumpSerial, "-")}\n")
        stringBuilder.append("Pump Version Response: ${sp.getString(TandemPumpConst.Prefs.PumpVersionResponse, "-")} \n")

        aapsLogger.info(LTag.PUMPCOMM, "onPairingStatus: \n ${stringBuilder.toString()}")

    }


}