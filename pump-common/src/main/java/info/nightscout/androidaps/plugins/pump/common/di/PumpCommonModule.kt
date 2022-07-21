package info.nightscout.androidaps.plugins.pump.common.di

import dagger.Module

@Module(
    includes = [
        PumpCommonModuleAbstract::class,
        PumpCommonModuleImpl::class
    ]
)
open class PumpCommonModule