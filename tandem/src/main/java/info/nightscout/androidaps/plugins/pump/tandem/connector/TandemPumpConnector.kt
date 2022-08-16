package info.nightscout.androidaps.plugins.pump.tandem.connector

import android.content.Context
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.*
import com.jwoglom.pumpx2.pump.messages.response.ErrorResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.BasalLimitSettingsResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ControlIQInfoV1Response
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ControlIQInfoV2Response
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.GlobalMaxBolusSettingsResponse
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
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemCommandType
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpApiVersion
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpSettingType
import info.nightscout.androidaps.plugins.pump.tandem.driver.TandemPumpStatus
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpConst
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
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
class TandemPumpConnector @Inject constructor(var tandemPumpStatus: TandemPumpStatus,
                                              var context: Context,
                                              var tandemPumpUtil: TandemPumpUtil,
                                              //var pumpStatus: TandemPumpStatus,
                                              injector: HasAndroidInjector,
                                              var sp: SP,
                                              aapsLogger: AAPSLogger,
                                              var tandemDataConverter: TandemDataConverter
): PumpDummyConnector(tandemPumpStatus, tandemPumpUtil, injector, aapsLogger) {

    var tandemCommunicationManager: TandemCommunicationManager? = null
    var btAddressUsed: String? = null
    var tandemPumpApiVersion: TandemPumpApiVersion = TandemPumpApiVersion.VERSION_2_1

    // TODO Better Error response handling

    fun getCommunicationManager(): TandemCommunicationManager {
        return tandemCommunicationManager!!
    }


    override fun connectToPump(): Boolean {
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
                pumpStatus = tandemPumpStatus,
                btAddress = newBtAddress
            )
            this.btAddressUsed = newBtAddress
        }

        return getCommunicationManager().connect()

    }


    override fun disconnectFromPump(): Boolean {
        getCommunicationManager().disconnect()
        return true
    }


    override fun retrieveFirmwareVersion(): DataCommandResponse<FirmwareVersionInterface?> {
        val version = sp.getStringOrNull(TandemPumpConst.Prefs.PumpApiVersion, null)

        if (version!=null) {
            this.tandemPumpApiVersion = TandemPumpApiVersion.valueOf(version)
        } else {
            this.tandemPumpApiVersion = TandemPumpApiVersion.VERSION_2_1
        }

        return DataCommandResponse(
            PumpCommandType.GetFirmwareVersion, true, null, this.tandemPumpApiVersion)
    }


    override fun retrieveConfiguration(): DataCommandResponse<Map<String,String>?> {

        val map:  MutableMap<String,String> = mutableMapOf()

        try {
            addToSettings(map, getCommunicationManager().sendCommand(getCorrectRequest(TandemCommandType.ControlIQInfo)))
            addToSettings(map, getCommunicationManager().sendCommand(BasalLimitSettingsRequest()))
            addToSettings(map, getCommunicationManager().sendCommand(GlobalMaxBolusSettingsRequest()))

            // TODO postProcessConfiguration()

            return DataCommandResponse(
                PumpCommandType.GetSettings, true, null, map
            )
        } catch(ex: Exception) {
            return DataCommandResponse(
                PumpCommandType.GetSettings, false, "Problem with reading some data: Ex: " + ex.toString(), map)
        }
    }

    fun addToSettings(settingsMap: MutableMap<String,String>, message: Message?) {

        // TODO add to Settings

        when(message) {
            is ControlIQInfoV1Response -> {  settingsMap.put(TandemPumpSettingType.CONTROL_IQ_ENABLED.name, message.closedLoopEnabled.toString()) }
            is ControlIQInfoV2Response -> {  settingsMap.put(TandemPumpSettingType.CONTROL_IQ_ENABLED.name, message.closedLoopEnabled.toString()) }
            is BasalLimitSettingsResponse -> {  settingsMap.put(TandemPumpSettingType.BASAL_LIMIT.name, message.basalLimit.toString())  }
            is GlobalMaxBolusSettingsResponse -> {  settingsMap.put(TandemPumpSettingType.MAX_BOLUS.name, message.maxBolus.toString()) }
            is ErrorResponse -> {
                aapsLogger.error(LTag.PUMPBTCOMM, "Problem with packets from Tandem: requestedCodeId=${message.requestCodeId}, errorCode: ${message.errorCode}")
                throw Exception("Problem with packets from Tandem: requestedCodeId=${message.requestCodeId}, errorCode: ${message.errorCode}")
            }
        }




    }


    private fun getCorrectRequest(command: TandemCommandType): Message {
        return when(tandemPumpApiVersion) {
            TandemPumpApiVersion.VERSION_0_x,
            TandemPumpApiVersion.VERSION_1_x,
            TandemPumpApiVersion.VERSION_2_0,
            TandemPumpApiVersion.VERSION_2_1,
            TandemPumpApiVersion.VERSION_2_2,
            TandemPumpApiVersion.VERSION_2_3,
            TandemPumpApiVersion.VERSION_2_4           -> {
                when(command) {
                    TandemCommandType.ControlIQInfo  -> ControlIQInfoV1Request()
                    TandemCommandType.CurrentBattery -> CurrentBatteryV1Request()
                }
            }

            TandemPumpApiVersion.VERSION_2_5_OR_HIGHER -> {
                when(command) {
                    TandemCommandType.ControlIQInfo  -> ControlIQInfoV2Request()
                    TandemCommandType.CurrentBattery -> CurrentBatteryV2Request()
                }
            }
            else -> throw Exception()
        }
    }


    override fun retrieveBatteryStatus(): DataCommandResponse<Int?> {

        val responseMessage: Message? = getCommunicationManager().sendCommand(getCorrectRequest(TandemCommandType.CurrentBattery))

        if (responseMessage==null || responseMessage is ErrorResponse) {
            return DataCommandResponse(
                PumpCommandType.GetBatteryStatus, false, "Error communicating with pump", 0)
        } else {
            val response = tandemDataConverter.convertMessage(responseMessage) as DataCommandResponse<Int?>
            pumpStatus.batteryRemaining = response.value!!

            return response
        }
    }


    override fun retrieveRemainingInsulin(): DataCommandResponse<Double?> {

        val responseMessage: Message? = getCommunicationManager().sendCommand(InsulinStatusRequest())

        if (responseMessage==null || responseMessage is ErrorResponse) {
            return DataCommandResponse(
                PumpCommandType.GetRemainingInsulin, false, "Error communicating with pump.", 0.0)
        } else {
            val response = tandemDataConverter.convertMessage(responseMessage) as DataCommandResponse<Double?>
            pumpStatus.reservoirRemainingUnits = response.value!!

            return response
        }
    }


    init {
        supportedCommandsList = setOf(PumpCommandType.GetFirmwareVersion)
    }

    override fun getSupportedCommands(): Set<PumpCommandType> {
        return supportedCommandsList
    }

}