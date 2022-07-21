package info.nightscout.androidaps.plugins.pump.common.driver.scheduler

interface PumpCommandSchedulerSettings {

    fun canPumpBeUnreachable(): Boolean

    fun getPossibleUpdateDefinitions(): MutableMap<PumpStatusRefreshType,Int>

    fun getRefreshTime(type: PumpStatusRefreshType): Int
}