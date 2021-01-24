package info.nightscout.androidaps.plugins.pump.ypsopump.event

import info.nightscout.androidaps.events.Event

// This is event sent from device selection which requires us to re-read firmware version of device
class EventPumpConfigurationChanged : Event()
