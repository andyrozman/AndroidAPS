package info.nightscout.androidaps.plugins.pump.tandem.driver.config

import info.nightscout.androidaps.plugins.pump.common.driver.PumpBLESelector
import info.nightscout.androidaps.plugins.pump.common.driver.PumpDriverConfiguration
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryDataProvider
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