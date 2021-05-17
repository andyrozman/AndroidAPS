package info.nightscout.androidaps.plugins.pump.ypsopump.comm

import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.*
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType.*
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.experimental.and

@Singleton
class YpsoPumpDataConverter @Inject constructor(var pumpStatus: YpsopumpPumpStatus,
                                                var aapsLogger: AAPSLogger) {

    private val YPSOPUMP_10_MASTER_VERSIONS = Regex("^[a-zA-Z]?01\\.0[01]\\.[0-9][0-9]$")

    fun convertReadResult(uuid: String, data: ByteArray): Any? {

        aapsLogger.debug(LTag.PUMPBTCOMM, "Data Received: [uid=" + uuid + ", data=" + ByteUtil.getHex(data) + "]")

        val characteristic: YpsoGattCharacteristic = YpsoGattCharacteristic.lookup(uuid)!!


        return when (characteristic) {
            YpsoGattCharacteristic.MASTER_SOFTWARE_VERSION -> decodeMasterSoftwareVersion(data)
            YpsoGattCharacteristic.SYSTEM_DATE             -> decodeDate(data)
            YpsoGattCharacteristic.SYSTEM_TIME             -> decodeTime(data)
            YpsoGattCharacteristic.ALARM_ENTRY_COUNT,
            YpsoGattCharacteristic.ALARM_ENTRY_VALUE,
            YpsoGattCharacteristic.EVENT_ENTRY_COUNT,
            YpsoGattCharacteristic.EVENT_ENTRY_VALUE,
            YpsoGattCharacteristic.SETTINGS_VALUE,
            YpsoGattCharacteristic.SUPERVISOR_SOFTWARE_VERSION,
            YpsoGattCharacteristic.SYSTEM_ENTRY_COUNT,
            YpsoGattCharacteristic.SYSTEM_ENTRY_VALUE      -> decodeUnsupportedEntry(data)

            else                                           -> {
                aapsLogger.debug(LTag.PUMPBTCOMM, "Unknown uuid value: " + uuid)
                null
            }
        }

//        if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.MASTER_SOFTWARE_VERSION_UUID) {
//            mBleCallBack.OnReadMasterSoftwareVersion(characteristic.getValue(), status as Int)
//        } else if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.SYSTEM_TIME_UUID) {
//            mBleCallBack.OnReadSystemTime(characteristic.getValue(), status as Int)
//        } else if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.SYSTEM_DATE_UUID) {
//            mBleCallBack.OnReadSystemDate(characteristic.getValue(), status as Int)
//        } else if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.ALARM_ENTRY_COUNT_UUID) {
//            mBleCallBack.OnReadAlarmEntryCount(characteristic.getValue(), status as Int)
//        } else if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.ALARM_ENTRY_VALUE_UUID) {
//            mBleCallBack.OnReadAlarmEntryValue(characteristic.getValue(), status as Int)
//        } else if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.EVENT_ENTRY_COUNT_UUID) {
//            mBleCallBack.OnReadEventEntryCount(characteristic.getValue(), status as Int)
//        } else if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.EVENT_ENTRY_VALUE_UUID) {
//            mBleCallBack.OnReadEventEntryValue(characteristic.getValue(), status as Int)
//        } else if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.SETTINGS_VALUE_UUID) {
//            mBleCallBack.OnReadSettingValue(characteristic.getValue(), status as Int)
//        } else if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.SUPERVISOR_SOFTWARE_VERSION_UUID) {
//            mBleCallBack.OnReadSupervisorSoftwareVersion(characteristic.getValue(), status as Int)
//        } else if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.SYSTEM_ENTRY_COUNT_UUID) {
//            mBleCallBack.OnReadSystemEntryCount(characteristic.getValue(), status as Int)
//        } else if (characteristic.getUuid().toString().toUpperCase() == YpsoGattUids.SYSTEM_ENTRY_VALUE_UUID) {
//            mBleCallBack.OnReadSystemEntryValue(characteristic.getValue(), status as Int)
//        }

        //return null
    }

    private fun decodeUnsupportedEntry(data: ByteArray): Any? {
        TODO("Not yet implemented")
        aapsLogger.error(LTag.PUMPBTCOMM, "Unsupported Entry: ")
        return null;
    }

    fun decodeMasterSoftwareVersion(data: ByteArray): YpsoPumpFirmware {
        val versionString = ByteUtil.getString(data)

        aapsLogger.debug(LTag.PUMPBTCOMM, "Decoded Master Software Version: As string: " + versionString)

        if (StringUtils.isBlank(versionString)) {
            return YpsoPumpFirmware.Unknown
        }
        return if (versionString != null) {
            if (versionString.matches(YPSOPUMP_10_MASTER_VERSIONS)) {
                YpsoPumpFirmware.VERSION_1_0
            } else
                YpsoPumpFirmware.VERSION_1_5
        } else
            YpsoPumpFirmware.Unknown
    }

    fun decodeDate(data: ByteArray): DateTimeDto {
        val dt = DateTimeDto()
        //dt.year = getCharValueAsNumber(data, 2)
        dt.year = ByteUtil.toInt(data[1], data[0])
        dt.month = ByteUtil.toInt(data[2])
        dt.day = ByteUtil.toInt(data[3])

        aapsLogger.debug(LTag.PUMPBTCOMM, "Decoded Date: " + dt.toString())

        return dt
    }

    fun decodeTime(data: ByteArray): DateTimeDto {
        val dt = DateTimeDto()
        dt.hour = byteToInt(data, 0, 1) //getCharValueAsNumber(data, 1)
        dt.minute = ByteUtil.toInt(data[1]) //getCharValueAsNumber(data, 1, 1)
        dt.second = ByteUtil.toInt(data[2]) //getCharValueAsNumber(data, 2, 1)

        aapsLogger.debug(LTag.PUMPBTCOMM, "Decoded Time: " + dt.toString())

        return dt
    }

//    protected fun getCharValueAsNumber(paBuffer: ByteArray, piLength: Int): Int {
//        return getCharValueAsNumber(paBuffer, 0, piLength)
//    }
//
//    protected fun getCharValueAsNumber(paBuffer: ByteArray, piStart: Int, piLength: Int): Int {
//        var num = 0
//        for (num2 in piLength - 1 downTo -1 + 1) {
//            num = num shl 8
//            num = num.toInt() or (paBuffer[num2 + piStart] and 0xFF.toByte()).toInt()
//        }
//        return num
//    }

    fun decodeEvent(data: ByteArray): EventDto? {
        var eventDto: EventDto? = null
        // firmware should always be there, but if its not we default to 1.5
        if (pumpStatus.ypsopumpFirmware == null || pumpStatus.ypsopumpFirmware == YpsoPumpFirmware.VERSION_1_5) {

            val date = DateTime(2000, 1, 1, 0, 0)
                .plusSeconds(byteToInt(data, 0, 4))

            var entryTypeInt = byteToInt(data, 4, 1);
            var entryType = YpsoPumpEventType.getByCode(entryTypeInt)

            eventDto = EventDto(
                DateTimeDto(date.year, date.monthOfYear, date.dayOfMonth, date.hourOfDay, date.minuteOfHour, date.secondOfMinute),
                entryType,
                entryTypeInt,
                byteToInt(data, 5, 2),
                byteToInt(data, 7, 2),
                byteToInt(data, 9, 2),
                byteToInt(data, 11, 4),
                byteToInt(data, 15, 2))
        } else {
            val year = 2000 + (data[0] and 0xFF.toByte())
            val month = (data[1] and 0xFF.toByte()).toInt()
            val day = (data[2] and 0xFF.toByte()).toInt()
            val hour = (data[3] and 0xFF.toByte()).toInt()
            val minute = (data[4] and 0xFF.toByte()).toInt()
            val second = (data[5] and 0xFF.toByte()).toInt()

            val entryTypeInt = byteToInt(data, 10, 1);
            val entryType = YpsoPumpEventType.getByCode(entryTypeInt)

            eventDto = EventDto(
                DateTimeDto(year, month, day, hour, minute, second),
                entryType,
                entryTypeInt,
                byteToInt(data, 11, 2),
                byteToInt(data, 13, 2),
                byteToInt(data, 15, 2),
                byteToInt(data, 6, 2),
                byteToInt(data, 8, 2))
        }

        when (eventDto.entryType) {
            BASAL_PROFILE_A_PATTERN_CHANGED,
            BASAL_PROFILE_B_PATTERN_CHANGED -> eventDto.subObject = BasalProfileEntry(
                eventDto.entryValue1,
                eventDto.entryValue2 / 100.0)
            DAILY_TOTAL_INSULIN             -> eventDto.subObject = TotalDailyInsulin((((eventDto.entryValue2 shl 16) + eventDto.entryValue1) * 1.0) / 100.0)
            DATE_CHANGED                    -> eventDto.subObject = DateTimeDto(2000 + eventDto.entryValue3,
                eventDto.entryValue2,
                eventDto.entryValue1,
                eventDto.dateTimeLocal.hour,
                eventDto.dateTimeLocal.minute,
                eventDto.dateTimeLocal.second)
            TIME_CHANGED                    -> eventDto.subObject = DateTimeDto(eventDto.dateTimeLocal.year,
                eventDto.dateTimeLocal.month,
                eventDto.dateTimeLocal.day,
                eventDto.entryValue1,
                eventDto.entryValue2,
                eventDto.entryValue3)

            BOLUS_NORMAL,
            BOLUS_NORMAL_RUNNING,
            BOLUS_NORMAL_ABORT,
            BOLUS_BLIND,
            BOLUS_BLIND_RUNNING,
            BOLUS_BLIND_ABORT               -> {
                eventDto.subObject = Bolus(eventDto.entryValue1 / 100.0,
                    false,
                    eventDto.entryType == BOLUS_NORMAL_ABORT || eventDto.entryType == BOLUS_BLIND_ABORT,
                    eventDto.entryType == BOLUS_NORMAL_RUNNING || eventDto.entryType == BOLUS_BLIND_RUNNING)
            }

            BOLUS_EXTENDED,
            BOLUS_EXTENDED_RUNNING,
            BOLUS_EXTENDED_ABORT            -> {
                eventDto.subObject = Bolus(eventDto.entryValue1 / 100.0,
                    eventDto.entryValue2,
                    false,
                    eventDto.entryType == BOLUS_NORMAL_ABORT || eventDto.entryType == BOLUS_BLIND_ABORT,
                    eventDto.entryType == BOLUS_EXTENDED_RUNNING)
            }

            BOLUS_COMBINED_RUNNING,
            BOLUS_COMBINED,
            BOLUS_COMBINED_ABORT            -> {
                eventDto.subObject = Bolus(eventDto.entryValue2 / 100.0,
                    (eventDto.entryValue1 - eventDto.entryValue2) / 100.0,
                    eventDto.entryValue3,
                    false,
                    eventDto.entryType == BOLUS_NORMAL_ABORT || eventDto.entryType == BOLUS_BLIND_ABORT,
                    eventDto.entryType == BOLUS_COMBINED_RUNNING)
            }

            TEMPORARY_BASAL,
            TEMPORARY_BASAL_RUNNING,
            TEMPORARY_BASAL_ABORT           -> {
                eventDto.subObject = BasalProfileTemp(
                    eventDto.entryValue1,
                    eventDto.entryValue2,
                    eventDto.entryType == TEMPORARY_BASAL_RUNNING)
            }

            ALARM_BATTERY_REMOVED           -> eventDto.subObject = Alarm(AlarmType.BatteryRemoved)
            ALARM_BATTERY_EMPTY             -> eventDto.subObject = Alarm(AlarmType.BatteryEmpty, eventDto.entryValue1)
            ALARM_REUSABLE_ERROR            -> eventDto.subObject = Alarm(AlarmType.ReusableError, eventDto.entryValue1, eventDto.entryValue2, eventDto.entryValue3)
            ALARM_NO_CARTRIDGE              -> eventDto.subObject = Alarm(AlarmType.NoCartridge)
            ALARM_CARTRIDGE_EMPTY           -> eventDto.subObject = Alarm(AlarmType.CartridgeEmpty)
            ALARM_OCCLUSION                 -> eventDto.subObject = Alarm(AlarmType.Occlusion, eventDto.entryValue1)
            ALARM_AUTO_STOP                 -> eventDto.subObject = Alarm(AlarmType.AutoStop)
            ALARM_LIPO_DISCHARGED           -> eventDto.subObject = Alarm(AlarmType.LipoDischarged, eventDto.entryValue1)
            ALARM_BATTERY_REJECTED          -> eventDto.subObject = Alarm(AlarmType.BatteryRejected, eventDto.entryValue1)

            BOLUS_STEP_CHANGED              -> {
                val step = eventDto.entryValue1 / 100.0
                eventDto.subObject = ConfigurationChanged(ConfigurationType.BolusStepChanged, "" + step)
            }

            BASAL_RATE_CAP_CHANGED,
            BOLUS_AMOUNT_CAP_CHANGED        -> {
                val cap = String.format(Locale.ROOT, "NEW=%.2f;OLD=%.2f", eventDto.entryValue1 / 100.0, eventDto.entryValue2 / 100.0)
                eventDto.subObject = ConfigurationChanged(if (eventDto.entryType == BOLUS_AMOUNT_CAP_CHANGED) ConfigurationType.BolusAmountCapChanged else ConfigurationType.BasalAmountCapChanged,
                    "" + cap)
            }

            PUMP_MODE_CHANGED,
            DELIVERY_STATUS_CHANGED         -> {
                val isRunning = (eventDto.entryValue1 == 10)

                eventDto.subObject = if (isRunning) PumpStatusChanged(PumpStatusType.PumpRunning)
                else PumpStatusChanged(PumpStatusType.PumpSuspended)
            }

            BATTERY_REMOVED                 -> eventDto.subObject = PumpStatusChanged(PumpStatusType.BatteryRemoved)
            BASAL_PROFILE_CHANGED           -> eventDto.subObject = ConfigurationChanged(ConfigurationType.BasalProfileChanged,
                if (eventDto.entryValue1 == 3) "A" else "B")

            PRIMING,
            CANNULA_PRIMING                 -> {
                val priming = String.format(Locale.ROOT, "AMOUNT=%.2f;MOTION_STATUS=%d", eventDto.entryValue1 / 100.0, eventDto.entryValue2)
                eventDto.subObject = PumpStatusChanged(PumpStatusType.Priming, priming)
//                val piEventTypeId2 = if (ypsoPumpVersion == eYpsoPumpVersion.YpsoPump_15 &&
//                        ypsoPumpEventType == eYpsoPumpEventType.PRIMING_FINISHED) 23 else if (ypsoPumpVersion != eYpsoPumpVersion.YpsoPump_15 ||
//                        ypsoPumpEventType != eYpsoPumpEventType.CANNULA_PRIMING_FINISHED) 20 else 24
            }

            REWIND                          -> {
                val rewind = String.format(Locale.ROOT, "VALUE_1=%d;VALUE_2=%d;VALUE_3=%d", eventDto.entryValue1, eventDto.entryValue2, eventDto.entryValue2)
                eventDto.subObject = PumpStatusChanged(PumpStatusType.Rewind, rewind)
            }

            else                            -> {
                aapsLogger.warn(LTag.PUMPBTCOMM, "Event Type ${eventDto.entryType} is not supported.")
            }

//            BOLUS_DELAYED_BACKUP -> TODO()
//            BOLUS_COMBINED_BACKUP -> TODO()
//            BASAL_PROFILE_TEMP_BACKUP -> TODO()
        }

        return eventDto
    }

    fun byteToInt(data: ByteArray, start: Int, length: Int): Int {
        if (length == 1) {
            return ByteUtil.toInt(data[start])
        } else if (length == 2) {
            return ByteUtil.toInt(data[start], data[start + 1], ByteUtil.BitConversion.LITTLE_ENDIAN)
        } else if (length == 3) {
            return ByteUtil.toInt(data[start], data[start + 1], data[start + 2], ByteUtil.BitConversion.LITTLE_ENDIAN)
        } else if (length == 4) {
            return ByteUtil.toInt(data[start], data[start + 1], data[start + 2], data[start + 3], ByteUtil.BitConversion.LITTLE_ENDIAN)
        } else {
            aapsLogger.error(LTag.PUMPBTCOMM, "byteToInt, length $length not supported.")
            return 0
        }
    }

}