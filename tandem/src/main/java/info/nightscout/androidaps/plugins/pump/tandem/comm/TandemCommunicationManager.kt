package info.nightscout.androidaps.plugins.pump.tandem.comm

import android.content.Context
import com.jwoglom.pumpx2.pump.bluetooth.TandemBluetoothHandler
import com.jwoglom.pumpx2.pump.bluetooth.TandemPump

import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.builders.CentralChallengeBuilder
import com.jwoglom.pumpx2.pump.messages.response.authentication.CentralChallengeResponse
import com.welie.blessed.BluetoothPeripheral
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.sharedPreferences.SP
import java.util.*
import javax.inject.Inject

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

    var responses: MutableMap<Int, Message> = mutableMapOf()

    var bluetoothHandler: TandemBluetoothHandler? = null

    fun connect(): Boolean {
        if (bluetoothHandler==null) {
            createBluetoothHandler()
        }

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
            bluetoothHandler = null
        }
        connected = false
        inConnectMode = false

    }


    open fun createBluetoothHandler(): TandemBluetoothHandler? {
        if (bluetoothHandler != null) {
            return bluetoothHandler
        }
        //tandemEventCallback = PumpX2TandemPump(getApplicationContext())
        bluetoothHandler = TandemBluetoothHandler.getInstance(context, this)
        return bluetoothHandler
    }

    override fun onInitialPumpConnection(peripheral: BluetoothPeripheral?)  {
        super.onInitialPumpConnection(peripheral)
        this.peripheral = peripheral!!
    }


    fun sendCommand() {
        //this.sendCommand()
    }



    override fun onReceiveMessage(peripheral: BluetoothPeripheral?, message: Message?) {
        //TODO("Not yet implemented")

        if (inConnectMode)  {





        } else {

        }







    }

    override fun onWaitingForPairingCode(peripheral: BluetoothPeripheral?, centralChallenge: CentralChallengeResponse?) {



    }



}