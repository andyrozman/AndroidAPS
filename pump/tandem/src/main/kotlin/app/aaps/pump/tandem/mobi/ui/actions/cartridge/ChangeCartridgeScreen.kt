@file:OptIn(ExperimentalMaterial3Api::class)

package app.aaps.pump.tandem.mobi.ui.actions.cartridge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.common.test.ResourceHelperTest
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.data.defs.RefreshData
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.mobi.ui.actions.setUpPreviewState

import app.aaps.pump.tandem.mobi.ui.util.intervalOf
import app.aaps.shared.tests.AAPSLoggerTest
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.control.EnterChangeCartridgeModeRequest
import com.jwoglom.pumpx2.pump.messages.request.control.ExitChangeCartridgeModeRequest
import com.jwoglom.pumpx2.pump.messages.request.control.SuspendPumpingRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlarmStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlertStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.HomeScreenMirrorRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.LoadStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.TimeSinceResetRequest
import com.jwoglom.pumpx2.pump.messages.response.controlStream.EnterChangeCartridgeModeStateStreamResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChangeCartridgeScreen(
    innerPadding: PaddingValues = PaddingValues(),
    sendPumpCommands: (List<Message>) -> Boolean,
    resourceHelper: ResourceHelper,
    aapsLogger: AAPSLogger,
    navigateBack: () -> Unit,
    refreshMainAppData: (RefreshData) -> Unit,
    showHeader: Boolean = true
) {
    val ds = LocalTandemDataStore.current
    @Suppress("PropertyName")
    val TAG = LTag.PUMP

    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(true) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showSuspendDialog by remember { mutableStateOf(false) }
    var isSuspending by remember { mutableStateOf(false) }
    var isStartingChangeCartridge by remember { mutableStateOf(false) }

    fun refresh() = refreshScope.launch {
        aapsLogger.info(TAG, "reloading ChangeCartridgeScreen with force")
        refreshing = true
        sendPumpCommands(changeCartridgeScreenCommands)
        withContext(Dispatchers.IO) { Thread.sleep(250) }
        refreshing = false
    }

    LaunchedEffect(intervalOf(60)) {
        aapsLogger.info(TAG, "reloading ChangeCartridgeScreen from interval")
        refresh()
    }

    LaunchedEffect(Unit) {
        aapsLogger.info(TAG, "Initial alert/alarm poll on ChangeCartridgeScreen")
        sendPumpCommands(listOf(AlertStatusRequest(), AlarmStatusRequest()))
    }

    LaunchedEffect(intervalOf(10)) {
        aapsLogger.info(TAG, "Periodic alert/alarm poll on ChangeCartridgeScreen")
        sendPumpCommands(listOf(AlertStatusRequest(), AlarmStatusRequest()))
    }

    val pumpRunningState = ds.pumpRunningState.observeAsState()
    val inChangeCartridgeMode = ds.inChangeCartridgeMode.observeAsState()
    val enterChangeCartridgeState = ds.enterChangeCartridgeState.observeAsState()
    val detectingCartridgeState = ds.detectingCartridgeState.observeAsState()

    val notificationBundle = ds.notificationBundle.observeAsState()
    val notifications: List<Any> = notificationBundle.value?.get()?.toList() ?: emptyList()

    fun sendPumpCommand(msg: Message) {
        sendPumpCommands(listOf(msg))
    }

    val isInActiveMode = inChangeCartridgeMode.value == true &&
        detectingCartridgeState.value?.isComplete != true
    val hasActiveNotifications = notifications.isNotEmpty()

    fun requestCancelOrBack() {
        if (isInActiveMode) {
            showCancelDialog = true
        } else {
            navigateBack()
        }
    }

    BackHandler { requestCancelOrBack() }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(resourceHelper.gs(R.string.cc_cancel_confirm_title)) },
            text = { Text(resourceHelper.gs(R.string.cc_cancel_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    refreshScope.launch {
                        sendPumpCommand(ExitChangeCartridgeModeRequest())
                        navigateBack()
                    }
                }) { Text(resourceHelper.gs(R.string.common_cancel)) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(resourceHelper.gs(R.string.common_continue))
                }
            }
        )
    }

    if (showSuspendDialog) {
        AlertDialog(
            onDismissRequest = { showSuspendDialog = false },
            title = { Text(resourceHelper.gs(R.string.ca_suspend_confirm_title)) },
            text = { Text(resourceHelper.gs(R.string.ca_suspend_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showSuspendDialog = false
                    isSuspending = true
                    sendPumpCommand(SuspendPumpingRequest())
                    refreshScope.launch {
                        repeat(5) {
                            if (pumpRunningState.value == PumpRunningState.Suspended) {
                                return@repeat
                            }
                            withContext(Dispatchers.IO) { Thread.sleep(1000) }
                            sendPumpCommand(HomeScreenMirrorRequest())
                        }
                        isSuspending = false
                    }
                }) { Text(resourceHelper.gs(R.string.ca_btn_suspend_insulin)) }
            },
            dismissButton = {
                TextButton(onClick = { showSuspendDialog = false }) {
                    Text(resourceHelper.gs(R.string.common_cancel))
                }
            }
        )
    }

    val totalSteps = 4
    val currentStep = when {
        detectingCartridgeState.value?.isComplete == true -> 4
        detectingCartridgeState.value != null -> 3
        enterChangeCartridgeState.value?.state == EnterChangeCartridgeModeStateStreamResponse.ChangeCartridgeState.READY_TO_CHANGE -> 2
        inChangeCartridgeMode.value == true -> 1
        else -> 1
    }

    CartridgeWorkflowScreen(
        title = resourceHelper.gs(R.string.cc_title),
        innerPadding = innerPadding,
        refreshing = refreshing,
        onRefresh = { refresh() },
        onBack = ::requestCancelOrBack,
        resourceHelper = resourceHelper,
        showHeader = showHeader,
        stepIndicator = {
            WizardStepIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps,
                resourceHelper = resourceHelper,
            )
        },
        notifications = notifications,
        sendPumpCommands = sendPumpCommands,
        refreshScope = refreshScope,
        body = {
            if (detectingCartridgeState.value != null) {
                Text(
                    text = resourceHelper.gs(R.string.ca_status_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (detectingCartridgeState.value?.isComplete == true) {
                    Text(
                        text = resourceHelper.gs(R.string.cc_complete),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    refreshMainAppData(RefreshData.PUMP_CANNULA_CHANGED)
                } else {
                    Text(
                        text = resourceHelper.gs(R.string.cc_detect_insulin_cart),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = resourceHelper.gs(R.string.ca_progress_heading),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resourceHelper.gs(R.string.common_percent_complete, "${detectingCartridgeState.value?.percentComplete}"),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (enterChangeCartridgeState.value?.state == EnterChangeCartridgeModeStateStreamResponse.ChangeCartridgeState.READY_TO_CHANGE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resourceHelper.gs(R.string.cc_can_remove_cart),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = resourceHelper.gs(R.string.cc_when_inserted_press),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else if (inChangeCartridgeMode.value == true) {
                Text(
                    text = resourceHelper.gs(R.string.ca_status_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resourceHelper.gs(R.string.cc_preparing_cc),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else if (pumpRunningState.value == PumpRunningState.Suspended) {
                Text(
                    text = resourceHelper.gs(R.string.ca_before_you_start_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resourceHelper.gs(R.string.ca_disconnect_pump_from_site, resourceHelper.gs(R.string.cc_btn_begin)),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = resourceHelper.gs(R.string.ca_before_you_start_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resourceHelper.gs(R.string.ca_before_stop_delivery, resourceHelper.gs(R.string.cc_action)),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        actions = {
            if (detectingCartridgeState.value?.isComplete == true) {
                PrimaryActionButton(
                    text = resourceHelper.gs(R.string.common_done),
                    onClick = {
                        ds.completedCartridgeActions.value =
                            setOf(CompletedCartridgeAction.CHANGE_CARTRIDGE)
                        ds.loadStatus.value = null
                        refreshScope.launch { navigateBack() }
                    }
                )
            } else if (enterChangeCartridgeState.value?.state == EnterChangeCartridgeModeStateStreamResponse.ChangeCartridgeState.READY_TO_CHANGE) {
                PrimaryActionButton(
                    text = resourceHelper.gs(R.string.cc_btn_cart_inserted),
                    onClick = {
                        refreshScope.launch {
                            sendPumpCommand(ExitChangeCartridgeModeRequest())
                        }
                    }
                )
            } else if (inChangeCartridgeMode.value != true) {
                if (pumpRunningState.value != PumpRunningState.Suspended) {
                    SecondaryActionButton(
                        text = resourceHelper.gs(R.string.ca_btn_suspend_insulin),
                        onClick = { showSuspendDialog = true },
                        enabled = !hasActiveNotifications,
                        loading = isSuspending
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                PrimaryActionButton(
                    text = resourceHelper.gs(R.string.cc_btn_begin),
                    onClick = {
                        isStartingChangeCartridge = true
                        sendPumpCommand(EnterChangeCartridgeModeRequest())
                        refreshScope.launch {
                            // 5s watchdog so the spinner doesn't stick.
                            repeat(5) {
                                if (inChangeCartridgeMode.value == true) return@repeat
                                withContext(Dispatchers.IO) { Thread.sleep(1000) }
                            }
                            isStartingChangeCartridge = false
                        }
                    },
                    enabled = pumpRunningState.value == PumpRunningState.Suspended && !hasActiveNotifications,
                    loading = isStartingChangeCartridge
                )
            }
        }
    )
}

val changeCartridgeScreenCommands = listOf(
    HomeScreenMirrorRequest(),
    TimeSinceResetRequest(),
    LoadStatusRequest()
)

@Preview(showBackground = true)
@Composable
private fun ChangeCartridgeScreenPreview() {
    MaterialTheme() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)
            ChangeCartridgeScreen(
                sendPumpCommands = { _ -> true },
                navigateBack = {},
                resourceHelper = ResourceHelperTest(),
                aapsLogger = AAPSLoggerTest(),
                refreshMainAppData = {}
            )
        }
    }
}
