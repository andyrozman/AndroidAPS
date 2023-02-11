package info.nightscout.androidaps.plugins.pump.tandem.defs

import info.nightscout.pump.common.defs.PumpHistoryEntryGroup

enum class TandemPumpHistoryType(val code: Int, val group: PumpHistoryEntryGroup, val resourceId: Int? = null) {

    UNDEFINED(-1, PumpHistoryEntryGroup.Unknown),

    TEMP_RATE_ACTIVATED(2, PumpHistoryEntryGroup.Basal),
    BASAL_RATE_CHANGE(3, PumpHistoryEntryGroup.Basal),
    PUMPING_SUSPENDED(11, PumpHistoryEntryGroup.Basal),
    PUMPING_RESUMED(12, PumpHistoryEntryGroup.Basal),
    TIME_CHANGED(13, PumpHistoryEntryGroup.Configuration),
    DATE_CHANGED(14, PumpHistoryEntryGroup.Configuration),
    TEMP_RATE_COMPLETED(15, PumpHistoryEntryGroup.Basal),
    BG_READING_TAKEN(16, PumpHistoryEntryGroup.Glucose),
    BOLUS_COMPLETED(20, PumpHistoryEntryGroup.Bolus),
    BOLEX_COMPLETED(21, PumpHistoryEntryGroup.Bolus),
    BOLUS_ACTIVATED(55, PumpHistoryEntryGroup.Bolus),
    IDP_MSG2(57, PumpHistoryEntryGroup.Basal),
    BOLEX_ACTIVATED(59, PumpHistoryEntryGroup.Bolus),

    BOLUS_REQUESTED_MSG_1(64, PumpHistoryEntryGroup.Bolus),
    BOLUS_REQUESTED_MSG_2(65, PumpHistoryEntryGroup.Bolus),
    BOLUS_REQUESTED_MSG_3(66, PumpHistoryEntryGroup.Bolus),

    IDP_TD_SEG(68, PumpHistoryEntryGroup.Basal),
    IDP(69, PumpHistoryEntryGroup.Basal),
    IDP_BOLUS(70, PumpHistoryEntryGroup.Basal),
    IDP_LIST(71, PumpHistoryEntryGroup.Basal),
    PARAM_PUMP_SETTINGS(73, PumpHistoryEntryGroup.Configuration),
    PARAM_GLOBAL_SETTINGS(74, PumpHistoryEntryGroup.Configuration),
    NEW_DAY(90, PumpHistoryEntryGroup.Basal),
    PARAM_REMINDER(96, PumpHistoryEntryGroup.Configuration),
    HOMIN_SETTINGS_CHANGE(142, PumpHistoryEntryGroup.Configuration),
    CGM_ANNU_SETTINGS(157, PumpHistoryEntryGroup.Configuration),
    CGM_TRANSMITTER_ID(156, PumpHistoryEntryGroup.Configuration),
    CGM_HGA_SETTINGS(165, PumpHistoryEntryGroup.Configuration),
    CGM_LGA_SETTINGS(166, PumpHistoryEntryGroup.Configuration),
    CGM_RRA_SETTINGS(167, PumpHistoryEntryGroup.Configuration),
    CGM_FRA_SETTINGS(168, PumpHistoryEntryGroup.Configuration),
    CGM_OOR_SETTINGS(169, PumpHistoryEntryGroup.Configuration),
    HYPO_MINIMIZER_SUSPEND(198, PumpHistoryEntryGroup.Configuration),
    HYPO_MINIMIZER_RESUME(199, PumpHistoryEntryGroup.Configuration),
    CGM_DATA_GXB(256, PumpHistoryEntryGroup.Glucose),
    BASAL_DELIVERY(279, PumpHistoryEntryGroup.Basal),
    BOLUS_DELIVERY(280, PumpHistoryEntryGroup.Bolus);


