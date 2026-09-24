package app.aaps.pump.common.driver.connector

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.profile.Profile
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.pump.common.data.BasalProfileDto
import app.aaps.pump.common.driver.connector.commands.parameters.PumpHistoryFilterInterface
import app.aaps.pump.common.driver.connector.commands.response.ResultCommandResponse
import app.aaps.pump.common.driver.connector.commands.response.DataCommandResponse
import app.aaps.pump.common.driver.connector.defs.PumpCommandType
import app.aaps.pump.common.defs.PumpConfigurationTypeInterface
import app.aaps.pump.common.driver.connector.commands.data.AdditionalResponseDataInterface
import app.aaps.pump.common.driver.connector.commands.data.CustomCommandTypeInterface
import app.aaps.pump.common.data.PumpStatus
import app.aaps.pump.common.data.PumpTimeDifferenceDto
import app.aaps.pump.common.defs.BolusData
import app.aaps.pump.common.defs.TempBasalPair
import app.aaps.pump.common.utils.PumpUtil
import org.joda.time.DateTime



open class PumpDummyConnector(var pumpStatus: PumpStatus,
                              var pumpUtil: PumpUtil,
                              aapsLogger: AAPSLogger
) : PumpConnectorAbstract(aapsLogger) {

    // var pumpStatus: YpsopumpPumpStatus? = null // ???

    var successfulResponse =
        ResultCommandResponse(PumpCommandType.GetBolus, true, null)

    var successfulResponseForSet =
        DataCommandResponse<AdditionalResponseDataInterface?>(PumpCommandType.SetBolus, true, null, null)

    var successfulBooleanResponseForSet =
        DataCommandResponse<Boolean?>(PumpCommandType.SetBolus, true, null, true)


    var supportedCommandsList: Set<PumpCommandType>



    override fun getSupportedCommands(): Set<PumpCommandType> {
        return supportedCommandsList
    }


    override fun connectToPump(): Boolean {
        pumpUtil.sleepSeconds(10)
        return true
    }

    override fun disconnectFromPump(): Boolean {
        pumpUtil.sleepSeconds(10)
        return true
    }

    // override fun retrieveFirmwareVersion(): DataCommandResponse<FirmwareVersionInterface?> {
    //     return DataCommandResponse(
    //         PumpCommandType.GetFirmwareVersion, true, null, TandemPumpApiVersion.VERSION_2_1)
    // }

    override fun sendBolus(detailedBolusInfo: DetailedBolusInfo): DataCommandResponse<BolusData?> {
        pumpUtil.sleepSeconds(10)
        //return successfulResponseForSet.cloneWithNewCommandType(PumpCommandType.SetBolus)

        return DataCommandResponse(
            PumpCommandType.SetBolus, true, null, BolusData(amountImmediateDelivered = 1.0,
                                                            timestamp = System.currentTimeMillis())
        )
    }

    override fun getBolus(): DataCommandResponse<BolusData?> {
        pumpUtil.sleepSeconds(10)

        return DataCommandResponse(
            PumpCommandType.GetBolus, true, null, BolusData(amountImmediateDelivered = 1.0,
                                                            timestamp = System.currentTimeMillis())
        )
    }

    override fun retrieveTemporaryBasal(): DataCommandResponse<TempBasalPair?> {
        pumpUtil.sleepSeconds(10)

        return if (pumpStatus.currentTempBasal == null ||
                System.currentTimeMillis() > pumpStatus.currentTempBasalEstimatedEnd!!
            ) {
                // tbr not set
                DataCommandResponse(
                    PumpCommandType.GetTemporaryBasal, true, null, TempBasalPair(0.0, true, 0)
                )
            } else {
                DataCommandResponse(
                    PumpCommandType.GetTemporaryBasal, true, null, pumpStatus.currentTempBasal
                )
            }
    }


    override fun sendTemporaryBasal(value: Int, duration: Int): DataCommandResponse<TempBasalPair?> {
        pumpUtil.sleepSeconds(10)

        val tempBasalPair = TempBasalPair(insulinRate = value.toDouble(),
                                          isPercent = true,
                                          start = System.currentTimeMillis(),
                                          durationMinutes = duration)

        return DataCommandResponse(commandType = PumpCommandType.SetTemporaryBasal,
                                   success = true,
                                   errorDescription = null,
                                   value = tempBasalPair)
    }

    override fun cancelTemporaryBasal(): DataCommandResponse<AdditionalResponseDataInterface?> {
        pumpUtil.sleepSeconds(10)
        return successfulResponseForSet.cloneWithNewCommandType(PumpCommandType.CancelTemporaryBasal)
    }

    override fun retrieveBasalProfile(): DataCommandResponse<BasalProfileDto?> {
        pumpUtil.sleepSeconds(10)
        return if (pumpStatus.basalsByHour==null) {
            DataCommandResponse(
                PumpCommandType.GetBasalProfile, true, null, null)
        } else {
            DataCommandResponse(
                PumpCommandType.GetBasalProfile, true, null, BasalProfileDto(pumpStatus.basalsByHour)
            )
        }
    }

    override fun sendBasalProfile(profile: Profile): DataCommandResponse<Boolean?> {
        pumpUtil.sleepSeconds(10)
        // return successfulResponseForSet.cloneWithNewCommandType(PumpCommandType.SetBasalProfile)
        return DataCommandResponse(
            PumpCommandType.SetBasalProfile, true, null, true)
    }

    override fun retrieveRemainingInsulin(): DataCommandResponse<Double?> {
        return DataCommandResponse(
            PumpCommandType.GetRemainingInsulin, true, null, 100.0)
    }

    override fun retrieveConfiguration(): DataCommandResponse<MutableMap<PumpConfigurationTypeInterface, Any>?> {
        return DataCommandResponse(
            PumpCommandType.GetSettings, true, null, mutableMapOf())
    }

    override fun retrieveBatteryStatus(): DataCommandResponse<Int?> {
        return DataCommandResponse(
            PumpCommandType.GetBatteryStatus, true, null, 75)
    }

    override fun getPumpHistory(): DataCommandResponse<List<Any>?> {
        return DataCommandResponse(
            PumpCommandType.GetHistory, true, null, listOf())
    }

    override fun cancelBolus(bolusData: BolusData?): DataCommandResponse<AdditionalResponseDataInterface?> {
        return successfulResponseForSet.cloneWithNewCommandType(PumpCommandType.CancelBolus)
    }

    override fun getTime(): DataCommandResponse<PumpTimeDifferenceDto?> {
        return DataCommandResponse(
            PumpCommandType.GetTime, true, null, PumpTimeDifferenceDto(DateTime.now(), DateTime.now())
        )
    }

    // override fun getPumpStatus(): DataCommandResponse<AdditionalResponseDataInterface?> {
    //     return DataCommandResponse(
    //         PumpCommandType.GetPumpStatus, true, null, null)
    //
    // }


    override fun setTime(): DataCommandResponse<Boolean?> {
        return successfulBooleanResponseForSet.cloneWithNewCommandType(PumpCommandType.SetTime)
    }

    override fun getFilteredPumpHistory(filter: PumpHistoryFilterInterface): DataCommandResponse<List<Any>?> {
        return DataCommandResponse(
            PumpCommandType.GetHistory, true, null, listOf())
    }

    override fun getPumpStatus(): DataCommandResponse<AdditionalResponseDataInterface?> {
        return successfulResponseForSet.cloneWithNewCommandType(PumpCommandType.GetPumpStatus)
    }

    override fun executeCustomCommand(commandType: CustomCommandTypeInterface, data: Any?): DataCommandResponse<AdditionalResponseDataInterface?> {
        return successfulResponseForSet.cloneWithNewCommandType(PumpCommandType.CustomCommand)
    }

    init {
        supportedCommandsList = setOf()
    }
}