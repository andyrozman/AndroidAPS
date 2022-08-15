package info.nightscout.androidaps.plugins.pump.tandem.connector

import android.content.Context
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.BasalLimitSettingsRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.ControlIQInfoV1Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.ControlIQInfoV2Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.CurrentBatteryV1Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.CurrentBatteryV2Request
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.BasalLimitSettingsResponse
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus
import info.nightscout.androidaps.plugins.pump.common.driver.connector.PumpConnectorAbstract
import info.nightscout.androidaps.plugins.pump.common.driver.connector.PumpDummyConnector
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.FirmwareVersionInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.DataCommandResponse
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.common.util.PumpUtil
import info.nightscout.androidaps.plugins.pump.tandem.comm.TandemCommunicationManager
import info.nightscout.androidaps.plugins.pump.tandem.comm.TandemDataConverter
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpApiVersion
import info.nightscout.androidaps.plugins.pump.tandem.driver.TandemPumpStatus
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpConst
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.sharedPreferences.SP
import javax.inject.Inject

/**
 * TODO
 * All commands that will be supported need to be implemented here (look at PumpConnectorInterface), and they also need
 * to be added to supportedCommandsList.
 *
 * Any command will be used from TandemPumpConnectionManager, if its not used there, then it doesn't need to be
 * implemented.
 *
 *
 */
class TandemPumpConnector @Inject constructor(pumpStatus: TandemPumpStatus,
                                              var context: Context,
                                              var tandemPumpUtil: TandemPumpUtil,
                                              injector: HasAndroidInjector,
                                              var sp: SP,
                                              aapsLogger: AAPSLogger,
                                              var tandemDataConverter: TandemDataConverter
): PumpDummyConnector(pumpStatus, tandemPumpUtil, injector, aapsLogger) {

    var tandemCommunicationManager: TandemCommunicationManager? = null
    var btAddressUsed: String? = null
    var tandemPumpApiVersion: TandemPumpApiVersion = TandemPumpApiVersion.VERSION_2_0

    fun getCommunicationManager(): TandemCommunicationManager {
        return tandemCommunicationManager!!
    }


    override fun connectToPump(): Boolean {
        var createInstance: Boolean = false
        var newBtAddress = sp.getStringOrNull(TandemPumpConst.Prefs.PumpAddress, null)

        if (!btAddressUsed.isNullOrEmpty()) {
            if (btAddressUsed.equals(newBtAddress)) {
                newBtAddress = null
            }
        } else {
            if (newBtAddress.isNullOrEmpty()) {
                return false;
            }
        }

        if (!newBtAddress.isNullOrEmpty()) {
            tandemCommunicationManager = TandemCommunicationManager(
                context = context,
                aapsLogger = aapsLogger,
                sp = sp,
                pumpUtil = tandemPumpUtil,
                btAddress = newBtAddress
            )
            this.btAddressUsed = newBtAddress
        }

        return getCommunicationManager().connect()

    }


    override fun disconnectFromPump(): Boolean {
        pumpUtil.sleepSeconds(10)
        tandemCommunicationManager!!.disconnect()
        return true
    }


    override fun retrieveFirmwareVersion(): DataCommandResponse<FirmwareVersionInterface?> {
        val version = sp.getStringOrNull(TandemPumpConst.Prefs.PumpApiVersion, null)

        if (version!=null) {
            this.tandemPumpApiVersion = TandemPumpApiVersion.valueOf(version)
        } else {
            this.tandemPumpApiVersion = TandemPumpApiVersion.VERSION_2_0
        }

        return DataCommandResponse(
            PumpCommandType.GetFirmwareVersion, true, null, this.tandemPumpApiVersion)
    }


    override fun retrieveConfiguration(): DataCommandResponse<Map<String,String>?> {
        return DataCommandResponse(
            PumpCommandType.GetSettings, true, null, mapOf())

        var map:  MutableMap<String,String> = mutableMapOf()

        // TODO check the versions
        if (tandemPumpApiVersion.isSameVersion(TandemPumpApiVersion.VERSION_2_2_OR_HIGHER)) {
            addToSettings(map, getCommunicationManager().sendCommand(CurrentBatteryV2Request()))
            addToSettings(map, getCommunicationManager().sendCommand(ControlIQInfoV2Request()))
        } else {
            addToSettings(map, getCommunicationManager().sendCommand(CurrentBatteryV1Request()))
            addToSettings(map, getCommunicationManager().sendCommand(ControlIQInfoV1Request()))
        }


        addToSettings(map, getCommunicationManager().sendCommand(BasalLimitSettingsRequest()))

    }

    fun addToSettings(settingsMap: Map<String,String>, message: Message?) {

        // TODO add to Settings


    }


    override fun retrieveBatteryStatus(): DataCommandResponse<Int?> {

        var responseMessage: Message? = null

        // TODO check the versions
        if (tandemPumpApiVersion.isSameVersion(TandemPumpApiVersion.VERSION_2_2_OR_HIGHER)) {
            responseMessage = getCommunicationManager().sendCommand(CurrentBatteryV2Request())
        } else {
            responseMessage = getCommunicationManager().sendCommand(CurrentBatteryV1Request())
        }

        if (responseMessage==null) {
            return DataCommandResponse(
                PumpCommandType.GetBatteryStatus, false, "Error communicating with pump", 0)
        } else {
            return tandemDataConverter.convertMessage(responseMessage) as DataCommandResponse<Int?>
        }

    }


    init {
        supportedCommandsList = setOf(PumpCommandType.GetFirmwareVersion)
    }

    override fun getSupportedCommands(): Set<PumpCommandType> {
        return supportedCommandsList
    }

}