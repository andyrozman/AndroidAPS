package app.aaps.pump.tandem.common.database.dao

import androidx.room.*
import app.aaps.pump.tandem.common.database.data.entity.TandemQualifyingEventEntity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
abstract class TandemQualifyingEventsDao {

    @Query("SELECT * from qualifying_events WHERE pumpSerial=:serialNo AND dateTime >= :since")
    abstract fun allSinceWithSerial(serialNo: Int, since: Long): Single<List<TandemQualifyingEventEntity>>

    @Query("SELECT * from qualifying_events WHERE pumpSerial=:serialNo AND dateTime >= :since order by id desc")
    abstract fun allSinceWithSerialBlocking(serialNo: Int, since: Long): List<TandemQualifyingEventEntity>

    @Query("SELECT * from qualifying_events WHERE pumpSerial=:serialNo order by id desc limit 30")
    abstract fun getLast30ItemsWithSerialBlocking(serialNo: Int): List<TandemQualifyingEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveBlocking(tandemHistoryRecordEntity: TandemQualifyingEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(tandemHistoryRecordEntity: TandemQualifyingEventEntity): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveAll(list: List<TandemQualifyingEventEntity>): Completable

    @Query("SELECT COUNT(*) FROM qualifying_events")
    abstract fun getEventsCount(): Single<Long>


}