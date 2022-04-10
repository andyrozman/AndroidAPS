package info.nightscout.androidaps.dependencyInjection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.YpsoPumpFragment
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpConnectionManager
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.*
import info.nightscout.androidaps.plugins.pump.ypsopump.dialog.PumpHistoryActivity
import info.nightscout.androidaps.plugins.pump.ypsopump.dialog.YpsoPumpBLEConfigActivity
import info.nightscout.androidaps.plugins.pump.ypsopump.dialog.YpsoPumpHistoryActivity
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.config.YpsoPumpBLESelector
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.config.YpsopumpPumpDriverConfiguration
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.config.YpsopumpPumpHistoryDataProvider
import info.nightscout.androidaps.plugins.pump.ypsopump.handlers.YpsoPumpHistoryHandler
import info.nightscout.androidaps.plugins.pump.ypsopump.handlers.YpsoPumpStatusHandler
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil

@Module(includes = [YpsoPumpDatabaseModule::class])
@Suppress("unused")
abstract class YpsoPumpModule {

    // Driver basics
    @ContributesAndroidInjector abstract fun ypsopumpPumpStatus(): YpsopumpPumpStatus
    @ContributesAndroidInjector abstract fun contributeYpsoPumpUtil(): YpsoPumpUtil
    @ContributesAndroidInjector abstract fun contributeYpsoPumpStatusHandler(): YpsoPumpStatusHandler

    // Data
    @ContributesAndroidInjector
    abstract fun contributesYpsoPumpDataConverter(): YpsoPumpDataConverter

    @ContributesAndroidInjector
    abstract fun contributeYpsoPumpHistoryHandler(): YpsoPumpHistoryHandler

    // Communication Layer
    @ContributesAndroidInjector abstract fun ypsopumpConnectionManager(): YpsoPumpConnectionManager

    // BLE Commands (comm.ble.command)
    @ContributesAndroidInjector abstract fun contributeGetFirmwareVersion(): GetFirmwareVersion
    @ContributesAndroidInjector abstract fun contributeGetBasalProfile(): GetBasalProfile
    @ContributesAndroidInjector abstract fun contributeGetDateTime(): GetDateTime
    @ContributesAndroidInjector abstract fun contributeGetPumpSettings(): GetPumpSettings
    @ContributesAndroidInjector abstract fun contributeGetEvents(): GetEvents
    @ContributesAndroidInjector abstract fun contributeGetAlarms(): GetAlarms
    @ContributesAndroidInjector abstract fun contributeGetSystemEntries(): GetSystemEntries
    @ContributesAndroidInjector abstract fun contributeGetLastEvent(): GetLastEvent

    // Activites and Fragments
    @ContributesAndroidInjector abstract fun contributesYpsoPumpBLEConfigActivity(): YpsoPumpBLEConfigActivity
    @ContributesAndroidInjector abstract fun contributesYpsoPumpFragment(): YpsoPumpFragment
    @ContributesAndroidInjector abstract fun contributesYpsoPumpHistoryActivity(): YpsoPumpHistoryActivity
    @ContributesAndroidInjector abstract fun contributesPumpHistoryActivity(): PumpHistoryActivity  // TODO move to common

    // Configuration
    @ContributesAndroidInjector abstract fun contributesYpsoPumpBLESelector(): YpsoPumpBLESelector
    @ContributesAndroidInjector abstract fun contributesYpsoPumpHistoryDataProvider(): YpsopumpPumpHistoryDataProvider
    @ContributesAndroidInjector abstract fun contributesYpsopumpPumpDriverConfiguration(): YpsopumpPumpDriverConfiguration

}