    // BOLUS_EXTENDED_RUNNING(1, PumpHistoryEntryGroup.Bolus),
    // BOLUS_NORMAL(2, PumpHistoryEntryGroup.Bolus),
    // BOLUS_EXTENDED(3, PumpHistoryEntryGroup.Bolus),
    // PRIMING(4, PumpHistoryEntryGroup.Base),
    // BOLUS_STEP_CHANGED(5, PumpHistoryEntryGroup.Configuration),
    // BASAL_PROFILE_SWITCHED(6, PumpHistoryEntryGroup.Basal),
    // BASAL_PROFILE_A_PATTERN_CHANGED(7, PumpHistoryEntryGroup.Basal),
    // BASAL_PROFILE_B_PATTERN_CHANGED(8, PumpHistoryEntryGroup.Basal),
    // TEMPORARY_BASAL_RUNNING(9, PumpHistoryEntryGroup.Basal),
    // TEMPORARY_BASAL(10, PumpHistoryEntryGroup.Basal),
    // DATE_CHANGED(12, PumpHistoryEntryGroup.Configuration),
    // TIME_CHANGED(13, PumpHistoryEntryGroup.Configuration),
    // PUMP_MODE_CHANGED(14, PumpHistoryEntryGroup.Base),
    // REWIND(0x10, PumpHistoryEntryGroup.Base),
    // BOLUS_COMBINED_RUNNING(17, PumpHistoryEntryGroup.Bolus),
    // BOLUS_COMBINED(18, PumpHistoryEntryGroup.Bolus),
    // BOLUS_NORMAL_RUNNING(19, PumpHistoryEntryGroup.Bolus),
    // BOLUS_DELAYED_BACKUP(20, PumpHistoryEntryGroup.Bolus),
    // BOLUS_COMBINED_BACKUP(21, PumpHistoryEntryGroup.Bolus),
    // BASAL_PROFILE_TEMP_BACKUP(22, PumpHistoryEntryGroup.Basal),
    // DAILY_TOTAL_INSULIN(23, PumpHistoryEntryGroup.Statistic),
    // BATTERY_REMOVED(24, PumpHistoryEntryGroup.Other),
    // CANNULA_PRIMING(25, PumpHistoryEntryGroup.Base),
    // BOLUS_BLIND(26, PumpHistoryEntryGroup.Bolus),
    // BOLUS_BLIND_RUNNING(27, PumpHistoryEntryGroup.Bolus),
    // BOLUS_BLIND_ABORT(28, PumpHistoryEntryGroup.Bolus),
    // BOLUS_NORMAL_ABORT(29, PumpHistoryEntryGroup.Bolus),
    // BOLUS_EXTENDED_ABORT(30, PumpHistoryEntryGroup.Bolus),
    // BOLUS_COMBINED_ABORT(0x1F, PumpHistoryEntryGroup.Bolus),
    // TEMPORARY_BASAL_ABORT(0x20, PumpHistoryEntryGroup.Basal),
    // BOLUS_AMOUNT_CAP_CHANGED(33, PumpHistoryEntryGroup.Configuration),
    // BASAL_RATE_CAP_CHANGED(34, PumpHistoryEntryGroup.Configuration),
    // ALARM_BATTERY_REMOVED(100, PumpHistoryEntryGroup.Alarm),
    // ALARM_BATTERY_EMPTY(101, PumpHistoryEntryGroup.Alarm),
    // ALARM_REUSABLE_ERROR(102, PumpHistoryEntryGroup.Alarm),
    // ALARM_NO_CARTRIDGE(103, PumpHistoryEntryGroup.Alarm),
    // ALARM_CARTRIDGE_EMPTY(104, PumpHistoryEntryGroup.Alarm),
    // ALARM_OCCLUSION(105, PumpHistoryEntryGroup.Alarm),
    // ALARM_AUTO_STOP(106, PumpHistoryEntryGroup.Alarm),
    // ALARM_LIPO_DISCHARGED(107, PumpHistoryEntryGroup.Alarm),
    // ALARM_BATTERY_REJECTED(108, PumpHistoryEntryGroup.Alarm),
    // DELIVERY_STATUS_CHANGED(150, PumpHistoryEntryGroup.Base),
    // BASAL_PROFILE_A_CHANGED(4000, PumpHistoryEntryGroup.Basal),
    // BASAL_PROFILE_B_CHANGED(4001, PumpHistoryEntryGroup.Basal),
    ;

    fun getDescriptionResourceId(): Int {
        return if (resourceId == null)
            this.group.resourceId
        else
            resourceId
    }

    companion object {

        private var mapByEventId: MutableMap<Int, TandemPumpHistoryType> = mutableMapOf()

        @JvmStatic
        fun getByCode(code: Int): TandemPumpHistoryType {
            return if (mapByEventId.containsKey(code)) {
                mapByEventId[code]!!
            } else {
                UNDEFINED
            }
        }

        // @JvmStatic
        // fun isRunningEvent(entryType: TandemPumpEventType): Boolean {
        //     return (entryType == BOLUS_EXTENDED_RUNNING) ||
        //         (entryType == TEMPORARY_BASAL_RUNNING) ||
        //         (entryType == BOLUS_COMBINED_RUNNING) ||
        //         (entryType == BOLUS_NORMAL_RUNNING) ||
        //         (entryType == BOLUS_BLIND_RUNNING)
        // }

        init {
            for (value in values()) {
                mapByEventId[value.code] = value
            }
        }
    }
}