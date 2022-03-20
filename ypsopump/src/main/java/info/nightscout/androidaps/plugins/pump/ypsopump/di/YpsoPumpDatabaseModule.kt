package info.nightscout.androidaps.dependencyInjection

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.Reusable
import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.plugins.pump.ypsopump.database.HistoryMapper
import info.nightscout.androidaps.plugins.pump.ypsopump.database.HistoryRecordDao
import info.nightscout.androidaps.plugins.pump.ypsopump.database.YpsoPumpDatabase
import info.nightscout.androidaps.plugins.pump.ypsopump.database.YpsoPumpHistory
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import javax.inject.Singleton

@Module
@Suppress("unused")
class YpsoPumpDatabaseModule {

    @Provides
    @Singleton
    internal fun provideDatabase(context: Context): YpsoPumpDatabase = YpsoPumpDatabase.build(context)

    @Provides
    @Singleton
    internal fun provideHistoryRecordDao(historyDatabase: YpsoPumpDatabase): HistoryRecordDao = historyDatabase.historyRecordDao()

    @Provides
    @Reusable // no state, let system decide when to reuse or create new.
    internal fun provideHistoryMapper(ypsoPumpUtil: YpsoPumpUtil, aapsLogger: AAPSLogger) = HistoryMapper(ypsoPumpUtil, aapsLogger)

    @Provides
    @Singleton
    internal fun provideYpsoPumpHistory(
        dao: HistoryRecordDao,
        pumpHistoryDatabase: YpsoPumpDatabase,
        historyMapper: HistoryMapper,
        pumpSync: PumpSync,
        ypsoPumpUtil: YpsoPumpUtil,
        pumpStatus: YpsopumpPumpStatus,
        aapsLogger: AAPSLogger
    ) =
        YpsoPumpHistory(dao, pumpHistoryDatabase, historyMapper, pumpSync, ypsoPumpUtil, pumpStatus, aapsLogger)

}