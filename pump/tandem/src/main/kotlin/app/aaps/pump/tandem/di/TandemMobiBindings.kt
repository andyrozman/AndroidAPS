package app.aaps.pump.tandem.di

import app.aaps.core.interfaces.di.FeatureMemberInjectors
import app.aaps.pump.tandem.common.comm.ui.TandemUIDataStore
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
    fun provideTandemUIDataStore(): TandemUIDataStore = tandemUiDataStore

    @Provides
    @FeatureMemberInjectors
    @IntoMap
    @ClassKey(TandemService::class)
    fun bindTandemService(
        injector: MembersInjector<TandemService>
    ): MembersInjector<*> = injector


    // @Provides
    // @FeatureMemberInjectors
    // @IntoMap
    // @ClassKey(TandemMobiConnectionWizardActivity::class)
    // fun bindInsightAlertActivity(injector: MembersInjector<TandemMobiConnectionWizardActivity>): MembersInjector<*> = injector

}