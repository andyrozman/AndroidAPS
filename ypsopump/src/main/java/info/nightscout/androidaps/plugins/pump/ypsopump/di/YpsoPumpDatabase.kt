package info.nightscout.androidaps.dependencyInjection

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.YpsoPumpFragment
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpConnectionManager
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.*
import info.nightscout.androidaps.plugins.pump.ypsopump.config.YpsoPumpBLEConfigActivity
import info.nightscout.androidaps.plugins.pump.ypsopump.database.HistoryMapper
import info.nightscout.androidaps.plugins.pump.ypsopump.database.HistoryRecordDao
import info.nightscout.androidaps.plugins.pump.ypsopump.database.YpsoPumpHistory
import info.nightscout.androidaps.plugins.pump.ypsopump.database.YpsoPumpHistoryDatabase
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import javax.inject.Singleton

@Module
@Suppress("unused")
class YpsoPumpDatabase {

    @Provides
    @Singleton
    internal fun provideDatabase(context: Context): YpsoPumpHistoryDatabase = YpsoPumpHistoryDatabase.build(context)

    @Provides
    @Singleton
    internal fun provideHistoryRecordDao(dashHistoryDatabase: YpsoPumpHistoryDatabase): HistoryRecordDao = dashHistoryDatabase.historyRecordDao()

    @Provides
    @Reusable // no state, let system decide when to reuse or create new.
    internal fun provideHistoryMapper(ypsoPumpUtil: YpsoPumpUtil) = HistoryMapper(ypsoPumpUtil)

    @Provides
    @Singleton
    internal fun provideDashHistory(dao: HistoryRecordDao, historyMapper: HistoryMapper) =
        YpsoPumpHistory(dao, historyMapper)

}