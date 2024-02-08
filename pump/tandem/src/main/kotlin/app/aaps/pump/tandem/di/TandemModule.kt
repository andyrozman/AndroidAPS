package app.aaps.pump.tandem.di

import app.aaps.pump.tandem.TandemPumpFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector
import info.nightscout.androidaps.dependencyInjection.TandemDatabaseModule

@Module
@Suppress("unused")
abstract class TandemModule {

    // Activites and Fragments
    @ContributesAndroidInjector abstract fun contributesTandemPumpFragment(): TandemPumpFragment

}