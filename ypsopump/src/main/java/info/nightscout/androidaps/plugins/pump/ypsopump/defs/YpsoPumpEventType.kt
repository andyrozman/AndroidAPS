package info.nightscout.androidaps.plugins.pump.ypsopump.defs

import java.util.*

enum class YpsoPumpEventType(val code: Int) {
    UNDEFINED(-1),
    BOLUS_EXTENDED_RUNNING(1),
    BOLUS_NORMAL(2),
    BOLUS_EXTENDED(3),
    PRIMING(4),
    BOLUS_STEP_CHANGED(5),
    BASAL_PROFILE_CHANGED(6),
    BASAL_PROFILE_A_PATTERN_CHANGED(7),
    BASAL_PROFILE_B_PATTERN_CHANGED(8),
    TEMPORARY_BASAL_RUNNING(9),
    TEMPORARY_BASAL(10),
    DATE_CHANGED(12),
    TIME_CHANGED(13),
    PUMP_MODE_CHANGED(14),
    REWIND(0x10),
    BOLUS_COMBINED_RUNNING(17),
    BOLUS_COMBINED(18),
    BOLUS_NORMAL_RUNNING(19),
    BOLUS_DELAYED_BACKUP(20),
    BOLUS_COMBINED_BACKUP(21),
    BASAL_PROFILE_TEMP_BACKUP(22),
    DAILY_TOTAL_INSULIN(23),
    BATTERY_REMOVED(24),
    CANNULA_PRIMING(25),
    BOLUS_BLIND(26),
    BOLUS_BLIND_RUNNING(27),
    BOLUS_BLIND_ABORT(28),
    BOLUS_NORMAL_ABORT(29),
    BOLUS_EXTENDED_ABORT(30),
    BOLUS_COMBINED_ABORT(0x1F),
    TEMPORARY_BASAL_ABORT(0x20),
    BOLUS_AMOUNT_CAP_CHANGED(33),
    BASAL_RATE_CAP_CHANGED(34),
    ALARM_BATTERY_REMOVED(100),
    ALARM_BATTERY_EMPTY(101),
    ALARM_REUSABLE_ERROR(102),
    ALARM_NO_CARTRIDGE(103),
    ALARM_CARTRIDGE_EMPTY(104),
    ALARM_OCCLUSION(105),
    ALARM_AUTO_STOP(106),
    ALARM_LIPO_DISCHARGED(107),
    ALARM_BATTERY_REJECTED(108),
    DELIVERY_STATUS_CHANGED(150);

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

        init {
            for (value in values()) {
                mapByEventId[value.code] = value
            }
        }
    }
}