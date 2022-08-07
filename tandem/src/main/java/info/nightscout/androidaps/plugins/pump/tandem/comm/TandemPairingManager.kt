package info.nightscout.androidaps.plugins.pump.tandem.comm

import android.content.Context
import com.jwoglom.pumpx2.pump.bluetooth.TandemBluetoothHandler
import com.jwoglom.pumpx2.pump.bluetooth.TandemPump
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.response.authentication.CentralChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.authentication.PumpChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ApiVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpVersionResponse
import com.welie.blessed.BluetoothPeripheral
import info.nightscout.androidaps.plugins.pump.common.defs.PumpErrorType
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpApiVersion
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpConst
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.sharedPreferences.SP
import javax.inject.Inject

class TandemPairingManager @Inject constructor(
    var context: Context,
    var aapsLogger: AAPSLogger,
    var sp: SP,
    var pumpUtil: TandemPumpUtil
) : TandemPump(context) {

    var bluetoothHandler: TandemBluetoothHandler? = null

    fun startPairing(btAddress: String) {
        createBluetoothHandler()

        // TODO initiaate pairing


    }

    fun shutdownPairingManager() {
        stopBluetoothHandler()
    }


    open fun createBluetoothHandler(): TandemBluetoothHandler? {
        if (bluetoothHandler != null) {
            return bluetoothHandler
        }
        //tandemEventCallback = PumpX2TandemPump(getApplicationContext())
        bluetoothHandler = TandemBluetoothHandler.getInstance(context, this)
        return bluetoothHandler
    }

    open fun stopBluetoothHandler() {
        bluetoothHandler!!.stop()
        bluetoothHandler = null
        //return getBluetoothHandler()
    }

    override fun onReceiveMessage(peripheral: BluetoothPeripheral?, message: Message?) {

        // the parsed message which extends the Message class. To identify what type of
        // *                message was found, check with `if (message instanceof ApiVersionResponse)

        if (message is ApiVersionResponse) {
            sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 99)

            val apiVersionResponse = message as ApiVersionResponse

            sp.putString(TandemPumpConst.Prefs.PumpApiVersion, TandemPumpApiVersion.getApiVersionFromResponse(apiVersionResponse).name)
        } else if (message is PumpVersionResponse) {
            val pumpVersionResponse = message as PumpVersionResponse

            sp.putString(TandemPumpConst.Prefs.PumpSerial, "" + pumpVersionResponse.serialNum)
        }

        sp.putString(TandemPumpConst.Prefs.PumpAddress, peripheral!!.address)



        TODO("Not yet implemented")
    }

    override fun onWaitingForPairingCode(peripheral: BluetoothPeripheral?, centralChallenge: CentralChallengeResponse?) {

        // display dialog after success, initiate pairing
        var success = false
        var pairCode: String? = null

        sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 1)

        if (success) {
            sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 2)
            pair(peripheral, centralChallenge, pairCode)
        }


        TODO("Not yet implemented")
    }

    override fun onInvalidPairingCode(peripheral: BluetoothPeripheral?, resp: PumpChallengeResponse?) {
        sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, -1)
        pumpUtil.errorType = PumpErrorType.PumpPairInvalidPairCode

        // TODO display
    }
}