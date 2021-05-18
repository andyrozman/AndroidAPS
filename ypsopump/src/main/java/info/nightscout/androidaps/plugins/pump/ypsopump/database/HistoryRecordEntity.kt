package info.nightscout.androidaps.plugins.pump.ypsopump.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.*
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType

@Entity(tableName = "history_records")
data class HistoryRecordEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    val date: Long, // when event actually happened (yyyymmddHHmmss)
    val historyRecordType: HistoryEntryType, // history entry type: event, alarm, systemEntry
    val entryType: YpsoPumpEventType, // pump event type
    var entryTypeAsInt: Int, // backup of eventId in case pumpEventType not recognized
    var value1: Int, // backup value1
    var value2: Int, // backup value2
    var value3: Int, // backup value3
    var eventSequenceNumber: Int, // sequence Number, this is unique pumpId
    @Embedded(prefix = "temporaryBasal_") var temporaryBasalRecord: TemporaryBasal?,
    @Embedded(prefix = "bolus_") var bolusRecord: Bolus?,
    @Embedded(prefix = "tdi_") var tdiRecord: TotalDailyInsulin?,
    @Embedded(prefix = "basal_profile_") var basalProfileRecord: BasalProfile?,
    @Embedded(prefix = "alarm_") var alarmRecord: Alarm?,
    @Embedded(prefix = "config_") var configRecord: ConfigurationChanged?,
    @Embedded(prefix = "pumpstatus_") var pumpStatusRecord: PumpStatusChanged?,
    @Embedded(prefix = "datetime_") var dateTimeRecord: DateTimeChanged?,
    var createdAt: Long, // creation date of the record
    var updatedAt: Long  // update date of the record
)
