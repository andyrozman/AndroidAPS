package info.nightscout.androidaps.plugins.pump.ypsopump.database

import androidx.room.Embedded
import androidx.room.PrimaryKey
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.*
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import javax.inject.Inject

class HistoryMapper @Inject constructor(var ypsoPumpUtil: YpsoPumpUtil) {

    fun domainToEntity(eventDto: EventDto): HistoryRecordEntity {
        var historyRecordEntity = HistoryRecordEntity(eventDto.id!!,
            eventDto.serial,
            eventDto.historyEntryType,
            eventDto.dateTime.toATechDate(),
            eventDto.entryType,
            eventDto.entryTypeAsInt,
            eventDto.value1,
            eventDto.value2,
            eventDto.value3,
            eventDto.eventSequenceNumber,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            if (eventDto.created==null) 0L else eventDto.created!!,
            if (eventDto.updated==null) 0L else eventDto.updated!!
        )

        if (eventDto.subObject is Bolus) {
            historyRecordEntity.bolusRecord = eventDto.subObject as Bolus
        } else if (eventDto.subObject is TemporaryBasal) {
            historyRecordEntity.temporaryBasalRecord = eventDto.subObject as TemporaryBasal
        } else if (eventDto.subObject is TotalDailyInsulin) {
            historyRecordEntity.tdiRecord = eventDto.subObject as TotalDailyInsulin
        } else if (eventDto.subObject is BasalProfile) {
            historyRecordEntity.basalProfileRecord = eventDto.subObject as BasalProfile
        } else if (eventDto.subObject is Alarm) {
            historyRecordEntity.alarmRecord = eventDto.subObject as Alarm
        } else if (eventDto.subObject is ConfigurationChanged) {
            historyRecordEntity.configRecord = eventDto.subObject as ConfigurationChanged
        } else if (eventDto.subObject is PumpStatusChanged) {
            historyRecordEntity.pumpStatusRecord = eventDto.subObject as PumpStatusChanged
        } else if (eventDto.subObject is DateTimeChanged) {
            historyRecordEntity.dateTimeRecord = eventDto.subObject as DateTimeChanged
        }

        return historyRecordEntity;

    }

    fun entityToDomain(entity: HistoryRecordEntity): EventDto {

        val eventDto = EventDto(
            entity.id,
            entity.serial,
            entity.historyRecordType,
            ypsoPumpUtil.toDateTimeDto(entity.date),
            entity.entryType,
            entity.entryTypeAsInt,
            entity.value1,
            entity.value2,
            entity.value3,
            entity.eventSequenceNumber,
            null,
            entity.createdAt,
            entity.updatedAt
        )

        if (entity.bolusRecord!=null) {
            eventDto.subObject = entity.bolusRecord
        } else if (entity.temporaryBasalRecord!=null) {
            eventDto.subObject = entity.temporaryBasalRecord
        } else if (entity.tdiRecord!=null) {
            eventDto.subObject = entity.tdiRecord
        } else if (entity.basalProfileRecord!=null) {
            eventDto.subObject = entity.basalProfileRecord
        } else if (entity.alarmRecord!=null) {
            eventDto.subObject = entity.alarmRecord
        } else if (entity.configRecord!=null) {
            eventDto.subObject = entity.configRecord
        } else if (entity.pumpStatusRecord!=null) {
            eventDto.subObject = entity.pumpStatusRecord
        } else if (entity.dateTimeRecord!=null) {
            eventDto.subObject = entity.dateTimeRecord
        }

        return eventDto;

    }

}