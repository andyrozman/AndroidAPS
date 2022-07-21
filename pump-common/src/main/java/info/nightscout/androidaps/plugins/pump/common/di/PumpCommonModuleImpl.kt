package info.nightscout.androidaps.plugins.pump.common.di

import dagger.Module
import dagger.Provides
import info.nightscout.androidaps.interfaces.ActivePlugin
import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.interfaces.ResourceHelper
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.pump.common.driver.scheduler.PumpCommandSchedulerUtil
import info.nightscout.androidaps.plugins.pump.common.sync.PumpSyncStorage
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.sharedPreferences.SP
import javax.inject.Singleton

@Module
@Suppress("unused")
class PumpCommonModuleImpl {

    @Provides
    @Singleton
    fun providesPumpSyncStorage(
        pumpSync: PumpSync,
        sp: SP,
        aapsLogger: AAPSLogger
    ): PumpSyncStorage {
        return PumpSyncStorage(pumpSync, sp, aapsLogger)
    }

    @Provides
    @Singleton
    fun providesPumpCommandSchedulerUtil(aapsLogger: AAPSLogger,
                                   rxBus: RxBus,
                                   resourceHelper: ResourceHelper,
                                   activePlugin: ActivePlugin
    ): PumpCommandSchedulerUtil {
        return PumpCommandSchedulerUtil(aapsLogger, rxBus, resourceHelper, activePlugin)
    }



}