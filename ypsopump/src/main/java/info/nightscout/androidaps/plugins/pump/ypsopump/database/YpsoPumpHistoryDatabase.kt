package info.nightscout.androidaps.plugins.pump.ypsopump.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [HistoryRecordEntity::class],
    exportSchema = true,
    version = YpsoPumpHistoryDatabase.VERSION
)
@TypeConverters(DatabaseConverters::class)
abstract class YpsoPumpHistoryDatabase: RoomDatabase() {

    abstract fun historyRecordDao() : HistoryRecordDao

    companion object {

        const val VERSION = 1

        fun build(context: Context) =
            Room.databaseBuilder(context.applicationContext, YpsoPumpHistoryDatabase::class.java, "ypsopump_history_database.db")
                .fallbackToDestructiveMigration()
                .build()
    }

}