package info.nightscout.androidaps.plugins.pump.tandem.event

import com.jwoglom.pumpx2.pump.messages.response.authentication.CentralChallengeResponse
import com.welie.blessed.BluetoothPeripheral
import info.nightscout.androidaps.events.Event
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState

class EventTandemPumpHasPairingCode(var peripheral: BluetoothPeripheral, var btAddress: String, var challenge: CentralChallengeResponse, var pairingCode: String) : Event()
