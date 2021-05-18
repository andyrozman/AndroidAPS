package info.nightscout.androidaps.plugins.pump.ypsopump.database

import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.*
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType
import io.reactivex.Single
import java.lang.System.currentTimeMillis
import javax.inject.Inject

class YpsoPumpHistory @Inject constructor(
    private val dao: HistoryRecordDao,
    private val historyMapper: HistoryMapper
) {
    //fun markSuccess(id: String, date: Long): Completable = dao.markResolved(id, ResolvedResult.SUCCESS, currentTimeMillis())

    //fun markFailure(id: String, date: Long): Completable = dao.markResolved(id, ResolvedResult.FAILURE, currentTimeMillis())

    fun createRecord(
        date: Long,
        historyRecordType: HistoryEntryType,
        entryType: YpsoPumpEventType,
        entryTypeAsInt: Int,
        value1: Int,
        value2: Int,
        value3: Int,
        eventSequenceNumber: Int,
        tempBasalRecord: TemporaryBasal?,
        bolusRecord: Bolus?,
        tdiRecord: TotalDailyInsulin?,
        basalProfileRecord: BasalProfile?,
        alarmRecord: Alarm?,
        configRecord: ConfigurationChanged?,
        pumpStatusRecord: PumpStatusChanged?,
        dateTimeRecord: DateTimeChanged?
    ): Single<Long> {
        val id = 0L

        // TODO Db
        // when {
        //     commandType == SET_BOLUS && bolusRecord == null               ->
        //         Single.error(IllegalArgumentException("bolusRecord missing on SET_BOLUS"))
        //     commandType == SET_TEMPORARY_BASAL && tempBasalRecord == null ->
        //         Single.error<String>(IllegalArgumentException("tempBasalRecord missing on SET_TEMPORARY_BASAL"))
        //     else                                                          -> null
        // }?.let { return it }


        return dao.save(
            HistoryRecordEntity(
                id = id,
                date = date,
                historyRecordType = historyRecordType,
                entryType = entryType,
                entryTypeAsInt = entryTypeAsInt,
                value1 = value1,
                value2 = value2,
                value3 = value3,
                eventSequenceNumber = eventSequenceNumber,
                createdAt = currentTimeMillis(),
                updatedAt = currentTimeMillis(),
                temporaryBasalRecord = tempBasalRecord,
                bolusRecord = bolusRecord,
                tdiRecord = tdiRecord,
                basalProfileRecord = basalProfileRecord,
                alarmRecord = alarmRecord,
                configRecord = configRecord,
                pumpStatusRecord = pumpStatusRecord,
                dateTimeRecord = dateTimeRecord
            )
        ).toSingle { id }
    }

    fun addRecord(eventDto: EventDto) : Single<Long> {
        val entity = historyMapper.domainToEntity(eventDto)
        entity.id = 0L
        entity.createdAt = currentTimeMillis()
        entity.updatedAt = currentTimeMillis()

        return dao.save(entity).toSingle { entity.id }
    }

    fun getRecords(): Single<List<EventDto>> =
        dao.all().map { list -> list.map(historyMapper::entityToDomain) }

    fun getRecordsAfter(time: Long): Single<List<HistoryRecordEntity>> = dao.allSince(time)

}