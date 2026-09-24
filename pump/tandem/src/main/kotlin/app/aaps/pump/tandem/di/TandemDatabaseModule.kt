package app.aaps.pump.tandem.di

import android.content.Context
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.pump.tandem.common.database.TandemPumpDatabase
import app.aaps.pump.tandem.common.database.dao.TandemHistoryRecordDao
import app.aaps.pump.tandem.common.database.dao.TandemQualifyingEventsDao
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// @ContributesTo(AppScope::class)
// @BindingContainer
// @Suppress("unused")
// class TandemDatabaseModule {
//
//     @Provides
//
//     internal fun provideDatabase(context: Context,
//                                  aapsLogger: AAPSLogger,
//                                  aapsSchedulers: AapsSchedulers): TandemPumpDatabase =
//         TandemPumpDatabase.build(context, aapsLogger, aapsSchedulers)
//
//     @Provides
//     @Singleton
//     internal fun provideHistoryRecordDao(historyDatabase: TandemPumpDatabase): TandemHistoryRecordDao =
//         historyDatabase.historyRecordDao()
//
//     @Provides
//     @Singleton
//     internal fun provideTandemQualifyingEventsDao(historyDatabase: TandemPumpDatabase): TandemQualifyingEventsDao =
//         historyDatabase.qualifyingEventsDao()
//
// }