package app.aaps.pump.tandem.common.comm.ui

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.notifications.NotificationId
import app.aaps.core.interfaces.notifications.NotificationLevel
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.tandem.common.comm.TandemPumpCommunicationManager
import app.aaps.pump.tandem.common.comm.data.CommunicationListener
import app.aaps.pump.tandem.common.comm.history.HistoryRetriever
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.response.HomeScreenMirrorDto
import app.aaps.pump.tandem.common.util.TandemPumpUtil

import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.models.NotificationBundle
import com.jwoglom.pumpx2.pump.messages.models.StatusMessage
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.TempRateRequest
import com.jwoglom.pumpx2.pump.messages.response.control.DismissNotificationResponse
import com.jwoglom.pumpx2.pump.messages.response.control.EnterChangeCartridgeModeResponse
import com.jwoglom.pumpx2.pump.messages.response.control.EnterFillTubingModeResponse
import com.jwoglom.pumpx2.pump.messages.response.control.ExitChangeCartridgeModeResponse
import com.jwoglom.pumpx2.pump.messages.response.control.ExitFillTubingModeResponse
import com.jwoglom.pumpx2.pump.messages.response.control.ResumePumpingResponse
import com.jwoglom.pumpx2.pump.messages.response.control.SetTempRateResponse
import com.jwoglom.pumpx2.pump.messages.response.control.SuspendPumpingResponse
import com.jwoglom.pumpx2.pump.messages.response.controlStream.DetectingCartridgeStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.controlStream.EnterChangeCartridgeModeStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.controlStream.ExitFillTubingModeStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.controlStream.FillCannulaStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.controlStream.FillTubingStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ApiVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HistoryLogResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HistoryLogStatusResponse
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.LoadStatusRequest
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HomeScreenMirrorResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.InsulinStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.LoadStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TempRateResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TimeSinceResetResponse
import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLogStreamResponse
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TandemUICommunication @Inject constructor (
    var dataStore: TandemUIDataStore,
    var pumpStatus: TandemPumpStatus,
    var aapsLogger: AAPSLogger,
    var pumpUtil: TandemPumpUtil,
    var uiInteraction: UiInteraction,
    var notificationManager: NotificationManager
): CommunicationListener {

    var TAG = LTag.PUMPCOMM

    var tandemPumpCommunicationManager : TandemPumpCommunicationManager? = null
        set(value) {
            aapsLogger.debug("tandemCommunicationManager set: $value")
            if (value==null) {
                if (field!=null) {
                    field!!.communicationListener = null
                }
            }
            field = value
            aapsLogger.debug("tandemCommunicationManager set: $field")
        }

    lateinit var historyRetriever: HistoryRetriever

    var messageCount = 0


    fun sendCommand(request: Message): Boolean {

        aapsLogger.warn(TAG, "Send command $request")

        aapsLogger.warn(TAG, "Send command ${this.tandemPumpCommunicationManager}")

        if (this.tandemPumpCommunicationManager==null) {
            aapsLogger.error(TAG, "Command ${request.javaClass.name} couldn't be executed because tandemCommunicationManager is null.")
            return false
        }

        if (!tandemPumpCommunicationManager!!.isListenerEnabled()) {
            tandemPumpCommunicationManager!!.communicationListener = this
        }

        if (!tandemPumpCommunicationManager!!.isPumpFullyConnected()) {
            aapsLogger.warn(TAG, "Command ${request::javaClass.name} couldn't be executed because pump is not fully connected yet.")
            return false
        }

        tandemPumpCommunicationManager!!.sendCommandWithListener(request)

        return true

    }


    override fun onReceiveMessage(message: Message) {

        //aapsLogger.error(TAG, "onReceiveMessageListener: $message")

        messageCount++

        if (message is ApiVersionResponse) {
            aapsLogger.debug(TAG, "Got ApiVersionRequest: $message")
            //checkPumpInitMessagesReceived(peripheral)
        } else if (message is TimeSinceResetResponse) {
            aapsLogger.debug(TAG,"Got TimeSinceResetResponse: $message")
            //checkPumpInitMessagesReceived(peripheral)
        }

        aapsLogger.debug(TAG , "TUC: Message received: ${message.javaClass.name}")
        dataStore.pumpLastMessageTimestamp.value = Instant.now()
        dataStore.debugLastReceivedMessage.postValue(message)


        if (NotificationBundle.isNotificationResponse(message)) {
            // returns an instance of itself: ensures that watchers get the updated values
            if (dataStore.notificationBundle.value == null) {
                dataStore.notificationBundle.value = NotificationBundle()
            }

            dataStore.notificationBundle.value = dataStore.notificationBundle.value?.add(message)

            return
        }


        when (message) {

            is DismissNotificationResponse -> {
                aapsLogger.info(TAG, "DismissNotificationResponse received: success=${message.isStatusOK}")
            }
            is HomeScreenMirrorResponse -> {
                // dataStore.basalStatus.value = when (message.basalStatusIcon) {
                //     HomeScreenMirrorResponse.BasalStatusIcon.BASAL -> BasalStatus.ON
                //     HomeScreenMirrorResponse.BasalStatusIcon.ZERO_BASAL -> BasalStatus.ZERO
                //     HomeScreenMirrorResponse.BasalStatusIcon.TEMP_RATE -> BasalStatus.TEMP_RATE
                //     HomeScreenMirrorResponse.BasalStatusIcon.ZERO_TEMP_RATE -> BasalStatus.ZERO_TEMP_RATE
                //     HomeScreenMirrorResponse.BasalStatusIcon.SUSPEND -> BasalStatus.PUMP_SUSPENDED
                //     HomeScreenMirrorResponse.BasalStatusIcon.HYPO_SUSPEND_BASAL_IQ -> BasalStatus.BASALIQ_SUSPENDED
                //     HomeScreenMirrorResponse.BasalStatusIcon.INCREASE_BASAL -> BasalStatus.CONTROLIQ_INCREASED
                //     HomeScreenMirrorResponse.BasalStatusIcon.ATTENUATED_BASAL -> BasalStatus.CONTROLIQ_REDUCED
                //     else -> BasalStatus.UNKNOWN
                // }
                val runningState = if (message.basalStatusIcon == HomeScreenMirrorResponse.BasalStatusIcon.SUSPEND) PumpRunningState.Suspended else PumpRunningState.Running
                dataStore.pumpRunningState.value = runningState
                pumpStatus.pumpRunningState = runningState

                pumpStatus.pumpStatusMirror = HomeScreenMirrorDto()
                pumpStatus.pumpStatusMirror!!.parse(message.cargo)
            }
            // is CurrentBasalStatusResponse -> {
            //     dataStore.basalRate.value = "${twoDecimalPlaces1000Unit(message.currentBasalRate)} U"
            // }

            is TempRateResponse -> {
                dataStore.tempRateActive.value = message.active
                dataStore.tempRateDetails.value = message
            }

//             is BolusCalcDataSnapshotResponse -> {
// //                    if (!cached) {
// //                        dataStore.bolusCalcDataSnapshot.value = message
// //                    }
//             }

            // is BolusPermissionResponse -> {
            //     dataStore.bolusPermissionResponse.value = message
            // }
            // is RemoteCarbEntryResponse -> {
            //     dataStore.bolusCarbEntryResponse.value = message
            // }
            // is InitiateBolusResponse -> {
            //     dataStore.bolusInitiateResponse.value = message
            // }
            // is CancelBolusResponse -> {
            //     if (dataStore.bolusCancelResponse.value == null || message.wasCancelled()) {
            //         dataStore.bolusCancelResponse.value = message
            //     } else {
            //         Timber.w("skipping population of bolusCancelResponse: $message because a successful cancellation already existed in the state: ${dataStore.bolusCancelResponse.value}");
            //     }
            // }
            // is CurrentBolusStatusResponse -> {
            //     dataStore.bolusCurrentResponse.value = message
            // }
            is TimeSinceResetResponse -> {
                dataStore.timeSinceResetResponse.value = message
            }
            is EnterChangeCartridgeModeStateStreamResponse -> {
                dataStore.enterChangeCartridgeState.value = message
            }
            is DetectingCartridgeStateStreamResponse -> {
                dataStore.detectingCartridgeState.value = message
            }
            is FillTubingStateStreamResponse -> {
                dataStore.fillTubingState.value = message
            }
            is ExitFillTubingModeStateStreamResponse -> {
                dataStore.exitFillTubingState.value = message
            }
            is FillCannulaStateStreamResponse -> {
                dataStore.fillCannulaState.value = message
            }
            is InsulinStatusResponse -> {
                dataStore.insulinStatus.value = message
            }
            is LoadStatusResponse -> {
                dataStore.loadStatus.value = message
                // Reconcile mode flags from the source of truth.
                val active = message.isLoadingActive
                val ls = message.loadState
                val inChange = active && ls == LoadStatusResponse.LoadState.CHANGE_CARTRIDGE
                val inTubing = active && ls == LoadStatusResponse.LoadState.PRIME_TUBING
                if (dataStore.inChangeCartridgeMode.value != inChange) dataStore.inChangeCartridgeMode.value = inChange
                if (dataStore.inFillTubingMode.value != inTubing) dataStore.inFillTubingMode.value = inTubing
            }
            // is PumpingStateStreamResponse -> {
            //     dataStore.pumpingState.value = message
            // }
            is EnterChangeCartridgeModeResponse -> {
                if (!message.isStatusOK) {
                    unsuccessfulAlert(message.messageName())
                    sendCommand(LoadStatusRequest())
                } else {
                    dataStore.inChangeCartridgeMode.value = true
                }
            }
            is ExitChangeCartridgeModeResponse -> {
                if (message.status != 0) {
                    unsuccessfulAlert(message.messageName())
                    sendCommand(LoadStatusRequest())
                } else {
                    dataStore.inChangeCartridgeMode.value = false
                }
            }
            is EnterFillTubingModeResponse -> {
                if (!message.isStatusOK) {
                    unsuccessfulAlert(message.messageName())
                    sendCommand(LoadStatusRequest())
                } else {
                    dataStore.inFillTubingMode.value = true
                }
            }
            is ExitFillTubingModeResponse -> {
                if (!message.isStatusOK) {
                    unsuccessfulAlert(message.messageName())
                    sendCommand(LoadStatusRequest())
                } else {
                    dataStore.inFillTubingMode.value = false
                }
            }
            is SetTempRateResponse -> {
                if (!message.isStatusOK)
                    unsuccessfulAlert(message.messageName())
                else {
                    aapsLogger.debug("Set Temp Rate Sent: ${message.tempRateId}")
                    sendCommand(TempRateRequest()) // for refreshing the screen
                }
            }
            is HistoryLogStatusResponse -> {
                dataStore.historyLogStatus.value = message

                if (::historyRetriever.isInitialized) {
                    historyRetriever.receivedStatus(message)
                }
            }
            is HistoryLogResponse -> {
                if (::historyRetriever.isInitialized) {
                    historyRetriever.receivedLogResponse(message)
                }
            }
            is HistoryLogStreamResponse -> {
                if (::historyRetriever.isInitialized) {
                    historyRetriever.receivedLogStreamResponse(message)
                }
            }
            is ResumePumpingResponse    -> {
                aapsLogger.error(TAG, "ResumePumpingResponse received with status=${message.isStatusOK}")
                if (message.isStatusOK) {
                    //dataStore.basalStatus.value = BasalStatus.ON
                    dataStore.pumpRunningState.value = PumpRunningState.Running
                    pumpStatus.pumpRunningState = PumpRunningState.Running
                } else {
                    unsuccessfulAlert(message.messageName())
                }
            }
            is SuspendPumpingResponse   -> {
                aapsLogger.error(TAG, "SuspendPumpingResponse received with status=${message.isStatusOK}")
                if (message.isStatusOK) {
                    dataStore.pumpRunningState.value = PumpRunningState.Suspended
                    pumpStatus.pumpRunningState = PumpRunningState.Suspended
                } else {
                    unsuccessfulAlert(message.messageName())
                }
            }
            is StatusMessage            -> {
                aapsLogger.error(TAG, "Received StatusMessage which is not yet supported: ${message.javaClass.name}, success=${message.isStatusOK}")
                if (!message.isStatusOK) unsuccessfulAlert(message.messageName())
            }
            else -> {
                aapsLogger.error(TAG, "Command processing for ${message.javaClass.name} not yet supported.")
            }
        }

    }

    private fun unsuccessfulAlert(req: String) {

        aapsLogger.error(TAG,"$req was not successful. The pump returned an error fulfilling the request." )


        notificationManager.post(
            id = NotificationId.TANDEM_PUMP_MESSAGE_ERROR,
            text = "$req was not successful. The pump returned an error fulfilling the request.",
            level = NotificationLevel.URGENT
        )

//        val show = AlertDialog.Builder(context)
//            .setTitle("Failed Pump Request")
//            .setMessage("$req was not successful. The pump returned an error fulfilling the request.")
//            .setPositiveButton("OK", null)
//            .show()
    }



//    }


}