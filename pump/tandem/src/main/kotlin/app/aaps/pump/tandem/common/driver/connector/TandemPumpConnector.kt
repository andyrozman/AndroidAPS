package app.aaps.pump.tandem.common.driver.connector

import android.content.Context
import app.aaps.core.data.model.BS
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.profile.Profile
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.pump.BolusProgressData
// import app.aaps.core.interfaces.rx.events.EventOverviewBolusProgress
// import app.aaps.core.interfaces.rx.events.EventOverviewBolusStopDeliveryEnabled
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.pump.common.data.BasalProfileDto
import app.aaps.pump.common.data.PumpTimeDifferenceDto
import app.aaps.pump.common.defs.BolusData
import app.aaps.pump.common.defs.BolusType
import app.aaps.pump.common.defs.PumpConfigurationTypeInterface
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.common.defs.PumpUpdateFragmentType
import app.aaps.pump.common.defs.TempBasalPair
import app.aaps.pump.common.driver.connector.PumpDummyConnector
import app.aaps.pump.common.driver.connector.commands.data.AdditionalResponseDataInterface
import app.aaps.pump.common.driver.connector.commands.data.CustomCommandTypeInterface
import app.aaps.pump.common.driver.connector.commands.data.FirmwareVersionInterface
import app.aaps.pump.common.driver.connector.commands.parameters.PumpHistoryFilterInterface
import app.aaps.pump.common.driver.connector.commands.response.DataCommandResponse
import app.aaps.pump.common.driver.connector.defs.PumpCommandType
import app.aaps.pump.common.events.EventPumpFragmentValuesChanged
import app.aaps.pump.common.R as Rpc
import app.aaps.pump.tandem.common.comm.TandemPumpCommunicationManager
import app.aaps.pump.tandem.common.comm.TandemDataConverter
import app.aaps.pump.tandem.common.comm.maint.TandemConnectionFixer
import app.aaps.pump.tandem.common.data.IDPSegmentDto
import app.aaps.pump.tandem.common.data.PumpProfileDto
import app.aaps.pump.tandem.common.data.defs.QuickBolusType
import app.aaps.pump.tandem.common.data.defs.TandemCommandType
import app.aaps.pump.tandem.common.data.defs.TandemNotificationType
import app.aaps.pump.tandem.common.data.defs.TandemPumpApiVersion
import app.aaps.pump.tandem.common.data.defs.TandemPumpSettingType
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.def.TandemCustomCommand.*
import app.aaps.pump.tandem.common.driver.connector.def.ControlCommandResponse
import app.aaps.pump.tandem.common.driver.connector.def.TandemCustomCommand
import app.aaps.pump.tandem.common.driver.connector.response.AlarmStatusDto
import app.aaps.pump.tandem.common.driver.connector.response.AlertStatusDto
import app.aaps.pump.tandem.common.driver.connector.response.HomeScreenMirrorDto
import app.aaps.pump.tandem.common.driver.tandemDataStore
import app.aaps.pump.tandem.common.driver.connector.response.MalfunctionStatusDto
import app.aaps.pump.tandem.common.driver.connector.response.PumpGlobalsDto
import app.aaps.pump.tandem.common.driver.connector.response.PumpVersionDto
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.tandem.common.util.PumpX2L
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import com.jwoglom.pumpx2.pump.PumpState
import com.jwoglom.pumpx2.pump.bluetooth.TandemConfig
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.models.InsulinUnit
import com.jwoglom.pumpx2.pump.messages.models.NotificationBundle
import com.jwoglom.pumpx2.pump.messages.models.PairingCodeType
import com.jwoglom.pumpx2.pump.messages.models.StatusMessage
import com.jwoglom.pumpx2.pump.messages.request.control.BolusPermissionRequest
import com.jwoglom.pumpx2.pump.messages.request.control.CancelBolusRequest
import com.jwoglom.pumpx2.pump.messages.request.control.ChangeControlIQSettingsRequest
import com.jwoglom.pumpx2.pump.messages.request.control.ChangeTimeDateRequest
import com.jwoglom.pumpx2.pump.messages.request.control.CreateIDPRequest
import com.jwoglom.pumpx2.pump.messages.request.control.DismissNotificationRequest
import com.jwoglom.pumpx2.pump.messages.request.control.InitiateBolusRequest
import com.jwoglom.pumpx2.pump.messages.request.control.SetIDPSegmentRequest
import com.jwoglom.pumpx2.pump.messages.request.control.SetMaxBasalLimitRequest
import com.jwoglom.pumpx2.pump.messages.request.control.SetMaxBolusLimitRequest
import com.jwoglom.pumpx2.pump.messages.request.control.SetPumpSoundsRequest
import com.jwoglom.pumpx2.pump.messages.request.control.SetQuickBolusSettingsRequest
import com.jwoglom.pumpx2.pump.messages.request.control.SetTempRateRequest
import com.jwoglom.pumpx2.pump.messages.request.control.StopTempRateRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlarmStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlertStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.BasalLimitSettingsRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.ControlIQInfoV1Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.ControlIQInfoV2Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.CurrentBatteryV1Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.CurrentBatteryV2Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.CurrentBolusStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.GlobalMaxBolusSettingsRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.HomeScreenMirrorRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.IDPSegmentRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.IDPSettingsRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.InsulinStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.LastBolusStatusV2Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.MalfunctionStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.ProfileStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.PumpFeaturesV1Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.PumpFeaturesV2Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.PumpGlobalsRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.PumpVersionRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.TempRateRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.TimeSinceResetRequest
import com.jwoglom.pumpx2.pump.messages.response.ErrorResponse
import com.jwoglom.pumpx2.pump.messages.response.control.BolusPermissionResponse
import com.jwoglom.pumpx2.pump.messages.response.control.CancelBolusResponse
import com.jwoglom.pumpx2.pump.messages.response.control.ChangeControlIQSettingsResponse
import com.jwoglom.pumpx2.pump.messages.response.control.CreateIDPResponse
import com.jwoglom.pumpx2.pump.messages.response.control.DismissNotificationResponse
import com.jwoglom.pumpx2.pump.messages.response.control.InitiateBolusResponse
import com.jwoglom.pumpx2.pump.messages.response.control.SetIDPSegmentResponse
import com.jwoglom.pumpx2.pump.messages.response.control.SetMaxBasalLimitResponse
import com.jwoglom.pumpx2.pump.messages.response.control.SetMaxBolusLimitResponse
import com.jwoglom.pumpx2.pump.messages.response.control.SetPumpSoundsResponse
import com.jwoglom.pumpx2.pump.messages.response.control.SetQuickBolusSettingsResponse
import com.jwoglom.pumpx2.pump.messages.response.control.SetTempRateResponse
import com.jwoglom.pumpx2.pump.messages.response.control.StopTempRateResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.AlarmStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.AlertStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.BasalIQStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.BasalLimitSettingsResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ControlIQInfoV1Response
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ControlIQInfoV2Response
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CurrentBatteryAbstractResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CurrentBolusStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.GlobalMaxBolusSettingsResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HomeScreenMirrorResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.IDPSegmentResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.IDPSegmentResponse.IDPSegmentStatus
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.IDPSettingsResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.InsulinStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.LastBolusStatusV2Response
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.MalfunctionBitmaskStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ProfileStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpFeaturesV1Response
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpFeaturesV2Response
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpGlobalsResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TempRateResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TimeSinceResetResponse
import com.jwoglom.pumpx2.pump.messages.response.historyLog.BolusDeliveryHistoryLog
import org.joda.time.DateTime
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton



/**
 * All commands that will be supported need to be implemented here (look at PumpConnectorInterface), and they also need
 * to be added to supportedCommandsList.
 *
 * Any command will be used from TandemPumpConnectionManager, if its not used there, then it doesn't need to be
 * implemented.
 */
