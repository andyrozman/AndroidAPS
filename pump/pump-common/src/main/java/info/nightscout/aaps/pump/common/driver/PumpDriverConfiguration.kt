package info.nightscout.aaps.pump.common.driver

import info.nightscout.aaps.pump.common.driver.ble.PumpBLESelector
import info.nightscout.aaps.pump.common.driver.db.PumpDriverDatabaseOperation
import info.nightscout.aaps.pump.common.driver.history.PumpHistoryDataProvider

interface PumpDriverConfiguration {

    fun getPumpBLESelector(): PumpBLESelector?

    fun getPumpHistoryDataProvider(): PumpHistoryDataProvider?

    fun getPumpDriverDatabaseOperation(): PumpDriverDatabaseOperation

    var logPrefix : String

    var canHandleDST : Boolean

    var hasService: Boolean


}