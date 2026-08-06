@file:OptIn(ExperimentalMaterial3Api::class)

package app.aaps.pump.tandem.mobi.ui.actions.cartridge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.mobi.ui.actions.PumpStatusHeader
import app.aaps.pump.tandem.mobi.ui.util.Line
import app.aaps.pump.tandem.mobi.ui.util.intervalOf
import app.aaps.pump.tandem.mobi.ui.util.HeaderLineWithBackButton
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.comm.ui.CoreCartridgeActionsModel
import app.aaps.core.ui.R as Rco
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.HomeScreenMirrorRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.InsulinStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.LoadStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.TimeSinceResetRequest
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.LoadStatusResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class MenuAvailability(val enabled: Boolean, val disabledReason: String? = null)

private fun changeCartridgeAvailability(
    loadStatus: LoadStatusResponse?,
    resourceHelper: ResourceHelper,
): MenuAvailability {
    if (loadStatus?.isLoadingActive != true) return MenuAvailability(true)
    return when (loadStatus.loadState) {
        LoadStatusResponse.LoadState.LOAD_CARTRIDGE ->
            MenuAvailability(false, resourceHelper.gs(R.string.ca_disabled_load_then_fill_tubing))
        LoadStatusResponse.LoadState.PRIME_TUBING ->
            MenuAvailability(false, resourceHelper.gs(R.string.ca_disabled_fill_tubing_in_progress))
        LoadStatusResponse.LoadState.PRIME_CANNULA ->
            MenuAvailability(false, resourceHelper.gs(R.string.ca_disabled_fill_cannula_in_progress))
        else -> MenuAvailability(true)
    }
}

private fun fillTubingAvailability(
    loadStatus: LoadStatusResponse?,
    resourceHelper: ResourceHelper,
): MenuAvailability {
    if (loadStatus?.isLoadingActive != true) return MenuAvailability(true)
    return when (loadStatus.loadState) {
        LoadStatusResponse.LoadState.CHANGE_CARTRIDGE ->
            MenuAvailability(false, resourceHelper.gs(R.string.ca_disabled_change_in_progress))
        LoadStatusResponse.LoadState.PRIME_CANNULA ->
            MenuAvailability(false, resourceHelper.gs(R.string.ca_disabled_fill_cannula_in_progress))
        else -> MenuAvailability(true)
    }
}

private fun fillCannulaAvailability(
    loadStatus: LoadStatusResponse?,
    resourceHelper: ResourceHelper,
): MenuAvailability {
    if (loadStatus?.isLoadingActive != true) return MenuAvailability(true)
    return when (loadStatus.loadState) {
        LoadStatusResponse.LoadState.CHANGE_CARTRIDGE,
        LoadStatusResponse.LoadState.LOAD_CARTRIDGE ->
            MenuAvailability(false, resourceHelper.gs(R.string.ca_disabled_change_in_progress))
        LoadStatusResponse.LoadState.PRIME_TUBING ->
            MenuAvailability(false, resourceHelper.gs(R.string.ca_disabled_fill_tubing_in_progress))
        else -> MenuAvailability(true)
    }
}

@Composable
private fun GatedMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    availability: MenuAvailability,
    done: Boolean,
    onClick: () -> Unit,
) {
    val alpha = if (availability.enabled) 1f else 0.45f
    val itemColors = ListItemDefaults.colors(
        headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        leadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        supportingColor = MaterialTheme.colorScheme.error,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart)
    ) {
        ListItem(
            colors = itemColors,
            headlineContent = { Text(text = title) },
            supportingContent = {
                availability.disabledReason?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            },
            leadingContent = {
                Icon(icon, contentDescription = null)
            },
            trailingContent = if (done) {
                {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32) // Material Green 800
                    )
                }
            } else null,
            modifier = if (availability.enabled) Modifier.clickable { onClick() } else Modifier,
        )
    }
}

