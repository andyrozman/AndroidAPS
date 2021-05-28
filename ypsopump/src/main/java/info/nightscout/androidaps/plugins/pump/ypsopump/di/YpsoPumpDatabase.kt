package info.nightscout.androidaps.dependencyInjection

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.Reusable
import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.logging.AAPSLogger
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
    internal fun provideHistoryRecordDao(historyDatabase: YpsoPumpHistoryDatabase): HistoryRecordDao = historyDatabase.historyRecordDao()

    @Provides
    @Reusable // no state, let system decide when to reuse or create new.
    internal fun provideHistoryMapper(ypsoPumpUtil: YpsoPumpUtil) = HistoryMapper(ypsoPumpUtil)

    @Provides
    @Singleton
    internal fun provideYpsoPumpHistory(dao: HistoryRecordDao, pumpHistoryDatabase: YpsoPumpHistoryDatabase, historyMapper: HistoryMapper, pumpSync: PumpSync, pumpStatus: YpsopumpPumpStatus, aapsLogger: AAPSLogger) =
        YpsoPumpHistory(dao, pumpHistoryDatabase, historyMapper, pumpSync, pumpStatus, aapsLogger)

}