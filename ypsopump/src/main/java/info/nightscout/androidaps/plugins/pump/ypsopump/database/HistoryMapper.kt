package info.nightscout.androidaps.plugins.pump.ypsopump.database

import info.nightscout.androidaps.plugins.pump.ypsopump.data.*
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import javax.inject.Inject

class HistoryMapper @Inject constructor(var ypsoPumpUtil: YpsoPumpUtil) {

    fun domainToEntity(eventDto: EventDto): HistoryRecordEntity {
        var historyRecordEntity = HistoryRecordEntity(
            id = eventDto.id!!,
            serial = eventDto.serial,
            historyRecordType = eventDto.historyEntryType,
            date = eventDto.dateTime.toATechDate(),
            entryType = eventDto.entryType,
            entryTypeAsInt = eventDto.entryTypeAsInt,
            value1 = eventDto.value1,
            value2 = eventDto.value2,
            value3 = eventDto.value3,
            eventSequenceNumber = eventDto.eventSequenceNumber,
            temporaryBasalRecord = null,
            bolusRecord = null,
            tddRecord = null,
            basalProfileRecord = null,
            alarmRecord = null,
            configRecord = null,
            pumpStatusRecord = null,
            dateTimeRecord = null,
            if (eventDto.created == null) 0L else eventDto.created!!,
            if (eventDto.updated == null) 0L else eventDto.updated!!
        )

        if (eventDto.subObject is Bolus) {
            historyRecordEntity.bolusRecord = eventDto.subObject as Bolus
        } else if (eventDto.subObject is TemporaryBasal) {
            historyRecordEntity.temporaryBasalRecord = eventDto.subObject as TemporaryBasal
        } else if (eventDto.subObject is TotalDailyInsulin) {
            historyRecordEntity.tddRecord = eventDto.subObject as TotalDailyInsulin
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

        if (eventDto.subObject2 != null) {
            historyRecordEntity.temporaryBasalRecord = eventDto.subObject2 as TemporaryBasal
        }

        return historyRecordEntity;
    }

    fun entityToDomain(entity: HistoryRecordEntity): EventDto {

        val eventDto = EventDto(
            id = entity.id,
            serial = entity.serial,
            historyEntryType = entity.historyRecordType,
            dateTime = ypsoPumpUtil.toDateTimeDto(entity.date),
            entryType = entity.entryType,
            entryTypeAsInt = entity.entryTypeAsInt,
            value1 = entity.value1,
            value2 = entity.value2,
            value3 = entity.value3,
            eventSequenceNumber = entity.eventSequenceNumber,
            subObject = null,
            subObject2 = null,
            created = entity.createdAt,
            updated = entity.updatedAt
        )

        // TODO include subObject2 - done, not tested

        if (entity.bolusRecord != null) {
            eventDto.subObject = entity.bolusRecord
        } else if (entity.temporaryBasalRecord != null) {
            if (entity.entryType == YpsoPumpEventType.DELIVERY_STATUS_CHANGED) {
                eventDto.subObject2 = entity.temporaryBasalRecord
            } else {
                eventDto.subObject = entity.temporaryBasalRecord
            }
        } else if (entity.tddRecord != null) {
            eventDto.subObject = entity.tddRecord
        } else if (entity.basalProfileRecord != null) {
            eventDto.subObject = entity.basalProfileRecord
        } else if (entity.alarmRecord != null) {
            eventDto.subObject = entity.alarmRecord
        } else if (entity.configRecord != null) {
            eventDto.subObject = entity.configRecord
        } else if (entity.pumpStatusRecord != null) {
            eventDto.subObject = entity.pumpStatusRecord
        } else if (entity.dateTimeRecord != null) {
            eventDto.subObject = entity.dateTimeRecord
        }

        return eventDto;
    }

}