@Singleton
class TandemPumpConnector @Inject constructor(var tandemPumpStatus: TandemPumpStatus,
                                              var context: Context,
                                              var tandemPumpUtil: TandemPumpUtil,
                                              var rxBus: RxBus,
                                              var resourceHelper: ResourceHelper,
                                              var preferences: Preferences,
                                              var sp: SP,
                                              var tandemConnectionFixer: TandemConnectionFixer,
                                              aapsLogger: AAPSLogger,
                                              val pumpX2L: PumpX2L,
                                              private var tandemDataConverter: TandemDataConverter,
                                              var bolusProgressData: BolusProgressData
): PumpDummyConnector(tandemPumpStatus, tandemPumpUtil, aapsLogger) {

    private var tandemPumpCommunicationManager: TandemPumpCommunicationManager? = null
    var btAddressUsed: String? = null

    private var TAG = LTag.PUMPCOMM

    // TODO Better Error response handling

    fun getCommunicationManager(): TandemPumpCommunicationManager? {
        return tandemPumpCommunicationManager
    }


    override fun connectToPump(): Boolean {
        var newBtAddress = tandemPumpUtil
            .getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.PumpAddress, null)
        //sp.getStringOrNull(TandemPumpConst.Prefs.PumpAddress, null)

        aapsLogger.info(TAG, "connectToPump with $newBtAddress")

        if (!btAddressUsed.isNullOrEmpty()) {
            if (btAddressUsed.equals(newBtAddress)) {
                newBtAddress = null
            }
        } else {
            if (newBtAddress.isNullOrEmpty()) {
                return false
            }
        }

        if (!newBtAddress.isNullOrEmpty()) {
            // Disconnect any existing connection before creating a new manager
            // This ensures we start fresh, especially important after pairing
            if (tandemPumpCommunicationManager != null) {
                aapsLogger.info(TAG, "Disconnecting existing connection before creating new manager for $newBtAddress")
                tandemPumpCommunicationManager!!.disconnect()
                tandemPumpCommunicationManager = null
                btAddressUsed = null
            }

            // TODO(jwoglom): AAPS should conditionally set this to allow for safer pump simulation
            PumpState.enableActionsAffectingInsulinDelivery()

            val cfg = TandemConfig()
                .withFilterToBluetoothMac(newBtAddress)
                // TODO(jwoglom): this should be configurable to allow for old firmware tslim x2 pump simulation
                .withPairingCodeType(PairingCodeType.SHORT_6CHAR)

            this.tandemPumpCommunicationManager = TandemPumpCommunicationManager(
                context = context,
                aapsLogger = aapsLogger,
                pumpUtil = tandemPumpUtil,
                pumpStatus = tandemPumpStatus,
                pumpConfig = cfg,
                rxBus = rxBus,
                preferences = preferences,
                resourceHelper = resourceHelper,
                timberTree = pumpX2L,
                tandemConnectionFixer = tandemConnectionFixer
            )
            this.btAddressUsed = newBtAddress
            tandemPumpStatus.pumpAddress = newBtAddress
        }

        return getCommunicationManager()!!.connect()

    }


    override fun disconnectFromPump(): Boolean {
        aapsLogger.info(TAG, "disconnectFromPump")

        val c = getCommunicationManager()
        if (c == null) {
            aapsLogger.warn(TAG, "disconnectFromPump: nothing to do, communication manager already destroyed")
            return false
        }
        val result = c.disconnect()
        // Release the cached manager/address so the next connect builds a fresh one.
        // Without this, a subsequent wizard-driven re-pair reuses the torn-down instance.
        tandemPumpCommunicationManager = null
        btAddressUsed = null
        return result
    }


    override fun retrieveFirmwareVersion(): DataCommandResponse<FirmwareVersionInterface?> {
        val version = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(
            TandemStringPreferenceKey.PumpApiVersion, null)

        aapsLogger.info(TAG, "retrieveFirmwareVersion result: ${version}")

        return DataCommandResponse(
            PumpCommandType.GetFirmwareVersion, true, null, this.tandemPumpStatus.tandemPumpFirmware)
    }


    override fun retrieveConfiguration(): DataCommandResponse<MutableMap<PumpConfigurationTypeInterface, Any>?> {

        aapsLogger.info(TAG, "retrieveConfiguration")

        val map:  MutableMap<PumpConfigurationTypeInterface, Any> = mutableMapOf()

        try {
            val comm = getCommunicationManager()
            if (comm == null) {
                return DataCommandResponse(
                    PumpCommandType.GetSettings, false, "Problem reading settings, no active communicationManager", map
                )
            }
            addToSettings(TandemPumpSettingType.CONTROL_IQ_ENABLED, map, comm.sendCommand(getCorrectRequest(TandemCommandType.ControlIQInfo)))
            addToSettings(TandemPumpSettingType.BASAL_LIMIT, map, comm.sendCommand(BasalLimitSettingsRequest()))
            addToSettings(TandemPumpSettingType.MAX_BOLUS, map, comm.sendCommand(GlobalMaxBolusSettingsRequest()))
            addToSettings(TandemPumpSettingType.QUICK_BOLUS, map, comm.sendCommand(PumpGlobalsRequest()))

            if (this.tandemPumpStatus.tandemPumpFirmware.isSameVersion(TandemPumpApiVersion.VERSION_2_1_to_2_4)) {
                if (tandemPumpStatus.featuresV1==null) {
                    addToSettings(TandemPumpSettingType.PUMP_FEATURES_1, map, comm.sendCommand(PumpFeaturesV1Request()))
                }
            } else {
                if (tandemPumpStatus.featuresV2==null) {
                    addToSettings(TandemPumpSettingType.PUMP_FEATURES_2, map, comm.sendCommand(PumpFeaturesV2Request()))
                }
            }

            val settings = tandemPumpUtil.gson.toJson(map)

            aapsLogger.info(TAG, "retrieveConfiguration result: ${settings}")

            return DataCommandResponse(
                PumpCommandType.GetSettings, true, null, map
            )
        } catch(ex: Exception) {
            return DataCommandResponse(
                PumpCommandType.GetSettings, false, "Problem reading settings. Ex: " + ex.toString(), map)
        }
    }


    private fun addToSettings(settingType: TandemPumpSettingType, settingsMap: MutableMap<PumpConfigurationTypeInterface, Any>, message: Message?) {

        when(message) {
            is ControlIQInfoV1Response -> {  settingsMap.put(settingType, message.closedLoopEnabled) }
            is ControlIQInfoV2Response -> {  settingsMap.put(settingType, message.closedLoopEnabled) }
            is BasalLimitSettingsResponse -> {  settingsMap.put(settingType, message.basalLimit)  }
            is GlobalMaxBolusSettingsResponse -> {  settingsMap.put(settingType, message.maxBolus) }
            is BasalIQStatusResponse  -> {  settingsMap.put(settingType, message.basalIQStatusState)  }
            is PumpGlobalsResponse    -> {  settingsMap.put(settingType, message) }
            is PumpFeaturesV2Response -> {
                settingsMap.put(settingType, pumpUtil.gson.toJson(message))
                tandemPumpStatus.featuresV2 = message
            }
            is PumpFeaturesV1Response -> {
                settingsMap.put(settingType, pumpUtil.gson.toJson(message))
                tandemPumpStatus.featuresV1 = message
            }
            is ErrorResponse          -> {
                aapsLogger.error(LTag.PUMPBTCOMM, "Problem with packets from Tandem: requestedCodeId=${message.requestCodeId}, errorCode: ${message.errorCode}")
                throw Exception("Problem with packets from Tandem: requestedCodeId=${message.requestCodeId}, errorCode: ${message.errorCode}")
            }
            null -> {
                aapsLogger.error(LTag.PUMPBTCOMM, "Received null response from Tandem for ${settingType.name}")
                throw Exception("Received null response from Tandem for ${settingType.name}")
            }
        }
    }


    override fun retrieveBatteryStatus(): DataCommandResponse<Int?> {

        aapsLogger.info(LTag.PUMPCOMM, "retrieveBatteryStatus")

        val responseData: DataCommandResponse<Int?> = sendAndReceivePumpData(
            PumpCommandType.GetBatteryStatus,
            getCorrectRequest(TandemCommandType.CurrentBattery))
        {   rawData -> tandemDataConverter.getBatteryResponse(rawData as CurrentBatteryAbstractResponse) }

        aapsLogger.info(TAG, "retrieveBatteryStatus result: ${responseData.value}")

        return responseData

    }


    override fun retrieveRemainingInsulin(): DataCommandResponse<Double?> {

        aapsLogger.info(LTag.PUMPCOMM, "retrieveRemainingInsulin")

        val responseData: DataCommandResponse<Double?> = sendAndReceivePumpData(
            PumpCommandType.GetRemainingInsulin,
            InsulinStatusRequest())
        {  rawContent -> tandemDataConverter.getInsulinStatus(rawContent as InsulinStatusResponse) }

        aapsLogger.info(TAG, "retrieveRemainingInsulin result: ${responseData.value}")

        return responseData
    }


    override fun getPumpHistory(): DataCommandResponse<List<Any>?> {
        // NOTE: pump history will be handled through HistoryRetriever
        return DataCommandResponse<List<Any>?>(
            PumpCommandType.GetHistory, false, "Command not implemented.", null)
    }


    var bolusId = 0
    var activeBolusData : BolusData? = null

    override fun sendBolus(detailedBolusInfo: DetailedBolusInfo): DataCommandResponse<BolusData?> {

        // TODO Bolus handling for current bolus seems to have changed, extend this method with activeBolus updating

        // 1. send BolusPermissionRequest(), get BolusPermissionResponse() which contains a bolusId
        // 2. send InitiateBolusRequest() with:
        //
        // @param totalVolume      the amount of insulin to be delivered, in milliunits. Use {@link com.jwoglom.pumpx2.pump.messages.models.InsulinUnit#from1To1000}.
        // * @param bolusID          the bolus ID returned from {@link com.jwoglom.pumpx2.pump.messages.response.control.BolusPermissionResponse}
        // * @param bolusTypeBitmask the bitmask of bolus type. Use {@link com.jwoglom.pumpx2.pump.messages.response.historyLog.BolusDeliveryHistoryLog.BolusType#toBitmask(BolusDeliveryHistoryLog.BolusType...)}
        // * @param foodVolume       the amount of insulin attributed within metadata from food (carbs; optional)
        // * @param correctionVolume the amount of insulin attributed within metadata from a correction (optional)
        // * @param bolusCarbs       the number of carbs attributed within metadata (optional)
        // * @param bolusBG          the current BG attributed within metadata, if overridden from the current CGM reading (optional)
        // * @param bolusIOB         the current IOB attributed within metadata (optional)
        //
        //
        // 3. get InitiateBolusResponse() which is a StatusMessage that if successful, means the bolus is starting to be delivered
        // 4. periodically send CurrentBolusStatusRequest() which returns CurrentBolusStatusResponse() and will contain a CurrentBolusStatus of REQUESTING for a bit until it then switches to DELIVERING
        // 5. when bolus status switches to ALREADY_DELIVERED_OR_INVALID, then you can call LastBolusStatusV2Request() and should see the same bolus id referenced and the amount which was delivered. if anything else goes wrong (like an occlusion) you'll get a pump alarm

        // bolusProgressData.start(insulin = detailedBolusInfo.insulin,
        //                         isSMB = detailedBolusInfo.bolusType == BS.Type.SMB,
        //                         isPriming = false)

        // TODO logs change or remove

        activeBolusData = BolusData(amountImmediateRequested = detailedBolusInfo.insulin,
                                    amountImmediateDelivered = 0.0,
                                    bolusType = if (detailedBolusInfo.bolusType==BS.Type.SMB) BolusType.NORMAL_SMB else BolusType.NORMAL
                                    )

        pumpStatus.activeBolusData = activeBolusData

        // EventOverviewBolusProgress.t = EventOverviewBolusProgress.Treatment(insulin = 0.0,
        //                                                                     carbs = 0,
        //                                                                     isSMB = detailedBolusInfo.bolusType === BS.Type.SMB,
        //                                                                     id = detailedBolusInfo.id)

        sendBolusEvent(bolusEvent = TandemBolusEvent.Initiating,
                       fullAmount = detailedBolusInfo.insulin,
                       id = detailedBolusInfo.id)

        val permissionResponseMessage: BolusPermissionResponse? = getCommunicationManager()?.sendCommand(
            BolusPermissionRequest()
        ) as BolusPermissionResponse?

        aapsLogger.debug(TAG, "BolusPermissionResponse: $permissionResponseMessage")

        if (permissionResponseMessage==null) {
            aapsLogger.error(TAG, "BolusPermissionResponse was not received." )

            return DataCommandResponse(
                PumpCommandType.SetBolus, false,
                "Error getting response from sending BolusPermissionResponse: null",
                null
            )
        }

        if (!permissionResponseMessage.isStatusOK) {
            aapsLogger.error(TAG, "BolusPermissionResponse status is not ok, with reason: ${permissionResponseMessage.nackReason.name}." )

            return DataCommandResponse(
                PumpCommandType.SetBolus, false,
                "BolusPermissionResponse status is not ok, with reason: ${permissionResponseMessage.nackReason.name}.",
                null
            )
        }

        bolusId = permissionResponseMessage.bolusId

        val volume = (detailedBolusInfo.insulin * 1000).toLong() // no decimals
        val bolusCarbs = 0

        val bolusRequest = InitiateBolusRequest(volume, permissionResponseMessage.bolusId,
                                                BolusDeliveryHistoryLog.BolusType.FOOD1.mask(),
                                                0, 0,
                                                bolusCarbs, 0, 0)

        val bolusRequestResponse: InitiateBolusResponse? = getCommunicationManager()?.sendCommand(
            bolusRequest
        ) as InitiateBolusResponse?

        if (bolusRequestResponse==null) {
            aapsLogger.error(TAG, "InitiateBolusResponse was not received." )

            return DataCommandResponse(
                PumpCommandType.SetBolus, false,
                "Error getting response from sending InitiateBolusResponse: null",
                null
            )
        }

        aapsLogger.debug(TAG, "BolusRequest: $bolusRequest and response: $bolusRequestResponse")

        sendBolusEvent(bolusEvent = TandemBolusEvent.Preparing,
                       fullAmount = detailedBolusInfo.insulin,
                       id = detailedBolusInfo.id)


        if (!bolusRequestResponse.wasBolusInitiated()) {
            aapsLogger.error(TAG, "InitiateBolusResponse status was not ok, bolus was not started: status: ${bolusRequestResponse.statusType.name}." )

            return DataCommandResponse(
                PumpCommandType.SetBolus, false,
                "InitiateBolusResponse status was not ok, bolus was not started: status: ${bolusRequestResponse.statusType.name}.",
                null
            )
        }

        var finished = false
        var bolusStatusResponse: CurrentBolusStatusResponse? = null
        var bolusStartTime: Long = System.currentTimeMillis()
        var deliveryStartTime: Long? = null
        var startedRequesting = false
        var startedDelivering = false

        val maxBolus = detailedBolusInfo.insulin - 0.01

        while (!finished) {

            Thread.sleep(500)

            bolusStatusResponse = getCommunicationManager()
                ?.sendCommand(CurrentBolusStatusRequest()) as CurrentBolusStatusResponse?

            if (bolusStatusResponse==null) {
                aapsLogger.warn(TAG, "No response for CurrentBolusStatusResponse")
                val elapsedSeconds = (System.currentTimeMillis() - bolusStartTime) / 1000
                if (elapsedSeconds >= 30) {
                    return DataCommandResponse(
                        PumpCommandType.SetBolus, false,
                        "Never received BolusStatusResponse",
                        null
                    )
                }
            } else {
                if (bolusStatusResponse.status== CurrentBolusStatusResponse.CurrentBolusStatus.ALREADY_DELIVERED_OR_INVALID) {
                    if (startedDelivering && deliveryStartTime != null) {
                        aapsLogger.error(TAG, "Bolus delivered: " + getJsonStringFromObject(bolusStatusResponse))
                        finished = true
                        val deliveryTimeSec = ((System.currentTimeMillis() - deliveryStartTime) / 1000).toInt()
                        val totalTimeSec = ((System.currentTimeMillis() - bolusStartTime) / 1000).toInt()
                        aapsLogger.error(TAG, "Bolus: amount=${detailedBolusInfo.insulin}, bolusId=$bolusId, deliveryTimeSec=${deliveryTimeSec} totalTimeSec=${totalTimeSec}")
                        bolusId = 0
                        sendBolusEvent(bolusEvent = TandemBolusEvent.DeliveryDone)
                    } else {
                        val elapsedSeconds = (System.currentTimeMillis() - bolusStartTime) / 1000
                        if (elapsedSeconds >= 30) {
                            return DataCommandResponse(
                                PumpCommandType.SetBolus, false,
                                "Never received BolusStatusResponse",
                                null
                            )
                        }
                    }
                } else {
                    //aapsLogger.error(TAG, "Bolus status: ${bolusStatusResponse.status.name}")
                    if (bolusStatusResponse.status == CurrentBolusStatusResponse.CurrentBolusStatus.REQUESTING) {
                        if (!startedRequesting) {
                            startedRequesting = true
                            sendBolusEvent(bolusEvent = TandemBolusEvent.Requesting)
                        }

                    } else if (bolusStatusResponse.status == CurrentBolusStatusResponse.CurrentBolusStatus.DELIVERING) {

                        if (!startedDelivering) {
                            // rxBus.send(EventOverviewBolusStopDeliveryEnabled(isEnabled = true))  // TODO dev4
                            startedDelivering = true
                        }

                        if (deliveryStartTime==null) {
                            deliveryStartTime = System.currentTimeMillis()
                            aapsLogger.error(TAG, "Bolus status: ${bolusStatusResponse.status.name}")
                        } else {
                            val bolusTimeSec = ((System.currentTimeMillis() - deliveryStartTime)/1000).toInt()
                            val avgRateUnder10u = 28.745858684009523
                            val avgRateOver10u = 11.200233479381131
                            val rateForBolus = when {
                                detailedBolusInfo.insulin >= 10 -> avgRateOver10u
                                else -> avgRateUnder10u
                            }

                            val bolusAmount = (bolusTimeSec / rateForBolus)

                            if (bolusAmount > maxBolus) {
                                aapsLogger.error(TAG, "Bolus Delivering: Max $maxBolus U")
                                sendBolusEvent(bolusEvent = TandemBolusEvent.Delivering, amountBolus = maxBolus, fullAmount = detailedBolusInfo.insulin)
                            } else {
                                aapsLogger.error(TAG, "Bolus Delivering: $bolusAmount U")
                                sendBolusEvent(bolusEvent = TandemBolusEvent.Delivering, amountBolus = bolusAmount, fullAmount = detailedBolusInfo.insulin)
                            }
                        }
                    } else {
                        aapsLogger.error(TAG, "Bolus status unknown: ${bolusStatusResponse.status.name}")
                    }
                }
            }

        }

        // when CurrentBolusStatusResponse comes back as delivered, we don't have
        // any bolus details, so we just read the latest bolus which just occurred
        val bolusDataCommandResponse = getBolus()

        if (bolusDataCommandResponse.isSuccess) {
            this.tandemPumpStatus.tandemLastBolus = bolusDataCommandResponse.value
        } else {
            aapsLogger.error(TAG, "Couldn't get latest bolus, after bolus was delivered")
        }

        return bolusDataCommandResponse
    }


    fun sendBolusEvent(bolusEvent: TandemBolusEvent,
                       amountBolus: Double = 0.0,
                       fullAmount: Double = 0.0,
                       id : Long? = null) {
        //var bolusingEvent : EventOverviewBolusProgress? = null
        var percent = 0
        var status = ""

        // TODO clean sendBolusEvent

        when(bolusEvent) {
            TandemBolusEvent.Initiating -> {
                percent = 0
                status = resourceHelper.gs(Rpc.string.bolus_preparing, fullAmount)
                //pumpStatus.activeBolusData.bolusStatus.
            }
            TandemBolusEvent.Preparing    -> {
                percent = 5
                status = resourceHelper.gs(Rpc.string.bolus_preparing,fullAmount)
            }
            TandemBolusEvent.Requesting -> {
                percent = 10
                status = resourceHelper.gs(Rpc.string.bolus_preparing, fullAmount)
            }
            TandemBolusEvent.Delivering   -> {
                percent = 10 + ((amountBolus/fullAmount)*90).toInt()
                if (percent>=95) {
                    percent = 95
                } else if (percent<1) {
                    percent = 0
                }

                var amountBolus2 = amountBolus

                if (amountBolus2>fullAmount) {
                    amountBolus2 = fullAmount
                }

                // active bolus display
                activeBolusData!!.amountImmediateDelivered = amountBolus2
                pumpStatus.activeBolusData = activeBolusData

                status = resourceHelper.gs(Rpc.string.bolus_delivered_so_far, amountBolus2, fullAmount)
            }
            TandemBolusEvent.DeliveryDone -> {
                percent = 100
                //activeBolusData!!.amountImmediateDelivered = activeBolusData!!.amountImmediateRequested
                pumpStatus.activeBolusData = null
                status = resourceHelper.gs(Rpc.string.bolus_finished_post_bolus)
            }
        }

        aapsLogger.info(TAG, "Sending Bolus Event: status=${status}, progress=${percent}")

        bolusProgressData.updateProgress(percent = percent)

    }


    enum class TandemBolusEvent {
        /**
         * Initiating: calling BolusPermissionRequest and InitiateBolusRequest commands
         */
        Initiating,

        /**
         * Preparing: between call to InitiateBolusRequest and when we receive data on CurrentBolusStatusRequest
         */
        Preparing,

        /**
         * Requesting: CurrentBolusStatusResponse.CurrentBolusStatus.REQUESTING (pump preparing piston)
         */
        Requesting,

        /**
         * Delivering: CurrentBolusStatusResponse.CurrentBolusStatus.DELIVERING (pump delivering insulin)
         */
        Delivering,

        /**
         * DeliveryDone: insulin delivered and pump bolus operation complete
         */
        DeliveryDone
    }


    fun getJsonStringFromObject(obj: Any) : String {
        return pumpUtil.gson.toJson(obj)
    }


    override fun cancelBolus(bolusData: BolusData?): DataCommandResponse<AdditionalResponseDataInterface?> {
        if (bolusId==0) {
            // TODO(jwoglom): since this is a safety critical path, we need to handle bolusId being null
            // and fall back on requesting active bolus ID and then canceling it. and also do the same
            // if we get a rejection (status!=0) on CancelBolusResponse.
            aapsLogger.warn(TAG, "cancelBolus: no BolusId found, fetching the active bolus ID as fallback")

            val currentBolus: CurrentBolusStatusResponse? = getCommunicationManager()?.sendCommand(CurrentBolusStatusRequest()) as CurrentBolusStatusResponse?
            aapsLogger.warn(TAG, "cancelBolus: current bolus: $currentBolus")

            if (currentBolus == null || currentBolus.status == CurrentBolusStatusResponse.CurrentBolusStatus.ALREADY_DELIVERED_OR_INVALID) {

                return DataCommandResponse(
                    PumpCommandType.CancelBolus, false, "No Bolus Id found, bolus either already finished or did not occur",
                    null
                )
            }

            bolusId = currentBolus.bolusId
        } else {
            aapsLogger.info(TAG, "cancelBolus (bolusId=$bolusId)")
        }

        val responseMessage: CancelBolusResponse? = getCommunicationManager()?.sendCommand(CancelBolusRequest(bolusId))
            as CancelBolusResponse?

        aapsLogger.debug(TAG, "cancelBolus responseMessage: $responseMessage ")


        return if (responseMessage != null && responseMessage.status == CancelBolusResponse.CancelStatus.SUCCESS) {
            DataCommandResponse(
                PumpCommandType.CancelBolus,
                true,
                null,
                null
            )
        } else {
            DataCommandResponse(
                PumpCommandType.CancelBolus,
                false,
                "Error cancelling bolus, got status ${responseMessage?.statusId}: $responseMessage",
                null
            )

        }
    }


    override fun getBolus(): DataCommandResponse<BolusData?> {

        aapsLogger.info(LTag.PUMPCOMM, "getBolus")

        val responseData: DataCommandResponse<BolusData?> = sendAndReceivePumpData(
            PumpCommandType.GetBolus,
            LastBolusStatusV2Request())
        {  rawContent -> tandemDataConverter.getBolus(rawContent as LastBolusStatusV2Response) }

        aapsLogger.info(TAG, "getBolus result: ${responseData.value}")

        return responseData

    }


    override fun retrieveTemporaryBasal(): DataCommandResponse<TempBasalPair?> {

        aapsLogger.info(LTag.PUMPCOMM, "retrieveTemporaryBasal")

        val responseData: DataCommandResponse<TempBasalPair?> = sendAndReceivePumpData(
            PumpCommandType.GetTemporaryBasal,
            TempRateRequest())
        {  rawContent -> tandemDataConverter.getTempBasalRate(rawContent as TempRateResponse) }

        return responseData

    }


    override fun sendTemporaryBasal(value: Int, duration: Int): DataCommandResponse<TempBasalPair?> {

        aapsLogger.info(LTag.PUMPCOMM, "sendTemporaryBasal [amount=$value,duration=$duration]")

        if (!TandemPumpApiVersion.isMobi(this.tandemPumpStatus.tandemPumpFirmware)) {
            aapsLogger.warn(LTag.PUMPCOMM, "Running on TandemApiVersion that doesn't support TBR, running open loop.")
            return super.sendTemporaryBasal(value, duration)
        }

        val responseMessage: SetTempRateResponse? = getCommunicationManager()?.sendCommand(
            SetTempRateRequest(duration, value)) as SetTempRateResponse?

        if (responseMessage!=null) {

            aapsLogger.info(TAG, "TBR: setTempBasalRate [duration=${duration},amount=${value},id=${responseMessage.tempRateId}]: isSuccess=${responseMessage.isStatusOK}")

            val tbr = TempBasalPair(insulinRate = value.toDouble(),
                                    isPercent = true,
                                    durationMinutes = duration,
                                    start = System.currentTimeMillis())

            tbr.id = responseMessage.tempRateId.toLong()

            return DataCommandResponse(
                PumpCommandType.SetTemporaryBasal, responseMessage.isStatusOK,
                if (responseMessage.isStatusOK) null else "Error sending TBR: status=${responseMessage.status}",
                tbr
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.SetTemporaryBasal, false,
                "Error getting response from sending TBR: null",
                null
            )
        }
    }


    override fun cancelTemporaryBasal(): DataCommandResponse<AdditionalResponseDataInterface?> {

        aapsLogger.info(LTag.PUMPCOMM, "cancelTemporaryBasal")

        if (!TandemPumpApiVersion.isMobi(this.tandemPumpStatus.tandemPumpFirmware)) {
            aapsLogger.warn(LTag.PUMPCOMM, "Running on TandemApiVersion that doesn't support cancelTBR, running closed loop.")
            return super.cancelTemporaryBasal()
        }

        val responseMessage: StopTempRateResponse? = getCommunicationManager()
            ?.sendCommand(StopTempRateRequest()) as StopTempRateResponse?

        return if (responseMessage!=null) {
            DataCommandResponse(
                commandType = PumpCommandType.CancelTemporaryBasal, responseMessage.isStatusOK,
                errorDescription = if (responseMessage.isStatusOK) null else "Error sending cancelTBR: status=${responseMessage.status}",
                value = ControlCommandResponse(responseMessage.tempRateId, responseMessage.status)
            )
        } else {
            DataCommandResponse(
                PumpCommandType.CancelTemporaryBasal, false,
                "Error getting response from sending cancelTBR: null",
                null
            )
        }
    }


    fun executeSimpleSetCommand(requestMessage: Message,
                                pumpCommandType: PumpCommandType,
                                operation: String): DataCommandResponse<AdditionalResponseDataInterface?> {

        val responseMessage: StatusMessage? = getCommunicationManager()
            ?.sendCommand(requestMessage) as StatusMessage?

        return if (responseMessage!=null) {
            DataCommandResponse(
                pumpCommandType, responseMessage.isStatusOK,
                if (responseMessage.isStatusOK) null else "Error sending $operation: status=${responseMessage.status}",
                null
            )
        } else {
            DataCommandResponse(
                PumpCommandType.SetTemporaryBasal, false,
                "Error getting response from sending $operation: null",
                null
            )
        }
    }


    override fun retrieveBasalProfile(): DataCommandResponse<BasalProfileDto?> {

        val pumpProfileDto = getBasalProfileInternal()

        if (pumpProfileDto.success) {
            val basalProfileResponse = tandemDataConverter.getBasalProfileResponse(pumpProfileDto)
            return basalProfileResponse
        } else {
            return DataCommandResponse(PumpCommandType.GetBasalProfile, false, pumpProfileDto.errorDescription, null)
        }

    }


    fun getBasalProfileInternal(): PumpProfileDto {

        aapsLogger.info(LTag.PUMPCOMM, "getBasalProfileInternal - getBasal Profile from Pump")

        val pumpProfileDto: PumpProfileDto = getInitialBasalProfileConfiguration()

        if (!pumpProfileDto.success || pumpProfileDto.isNewScenario) {
            return pumpProfileDto
        }

        val numberOfSegments = pumpProfileDto.idpSettingsResponse!!.numberOfProfileSegments

        aapsLogger.debug(LTag.PUMPCOMM, "IDPSettings has $numberOfSegments segments.")

        var responseMessage : Message?
        var responseText : String?
        val idpId = pumpProfileDto.activeIdpId!!

        for (i in 0..numberOfSegments-1) {

            aapsLogger.debug(LTag.PUMPCOMM, "IDPSegmentRequest [idpId=$idpId, segmentIndex=$i]")

            responseMessage = getCommunicationManager()
                ?.sendCommand(IDPSegmentRequest(idpId, i))

            responseText = checkResponse(responseMessage, "IDPSegmentRequest [idpId=${idpId}, segmentNumber=${i}]")

            if (responseText!=null) {
                pumpProfileDto.success = false
                pumpProfileDto.errorDescription = responseText
                return pumpProfileDto
            } else {
                val rspMsg = responseMessage as IDPSegmentResponse
                pumpProfileDto.mapSegments.put(i, rspMsg)
            }
        }

        val gsonText = pumpUtil.gson.toJson(pumpProfileDto);

        aapsLogger.debug(LTag.PUMPCOMM, "PumpProfileDto: $gsonText")

        return pumpProfileDto
    }


    private fun checkResponse(responseMessage: Message?, description: String): String? {
        return if (responseMessage==null || responseMessage is ErrorResponse) {
            val responseText = if (responseMessage==null) {
                "Response for $description was null (timeout or some other problem)."
            } else {
                val errorResponse: ErrorResponse = responseMessage as ErrorResponse
                "Received Error Response: ${errorResponse.errorCode.name} on ${description}."
            }

            aapsLogger.error(TAG, responseText)

            responseText
        } else {
            null
        }
    }

    var zeroByte : Byte = 0


    fun getInitialBasalProfileConfiguration(): PumpProfileDto {

        var responseMessage: Message? = getCommunicationManager()
            ?.sendCommand(ProfileStatusRequest())

        var responseText = checkResponse(responseMessage, "ProfileStatusRequest")

        val pumpProfileDto = PumpProfileDto()

        if (responseText!=null) {
            pumpProfileDto.success = false
            pumpProfileDto.errorDescription = responseText
            return pumpProfileDto
        }

        pumpProfileDto.profileStatusResponse = responseMessage as ProfileStatusResponse

        val idpId = pumpProfileDto.profileStatusResponse!!.idpSlot0Id

        pumpProfileDto.activeIdpId = idpId

        aapsLogger.debug(TAG, "IdpId of idpSlot0Id: $idpId")

        if (idpId==-1) {
            aapsLogger.info(TAG, "No profiles exists on pump, NEW scenario needs to run")
            pumpProfileDto.isNewScenario = true
            return pumpProfileDto
        }

        responseMessage = getCommunicationManager()
            ?.sendCommand(IDPSettingsRequest(idpId))

        responseText = checkResponse(responseMessage, "IDPSettingsRequest (idpId=${idpId}")

        if (responseText!=null) {
            pumpProfileDto.success = false
            pumpProfileDto.errorDescription = responseText
            return pumpProfileDto
        }

        pumpProfileDto.idpSettingsResponse = responseMessage as IDPSettingsResponse

        return pumpProfileDto

    }


    val idpStatusId = IDPSegmentStatus.toBitmask(IDPSegmentStatus.BASAL_RATE,
                                                 IDPSegmentStatus.START_TIME,
                                                 IDPSegmentStatus.CARB_RATIO,
                                                 IDPSegmentStatus.TARGET_BG,
                                                 IDPSegmentStatus.CORRECTION_FACTOR)

    // TODO(jwoglom): use IDPManager which handles some of these complexities
    override fun sendBasalProfile(profile: Profile): DataCommandResponse<Boolean?> {

        aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfile")

        if (!TandemPumpApiVersion.isMobi(this.tandemPumpStatus.tandemPumpFirmware)) {
            aapsLogger.warn(LTag.PUMPCOMM, "Running on TandemApiVersion that doesn't support sendBasalProfile, running open loop.")
            return super.sendBasalProfile(profile)
        }

        aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfile - getBasal Profile from Pump")

        val pumpProfileDto = getBasalProfileInternal()

        if (!pumpProfileDto.success) {
            aapsLogger.error(LTag.PUMPCOMM, "sendBasalProfile: ERROR:  NOT SUCCESS - ${pumpProfileDto.errorDescription}")

            return DataCommandResponse(PumpCommandType.SetBasalProfile, false, pumpProfileDto.errorDescription, null)
        }

        val idpSegments = tandemDataConverter.getIDPSegmentsFromProfile(profile)

        // TODO(jwoglom): make and use PumpX2 const
        if (idpSegments.size>16) {
            aapsLogger.error(LTag.PUMPCOMM, "sendBasalProfile - You have more than 16 basal segments. Profile must be adjusted to have only 16 segments. Last valid segment (16) will be extended over remaining time.")
            tandemPumpUtil.sendNotification(TandemNotificationType.TandemBasalProfileError);
        }

        val gsonText = pumpUtil.gson.toJson(idpSegments)
        aapsLogger.debug(LTag.PUMPCOMM, "sendBasalProfile - Converted segments: $gsonText")

        var responseText : String?
        var success = false


        if (pumpProfileDto.isNewScenario) {

            aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfile - NEW Scenario - Create Initial IDP")

            val createIDPRequest = CreateIDPRequest("AAPS Profile",
                                                  idpSegments[0].profileCarbRatio.toInt(),
                                                  idpSegments[0].profileBasalRate,
                                                  idpSegments[0].profileTargetBG,
                                                  idpSegments[0].profileISF,
                                                  (profile.iCfg?.dia?.toInt() ?: 0) * 60,
                                                  0) // for AAPS profile we are setting this to 0

            val responseMessage = getCommunicationManager()?.sendCommand(createIDPRequest) as CreateIDPResponse?

            responseText = checkResponse(responseMessage, "sendBasalProfile - NEW Scenario - Create Initial IDP")

            if (responseText!=null || responseMessage == null) {
                aapsLogger.error(LTag.PUMPCOMM, "sendBasalProfile: ERROR: $responseText  - NEW Scenario - Create Initial IDP")

                return DataCommandResponse(PumpCommandType.SetBasalProfile, false, responseText, false)
            }

            pumpProfileDto.activeIdpId = responseMessage.newIdpId

            success = responseMessage.isStatusOK

            aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfile - Create Initial IDP - Status: success=$success")

        } else {
            // delete all segments excepts first one
            if (pumpProfileDto.mapSegments.size>1) {
                for (index in (pumpProfileDto.mapSegments.size-1) downTo 1) {
                    deleteIDPSegment(pumpProfileDto.mapSegments[index]!!, index)
                }
            }
        }

        val idpId = pumpProfileDto.activeIdpId!!

        var segmentCount = idpSegments.size-1

        if (segmentCount>15) {
            segmentCount = 15
        }


        for(index in 0..segmentCount) {

            val operation = if (pumpProfileDto.isNewScenario || index>0)
                SetIDPSegmentRequest.IDPSegmentOperation.CREATE_SEGMENT
            else
                SetIDPSegmentRequest.IDPSegmentOperation.MODIFY_SEGMENT_ID

            if (pumpProfileDto.isNewScenario && index==0) {
                continue; // if we have new scenario we already created 1st segment when we created profile
            }

            aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfile - SetIDPSegmentRequest [index=${index},operation=${operation}]")

            val setSegment = SetIDPSegmentRequest(idpId, 0, 0, operation,
                                                  idpSegments[index].profileStartTime,
                                                  idpSegments[index].profileBasalRate,
                                                  idpSegments[index].profileCarbRatio,
                                                  idpSegments[index].profileTargetBG,
                                                  idpSegments[index].profileISF,
                                                  idpStatusId)

            val responseMessage = getCommunicationManager()?.sendCommand(setSegment) as SetIDPSegmentResponse?

            responseText = checkResponse(responseMessage, "sendBasalProfile - SetIDPSegmentRequest (idpId=${idpId},segmentIndex=$index)")

            if (responseText!=null || responseMessage == null) {
                aapsLogger.error(LTag.PUMPCOMM, "sendBasalProfile: ERROR:  $responseText    SetIDPSegmentRequest [index=${index},operation=$operation]")

                return DataCommandResponse(PumpCommandType.SetBasalProfile, false, responseText, false)
            }

            success = responseMessage.isStatusOK

            aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfile - SetIDPSegmentRequest[index=${index},operation=$operation] Status: success=$success")

        }

        return DataCommandResponse(PumpCommandType.SetBasalProfile, success, if (success) null else "Problem setting basal profile.", success)
    }


    fun sendBasalProfileNew(pumpProfileDto: PumpProfileDto, idpSegments: List<IDPSegmentDto>): DataCommandResponse<Boolean?> {

        aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfileNew")

        // if (!TandemPumpApiVersion.isMobi(this.tandemPumpStatus.tandemPumpFirmware)) {
        //     aapsLogger.warn(LTag.PUMPCOMM, "Running on TandemApiVersion that doesn't support sendBasalProfile, running open loop.")
        //     return super.sendBasalProfile(profile)
        // }
        //
        // aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfile - getBasal Profile from Pump")
        //
        // val pumpProfileDto = getBasalProfileInternal()
        //
        // if (!pumpProfileDto.success) {
        //     aapsLogger.error(LTag.PUMPCOMM, "sendBasalProfile: ERROR:  NOT SUCCESS - ${pumpProfileDto.errorDescription}")
        //
        //     return DataCommandResponse(PumpCommandType.SetBasalProfile, false, pumpProfileDto.errorDescription, null)
        // }
        //
        // val idpSegments = tandemDataConverter.getIDPSegmentsFromProfile(profile)
        //
        // if (idpSegments.size>16) {
        //     aapsLogger.error(LTag.PUMPCOMM, "sendBasalProfile - You have more than 16 basal segments. Profile must be adjusted to have only 16 segments. Last valid segment (16) will be extended over remaining time.")
        //
        //     val notification = Notification(Notification.TANDEM_BASAL_PROFILE_ERROR, resourceHelper.gs(R.string.tandem_error_profile_only_16_segments), Notification.URGENT, 24*60)
        //     rxBus.send(EventNewNotification(notification))
        // }
        //
        // val gsonText = pumpUtil.gson.toJson(idpSegments)
        // aapsLogger.debug(LTag.PUMPCOMM, "sendBasalProfile - Converted segments: $gsonText")
        //
        var responseText : String?
        val idpId = pumpProfileDto.activeIdpId!!
        var success = false


        // create



        var segmentCount = idpSegments.size-1

        if (segmentCount>15) {
            segmentCount = 15
        }


        for(index in 0..segmentCount) {

            val operation = SetIDPSegmentRequest.IDPSegmentOperation.CREATE_SEGMENT

            aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfile - SetIDPSegmentRequest [index=${index},operation=${operation}]")

            val setSegment = SetIDPSegmentRequest(idpId, 0, 0, operation,
                                                  idpSegments[index].profileStartTime,
                                                  idpSegments[index].profileBasalRate,
                                                  idpSegments[index].profileCarbRatio,
                                                  idpSegments[index].profileTargetBG,
                                                  idpSegments[index].profileISF,
                                                  idpStatusId)

            val responseMessage = getCommunicationManager()?.sendCommand(setSegment) as SetIDPSegmentResponse

            responseText = checkResponse(responseMessage, "sendBasalProfile - SetIDPSegmentRequest (idpId=${idpId},segmentIndex=$index)")

            if (responseText!=null || responseMessage == null) {
                aapsLogger.error(LTag.PUMPCOMM, "sendBasalProfile: ERROR:  $responseText    SetIDPSegmentRequest [index=${index},operation=$operation]")

                return DataCommandResponse(PumpCommandType.SetBasalProfile, false, responseText, false)
            }

            success = responseMessage.isStatusOK

            aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfile - SetIDPSegmentRequest[index=${index},operation=$operation] Status: success=$success")

        }

        return DataCommandResponse(PumpCommandType.SetBasalProfile, success, if (success) null else "Problem setting basal profile.", success)
    }


    private fun deleteIDPSegment(idpSegment: IDPSegmentResponse, index: Int): Boolean {

        val setSegment = SetIDPSegmentRequest(idpSegment.idpId, 0, index, SetIDPSegmentRequest.IDPSegmentOperation.DELETE_SEGMENT_ID,
                                              idpSegment.profileStartTime, idpSegment.profileBasalRate,
                                              idpSegment.profileCarbRatio, idpSegment.profileTargetBG, idpSegment.profileISF,
                                              idpStatusId)

        val responseMessage = getCommunicationManager()?.sendCommand(setSegment) as SetIDPSegmentResponse?

        val responseText = checkResponse(responseMessage, "SetIDPSegmentRequest [idpId=${idpSegment.idpId},segmentIndex=$index]")

        if (responseText!=null) {
            aapsLogger.error(LTag.PUMPCOMM, "sendBasalProfile - SetIDPSegmentRequest: ERROR: ${responseText} - SetIDPSegmentRequest [index=${index},operation=DELETE_SEGMENT_ID]")
            return false
        }

        val success = responseMessage?.isStatusOK == true

        aapsLogger.info(LTag.PUMPCOMM, "sendBasalProfile - SetIDPSegmentRequest[segmentIndex=$index,op=DELETE_SEGMENT_ID] Status: success=$success")

        return success
    }


    override fun getTime(): DataCommandResponse<PumpTimeDifferenceDto?> {
        // TimeSinceResetRequest
        aapsLogger.info(LTag.PUMPCOMM, "getTime")

        val responseMessage = getCommunicationManager()?.sendCommand(TimeSinceResetRequest())
            as TimeSinceResetResponse

        val responseText = checkResponse(responseMessage, "TimeSinceResetRequest")

        if (responseText!=null) {
            return DataCommandResponse(PumpCommandType.GetTime, false, responseText, null)
        }

        val dtPump = DateTime(responseMessage.currentTimeInstant.toEpochMilli())

        val pumpTimeDifference = PumpTimeDifferenceDto(DateTime.now(), dtPump)

        return DataCommandResponse(PumpCommandType.SetTime, true,
                                   null,
                                   pumpTimeDifference)
    }


    override fun setTime(): DataCommandResponse<Boolean?> {

        aapsLogger.info(LTag.PUMPCOMM, "setTime")

        if (!TandemPumpApiVersion.isMobi(this.tandemPumpStatus.tandemPumpFirmware)) {
            aapsLogger.warn(LTag.PUMPCOMM, "Running on TandemApiVersion that doesn't support setTime, running open loop.")
            return super.setTime()
        }

        val responseMessage = getCommunicationManager()?.sendCommand(ChangeTimeDateRequest(Instant.now())) as StatusMessage?


        val responseText = checkResponse(responseMessage, "ChangeTimeDateRequest")

        if (responseText!=null || responseMessage == null) {
            return DataCommandResponse(PumpCommandType.SetTime, false, responseText, false)
        }

        return DataCommandResponse(PumpCommandType.SetTime, responseMessage.status==0,
                                   if (responseMessage.status==0) null else "Error sending ChangeTimeDate: status=${responseMessage.status}",
                                   responseMessage.status==0)
    }


    override fun getFilteredPumpHistory(filter: PumpHistoryFilterInterface): DataCommandResponse<List<Any>?> {
        // pump history will be done through HistoryRetriever, so this method won't be implemented
        return DataCommandResponse(
            PumpCommandType.GetHistory, true, null, listOf())
    }


    override fun getPumpStatus(): DataCommandResponse<AdditionalResponseDataInterface?> {

        aapsLogger.info(LTag.PUMPCOMM, "getPumpStatus")

        val responseMessage: HomeScreenMirrorResponse? = getCommunicationManager()
            ?.sendCommand(HomeScreenMirrorRequest()) as HomeScreenMirrorResponse?

        if (responseMessage!=null) {

            val gsonData = getJsonStringFromObject(responseMessage)
            aapsLogger.debug(LTag.PUMPCOMM, " MirrorResponse: $gsonData")

            val homeScreenMirrorDto = HomeScreenMirrorDto()
            homeScreenMirrorDto.parse(responseMessage.cargo)

            tandemPumpStatus.pumpStatusMirror = homeScreenMirrorDto
            val runningState = if (homeScreenMirrorDto.basalStatusIcon ==
                HomeScreenMirrorResponse.BasalStatusIcon.SUSPEND) PumpRunningState.Suspended
            else PumpRunningState.Running
            tandemPumpStatus.pumpRunningState = runningState
            // Mirror to UI LiveData so the UI reflects loop-observed state.
            tandemDataStore.postPumpRunningState(runningState)
            rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.PumpStatus))

            return DataCommandResponse(
                PumpCommandType.GetPumpStatus, true,
                null,
                homeScreenMirrorDto
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.GetPumpStatus, false,
                "Error getting status from Pump: null",
                null
            )
        }
    }


    override fun executeCustomCommand(commandType: CustomCommandTypeInterface, data: Any?): DataCommandResponse<AdditionalResponseDataInterface?> {
        val commandTypeInternal = commandType as TandemCustomCommand

        return when(commandTypeInternal) {
            SET_MAX_BOLUS      -> setMaxBolus(data as Int)
            SET_MAX_BASAL      -> setMaxBasal(data as Int)
            SET_CONTROL_IQ     -> setControlIQDisabled()
            GET_PUMP_INFO      -> getPumpInfo()
            GET_ALERTS         -> getAlerts()
            GET_ALARMS         -> getAlarms()
            DISMISS_ALERT      -> dismissNotificationAlert(data as Long)
            SET_QUICK_BOLUS    -> setQuickBolus(data as QuickBolusType)
            GET_MALFUNCTIONS   -> getMalfunctions()
            SET_PUMP_SOUNDS    -> setPumpSounds(data as PumpGlobalsResponse.AnnunciationEnum)
            else               -> {
                aapsLogger.error(TAG, "Unhandled Custom Command: ${commandType.name}")
                DataCommandResponse(
                    PumpCommandType.CustomCommand, false, "Command ${commandType.getKey()} not available.", null)
            }
        }

    }

    private fun setPumpSounds(soundVolume: PumpGlobalsResponse.AnnunciationEnum): DataCommandResponse<AdditionalResponseDataInterface?> {

        aapsLogger.info(LTag.PUMPCOMM, "set All Pump Sounds to ${soundVolume.name}")

        // The GENERAL bit here writes what PumpGlobalsResponse reads back as buttonAnnun - the
        // read and write messages use different names for the same pump setting.
        // isSoundIncorrectlySet() also checks bolusAnnun and fillTubingAnnun. Neither has a
        // matching field anywhere in pumpX2 (checked every request class with an Annun field,
        // and the fill-tubing mode requests take no parameters at all), so this request can
        // never correct either one - there is currently no known way to write them.
        val changeBitmask = SetPumpSoundsRequest.ChangeBitmask.toBitmask(
            SetPumpSoundsRequest.ChangeBitmask.QUICK_BOLUS,
            SetPumpSoundsRequest.ChangeBitmask.GENERAL,
            SetPumpSoundsRequest.ChangeBitmask.REMINDER,
            SetPumpSoundsRequest.ChangeBitmask.ALERT,
            SetPumpSoundsRequest.ChangeBitmask.ALARM
        )

        val pumpSoundRequest = SetPumpSoundsRequest(0, // firstByteUnknown
                                                    soundVolume.id(), // quickBolus
                                                    soundVolume.id(), // general (== PumpGlobalsResponse.buttonAnnun)
                                                    soundVolume.id(), // reminder
                                                    soundVolume.id(), // alert
                                                    soundVolume.id(), // alarm
                                                    0, // cgmAlertAnnunA (not changed, bit not set)
                                                    0, // cgmAlertAnnunB (not changed, bit not set)
                                                    changeBitmask)

        val responseMessage: SetPumpSoundsResponse? = getCommunicationManager()
            ?.sendCommand(pumpSoundRequest) as SetPumpSoundsResponse?

        if (responseMessage!=null) {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, responseMessage.status==0,
                if (responseMessage.status==0) null else "Error sending SetPumpSoundsRequest(generalAnnun=${soundVolume.name}): status=${responseMessage.status}",
                null
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response from sending SetPumpSoundsRequest: null",
                null
            )
        }
    }

    private fun dismissNotificationAlert(value: Long): DataCommandResponse<AdditionalResponseDataInterface?> {
        return dismissNotification(DismissNotificationRequest.NotificationType.ALERT, value);
    }


    private fun dismissNotification(notificationType: DismissNotificationRequest.NotificationType, notificationId: Long ): DataCommandResponse<AdditionalResponseDataInterface?> {

        val dismissNotificationRequest = DismissNotificationRequest(
            notificationType,
            notificationId
        )

        val details = "[type=${notificationType.name},id=$notificationId]"

        aapsLogger.info(LTag.PUMPCOMM, "dismissNotification $details")

        val responseMessage: DismissNotificationResponse? = getCommunicationManager()
            ?.sendCommand(dismissNotificationRequest) as DismissNotificationResponse?

        if (responseMessage!=null) {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, responseMessage.isStatusOK,
                if (responseMessage.isStatusOK) null else "Error sending dismissNotification $details: status=${responseMessage.status}",
                null
            )
        } else {
            aapsLogger.error(TAG, "Error getting response from sending dismissNotification $details: null")
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response from sending dismissNotification $details: null",
                null
            )
        }
    }


    private fun getPumpInfo(): DataCommandResponse<AdditionalResponseDataInterface?> {

        aapsLogger.info(TAG, "getPumpInfo")

        val responseMessage: PumpVersionResponse? = getCommunicationManager()
            ?.sendCommand(PumpVersionRequest()) as PumpVersionResponse?

        if (responseMessage!=null) {

            val pumpInfo = PumpVersionDto()
            pumpInfo.parse(responseMessage.cargo)

            return DataCommandResponse(
                PumpCommandType.CustomCommand, success = true,
                null,
                pumpInfo
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response from sending PumpVersionRequest: null",
                null
            )
        }
    }


    private fun setMaxBolus(bolusAmount: Int): DataCommandResponse<AdditionalResponseDataInterface?> {

        aapsLogger.info(LTag.PUMPCOMM, "setMaxBolus [bolusAmount=$bolusAmount]")

        val responseMessage: SetMaxBolusLimitResponse? = getCommunicationManager()
            ?.sendCommand(SetMaxBolusLimitRequest(
                InsulinUnit.from1To1000(bolusAmount.toDouble()).toInt()
            )) as SetMaxBolusLimitResponse?

        if (responseMessage!=null) {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, responseMessage.isStatusOK,
                if (responseMessage.isStatusOK) null else "Error sending SetMaxBolusLimit(maxBolus=${bolusAmount}): status=${responseMessage.status}",
                null
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response from sending SetMaxBolusLimit: null",
                null
            )
        }
    }


    private fun getAlerts(): DataCommandResponse<AdditionalResponseDataInterface?> {

        aapsLogger.info(LTag.PUMPCOMM, "getAlerts")

        val responseMessage: AlertStatusResponse? = getCommunicationManager()
            ?.sendCommand(AlertStatusRequest()) as AlertStatusResponse?

        if (responseMessage!=null) {

            val alertStatusDto = AlertStatusDto()
            alertStatusDto.parse(responseMessage.cargo)

            return DataCommandResponse(
                PumpCommandType.CustomCommand, true,
                null,
                alertStatusDto
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response from sending AlertStatusRequest: null",
                null
            )
        }
    }


    private fun getAlarms(): DataCommandResponse<AdditionalResponseDataInterface?> {

        aapsLogger.info(LTag.PUMPCOMM, "getAlerts")

        val responseMessage: AlarmStatusResponse? = getCommunicationManager()
            ?.sendCommand(AlarmStatusRequest()) as AlarmStatusResponse?

        if (responseMessage!=null) {

            val alarmStatusDto = AlarmStatusDto()
            alarmStatusDto.parse(responseMessage.cargo)

            return DataCommandResponse(
                PumpCommandType.CustomCommand, true,
                null,
                alarmStatusDto
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response from sending AlarmStatusRequest: null",
                null
            )
        }
    }


    private fun getMalfunctions(): DataCommandResponse<AdditionalResponseDataInterface?> {
        // NOTE: MalfunctionStatusResponse returns data which is not always necessarily
        // reflective of an actual malfunction. If the codeA matches an active alarm or alert,
        // we ignore the malfunction in favor of notifying for that specific alert code.
        // As a result, we will actually re-fetch the current alarm and alert state here
        // to then use pumpx2's NotificationBundle filtering logic to prevent errant notifications.
        aapsLogger.info(LTag.PUMPCOMM, "getMalfunctions")

        val responseMessage: MalfunctionBitmaskStatusResponse? = getCommunicationManager()
            ?.sendCommand(MalfunctionStatusRequest()) as MalfunctionBitmaskStatusResponse?

        if (responseMessage!=null) {

            val bundle = NotificationBundle()
            val alertResponse: AlertStatusResponse? = getCommunicationManager()
                ?.sendCommand(AlertStatusRequest()) as AlertStatusResponse?
            if (alertResponse != null) {
                bundle.add(alertResponse)
            }

            val alarmResponse: AlarmStatusResponse? = getCommunicationManager()
                ?.sendCommand(AlarmStatusRequest()) as AlarmStatusResponse?
            if (alarmResponse != null) {
                bundle.add(alarmResponse)
            }

            // if after adding the malfunction response to the bundle
            // it is automatically excluded, then ignore it
            bundle.add(responseMessage)
            val malfunctionMatchesActiveAlertOrAlarm = bundle.get().none { it is MalfunctionBitmaskStatusResponse }

            val malfunctionStatusDto = MalfunctionStatusDto(malfunctionMatchesActiveAlertOrAlarm)
            malfunctionStatusDto.parse(responseMessage.cargo)


            return DataCommandResponse(
                PumpCommandType.CustomCommand, true,
                null,
                malfunctionStatusDto
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response from sending MalfunctionStatusRequest: null",
                null
            )
        }
    }


    private fun getQuickBolus(): DataCommandResponse<AdditionalResponseDataInterface?> {

        val responseMessage: PumpGlobalsResponse? = getCommunicationManager()
            ?.sendCommand((PumpGlobalsRequest())) as PumpGlobalsResponse?

        if (responseMessage!=null) {

            val pumpGlobalsDto = PumpGlobalsDto()
            pumpGlobalsDto.parse(responseMessage.cargo)

            return DataCommandResponse(
                PumpCommandType.CustomCommand, true,
                null,
                pumpGlobalsDto
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response for getQuickBolus: null",
                null
            )
        }
    }



    /**
     * @param basalAmount units
     */
    private fun setMaxBasal(basalAmount: Int): DataCommandResponse<AdditionalResponseDataInterface?> {

        aapsLogger.info(LTag.PUMPCOMM, "setMaxBasal [basalAmount=$basalAmount]")

        // units -> milliunits
        val newMaxBasalLimit = SetMaxBasalLimitRequest((basalAmount * 1000).toLong())

        val responseMessage: SetMaxBasalLimitResponse? = getCommunicationManager()
            ?.sendCommand(newMaxBasalLimit) as SetMaxBasalLimitResponse?

        if (responseMessage!=null) {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, responseMessage.status==0,
                if (responseMessage.status==0) null else "Error sending SetMaxBasalLimit(maxBasal=${basalAmount}): status=${responseMessage.status}",
                null
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response from sending SetMaxBasalLimit: null",
                null
            )
        }
    }


    private fun setQuickBolus(quickBolusType: QuickBolusType): DataCommandResponse<AdditionalResponseDataInterface?> {
        aapsLogger.info(LTag.PUMPCOMM, "set QuickBolus [quickBolus=$quickBolusType]")

        val quickBolusIncrement = SetQuickBolusSettingsRequest.QuickBolusIncrement.valueOf(quickBolusType.name)

        val responseMessage: SetQuickBolusSettingsResponse? = getCommunicationManager()
            ?.sendCommand(SetQuickBolusSettingsRequest(quickBolusIncrement)) as SetQuickBolusSettingsResponse?

        if (responseMessage!=null) {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, responseMessage.status==0,
                if (responseMessage.status==0) null else "Error sending SetQuickBolusSettings(quickBolus=$quickBolusType): status=${responseMessage.status}",
                null
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response from sending SetQuickBolusSettings: null",
                null
            )
        }
    }



    private fun setControlIQDisabled(): DataCommandResponse<AdditionalResponseDataInterface?> {

        aapsLogger.info(LTag.PUMPCOMM, "setControlIQDisabled")

        val responseMessage: ChangeControlIQSettingsResponse? = getCommunicationManager()
            ?.sendCommand(ChangeControlIQSettingsRequest(false, 0, 0)) as ChangeControlIQSettingsResponse?

        if (responseMessage!=null) {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, responseMessage.isStatusOK,
                if (responseMessage.isStatusOK) null else "Error sending ChangeControlIQSettings(enabled=false): status=${responseMessage.status}",
                null
            )
        } else {
            return DataCommandResponse(
                PumpCommandType.CustomCommand, false,
                "Error getting response from sending ChangeControlIQSettings: null",
                null
            )
        }
    }




    private inline fun <reified T>  sendAndReceivePumpData(commandType: PumpCommandType,
                                                           requestMessage: Message,
                                                           decode: (responseMessage: Message) -> T
    ): T {

        aapsLogger.info(TAG, "TANDEMDBG: sendAndReceivePumpData for $commandType  - Request message: ${requestMessage.javaClass.name}")

        val responseMessage: Message? = getCommunicationManager()?.sendCommand(requestMessage)

        aapsLogger.info(TAG, "TANDEMDBG: sendAndReceivePumpData: Response: $responseMessage")

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


    private fun getCorrectRequest(command: TandemCommandType): Message {

        return if (this.tandemPumpStatus.tandemPumpFirmware == TandemPumpApiVersion.VERSION_2_1_to_2_4) {
            when(command) {
                TandemCommandType.ControlIQInfo  -> ControlIQInfoV1Request()
                TandemCommandType.CurrentBattery -> CurrentBatteryV1Request()
            }
        } else if (this.tandemPumpStatus.tandemPumpFirmware == TandemPumpApiVersion.Unknown) {
            throw Exception("Unidentified version: ${this.tandemPumpStatus.tandemPumpFirmware} - ${tandemPumpStatus.apiVersionResponse?.apiVersion}")
        } else {
            when(command) {
                TandemCommandType.ControlIQInfo  -> ControlIQInfoV2Request()
                TandemCommandType.CurrentBattery -> CurrentBatteryV2Request()
            }
        }

        // TODO remove this
        // return when(this.tandemPumpStatus.tandemPumpFirmware) {
        //
        //     // TODO(jwoglom): we can use builder methods in PumpX2 given the api version
        //     TandemPumpApiVersion.VERSION_2_1_to_2_4,
        //     -> {
        //         when(command) {
        //             TandemCommandType.ControlIQInfo  -> ControlIQInfoV1Request()
        //             TandemCommandType.CurrentBattery -> CurrentBatteryV1Request()
        //         }
        //     }
        //
        //     TandemPumpApiVersion.VERSION_2_5_OR_HIGHER,
        //     TandemPumpApiVersion.VERSION_3_0,
        //     TandemPumpApiVersion.VERSION_3_2,
        //     TandemPumpApiVersion.VERSION_3_4,
        //     TandemPumpApiVersion.VERSION_3_5_MOBI,
        //     TandemPumpApiVersion.VERSION_3_6_MOBI,
        //     TandemPumpApiVersion.VERSION_3_8_MOBI,
        //     TandemPumpApiVersion.VERSION_4_x -> {
        //         when(command) {
        //             TandemCommandType.ControlIQInfo  -> ControlIQInfoV2Request()
        //             TandemCommandType.CurrentBattery -> CurrentBatteryV2Request()
        //         }
        //     }
        //     else -> throw Exception("Unidentified version: ${this.tandemPumpStatus.tandemPumpFirmware} - ${tandemPumpStatus.apiVersionResponse?.apiVersion}")
        // }
    }


    init {
        supportedCommandsList = setOf(
            PumpCommandType.GetFirmwareVersion,
            PumpCommandType.GetRemainingInsulin,
            PumpCommandType.GetTime,
            PumpCommandType.SetTime,
            PumpCommandType.GetSettings,
            PumpCommandType.GetBatteryStatus,
            PumpCommandType.GetTemporaryBasal,
            PumpCommandType.SetTemporaryBasal,
            PumpCommandType.CancelTemporaryBasal
        )
    }


    override fun getSupportedCommands(): Set<PumpCommandType> {
        return supportedCommandsList
    }


    fun isConnected(): Boolean {
        return if (tandemPumpCommunicationManager==null) {
            false
        } else {
            getCommunicationManager()?.connected == true
        }
    }

}