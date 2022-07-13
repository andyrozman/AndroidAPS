package info.nightscout.androidaps.plugins.pump.tandem.defs

import info.nightscout.androidaps.plugins.pump.common.defs.PumpHistoryEntryGroup

enum class YpsoPumpEventType(val code: Int, val group: PumpHistoryEntryGroup, val resourceId: Int? = null) {
    UNDEFINED(-1, PumpHistoryEntryGroup.Unknown),
    BOLUS_EXTENDED_RUNNING(1, PumpHistoryEntryGroup.Bolus),
    BOLUS_NORMAL(2, PumpHistoryEntryGroup.Bolus),
    BOLUS_EXTENDED(3, PumpHistoryEntryGroup.Bolus),
    PRIMING(4, PumpHistoryEntryGroup.Base),
    BOLUS_STEP_CHANGED(5, PumpHistoryEntryGroup.Configuration),
    BASAL_PROFILE_SWITCHED(6, PumpHistoryEntryGroup.Basal),
    BASAL_PROFILE_A_PATTERN_CHANGED(7, PumpHistoryEntryGroup.Basal),
    BASAL_PROFILE_B_PATTERN_CHANGED(8, PumpHistoryEntryGroup.Basal),
    TEMPORARY_BASAL_RUNNING(9, PumpHistoryEntryGroup.Basal),
    TEMPORARY_BASAL(10, PumpHistoryEntryGroup.Basal),
    DATE_CHANGED(12, PumpHistoryEntryGroup.Configuration),
    TIME_CHANGED(13, PumpHistoryEntryGroup.Configuration),
    PUMP_MODE_CHANGED(14, PumpHistoryEntryGroup.Base),
    REWIND(0x10, PumpHistoryEntryGroup.Base),
    BOLUS_COMBINED_RUNNING(17, PumpHistoryEntryGroup.Bolus),
    BOLUS_COMBINED(18, PumpHistoryEntryGroup.Bolus),
    BOLUS_NORMAL_RUNNING(19, PumpHistoryEntryGroup.Bolus),
    BOLUS_DELAYED_BACKUP(20, PumpHistoryEntryGroup.Bolus),
    BOLUS_COMBINED_BACKUP(21, PumpHistoryEntryGroup.Bolus),
    BASAL_PROFILE_TEMP_BACKUP(22, PumpHistoryEntryGroup.Basal),
    DAILY_TOTAL_INSULIN(23, PumpHistoryEntryGroup.Statistic),
    BATTERY_REMOVED(24, PumpHistoryEntryGroup.Other),
    CANNULA_PRIMING(25, PumpHistoryEntryGroup.Base),
    BOLUS_BLIND(26, PumpHistoryEntryGroup.Bolus),
    BOLUS_BLIND_RUNNING(27, PumpHistoryEntryGroup.Bolus),
    BOLUS_BLIND_ABORT(28, PumpHistoryEntryGroup.Bolus),
    BOLUS_NORMAL_ABORT(29, PumpHistoryEntryGroup.Bolus),
    BOLUS_EXTENDED_ABORT(30, PumpHistoryEntryGroup.Bolus),
    BOLUS_COMBINED_ABORT(0x1F, PumpHistoryEntryGroup.Bolus),
    TEMPORARY_BASAL_ABORT(0x20, PumpHistoryEntryGroup.Basal),
    BOLUS_AMOUNT_CAP_CHANGED(33, PumpHistoryEntryGroup.Configuration),
    BASAL_RATE_CAP_CHANGED(34, PumpHistoryEntryGroup.Configuration),
    ALARM_BATTERY_REMOVED(100, PumpHistoryEntryGroup.Alarm),
    ALARM_BATTERY_EMPTY(101, PumpHistoryEntryGroup.Alarm),
    ALARM_REUSABLE_ERROR(102, PumpHistoryEntryGroup.Alarm),
    ALARM_NO_CARTRIDGE(103, PumpHistoryEntryGroup.Alarm),
    ALARM_CARTRIDGE_EMPTY(104, PumpHistoryEntryGroup.Alarm),
    ALARM_OCCLUSION(105, PumpHistoryEntryGroup.Alarm),
    ALARM_AUTO_STOP(106, PumpHistoryEntryGroup.Alarm),
    ALARM_LIPO_DISCHARGED(107, PumpHistoryEntryGroup.Alarm),
    ALARM_BATTERY_REJECTED(108, PumpHistoryEntryGroup.Alarm),
    DELIVERY_STATUS_CHANGED(150, PumpHistoryEntryGroup.Base),
    BASAL_PROFILE_A_CHANGED(4000, PumpHistoryEntryGroup.Basal),
    BASAL_PROFILE_B_CHANGED(4001, PumpHistoryEntryGroup.Basal),
    ;

    fun getDescriptionResourceId(): Int {
        return if (resourceId == null)
            this.group.resourceId
        else
            resourceId
    }

    companion object {

        private var mapByEventId: MutableMap<Int, YpsoPumpEventType> = mutableMapOf()

        @JvmStatic
        fun getByCode(code: Int): YpsoPumpEventType {
            return if (mapByEventId.containsKey(code)) {
                mapByEventId[code]!!
            } else {
                UNDEFINED
            }
        }

        @JvmStatic
        fun isRunningEvent(entryType: YpsoPumpEventType): Boolean {
            return (entryType == BOLUS_EXTENDED_RUNNING) ||
                (entryType == TEMPORARY_BASAL_RUNNING) ||
                (entryType == BOLUS_COMBINED_RUNNING) ||
                (entryType == BOLUS_NORMAL_RUNNING) ||
                (entryType == BOLUS_BLIND_RUNNING)
        }

        init {
            for (value in values()) {
                mapByEventId[value.code] = value
            }
        }
    }
}