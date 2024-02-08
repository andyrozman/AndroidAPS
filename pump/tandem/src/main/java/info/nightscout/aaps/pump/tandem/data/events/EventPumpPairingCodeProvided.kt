package info.nightscout.aaps.pump.tandem.data.events

import app.aaps.core.interfaces.rx.events.Event


class EventPumpPairingCodeProvided(var pairingCode: String) : Event()