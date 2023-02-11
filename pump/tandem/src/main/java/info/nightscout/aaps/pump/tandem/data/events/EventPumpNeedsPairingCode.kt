package info.nightscout.aaps.pump.tandem.data.events

import info.nightscout.rx.events.Event

class EventPumpNeedsPairingCode(var instructions: String) : Event()