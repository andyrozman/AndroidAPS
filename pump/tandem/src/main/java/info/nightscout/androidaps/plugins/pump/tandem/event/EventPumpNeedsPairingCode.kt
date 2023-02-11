package info.nightscout.androidaps.plugins.pump.tandem.event

import info.nightscout.rx.events.Event

class EventPumpNeedsPairingCode(var instructions: String) : Event()