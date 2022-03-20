package info.nightscout.androidaps.plugins.pump.ypsopump.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.BasalProfileResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.MapDataCommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.SimpleDataCommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.TemporaryBasalResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.shared.logging.AAPSLogger

abstract class YpsoPumpConnectorAbstract(protected var pumpUtil: YpsoPumpUtil,
                                         protected var injector: HasAndroidInjector,
                                         protected var aapsLogger: AAPSLogger) : YpsoPumpConnectorInterface {

    var unSuccessfulResponse = CommandResponse.builder().success(false).errorDescription("Command not implemented.").build()

    override fun retrieveBatteryStatus(): SimpleDataCommandResponse? {
        return SimpleDataCommandResponse.builder().success(false).errorDescription("Command not implemented.").build()
    }

    override fun connectToPump(): Boolean {
        pumpUtil.sleepSeconds(10)
        return true
    }

    override fun disconnectFromPump() {}


    override fun retrieveFirmwareVersion(): YpsoPumpFirmware {
        return YpsoPumpFirmware.VERSION_1_0
    }

    override fun sendBolus(detailedBolusInfo: DetailedBolusInfo): CommandResponse? {
        return unSuccessfulResponse
    }

    override fun retrieveTemporaryBasal(): TemporaryBasalResponse? {
        return null
    }

    override fun sendTemporaryBasal(value: Int, duration: Int): CommandResponse? {
        return unSuccessfulResponse
    }

    override fun retrieveBasalProfile(): BasalProfileResponse? {
        return null
    }

    override fun sendBasalProfile(profile: Profile): CommandResponse? {
        return unSuccessfulResponse
    }

    override fun cancelTemporaryBasal(): CommandResponse? {
        return unSuccessfulResponse
    }

    override fun retrieveConfiguration(): MapDataCommandResponse {
        return MapDataCommandResponse.builder().success(false).errorDescription("Command not implemented.").build()
    }

    override fun retrieveRemainingInsulin(): SimpleDataCommandResponse? {
        return SimpleDataCommandResponse.builder().success(false).errorDescription("Command not implemented.").build()
    }

    override fun getPumpHistory(): Unit {

    }
}