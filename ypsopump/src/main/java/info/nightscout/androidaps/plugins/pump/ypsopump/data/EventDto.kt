package info.nightscout.androidaps.plugins.pump.ypsopump.data

import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.plugins.pump.common.defs.PumpHistoryEntryGroup
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpDataConverter
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryEntry
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType
import info.nightscout.androidaps.utils.resources.ResourceHelper

sealed class EventObject {
    abstract fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String
}

data class EventDto(var id: Int?,
                    var serial: Long,
                    var historyEntryType: HistoryEntryType,
                    var dateTime: DateTimeDto,
                    var entryType: YpsoPumpEventType,
                    var entryTypeAsInt: Int,
                    var value1: Int,
                    var value2: Int,
                    var value3: Int,
                    var eventSequenceNumber: Int,
                    var subObject: EventObject? = null,
                    var subObject2: EventObject? = null, // this is used only for fake TBR that emulates Pump Start/Stop for now
                    var created: Long? = null,
                    var updated: Long? = null
) : Comparable<EventDto>, PumpHistoryEntry {

    lateinit var resolvedType: String
    lateinit var resolvedValue: String

    val dateTimeString: String
        get() = "${dateTime.day}.${dateTime.month}.${dateTime.year} ${dateTime.hour}:${dateTime.minute}:${dateTime.second}"

    fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        if (subObject != null) {
            return subObject!!.getDisplayableValue(resourceHelper, ypsoPumpDataConverter)
        } else {
            return "???"
        }
    }

    override fun prepareEntryData(resourceHelper: ResourceHelper, pumpDataConverter: PumpDataConverter) {
        val ypsoPumpDataConverter = pumpDataConverter as YpsoPumpDataConverter

        resolvedType = entryType.name
        resolvedValue = getDisplayableValue(resourceHelper, ypsoPumpDataConverter)
    }

    override fun getEntryDateTime(): String = dateTimeString

    override fun getEntryType(): String = resolvedType

    override fun getEntryValue(): String = resolvedValue

    override fun getEntryTypeGroup(): PumpHistoryEntryGroup = entryType.group

    override fun toString(): String {

        var entryTypeFormated = entryType.name

        if (entryTypeFormated.length > 24) {
            entryTypeFormated = entryTypeFormated.substring(0, 24)
        } else if (entryTypeFormated.length < 24) {
            entryTypeFormated = entryTypeFormated.padEnd(24, ' ')
        }

        var sequenceString = "" + eventSequenceNumber
        sequenceString = sequenceString.padStart(8, ' ')

        val dataLine = dateTime.toString() + "   " + entryTypeFormated + "  " + sequenceString

        if (subObject == null) {
            return dataLine + "      value1=" + value1 + ", value2=" + value2 + ", value3=" + value3
        } else {
            return dataLine + "      " + subObject.toString() + " " + dateTime.toLocalDateTime()
        }
    }

    override fun compareTo(other: EventDto): Int {
        return this.eventSequenceNumber - other.eventSequenceNumber
    }
}

enum class HistoryEntryType {
    Event,
    Alarm,
    SystemEntry
}

data class BasalProfile(var profile: HashMap<Int, BasalProfileEntry>) : EventObject() {

    // TODO i18n
    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return ypsoPumpDataConverter.convertBasalProfileToString(profile, ", ")
    }

}

data class BasalProfileEntry(var hour: Int,
                             var rate: Double) : EventObject() {

    // TODO i18n
    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        if (hour<10)
            return "0" + hour + "=" + String.format("%.2f", rate)
        else
            return "" + hour + "=" + String.format("%d=%.2f", hour, rate)
    }

}

data class TotalDailyInsulin(var bolus: Double,
                             var basal: Double,
                             var total: Double,
                             var isTotalOnly: Boolean): EventObject() {
    constructor(total: Double) : this(0.0, 0.0, total, true)

    constructor(bolus: Double, basal: Double) : this(bolus, basal, basal + bolus, false)

    // TODO i18n
    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        if (isTotalOnly) {
            return "Basal & Bolus: " + total
        } else {
            return "Basal: $basal, Bolus: $bolus, Total: $total"
        }
    }

}

