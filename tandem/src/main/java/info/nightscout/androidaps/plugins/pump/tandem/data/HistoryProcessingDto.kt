package info.nightscout.androidaps.plugins.pump.tandem.data

@Deprecated("Probably Not Needed with Tandem")
class HistoryProcessingDto constructor(fullEvents: MutableList<EventDto>) {

    var basalEntries: MutableList<EventDto>? = null
    var endListOfEvents: MutableList<EventDto>? = null
}