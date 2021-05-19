package info.nightscout.androidaps.dependencyInjection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.YpsoPumpFragment
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpConnectionManager
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.*
import info.nightscout.androidaps.plugins.pump.ypsopump.dialog.YpsoPumpBLEConfigActivity
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil

@Module(includes = [YpsoPumpDatabase::class])
@Suppress("unused")
abstract class YpsoPumpModule {

    // Drivers basics
    @ContributesAndroidInjector abstract fun ypsopumpPumpStatus(): YpsopumpPumpStatus
    @ContributesAndroidInjector abstract fun ypsopumpUtil(): YpsoPumpUtil

    // Data
    @ContributesAndroidInjector abstract fun contributesYpsoPumpDataConverter(): YpsoPumpDataConverter

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

    // Activites and Fragments
    @ContributesAndroidInjector abstract fun contributesYpsoPumpBLEConfigActivity(): YpsoPumpBLEConfigActivity
    @ContributesAndroidInjector abstract fun contributesYpsoPumpFragment(): YpsoPumpFragment

}