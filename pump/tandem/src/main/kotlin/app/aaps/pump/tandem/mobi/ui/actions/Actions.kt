@file:OptIn(ExperimentalMaterial3Api::class)

package app.aaps.pump.tandem.mobi.ui.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.common.test.ResourceHelperTest
import app.aaps.pump.tandem.common.comm.ui.TandemUIDataStore
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.data.defs.RefreshData
import app.aaps.core.ui.R as Rco
import app.aaps.pump.tandem.mobi.ui.util.HeaderLine
import app.aaps.pump.tandem.mobi.ui.util.LifecycleStateObserver
import app.aaps.pump.tandem.mobi.ui.util.intervalOf
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.common.driver.tandemUiDataStore
import app.aaps.pump.tandem.mobi.ui.util.compactTBRDisplay
import app.aaps.shared.tests.AAPSLoggerTest
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.control.StopTempRateRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.HomeScreenMirrorRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.InsulinStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.LoadStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.TempRateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant


@Composable
fun Actions(
    innerPadding: PaddingValues = PaddingValues(),
    sendPumpCommands: (List<Message>) -> Boolean,
    _stopTempRateMenuState: Boolean = false,
    aapsLogger: AAPSLogger,
    resourceHelper: ResourceHelper,
    navigateToCartridgeActions: () -> Unit,
    navigateToPumpInfo: () -> Unit,
    refreshMainAppData: (RefreshData) -> Unit,
    showHeader: Boolean = true
) {

    var showStartStopInsulinSheet by remember { mutableStateOf(false) }
    var showStopTempRateMenu by remember { mutableStateOf(_stopTempRateMenuState) }

    val ds = LocalTandemDataStore.current
    @Suppress("LocalVariable")
    val TAG = LTag.PUMPCOMM

    val tempRateActive = ds.tempRateActive.observeAsState()
    val tempRateDetails = ds.tempRateDetails.observeAsState()
    val pumpRunningState = ds.pumpRunningState.observeAsState()

    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(true) }

    fun fetchDataStoreFields() {
        sendPumpCommands(actionsCommands)
    }

    fun waitForLoaded() = refreshScope.launch {

        var sinceLastFetchTime = 0
        while (true) {
            val nullFields = actionsFields.filter { field -> field.value == null }.toSet()
            if (nullFields.isEmpty()) {
                break
            }

            aapsLogger.debug(TAG, "Actions loading: remaining ${nullFields.size}: ${actionsFields.map { it.value }}")
            if (sinceLastFetchTime >= 2500) {
                aapsLogger.debug(TAG, "Actions loading re-fetching with cache")
                fetchDataStoreFields()
                sinceLastFetchTime = 0
            }

            withContext(Dispatchers.IO) {
                Thread.sleep(250)
            }
            sinceLastFetchTime += 250
        }
        aapsLogger.debug(TAG, "Actions loading done: ${actionsFields.map { it.value }}")
        refreshing = false
    }

    fun refresh() = refreshScope.launch {

        aapsLogger.debug(TAG, "reloading Actions with force")
        refreshing = true

        actionsFields.forEach { field -> field.value = null }
        fetchDataStoreFields()
    }

    //val state = rememberPullRefreshState(refreshing, ::refresh)
    val pullRefreshState = rememberPullToRefreshState()

