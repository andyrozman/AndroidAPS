package info.nightscout.androidaps.plugins.pump.common.driver.connector

import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.FirmwareVersionInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.PumpHistoryEntryInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.parameters.PumpHistoryFilterInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.*
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.common.data.DateTimeDto

interface PumpConnectorInterface {

    fun connectToPump(): Boolean
    fun disconnectFromPump(): Boolean

    fun retrieveFirmwareVersion(): DataCommandResponse<FirmwareVersionInterface?>

    fun sendBolus(detailedBolusInfo: DetailedBolusInfo): ResultCommandResponse
    fun cancelBolus(): ResultCommandResponse

    fun retrieveTemporaryBasal(): DataCommandResponse<TempBasalPair?>
    fun sendTemporaryBasal(value: Int, duration: Int): ResultCommandResponse
    fun cancelTemporaryBasal(): ResultCommandResponse

    fun retrieveBasalProfile(): DataCommandResponse<DoubleArray?>
    fun sendBasalProfile(profile: Profile): ResultCommandResponse

    fun retrieveConfiguration(): DataCommandResponse<Map<String, String>?>
    fun retrieveRemainingInsulin(): DataCommandResponse<Double?>
    fun retrieveBatteryStatus(): DataCommandResponse<Int?>

    fun getTime(): DataCommandResponse<DateTimeDto?>
    fun setTime(): ResultCommandResponse?

    fun getPumpHistory(): DataCommandResponse<List<PumpHistoryEntryInterface>?>
    fun getFilteredPumpHistory(filter: PumpHistoryFilterInterface): DataCommandResponse<List<PumpHistoryEntryInterface>?>

    fun getSupportedCommands(): Set<PumpCommandType>
}