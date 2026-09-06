package app.aaps.pump.tandem.common.comm.ui

import app.aaps.core.ui.compose.siteRotation.SiteLocationStepHost
import kotlinx.coroutines.flow.StateFlow

interface CoreCartridgeActionsModelInterface : SiteLocationStepHost {

    fun hideNotifications()
    fun setTime()

    val hideNotification: StateFlow<Boolean>
    val showSiteLocationStep: Boolean
}

