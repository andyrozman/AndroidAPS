package app.aaps.pump.tandem.common.database.data

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.pump.common.defs.PumpHistoryEntryGroup
import app.aaps.pump.common.driver.history.PumpHistoryPeriod
import app.aaps.pump.tandem.common.comm.qe.QualifyingEventHandler
import app.aaps.pump.tandem.common.data.defs.QualifyingEventsRange
import app.aaps.pump.tandem.common.database.TandemPumpDatabase
import app.aaps.pump.tandem.common.database.data.defs.DatabaseQueryParameters
import app.aaps.pump.tandem.common.database.data.dto.TandemHistoryRecordDto
import app.aaps.pump.tandem.common.database.data.entity.TandemHistoryRecordEntity
import app.aaps.pump.tandem.common.database.data.entity.TandemQualifyingEventEntity
import app.aaps.pump.tandem.common.database.data.entity.TandemSiteChangeEntity
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.UnknownHistoryLog
import io.reactivex.rxjava3.core.Single
import java.lang.System.currentTimeMillis
import java.util.GregorianCalendar
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DbDataHandler @Inject constructor(
    val tandemHistoryConverter: TandemHistoryConverter,
    var tandemPumpDatabase: TandemPumpDatabase,
    val rxBus: RxBus,
    val aapsLogger: AAPSLogger,
    val pumpStatus: TandemPumpStatus,
    val aapsSchedulers: AapsSchedulers,
    val preferences: Preferences,
    val qualifyingEventHandler: QualifyingEventHandler

) {

    @Suppress("PropertyName")
    val TAG = LTag.PUMPCOMM

    // fun createHistoryRecord(
    //     sequenceId: Long,
    //     pumpSerial: Int,
    //     typeId: Int,
    //     pumpTime: Long,   // EpochInMillis (pump stores time as EpochSeconds from Jan2008, we don't)
    //     payload: ByteArray,
    //     entitySubId: Int? = null, // some entities have special id (for example TBR has tempRateId)
    // ): Single<Long> {
    //
    //     // var id = sequenceId
    //
    //     return tandemPumpDatabase.historyRecordDao().save(
    //         TandemHistoryRecordEntity(
    //             sequenceId = sequenceId,
    //             pumpSerial = pumpSerial,
    //             typeId = typeId,
    //             pumpTime = pumpTime,
    //             payload = payload,
    //             entitySubId = entitySubId,
    //             createdAt = currentTimeMillis(),
    //             updatedAt = currentTimeMillis()
    //         )
    //     ).toSingle { sequenceId }
    // }


    // fun createQualifyingEventRecord(
    //     pumpSerial: Int,
    //     dateTime: Long,
    //     name: String,
    //     description: String? = null, // some entities have special id (for example TBR has tempRateId)
    // ): Single<Long> {
    //
    //     var id :Long  = 0
    //
    //     return tandemPumpDatabase.qualifyingEventsDao().save(
    //         TandemQualifyingEventEntity(
    //             pumpSerial = pumpSerial,
    //             dateTime = dateTime,
    //             name = name,
    //             description = description
    //         )
    //     ).toSingle { id }
    //
    // }



    fun addHistoryLogs(listOfHistoryEntries: MutableCollection<HistoryLog>, debugHistoryDownload: Boolean = false) {

        val logPrefix =  if (debugHistoryDownload) "HST: " else ""

        aapsLogger.debug(TAG, "${logPrefix}Received entries: ${listOfHistoryEntries.size}")

        val entities = listOfHistoryEntries.stream()
            .map { item -> tandemHistoryConverter.getTandemHistoryRecordEntity(item) }
            .collect(Collectors.toList())

        aapsLogger.debug(TAG, "${logPrefix}Mapped entities: ${entities.size}")

        tandemPumpDatabase.historyRecordDao().saveAll(entities)
            .subscribeOn(aapsSchedulers.io)
            .observeOn(aapsSchedulers.main)
            .subscribe(
                { aapsLogger.debug(TAG, "HST: Inserted TandemHistoryRecordEntity/HistoryLog: ${entities.size} ") },
                { error -> aapsLogger.error(TAG, "HST: Failed to insert TandemHistoryRecordEntity: ${error.message}", error) }
            )
    }


    fun addQualifyingEventRecords(
        listOfEvents: List<TandemQualifyingEventEntity>
    ) {

        tandemPumpDatabase.qualifyingEventsDao().saveAll(listOfEvents)
            .subscribeOn(aapsSchedulers.io)
            .observeOn(aapsSchedulers.main)
            .subscribe(
                { aapsLogger.debug(TAG, "Inserted QualifyingEvents: ${listOfEvents.size} ") },
                { error -> aapsLogger.error(TAG, "Failed to insert QualifyingEvents: ${error.message}", error) }
            )
    }


    fun addSiteChange(
        tandemSiteChangeEntity: TandemSiteChangeEntity
    ) {
        tandemPumpDatabase.siteChangeDao()
            .save(tandemSiteChangeEntity)
            .subscribeOn(aapsSchedulers.io)
            .observeOn(aapsSchedulers.main)
            .subscribe(
                { aapsLogger.debug(TAG, "Inserted SiteChange Record") },
                { error -> aapsLogger.error(TAG, "Failed to insert SiteChange: ${error.message}", error) }
            )
    }


    fun updateSiteChangeWithStoredTrue(siteChangeRecord: TandemSiteChangeEntity) {
        tandemPumpDatabase.siteChangeDao()
            .updateSiteChangeWithStoredInEventTrue(siteChangeRecord.id)
            .subscribeOn(aapsSchedulers.io)
            .observeOn(aapsSchedulers.main)
            .subscribe(
                { aapsLogger.debug(TAG, "Updated SiteChange Record [id=${siteChangeRecord.id}]") },
                { error -> aapsLogger.error(TAG, "Failed to update SiteChange[id=${siteChangeRecord.id}]: ${error.message}", error) }
            )
    }


    fun getUnassignedSiteChanges() : List<TandemSiteChangeEntity> {
        return tandemPumpDatabase.siteChangeDao()
            .allNotStoredWithSerialBlocking(pumpStatus.serialNumber)
    }

    fun getLatestHistorySequenceIds(limit: Int): Set<Long> {
        return tandemPumpDatabase.historyRecordDao()
            .getLatestSequenceIdsBlocking(pumpStatus.serialNumber.toInt(), limit)
            .toSet()
    }

    fun getMaxHistorySequenceId(): Long? {
        return tandemPumpDatabase.historyRecordDao()
            .getMaxSequenceIdBlocking(pumpStatus.serialNumber.toInt())
    }


    fun getCurrentQEItemsBlocking(): List<TandemQualifyingEventEntity> {

        val qeRange = QualifyingEventsRange.valueOf(preferences.get(TandemStringPreferenceKey.QualifyingEventsRangePref))

        aapsLogger.info(TAG, "Get Current QualifyingEvents Items with range: ${qeRange.name}")

        var itemLimit = 0

        var qeItems = if (qeRange==QualifyingEventsRange.LAST_15_ITEMS) {
            itemLimit = 15
            tandemPumpDatabase.qualifyingEventsDao().getLast30ItemsWithSerialBlocking(pumpStatus.serialNumber.toInt())
        } else {
            val gcNow = GregorianCalendar()
            when (qeRange) {
                QualifyingEventsRange.LAST_3_HOURS  -> gcNow.add(GregorianCalendar.HOUR_OF_DAY, -3)
                QualifyingEventsRange.LAST_6_HOURS  -> gcNow.add(GregorianCalendar.HOUR_OF_DAY, -6)
                QualifyingEventsRange.LAST_12_HOURS -> gcNow.add(GregorianCalendar.HOUR_OF_DAY, -12)
                QualifyingEventsRange.LAST_24_HOURS -> gcNow.add(GregorianCalendar.HOUR_OF_DAY, -24)
                QualifyingEventsRange.LAST_15_ITEMS -> { }
            }
            tandemPumpDatabase.qualifyingEventsDao().allSinceWithSerialBlocking(pumpStatus.serialNumber.toInt(), gcNow.timeInMillis)
        }

        qeItems = qualifyingEventHandler.filterQualifyingEventsAndLimit(qeItems, itemLimit)

        return qeItems
    }


    fun getHistoryRecords(queryParameters: DatabaseQueryParameters): List<TandemHistoryRecordDto> {

        var gc = GregorianCalendar()

        when(queryParameters.historyTime) {
            PumpHistoryPeriod.TODAY         -> {
                gc = GregorianCalendar(gc.get(GregorianCalendar.DAY_OF_MONTH),
                                       gc.get(GregorianCalendar.MONTH),
                                       gc.get(GregorianCalendar.YEAR),
                                       0, 0)
            }
            PumpHistoryPeriod.LAST_HOUR     -> {
                gc.add(GregorianCalendar.HOUR_OF_DAY, -1)
            }
            null,
            PumpHistoryPeriod.LAST_3_HOURS  -> {
                gc.add(GregorianCalendar.HOUR_OF_DAY, -3)
            }
            PumpHistoryPeriod.LAST_6_HOURS  -> {
                gc.add(GregorianCalendar.HOUR_OF_DAY, -6)
            }
            PumpHistoryPeriod.LAST_12_HOURS -> {
                gc.add(GregorianCalendar.HOUR_OF_DAY, -12)
            }
            PumpHistoryPeriod.LAST_24_HOURS -> {
                gc.add(GregorianCalendar.DAY_OF_YEAR, -1)
            }
            PumpHistoryPeriod.LAST_2_DAYS   -> {
                gc.add(GregorianCalendar.DAY_OF_YEAR, -2)
            }
            PumpHistoryPeriod.LAST_4_DAYS   -> {
                gc.add(GregorianCalendar.DAY_OF_YEAR, -4)
            }
            PumpHistoryPeriod.LAST_WEEK     -> {
                gc.add(GregorianCalendar.WEEK_OF_YEAR, -1)
            }
            PumpHistoryPeriod.LAST_MONTH    -> {
                gc.add(GregorianCalendar.MONTH, -1)
            }
            PumpHistoryPeriod.ALL           -> {
                gc.add(GregorianCalendar.DAY_OF_YEAR, -144)
            }
        }

        aapsLogger.error(TAG, "HST: Get History (serial=${pumpStatus.serialNumber.toInt()}, since=${gc.timeInMillis}")

        val entities = tandemPumpDatabase.historyRecordDao()
            .allSinceWithSerialBlocking(pumpStatus.serialNumber.toInt(), gc.timeInMillis)

        aapsLogger.error(TAG, "HST: Get Entities: ${entities.size}")

        val listOut = mutableListOf<TandemHistoryRecordDto>()

        val targetGroup = queryParameters.groupType!!

        tandemHistoryConverter.prepareForConversion()

        for (entity in entities) {
            val historyRecordDto = tandemHistoryConverter.getHistoryRecordDto(entity)

            if (isIncludedGroup(historyRecordDto, targetGroup)) {
                listOut.add(historyRecordDto)
            }
        }

        aapsLogger.error(TAG, "HST: List Out: ${listOut.size}")

        return listOut.toList()
    }

    private fun isIncludedGroup(historyRecordDto: TandemHistoryRecordDto,
                                targetGroup: PumpHistoryEntryGroup): Boolean {
        if (targetGroup==PumpHistoryEntryGroup.All)
            return true
        else if (targetGroup== PumpHistoryEntryGroup.AllNoUnknowns) {
            return historyRecordDto.historyLog !is UnknownHistoryLog
        } else {
            return historyRecordDto.group == targetGroup
        }
    }

    fun databaseStatistics() {

        aapsLogger.error(LTag.PUMPCOMM, "\n" +
            "Database Statistics - Tandem Database\n" +
            "======================================\n")

        tandemPumpDatabase.qualifyingEventsDao().getEventsCount()
            .subscribeOn(aapsSchedulers.io)
            .observeOn(aapsSchedulers.main)
            .subscribe { count: Long ->
                aapsLogger.error(LTag.PUMPCOMM, " Qualifying Events - $count") }

        tandemPumpDatabase.historyRecordDao().getHistoryCount()
        .subscribeOn(aapsSchedulers.io)
        .observeOn(aapsSchedulers.main)
        .subscribe { count: Long ->
            aapsLogger.error(LTag.PUMPCOMM, " History Records - $count") }

    }


}
