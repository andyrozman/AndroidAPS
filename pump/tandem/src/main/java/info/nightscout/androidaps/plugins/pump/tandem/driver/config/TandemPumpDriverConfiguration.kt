package info.nightscout.androidaps.plugins.pump.tandem.driver.config

import info.nightscout.pump.common.driver.PumpDriverConfiguration
import info.nightscout.pump.common.driver.ble.PumpBLESelector
import info.nightscout.pump.common.driver.history.PumpHistoryDataProvider
import javax.inject.Inject

class TandemPumpDriverConfiguration @Inject constructor(
    var pumpBLESelector: TandemBLESelector //,
    //var pumpHistoryDataProvider: TandemHistoryDataProvider
) : PumpDriverConfiguration {

    override fun getPumpBLESelector(): PumpBLESelector {
        return pumpBLESelector;
    }

    override fun getPumpHistoryDataProvider(): PumpHistoryDataProvider? {
        return null
        // return pumpHistoryDataProvider
    }
}