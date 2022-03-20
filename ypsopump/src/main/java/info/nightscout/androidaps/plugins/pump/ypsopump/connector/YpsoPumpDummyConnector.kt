package info.nightscout.androidaps.plugins.pump.ypsopump.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.BasalProfileResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.TemporaryBasalResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import javax.inject.Singleton

@Singleton
class YpsoPumpDummyConnector(var pumpStatus: YpsopumpPumpStatus,
                             ypsopumpUtil: YpsoPumpUtil,
                             injector: HasAndroidInjector,
                             aapsLogger: AAPSLogger) : YpsoPumpConnectorAbstract(ypsopumpUtil, injector, aapsLogger) {

    // var pumpStatus: YpsopumpPumpStatus? = null // ???

    override fun connectToPump(): Boolean {
        pumpUtil.sleepSeconds(10)
        return true
    }

    override fun disconnectFromPump() {
        pumpUtil.sleepSeconds(10)
    }

    override fun retrieveFirmwareVersion(): YpsoPumpFirmware {
        return YpsoPumpFirmware.VERSION_1_0
    }

    override fun sendBolus(detailedBolusInfo: DetailedBolusInfo): CommandResponse {
        pumpUtil.sleepSeconds(10)
        return CommandResponse.builder().success(true).build()
    }

    override fun retrieveTemporaryBasal(): TemporaryBasalResponse {
        pumpUtil.sleepSeconds(10)

        return if (pumpStatus.tempBasalEnd == null || System.currentTimeMillis() > pumpStatus.tempBasalEnd!!) {
            return TemporaryBasalResponse.builder().success(true).tempBasalPair(TempBasalPair(0.0, true, 0)).build()
        } else {
            val tempBasalPair = TempBasalPair()
            tempBasalPair.insulinRate = pumpStatus.tempBasalPercent.toDouble()
            val diff = pumpStatus.tempBasalStart!! - System.currentTimeMillis()
            val diffMin = (diff / (1000 * 60)).toInt()
            tempBasalPair.durationMinutes = pumpStatus.tempBasalDuration - diffMin
            TemporaryBasalResponse.builder().success(true).tempBasalPair(tempBasalPair).build()
        }
    }

    override fun sendTemporaryBasal(value: Int, duration: Int): CommandResponse {
        pumpUtil.sleepSeconds(10)
        return CommandResponse.builder().success(true).build()
    }

    override fun cancelTemporaryBasal(): CommandResponse {
        pumpUtil.sleepSeconds(10)
        return CommandResponse.builder().success(true).build()
    }

    override fun retrieveBasalProfile(): BasalProfileResponse {
        pumpUtil.sleepSeconds(10)
        return BasalProfileResponse.builder().success(true).basalProfile(null).build()
    }

    override fun sendBasalProfile(profile: Profile): CommandResponse {
        pumpUtil.sleepSeconds(10)
        return CommandResponse.builder().success(true).build()
    }

    // override fun retrieveRemainingInsulin(): SimpleDataCommandResponse {
    //     return null
    // }
    //
    // override fun retrieveConfiguration(): MapDataCommandResponse {
    //     return null
    // }
    //
    // override fun retrieveBatteryStatus(): SimpleDataCommandResponse {
    //     return null
    // }

    override fun getPumpHistory() {}

    init {
        pumpStatus = ypsopumpUtil.ypsopumpPumpStatus
    }
}