enum class BolusType {
    Normal,
    Extended,
    Combined,
    SMB,
    Priming
}


data class Bolus(var bolusType: BolusType,
                 var immediateAmount: Double?,
                 var extendedAmount: Double?,
                 var durationMin: Int?,
                 var isCalculated: Boolean,
                 var isCancelled: Boolean,
                 var isRunning: Boolean): EventObject() {

    constructor(immediateAmount: Double?,
                isCalculated: Boolean,
                isCancelled: Boolean,
                isRunning: Boolean) : this(BolusType.Normal, immediateAmount, null, null, isCalculated, isCancelled, isRunning)

    constructor(extendedAmount: Double?,
                durationMin: Int?,
                isCalculated: Boolean,
                isCancelled: Boolean,
                isRunning: Boolean) : this(BolusType.Extended, null, extendedAmount, durationMin, isCalculated, isCancelled, isRunning)

    constructor(immediateAmount: Double?,
                extendedAmount: Double?,
                durationMin: Int?,
                isCalculated: Boolean,
                isCancelled: Boolean,
                isRunning: Boolean) : this(BolusType.Combined, immediateAmount, extendedAmount, durationMin, isCalculated, isCancelled, isRunning)

    // TODO i18n
    // TODO show values
    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return when (bolusType) {
            BolusType.Normal   -> "Bolus: Normal, Amount: $immediateAmount"
            BolusType.Extended -> "Bolus: Extended, Amount: $extendedAmount, Duration: $durationMin min"
            BolusType.Combined -> "Bolus: Combined, Immediate Amount: $immediateAmount, Extended Amount: $extendedAmount, Duration: $durationMin min"
            BolusType.SMB      -> "Bolus: SMB, Amount: $immediateAmount"
            BolusType.Priming  -> "Bolus: Priming, Amount: $immediateAmount"
        }
    }

}

data class TemporaryBasal(var percent: Int,
                          var minutes: Int,
                          var isRunning: Boolean,
                          var temporaryBasalType: PumpSync.TemporaryBasalType = PumpSync.TemporaryBasalType.NORMAL) : EventObject() {

    // TODO i18n
    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return "Rate: " + percent + "%, " + minutes + " min"
    }

}

enum class AlarmType(var parameterCount: Int = 0) {
    BatteryRemoved,
    BatteryEmpty(1),
    ReusableError(3),
    NoCartridge,
    CartridgeEmpty,
    Occlusion(1),
    AutoStop,
    LipoDischarged(1),
    BatteryRejected(1)
}


data class Alarm(var alarmType: AlarmType,
                 var value1: Int? = null,
                 var value2: Int? = null,
                 var value3: Int? = null): EventObject() {

    // TODO i18n
    // TODO show values
    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return "Alarm: " + alarmType.name
    }

}

enum class ConfigurationType {
    BolusStepChanged,
    BolusAmountCapChanged,
    BasalAmountCapChanged,
    BasalProfileChanged
}


data class ConfigurationChanged(var configurationType: ConfigurationType,
                                var value: String) : EventObject() {
    // TODO i18n
    // TODO show values
    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return configurationType.name + ": " + value
    }
}

enum class PumpStatusType {
    PumpRunning,
    PumpSuspended,
    Priming,
    Rewind,
    BatteryRemoved
}


data class PumpStatusChanged(var pumpStatusType: PumpStatusType,
                             var additonalData: String? = null): EventObject() {

    // TODO i18n
    // TODO show values
    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return pumpStatusType.name + " " + (if (additonalData!=null) additonalData else "")
    }

}

data class DateTimeChanged(var year: Int? = 0,
                       var month: Int? = 0,
                       var day: Int? = 0,
                       var hour: Int? = 0,
                       var minute: Int? = 0,
                       var second: Int? = 0,
                       var timeChanged: Boolean
): EventObject() {

    // TODO i18n
    // TODO show values
    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return "New Date/Time: $day.$month.$year $hour:$minute:$second"
    }
}