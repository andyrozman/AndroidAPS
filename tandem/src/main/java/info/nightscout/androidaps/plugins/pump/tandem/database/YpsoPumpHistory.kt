package info.nightscout.androidaps.plugins.pump.tandem.database

import com.google.gson.Gson
import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.plugins.pump.tandem.data.*
import info.nightscout.androidaps.plugins.pump.tandem.defs.YpsoPumpEventType
import info.nightscout.androidaps.plugins.pump.tandem.driver.TandemPumpStatus
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import io.reactivex.rxjava3.core.Single
import java.lang.System.currentTimeMillis
import java.util.*
import javax.inject.Inject

// TODO refactor this for Tandem
class YpsoPumpHistory @Inject constructor(
    val pumpHistoryDao: HistoryRecordDao,
    val pumpHistoryDatabase: YpsoPumpDatabase,
    val historyMapper: HistoryMapper,
    val pumpSync: PumpSync,
    val pumpUtil: TandemPumpUtil,
    val pumpStatus: TandemPumpStatus,
    val aapsLogger: AAPSLogger
) {

    var gson: Gson = pumpUtil.gson
    var prefix: String = "DB: "

    //fun markSuccess(id: String, date: Long): Completable = dao.markResolved(id, ResolvedResult.SUCCESS, currentTimeMillis())

    //fun markFailure(id: String, date: Long): Completable = dao.markResolved(id, ResolvedResult.FAILURE, currentTimeMillis())

    fun createRecord(
        serial: Long,
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
    ): Single<Int> {
        var id = eventSequenceNumber

        // TODO Db
        // when {
        //     commandType == SET_BOLUS && bolusRecord == null               ->
        //         Single.error(IllegalArgumentException("bolusRecord missing on SET_BOLUS"))
        //     commandType == SET_TEMPORARY_BASAL && tempBasalRecord == null ->
        //         Single.error<String>(IllegalArgumentException("tempBasalRecord missing on SET_TEMPORARY_BASAL"))
        //     else                                                          -> null
        // }?.let { return it }

        return pumpHistoryDao.save(
            HistoryRecordEntity(
                id = eventSequenceNumber,
                serial = serial,
                historyRecordType = historyRecordType,
                date = date,
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
                tddRecord = tdiRecord,
                basalProfileRecord = basalProfileRecord,
                alarmRecord = alarmRecord,
                configRecord = configRecord,
                pumpStatusRecord = pumpStatusRecord,
                dateTimeRecord = dateTimeRecord
            )
        ).toSingle { id }
    }

    // fun addRecord(eventDto: EventDto) : Single<Long> {
    //     val entity = historyMapper.domainToEntity(eventDto)
    //     entity.id = entity.eventSequenceNumber
    //     entity.createdAt = currentTimeMillis()
    //     entity.updatedAt = currentTimeMillis()
    //
    //
    //
    //     return pumpHistoryDao.save(entity).toSingle { entity.id }
    // }

    fun getRecords(): Single<List<EventDto>> =
        pumpHistoryDao.all().map { list -> list.map(historyMapper::entityToDomain) }

    fun getRecordsAfter(time: Long): Single<List<HistoryRecordEntity>> = pumpHistoryDao.allSince(time)

    // fun processList(entityList: List<HistoryRecordEntity>) {
    //     var first = true
    //     for (historyRecordEntity in entityList) {
    //         insertOrUpdate(historyRecordEntity)
    //     }
    // }

    fun getHistoryRecords(): List<HistoryRecordEntity> {
        val history = pumpHistoryDao.all().blockingGet()
        aapsLogger.info(LTag.PUMP, "History entries: ${history.size}")
        return history
    }

    fun getHistoryRecordsAfter(atdTime: Long): List<HistoryRecordEntity> {
        return pumpHistoryDao.allSince(atdTime).blockingGet()
    }

    fun insertOrUpdate(event: EventDto): HistoryRecordEntity? {
        // aapsLogger.debug(LTag.PUMP, prefix + "EventDto to convert = ${gson.toJson(event)}")
        // val entity = historyMapper.domainToEntity(event)
        // var returnEntity: HistoryRecordEntity? = null
        // pumpHistoryDatabase.runInTransaction {
        //     val dbEntity = pumpHistoryDao.getById(entity.id, entity.serial, entity.historyRecordType)
        //
        //     aapsLogger.debug(LTag.PUMP, prefix + "pumpHistoryDao.getById[${entity.id}] = ${gson.toJson(dbEntity)}")
        //
        //     if (dbEntity == null) {
        //         entity.id = entity.eventSequenceNumber
        //         entity.createdAt = System.currentTimeMillis()
        //         entity.updatedAt = entity.createdAt
        //
        //         aapsLogger.debug(LTag.PUMP, prefix + "pumpHistoryDao.saveBlocking()")
        //
        //         pumpHistoryDao.saveBlocking(entity)
        //         returnEntity = entity
        //     } else {
        //         if (isDifferentData(dbEntity, entity)) {
        //             val entityForUpdate = prepareData(dbEntity, entity)
        //             aapsLogger.debug(LTag.PUMP, prefix + "pumpHistoryDao.updateBlocking()")
        //             pumpHistoryDao.updateBlocking(entityForUpdate)
        //             returnEntity = entityForUpdate
        //         } else {
        //             aapsLogger.debug(LTag.PUMP, prefix + "same data no Db action.")
        //         }
        //     }
        // }
        //
        // return returnEntity
        //
        // // if (returnEntity != null) {
        // //     //sendDataToPumpSync(returnEntity!!)
        // // }
    return null
    }

    private fun prepareData(dbEntity: HistoryRecordEntity, newEntity: HistoryRecordEntity): HistoryRecordEntity {
        if (dbEntity.entryType != newEntity.entryType)
            dbEntity.entryType = newEntity.entryType

        if (dbEntity.entryTypeAsInt != newEntity.entryTypeAsInt)
            dbEntity.entryTypeAsInt = newEntity.entryTypeAsInt

        if (dbEntity.value1 != newEntity.value1)
            dbEntity.value1 != newEntity.value1

        if (dbEntity.value2 != newEntity.value2)
            dbEntity.value2 != newEntity.value2

        if (dbEntity.value3 != newEntity.value3)
            dbEntity.value3 != newEntity.value3

        if (dbEntity.date != newEntity.date)
            dbEntity.date != newEntity.date

        if (!Objects.equals(dbEntity.bolusRecord, newEntity.bolusRecord))
            dbEntity.bolusRecord = newEntity.bolusRecord

        if (!Objects.equals(dbEntity.temporaryBasalRecord, newEntity.temporaryBasalRecord))
            dbEntity.temporaryBasalRecord = newEntity.temporaryBasalRecord

        if (!Objects.equals(dbEntity.tddRecord, newEntity.tddRecord))
            dbEntity.tddRecord = newEntity.tddRecord

        if (!Objects.equals(dbEntity.basalProfileRecord, newEntity.basalProfileRecord))
            dbEntity.basalProfileRecord = newEntity.basalProfileRecord

        if (!Objects.equals(dbEntity.alarmRecord, newEntity.alarmRecord))
            dbEntity.alarmRecord = newEntity.alarmRecord

        if (!Objects.equals(dbEntity.configRecord, newEntity.configRecord))
            dbEntity.configRecord = newEntity.configRecord

        if (!Objects.equals(dbEntity.pumpStatusRecord, newEntity.pumpStatusRecord))
            dbEntity.pumpStatusRecord = newEntity.pumpStatusRecord

        if (!Objects.equals(dbEntity.dateTimeRecord, newEntity.dateTimeRecord))
            dbEntity.dateTimeRecord = newEntity.dateTimeRecord

        dbEntity.updatedAt = System.currentTimeMillis()

        return dbEntity
    }

    private fun isDifferentData(entity1: HistoryRecordEntity, entity2: HistoryRecordEntity): Boolean {
        if (entity1.entryType != entity2.entryType ||
            entity1.entryTypeAsInt != entity2.entryTypeAsInt ||
            entity1.value1 != entity2.value1 ||
            entity1.value2 != entity2.value2 ||
            entity1.value3 != entity2.value3 ||
            entity1.date != entity2.date)
            return true

        if (!Objects.equals(entity1.bolusRecord, entity2.bolusRecord) ||
            !Objects.equals(entity1.temporaryBasalRecord, entity2.temporaryBasalRecord) ||
            !Objects.equals(entity1.tddRecord, entity2.tddRecord) ||
            !Objects.equals(entity1.basalProfileRecord, entity2.basalProfileRecord) ||
            !Objects.equals(entity1.alarmRecord, entity2.alarmRecord) ||
            !Objects.equals(entity1.configRecord, entity2.configRecord) ||
            !Objects.equals(entity1.pumpStatusRecord, entity2.pumpStatusRecord) ||
            !Objects.equals(entity1.dateTimeRecord, entity2.dateTimeRecord))
            return true

        return false

    }

    fun getLatestHistoryEntry(serialNumber: Long, entryType: HistoryEntryType): HistoryRecordEntity? {
        return pumpHistoryDao.getLatestHistoryEntry(serialNumber, entryType)
    }

    fun getLatestDeliveryStatusChangedEntry(): HistoryRecordEntity? {
        return pumpHistoryDao.getLatestDeliveryStatusChanged(pumpStatus.serialNumber!!)
    }

}