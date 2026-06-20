package app.aaps.pump.tandem.common.comm.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.tandem.common.database.data.dto.TandemHistoryRecordDto
import app.aaps.pump.tandem.common.database.data.dto.TandemQualifyingEventDto
import com.jwoglom.pumpx2.pump.TandemError
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HistoryLogStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.InsulinStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TimeSinceResetResponse
import com.jwoglom.pumpx2.pump.messages.models.NotificationBundle
import com.jwoglom.pumpx2.pump.messages.response.controlStream.DetectingCartridgeStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.controlStream.EnterChangeCartridgeModeStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.controlStream.ExitFillTubingModeStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.controlStream.FillCannulaStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.controlStream.FillTubingStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.LoadStatusResponse
import app.aaps.pump.tandem.mobi.ui.actions.cartridge.CompletedCartridgeAction
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ApiVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TempRateResponse
import java.time.Instant


/**
 * Read-only view of pump-side UI state. Non-UI/backend code must NOT depend on this — backend
 * reads its source of truth from TandemPumpStatus flows instead. Provided for UI consumers that
 * only observe.
 */
interface TandemUiState {
    val pumpRunningState: LiveData<PumpRunningState>
    val pumpConnected: LiveData<Boolean>
}

/**
 * Write-only view of pump-side UI state. This is the ONLY surface non-UI/backend code is handed
 * (via the [tandemDataStore] global). Because it exposes no LiveData getters, accidental reads of
 * UI state from backend code won't compile. All methods are thread-safe (postValue).
 */
interface TandemUiStateWriter {
    fun postPumpRunningState(state: PumpRunningState)
    fun postPumpConnected(connected: Boolean)
    fun postApiVersionResponse(message: ApiVersionResponse)
    fun postPumpVersionResponse(message: PumpVersionResponse)
    fun postDebugLastTandemError(error: TandemError?)
}

class TandemUIDataStore : TandemUiState, TandemUiStateWriter {

    // Basic Stuff
    var apiVersionResponse = MutableLiveData<ApiVersionResponse>()
    var pumpVersionResponse = MutableLiveData<PumpVersionResponse>()
    val timeSinceResetResponse = MutableLiveData<TimeSinceResetResponse>()

    // TBR
    var tempRateActive = MutableLiveData<Boolean>(false)
    var tempRateDetails = MutableLiveData<TempRateResponse>()

    // Running State
    override var pumpRunningState = MutableLiveData<PumpRunningState>()

    // Notifications
    val notificationBundle = MutableLiveData<NotificationBundle>(NotificationBundle())
    val notificationsPresent = MutableLiveData<Boolean>(false)

    // Cartridge/Canula Actions
    val inChangeCartridgeMode = MutableLiveData<Boolean>()
    val inFillTubingMode = MutableLiveData<Boolean>()
    val enterChangeCartridgeState = MutableLiveData<EnterChangeCartridgeModeStateStreamResponse>()
    val detectingCartridgeState = MutableLiveData<DetectingCartridgeStateStreamResponse>()
    val fillTubingState = MutableLiveData<FillTubingStateStreamResponse>()
    val exitFillTubingState = MutableLiveData<ExitFillTubingModeStateStreamResponse>()
    val fillCannulaState = MutableLiveData<FillCannulaStateStreamResponse>()
    val loadStatus = MutableLiveData<LoadStatusResponse>()
    val insulinStatus = MutableLiveData<InsulinStatusResponse>()
    // Per-visit completion set; cleared on CartridgeActions exit.
    val completedCartridgeActions = MutableLiveData<Set<CompletedCartridgeAction>>(emptySet())

    // Temporary Data for TesterApp
    override val pumpConnected = MutableLiveData<Boolean>(false)
    val pumpLastConnectionTimestamp = MutableLiveData<Instant>()
    val pumpLastMessageTimestamp = MutableLiveData<Instant>()

    // Used by the Debug Commands screen: most recently received message overall.
    val debugLastReceivedMessage = MutableLiveData<Message?>()
    val debugLastTandemError = MutableLiveData<TandemError?>()


    // DATA ----------------------------------

    val dataQELoaded = MutableLiveData<Boolean>(false)
    val dataQE = MutableLiveData<MutableList<TandemQualifyingEventDto>>(mutableListOf())

    val dataHistoryLoaded = MutableLiveData<Boolean>(false)
    val dataHistory = MutableLiveData<MutableList<TandemHistoryRecordDto>>(mutableListOf())

    // REMINDER ----------------------------------

    val reminderDateTime = MutableLiveData<Long?>(null)
    val reminderDateTimeUpdated = MutableLiveData<Boolean>(false)

    val historyLogStatus = MutableLiveData<HistoryLogStatusResponse>()


    // --- TandemUiStateWriter (backend write surface; thread-safe) ---

    override fun postPumpRunningState(state: PumpRunningState) = pumpRunningState.postValue(state)
    override fun postPumpConnected(connected: Boolean) = pumpConnected.postValue(connected)
    override fun postApiVersionResponse(message: ApiVersionResponse) = apiVersionResponse.postValue(message)
    override fun postPumpVersionResponse(message: PumpVersionResponse) = pumpVersionResponse.postValue(message)
    override fun postDebugLastTandemError(error: TandemError?) = debugLastTandemError.postValue(error)
}