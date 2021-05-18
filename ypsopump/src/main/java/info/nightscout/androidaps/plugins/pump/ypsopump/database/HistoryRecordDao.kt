package info.nightscout.androidaps.plugins.pump.ypsopump.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Single

@Dao
abstract class HistoryRecordDao {

    @Query("SELECT * from history_records")
    abstract fun all(): Single<List<HistoryRecordEntity>>

    @Query("SELECT * from history_records")
    abstract fun allBlocking(): List<HistoryRecordEntity>

    @Query("SELECT * from history_records WHERE createdAt <= :since")
    abstract fun allSince(since: Long): Single<List<HistoryRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveBlocking(historyRecordEntity: HistoryRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(historyRecordEntity: HistoryRecordEntity): Completable

    @Delete
    abstract fun delete(historyRecordEntity: HistoryRecordEntity): Completable

    // @Query("UPDATE historyrecords SET resolvedResult = :resolvedResult, resolvedAt = :resolvedAt WHERE id = :id ")
    // abstract fun markResolved(id: String, resolvedResult: ResolvedResult, resolvedAt: Long): Completable

}