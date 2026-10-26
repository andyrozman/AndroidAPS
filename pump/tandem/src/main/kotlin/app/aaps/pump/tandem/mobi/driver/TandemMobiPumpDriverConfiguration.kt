package app.aaps.pump.tandem.mobi.driver

import app.aaps.core.data.pump.defs.PumpType
import app.aaps.pump.tandem.common.driver.config.TandemPumpDriverConfiguration
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@Inject
class TandemMobiPumpDriverConfiguration : TandemPumpDriverConfiguration(PumpType.TANDEM_MOBI_BT) {

    override var logPrefix: String = "TandemMobiPumpPlugin::"

}