// LocalLifecycleOwner.current
    LifecycleStateObserver(lifecycleOwner = LocalLifecycleOwner.current, onStop = {
        refreshScope.cancel()
    }) {
        aapsLogger.debug(TAG, "reloading Actions from onStart lifecyclestate")
        fetchDataStoreFields()
    }

    LaunchedEffect(intervalOf(60)) {
        aapsLogger.debug(TAG, "reloading Actions from interval")
        fetchDataStoreFields()
    }

    LaunchedEffect(refreshing) {
        waitForLoaded()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(isRefreshing = refreshing,
                           state= pullRefreshState,
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
                        HeaderLine(resourceHelper.gs(R.string.ui_a_title))
                    }
                }
                item {
                    PumpStatusHeader(resourceHelper = resourceHelper)
                }
                item {
                    ListItem(
                        headlineContent = { Text(
                            when (pumpRunningState.value) {
                                PumpRunningState.Unknown, null  -> resourceHelper.gs(R.string.ca_stop_start_insulin)
                                PumpRunningState.Suspended -> resourceHelper.gs(R.string.ca_start_insulin)
                                else                       -> resourceHelper.gs(R.string.ca_stop_insulin)
                            }
                        )},
                        supportingContent = { Text(
                            when (pumpRunningState.value) {
                                PumpRunningState.Unknown, null  -> resourceHelper.gs(R.string.ca_stop_resume_insulin_deliveries)
                                PumpRunningState.Suspended -> resourceHelper.gs(R.string.ca_resume_insulin_deliveries)
                                else                       -> resourceHelper.gs(R.string.ca_stop_insulin_deliveries)
                            }
                        ) },
                        leadingContent = {
                            Icon(
                                when (pumpRunningState.value) {
                                    PumpRunningState.Unknown, null  -> Icons.Filled.Close
                                    PumpRunningState.Suspended -> Icons.Filled.PlayArrow
                                    else                       -> Icons.Filled.Close
                                },
                                contentDescription = null,
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = when (pumpRunningState.value) {
                                PumpRunningState.Unknown, null  -> ListItemDefaults.containerColor
                                PumpRunningState.Suspended -> Color.Green.copy(alpha = 0.5F)
                                else                       -> Color.Red.copy(alpha = 0.5F)
                            }
                        ),
                        modifier = Modifier.clickable {
                            showStartStopInsulinSheet = true
                        }
                    )
                }


                item {
                    HorizontalDivider()
                }


                if (tempRateActive.value==true) {

                    item {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.TopStart)
                        ) {
                            fun prettyDuration(minutes: Long?): String {
                                return resourceHelper.gs(R.string.ca_hours_minutes_short,
                                                         minutes?.div(60),
                                                         minutes?.rem(60))
                            }

                            ListItem(
                                headlineContent = { Text(
                                    when (tempRateActive.value) {
                                        true -> resourceHelper.gs(R.string.ca_stop_temp_rate)
                                        else -> resourceHelper.gs(R.string.ca_start_temp_rate)
                                    }
                                )},
                                supportingContent =  {
                                    when (tempRateActive.value) {
                                        true -> Text(resourceHelper.gs(R.string.ca_active,
                                                                       compactTBRDisplay(tempRateResponse = tempRateDetails.value, resourceHelper = resourceHelper)))
                                        else -> null
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        when (tempRateActive.value) {
                                            true -> Icons.Filled.Close
                                            else -> Icons.Filled.Settings
                                        },
                                        contentDescription = null,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    when (tempRateActive.value) {
                                        true -> { showStopTempRateMenu = true }
                                        //false -> { openTempRateWindow() }
                                        else -> {}
                                    }
                                }
                            )
                            DropdownMenu(
                                expanded = showStopTempRateMenu,
                                onDismissRequest = { showStopTempRateMenu = false },
                                modifier = Modifier.fillMaxWidth(),
                            ) {

                                AlertDialog(
                                    onDismissRequest = { showStopTempRateMenu = false },
                                    title = {
                                        Text(resourceHelper.gs(R.string.ca_stop_temp_rate))
                                    },
                                    text = {
                                        Text(resourceHelper.gs(R.string.ca_stop_active_temp_rate,
                                                               tempRateDetails.value?.percentage,
                                                               "${prettyDuration(tempRateDetails.value?.duration?.div(60))}",
                                                               "${tempRateDetails.value?.startTimeInstant}"))
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                showStopTempRateMenu = false
                                            },
                                            modifier = Modifier.padding(top = 16.dp)
                                        ) {
                                            Text(resourceHelper.gs(Rco.string.cancel))
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                refreshScope.launch {
                                                    showStopTempRateMenu = false
                                                    sendPumpCommands(listOf(StopTempRateRequest()))
                                                    withContext(Dispatchers.IO) {
                                                        Thread.sleep(250)
                                                    }
                                                    sendPumpCommands(listOf(TempRateRequest()))
                                                }
                                            },
                                            modifier = Modifier.padding(top = 16.dp)
                                        ) {
                                            Text(resourceHelper.gs(R.string.ca_stop_temp_rate))
                                        }
                                    }
                                )

                            }
                        }
                    }  // TBR
                }


                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.TopStart)
                    ) {
                        ListItem(
                            headlineContent = { Text(
                                resourceHelper.gs(R.string.ca_cartridge_cannula_setup)
                            )},
                            supportingContent = {
                            },
                            leadingContent = {
                                Icon(Icons.Filled.Settings, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                navigateToCartridgeActions()
                            }
                        )
                    }
                }


                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.TopStart)
                    ) {
                        ListItem(
                            headlineContent = { Text(
                                resourceHelper.gs(R.string.ca_pump_info)
                            )},
                            supportingContent = {
                            },
                            leadingContent = {
                                Icon(Icons.Filled.Info, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                navigateToPumpInfo()
                            }
                        )
                    }
                }

            }
        )
    }

    if (showStartStopInsulinSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStartStopInsulinSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            StartStopInsulinScreen(
                sendPumpCommands = sendPumpCommands,
                resourceHelper = resourceHelper,
                aapsLogger = aapsLogger,
                refreshMainAppData = refreshMainAppData,
                navigateBack = { showStartStopInsulinSheet = false }
            )
        }
    }
}
val actionsCommands = listOf(
    HomeScreenMirrorRequest(),
    TempRateRequest(),
    InsulinStatusRequest(),
    LoadStatusRequest()
)

val actionsFields = listOf(
    tandemUiDataStore.pumpRunningState,
    tandemUiDataStore.tempRateActive,
    tandemUiDataStore.tempRateDetails,
)

@Preview(showBackground = true)
@Composable
private fun PreviewInsulinActive() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)
            Actions(
                sendPumpCommands = { _ -> true},
                aapsLogger = AAPSLoggerTest(),
                navigateToCartridgeActions = {},
                resourceHelper = ResourceHelperTest(),
                navigateToPumpInfo = {},
                refreshMainAppData = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewInsulinSuspended() {
    MaterialTheme() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)
            LocalTandemDataStore.current.pumpRunningState.value = PumpRunningState.Suspended
            Actions(
                sendPumpCommands = { _ -> true},
                aapsLogger = AAPSLoggerTest(),
                navigateToCartridgeActions = {},
                navigateToPumpInfo = {},
                resourceHelper = ResourceHelperTest(),
                refreshMainAppData = {}
            )
        }
    }
}


fun setUpPreviewState(ds: TandemUIDataStore) {
    ds.pumpConnected.value = true
    ds.pumpLastConnectionTimestamp.value = Instant.now().minusSeconds(120)
}
