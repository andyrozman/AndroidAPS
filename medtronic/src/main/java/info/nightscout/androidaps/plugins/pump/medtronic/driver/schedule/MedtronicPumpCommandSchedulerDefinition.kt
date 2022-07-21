package info.nightscout.androidaps.plugins.pump.medtronic.driver.schedule

import info.nightscout.androidaps.plugins.pump.common.driver.scheduler.PumpCommandSchedulerSettings
import info.nightscout.androidaps.plugins.pump.common.driver.scheduler.PumpStatusRefreshType

class MedtronicPumpCommandSchedulerDefinition : PumpCommandSchedulerSettings {

    override fun getPossibleUpdateDefinitions(): HashMap<PumpStatusRefreshType, Int> {
        TODO("Not yet implemented")
    }


    override fun canPumpBeUnreachable(): Boolean {
        return true
    }
}