package info.nightscout.androidaps.plugins.pump.common.driver.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.plugins.pump.common.data.BasalProfileDto
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.FirmwareVersionInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.parameters.PumpHistoryFilterInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.*
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.common.data.PumpTimeDifferenceDto
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.AdditionalResponseDataInterface

import info.nightscout.shared.logging.AAPSLogger

abstract class PumpConnectorAbstract(protected var injector: HasAndroidInjector,
                                     protected var aapsLogger: AAPSLogger) : PumpConnectorInterface {

    var unSuccessfulResponse =
        ResultCommandResponse(PumpCommandType.GetBolus, false, "Command not implemented.")

    var unSuccessfulResponseForSet =
        DataCommandResponse<AdditionalResponseDataInterface?>(PumpCommandType.SetBolus, false, "Command not implemented.", null)

    var unSucessfulDataResponse =
        DataCommandResponse<FirmwareVersionInterface?>(PumpCommandType.GetFirmwareVersion,
                                                                                 false, "Command not implemented.", null)

    override fun connectToPump(): Boolean {
        return false
    }

    override fun disconnectFromPump(): Boolean {
        return false
    }

    override fun retrieveFirmwareVersion(): DataCommandResponse<FirmwareVersionInterface?> {
        return DataCommandResponse<FirmwareVersionInterface?>(PumpCommandType.GetFirmwareVersion,
                                                       false, "Command not implemented.", null)
    }

    override fun sendBolus(detailedBolusInfo: DetailedBolusInfo): DataCommandResponse<AdditionalResponseDataInterface?> {
        return unSuccessfulResponseForSet.cloneWithNewCommandType(PumpCommandType.SetBolus)
    }

    override fun cancelBolus(): DataCommandResponse<AdditionalResponseDataInterface?> {
        return unSuccessfulResponseForSet.cloneWithNewCommandType(PumpCommandType.CancelBolus)
    }

    override fun retrieveTemporaryBasal(): DataCommandResponse<TempBasalPair?> {
        return DataCommandResponse<TempBasalPair?>(
            PumpCommandType.GetTemporaryBasal, false, "Command not implemented.", null)
    }

    override fun sendTemporaryBasal(value: Int, duration: Int): DataCommandResponse<AdditionalResponseDataInterface?> {
        return unSuccessfulResponseForSet.cloneWithNewCommandType(PumpCommandType.SetTemporaryBasal)
    }

    override fun cancelTemporaryBasal(): DataCommandResponse<AdditionalResponseDataInterface?> {
        return unSuccessfulResponseForSet.cloneWithNewCommandType(PumpCommandType.CancelTemporaryBasal)
    }

    override fun retrieveBasalProfile(): DataCommandResponse<BasalProfileDto?> {
        return DataCommandResponse<BasalProfileDto?>(
            PumpCommandType.GetBasalProfile, false, "Command not implemented.", null)
    }

    override fun sendBasalProfile(profile: Profile): DataCommandResponse<AdditionalResponseDataInterface?> {
        return unSuccessfulResponseForSet.cloneWithNewCommandType(PumpCommandType.SetBasalProfile)
    }


    override fun retrieveConfiguration(): DataCommandResponse<Map<String, String>?> {
        return DataCommandResponse<Map<String, String>?>(
            PumpCommandType.GetSettings, false, "Command not implemented.", null)
    }

    override fun retrieveRemainingInsulin(): DataCommandResponse<Double?> {
        return DataCommandResponse<Double?>(
            PumpCommandType.GetRemainingInsulin, false, "Command not implemented.", null)
    }

    override fun retrieveBatteryStatus(): DataCommandResponse<Int?> {
        return DataCommandResponse<Int?>(PumpCommandType.GetBatteryStatus,
                                         false, "Command not implemented.", null)
    }

    override fun getTime(): DataCommandResponse<PumpTimeDifferenceDto?> {
        return DataCommandResponse<PumpTimeDifferenceDto?>(
            PumpCommandType.GetTime, false, "Command not implemented.", null)
    }

    override fun setTime(): DataCommandResponse<AdditionalResponseDataInterface?> {
        return unSuccessfulResponseForSet.cloneWithNewCommandType(PumpCommandType.SetTime)
    }

    override fun getPumpHistory(): DataCommandResponse<List<Any>?> {
        return DataCommandResponse<List<Any>?>(
            PumpCommandType.GetHistory, false, "Command not implemented.", null)
    }

    override fun getFilteredPumpHistory(filter: PumpHistoryFilterInterface): DataCommandResponse<List<Any>?> {
        return DataCommandResponse<List<Any>?>(
            PumpCommandType.GetHistoryWithParameters,false, "Command not implemented.", null)
    }


















}