package info.nightscout.aaps.pump.common.driver

import info.nightscout.aaps.pump.common.driver.ble.PumpBLESelector
import info.nightscout.aaps.pump.common.driver.db.PumpDriverDatabaseOperation
import info.nightscout.aaps.pump.common.driver.db.PumpDriverDummyDatabaseOperation
import info.nightscout.aaps.pump.common.driver.history.PumpHistoryDataProvider

class PumpDriverDummyConfiguration : PumpDriverConfiguration {

    var pumpDriverDatabaseOperationHandler: PumpDriverDatabaseOperation = PumpDriverDummyDatabaseOperation()

    override fun getPumpBLESelector(): PumpBLESelector? {
        return null
    }

    override fun getPumpHistoryDataProvider(): PumpHistoryDataProvider? {
        return null
    }

    override fun getPumpDriverDatabaseOperation(): PumpDriverDatabaseOperation {
        return pumpDriverDatabaseOperationHandler
    }

    override var logPrefix: String = "Dummy"

    override var canHandleDST: Boolean = false

    override var hasService: Boolean = false

}