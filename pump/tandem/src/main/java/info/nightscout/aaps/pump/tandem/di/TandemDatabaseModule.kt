package info.nightscout.androidaps.dependencyInjection

//import android.content.Context
import dagger.Module
// import dagger.Provides
// import dagger.Reusable
// import info.nightscout.androidaps.interfaces.PumpSync
// import HistoryMapper
// import HistoryRecordDao
// import info.nightscout.androidaps.plugins.pump.tandem.database.YpsoPumpDatabase
// import info.nightscout.androidaps.plugins.pump.tandem.database.YpsoPumpHistory
// import TandemPumpStatus
// import TandemPumpUtil
// import app.aaps.core.interfaces.logging.AAPSLogger
// import javax.inject.Singleton

@Module
@Suppress("unused")
class TandemDatabaseModule {

    // @Provides
    // @Singleton
    // internal fun provideDatabase(context: Context): YpsoPumpDatabase = YpsoPumpDatabase.build(context)
    //
    // @Provides
    // @Singleton
    // internal fun provideHistoryRecordDao(historyDatabase: YpsoPumpDatabase): HistoryRecordDao = historyDatabase.historyRecordDao()
    //
    // @Provides
    // @Reusable // no state, let system decide when to reuse or create new.
    // internal fun provideHistoryMapper(tandemPumpUtil: TandemPumpUtil, aapsLogger: AAPSLogger) = HistoryMapper(tandemPumpUtil, aapsLogger)
    //
    // @Provides
    // @Singleton
    // internal fun provideYpsoPumpHistory(
    //     dao: HistoryRecordDao,
    //     pumpHistoryDatabase: YpsoPumpDatabase,
    //     historyMapper: HistoryMapper,
    //     pumpSync: PumpSync,
    //     tandemPumpUtil: TandemPumpUtil,
    //     pumpStatus: TandemPumpStatus,
    //     aapsLogger: AAPSLogger
    // ) =
    //     YpsoPumpHistory(dao, pumpHistoryDatabase, historyMapper, pumpSync, tandemPumpUtil, pumpStatus, aapsLogger)

}