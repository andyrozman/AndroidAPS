package info.nightscout.aaps.pump.tandem.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// TODO refactor this for Tandem

// @Database(
//     entities = [HistoryRecordEntity::class],
//     exportSchema = true,
//     version = YpsoPumpDatabase.VERSION
// )
// @TypeConverters(DatabaseConverters::class)
abstract class TandemPumpDatabase /*: RoomDatabase()*/ {

    //abstract fun historyRecordDao(): HistoryRecordDao

    // companion object {
    //
    //     const val VERSION = 1
    //
    //     fun build(context: Context) =
    //         Room.databaseBuilder(
    //             context.applicationContext,
    //             YpsoPumpDatabase::class.java,
    //             "ypsopump_database.db"
    //         )
    //             .allowMainThreadQueries()
    //             .fallbackToDestructiveMigration()
    //             .build()
    // }

}