@Composable
fun CartridgeActions(
    innerPadding: PaddingValues = PaddingValues(),
    sendPumpCommands: (List<Message>) -> Boolean,
    resourceHelper: ResourceHelper,
    aapsLogger: AAPSLogger,
    navigateToChangeCartridge: () -> Unit,
    navigateToFillTubing: () -> Unit,
    navigateToFillCannula: () -> Unit,
    navigateToSiteReminder: () -> Unit,
    navigateBack: () -> Unit,
    coreCartridgeActionsModel: CoreCartridgeActionsModel,
    showHeader: Boolean = true
) {

    val ds = LocalTandemDataStore.current
    @Suppress("PropertyName")
    val TAG = LTag.PUMP

    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(true) }
    var showIncompleteExitDialog by remember { mutableStateOf(false) }

    fun refresh() = refreshScope.launch {
        aapsLogger.info(TAG, "reloading CartridgeActions with force")
        refreshing = true
        sendPumpCommands(cartridgeActionsCommands)
        withContext(Dispatchers.IO) { Thread.sleep(250) }
        refreshing = false
    }

    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        aapsLogger.info(TAG, "Initial LoadStatus poll on CartridgeActions")
        sendPumpCommands(listOf(LoadStatusRequest()))
        if (ds.requestInsulinLoadData.value == true) {
            coreCartridgeActionsModel.loadModelData()
            ds.requestInsulinLoadData.value = false
        }
    }

    LaunchedEffect(intervalOf(60)) {
        aapsLogger.info(TAG, "reloading CartridgeActions from interval")
        refresh()
    }

    val loadStatus = ds.loadStatus.observeAsState()
    val completedActions = ds.completedCartridgeActions.observeAsState()


    // Clear per-visit completion set on pop (forward nav only emits ON_STOP).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                ds.completedCartridgeActions.value = emptySet()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val changeCartridgeMenu = changeCartridgeAvailability(loadStatus.value, resourceHelper)
    val fillTubingMenu = fillTubingAvailability(loadStatus.value, resourceHelper)
    val fillCannulaMenu = fillCannulaAvailability(loadStatus.value, resourceHelper)

    // Block exit if cartridge changed but tubing not yet filled.
    val completedSet = completedActions.value ?: emptySet()
    val shouldWarnOnExit = CompletedCartridgeAction.CHANGE_CARTRIDGE in completedSet &&
        CompletedCartridgeAction.FILL_TUBING !in completedSet

    fun requestExit() {
        if (shouldWarnOnExit) {
            showIncompleteExitDialog = true
        } else {
            navigateBack()
        }
    }

    BackHandler { requestExit() }

    if (showIncompleteExitDialog) {
        AlertDialog(
            onDismissRequest = { showIncompleteExitDialog = false },
            title = { Text(resourceHelper.gs(R.string.ca_incomplete_load_warning_title)) },
            text = { Text(resourceHelper.gs(R.string.ca_incomplete_load_warning_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showIncompleteExitDialog = false
                    navigateBack()
                }) { Text(resourceHelper.gs(Rco.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showIncompleteExitDialog = false }) {
                    Text(resourceHelper.gs(R.string.common_cancel))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = refreshing,
                state = pullRefreshState,
                onRefresh = { refresh() })
    ) {
        PullToRefreshDefaults.Indicator(
            isRefreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f)
        )

        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp),
            content = {
                if (showHeader) {
                    item {
                        HeaderLineWithBackButton(
                            text = resourceHelper.gs(R.string.ca_label),
                            onBackClick = ::requestExit,
                            resourceHelper = resourceHelper
                        )
                        HorizontalDivider()
                    }
                }

                item {
                    PumpStatusHeader(resourceHelper = resourceHelper)
                }

                item {
                    GatedMenuItem(
                        title = resourceHelper.gs(R.string.cc_title),
                        icon = Icons.Filled.Settings,
                        availability = changeCartridgeMenu,
                        done = CompletedCartridgeAction.CHANGE_CARTRIDGE in (completedActions.value ?: emptySet()),
                        onClick = {
                            refreshScope.launch {
                                ds.enterChangeCartridgeState.value = null
                                ds.detectingCartridgeState.value = null
                                sendPumpCommands(listOf(TimeSinceResetRequest()))
                                navigateToChangeCartridge()
                            }
                        }
                    )
                }

                item {
                    GatedMenuItem(
                        title = resourceHelper.gs(R.string.ft_title),
                        icon = Icons.Filled.Settings,
                        availability = fillTubingMenu,
                        done = CompletedCartridgeAction.FILL_TUBING in (completedActions.value ?: emptySet()),
                        onClick = {
                            refreshScope.launch {
                                ds.fillTubingState.value = null
                                ds.exitFillTubingState.value = null
                                ds.inFillTubingMode.value = false
                                sendPumpCommands(listOf(TimeSinceResetRequest()))
                                navigateToFillTubing()
                            }
                        }
                    )
                }

                item {
                    GatedMenuItem(
                        title = resourceHelper.gs(R.string.fc_title),
                        icon = Icons.Filled.Settings,
                        availability = fillCannulaMenu,
                        done = CompletedCartridgeAction.FILL_CANNULA in (completedActions.value ?: emptySet()),
                        onClick = {
                            refreshScope.launch {
                                ds.fillCannulaState.value = null
                                sendPumpCommands(listOf(TimeSinceResetRequest()))
                                navigateToFillCannula()
                            }
                        }
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.TopStart)
                    ) {
                        ListItem(
                            headlineContent = { Text(
                                text = resourceHelper.gs(R.string.sr_title)
                            )},
                            supportingContent = {
                            },
                            leadingContent = {
                                Icon(Icons.Filled.Info, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                navigateToSiteReminder()
                            }
                        )
                    }
                }

                item {
                    Line("\n")
                }
            }
        )
    }
}

