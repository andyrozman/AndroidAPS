package info.nightscout.aaps.pump.tandem.data.events

import info.nightscout.rx.events.Event

class EventPumpPairingCodeProvided(var pairingCode: String) : Event()