package app.aaps.pump.tandem.common.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.pump.tandem.common.database.cleanup.PumpDbCleanup
import app.aaps.pump.tandem.common.database.dao.TandemCleanupDao
import app.aaps.pump.tandem.common.database.dao.TandemHistoryRecordDao
import app.aaps.pump.tandem.common.database.dao.TandemQualifyingEventsDao
import app.aaps.pump.tandem.common.database.dao.TandemSiteChangeDao
import app.aaps.pump.tandem.common.database.data.entity.TandemHistoryRecordEntity
import app.aaps.pump.tandem.common.database.data.entity.TandemQualifyingEventEntity
import app.aaps.pump.tandem.common.database.data.entity.TandemSiteChangeEntity


@Database(
    entities = [TandemHistoryRecordEntity::class,
                TandemQualifyingEventEntity::class,
                TandemSiteChangeEntity::class],
    exportSchema = true,
    version = TandemPumpDatabase.VERSION,
    autoMigrations = [AutoMigration(from = 1, to = 2),
        AutoMigration(from = 4, to = 5)]
)
abstract class TandemPumpDatabase : RoomDatabase() {

    abstract fun historyRecordDao(): TandemHistoryRecordDao

    abstract fun qualifyingEventsDao(): TandemQualifyingEventsDao

    abstract fun siteChangeDao() : TandemSiteChangeDao

    abstract fun cleanupDao(): TandemCleanupDao

    companion object {

        const val VERSION = 5

        fun build(context: Context, aapsLogger: AAPSLogger, aapsSchedulers: AapsSchedulers) =
            Room.databaseBuilder(
                context.applicationContext,
                TandemPumpDatabase::class.java,
                "tandem_pump_database.db")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { db ->
                    PumpDbCleanup.runOnce(db, aapsSchedulers)
                        .subscribe({ /* ok */ },
                                   { e ->
                                       aapsLogger.error(LTag.PUMP, "Tandem Database Cleanup failed", e)
                                   })
                }

        // 2 -> 3 added indexes for faster access and for easier cleanup
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_records_pumpTime ON history_records(pumpTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_qualifying_events_dateTime ON qualifying_events(dateTime)")
            }
        }

        // New: idempotent, aligns runtime with Room’s EXPECTED schema for v4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_records_pumpTime ON history_records(pumpTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_qualifying_events_dateTime ON qualifying_events(dateTime)")
            }
        }

    }


}


