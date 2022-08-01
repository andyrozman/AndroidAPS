package info.nightscout.androidaps.dependencyInjection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.plugins.pump.tandem.TandemPumpFragment
import info.nightscout.androidaps.plugins.pump.tandem.comm.TandemCommunicationManager
import info.nightscout.androidaps.plugins.pump.tandem.comm.TandemPairingManager
import info.nightscout.androidaps.plugins.pump.tandem.connector.TandemPumpConnectionManager
import info.nightscout.androidaps.plugins.pump.tandem.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.tandem.connector.TandemPumpConnector
import info.nightscout.androidaps.plugins.pump.tandem.driver.TandemPumpStatus
import info.nightscout.androidaps.plugins.pump.tandem.driver.config.TandemBLESelector
import info.nightscout.androidaps.plugins.pump.tandem.driver.config.TandemPumpDriverConfiguration
import info.nightscout.androidaps.plugins.pump.tandem.driver.config.TandemHistoryDataProvider
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil

@Module(includes = [TandemDatabaseModule::class])
@Suppress("unused")
abstract class TandemModule {

    // Driver basics
    @ContributesAndroidInjector abstract fun contributeTandemPumpStatus(): TandemPumpStatus
    @ContributesAndroidInjector abstract fun contributeTandemPumpUtil(): TandemPumpUtil

    // Data
    @ContributesAndroidInjector
    abstract fun contributesYpsoPumpDataConverter(): YpsoPumpDataConverter // TODO remove


    // Communication Layer
    @ContributesAndroidInjector abstract fun contributeTandemConnectionManager(): TandemPumpConnectionManager
    @ContributesAndroidInjector abstract fun contributeTandemPumpConnector(): TandemPumpConnector
    @ContributesAndroidInjector abstract fun contributeTandemPairingManager(): TandemPairingManager
    @ContributesAndroidInjector abstract fun contributeTandemCommunicationManager(): TandemCommunicationManager


    // Activites and Fragments
    @ContributesAndroidInjector abstract fun contributesTandemPumpFragment(): TandemPumpFragment


    // Configuration
    @ContributesAndroidInjector abstract fun contributesTandemBLESelector(): TandemBLESelector
    @ContributesAndroidInjector abstract fun contributesTandemHistoryDataProvider(): TandemHistoryDataProvider
    @ContributesAndroidInjector abstract fun contributesTandemPumpDriverConfiguration(): TandemPumpDriverConfiguration

}