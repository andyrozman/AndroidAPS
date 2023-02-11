package info.nightscout.androidaps.plugins.pump.tandem.data.history

import androidx.annotation.StringRes
import info.nightscout.androidaps.plugins.pump.common.defs.PumpBolusType
import info.nightscout.pump.common.defs.PumpHistoryEntryGroup
import info.nightscout.pump.common.driver.history.PumpDataConverter
import info.nightscout.pump.common.driver.history.PumpHistoryEntry
import info.nightscout.androidaps.plugins.pump.tandem.R
import info.nightscout.androidaps.plugins.pump.tandem.comm.TandemDataConverter
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpHistoryType
import info.nightscout.shared.interfaces.ResourceHelper
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


data class Bolus(var bolusType: PumpBolusType,
                 var immediateAmount: Double?,
                 var extendedAmount: Double?,
                 var durationMin: Int?,
                 var bolusId: Int?,
                 var isCancelled: Boolean,
                 var isRunning: Boolean): HistoryLogObject() {

    constructor(immediateAmount: Double?,
                bolusId: Int?,
                isCancelled: Boolean,
                isRunning: Boolean) : this(PumpBolusType.NORMAL, immediateAmount, null, null, bolusId, isCancelled, isRunning)

    constructor(extendedAmount: Double?,
                durationMin: Int?,
                bolusId: Int?,
                isCancelled: Boolean,
                isRunning: Boolean) : this(bolusType = PumpBolusType.EXTENDED,
                                           immediateAmount = null,
                                           extendedAmount = extendedAmount,
                                           durationMin = durationMin,
                                           bolusId = bolusId,
                                           isCancelled = isCancelled,
                                           isRunning = isRunning)

    // constructor(immediateAmount: Double?,
    //             extendedAmount: Double?,
    //             durationMin: Int?,
    //             isCalculated: Boolean,
    //             isCancelled: Boolean,
    //             isRunning: Boolean) : this(PumpBolusType.COMBINED, immediateAmount, extendedAmount, durationMin, isCalculated, isCancelled, isRunning)

    override fun getDisplayableValue(resourceHelper: ResourceHelper, dataConverter: TandemDataConverter): String {
        return when (bolusType) {
            PumpBolusType.NORMAL   -> resourceHelper.gs(bolusType.resourceId, immediateAmount)
            PumpBolusType.EXTENDED -> resourceHelper.gs(bolusType.resourceId, extendedAmount, durationMin)
            PumpBolusType.COMBINED -> resourceHelper.gs(bolusType.resourceId, immediateAmount, extendedAmount, durationMin)
            PumpBolusType.SMB      -> resourceHelper.gs(bolusType.resourceId, immediateAmount)
            PumpBolusType.PRIME    -> resourceHelper.gs(bolusType.resourceId, immediateAmount)
        }
    }
}


data class BasalProfile(var profile: HashMap<Int, BasalProfileEntry>) : HistoryLogObject() {

    override fun getDisplayableValue(resourceHelper: ResourceHelper, dataConverter: TandemDataConverter): String {

        return "";
        // return resourceHelper.gs(
        //     R.string.ypsopump_history_basal_profile,
        //     ypsoPumpDataConverter.convertBasalProfileToString(profile, ", ")
        // )
    }

}

data class BasalProfileEntry(var hour: Int,
                             var rate: Double) : HistoryLogObject() {

    override fun getDisplayableValue(resourceHelper: ResourceHelper, dataConverter: TandemDataConverter): String {
        return String.format("%02d=%.2f", hour, rate)
    }

}


data class TemporaryBasal(
    var percent: Int,
    var minutes: Int,
    var isRunning: Boolean //,
    //var temporaryBasalType: PumpSync.TemporaryBasalType = PumpSync.TemporaryBasalType.NORMAL
) : HistoryLogObject() {

    override fun getDisplayableValue(resourceHelper: ResourceHelper, dataConverter: TandemDataConverter): String {
        return resourceHelper.gs(R.string.ypsopump_history_tbr, percent, minutes)
    }
}

enum class ConfigurationType(@StringRes var stringId: Int) {
    BolusStepChanged(R.string.ypsopump_config_bolus_step_changed),
    BolusAmountCapChanged(R.string.ypsopump_config_bolus_amount_cap_changed),
    BasalAmountCapChanged(R.string.ypsopump_config_basal_amount_cap_changed),
    BasalProfileChanged(R.string.ypsopump_config_basal_profile_changed)
}

data class ConfigurationChanged(var configurationType: ConfigurationType,
                                var value: String) : HistoryLogObject() {
    override fun getDisplayableValue(resourceHelper: ResourceHelper, dataConverter: TandemDataConverter): String {
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
                             var reason: Int? = null,
                             var additonalData: String? = null): HistoryLogObject() {

    override fun getDisplayableValue(resourceHelper: ResourceHelper, dataConverter: TandemDataConverter): String {
        return if (pumpStatusType == PumpStatusType.Priming) {
            resourceHelper.gs(pumpStatusType.stringId, additonalData)
        } else {
            resourceHelper.gs(pumpStatusType.stringId)
        }
    }
}



