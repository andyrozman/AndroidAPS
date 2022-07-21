package info.nightscout.androidaps.plugins.pump.medtronic.driver.schedule

import info.nightscout.androidaps.plugins.pump.common.driver.scheduler.PumpCommandSchedulerSettings
import info.nightscout.androidaps.plugins.pump.common.driver.scheduler.PumpStatusRefreshType
import info.nightscout.androidaps.plugins.pump.medtronic.defs.MedtronicCommandType

class MedtronicPumpCommandSchedulerSettings : PumpCommandSchedulerSettings {

    var refeshTimeMap: MutableMap<PumpStatusRefreshType, Int>

    override fun canPumpBeUnreachable(): Boolean {
        return true
    }

    override fun getPossibleUpdateDefinitions(): MutableMap<PumpStatusRefreshType, Int>  {
        return refeshTimeMap
    }

    init {
        refeshTimeMap = mutableMapOf()
        refeshTimeMap.put(PumpStatusRefreshType.PumpHistory, 5)
        refeshTimeMap.put(PumpStatusRefreshType.Configuration, 0)
        refeshTimeMap.put(PumpStatusRefreshType.RemainingInsulin, -1)
        refeshTimeMap.put(PumpStatusRefreshType.BatteryStatus, 55)
        refeshTimeMap.put(PumpStatusRefreshType.PumpTime, 60)
    }

}