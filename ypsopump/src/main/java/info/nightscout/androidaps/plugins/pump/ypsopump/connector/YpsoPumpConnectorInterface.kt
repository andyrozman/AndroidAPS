package info.nightscout.androidaps.plugins.pump.ypsopump.connector

import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.BasalProfileResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.MapDataCommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.SimpleDataCommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.TemporaryBasalResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware

interface YpsoPumpConnectorInterface {

    fun connectToPump(): Boolean
    fun disconnectFromPump()
    fun retrieveFirmwareVersion(): YpsoPumpFirmware?
    fun sendBolus(detailedBolusInfo: DetailedBolusInfo): CommandResponse?
    fun retrieveTemporaryBasal(): TemporaryBasalResponse?
    fun sendTemporaryBasal(value: Int, duration: Int): CommandResponse?
    fun cancelTemporaryBasal(): CommandResponse?
    fun retrieveBasalProfile(): BasalProfileResponse?
    fun sendBasalProfile(profile: Profile): CommandResponse?
    fun retrieveRemainingInsulin(): SimpleDataCommandResponse?
    fun retrieveConfiguration(): MapDataCommandResponse?
    fun retrieveBatteryStatus(): SimpleDataCommandResponse?
    fun getPumpHistory(): Unit
}