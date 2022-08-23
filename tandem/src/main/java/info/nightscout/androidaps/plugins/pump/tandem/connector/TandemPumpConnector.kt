package info.nightscout.androidaps.plugins.pump.tandem.connector

import android.content.Context
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.*
import com.jwoglom.pumpx2.pump.messages.response.ErrorResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.*
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.plugins.pump.common.data.PumpTimeDifferenceDto
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.driver.connector.PumpDummyConnector
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.FirmwareVersionInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.parameters.PumpHistoryFilterInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.DataCommandResponse
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.ResultCommandResponse
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
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
import java.util.*
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

    var TAG = LTag.PUMPCOMM

    // TODO Better Error response handling

    fun getCommunicationManager(): TandemCommunicationManager {
        return tandemCommunicationManager!!
    }


    override fun connectToPump(): Boolean {
        var newBtAddress = sp.getStringOrNull(TandemPumpConst.Prefs.PumpAddress, null)

        aapsLogger.info(TAG, "TANDEMDBG: connectToPump with ${newBtAddress}")

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
        aapsLogger.info(TAG, "TANDEMDBG: disconnectFromPump")

        getCommunicationManager().disconnect()
        return true
    }


    override fun retrieveFirmwareVersion(): DataCommandResponse<FirmwareVersionInterface?> {
        val version = sp.getStringOrNull(TandemPumpConst.Prefs.PumpApiVersion, null)

        aapsLogger.info(TAG, "TANDEMDBG: retrieveFirmwareVersion ${version}")

        if (version!=null) {
            this.tandemPumpApiVersion = TandemPumpApiVersion.valueOf(version)
        } else {
            this.tandemPumpApiVersion = TandemPumpApiVersion.VERSION_2_1
        }

        return DataCommandResponse(
            PumpCommandType.GetFirmwareVersion, true, null, this.tandemPumpApiVersion)
    }


    override fun retrieveConfiguration(): DataCommandResponse<Map<String,String>?> {

        // TODO
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

    private fun addToSettings(settingsMap: MutableMap<String,String>, message: Message?) {

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
            null -> {

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

        val responseData: DataCommandResponse<Int?> = sendAndReceivePumpData(PumpCommandType.GetBatteryStatus,
                                                                             getCorrectRequest(TandemCommandType.CurrentBattery))
        {   rawData -> tandemDataConverter.getBatteryResponse(rawData as CurrentBatteryAbstractResponse) }

        if (responseData.isSuccess) {
            pumpStatus.batteryRemaining = responseData.value!!
        }

        return responseData

    }


    // override fun retrieveRemainingInsulin(): DataCommandResponse<Double?> {
    //
    //     val responseMessage: Message? = getCommunicationManager().sendCommand(InsulinStatusRequest())
    //
    //     if (responseMessage==null || responseMessage is ErrorResponse) {
    //         return DataCommandResponse(
    //             PumpCommandType.GetRemainingInsulin, false, "Error communicating with pump.", 0.0)
    //     } else {
    //         val response = tandemDataConverter.convertMessage(responseMessage) as DataCommandResponse<Double?>
    //         pumpStatus.reservoirRemainingUnits = response.value!!
    //
    //         return response
    //     }
    // }

    override fun getPumpHistory(): DataCommandResponse<List<Any>?> {
        // TODO Connector: getPumpHistory
        return DataCommandResponse<List<Any>?>(
            PumpCommandType.GetHistory, false, "Command not implemented.", null)
    }


    override fun sendBolus(detailedBolusInfo: DetailedBolusInfo): ResultCommandResponse {
        // TODO Connector: sendBolus
        return super.sendBolus(detailedBolusInfo)
    }

    override fun retrieveTemporaryBasal(): DataCommandResponse<TempBasalPair?> {

        val responseData: DataCommandResponse<TempBasalPair?> = sendAndReceivePumpData(PumpCommandType.GetTemporaryBasal,
                                                                                TempRateRequest())
        {  rawContent -> tandemDataConverter.getTempBasalRate(rawContent as TempRateResponse) }

        if (responseData.isSuccess) {
            val tbr = responseData.value!!
            if (tbr.isActive) {
                pumpStatus.currentTempBasal = tbr
            }
        }

        return responseData
    }

    override fun sendTemporaryBasal(value: Int, duration: Int): ResultCommandResponse {
        // TODO Connector: sendTemporaryBasal
        return super.sendTemporaryBasal(value, duration)
    }

    override fun cancelTemporaryBasal(): ResultCommandResponse {
        // TODO Connector: cancelTemporaryBasal
        return super.cancelTemporaryBasal()
    }

    override fun retrieveBasalProfile(): DataCommandResponse<DoubleArray?> {
        // TODO Connector: retrieveBasalProfile
        //return super.retrieveBasalProfile()

        // 1. get ProfileStatusRequest, read idpslot0 value
        // 2. get IDPSettingsRequest(with that value)
        // 3. read all segments

        var responseMessage: Message? = getCommunicationManager().sendCommand(ProfileStatusRequest())

        var responseText = checkResponse(responseMessage, "ProfileStatusRequest")

        if (responseText!=null) {
            return DataCommandResponse<DoubleArray?>(PumpCommandType.SetBasalProfile, false, responseText, null)
        }

        val profileStatusResponse = responseMessage as ProfileStatusResponse

        val idpId = profileStatusResponse.idpSlot0Id

        responseMessage = getCommunicationManager().sendCommand(IDPSettingsRequest(idpId))

        responseText = checkResponse(responseMessage, "ProfileStatusRequest")

        if (responseText!=null) {
            return DataCommandResponse<DoubleArray?>(PumpCommandType.SetBasalProfile, false, responseText, null)
        }

        val settings = responseMessage as IDPSettingsResponse
        for (i in 1..settings.numberOfProfileSegments) {
            //queue(IDPSegmentRequest(settings.idpId, i))
        }

        // TODO

        // if (responseMessage!=null && responseMessage is ProfileStatusResponse) {
        //     aapsLogger.info(TAG, "TANDEMDBG: sendAndReceivePumpData for ${commandType}  - Request message: ${requestMessage.javaClass.name}")
        // } else {
        //     var ressponseText = if (responseMessage==null) {
        //         "Response was null (timeout or some other problem), when getting ProfileStatusRequest"
        //     } else {
        //         var errorResponse: ErrorResponse = responseMessage as ErrorResponse
        //         "Received Error Response: ${errorResponse.errorCode.name} on ProfileStatusRequest."
        //     }
        //     aapsLogger.info(TAG, "TANDEMDBG: ${ressponseText}")
        //
        //     return DataCommandResponse<DoubleArray?>(PumpCommandType.SetBasalProfile, false, ressponseText, null)
        //
        // }


        return super.retrieveBasalProfile()


    }

    private fun checkResponse(responseMessage: Message?, description: String): String? {
        return if (responseMessage==null || responseMessage is ErrorResponse) {
            val ressponseText = if (responseMessage==null) {
                "Response was null (timeout or some other problem), when getting ${description}"
            } else {
                val errorResponse: ErrorResponse = responseMessage as ErrorResponse
                "Received Error Response: ${errorResponse.errorCode.name} on ${description}."
            }
            aapsLogger.info(TAG, "TANDEMDBG: ${ressponseText}")

            ressponseText
        } else {
            null
        }
    }



    override fun sendBasalProfile(profile: Profile): ResultCommandResponse {
        // TODO Connector: sendBasalProfile
        return super.sendBasalProfile(profile)
    }

    override fun cancelBolus(): ResultCommandResponse {
        // TODO Connector: cancelBolus
        return super.cancelBolus()
    }

    override fun getTime(): DataCommandResponse<PumpTimeDifferenceDto?> {
        return DataCommandResponse(
            PumpCommandType.GetTime, true, null, pumpStatus.pumpTime)
    }

    override fun setTime(): ResultCommandResponse {
        // TODO Connector: setTime
        return super.setTime()
    }

    override fun getFilteredPumpHistory(filter: PumpHistoryFilterInterface): DataCommandResponse<List<Any>?> {
        // TODO Connector: getFilteredPumpHistory
        return DataCommandResponse(
            PumpCommandType.GetHistory, true, null, listOf())
    }


    override fun retrieveRemainingInsulin(): DataCommandResponse<Double?> {

        val responseData: DataCommandResponse<Double?> = sendAndReceivePumpData(PumpCommandType.GetRemainingInsulin,
                                                                                InsulinStatusRequest())
        {  rawContent -> tandemDataConverter.getInsulinStatus(rawContent as InsulinStatusResponse) }

        return responseData
    }



    private inline fun <reified T>  sendAndReceivePumpData(commandType: PumpCommandType,
                                                           requestMessage: Message,
                                                           decode: (responseMessage: Message) -> T
    ): T {

        aapsLogger.info(TAG, "TANDEMDBG: sendAndReceivePumpData for ${commandType}  - Request message: ${requestMessage.javaClass.name}")

        val responseMessage: Message? = getCommunicationManager().sendCommand(requestMessage)

        aapsLogger.info(TAG, "TANDEMDBG: sendAndReceivePumpData: Response: ${responseMessage}")

        return if (responseMessage==null) {
            DataCommandResponse(
                commandType, false, "Error communicating with pump (timeout).", null) as T
        } else if (responseMessage is ErrorResponse) {
            DataCommandResponse(
                commandType, false, "Error communicating with pump. Error: ${responseMessage.errorCode.name}", null) as T
        } else {
            val response = decode(responseMessage)

            aapsLogger.info(TAG, "TANDEMDBG: sendAndReceivePumpData: decoded Response: ${response}")

            response
        }
    }


    init {
        supportedCommandsList = setOf(PumpCommandType.GetFirmwareVersion)
    }

    override fun getSupportedCommands(): Set<PumpCommandType> {
        return supportedCommandsList
    }






}