package info.nightscout.androidaps.plugins.pump.tandem.event

import info.nightscout.rx.events.Event

class EventPumpPairingCodeProvided(var pairingCode: String) : Event()