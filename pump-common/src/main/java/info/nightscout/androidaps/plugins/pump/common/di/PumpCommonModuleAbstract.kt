package info.nightscout.androidaps.plugins.pump.common.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.plugins.pump.common.ble.BondStateReceiver
import info.nightscout.androidaps.plugins.pump.common.ui.PumpBLEConfigActivity

@Module
@Suppress("unused")
abstract class PumpCommonModuleAbstract {

    @ContributesAndroidInjector abstract fun contributesBondStateReceiver(): BondStateReceiver
    @ContributesAndroidInjector abstract fun contributesPumpBLEConfigActivity(): PumpBLEConfigActivity

}
