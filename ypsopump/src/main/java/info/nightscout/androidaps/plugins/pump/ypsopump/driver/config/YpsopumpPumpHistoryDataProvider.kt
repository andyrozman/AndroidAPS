package info.nightscout.androidaps.plugins.pump.ypsopump.driver.config

import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryDataProviderAbstract
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryEntry
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryText
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.shared.logging.AAPSLogger
import javax.inject.Inject

class YpsopumpPumpHistoryDataProvider @Inject constructor(
    var resourceHelper: ResourceHelper,
    var aapsLogger: AAPSLogger,
    var ypsoPumpUtil: YpsoPumpUtil,
    var ypsoPumpDataConverter: YpsoPumpDataConverter,
    var pumpSync: PumpSync
) : PumpHistoryDataProviderAbstract() {

    override fun getData(): List<PumpHistoryEntry> {
        return arrayListOf()
    }

    fun getInitialData(): List<PumpHistoryEntry> {
        return arrayListOf()
    }

    override fun getText(key: PumpHistoryText): String {
        TODO("Not yet implemented")
    }

}