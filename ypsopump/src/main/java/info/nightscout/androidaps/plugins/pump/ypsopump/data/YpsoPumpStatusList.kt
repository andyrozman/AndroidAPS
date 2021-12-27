package info.nightscout.androidaps.plugins.pump.ypsopump.comm.data

data class YpsoPumpStatusEntry(var serialNumber: Long,
                               var lastEventSequenceNumber: Int? = null,
                               var lastAlarmSequenceNumber: Int? = null,
                               var lastSystemEntrySequenceNumber: Int? = null,
                               var lastPrimeEvent: Int? = null,
                               var lastRewindvent: Int? = null,
                               var isPumpRunning: Boolean = true,
                               var runningEventsSequences: MutableSet<Int>? = mutableSetOf()) {

    fun getLastEntry(entryType: LastEntryType): Int? {
        return when (entryType) {
            LastEntryType.Prime                     -> this.lastPrimeEvent
            LastEntryType.Rewind                    -> this.lastRewindvent
            LastEntryType.EventSequenceNumber       -> this.lastEventSequenceNumber
            LastEntryType.AlarmSequenceNumber       -> this.lastAlarmSequenceNumber
            LastEntryType.SystemEntrySequenceNumber -> this.lastSystemEntrySequenceNumber
        }
    }

    fun setLastEntry(entryType: LastEntryType, value: Int) {
        when (entryType) {
            LastEntryType.Prime                     -> this.lastPrimeEvent = value
            LastEntryType.Rewind                    -> this.lastRewindvent = value
            LastEntryType.EventSequenceNumber       -> lastEventSequenceNumber = value
            LastEntryType.AlarmSequenceNumber       -> lastAlarmSequenceNumber = value
            LastEntryType.SystemEntrySequenceNumber -> lastSystemEntrySequenceNumber = value
        }
    }

}

data class YpsoPumpStatusList(var map: MutableMap<Long, YpsoPumpStatusEntry>)

enum class LastEntryType {
    Prime,
    Rewind,
    EventSequenceNumber,
    AlarmSequenceNumber,
    SystemEntrySequenceNumber
}