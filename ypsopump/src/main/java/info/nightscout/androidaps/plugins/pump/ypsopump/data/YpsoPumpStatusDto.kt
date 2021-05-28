package info.nightscout.androidaps.plugins.pump.ypsopump.comm.data

data class YpsoPumpStatusEntry(var serialNumber: Long,
                               var lastEventSequenceNumber: Int?,
                               var lastEventDate: Long?,
                               var lastAlarmSequenceNumber: Int?,
                               var lastAlarmDate: Long?,
                               var lastSystemEntrySequenceNumber: Int?,
                               var lastSystemEntryDate: Long?,
                               var isPumpRunning: Boolean = true,
                               var runningEventsSequences: MutableSet<Int>? = mutableSetOf()) {

    constructor(serialNumber: Long) : this(serialNumber,
        null,
        null,
        null,
        null,
        null,
        null)

}

data class YpsoPumpStatusList(var map: MutableMap<Long, YpsoPumpStatusEntry>)