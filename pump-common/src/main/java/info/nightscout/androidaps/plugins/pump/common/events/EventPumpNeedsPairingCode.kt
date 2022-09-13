package info.nightscout.androidaps.plugins.pump.common.events

import info.nightscout.androidaps.events.Event

class EventPumpNeedsPairingCode(var instructions: String) : Event()