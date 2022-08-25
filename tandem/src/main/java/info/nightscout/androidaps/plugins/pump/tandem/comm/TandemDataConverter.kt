package info.nightscout.androidaps.plugins.pump.tandem.comm

import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.helpers.Dates
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.*
import com.jwoglom.pumpx2.pump.messages.response.historyLog.*
import info.nightscout.androidaps.plugins.pump.common.data.BasalProfileDto
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.DataCommandResponse
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.common.driver.history.PumpDataConverter
import info.nightscout.androidaps.plugins.pump.tandem.data.history.DateTimeChanged
import info.nightscout.androidaps.plugins.pump.tandem.data.history.HistoryLogDto
import info.nightscout.androidaps.plugins.pump.tandem.data.history.HistoryLogObject
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpEventType
import info.nightscout.androidaps.plugins.pump.tandem.driver.TandemPumpStatus
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import info.nightscout.shared.sharedPreferences.SP
import javax.inject.Inject

class TandemDataConverter @Inject constructor(
    var aapsLogger: AAPSLogger,
    var sp: SP,
    var pumpStatus: TandemPumpStatus,
    var pumpUtil: TandemPumpUtil) : PumpDataConverter {

    // fun convertMessageToDataCommandResponse(message: Message): DataCommandResponse<Any?> {
    //
    //     when(message) {
    //
    //         // Battery Level
    //         is CurrentBatteryV1Response,
    //         is CurrentBatteryV2Response         -> return getBatteryResponse(message as CurrentBatteryAbstractResponse)
    //
    //         // Configuration
    //         // is ControlIQInfoV1Response,
    //         // is ControlIQInfoV2Response          -> return getControlIQEnabled(message)
    //         // is BasalLimitSettingsResponse       -> return message.basalLimit
    //         // is GlobalMaxBolusSettingsResponse   -> return message.maxBolus
    //
    //         // Insulin Level
    //         is InsulinStatusResponse            -> return getInsulinStatus(message)
    //
    //
    //
    //
    //         else                           -> {
    //             aapsLogger.warn(LTag.PUMPCOMM, "Can't convert Tandem response Message of type: ${message.opCode()} and class: ${message.javaClass.name}")
    //             return null
    //         }
    //     }
    //
    //     return null
    // }



    fun convertMessageToValue(message: Message) : Any? {

        when(message) {

            // Battery Level
            //is CurrentBatteryV1Response,
            //is CurrentBatteryV2Response         -> return getBatteryResponse(message as CurrentBatteryAbstractResponse)

            // Configuration
            is ControlIQInfoV1Response,
            is ControlIQInfoV2Response          -> return getControlIQEnabled(message)
            is BasalLimitSettingsResponse       -> return message.basalLimit
            is GlobalMaxBolusSettingsResponse   -> return message.maxBolus

            // Insulin Level
            //is InsulinStatusResponse            -> return getInsulinStatus(message)




            else                           -> {
                aapsLogger.warn(LTag.PUMPCOMM, "Can't convert Tandem response Message of type: ${message.opCode()} and class: ${message.javaClass.name}")
                return null
            }
        }

    }

    fun getInsulinStatus(message: InsulinStatusResponse): DataCommandResponse<Double?> {
        return DataCommandResponse(
            PumpCommandType.GetRemainingInsulin, true, null, message.currentInsulinAmount.toDouble())
    }

    fun getTempBasalRate(message: TempRateResponse): DataCommandResponse<TempBasalPair?> {

        val tempBasal = TempBasalPair()

        tempBasal.insulinRate = message.percentage.toDouble()
        tempBasal.isActive = message.active
        tempBasal.durationMinutes = message.duration.toInt()
        tempBasal.setStartTime(pumpUtil.getTimeFromPumpAsEpochMillis(message.startTime))

        return DataCommandResponse(
            PumpCommandType.GetTemporaryBasal, true, null, tempBasal)
    }


    fun getBasalProfileResponse(settings: IDPSettingsResponse, mapSegments: MutableMap<Int, IDPSegmentResponse>): DataCommandResponse<BasalProfileDto?> {

        val timedMap = mutableMapOf<Int, IDPSegmentResponse>()

        for (segment in mapSegments.values) {
            val hour = Math.floor(segment.profileStartTime/60.0).toInt()

            if (!timedMap.containsKey(hour)) {
                timedMap[hour] = segment
            }
        }

        var currentSegment: IDPSegmentResponse? = null

        val basalArray: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                                                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                                                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        for(time in 0..23) {
            if (timedMap.containsKey(time)) {
                currentSegment = timedMap.get(time)!!
            }

            basalArray[time] = if (currentSegment!!.profileBasalRate==0) 0.0 else currentSegment.profileBasalRate/1000.0
        }

        return DataCommandResponse(
            PumpCommandType.GetBasalProfile, true, null, BasalProfileDto(basalArray, settings.name))

    }


    private fun getBasalLimit(message: BasalLimitSettingsResponse): Long {
        return message.basalLimit
    }

    private fun getControlIQEnabled(message: Message): Boolean {
        if (message is ControlIQInfoV1Response)
            return message.closedLoopEnabled
        else if (message is ControlIQInfoV2Response)
            return message.closedLoopEnabled

        return false
    }

    fun getBatteryResponse(message: CurrentBatteryAbstractResponse): DataCommandResponse<Int?> {
        return DataCommandResponse(
            PumpCommandType.GetBatteryStatus, true, null, message.currentBatteryIbc)
    }


    //CGMHistoryLog

    fun decodeHistoryLogs(historyLogList: List<HistoryLog>): MutableList<HistoryLogDto> {
        var historyListOut = mutableListOf<HistoryLogDto>()

        for (historyLog in historyLogList) {
            val decodeHistoryLog = decodeHistoryLog(historyLog)
            if (decodeHistoryLog!=null) {
                historyListOut.add(decodeHistoryLog)
            }
        }

        return historyListOut
    }



    fun decodeHistoryLog(historyLogPump: HistoryLog): HistoryLogDto? {

        val historyLog = createHistoryLogDto(historyLogPump)

        when (historyLogPump) {

            is TimeChangeHistoryLog            -> historyLog.subObject = createTimeChangeRecord(historyLogPump)
            is DateChangeHistoryLog            -> historyLog.subObject = createDateChangeRecord(historyLogPump)
            // one is invalid duplicate ?
            //is DateChangeResponse,             -> historyLog.subObject = createDateChangeRecord(historyLogPump)

            // not implemented yet
            is BolexCompletedHistoryLog,
            is BolusCompletedHistoryLog,
            is BolusDeliveryHistoryLog,
            is BolusRequestedMsg1HistoryLog,
            is BolusRequestedMsg2HistoryLog,
            is BolusRequestedMsg3HistoryLog,

            // not supported
            is CGMHistoryLog,
            is BGHistoryLog                    -> historyLog.subObject = null
            else                               -> historyLog.subObject = null
        }



        return if (historyLog.subObject==null) null else historyLog

    }


    private fun createDateChangeRecord(historyLogPump: DateChangeHistoryLog): HistoryLogObject? {
        return createDateTimeChangeRecord(historyLogPump.getDateAfterInstant().toEpochMilli(), false)
    }

    private fun createDateChangeRecord(historyLogPump: DateChangeResponse): HistoryLogObject? {
        return createDateTimeChangeRecord(Dates.fromJan12008EpochDaysToDate(historyLogPump.dateAfter).toEpochMilli(), false)
    }

    private fun createTimeChangeRecord(historyLogPump: TimeChangeHistoryLog): HistoryLogObject? {
        return createDateTimeChangeRecord(Dates.fromJan12008EpochDaysToDate(historyLogPump.timeAfter).toEpochMilli(), true)
    }

    private fun createDateTimeChangeRecord(dateTime: Long, timeChanged: Boolean): DateTimeChanged {

        //historyLogPump.timeAfter

        //return DateTimeChanged()

        TODO("Not yet implemented")
    }

    fun createHistoryLogDto(historyLogPump: HistoryLog) : HistoryLogDto {
        return HistoryLogDto(id= null,
                             serial = pumpStatus.serialNumber,
                             pumpEventType = TandemPumpEventType.getByCode(historyLogPump.typeId()),
                             dateTimeInMillis = historyLogPump.pumpTimeSecInstant.toEpochMilli(),
                             sequenceNum = historyLogPump.sequenceNum,
                             subObject = null);
    }



}