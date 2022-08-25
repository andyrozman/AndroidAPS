package info.nightscout.androidaps.plugins.pump.tandem.data.history

import info.nightscout.androidaps.interfaces.ResourceHelper
import info.nightscout.androidaps.plugins.pump.common.defs.PumpHistoryEntryGroup
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpDataConverter
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryEntry
import info.nightscout.androidaps.plugins.pump.tandem.R
import info.nightscout.androidaps.plugins.pump.tandem.comm.TandemDataConverter
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpHistoryType
import java.util.*

sealed class HistoryLogObject {
    abstract fun getDisplayableValue(resourceHelper: ResourceHelper, dataConverter: TandemDataConverter): String
}

data class HistoryLogDto(var id: Long?,
                         var serial: Long,
                         var historyTypeIndex: Int,
                         var historyType: TandemPumpHistoryType,
                         var sequenceNum: Long,
                         var dateTimeMillis: Long,
                         var payload: String,
                         var subObject: HistoryLogObject? = null,
                         //var subObject2: HistoryLogObject? = null, // this is used only for fake TBR that emulates Pump Start/Stop for now
                         var created: Long = System.currentTimeMillis(),
                         var updated: Long = System.currentTimeMillis()
) : Comparable<HistoryLogDto>, PumpHistoryEntry {

    lateinit var resolvedDate: String
    lateinit var resolvedType: String
    lateinit var resolvedValue: String

    val dateTimeString: String
        get() = resolvedDate

    fun getDisplayableValue(resourceHelper: ResourceHelper, dataConverter: TandemDataConverter): String {
        if (subObject != null) {
            return subObject!!.getDisplayableValue(resourceHelper, dataConverter)
        } else {
            return "???"
        }
    }

    override fun prepareEntryData(resourceHelper: ResourceHelper, pumpDataConverter: PumpDataConverter) {
        val tandemPumpDataConverter = pumpDataConverter as TandemDataConverter

        val dateTimeObject = GregorianCalendar()
        dateTimeObject.timeInMillis = dateTimeMillis

        resolvedDate = resourceHelper.gs(R.string.tandem_history_date,
                                         dateTimeObject.get(Calendar.DAY_OF_MONTH),
                                         dateTimeObject.get(Calendar.MONTH),
                                         dateTimeObject.get(Calendar.YEAR),
                                         dateTimeObject.get(Calendar.HOUR_OF_DAY),
                                         dateTimeObject.get(Calendar.MINUTE),
                                         dateTimeObject.get(Calendar.SECOND))
        resolvedType = resourceHelper.gs(historyType.getDescriptionResourceId())
        resolvedValue = getDisplayableValue(resourceHelper, tandemPumpDataConverter)
    }

    override fun getEntryDateTime(): String = resolvedDate

    override fun getEntryType(): String = resolvedType

    override fun getEntryValue(): String = resolvedValue

    override fun getEntryTypeGroup(): PumpHistoryEntryGroup = historyType.group

    override fun toString(): String {

        var entryTypeFormated = historyType.name

        if (entryTypeFormated.length > 24) {
            entryTypeFormated = entryTypeFormated.substring(0, 24)
        } else if (entryTypeFormated.length < 24) {
            entryTypeFormated = entryTypeFormated.padEnd(24, ' ')
        }

        var sequenceString = "" + sequenceNum
        sequenceString = sequenceString.padStart(8, ' ')

        val dataLine = resolvedDate + "   " + entryTypeFormated + "  " + sequenceString

        if (subObject == null) {
            return dataLine + "      No Sub Object"
        } else {
            return dataLine + "      " + subObject.toString()
        }
    }

    override fun compareTo(other: HistoryLogDto): Int {
        return (this.sequenceNum - other.sequenceNum).toInt()
    }

}


data class DateTimeChanged(var year: Int? = 0,
                           var month: Int? = 0,
                           var day: Int? = 0,
                           var hour: Int? = 0,
                           var minute: Int? = 0,
                           var second: Int? = 0,
                           var timeChanged: Boolean
): HistoryLogObject() {

    override fun getDisplayableValue(resourceHelper: ResourceHelper, dataConverter: TandemDataConverter): String {
        val dt = resourceHelper.gs(R.string.tandem_history_date, day, month, year, hour, minute, second)
        return if (timeChanged) {
            resourceHelper.gs(R.string.tandem_history_time_changed, dt)
        } else {
            resourceHelper.gs(R.string.tandem_history_date_changed, dt)
        }
    }
}
