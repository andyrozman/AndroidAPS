package app.aaps.pump.tandem.di

import app.aaps.core.interfaces.di.PumpDriver
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.pump.tandem.common.comm.TandemDataConverter
import app.aaps.pump.tandem.common.comm.history.HistoryPostProcessor
import app.aaps.pump.tandem.common.comm.history.HistoryRetriever
import app.aaps.pump.tandem.common.comm.maint.TandemConnectionFixer
import app.aaps.pump.tandem.common.comm.qe.QualifyingEventHandler
import app.aaps.pump.tandem.common.database.data.TandemHistoryConverter
import app.aaps.pump.tandem.common.database.data.DbDataHandler
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnectionManager
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnector
import app.aaps.pump.tandem.common.service.TandemService
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import app.aaps.pump.tandem.mobi.ui.wizard.TandemMobiConnectionWizardActivity
import app.aaps.pump.tandem.mobi.driver.TandemMobiPumpDriverConfiguration
import dagger.Module
import dagger.android.ContributesAndroidInjector
import app.aaps.pump.tandem.common.util.PumpX2L
import app.aaps.pump.tandem.mobi.TandemMobiPumpPlugin
import app.aaps.pump.tandem.mobi.ui.TandemUiController
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap

@Module(includes = [TandemDatabaseModule::class, TandemModuleImpl::class])
@InstallIn(SingletonComponent::class)
@Suppress("unused")
abstract class TandemModule {

    // Driver basics
    @ContributesAndroidInjector abstract fun contributeTandemPumpUtil(): TandemPumpUtil
    @ContributesAndroidInjector abstract fun contributeTandemPumpStatus(): TandemPumpStatus

    // Communication Layer
    @ContributesAndroidInjector abstract fun contributeTandemConnectionManager(): TandemPumpConnectionManager
    @ContributesAndroidInjector abstract fun contributeTandemPumpConnector(): TandemPumpConnector
    @ContributesAndroidInjector abstract fun contributeTandemDataConverter(): TandemDataConverter

    // Configuration
    //@ContributesAndroidInjector abstract fun contributesTandemBLESelector(): TandemBLESelector
    //@ContributesAndroidInjector abstract fun contributesAdaptiveIntentPreference(): app.aaps.pump.tandem.common.util.AdaptiveIntentPreference

    // Data
    // @ContributesAndroidInjector abstract fun contributesTandemHistoryDataProvider(): TandemHistoryDataProvider

    // Service
    @ContributesAndroidInjector abstract fun contributesTandemService(): TandemService

    // Database
    @ContributesAndroidInjector abstract fun contributesDbDataHandler(): DbDataHandler
    @ContributesAndroidInjector abstract fun contributesDbDataConverter(): TandemHistoryConverter

    // Database Related (QE and History)
    @ContributesAndroidInjector abstract fun contributesQualifyingEventHandler(): QualifyingEventHandler
    @ContributesAndroidInjector abstract fun contributesHistoryRetriever(): HistoryRetriever
    @ContributesAndroidInjector abstract fun contributesHistoryPostProcessor(): HistoryPostProcessor


    // T-Mobi Package - Activites and Fragments
    @ContributesAndroidInjector abstract fun contributeAAPSTimber(): PumpX2L
    //@ContributesAndroidInjector abstract fun contributesTandemMobiPumpFragment(): TandemMobiPumpFragment
    @ContributesAndroidInjector abstract fun contributesTandemMobiPumpDriverConfiguration(): TandemMobiPumpDriverConfiguration
    @ContributesAndroidInjector abstract fun contributesTandemMobiPumpPlugin(): TandemMobiPumpPlugin
    @ContributesAndroidInjector abstract fun contributesTandemConnectionFixer(): TandemConnectionFixer
    @ContributesAndroidInjector abstract fun contributesTandemMobiConnectionWizardActivity(): TandemMobiConnectionWizardActivity


    // Compose UI Activities 3.0 -> Compose
    //@ContributesAndroidInjector abstract fun contributesActionsActivity(): ActionsActivity
    //@ContributesAndroidInjector abstract fun contributesDataActivity(): DataActivity


    // AAPS 4.0 Compose
    @ContributesAndroidInjector abstract fun contributesTandemUiController(): TandemUiController


    // @Provides
    // @Singleton
    //@ContributesAndroidInjector abstract fun contributesTandemUIDataStore(): TandemUIDataStore

    // T-Slim Package - Activites and Fragments (disabled for now, TSlim not supported, it is not loopable)
    // @ContributesAndroidInjector abstract fun contributesTandemPumpFragment(): TandemSlimPumpFragment
    // @ContributesAndroidInjector abstract fun contributesTandemPumpDriverConfiguration(): TandemPumpDriverConfiguration




    // Pump plugin registration — @IntKey range 1000–1200, see PluginsListModule for overview
    @Binds
    @PumpDriver
    @IntoMap
    @IntKey(1140)
    abstract fun bindTandemMobiPumpPlugin(plugin: TandemMobiPumpPlugin): PluginBase


}
