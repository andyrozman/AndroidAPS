package app.aaps.pump.common.driver.connector

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.profile.Profile
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.pump.common.data.BasalProfileDto
import app.aaps.pump.common.driver.connector.commands.data.FirmwareVersionInterface
import app.aaps.pump.common.driver.connector.commands.parameters.PumpHistoryFilterInterface
import app.aaps.pump.common.driver.connector.defs.PumpCommandType
import app.aaps.pump.common.defs.PumpConfigurationTypeInterface
import app.aaps.pump.common.driver.connector.commands.data.AdditionalResponseDataInterface
import app.aaps.pump.common.driver.connector.commands.response.DataCommandResponse
import app.aaps.pump.common.driver.connector.commands.response.ResultCommandResponse
import app.aaps.pump.common.data.PumpTimeDifferenceDto
import app.aaps.pump.common.defs.BolusData
import app.aaps.pump.common.defs.TempBasalPair


abstract class PumpConnectorAbstract(protected var aapsLogger: AAPSLogger
) : PumpConnectorInterface {

    var unSuccessfulResponse =
        ResultCommandResponse(PumpCommandType.GetBolus, false, "Command not implemented.")

    var unSuccessfulResponseForSet =
        DataCommandResponse<AdditionalResponseDataInterface?>(PumpCommandType.SetBolus, false, "Command not implemented.", null)

    var unSuccessfulBooleanResponseForSet =
        DataCommandResponse<Boolean?>(PumpCommandType.SetBolus, false, "Command not implemented.", false)

    var unSucessfulDataResponse =
        DataCommandResponse<FirmwareVersionInterface?>(
            PumpCommandType.GetFirmwareVersion,
            false, "Command not implemented.", null)

    override fun connectToPump(): Boolean {
        return false
    }

    override fun disconnectFromPump(): Boolean {
        return false
    }

    override fun retrieveFirmwareVersion(): DataCommandResponse<FirmwareVersionInterface?> {
        return DataCommandResponse<FirmwareVersionInterface?>(
            PumpCommandType.GetFirmwareVersion,
            false, "Command not implemented.", null)
    }

    override fun sendBolus(detailedBolusInfo: DetailedBolusInfo): DataCommandResponse<BolusData?> {
        //return unSuccessfulResponseForSet.cloneWithNewCommandType(PumpCommandType.SetBolus)
        return DataCommandResponse(
            PumpCommandType.SetTemporaryBasal, false, "Dummy Bolus",
            null
        )
    }

    override fun cancelBolus(bolusData: BolusData?): DataCommandResponse<AdditionalResponseDataInterface?> {
        return unSuccessfulResponseForSet.cloneWithNewCommandType(PumpCommandType.CancelBolus)
    }

    override fun retrieveTemporaryBasal(): DataCommandResponse<TempBasalPair?> {
        return DataCommandResponse<TempBasalPair?>(
            PumpCommandType.GetTemporaryBasal, false, "Command not implemented.", null)
    }

    override fun sendTemporaryBasal(value: Int, duration: Int): DataCommandResponse<TempBasalPair?> {
        return DataCommandResponse(
            PumpCommandType.SetTemporaryBasal, false, "Dummy TBR",
            TempBasalPair(value.toDouble(), true, duration, System.currentTimeMillis())
        )
    }

    override fun cancelTemporaryBasal(): DataCommandResponse<AdditionalResponseDataInterface?> {
        return unSuccessfulResponseForSet.cloneWithNewCommandType(PumpCommandType.CancelTemporaryBasal)
    }

    override fun retrieveBasalProfile(): DataCommandResponse<BasalProfileDto?> {
        return DataCommandResponse<BasalProfileDto?>(
            PumpCommandType.GetBasalProfile, false, "Command not implemented.", null)
    }

    override fun sendBasalProfile(profile: Profile): DataCommandResponse<Boolean?> {
        return DataCommandResponse<Boolean?>(
            PumpCommandType.GetBasalProfile, false, "Command not implemented.", false)
        // return unSuccessfulResponseForSet.cloneWithNewCommandType(PumpCommandType.SetBasalProfile)
    }


    override fun retrieveConfiguration(): DataCommandResponse<MutableMap<PumpConfigurationTypeInterface, Any>?> {
        return DataCommandResponse<MutableMap<PumpConfigurationTypeInterface, Any>?>(
            PumpCommandType.GetSettings, false, "Command not implemented.", null)
    }

    override fun retrieveRemainingInsulin(): DataCommandResponse<Double?> {
        return DataCommandResponse<Double?>(
            PumpCommandType.GetRemainingInsulin, false, "Command not implemented.", null)
    }

    override fun retrieveBatteryStatus(): DataCommandResponse<Int?> {
        return DataCommandResponse<Int?>(
            PumpCommandType.GetBatteryStatus,
            false, "Command not implemented.", null)
    }

    override fun getTime(): DataCommandResponse<PumpTimeDifferenceDto?> {
        return DataCommandResponse<PumpTimeDifferenceDto?>(
            PumpCommandType.GetTime, false, "Command not implemented.", null)
    }

    override fun setTime(): DataCommandResponse<Boolean?> {
        return unSuccessfulBooleanResponseForSet.cloneWithNewCommandType(PumpCommandType.SetTime)
    }

    override fun getPumpHistory(): DataCommandResponse<List<Any>?> {
        return DataCommandResponse<List<Any>?>(
            PumpCommandType.GetHistory, false, "Command not implemented.", null)
    }

    override fun getFilteredPumpHistory(filter: PumpHistoryFilterInterface): DataCommandResponse<List<Any>?> {
        return DataCommandResponse<List<Any>?>(
            PumpCommandType.GetHistoryWithParameters, false, "Command not implemented.", null)
    }


















}