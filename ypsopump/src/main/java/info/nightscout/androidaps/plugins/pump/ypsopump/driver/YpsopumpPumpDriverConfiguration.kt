package info.nightscout.androidaps.plugins.pump.ypsopump.driver

import info.nightscout.androidaps.plugins.pump.common.driver.PumpBLESelectorInterface
import info.nightscout.androidaps.plugins.pump.common.driver.PumpDriverConfiguration
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.ble.YpsoPumpBLESelector
import javax.inject.Inject

class YpsopumpPumpDriverConfiguration @Inject constructor(
    var pumpBLESelector: YpsoPumpBLESelector
) : PumpDriverConfiguration {

    override fun getPumpBLESelector(): PumpBLESelectorInterface {
        return pumpBLESelector;
    }
}