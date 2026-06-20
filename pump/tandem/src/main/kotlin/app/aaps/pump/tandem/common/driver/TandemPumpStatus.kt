package app.aaps.pump.tandem.common.driver

import androidx.compose.runtime.compositionLocalOf
import app.aaps.core.data.pump.defs.PumpDescription
import app.aaps.core.data.pump.defs.PumpType
import app.aaps.core.interfaces.profile.Profile
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.pump.tandem.common.data.defs.TandemPumpApiVersion
import app.aaps.pump.common.data.PumpStatus
import app.aaps.pump.common.defs.BasalProfileStatus
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.pump.common.defs.BolusData
import app.aaps.pump.common.defs.PumpConfigurationTypeInterface
import app.aaps.pump.common.defs.PumpDriverMode
import app.aaps.pump.common.defs.PumpUpdateFragmentType
import app.aaps.pump.common.events.EventPumpFragmentValuesChanged
import app.aaps.pump.tandem.common.comm.data.DisconnectDataDto
import app.aaps.pump.tandem.common.comm.ui.TandemUIDataStore
import app.aaps.pump.tandem.common.comm.ui.TandemUiStateWriter
import app.aaps.pump.tandem.common.data.SemaphoreInfoDto
import app.aaps.pump.tandem.common.driver.connector.response.HomeScreenMirrorDto
import app.aaps.pump.tandem.common.driver.connector.response.PumpVersionDto
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.AlarmStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.AlertStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ApiVersionResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpFeaturesV1Response
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpFeaturesV2Response
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// Single concrete UI datastore instance. UI / UI-feed code uses this (read + write) via
// [tandemUiDataStore] or the [LocalTandemDataStore] composition local.
val tandemUiDataStore = TandemUIDataStore()

// Backend (non-UI) handle — write-only view. Backend code references this, so accidental reads
// of UI state from non-UI code do not compile. See TandemUiStateWriter / TandemUiState.
var tandemDataStore: TandemUiStateWriter = tandemUiDataStore

var LocalTandemDataStore = compositionLocalOf { tandemUiDataStore }

@Singleton
class TandemPumpStatus @Inject constructor(val sp: SP,
                                           val rxBus: RxBus
) : PumpStatus(PumpType.TANDEM_MOBI_BT) {

    lateinit var pumpDescription: PumpDescription
    var errorDescription: String? = null

    // tandem pump firmware
    val tandemPumpFirmwareFlow = MutableStateFlow<TandemPumpApiVersion>(TandemPumpApiVersion.Unknown)
    var tandemPumpFirmware: TandemPumpApiVersion
        get() = tandemPumpFirmwareFlow.value
        set(value) {
            tandemPumpFirmwareFlow.value = value
        }

    // Connection state — backend source of truth for delivery availability.
    // Mirrored to tandemDataStore.pumpConnected for the UI.
    val pumpConnectedFlow = MutableStateFlow(false)


    // serial Number
    val serialNumberFlow = MutableStateFlow<Long>(0)
    var serialNumber: Long
        get() = serialNumberFlow.value
        set(value) {
            serialNumberFlow.value = value
        }

    // semaphore info
    val semaphoreInfoFlow = MutableStateFlow<SemaphoreInfoDto>(SemaphoreInfoDto())
    var semaphoreInfo: SemaphoreInfoDto
        get() = semaphoreInfoFlow.value
        set(value) {
            semaphoreInfoFlow.value = value
        }

    var pumpDriverMode : PumpDriverMode? = null

    var baseBasalRate = 0.0
    var basalProfileStatus = BasalProfileStatus.NotInitialized
    var basalProfile: Profile? = null

    var bolusStep: Double = 0.1   // ??

    // Tandem specific
    var pumpStatusMirror: HomeScreenMirrorDto? = null
    var settings: MutableMap<PumpConfigurationTypeInterface, Any>? = null
    var featuresV2: PumpFeaturesV2Response? = null
    var featuresV1: PumpFeaturesV1Response? = null
    var lastQualifyingEventsInfo: String? = null
    var tandemPumpVersion : PumpVersionDto? = null
    var tandemAlerts : Set<AlertStatusResponse.AlertResponseType>? = null
    var tandemAlarms : Set<AlarmStatusResponse.AlarmResponseType>? = null
    var tandemLastBolus: BolusData? = null
    var tandemSiteReminder: Long? = null
    var apiVersionResponse: ApiVersionResponse? = null

    var semaphoreNotifications: Boolean = false
        set(value) {
            field = value
            semaphoreInfo = SemaphoreInfoDto(value, semaphoreEvents, semaphoreHistory, semaphoreNeedsRefresh)
        }
    var semaphoreEvents = false
        set(value) {
            field = value
            semaphoreInfo = SemaphoreInfoDto(semaphoreNotifications, value, semaphoreHistory, semaphoreNeedsRefresh)
        }
    var semaphoreHistory = false
        set(value) {
            field = value
            semaphoreInfo = SemaphoreInfoDto(semaphoreNotifications, semaphoreEvents, value, semaphoreNeedsRefresh)
        }
    var semaphoreNeedsRefresh = false
        set(value) {
            field = value
            semaphoreInfo = SemaphoreInfoDto(semaphoreNotifications, semaphoreEvents, semaphoreHistory, value)
        }

    var disconnectData: DisconnectDataDto? = null


    fun initSettings() {
        activeProfileName = "UNKNOWN"
        reservoirRemainingUnits = 0.0
        reservoirFullUnits = when {
            pumpType == PumpType.TANDEM_MOBI_BT -> 200
            else -> 300
        }
        batteryRemaining = 50
        lastConnection = 0L //sp.getLong(TandemPumpConst.Statistics.LastGoodPumpCommunicationTime, 0L)
        //lastDataTime = lastConnection
    }

    fun resetPumpSettings() {
        // we are storing some pump settings which need to be deleted if pump changes (is unpaired)
        initSettings()
        pumpStatusMirror = null
        tandemPumpFirmware = TandemPumpApiVersion.Unknown
        basalProfile = null
        basalProfileStatus = BasalProfileStatus.NotInitialized
        serialNumber = 0
        errorDescription = null
        basalsByHour = null
    }


    val basalProfileForHour: Double
        get() {
            if (basalsByHour != null) {
                val c = GregorianCalendar()
                val hour = c[Calendar.HOUR_OF_DAY]
                return basalsByHour!![hour]
            }
            return 0.0
        }


    override val errorInfo: String
        get() = if (errorDescription == null) "-" else errorDescription!!


    override fun updateLastConnectionInFragment() {
        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.None))
    }


    init {
        initSettings()
    }
}