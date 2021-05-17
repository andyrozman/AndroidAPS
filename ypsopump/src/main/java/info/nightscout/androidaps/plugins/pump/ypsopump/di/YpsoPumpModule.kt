package info.nightscout.androidaps.dependencyInjection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.YpsoPumpFragment
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpConnectionManager
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.*
import info.nightscout.androidaps.plugins.pump.ypsopump.config.YpsoPumpBLEConfigActivity
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil

@Module
@Suppress("unused")
abstract class YpsoPumpModule {

    @ContributesAndroidInjector
    abstract fun ypsopumpPumpStatus(): YpsopumpPumpStatus

    @ContributesAndroidInjector
    abstract fun ypsopumpUtil(): YpsoPumpUtil

    @ContributesAndroidInjector
    abstract fun ypsopumpConnectionManager(): YpsoPumpConnectionManager

    @ContributesAndroidInjector
    abstract fun contributesYpsoPumpDataConverter(): YpsoPumpDataConverter

    @ContributesAndroidInjector
    abstract fun contributeGetFirmwareVersion(): GetFirmwareVersion

    @ContributesAndroidInjector
    abstract fun contributeGetBasalProfile(): GetBasalProfile

    @ContributesAndroidInjector
    abstract fun contributeGetDateTime(): GetDateTime

    @ContributesAndroidInjector
    abstract fun contributeGetPumpSettings(): GetPumpSettings

    @ContributesAndroidInjector
    abstract fun contributeGetEvents(): GetEvents

    @ContributesAndroidInjector
    abstract fun contributeGetAlarms(): GetAlarms

    @ContributesAndroidInjector
    abstract fun contributeGetSystemEntries(): GetSystemEntries

    @ContributesAndroidInjector
    abstract fun contributesYpsoPumpBLEConfigActivity(): YpsoPumpBLEConfigActivity

    @ContributesAndroidInjector
    abstract fun contributesYpsoPumpFragment(): YpsoPumpFragment

}