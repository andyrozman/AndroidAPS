package info.nightscout.androidaps.plugins.pump.tandem.driver.config

import info.nightscout.androidaps.plugins.pump.common.defs.PumpHistoryEntryGroup
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryDataProviderAbstract
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryEntry
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryPeriod
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpHistoryText
import info.nightscout.androidaps.plugins.pump.common.utils.DateTimeUtil
import info.nightscout.androidaps.plugins.pump.tandem.R
import info.nightscout.androidaps.plugins.pump.tandem.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.tandem.database.HistoryRecordEntity
import info.nightscout.androidaps.plugins.pump.tandem.database.YpsoPumpHistory
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.androidaps.interfaces.ResourceHelper
import info.nightscout.shared.logging.AAPSLogger
import javax.inject.Inject

class TandemHistoryDataProvider @Inject constructor(
    var resourceHelper: ResourceHelper,
    var aapsLogger: AAPSLogger,
    var tandemPumpUtil: TandemPumpUtil,
    var ypsoPumpHistory: YpsoPumpHistory,
    var ypsoPumpDataConverter: YpsoPumpDataConverter
) : PumpHistoryDataProviderAbstract() {

    var groupList: List<PumpHistoryEntryGroup> = listOf()

    override fun getData(period: PumpHistoryPeriod): List<PumpHistoryEntry> {
        val dbHistoryList: List<HistoryRecordEntity>
        val outList: MutableList<PumpHistoryEntry> = mutableListOf()

        if (period == PumpHistoryPeriod.ALL) {
            dbHistoryList = ypsoPumpHistory.pumpHistoryDao.allBlocking()
        } else {
            val startingTimeForData = getStartingTimeForData(period)
            dbHistoryList = ypsoPumpHistory.pumpHistoryDao.allSinceBlocking(DateTimeUtil.toATechDate(startingTimeForData))
        }

        for (historyRecordEntity in dbHistoryList) {
            val domainObject = ypsoPumpHistory.historyMapper.entityToDomain(historyRecordEntity)
            domainObject.prepareEntryData(resourceHelper = resourceHelper, pumpDataConverter = ypsoPumpDataConverter)
            outList.add(domainObject)
        }

        return outList
    }

    override fun getInitialPeriod(): PumpHistoryPeriod {
        return PumpHistoryPeriod.ALL
    }

    override fun getSpinnerWidthInPixels(): Int {
        return 180
    }

    override fun getAllowedPumpHistoryGroups(): List<PumpHistoryEntryGroup> {

        if (groupList.isNotEmpty())
            return groupList

        PumpHistoryEntryGroup.doTranslation(resourceHelper)

        val groupListInternal: MutableList<PumpHistoryEntryGroup> = mutableListOf()

        groupListInternal.add(PumpHistoryEntryGroup.All)
        groupListInternal.add(PumpHistoryEntryGroup.EventsOnly)
        groupListInternal.add(PumpHistoryEntryGroup.EventsNoStat)
        groupListInternal.add(PumpHistoryEntryGroup.Bolus)
        groupListInternal.add(PumpHistoryEntryGroup.Basal)
        groupListInternal.add(PumpHistoryEntryGroup.Base)
        groupListInternal.add(PumpHistoryEntryGroup.Configuration)
        groupListInternal.add(PumpHistoryEntryGroup.Statistic)
        groupListInternal.add(PumpHistoryEntryGroup.Other)
        groupListInternal.add(PumpHistoryEntryGroup.Alarm)
        groupListInternal.add(PumpHistoryEntryGroup.Unknown)

        this.groupList = groupListInternal

        return this.groupList
    }

    override fun getText(key: PumpHistoryText): String {

        val stringId: Int

        when (key) {
            PumpHistoryText.PUMP_HISTORY -> stringId = R.string.ypsopump_pump_history
            else                         -> return key.name
        }

        return resourceHelper.gs(stringId)

    }

    override fun isItemInSelection(itemGroup: PumpHistoryEntryGroup, targetGroup: PumpHistoryEntryGroup): Boolean {
        if (targetGroup == PumpHistoryEntryGroup.EventsNoStat || targetGroup == PumpHistoryEntryGroup.EventsOnly) {
            if (targetGroup == PumpHistoryEntryGroup.EventsOnly) {
                return itemGroup != PumpHistoryEntryGroup.Alarm
            } else {
                return (itemGroup != PumpHistoryEntryGroup.Alarm && itemGroup != PumpHistoryEntryGroup.Statistic)
            }
        } else {
            return itemGroup === targetGroup
        }

    }

}