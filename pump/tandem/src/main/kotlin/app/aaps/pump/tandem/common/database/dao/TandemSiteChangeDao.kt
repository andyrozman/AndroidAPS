package app.aaps.pump.tandem.common.database.dao

import androidx.room.*
import app.aaps.pump.tandem.common.database.data.entity.TandemQualifyingEventEntity
import app.aaps.pump.tandem.common.database.data.entity.TandemSiteChangeEntity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
abstract class TandemSiteChangeDao {

    @Query("SELECT * from site_change WHERE pumpSerial=:serialNo AND storedInEvent = false order by dateTime")
    abstract fun allNotStoredWithSerial(serialNo: Long): Single<List<TandemSiteChangeEntity>>

    @Query("SELECT * from site_change WHERE pumpSerial=:serialNo AND storedInEvent = false order by dateTime")
    abstract fun allNotStoredWithSerialBlocking(serialNo: Long): List<TandemSiteChangeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveBlocking(tandemSiteChangeEntity: TandemSiteChangeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(tandemSiteChangeEntity: TandemSiteChangeEntity): Completable

    @Query("SELECT COUNT(*) FROM site_change")
    abstract fun getSiteChangesCount(): Single<Long>

    @Query("UPDATE site_change SET storedInEvent=true WHERE id=:id")
    abstract fun updateSiteChangeWithStoredInEventTrue(id: Long): Completable

}