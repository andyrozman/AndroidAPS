package info.nightscout.pump.common.di

import dagger.Module
import dagger.Provides
import info.nightscout.androidaps.plugins.pump.common.di.PumpCommonModuleAbstract
import info.nightscout.androidaps.plugins.pump.common.di.PumpCommonModuleImpl
import info.nightscout.interfaces.pump.PumpSync
import info.nightscout.pump.common.sync.PumpSyncStorage
import info.nightscout.rx.logging.AAPSLogger
import info.nightscout.shared.sharedPreferences.SP
import javax.inject.Singleton


@Module(
    includes = [
        PumpCommonModuleAbstract::class,
        PumpCommonModuleImpl::class
    ]
)
open class PumpCommonModule

// @Module
// @Suppress("unused")
// class PumpCommonModule {
//
//     @Provides
//     @Singleton
//     fun providesPumpSyncStorage(
//         pumpSync: PumpSync,
//         sp: SP,
//         aapsLogger: AAPSLogger
//     ): PumpSyncStorage {
//         return PumpSyncStorage(pumpSync, sp, aapsLogger)
//     }
//
// }

/*
@Module(
    includes = [
        PumpCommonModuleAbstract::class,
        PumpCommonModuleImpl::class
    ]
)
open class PumpCommonModule
*/