val cartridgeActionsCommands = listOf(
    HomeScreenMirrorRequest(),
    TimeSinceResetRequest(),
    LoadStatusRequest(),
    InsulinStatusRequest()
)

//
// @Preview(showBackground = true)
// @Composable
// private fun DefaultPreview() {
//     MaterialTheme() {
//         Surface(
//             modifier = Modifier.fillMaxSize(),
//             color = Color.White,
//         ) {
//             setUpPreviewState(LocalTandemDataStore.current)
//             CartridgeActions(
//                 sendPumpCommands = { _ -> true},
//                 navigateBack = {},
//                 navigateToChangeCartridge = {},
//                 navigateToFillTubing = {},
//                 navigateToFillCannula = {},
//                 navigateToSiteReminder = {},
//                 resourceHelper = ResourceHelperTest(),
//                 aapsLogger = AAPSLoggerTest()
//             )
//         }
//     }
// }
//
// @Preview(showBackground = true)
// @Composable
// private fun DefaultPreviewChangeCartridge_InsulinNotStopped() {
//     MaterialTheme() {
//         Surface(
//             modifier = Modifier.fillMaxSize(),
//             color = Color.White,
//         ) {
//             setUpPreviewState(LocalTandemDataStore.current)
//             CartridgeActions(
//                 sendPumpCommands = { _ -> true},
//                 navigateBack = {},
//                 navigateToChangeCartridge = {},
//                 navigateToFillTubing = {},
//                 navigateToFillCannula = {},
//                 navigateToSiteReminder = {},
//                 resourceHelper = ResourceHelperTest(),
//                 aapsLogger = AAPSLoggerTest()
//             )
//         }
//     }
// }
//
// @Preview(showBackground = true)
// @Composable
// private fun DefaultPreviewChangeCartridge_InsulinStopped() {
//     MaterialTheme() {
//         Surface(
//             modifier = Modifier.fillMaxSize(),
//             color = Color.White,
//         ) {
//             setUpPreviewState(LocalTandemDataStore.current)
//             CartridgeActions(
//                 sendPumpCommands = { _ -> true},
//                 navigateBack = {},
//                 navigateToChangeCartridge = {},
//                 navigateToFillTubing = {},
//                 navigateToFillCannula = {},
//                 navigateToSiteReminder = {},
//                 resourceHelper = ResourceHelperTest(),
//                 aapsLogger = AAPSLoggerTest()
//             )
//         }
//     }
// }
