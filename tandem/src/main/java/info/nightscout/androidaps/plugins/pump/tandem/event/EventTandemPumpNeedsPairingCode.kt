package info.nightscout.androidaps.plugins.pump.tandem.event

import com.jwoglom.pumpx2.pump.messages.response.authentication.CentralChallengeResponse
import com.welie.blessed.BluetoothPeripheral
import info.nightscout.androidaps.events.Event

class EventTandemPumpNeedsPairingCode(var peripheral: BluetoothPeripheral, var btAddress: String, var challenge: CentralChallengeResponse) : Event()
