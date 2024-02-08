package info.nightscout.androidaps.plugins.pump.medtronic.driver

import info.nightscout.aaps.pump.common.driver.PumpDriverConfiguration
import info.nightscout.aaps.pump.common.driver.ble.PumpBLESelector
import info.nightscout.aaps.pump.common.driver.db.PumpDriverDatabaseOperation
import info.nightscout.aaps.pump.common.driver.history.PumpHistoryDataProvider

class MedtronicPumpDriverConfiguration: PumpDriverConfiguration {

    override fun getPumpBLESelector(): PumpBLESelector? {
        //TODO("Not yet implemented")
        return null
    }

    override fun getPumpHistoryDataProvider(): PumpHistoryDataProvider? {
        //TODO("Not yet implemented")
        return null
    }

    override fun getPumpDriverDatabaseOperation(): PumpDriverDatabaseOperation {
        TODO("Not yet implemented")
    }

    override var logPrefix: String = "MedtronicPumpPlugin::"
        //get() = "MedtronicPumpPlugin::"

    override var canHandleDST: Boolean = false
        // get() = TODO("Not yet implemented")
        // set(value) {}

    override var hasService: Boolean = true
        // get() = true
        // set(value) {}
}