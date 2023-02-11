package info.nightscout.pump.common.driver.connector

import info.nightscout.androidaps.plugins.pump.common.data.BasalProfileDto
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.FirmwareVersionInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.parameters.PumpHistoryFilterInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.*
import info.nightscout.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.common.defs.PumpConfigurationTypeInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.AdditionalResponseDataInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.CustomCommandTypeInterface
import info.nightscout.interfaces.profile.Profile
import info.nightscout.interfaces.pump.DetailedBolusInfo
import info.nightscout.pump.common.data.PumpTimeDifferenceDto
import info.nightscout.pump.common.defs.TempBasalPair

interface PumpConnectorInterface {

    fun connectToPump(): Boolean
    fun disconnectFromPump(): Boolean

    fun retrieveFirmwareVersion(): DataCommandResponse<FirmwareVersionInterface?>

    fun sendBolus(detailedBolusInfo: DetailedBolusInfo): DataCommandResponse<AdditionalResponseDataInterface?>  //  ResultCommandResponse
    fun cancelBolus(): DataCommandResponse<AdditionalResponseDataInterface?>  //ResultCommandResponse

    fun retrieveTemporaryBasal(): DataCommandResponse<TempBasalPair?>
    fun sendTemporaryBasal(value: Int, duration: Int): DataCommandResponse<AdditionalResponseDataInterface?>  //ResultCommandResponse
    fun cancelTemporaryBasal(): DataCommandResponse<AdditionalResponseDataInterface?>  //ResultCommandResponse

    fun retrieveBasalProfile(): DataCommandResponse<BasalProfileDto?>
    fun sendBasalProfile(profile: Profile): DataCommandResponse<AdditionalResponseDataInterface?>  //ResultCommandResponse

    fun retrieveConfiguration(): DataCommandResponse<MutableMap<PumpConfigurationTypeInterface, Any>?>
    fun retrieveRemainingInsulin(): DataCommandResponse<Double?>
    fun retrieveBatteryStatus(): DataCommandResponse<Int?>

    fun getTime(): DataCommandResponse<PumpTimeDifferenceDto?>
    fun setTime(): DataCommandResponse<AdditionalResponseDataInterface?>

    fun getPumpHistory(): DataCommandResponse<List<Any>?>
    fun getFilteredPumpHistory(filter: PumpHistoryFilterInterface): DataCommandResponse<List<Any>?>

    fun executeCustomCommand(commandType: CustomCommandTypeInterface): DataCommandResponse<AdditionalResponseDataInterface?>

    fun getSupportedCommands(): Set<PumpCommandType>
}