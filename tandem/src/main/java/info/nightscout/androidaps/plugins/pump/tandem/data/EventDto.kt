package info.nightscout.androidaps.plugins.pump.tandem.data

import androidx.annotation.StringRes
import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.plugins.pump.common.defs.PumpHistoryEntryGroup
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpDataConverter
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryEntry
import info.nightscout.androidaps.plugins.pump.tandem.R
import info.nightscout.androidaps.plugins.pump.tandem.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.tandem.defs.YpsoPumpEventType
import info.nightscout.androidaps.interfaces.ResourceHelper

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

    lateinit var resolvedDate: String
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

        resolvedDate = resourceHelper.gs(R.string.ypsopump_history_date, dateTime.day, dateTime.month, dateTime.year, dateTime.hour, dateTime.minute, dateTime.second)
        resolvedType = resourceHelper.gs(entryType.getDescriptionResourceId())
        resolvedValue = getDisplayableValue(resourceHelper, ypsoPumpDataConverter)
    }

    override fun getEntryDateTime(): String = resolvedDate

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

    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return resourceHelper.gs(
            R.string.ypsopump_history_basal_profile,
            ypsoPumpDataConverter.convertBasalProfileToString(profile, ", ")
        )
    }

}

data class BasalProfileEntry(var hour: Int,
                             var rate: Double) : EventObject() {

    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return String.format("%02d=%.2f", hour, rate)
    }

}

data class TotalDailyInsulin(var bolus: Double,
                             var basal: Double,
                             var total: Double,
                             var isTotalOnly: Boolean): EventObject() {
    constructor(total: Double) : this(0.0, 0.0, total, true)

    constructor(bolus: Double, basal: Double) : this(bolus, basal, basal + bolus, false)

    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        if (isTotalOnly) {
            return resourceHelper.gs(R.string.ypsopump_history_tdd_total_insulin, total)
        } else {
            return resourceHelper.gs(R.string.ypsopump_history_tdd_parts_insulin, basal, bolus, total)
        }
    }

}

enum class BolusType(@StringRes var stringId: Int) {
    Normal(R.string.ypsopump_history_bolus_normal),
    Extended(R.string.ypsopump_history_bolus_extended),
    Combined(R.string.ypsopump_history_bolus_combined),
    SMB(R.string.ypsopump_history_bolus_smb),
    Priming(R.string.ypsopump_history_bolus_prime)
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

    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return when (bolusType) {
            BolusType.Normal   -> resourceHelper.gs(bolusType.stringId, immediateAmount)
            BolusType.Extended -> resourceHelper.gs(bolusType.stringId, extendedAmount, durationMin)
            BolusType.Combined -> resourceHelper.gs(bolusType.stringId, immediateAmount, extendedAmount, durationMin)
            BolusType.SMB      -> resourceHelper.gs(bolusType.stringId, immediateAmount)
            BolusType.Priming  -> resourceHelper.gs(bolusType.stringId, immediateAmount)
        }
    }
}

data class TemporaryBasal(
    var percent: Int,
    var minutes: Int,
    var isRunning: Boolean,
    var temporaryBasalType: PumpSync.TemporaryBasalType = PumpSync.TemporaryBasalType.NORMAL
) : EventObject() {

    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return resourceHelper.gs(R.string.ypsopump_history_tbr, percent, minutes)
    }
}

enum class AlarmType(@StringRes var stringId: Int, var parameterCount: Int = 0) {
    BatteryRemoved(R.string.ypsopump_alarm_battery_removed),
    BatteryEmpty(R.string.ypsopump_alarm_battery_empty, 1),
    ReusableError(R.string.ypsopump_alarm_reusable_error, 3),
    NoCartridge(R.string.ypsopump_alarm_no_cartridge),
    CartridgeEmpty(R.string.ypsopump_alarm_cartridge_empty),
    Occlusion(R.string.ypsopump_alarm_occlusion, 1),
    AutoStop(R.string.ypsopump_alarm_auto_stop),
    LipoDischarged(R.string.ypsopump_alarm_lipo_discharged, 1),
    BatteryRejected(R.string.ypsopump_alarm_battery_rejected, 1)
}

data class Alarm(
    var alarmType: AlarmType,
    var value1: Int? = null,
    var value2: Int? = null,
    var value3: Int? = null
) : EventObject() {

    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return resourceHelper.gs(R.string.ypsopump_history_alarm, resourceHelper.gs(alarmType.stringId))
    }
}

enum class ConfigurationType(@StringRes var stringId: Int) {
    BolusStepChanged(R.string.ypsopump_config_bolus_step_changed),
    BolusAmountCapChanged(R.string.ypsopump_config_bolus_amount_cap_changed),
    BasalAmountCapChanged(R.string.ypsopump_config_basal_amount_cap_changed),
    BasalProfileChanged(R.string.ypsopump_config_basal_profile_changed)
}

data class ConfigurationChanged(var configurationType: ConfigurationType,
                                var value: String) : EventObject() {
    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return resourceHelper.gs(R.string.ypsopump_history_configuration_changed, resourceHelper.gs(configurationType.stringId), value)
    }
}

enum class PumpStatusType(@StringRes var stringId: Int) {
    PumpRunning(R.string.ypsopump_pump_status_type_pump_running),
    PumpSuspended(R.string.ypsopump_pump_status_type_pump_suspended),
    Priming(R.string.ypsopump_pump_status_type_priming),
    Rewind(R.string.ypsopump_pump_status_type_rewind),
    BatteryRemoved(R.string.ypsopump_pump_status_type_battery_removed)
}


data class PumpStatusChanged(var pumpStatusType: PumpStatusType,
                             var additonalData: String? = null): EventObject() {

    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        return if (pumpStatusType == PumpStatusType.Priming) {
            resourceHelper.gs(pumpStatusType.stringId, additonalData)
        } else {
            resourceHelper.gs(pumpStatusType.stringId)
        }
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

    override fun getDisplayableValue(resourceHelper: ResourceHelper, ypsoPumpDataConverter: YpsoPumpDataConverter): String {
        val dt = resourceHelper.gs(R.string.ypsopump_history_date, day, month, year, hour, minute, second)
        return if (timeChanged) {
            resourceHelper.gs(R.string.ypsopump_history_time_changed, dt)
        } else {
            resourceHelper.gs(R.string.ypsopump_history_date_changed, dt)
        }
    }
}