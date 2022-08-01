package info.nightscout.androidaps.plugins.pump.tandem.comm

import android.content.Context
import com.jwoglom.pumpx2.pump.TandemPump
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.builders.CentralChallengeBuilder
import com.jwoglom.pumpx2.pump.messages.response.authentication.CentralChallengeResponse
import com.welie.blessed.BluetoothPeripheral
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.sharedPreferences.SP
import javax.inject.Inject

class TandemCommunicationManager @Inject constructor(
    var context: Context,
    var aapsLogger: AAPSLogger,
    var sp: SP,
    var pumpUtil: TandemPumpUtil
) : TandemPump(context) {

    lateinit var peripheral: BluetoothPeripheral

    var responses: MutableMap<Int, Message> = mutableMapOf()

    override fun onInitialPumpConnection(peripheral: BluetoothPeripheral?)  {
        super.onInitialPumpConnection(peripheral)
        this.peripheral = peripheral!!
    }


    fun sendCommand() {
        //this.sendCommand()
    }



    override fun onReceiveMessage(peripheral: BluetoothPeripheral?, message: Message?) {
        TODO("Not yet implemented")





    }

    override fun onWaitingForPairingCode(peripheral: BluetoothPeripheral?, centralChallenge: CentralChallengeResponse?) {
    }

}