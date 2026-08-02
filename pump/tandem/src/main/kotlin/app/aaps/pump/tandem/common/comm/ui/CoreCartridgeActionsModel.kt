package app.aaps.pump.tandem.common.comm.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import app.aaps.core.data.model.TE
import app.aaps.core.data.time.T
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.insulin.InsulinManager
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.profile.ProfileRepository
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.keys.IntKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.ui.compose.siteRotation.BodyType
import app.aaps.core.ui.compose.siteRotation.SiteLocationStepHost
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val _hideNotification = MutableStateFlow(false)
    val hideNotification: StateFlow<Boolean> = _hideNotification.asStateFlow()

    // region SiteLocationStepHost

    fun hideNotifications() {
        this._hideNotification.value = true;
    }

    fun isNotififcationShowed(): Boolean {
        return !(_hideNotification.value)
    }


    override fun updateSiteLocation(location: TE.Location) {
        _siteLocation.value = location
    }

    override fun updateSiteArrow(arrow: TE.Arrow) {
        _siteArrow.value = arrow
    }

    /** Navigate from ATTACH to SITE_LOCATION if enabled, otherwise straight to ACTIVATE. */
    fun moveAfterPriming() {
        //moveStep(if (showSiteLocationStep) PatchStep.SITE_LOCATION else PatchStep.ATTACH_PATCH)
        aapsLogger.error(LTag.PUMP, "moveAfterPriming NOT IMPLEMENTED")
    }

    override fun completeSiteLocation() {
        // Site location is saved after activation completes (patchStartTime not available yet)
        //moveStep(PatchStep.ATTACH_PATCH)
        aapsLogger.error(LTag.PUMP, "completeSiteLocation NOT IMPLEMENTED")
    }


    override fun skipSiteLocation() {
        aapsLogger.error(LTag.PUMP, "skipSiteLocation NOT IMPLEMENTED")

        _siteLocation.value = TE.Location.NONE
        _siteArrow.value = TE.Arrow.NONE

        // TODO
        // moveStep(PatchStep.ATTACH_PATCH)
    }

    override fun bodyType(): BodyType =
        BodyType.fromPref(preferences.get(IntKey.SiteRotationUserProfile))

    override fun siteRotationEntries(): List<TE> = _siteRotationEntries.value

    private fun loadSiteRotationEntries() {
        aapsLogger.error(LTag.PUMP, "loadSiteRotationEntries NOT IMPLEMENTED")
        // scope.launch {
        //     _siteRotationEntries.value = persistenceLayer.getTherapyEventDataFromTime(
        //         System.currentTimeMillis() - T.days(45).msecs(), false
        //     ).filter { it.type == TE.Type.CANNULA_CHANGE || it.type == TE.Type.SENSOR_CHANGE }
        // }
    }

    /** Save site location/arrow to the CANNULA_CHANGE therapy event created during activation. */
    private fun saveSiteLocationToTherapyEvent(activationTimestamp: Long) {
        val location = _siteLocation.value.takeIf { it != TE.Location.NONE }
        val arrow = _siteArrow.value.takeIf { it != TE.Arrow.NONE }
        aapsLogger.error(LTag.PUMP, "saveSiteLocationToTherapyEvent NOT IMPLEMENTED")
        if (location != null || arrow != null) {

            // scope.launch {
            //     try {
            //         val entries = persistenceLayer.getTherapyEventDataFromToTime(activationTimestamp, activationTimestamp)
            //             .filter { it.type == TE.Type.CANNULA_CHANGE }
            //         entries.firstOrNull()?.let { te ->
            //             persistenceLayer.insertOrUpdateTherapyEvent(te.copy(location = location, arrow = arrow))
            //         }
            //     } catch (_: Exception) {
            //         // location is optional
            //     }
            // }
        }
    }

    fun resetSiteLocation() {

    }

    fun saveSiteLocation() {

    }



    // endregion



}