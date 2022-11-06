package info.nightscout.androidaps.plugins.pump.tandem.event

import info.nightscout.androidaps.events.Event
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState

class EventPumpDriverStateChanged(var driverStatus: PumpDriverState) : Event()
