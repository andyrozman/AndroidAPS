package app.aaps.pump.tandem.common.driver.config

import app.aaps.core.data.pump.defs.PumpType
import app.aaps.pump.common.driver.PumpDriverConfiguration

import app.aaps.pump.common.driver.db.PumpDriverDatabaseOperation
import app.aaps.pump.common.driver.db.PumpDriverDummyDatabaseOperation
import app.aaps.pump.common.driver.history.PumpHistoryDataProvider


abstract class TandemPumpDriverConfiguration(
    private var pumpType: PumpType
) : PumpDriverConfiguration {


    override fun getPumpType(): PumpType {
        return pumpType
    }

    // Tandem will not be using PumpHistoryActivity, it has its own Compose History UI
    override fun getPumpHistoryDataProvider(): PumpHistoryDataProvider? {
        return null
    }

    override fun getPumpDriverDatabaseOperation(): PumpDriverDatabaseOperation {
        return PumpDriverDummyDatabaseOperation()
    }

    override var logPrefix: String = "TandemMobiPlugin::"
    override var canHandleDST: Boolean = true
    override var hasService: Boolean = true
}