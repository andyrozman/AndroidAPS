package info.nightscout.androidaps.plugins.pump.ypsopump.handlers

import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.YpsoPumpStatusEntry
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.YpsoPumpStatusList
import info.nightscout.androidaps.plugins.pump.ypsopump.data.HistoryEntryType
import info.nightscout.androidaps.plugins.pump.ypsopump.database.HistoryRecordEntity
import info.nightscout.androidaps.plugins.pump.ypsopump.database.YpsoPumpHistory
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpConst
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.utils.sharedPreferences.SP
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YpsoPumpStatusHandler
@Inject constructor(var sp: SP,
                    var ypsoPumpUtil: YpsoPumpUtil,
                    var ypsoPumpHistory: YpsoPumpHistory,
                    var pumpStatus: YpsopumpPumpStatus) {

    fun loadYpsoPumpStatusList() {

        val jsonListValue = sp.getStringOrNull(YpsoPumpConst.Prefs.PumpStatusList, null)

        if (!jsonListValue.isNullOrBlank()) {
            val fromJson = ypsoPumpUtil.gson.fromJson(jsonListValue, YpsoPumpStatusList::class.java)
            pumpStatus.ypsoPumpStatusList = fromJson
        }

        if (pumpStatus.ypsoPumpStatusList == null) {
            pumpStatus.ypsoPumpStatusList = YpsoPumpStatusList(map = mutableMapOf())
        }

        val serialNumber = sp.getLong(YpsoPumpConst.Prefs.PumpSerial, 0L)

        if (serialNumber != 0L) {
            if (!pumpStatus.ypsoPumpStatusList!!.map.containsKey(serialNumber)) {
                Thread {
                    generateNewPumpStatusEntry(serialNumber)
                }.start()
            }
            pumpStatus.serialNumber = serialNumber
        }
    }

    private fun generateNewPumpStatusEntry(serialNumber: Long) {
        var ypsoPumpStatusEntry: YpsoPumpStatusEntry = YpsoPumpStatusEntry(serialNumber)

        var entry = getLatestEntryFromDatabase(serialNumber, HistoryEntryType.Event);

        if (entry != null) {
            ypsoPumpStatusEntry.lastEventSequenceNumber = entry.eventSequenceNumber
            //ypsoPumpStatusEntry.lastEventDate = entry.date
        } else {
            ypsoPumpStatusEntry.lastEventSequenceNumber = 0
            //ypsoPumpStatusEntry.lastEventDate = 0
        }

        entry = getLatestEntryFromDatabase(serialNumber, HistoryEntryType.Alarm);

        if (entry != null) {
            ypsoPumpStatusEntry.lastAlarmSequenceNumber = entry.eventSequenceNumber
            //ypsoPumpStatusEntry.lastAlarmDate = entry.date
        } else {
            ypsoPumpStatusEntry.lastAlarmSequenceNumber = 0
            //ypsoPumpStatusEntry.lastAlarmDate = 0
        }

        entry = getLatestEntryFromDatabase(serialNumber, HistoryEntryType.SystemEntry);

        if (entry != null) {
            ypsoPumpStatusEntry.lastSystemEntrySequenceNumber = entry.eventSequenceNumber
            //ypsoPumpStatusEntry.lastSystemEntryDate = entry.date
        } else {
            ypsoPumpStatusEntry.lastSystemEntrySequenceNumber = 0
            //ypsoPumpStatusEntry.lastSystemEntryDate = 0
        }

        pumpStatus.ypsoPumpStatusList!!.map.put(serialNumber, ypsoPumpStatusEntry)

        saveYpsoPumpStatusList()
    }

    private fun getLatestEntryFromDatabase(serialNumber: Long, entryType: HistoryEntryType): HistoryRecordEntity? {
        return ypsoPumpHistory.getLatestHistoryEntry(serialNumber, entryType)
    }

    fun switchPumpData() {
        val serialNumber = sp.getLong(YpsoPumpConst.Prefs.PumpSerial, 0L)

        if (serialNumber == 0L) {// this would only happen if user removed his pump, and haven't set the new one
            pumpStatus.serialNumber = null
            return
        }

        pumpStatus.serialNumber = serialNumber

        if (pumpStatus.ypsoPumpStatusList == null) {
            pumpStatus.ypsoPumpStatusList = YpsoPumpStatusList(map = mutableMapOf())
            generateNewPumpStatusEntry(serialNumber)
        } else if (!pumpStatus.ypsoPumpStatusList!!.map.containsKey(serialNumber)) {
            generateNewPumpStatusEntry(serialNumber)
        }
    }

    fun saveYpsoPumpStatusList() {
        sp.putString(YpsoPumpConst.Prefs.PumpStatusList, ypsoPumpUtil.gson.toJson(pumpStatus.ypsoPumpStatusList))
    }

}