package app.aaps.pump.tandem.di

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.pump.tandem.common.concurrency.CommSuspendGate
import app.aaps.pump.tandem.common.concurrency.PumpAvailabilityState
import app.aaps.pump.tandem.common.concurrency.PumpOpQueue
import app.aaps.pump.tandem.common.database.dao.TandemQualifyingEventsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
open class TandemModuleImpl {

    @Provides
    @Singleton
    fun providePumpAvailabilityState(logger: AAPSLogger): PumpAvailabilityState =
        PumpAvailabilityState(logger)

    @Provides
    @Singleton
    fun provideCommSuspendGate(logger: AAPSLogger): CommSuspendGate =
        CommSuspendGate(logger)

    @Provides
    @Singleton
    fun providePumpOpQueue(
        logger: AAPSLogger,
        availability: PumpAvailabilityState,
        commSuspend: CommSuspendGate
    ): PumpOpQueue = PumpOpQueue(logger, availability, commSuspend)
}