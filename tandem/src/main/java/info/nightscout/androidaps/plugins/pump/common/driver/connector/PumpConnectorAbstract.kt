package info.nightscout.androidaps.plugins.pump.common.driver.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.FirmwareVersionInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.PumpHistoryEntryInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.parameters.PumpHistoryFilterInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.*
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.common.data.DateTimeDto

import info.nightscout.shared.logging.AAPSLogger

abstract class PumpConnectorAbstract(protected var injector: HasAndroidInjector,
                                     protected var aapsLogger: AAPSLogger) : PumpConnectorInterface {

    var unSuccessfulResponse =
        ResultCommandResponse(PumpCommandType.GetBolus, false, "Command not implemented.")

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

    override fun sendBolus(detailedBolusInfo: DetailedBolusInfo): ResultCommandResponse {
        return unSuccessfulResponse.cloneWithNewCommandType(PumpCommandType.SetBolus)
    }

    override fun cancelBolus(): ResultCommandResponse {
        return unSuccessfulResponse.cloneWithNewCommandType(PumpCommandType.CancelBolus)
    }

    override fun retrieveTemporaryBasal(): DataCommandResponse<TempBasalPair?> {
        return DataCommandResponse<TempBasalPair?>(
            PumpCommandType.GetTemporaryBasal, false, "Command not implemented.", null)
    }

    override fun sendTemporaryBasal(value: Int, duration: Int): ResultCommandResponse {
        return unSuccessfulResponse.cloneWithNewCommandType(PumpCommandType.SetTemporaryBasal)
    }

    override fun cancelTemporaryBasal(): ResultCommandResponse {
        return unSuccessfulResponse.cloneWithNewCommandType(PumpCommandType.CancelTemporaryBasal)
    }

    override fun retrieveBasalProfile(): DataCommandResponse<DoubleArray?> {
        return DataCommandResponse<DoubleArray?>(
            PumpCommandType.GetBasalProfile, false, "Command not implemented.", null)
    }

    override fun sendBasalProfile(profile: Profile): ResultCommandResponse {
        return unSuccessfulResponse.cloneWithNewCommandType(PumpCommandType.SetBasalProfile)
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

    override fun getTime(): DataCommandResponse<DateTimeDto?> {
        return DataCommandResponse<DateTimeDto?>(
            PumpCommandType.GetTime, false, "Command not implemented.", null)
    }

    override fun setTime(): ResultCommandResponse {
        return unSuccessfulResponse.cloneWithNewCommandType(PumpCommandType.SetTime)
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