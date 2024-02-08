package info.nightscout.aaps.pump.common.events

import app.aaps.core.interfaces.rx.events.Event
import info.nightscout.pump.common.defs.PumpDriverState

class EventPumpDriverStateChanged(var driverStatus: PumpDriverState) : Event()
