package info.nightscout.aaps.pump.tandem.data.events

import info.nightscout.rx.events.Event

// This is event sent from device selection which requires us to re-read firmware version of device
class EventPumpConfigurationChanged : Event()
