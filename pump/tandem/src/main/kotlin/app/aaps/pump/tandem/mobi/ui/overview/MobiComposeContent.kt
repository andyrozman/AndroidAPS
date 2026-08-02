package app.aaps.pump.tandem.mobi.ui.overview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.ui.compose.ComposablePluginContent
import app.aaps.core.ui.compose.ToolbarConfig
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.comm.ui.CoreCartridgeActionsModel
import app.aaps.pump.tandem.common.data.defs.RefreshData
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.mobi.ui.TandemUiController
import app.aaps.pump.tandem.mobi.ui.actions.Actions
import app.aaps.pump.tandem.mobi.ui.actions.DebugCommands
import app.aaps.pump.tandem.mobi.ui.actions.PumpInfo
import app.aaps.pump.tandem.mobi.ui.actions.cartridge.CartridgeActions
import app.aaps.pump.tandem.mobi.ui.actions.cartridge.ChangeCartridgeScreen
import app.aaps.pump.tandem.mobi.ui.actions.cartridge.FillCannulaScreen
import app.aaps.pump.tandem.mobi.ui.actions.cartridge.FillTubingScreen
import app.aaps.pump.tandem.mobi.ui.actions.cartridge.SiteReminder
import app.aaps.pump.tandem.mobi.ui.data.DataDisplayMain
import app.aaps.pump.tandem.mobi.ui.data.History
import app.aaps.pump.tandem.mobi.ui.data.Notifications
import app.aaps.pump.tandem.mobi.ui.data.QualifyingEvents
import app.aaps.core.ui.R as Rco
import app.aaps.pump.common.R as Rc
import app.aaps.core.interfaces.R as Rci

private enum class MobiScreen {
    OVERVIEW,
    DATA,
    DATA_NOTIFICATIONS,
    DATA_EVENTS,
    DATA_HISTORY,

    ACTIONS,
    ACTIONS_CARTRIDGE_ACTIONS,
    ACTIONS_CHANGE_CARTRIDGE,
    ACTIONS_FILL_TUBING,
    ACTIONS_FILL_CANNULA,
    ACTIONS_PUMP_INFO,
    ACTIONS_DEBUG_COMMANDS,
    ACTIONS_SITE_REMINDER

}

