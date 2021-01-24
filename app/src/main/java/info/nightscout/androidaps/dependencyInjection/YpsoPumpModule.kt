package info.nightscout.androidaps.dependencyInjection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpConnectionManager
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil

@Module
@Suppress("unused")
abstract class YpsoPumpModule {
    @ContributesAndroidInjector abstract fun ypsopumpPumpStatus(): YpsopumpPumpStatus
    @ContributesAndroidInjector abstract fun ypsopumpUtil(): YpsoPumpUtil
    @ContributesAndroidInjector abstract fun ypsopumpConnectionManager(): YpsoPumpConnectionManager
}