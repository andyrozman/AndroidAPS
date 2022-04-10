package info.nightscout.androidaps.plugins.pump.ypsopump.driver.config

import info.nightscout.androidaps.plugins.pump.common.driver.PumpBLESelector
import info.nightscout.androidaps.plugins.pump.common.driver.PumpDriverConfiguration
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryDataProvider
import javax.inject.Inject

class YpsopumpPumpDriverConfiguration @Inject constructor(
    var pumpBLESelector: YpsoPumpBLESelector,
    var pumpHistoryDataProvider: YpsopumpPumpHistoryDataProvider
) : PumpDriverConfiguration {

    override fun getPumpBLESelector(): PumpBLESelector {
        return pumpBLESelector;
    }

    override fun getPumpHistoryDataProvider(): PumpHistoryDataProvider {
        return pumpHistoryDataProvider
    }
}