package info.nightscout.androidaps.dependencyInjection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.plugins.pump.tandem.TandemPumpFragment
import info.nightscout.androidaps.plugins.pump.tandem.connector.TandemPumpConnectionManager
import info.nightscout.androidaps.plugins.pump.tandem.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.tandem.driver.TandemPumpStatus
import info.nightscout.androidaps.plugins.pump.tandem.driver.config.TandemBLESelector
import info.nightscout.androidaps.plugins.pump.tandem.driver.config.TandemPumpDriverConfiguration
import info.nightscout.androidaps.plugins.pump.tandem.driver.config.TandemHistoryDataProvider
import info.nightscout.androidaps.plugins.pump.tandem.handlers.YpsoPumpStatusHandler
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil

//@Module(includes = [TandemDatabaseModule::class])
@Module
@Suppress("unused")
abstract class TandemPumpModule {

    // Driver basics
    @ContributesAndroidInjector abstract fun ypsopumpPumpStatus(): TandemPumpStatus
    @ContributesAndroidInjector abstract fun contributeYpsoPumpUtil(): TandemPumpUtil
    @ContributesAndroidInjector abstract fun contributeYpsoPumpStatusHandler(): YpsoPumpStatusHandler

    // Data
    @ContributesAndroidInjector
    abstract fun contributesYpsoPumpDataConverter(): YpsoPumpDataConverter


    // Communication Layer
    @ContributesAndroidInjector abstract fun tandemConnectionManager(): TandemPumpConnectionManager

    // BLE Commands (comm.ble.command)
    // @ContributesAndroidInjector abstract fun contributeGetFirmwareVersion(): GetFirmwareVersion
    // @ContributesAndroidInjector abstract fun contributeGetBasalProfile(): GetBasalProfile
    // @ContributesAndroidInjector abstract fun contributeGetDateTime(): GetDateTime
    // @ContributesAndroidInjector abstract fun contributeGetPumpSettings(): GetPumpSettings
    // @ContributesAndroidInjector abstract fun contributeGetEvents(): GetEvents
    // @ContributesAndroidInjector abstract fun contributeGetAlarms(): GetAlarms
    // @ContributesAndroidInjector abstract fun contributeGetSystemEntries(): GetSystemEntries
    // @ContributesAndroidInjector abstract fun contributeGetLastEvent(): GetLastEvent

    // Activites and Fragments
    //@ContributesAndroidInjector abstract fun contributesYpsoPumpBLEConfigActivity(): YpsoPumpBLEConfigActivity
    @ContributesAndroidInjector abstract fun contributesTandemPumpFragment(): TandemPumpFragment

    //@ContributesAndroidInjector abstract fun contributesYpsoPumpHistoryActivity(): YpsoPumpHistoryActivity

    // Configuration
    @ContributesAndroidInjector abstract fun contributesTandemBLESelector(): TandemBLESelector
    @ContributesAndroidInjector abstract fun contributesTandemHistoryDataProvider(): TandemHistoryDataProvider
    @ContributesAndroidInjector abstract fun contributesTandemPumpDriverConfiguration(): TandemPumpDriverConfiguration

}