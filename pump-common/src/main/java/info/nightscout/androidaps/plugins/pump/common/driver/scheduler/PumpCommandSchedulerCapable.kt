package info.nightscout.androidaps.plugins.pump.common.driver.scheduler

import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus
import info.nightscout.androidaps.plugins.pump.common.utils.PumpUtil

interface PumpCommandSchedulerCapable {

    fun executeScheduledCommand(pumpStatusRefreshType: PumpStatusRefreshType): Boolean

    var hasTimeDateOrTimeZoneChanged: Boolean

    val isPumpNotReachable: Boolean

    /**
     * This method should contain all functionality for checking the time on pump and then acting accordingly (we can
     * either automatically set time, we can show notification that time is wrong or we can do both, it really
     * depends how it is implemented in driver
     */
    fun checkTimeAndOptionallySetTime()

    fun getSchedulerSettings(): PumpCommandSchedulerSettings

    fun getPumpUtil(): PumpUtil

    fun getPumpStatus(): PumpStatus

}