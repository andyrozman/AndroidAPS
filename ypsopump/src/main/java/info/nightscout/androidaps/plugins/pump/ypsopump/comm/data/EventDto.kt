package info.nightscout.androidaps.plugins.pump.ypsopump.comm.data

import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType

data class EventDto(var dateTimeLocal: DateTimeDto,
                    var entryType: YpsoPumpEventType,
                    var entryTypeAsInt: Int,
                    var entryValue1: Int,
                    var entryValue2: Int,
                    var entryValue3: Int,
                    var eventSequenceNumber: Int,
                    var eventIndex: Int,
                    var subObject: Any? = null) {
    //var eventType: YpsoPumpEventType? = null


    override fun toString(): String {

        var entryTypeFormated = entryType.name

        if (entryTypeFormated.length > 24) {
            entryTypeFormated = entryTypeFormated.substring(0, 24)
        } else if (entryTypeFormated.length < 24) {
            entryTypeFormated = entryTypeFormated.padEnd(24, ' ')
        }

        var sequenceString = "" + eventSequenceNumber
        sequenceString = sequenceString.padStart(8, ' ')


        val dataLine = dateTimeLocal.toString() + "   " + entryTypeFormated + "  " + sequenceString

        if (subObject == null) {
            return dataLine + "      value1=" + entryValue1 + ", value2=" + entryValue2 + ", value3=" + entryValue3
        } else {
            return dataLine + "      " + subObject.toString()
        }
    }
}


data class BasalProfile(var profile: HashMap<Int, BasalProfileEntry>)


data class BasalProfileEntry(var hour: Int,
                             var rate: Double)


data class TotalDailyInsulin(var bolus: Double,
                             var basal: Double,
                             var total: Double,
                             var isTotalOnly: Boolean) {
    constructor(total: Double) : this(0.0, 0.0, total, true)

    constructor(bolus: Double, basal: Double) : this(bolus, basal, basal + bolus, false)

}

enum class BolusType {
    Normal,
    Extended,
    Combined
}


data class Bolus(var bolusType: BolusType,
                 var immediateAmount: Double?,
                 var extendedAmount: Double?,
                 var durationMin: Int?,
                 var isCalculated: Boolean,
                 var isCancelled: Boolean,
                 var isRunning: Boolean) {

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

}

data class BasalProfileTemp(var percent: Int,
                            var minutes: Int,
                            var isRunning: Boolean)

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
                 var value3: Int? = null)


enum class ConfigurationType {
    BolusStepChanged,
    BolusAmountCapChanged,
    BasalAmountCapChanged,
    BasalProfileChanged
}


data class ConfigurationChanged(var configurationType: ConfigurationType,
                                var value: String)


enum class PumpStatusType {
    PumpRunning,
    PumpSuspended,
    Priming,
    Rewind,
    BatteryRemoved
}


data class PumpStatusChanged(var pumpStatusType: PumpStatusType,
                             var additonalData: String? = null)