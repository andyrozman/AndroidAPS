package info.nightscout.androidaps.dependencyInjection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.aaps.pump.tandem.TandemPumpFragment
import info.nightscout.aaps.pump.tandem.comm.TandemDataConverter
import info.nightscout.aaps.pump.tandem.driver.connector.TandemPumpConnectionManager
import info.nightscout.aaps.pump.tandem.driver.connector.TandemPumpConnector
import info.nightscout.aaps.pump.tandem.ui.TandemPumpBLEConfigActivity
import info.nightscout.aaps.pump.tandem.driver.TandemPumpStatus
import info.nightscout.aaps.pump.tandem.driver.config.TandemBLESelector
import info.nightscout.aaps.pump.tandem.driver.config.TandemPumpDriverConfiguration
import info.nightscout.aaps.pump.tandem.driver.config.TandemHistoryDataProvider
import info.nightscout.aaps.pump.tandem.util.TandemPumpUtil

@Module(includes = [TandemDatabaseModule::class])
@Suppress("unused")
abstract class TandemModule {

    // Driver basics
    @ContributesAndroidInjector abstract fun contributeTandemPumpStatus(): TandemPumpStatus
    @ContributesAndroidInjector abstract fun contributeTandemPumpUtil(): TandemPumpUtil

    // Data
    //@ContributesAndroidInjector
    //abstract fun contributesYpsoPumpDataConverter(): YpsoPumpDataConverter // TODO remove after history implemented

    // Communication Layer
    @ContributesAndroidInjector abstract fun contributeTandemConnectionManager(): TandemPumpConnectionManager
    @ContributesAndroidInjector abstract fun contributeTandemPumpConnector(): TandemPumpConnector
    @ContributesAndroidInjector abstract fun contributeTandemDataConverter(): TandemDataConverter
    //@ContributesAndroidInjector abstract fun contributeTandemPairingManager(): TandemPairingManager
    //@ContributesAndroidInjector abstract fun contributeTandemCommunicationManager(): TandemCommunicationManager

    // Activites and Fragments
    @ContributesAndroidInjector abstract fun contributesTandemPumpFragment(): TandemPumpFragment
    @ContributesAndroidInjector abstract fun contributesTandemPumpBLEConfigActivity(): TandemPumpBLEConfigActivity

    // Configuration
    @ContributesAndroidInjector abstract fun contributesTandemBLESelector(): TandemBLESelector
    @ContributesAndroidInjector abstract fun contributesTandemHistoryDataProvider(): TandemHistoryDataProvider
    @ContributesAndroidInjector abstract fun contributesTandemPumpDriverConfiguration(): TandemPumpDriverConfiguration

}