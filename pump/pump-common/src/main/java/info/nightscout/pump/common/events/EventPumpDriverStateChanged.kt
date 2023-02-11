package info.nightscout.androidaps.plugins.pump.common.events

import info.nightscout.rx.events.Event
import info.nightscout.pump.common.defs.PumpDriverState

class EventPumpDriverStateChanged(var driverStatus: PumpDriverState) : Event()
