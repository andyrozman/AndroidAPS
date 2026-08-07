package app.aaps.pump.tandem.common.comm.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.data.model.ICfg
import app.aaps.core.data.model.TE
import app.aaps.core.data.time.T
import app.aaps.core.data.ue.Sources
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.insulin.InsulinManager
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.profile.ProfileRepository
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.IntKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.ui.compose.siteRotation.BodyType
import app.aaps.core.ui.compose.siteRotation.SiteLocationStepHost
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.keys.TandemLongNonPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringNonPreferenceKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO clean up the code

@HiltViewModel
@Stable
class CoreCartridgeActionsModel @Inject constructor(
    private val aapsLogger: AAPSLogger,
    //private val medtrumPlugin: MedtrumPlugin,
    private val commandQueue: CommandQueue,
    //val medtrumPump: MedtrumPump,
    private val pumpStatus: TandemPumpStatus,
    private val insulinManager: InsulinManager,
    private val profileFunction: ProfileFunction,
    private val profileRepository: ProfileRepository,
    private val preferences: Preferences,
    private val persistenceLayer: PersistenceLayer

) : ViewModel(), SiteLocationStepHost {

    // Site location state (for SITE_LOCATION step)
    private val _siteLocation = MutableStateFlow(TE.Location.NONE)
    override val siteLocation: StateFlow<TE.Location> = _siteLocation.asStateFlow()

    private val _siteArrow = MutableStateFlow(TE.Arrow.NONE)
    override val siteArrow: StateFlow<TE.Arrow> = _siteArrow.asStateFlow()

    private val _siteRotationEntries = MutableStateFlow<List<TE>>(emptyList())

    // Insulin selection state
    private val _availableInsulins = MutableStateFlow<List<ICfg>>(emptyList())
    val availableInsulins: StateFlow<List<ICfg>> = _availableInsulins.asStateFlow()

    private val _selectedInsulin = MutableStateFlow<ICfg?>(null)
    val selectedInsulin: StateFlow<ICfg?> = _selectedInsulin.asStateFlow()

    private val _activeInsulinLabel = MutableStateFlow<String?>(null)
    val activeInsulinLabel: StateFlow<String?> = _activeInsulinLabel.asStateFlow()

    val TAG = LTag.PUMP

    val showSiteLocationStep: Boolean
        get() = preferences.get(BooleanKey.SiteRotationManagePump)

    /** Whether the insulin change step should be shown (multiple insulins available) */
    val showInsulinStep: Boolean
        get() = _availableInsulins.value.size > 1

    val concentrationEnabled: Boolean
        get() = preferences.get(BooleanKey.GeneralInsulinConcentration)

    private val _hideNotification = MutableStateFlow(false)
    val hideNotification: StateFlow<Boolean> = _hideNotification.asStateFlow()

    // region SiteLocationStepHost

    fun hideNotifications() {
        this._hideNotification.value = true;
    }

    fun isNotififcationShowed(): Boolean {
        return !(_hideNotification.value)
    }


    fun reset() {
        aapsLogger.info(LTag.PUMP, "reset: clearing state for new workflow session")
        _siteLocation.value = TE.Location.NONE
        _siteArrow.value = TE.Arrow.NONE
        _siteRotationEntries.value = emptyList()

        _availableInsulins.value = emptyList()
        _selectedInsulin.value = null;
        _activeInsulinLabel.value = null;
    }

    fun loadModelData() {
        reset()
        loadSiteRotationEntries()
        loadInsulins()
    }


    override fun updateSiteLocation(location: TE.Location) {
        _siteLocation.value = location
    }

    override fun updateSiteArrow(arrow: TE.Arrow) {
        _siteArrow.value = arrow
    }

    /** Navigate from ATTACH to SITE_LOCATION if enabled, otherwise straight to ACTIVATE. */
    // fun moveAfterPriming() {
    //     //moveStep(if (showSiteLocationStep) PatchStep.SITE_LOCATION else PatchStep.ATTACH_PATCH)
    //     aapsLogger.error(LTag.PUMP, "moveAfterPriming NOT IMPLEMENTED")
    // }

    /**
     * Park the selection until the pump reports the cannula fill.
     *
     * The CANNULA_CHANGE therapy event is not created here: it is inserted by
     * `HistoryPostProcessor` when the pump's `CannulaFilledHistoryLog` is retrieved, which happens
     * after this workflow ends. Preferences are used rather than in-memory state so the selection
     * survives a process restart while waiting.
     */
    override fun completeSiteLocation() {
        val location = _siteLocation.value.takeIf { it != TE.Location.NONE }
        val arrow = _siteArrow.value.takeIf { it != TE.Arrow.NONE }
        if (location == null && arrow == null) {
            clearPendingSiteLocation()
            return
        }
        preferences.put(TandemStringNonPreferenceKey.PendingSiteLocation, location?.name ?: "")
        preferences.put(TandemStringNonPreferenceKey.PendingSiteArrow, arrow?.name ?: "")
        preferences.put(TandemLongNonPreferenceKey.PendingSiteSelectedAt, System.currentTimeMillis())
        aapsLogger.info(LTag.PUMP, "completeSiteLocation: parked location=$location arrow=$arrow")
    }


    override fun skipSiteLocation() {
        aapsLogger.info(LTag.PUMP, "skipSiteLocation")
        _siteLocation.value = TE.Location.NONE
        _siteArrow.value = TE.Arrow.NONE
        clearPendingSiteLocation()
    }

    private fun clearPendingSiteLocation() {
        preferences.put(TandemStringNonPreferenceKey.PendingSiteLocation, "")
        preferences.put(TandemStringNonPreferenceKey.PendingSiteArrow, "")
        preferences.put(TandemLongNonPreferenceKey.PendingSiteSelectedAt, 0L)
    }

    override fun bodyType(): BodyType =
        BodyType.fromPref(preferences.get(IntKey.SiteRotationUserProfile))

    override fun siteRotationEntries(): List<TE> = _siteRotationEntries.value

    fun loadSiteRotationEntries() {
        // TODO remove sensor change perhaps
        aapsLogger.error(LTag.PUMP, "loadSiteRotationEntries")
        viewModelScope.launch {
            _siteRotationEntries.value = persistenceLayer.getTherapyEventDataFromTime(
                System.currentTimeMillis() - T.days(45).msecs(), false
            ).filter { it.type == TE.Type.CANNULA_CHANGE || it.type == TE.Type.SENSOR_CHANGE }
        }
    }

    // endregion


    fun loadInsulins() {
        aapsLogger.error(LTag.PUMP, "in loadInsulins")
        if (_availableInsulins.value.isNotEmpty()) return
        aapsLogger.error(LTag.PUMP, "in loadInsulins - loading")
        viewModelScope.launch {
            val insulins = insulinManager.insulins.map { it.deepClone() }
            val activeLabel = profileFunction.getProfile()?.iCfg?.insulinLabel
            val current = insulins.find { it.insulinLabel == activeLabel } ?: insulins.firstOrNull()
            _availableInsulins.value = insulins
            _selectedInsulin.value = current
            _activeInsulinLabel.value = activeLabel
            aapsLogger.error(LTag.PUMP, "loadInsulins: selectedInsulin: ${_selectedInsulin.value}, activeInsulinLabel: ${_activeInsulinLabel.value}")
        }
    }


    fun selectInsulin(iCfg: ICfg) {
        aapsLogger.error(LTag.PUMP, "selectInsulin: ${iCfg}")
        _selectedInsulin.value = iCfg
    }


    /** Execute profile switch if user selected a different insulin. Called after activation completes. */
    fun executeInsulinProfileSwitch() {
        aapsLogger.error(TAG, "executeInsulinProfileSwitch - start")
        val selected = _selectedInsulin.value ?: return
        viewModelScope.launch {
            // Recompute from the currently active profile instead of trusting the cached value,
            // which is captured during initializePatchStep — before PROFILE_GATE selection has
            // produced an EffectiveProfileSwitch — and is not refreshed across activation sessions.
            val activeLabel = profileFunction.getProfile()?.iCfg?.insulinLabel
            aapsLogger.error(TAG, "executeInsulinProfileSwitch: ${activeLabel}")
            if (selected.insulinLabel == activeLabel) return@launch
            aapsLogger.error(TAG, "executeInsulinProfileSwitch: different insulin create switch")
            profileFunction.createProfileSwitchWithNewInsulin(selected, Sources.Tandem)
        }
    }

}