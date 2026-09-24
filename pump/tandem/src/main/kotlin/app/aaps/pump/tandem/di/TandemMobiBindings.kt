package app.aaps.pump.tandem.di

import android.content.Context
import app.aaps.core.interfaces.di.FeatureMemberInjectors
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.pump.tandem.common.comm.ui.TandemUIDataStore
import app.aaps.pump.tandem.common.database.TandemPumpDatabase
import app.aaps.pump.tandem.common.database.dao.TandemHistoryRecordDao
import app.aaps.pump.tandem.common.database.dao.TandemQualifyingEventsDao
import app.aaps.pump.tandem.common.driver.tandemUiDataStore
import app.aaps.pump.tandem.common.service.TandemService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.MembersInjector
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object TandemMobiBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(context: Context,
                                 aapsLogger: AAPSLogger,
                                 aapsSchedulers: AapsSchedulers): TandemPumpDatabase =
        TandemPumpDatabase.build(context, aapsLogger, aapsSchedulers)

    @Provides
    @SingleIn(AppScope::class)
    fun provideHistoryRecordDao(historyDatabase: TandemPumpDatabase): TandemHistoryRecordDao =
        historyDatabase.historyRecordDao()

    @Provides
    @SingleIn(AppScope::class)
    fun provideTandemQualifyingEventsDao(historyDatabase: TandemPumpDatabase): TandemQualifyingEventsDao =
        historyDatabase.qualifyingEventsDao()


    @Provides
    @SingleIn(AppScope::class)
    fun provideTandemUIDataStore(): TandemUIDataStore = tandemUiDataStore

    @Provides
    @FeatureMemberInjectors
    @IntoMap
    @ClassKey(TandemService::class)
    fun bindTandemService(
        injector: MembersInjector<TandemService>
    ): MembersInjector<*> = injector

}