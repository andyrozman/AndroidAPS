package info.nightscout.pump.common.di

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.sharedPreferences.SP
import dagger.Module
import info.nightscout.aaps.pump.common.di.PumpCommonModuleAbstract
import info.nightscout.aaps.pump.common.di.PumpCommonModuleImpl

@Module(
    includes = [
        PumpCommonModuleAbstract::class,
        PumpCommonModuleImpl::class
    ]
)
open class PumpCommonModule
