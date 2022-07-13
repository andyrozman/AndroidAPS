package info.nightscout.androidaps.plugins.pump.tandem.database

import androidx.room.Embedded
import androidx.room.Entity
import info.nightscout.androidaps.plugins.pump.tandem.data.*
import info.nightscout.androidaps.plugins.pump.tandem.defs.YpsoPumpEventType

// TODO refactor this for Tandem

@Entity(tableName = "history_records", primaryKeys = ["id", "serial", "historyRecordType"])
data class HistoryRecordEntity(
    var id: Int,
    var serial: Long,
    val historyRecordType: HistoryEntryType, // history entry type: event, alarm, systemEntry
    var date: Long, // when event actually happened (yyyymmddHHmmss)
    var entryType: YpsoPumpEventType, //YpsoPumpEventType, // pump event type
    var entryTypeAsInt: Int, // backup of eventId in case pumpEventType not recognized
    var value1: Int, // backup value1
    var value2: Int, // backup value2
    var value3: Int, // backup value3
    var eventSequenceNumber: Int, // sequence Number, this is unique pumpId
    @Embedded(prefix = "temporaryBasal_") var temporaryBasalRecord: TemporaryBasal?,
    @Embedded(prefix = "bolus_") var bolusRecord: Bolus?,
    @Embedded(prefix = "tdd_") var tddRecord: TotalDailyInsulin?,
    @Embedded(prefix = "basal_profile_") var basalProfileRecord: BasalProfile?,
    @Embedded(prefix = "alarm_") var alarmRecord: Alarm?,
    @Embedded(prefix = "config_") var configRecord: ConfigurationChanged?,
    @Embedded(prefix = "pumpstatus_") var pumpStatusRecord: PumpStatusChanged?,
    @Embedded(prefix = "datetime_") var dateTimeRecord: DateTimeChanged?,
    var createdAt: Long, // creation date of the record
    var updatedAt: Long  // update date of the record
)
