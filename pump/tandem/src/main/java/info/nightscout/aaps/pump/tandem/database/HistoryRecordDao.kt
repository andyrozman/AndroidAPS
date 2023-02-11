package info.nightscout.aaps.pump.tandem.database

import androidx.room.*
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

// TODO refactor this for Tandem

@Dao
abstract class HistoryRecordDao {

    @Query("SELECT * from history_records")
    abstract fun all(): Single<List<HistoryRecordEntity>>

    @Query("SELECT * from history_records order by sequenceNum desc")
    abstract fun allBlocking(): List<HistoryRecordEntity>

    @Query("SELECT * from history_records WHERE dateTimeMillis >= :since")
    abstract fun allSince(since: Long): Single<List<HistoryRecordEntity>>

    @Query("SELECT * from history_records WHERE dateTimeMillis >= :since order by sequenceNum desc")
    abstract fun allSinceBlocking(since: Long): List<HistoryRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveBlocking(historyRecordEntity: HistoryRecordEntity)

    // @Query("SELECT * from history_records where id = :id and serial= :serialNumber and entryType= :entryType")
    // abstract fun getById(id: Int, serialNumber: Long, entryType: HistoryEntryType): HistoryRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(historyRecordEntity: HistoryRecordEntity): Completable

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun update(historyRecordEntity: HistoryRecordEntity): Completable

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateBlocking(historyRecordEntity: HistoryRecordEntity)

    @Delete
    abstract fun delete(historyRecordEntity: HistoryRecordEntity): Completable

    // @Query(
    //     "SELECT * from history_records where serial = :serialNumber and historyRecordType= :entryType " +
    //         " and id= (select max(id) from history_records where serial = :serialNumber " +
    //         " and historyRecordType= :entryType) "
    // )
    // abstract fun getLatestHistoryEntry(serialNumber: Long, entryType: HistoryEntryType): HistoryRecordEntity?

    // @Query(
    //     "SELECT * from history_records where serial = :serialNumber and historyRecordType='Event' " +
    //     " and id= ( select max(id) from history_records where serial = :serialNumber " +
    //     "           and historyRecordType='Event' " +
    //     "           and (entryType='PUMP_MODE_CHANGED' or entryType='DELIVERY_STATUS_CHANGED')) ")
    // abstract fun getLatestDeliveryStatusChanged(serialNumber: Long): HistoryRecordEntity?

    //PUMP_MODE_CHANGED, DELIVERY_STATUS_CHANGED

    // select * from test_table where type = 'EVENT' and number_count =
    // (select max(test_table.number_count) from test_table where type = 'EVENT')

    // @Query("UPDATE historyrecords SET resolvedResult = :resolvedResult, resolvedAt = :resolvedAt WHERE id = :id ")
    // abstract fun markResolved(id: String, resolvedResult: ResolvedResult, resolvedAt: Long): Completable

}