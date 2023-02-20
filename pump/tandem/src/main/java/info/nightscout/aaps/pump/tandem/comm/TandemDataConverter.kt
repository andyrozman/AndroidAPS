package info.nightscout.aaps.pump.tandem.comm

import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.helpers.Dates
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.*
import com.jwoglom.pumpx2.pump.messages.response.historyLog.*
import info.nightscout.aaps.pump.common.data.BasalProfileDto
import info.nightscout.aaps.pump.common.driver.connector.commands.response.DataCommandResponse

import info.nightscout.aaps.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.aaps.pump.tandem.data.defs.TandemPumpHistoryType
import info.nightscout.aaps.pump.tandem.driver.TandemPumpStatus
import info.nightscout.aaps.pump.tandem.util.TandemPumpUtil
import info.nightscout.pump.common.defs.TempBasalPair
import info.nightscout.aaps.pump.common.driver.history.PumpDataConverter
import info.nightscout.aaps.pump.tandem.data.history.*
import info.nightscout.pump.core.utils.ByteUtil
import info.nightscout.rx.logging.AAPSLogger
import info.nightscout.rx.logging.LTag

import info.nightscout.shared.sharedPreferences.SP
import org.joda.time.DateTime
import javax.inject.Inject

class TandemDataConverter @Inject constructor(
    var aapsLogger: AAPSLogger,
    var sp: SP,
    var pumpStatus: TandemPumpStatus,
    var pumpUtil: TandemPumpUtil
) : PumpDataConverter {

    var tbrMap = HashMap<Int, TemporaryBasal>()


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
    //             aapsLogger.warn(LTag.PUMPCOMM, "Can't convert Tandem response Message of type: ${message.opCode()} and class: ${messageClass.name}")
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
            PumpCommandType.GetBasalProfile, true, null, BasalProfileDto(basalArray, settings.name)
        )

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

        if (!isLogTypeSupported(historyLogPump)) {
            return null
        }

        val historyLog = createHistoryLogDto(historyLogPump)

        when (historyLogPump) {

            // Date Time - WIP
            is TimeChangedHistoryLog           -> historyLog.subObject = createTimeChangeRecord(historyLogPump)
            is DateChangeHistoryLog            -> historyLog.subObject = createDateChangeRecord(historyLogPump)

            // Bolus - WIP
            is BolusCompletedHistoryLog        -> historyLog.subObject = createBolusRecord(historyLogPump)
            is BolexCompletedHistoryLog        -> historyLog.subObject = createBolusRecord(historyLogPump)
            is BolexActivatedHistoryLog        -> historyLog.subObject = createBolusRecord(historyLogPump)
            is BolusActivatedHistoryLog        -> historyLog.subObject = createBolusRecord(historyLogPump)

            // Pump Status Changes - WIP
            is PumpingResumedHistoryLog        -> historyLog.subObject = PumpStatusChanged(PumpStatusType.PumpRunning)
            is PumpingSuspendedHistoryLog      -> historyLog.subObject = PumpStatusChanged(PumpStatusType.PumpSuspended, historyLogPump.reason)

            // TBR
            is TempRateActivatedHistoryLog     -> historyLog.subObject = createTBRRecord(historyLogPump)
            is TempRateCompletedHistoryLog     -> historyLog.subObject = createTBRRecord(historyLogPump)

            // Alarm/Alert
            is AlarmActivatedHistoryLog        -> historyLog.subObject = createAlarmRecord(historyLogPump)
            is AlertActivatedHistoryLog        -> historyLog.subObject = createAlertRecord(historyLogPump)


            // not implemented yet

            // Bolus
            is BolusDeliveryHistoryLog,
            //is BolexActivatedHistoryLog,
            //is BolusActivatedHistoryLog,
            is CarbEnteredHistoryLog,

            // Bolus Delivery
            is BolusRequestedMsg1HistoryLog,
            is BolusRequestedMsg2HistoryLog,
            is BolusRequestedMsg3HistoryLog,

            // Basal
            is BasalDeliveryHistoryLog,
            is BasalRateChangeHistoryLog,



            // Configuration Changes
            is ParamChangeGlobalSettingsHistoryLog,
            is ParamChangePumpSettingsHistoryLog,

            // Pump Status Changes
            is CannulaFilledHistoryLog,
            is CartridgeFilledHistoryLog,
            is TubingFilledHistoryLog,




            is  CorrectionDeclinedHistoryLog,
            is  DailyBasalHistoryLog,
            is  DataLogCorruptionHistoryLog,
            is  FactoryResetHistoryLog,
            is  HypoMinimizerResumeHistoryLog,
            is  HypoMinimizerSuspendHistoryLog,


            is  LogErasedHistoryLog,
            is  NewDayHistoryLog,



            //is  ,
            is  UnknownHistoryLog,





            // not supported (and won't be supported)
                    -> historyLog.subObject = null
            else                               -> historyLog.subObject = null
        }



        return if (historyLog.subObject==null) null else historyLog

    }


    private fun isLogTypeSupported(historyLogPump: HistoryLog): Boolean {

        when (historyLogPump) {
            is IdpActionHistoryLog,
            is IdpActionMsg2HistoryLog,
            is IdpBolusHistoryLog,
            is IdpListHistoryLog,
            is IdpTimeDependentSegmentHistoryLog,
            is UsbConnectedHistoryLog,
            is UsbDisconnectedHistoryLog,
            is UsbEnumeratedHistoryLog,
            is CgmCalibrationGxHistoryLog,
            is CgmCalibrationHistoryLog,
            is CgmDataGxHistoryLog,
            is CgmDataSampleHistoryLog,
            is CGMHistoryLog,
            is ParamChangeReminderHistoryLog,
            is ParamChangeRemSettingsHistoryLog,
            is ControlIQPcmChangeHistoryLog,
            is ControlIQUserModeChangeHistoryLog,
            is BGHistoryLog                             -> return false

            else                                        -> return true
        }

    }


    private fun createBolusRecord(bolusLog: BolexCompletedHistoryLog): HistoryLogObject {

        val bolus = Bolus(bolusId = bolusLog.bolusId,
                          immediateAmount = bolusLog.insulinDelivered.toDouble(),
                          isCancelled = false,
                          isRunning = bolusLog.completionStatus == 0  // TODO completionStatus Bolus
        )



        return bolus

    }

    private fun createBolusRecord(historyLogPump: BolusActivatedHistoryLog): HistoryLogObject? {
        TODO("Not yet implemented")
    }

    private fun createBolusRecord(historyLogPump: BolexActivatedHistoryLog): HistoryLogObject? {
        TODO("Not yet implemented")
    }


    private fun createTBRRecord(historyLogPump: TempRateActivatedHistoryLog): HistoryLogObject? {
        val temporaryBasal = TemporaryBasal(
                    percent = historyLogPump.percent.toInt(),
                    minutes = historyLogPump.duration.toInt(),
                    isRunning = true,
                    tempRateId = historyLogPump.tempRateId
                )
        tbrMap.put(historyLogPump.tempRateId, temporaryBasal)

        return temporaryBasal
    }


    private fun createTBRRecord(historyLogPump: TempRateCompletedHistoryLog): HistoryLogObject? {
        // TODO("Not yet implemented")
        //return TemporaryBasal(historyLogPump.)




        historyLogPump.tempRateId
        historyLogPump.timeLeft

        //TemporaryBasal tbr =

        return null
    }






    private fun createBolusRecord(bolusLog: BolusCompletedHistoryLog): HistoryLogObject {

        val bolus = Bolus(bolusId = bolusLog.bolusId,
                          immediateAmount = bolusLog.insulinDelivered.toDouble(),
                          isCancelled = false,
                          isRunning = bolusLog.completionStatus == 0  // TODO completionStatus Bolus
        )

        return bolus

    }


    private fun createAlertRecord(historyLogPump: AlertActivatedHistoryLog): HistoryLogObject? {
        TODO("Not yet implemented")
    }

    private fun createAlarmRecord(historyLogPump: AlarmActivatedHistoryLog): HistoryLogObject? {
        TODO("Not yet implemented")
    }



    private fun createDateChangeRecord(historyLogPump: DateChangeHistoryLog): HistoryLogObject {
        return createDateTimeChangeRecord(historyLogPump.getDateAfterInstant().toEpochMilli(), false)
    }

    // private fun createDateChangeRecord(historyLogPump: DateChangeResponse): HistoryLogObject {
    //     return createDateTimeChangeRecord(Dates.fromJan12008EpochDaysToDate(historyLogPump.dateAfter).toEpochMilli(), false)
    // }

    private fun createTimeChangeRecord(historyLogPump: TimeChangedHistoryLog): HistoryLogObject {
        return createDateTimeChangeRecord(Dates.fromJan12008EpochDaysToDate(historyLogPump.timeAfter).toEpochMilli(), true)
    }

    private fun createDateTimeChangeRecord(dateTime: Long, timeChanged: Boolean): DateTimeChanged {
        val dt = DateTime().withMillis(dateTime)
        return DateTimeChanged(year = dt.year, month = dt.monthOfYear, day = dt.dayOfMonth,
                               hour = dt.hourOfDay, minute = dt.minuteOfHour, second = dt.secondOfMinute,
                               timeChanged = timeChanged)
    }

    fun createHistoryLogDto(historyLogPump: HistoryLog) : HistoryLogDto {
        return HistoryLogDto(id= null,
                             serial = pumpStatus.serialNumber,
                             historyTypeIndex = historyLogPump.typeId(),
                             historyType = TandemPumpHistoryType.getByCode(historyLogPump.typeId()),
                             dateTimeMillis = historyLogPump.pumpTimeSecInstant.toEpochMilli(),
                             sequenceNum = historyLogPump.sequenceNum,
                             payload = ByteUtil.getCompactString(historyLogPump.cargo),
                             subObject = null)
    }



}