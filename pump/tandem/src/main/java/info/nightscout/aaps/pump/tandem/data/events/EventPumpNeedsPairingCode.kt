package info.nightscout.aaps.pump.tandem.data.events

import app.aaps.core.interfaces.rx.events.Event

class EventPumpNeedsPairingCode(var instructions: String) : Event()