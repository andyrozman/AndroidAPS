package info.nightscout.aaps.pump.tandem.data.events

import info.nightscout.rx.events.Event
import info.nightscout.pump.common.defs.PumpDriverState

class EventPumpDriverStateChanged(var driverStatus: PumpDriverState) : Event()
