package info.nightscout.androidaps.plugins.pump.ypsopump.database

import androidx.room.*
import info.nightscout.androidaps.plugins.pump.ypsopump.data.HistoryEntryType
import io.reactivex.Completable
import io.reactivex.Single

@Dao
abstract class HistoryRecordDao {

    @Query("SELECT * from history_records")
    abstract fun all(): Single<List<HistoryRecordEntity>>

    @Query("SELECT * from history_records")
    abstract fun allBlocking(): List<HistoryRecordEntity>

    @Query("SELECT * from history_records WHERE date >= :since")
    abstract fun allSince(since: Long): Single<List<HistoryRecordEntity>>

    // @Query("SELECT * from history_records WHERE date >= :since")
    // abstract fun allSinceBlocking(since: Long): List<HistoryRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveBlocking(historyRecordEntity: HistoryRecordEntity)

    @Query("SELECT * from history_records where id = :id and serial= :serialNumber and entryType= :entryType")
    abstract fun getById(id: Int, serialNumber: Long, entryType: HistoryEntryType): HistoryRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(historyRecordEntity: HistoryRecordEntity): Completable

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun update(historyRecordEntity: HistoryRecordEntity): Completable

    @Delete
    abstract fun delete(historyRecordEntity: HistoryRecordEntity): Completable

    @Query("SELECT * from history_records where serial = :serialNumber and historyRecordType= :entryType " +
        " and id= (select max(id) from history_records where serial = :serialNumber " +
        " and historyRecordType= :entryType) ")
    abstract fun getLatestHistoryEntry(serialNumber: Long, entryType: HistoryEntryType): HistoryRecordEntity?

    @Query("SELECT * from history_records where serial = :serialNumber and historyRecordType='Event' " +
        " and id= ( select max(id) from history_records where serial = :serialNumber " +
        "           and historyRecordType='Event' " +
        "           and (entryType='PUMP_MODE_CHANGED' or entryType='DELIVERY_STATUS_CHANGED')) ")
    abstract fun getLatestDeliveryStatusChanged(serialNumber: Long): HistoryRecordEntity?

    //PUMP_MODE_CHANGED, DELIVERY_STATUS_CHANGED

    // select * from test_table where type = 'EVENT' and number_count =
    // (select max(test_table.number_count) from test_table where type = 'EVENT')

    // @Query("UPDATE historyrecords SET resolvedResult = :resolvedResult, resolvedAt = :resolvedAt WHERE id = :id ")
    // abstract fun markResolved(id: String, resolvedResult: ResolvedResult, resolvedAt: Long): Completable

}