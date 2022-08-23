package info.nightscout.androidaps.plugins.pump.common.driver.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.FirmwareVersionInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.PumpHistoryEntryInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.parameters.PumpHistoryFilterInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.ResultCommandResponse
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.DataCommandResponse
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.common.util.PumpUtil
import info.nightscout.androidaps.plugins.pump.common.data.DateTimeDto
import info.nightscout.androidaps.plugins.pump.common.data.PumpTimeDifferenceDto
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpApiVersion
import info.nightscout.shared.logging.AAPSLogger
import org.joda.time.DateTime
import java.util.*
import javax.inject.Singleton

@Singleton
open class PumpDummyConnector(var pumpStatus: PumpStatus,
                              var pumpUtil: PumpUtil,
                              injector: HasAndroidInjector,
                              aapsLogger: AAPSLogger) : PumpConnectorAbstract(injector, aapsLogger) {

    // var pumpStatus: YpsopumpPumpStatus? = null // ???

    var successfulResponse =
        ResultCommandResponse(PumpCommandType.GetBolus, true, null)

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

    override fun retrieveFirmwareVersion(): DataCommandResponse<FirmwareVersionInterface?> {
        return DataCommandResponse(
            PumpCommandType.GetFirmwareVersion, true, null, TandemPumpApiVersion.VERSION_2_1)
    }

    override fun sendBolus(detailedBolusInfo: DetailedBolusInfo): ResultCommandResponse {
        pumpUtil.sleepSeconds(10)
        return successfulResponse.cloneWithNewCommandType(PumpCommandType.SetBolus)
    }

    override fun retrieveTemporaryBasal(): DataCommandResponse<TempBasalPair?> {
        pumpUtil.sleepSeconds(10)

        return if (pumpStatus.tempBasalStart == null ||
            pumpStatus.tempBasalEnd == null ||
            System.currentTimeMillis() > pumpStatus.tempBasalEnd!!) {
            DataCommandResponse(
                PumpCommandType.GetTemporaryBasal, true, null, TempBasalPair(0.0, true, 0))
        } else {
            val tempBasalPair = TempBasalPair()
            tempBasalPair.insulinRate = pumpStatus.tempBasalAmount!!
            val diff = pumpStatus.tempBasalStart!! - System.currentTimeMillis()
            val diffMin = (diff / (1000 * 60)).toInt()
            tempBasalPair.durationMinutes = (pumpStatus.tempBasalDuration!! - diffMin)

            DataCommandResponse(
                PumpCommandType.GetTemporaryBasal, true, null, tempBasalPair)
        }
    }

    override fun sendTemporaryBasal(value: Int, duration: Int): ResultCommandResponse {
        pumpUtil.sleepSeconds(10)
        return successfulResponse.cloneWithNewCommandType(PumpCommandType.SetTemporaryBasal)
    }

    override fun cancelTemporaryBasal(): ResultCommandResponse {
        pumpUtil.sleepSeconds(10)
        return successfulResponse.cloneWithNewCommandType(PumpCommandType.CancelTemporaryBasal)
    }

    override fun retrieveBasalProfile(): DataCommandResponse<DoubleArray?> {
        pumpUtil.sleepSeconds(10)
        return if (pumpStatus.basalsByHour==null) {
            DataCommandResponse(
                PumpCommandType.GetBasalProfile, true, null, null)
        } else {
            DataCommandResponse(
                PumpCommandType.GetBasalProfile, true, null, pumpStatus.basalsByHour)
        }
    }

    override fun sendBasalProfile(profile: Profile): ResultCommandResponse {
        pumpUtil.sleepSeconds(10)
        return successfulResponse.cloneWithNewCommandType(PumpCommandType.SetBasalProfile)
    }

    override fun retrieveRemainingInsulin(): DataCommandResponse<Double?> {
        return DataCommandResponse(
            PumpCommandType.GetRemainingInsulin, true, null, 100.0)
    }

    override fun retrieveConfiguration(): DataCommandResponse<Map<String,String>?> {
        return DataCommandResponse(
            PumpCommandType.GetSettings, true, null, mapOf())
    }

    override fun retrieveBatteryStatus(): DataCommandResponse<Int?> {
        return DataCommandResponse(
            PumpCommandType.GetBatteryStatus, true, null, 75)
    }

    override fun getPumpHistory(): DataCommandResponse<List<Any>?> {
        return DataCommandResponse(
            PumpCommandType.GetHistory, true, null, listOf())
    }

    override fun cancelBolus(): ResultCommandResponse {
        return successfulResponse.cloneWithNewCommandType(PumpCommandType.CancelBolus)
    }

    override fun getTime(): DataCommandResponse<PumpTimeDifferenceDto?> {
        return DataCommandResponse(
            PumpCommandType.GetTime, true, null, PumpTimeDifferenceDto(DateTime.now(), DateTime.now())
        )
    }

    override fun setTime(): ResultCommandResponse {
        return successfulResponse.cloneWithNewCommandType(PumpCommandType.SetTime)
    }

    override fun getFilteredPumpHistory(filter: PumpHistoryFilterInterface): DataCommandResponse<List<Any>?> {
        return DataCommandResponse(
            PumpCommandType.GetHistory, true, null, listOf())
    }


    init {
        supportedCommandsList = setOf()
    }
}