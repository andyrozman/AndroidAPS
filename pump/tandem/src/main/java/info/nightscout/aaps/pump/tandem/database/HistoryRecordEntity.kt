package info.nightscout.aaps.pump.tandem.database

import androidx.room.Embedded
import androidx.room.Entity
import info.nightscout.aaps.pump.tandem.data.history.DateTimeChanged
import info.nightscout.aaps.pump.tandem.data.defs.TandemPumpHistoryType


// TODO refactor this for Tandem

@Entity(tableName = "history_records", primaryKeys = ["id", "serial"])
data class HistoryRecordEntity(
    var id: Long,
    var serial: Long,
    var historyTypeIndex: Int,
    var historyType: TandemPumpHistoryType,
    var sequenceNum: Long,
    var dateTimeMillis: Long,
    var payload: String,
    @Embedded(prefix = "datetime_") var dateTimeRecord: DateTimeChanged?,

    // TODO add more history types
    // @Embedded(prefix = "temporaryBasal_") var temporaryBasalRecord: TemporaryBasal?,
    // @Embedded(prefix = "bolus_") var bolusRecord: Bolus?,
    // @Embedded(prefix = "tdd_") var tddRecord: TotalDailyInsulin?,
    // @Embedded(prefix = "basal_profile_") var basalProfileRecord: BasalProfile?,
    // @Embedded(prefix = "alarm_") var alarmRecord: Alarm?,
    // @Embedded(prefix = "config_") var configRecord: ConfigurationChanged?,
    // @Embedded(prefix = "pumpstatus_") var pumpStatusRecord: PumpStatusChanged?,

    var createdAt: Long, // creation date of the record
    var updatedAt: Long  // update date of the record
)
