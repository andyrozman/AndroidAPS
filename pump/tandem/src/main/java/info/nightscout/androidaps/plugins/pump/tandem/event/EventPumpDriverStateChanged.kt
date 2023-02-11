package info.nightscout.androidaps.plugins.pump.tandem.event

import info.nightscout.rx.events.Event
import info.nightscout.pump.common.defs.PumpDriverState

class EventPumpDriverStateChanged(var driverStatus: PumpDriverState) : Event()