class MobiComposeContent(
    private val pluginName: String,
    private val tandemPumpStatus: TandemPumpStatus,
    private val aapsLogger: AAPSLogger,
    private val resourceHelper: ResourceHelper,
    private val tandemUiController: TandemUiController,


) : ComposablePluginContent {


    @Composable
    override fun Render(
        setToolbarConfig: (ToolbarConfig) -> Unit,
        onNavigateBack: () -> Unit,
        onSettings: (() -> Unit)?
    ) {
        val overviewViewModel: MobiOverviewViewModelV2 = hiltViewModel()
        val coreCartridgeActionsModel: CoreCartridgeActionsModel = hiltViewModel()

        // Navigation state
        var currentScreen by remember { mutableStateOf(MobiScreen.OVERVIEW) }

        // Suppress AAPS auto-reconnect only while the user is in the cartridge-change workflow
        // (cartridge change owns the pump comm channel for a long, stateful, multi-step
        // operation). Browsing Actions / Data does not — those sends are serialized by
        // PumpOpQueue at USER_INITIATED priority and AAPS Loop can safely interleave.
        val inCartridgeFlow = currentScreen == MobiScreen.ACTIONS_CHANGE_CARTRIDGE ||
            currentScreen == MobiScreen.ACTIONS_FILL_TUBING ||
            currentScreen == MobiScreen.ACTIONS_FILL_CANNULA
        DisposableEffect(inCartridgeFlow) {
            tandemUiController.setCartridgeChangeMode(inCartridgeFlow)
            onDispose { if (inCartridgeFlow) tandemUiController.setCartridgeChangeMode(false) }
        }

        // Dialogs
        var showUnpairDialog by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // errorMessage?.let { msg ->
        //     OkDialog(
        //         title = stringResource(R.string.pumperror),
        //         message = msg,
        //         onDismiss = { errorMessage = null }
        //     )
        // }

        //val iconBack = Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Rco.string.back))


        val overviewNavIcon: @Composable () -> Unit = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Rco.string.back))
            }
        }

        val nvaIconBackFromActions: @Composable () -> Unit = {
            IconButton(onClick = {
                currentScreen = MobiScreen.OVERVIEW
                tandemUiController.disposeTandemUiCommunication(TandemUiController.AdditionalConfigurationScreens.Actions)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Rco.string.back))
            }
        }

        val nvaIconBackFromData: @Composable () -> Unit = {
            IconButton(onClick = {
                currentScreen = MobiScreen.OVERVIEW
                tandemUiController.disposeTandemUiCommunication(TandemUiController.AdditionalConfigurationScreens.Data)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Rco.string.back))
            }
        }


        val navIconBackToData: @Composable () -> Unit = {
            IconButton(onClick = { currentScreen = MobiScreen.DATA }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Rco.string.back))
            }
        }

        val navIconBackToActions: @Composable () -> Unit = {
            IconButton(onClick = { currentScreen = MobiScreen.ACTIONS }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Rco.string.back))
            }
        }

        val navIconBackToPumpInfo: @Composable () -> Unit = {
            IconButton(onClick = { currentScreen = MobiScreen.ACTIONS_PUMP_INFO }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Rco.string.back))
            }
        }

        val navIconBackToCartridgeActions: @Composable () -> Unit = {
            IconButton(onClick = { currentScreen = MobiScreen.ACTIONS }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Rco.string.back))
            }
        }


        val settingsAction: @Composable RowScope.() -> Unit = {
            onSettings?.let { action ->
                IconButton(onClick = action) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(Rco.string.settings))
                }
            }
        }

        LaunchedEffect(currentScreen) {
            setToolbarConfig(
                when (currentScreen) {
                    MobiScreen.OVERVIEW     -> ToolbarConfig(title = pluginName, navigationIcon = overviewNavIcon, actions = settingsAction)
                    MobiScreen.ACTIONS      -> ToolbarConfig(title = resourceHelper.gs(R.string.ui_a_title),
                                                             navigationIcon = nvaIconBackFromActions, actions = {})
                    MobiScreen.DATA         -> ToolbarConfig(title = resourceHelper.gs(R.string.data_data),
                                                             navigationIcon = nvaIconBackFromData, actions = {})
                    MobiScreen.DATA_NOTIFICATIONS -> ToolbarConfig(title = resourceHelper.gs(R.string.data_notifications),
                                                                   navigationIcon = navIconBackToData, actions = {})
                    MobiScreen.DATA_EVENTS -> ToolbarConfig(title = resourceHelper.gs(R.string.data_events),
                                                            navigationIcon = navIconBackToData, actions = {})
                    MobiScreen.DATA_HISTORY -> ToolbarConfig(title = resourceHelper.gs(R.string.data_pump_history),
                                                             navigationIcon = navIconBackToData, actions = {})
                    MobiScreen.ACTIONS_CARTRIDGE_ACTIONS -> ToolbarConfig(title = resourceHelper.gs(R.string.ca_label),
                                                                          navigationIcon = navIconBackToActions, actions = {})
                    MobiScreen.ACTIONS_PUMP_INFO -> ToolbarConfig(title = resourceHelper.gs(R.string.pi_title),
                                                                  navigationIcon = navIconBackToActions, actions = {})
                    MobiScreen.ACTIONS_DEBUG_COMMANDS -> ToolbarConfig(title = resourceHelper.gs(R.string.debug_commands_title),
                                                                       navigationIcon = navIconBackToPumpInfo, actions = {})
                    MobiScreen.ACTIONS_CHANGE_CARTRIDGE -> ToolbarConfig(title = resourceHelper.gs(R.string.cc_title),
                                                                         navigationIcon = navIconBackToCartridgeActions, actions = {})
                    MobiScreen.ACTIONS_FILL_TUBING -> ToolbarConfig(title = resourceHelper.gs(R.string.ft_title),
                                                                    navigationIcon = navIconBackToCartridgeActions, actions = {})
                    MobiScreen.ACTIONS_FILL_CANNULA -> ToolbarConfig(title = resourceHelper.gs(R.string.fc_title),
                                                                     navigationIcon = navIconBackToCartridgeActions, actions = {})
                    MobiScreen.ACTIONS_SITE_REMINDER -> ToolbarConfig(title = resourceHelper.gs(R.string.sr_title),
                                                                      navigationIcon = navIconBackToCartridgeActions, actions = {})
                }
            )
        }

        // Handle one-time events from overview
        LaunchedEffect(overviewViewModel) {
            overviewViewModel.events.collect { event ->
                when (event) {
                    MobiOverviewEventv2.OpenEvents           -> currentScreen = MobiScreen.DATA_EVENTS
                    MobiOverviewEventv2.OpenHistory          -> currentScreen = MobiScreen.DATA_HISTORY
                    MobiOverviewEventv2.OpenNotification     -> currentScreen = MobiScreen.DATA_NOTIFICATIONS
                    MobiOverviewEventv2.StartActions         -> currentScreen = MobiScreen.ACTIONS
                    MobiOverviewEventv2.StartData            -> currentScreen = MobiScreen.DATA
                }
            }
        }

        when (currentScreen) {

            MobiScreen.OVERVIEW      -> {
                MobiOverviewScreen(
                    viewModel = overviewViewModel
                )
            }

            MobiScreen.DATA -> {
                DataDisplayMain(
                    sendPumpCommands = { messages -> tandemUiController.sendPumpCommands(messages) },
                    aapsLogger = aapsLogger,
                    resourceHelper = resourceHelper,
                    showHeader = false,
                    navigateToPumpHistory = {
                        currentScreen = MobiScreen.DATA_HISTORY
                    },
                    navigateToEvents = {
                        currentScreen = MobiScreen.DATA_EVENTS
                    },
                    navigateToNotifications = {
                        currentScreen = MobiScreen.DATA_NOTIFICATIONS
                    },
                    refreshMainAppData = { refreshData -> tandemUiController.refreshMainAppData(refreshData = refreshData)},
                )
            }

            MobiScreen.DATA_HISTORY -> {
                History(
                    refreshDatabase = { target, queryParameters -> tandemUiController.refreshDatabase(target, queryParameters) },
                    refreshMainAppData = { refreshData -> tandemUiController.refreshMainAppData(refreshData = refreshData)},
                    aapsLogger = aapsLogger,
                    resourceHelper = resourceHelper,
                    showHeader = false,
                    navigateBack = {
                        currentScreen = MobiScreen.DATA
                    },
                )
            }

            MobiScreen.DATA_EVENTS -> {
                QualifyingEvents (
                    aapsLogger = aapsLogger,
                    resourceHelper = resourceHelper,
                    showHeader = false,
                    refreshDatabase = { target, queryParameters -> tandemUiController.refreshDatabase(target, queryParameters) },
                    refreshMainAppData = { refreshData -> tandemUiController.refreshMainAppData(refreshData = refreshData)},
                    navigateBack = {
                        currentScreen = MobiScreen.DATA
                    },
                )
            }

            MobiScreen.DATA_NOTIFICATIONS -> {
                Notifications(
                    resourceHelper = resourceHelper,
                    aapsLogger = aapsLogger,
                    showHeader = false,
                    sendPumpCommands = { messages -> tandemUiController.sendPumpCommands(messages) },
                    refreshMainAppData = { refreshData -> tandemUiController.refreshMainAppData(refreshData = refreshData)},
                    navigateBack = {
                        currentScreen = MobiScreen.DATA
                    }
                )
            }

            MobiScreen.ACTIONS -> {
                Actions(
                    sendPumpCommands = { messages -> tandemUiController.sendPumpCommands(messages) },
                    aapsLogger = aapsLogger,
                    resourceHelper = resourceHelper,
                    showHeader = false,
                    refreshMainAppData = { data -> tandemUiController.refreshMainAppData(data) },
                    navigateToPumpInfo = {
                        currentScreen = MobiScreen.ACTIONS_PUMP_INFO
                    },
                    navigateToCartridgeActions = {
                        currentScreen = MobiScreen.ACTIONS_CARTRIDGE_ACTIONS
                    }
                )
            }

            MobiScreen.ACTIONS_PUMP_INFO -> {
                PumpInfo(
                    resourceHelper = resourceHelper,
                    tandemPumpStatus = tandemPumpStatus,
                    showHeader = false,
                    navigateBack = {
                        currentScreen = MobiScreen.ACTIONS
                    },
                    navigateToDebugCommands = {
                        currentScreen = MobiScreen.ACTIONS_DEBUG_COMMANDS
                    },
                )
            }

            MobiScreen.ACTIONS_DEBUG_COMMANDS -> {
                DebugCommands(
                    sendPumpCommands = { messages -> tandemUiController.sendPumpCommands(messages) },
                    resourceHelper = resourceHelper,
                    aapsLogger = aapsLogger,
                    showHeader = false,
                    navigateBack = {
                        currentScreen = MobiScreen.ACTIONS_PUMP_INFO
                    },
                )
            }

            MobiScreen.ACTIONS_CARTRIDGE_ACTIONS -> {
                CartridgeActions(
                    aapsLogger = aapsLogger,
                    sendPumpCommands = { messages -> tandemUiController.sendPumpCommands(messages) },
                    resourceHelper = resourceHelper,
                    showHeader = false,
                    navigateToChangeCartridge = {
                        currentScreen = MobiScreen.ACTIONS_CHANGE_CARTRIDGE
                    },
                    navigateToFillTubing = {
                        currentScreen = MobiScreen.ACTIONS_FILL_TUBING
                    },
                    navigateToFillCannula = {
                        currentScreen = MobiScreen.ACTIONS_FILL_CANNULA
                    },
                    navigateToSiteReminder = {
                        currentScreen = MobiScreen.ACTIONS_SITE_REMINDER
                    },
                    navigateBack = {
                        currentScreen = MobiScreen.ACTIONS
                    },
                )
            }


            MobiScreen.ACTIONS_CHANGE_CARTRIDGE -> {
                ChangeCartridgeScreen(
                    aapsLogger = aapsLogger,
                    sendPumpCommands = { messages -> tandemUiController.sendPumpCommands(messages) },
                    resourceHelper = resourceHelper,
                    showHeader = false,
                    refreshMainAppData = { data -> tandemUiController.refreshMainAppData(data) },
                    coreCartridgeActionsModel = coreCartridgeActionsModel,
                    navigateBack = {
                        currentScreen = MobiScreen.ACTIONS_CARTRIDGE_ACTIONS
                    },
                )
            }


            MobiScreen.ACTIONS_FILL_TUBING -> {
                FillTubingScreen(
                    aapsLogger = aapsLogger,
                    sendPumpCommands = { messages -> tandemUiController.sendPumpCommands(messages) },
                    resourceHelper = resourceHelper,
                    showHeader = false,
                    refreshMainAppData = { data -> tandemUiController.refreshMainAppData(data) },
                    coreCartridgeActionsModel = coreCartridgeActionsModel,
                    showSiteSelection = tandemPumpStatus.showSiteLocationStep,
                    navigateBack = {
                        currentScreen = MobiScreen.ACTIONS_CARTRIDGE_ACTIONS
                    },
                )
            }


            MobiScreen.ACTIONS_FILL_CANNULA -> {
                FillCannulaScreen(
                    aapsLogger = aapsLogger,
                    sendPumpCommands = { messages -> tandemUiController.sendPumpCommands(messages) },
                    resourceHelper = resourceHelper,
                    showHeader = false,
                    coreCartridgeActionsModel = coreCartridgeActionsModel,
                    navigateBack = {
                        currentScreen = MobiScreen.ACTIONS_CARTRIDGE_ACTIONS
                    }
                )
            }


            MobiScreen.ACTIONS_SITE_REMINDER -> {
                SiteReminder(
                    resourceHelper = resourceHelper,
                    aapsLogger = aapsLogger,
                    showHeader = false,
                    refreshMainAppData = { data -> tandemUiController.refreshMainAppData(data) },
                    pumpStatusData = tandemPumpStatus,
                    navigateBack = {
                        currentScreen = MobiScreen.ACTIONS_CARTRIDGE_ACTIONS
                    }
                )
            }

        }
    }
}
