package info.nightscout.aaps.pump.tandem.database

import androidx.room.TypeConverter
import info.nightscout.aaps.pump.tandem.data.defs.TandemPumpHistoryType

class DatabaseConverters {

    @TypeConverter
    fun toTandemPumpHistoryType(s: String) = enumValueOf<TandemPumpHistoryType>(s)

    @TypeConverter
    fun fromTandemPumpHistoryType(tandemPumpHistoryType: TandemPumpHistoryType) = tandemPumpHistoryType.name

    // OLD ONES MIGHT NEED TO BE REMOVED

    // @TypeConverter
    // fun toBolusType(s: String) = enumValueOf<BolusType>(s)
    //
    // @TypeConverter
    // fun fromBolusType(bolusType: BolusType) = bolusType.name
    //
    // @TypeConverter
    // fun toAlarmType(s: String) = enumValueOf<AlarmType>(s)
    //
    // @TypeConverter
    // fun fromBolusType(alarmType: AlarmType) = alarmType.name
    //
    // @TypeConverter
    // fun toConfigurationType(s: String) = enumValueOf<ConfigurationType>(s)
    //
    // @TypeConverter
    // fun fromConfigurationType(configurationType: ConfigurationType) = configurationType.name
    //
    // @TypeConverter
    // fun toPumpStatusType(s: String) = enumValueOf<PumpStatusType>(s)
    //
    // @TypeConverter
    // fun fromPumpStatusType(pumpStatusType: PumpStatusType) = pumpStatusType.name
    //
    // @TypeConverter
    // fun toHistoryEntryType(s: String) = enumValueOf<HistoryEntryType>(s)
    //
    // @TypeConverter
    // fun fromHistoryEntryType(historyEntryType: HistoryEntryType) = historyEntryType.name






    // @TypeConverter
    // fun toBasalProfile(s: String): BasalProfile {
    //     val patterns = s.split(";")
    //
    //     val profileMap: HashMap<Int, BasalProfileEntry> = hashMapOf()
    //
    //     for(value in patterns) {
    //         val entry = value.split("=")
    //         val hour = entry[0].toInt()
    //         val rate = entry[1].toDouble()
    //         val basalProfileEntry = BasalProfileEntry(hour, rate)
    //
    //         profileMap.put(hour, basalProfileEntry)
    //     }
    //
    //     return BasalProfile(profileMap)
    // }

    // @TypeConverter
    // fun fromBasalProfile(basalProfile: BasalProfile): String {
    //     var stringBuilder: StringBuilder = java.lang.StringBuilder()
    //
    //     for (pattern in basalProfile.profile) {
    //         stringBuilder.append(pattern.key)
    //         stringBuilder.append("=")
    //         stringBuilder.append(pattern.value.toString())
    //         stringBuilder.append(";")
    //     }
    //
    //     return stringBuilder.substring(0, stringBuilder.length-1);
    // }


    // @TypeConverter
    // fun toBasalProfile(s: String): HashMap<Int, BasalProfileEntry> {
    //     val patterns = s.split(";")
    //
    //     val profileMap: HashMap<Int, BasalProfileEntry> = hashMapOf()
    //
    //     for(value in patterns) {
    //         val entry = value.split("=")
    //         val hour = entry[0].toInt()
    //         val rate = entry[1].toDouble()
    //         val basalProfileEntry = BasalProfileEntry(hour, rate)
    //
    //         profileMap.put(hour, basalProfileEntry)
    //     }
    //
    //     return profileMap
    // }
    //
    // @TypeConverter
    // fun fromBasalProfile(map: HashMap<Int, BasalProfileEntry>) : String {
    //     val stringBuilder: StringBuilder = java.lang.StringBuilder()
    //
    //     for (pattern in map.entries) {
    //         stringBuilder.append(pattern.key)
    //         stringBuilder.append("=")
    //         stringBuilder.append(pattern.value.toString())
    //         stringBuilder.append(";")
    //     }
    //
    //     return stringBuilder.substring(0, stringBuilder.length-1);
    //
    // }


}