@file:OptIn(ExperimentalMaterial3Api::class)

package app.aaps.pump.tandem.mobi.ui.actions.cartridge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.ui.compose.AapsSpacing
import app.aaps.core.ui.compose.pump.StepProgressIndicator

import app.aaps.pump.common.test.ResourceHelperTest
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.comm.ui.CoreCartridgeActionsModel
import app.aaps.pump.tandem.common.comm.ui.CoreCartridgeActionsModelInterface
import app.aaps.pump.tandem.common.comm.ui.CoreCartridgeActionsModelTest
import app.aaps.core.ui.R as Rco
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.mobi.ui.actions.setUpPreviewState

import app.aaps.pump.tandem.mobi.ui.util.DecimalOutlinedText
import app.aaps.pump.tandem.mobi.ui.util.intervalOf
import app.aaps.shared.tests.AAPSLoggerTest
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.models.InsulinUnit
import com.jwoglom.pumpx2.pump.messages.request.control.FillCannulaRequest
import com.jwoglom.pumpx2.pump.messages.request.control.ResumePumpingRequest
import com.jwoglom.pumpx2.pump.messages.request.control.SuspendPumpingRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlarmStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlertStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.HomeScreenMirrorRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.LoadStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.TimeSinceResetRequest
import com.jwoglom.pumpx2.pump.messages.response.controlStream.FillCannulaStateStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HomeScreenMirrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FillCannulaScreen(
    innerPadding: PaddingValues = PaddingValues(),
    sendPumpCommands: (List<Message>) -> Boolean,
    resourceHelper: ResourceHelper,
    aapsLogger: AAPSLogger,
    navigateBack: () -> Unit,
    showHeader: Boolean = true,
    onStepChanged: (Int) -> Unit,
    coreCartridgeActionsModel: CoreCartridgeActionsModelInterface
) {
    val ds = LocalTandemDataStore.current
    @Suppress("PropertyName")
    val TAG = LTag.PUMP

    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(true) }
    var cannulaFillAmountStr by remember { mutableStateOf<String?>(null) }
    var cannulaFillAmount by remember { mutableStateOf<Double?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showSuspendDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var showExitWithoutResumeDialog by remember { mutableStateOf(false) }
    var isSuspending by remember { mutableStateOf(false) }
    var isResuming by remember { mutableStateOf(false) }

    fun allowedCannulaFillAmount(units: Double?): Boolean {
        return units != null && units > 0 && units <= 3.0
    }

    fun sendPumpCommand(msg: Message) {
        sendPumpCommands(listOf(msg))
    }

    fun refresh() = refreshScope.launch {
        aapsLogger.info(TAG, "reloading FillCannulaScreen with force")
        refreshing = true
        sendPumpCommands(fillCannulaScreenCommands)
        withContext(Dispatchers.IO) { Thread.sleep(250) }
        refreshing = false
    }

    LaunchedEffect(intervalOf(60)) {
        aapsLogger.info(TAG, "reloading FillCannulaScreen from interval")
        refresh()
    }

    LaunchedEffect(Unit) {
        aapsLogger.info(TAG, "Initial alert/alarm poll on FillCannulaScreen")
        // Mirror FIRST so pumpSuspended reflects reality before the buttons enable. The
        // cached fillCannulaState is intentionally KEPT: re-entering this screen after a
        // completed fill should still show the "filled" state.
        sendPumpCommands(listOf(HomeScreenMirrorRequest(), AlertStatusRequest(), AlarmStatusRequest()))
    }

    LaunchedEffect(intervalOf(10)) {
        aapsLogger.info(TAG, "Periodic alert/alarm poll on FillCannulaScreen")
        sendPumpCommands(listOf(AlertStatusRequest(), AlarmStatusRequest()))
    }

    val fillCannulaState = ds.fillCannulaState.observeAsState()
    val mirrorBasalStatus = ds.mirrorBasalStatus.observeAsState()

    // Delivery state shown by the buttons - newest source wins:
    //  1. commands on this screen set it optimistically (instant feedback),
    //  2. HomeScreenMirror samples are ground truth and correct it, but only outside the
    //     short grace window right after a command (a mirror captured before the command
    //     took effect must not flip the UI back).
    var uiSuspended by remember { mutableStateOf<Boolean?>(null) }
    var uiSetAtMs by remember { mutableStateOf(0L) }
    val mirrorIcon = mirrorBasalStatus.value

    LaunchedEffect(mirrorIcon) {
        val m = mirrorIcon ?: return@LaunchedEffect
        val inGrace = uiSuspended != null && System.currentTimeMillis() - uiSetAtMs < MIRROR_GRACE_MS
        if (!inGrace) {
            uiSuspended = m == HomeScreenMirrorResponse.BasalStatusIcon.SUSPEND
            uiSetAtMs = System.currentTimeMillis()
        }
    }

    val pumpSuspended = uiSuspended == true
    val deliveryKnown = uiSuspended != null

    fun setDeliverySuspended(v: Boolean) {
        uiSuspended = v
        uiSetAtMs = System.currentTimeMillis()
    }

    val notificationBundle = ds.notificationBundle.observeAsState()
    val notifications: List<Any> = notificationBundle.value?.get()?.toList() ?: emptyList()

    val isInActiveMode = fillCannulaState.value != null &&
        fillCannulaState.value?.state != FillCannulaStateStreamResponse.FillCannulaState.CANNULA_FILLED
    val hasActiveNotifications = notifications.isNotEmpty()

    fun requestCancelOrBack() {
        if (isInActiveMode) {
            showCancelDialog = true
        } else {
            aapsLogger.info(TAG, "FC-NAV: requestCancelOrBack -> navigateBack (not active mode)")
            navigateBack()
        }
    }

    BackHandler { requestCancelOrBack() }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(resourceHelper.gs(R.string.fc_cancel_confirm_title)) },
            text = { Text(resourceHelper.gs(R.string.fc_cancel_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    navigateBack()
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
                    setDeliverySuspended(true)
                    isSuspending = true
                    sendPumpCommand(SuspendPumpingRequest())
                    refreshScope.launch {
                        // Mirror-confirmed suspend - same ground truth as the resume path
                        // (basalStatusIcon=SUSPEND), not the ACK-derived pumpRunningState: both
                        // are mirror-fed today, but the raw icon cannot silently regress to
                        // ACK-trust if that derivation changes.
                        var suspendedConfirmed = false
                        repeat(5) {
                            if (mirrorBasalStatus.value == HomeScreenMirrorResponse.BasalStatusIcon.SUSPEND) {
                                suspendedConfirmed = true
                                return@repeat
                            }
                            withContext(Dispatchers.IO) { Thread.sleep(1000) }
                            sendPumpCommand(HomeScreenMirrorRequest())
                        }
                        isSuspending = false
                        if (!suspendedConfirmed) {
                            aapsLogger.error(TAG, "FC-NAV: suspend NOT confirmed by mirror within 5s - optimistic UI state stands until next mirror sample")
                        }
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

    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text(resourceHelper.gs(R.string.ca_resume_confirm_title)) },
            text = { Text(resourceHelper.gs(R.string.ca_resume_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showResumeDialog = false
                    isResuming = true
                    refreshScope.launch {
                        // Verified resume: ResumePumpingResponse can ACK success while the
                        // pump stays suspended (observed 2026-08-25). HomeScreenMirror's
                        // basalStatusIcon is the ground truth - BUT note the loop may be
                        // running a zero-temp, in which case the icon is ZERO_TEMP_RATE,
                        // not BASAL. Anything != SUSPEND means delivering.
                        var resumed = false
                        repeat(3) {
                            if (resumed) return@repeat
                            sendPumpCommand(ResumePumpingRequest())
                            repeat(6) {
                                withContext(Dispatchers.IO) { Thread.sleep(1000) }
                                sendPumpCommand(HomeScreenMirrorRequest())
                                val icon = mirrorBasalStatus.value
                                if (icon != null && icon != HomeScreenMirrorResponse.BasalStatusIcon.SUSPEND) {
                                    resumed = true
                                    return@repeat
                                }
                            }
                        }
                        isResuming = false
                        if (resumed) {
                            ds.completedCartridgeActions.value =
                                (ds.completedCartridgeActions.value ?: emptySet()) +
                                    CompletedCartridgeAction.FILL_CANNULA
                            ds.loadStatus.value = null
                            aapsLogger.info(TAG, "FC-NAV: resume VERIFIED -> navigateBack")
                            setDeliverySuspended(false)
                            navigateBack()
                        } else {
                            aapsLogger.error(TAG, "FC-NAV: resume NOT verified after retries - showing exit warning")
                            // Delivery did not resume - keep the screen open with the
                            // exit warning so this cannot silently pass.
                            showExitWithoutResumeDialog = true
                        }
                    }
                }) { Text(resourceHelper.gs(R.string.ca_btn_resume_insulin)) }
            },
            dismissButton = {
                TextButton(onClick = { showResumeDialog = false }) {
                    Text(resourceHelper.gs(R.string.common_cancel))
                }
            }
        )
    }

    if (showExitWithoutResumeDialog) {
        AlertDialog(
            onDismissRequest = { showExitWithoutResumeDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            text = {
                Text(
                    text = resourceHelper.gs(R.string.fc_exit_without_resume_body),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitWithoutResumeDialog = false
                    ds.completedCartridgeActions.value =
                        (ds.completedCartridgeActions.value ?: emptySet()) +
                            CompletedCartridgeAction.FILL_CANNULA
                    ds.loadStatus.value = null
                    navigateBack()
                }) { Text(resourceHelper.gs(R.string.fc_btn_exit_anyway)) }
            },
            dismissButton = {
                TextButton(onClick = { showExitWithoutResumeDialog = false }) {
                    Text(resourceHelper.gs(R.string.common_cancel))
                }
            }
        )
    }

    val totalSteps = 3
    val currentStep = when {
        fillCannulaState.value?.state == FillCannulaStateStreamResponse.FillCannulaState.CANNULA_FILLED -> 3
        fillCannulaState.value != null -> 2
        else -> 1
    }

    CartridgeWorkflowScreen(
        title = resourceHelper.gs(R.string.fc_title),
        innerPadding = innerPadding,
        refreshing = refreshing,
        onRefresh = { refresh() },
        onBack = ::requestCancelOrBack,
        resourceHelper = resourceHelper,
        showHeader = showHeader,
        showBack = (currentStep == 1),
        onStepChanged = onStepChanged,
        currentStep = currentStep,
        stepIndicator = {
            StepProgressIndicator(
                currentStep = currentStep - 1,
                totalSteps = totalSteps //,
                //resourceHelper = resourceHelper,
            )
        },
        notifications = notifications,
        sendPumpCommands = sendPumpCommands,
        coreCartridgeActionsModel = coreCartridgeActionsModel,
        refreshScope = refreshScope,
        body = {
            if (fillCannulaState.value?.state == FillCannulaStateStreamResponse.FillCannulaState.CANNULA_FILLED) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = resourceHelper.gs(R.string.fc_complete),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = resourceHelper.gs(R.string.fc_setup_complete_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (fillCannulaState.value != null) {
                Text(
                    text = resourceHelper.gs(R.string.ca_status_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resourceHelper.gs(R.string.fc_filling_with, cannulaFillAmount),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = resourceHelper.gs(R.string.fc_filling_state, fillCannulaState.value?.stateId),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (pumpSuspended) {
                Text(
                    text = resourceHelper.gs(R.string.ca_before_you_start_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resourceHelper.gs(R.string.fc_cannula_fill_amount),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                DecimalOutlinedText(
                    title = resourceHelper.gs(R.string.fc_fill_amount),
                    value = cannulaFillAmountStr,
                    decimalPlaces = 1,
                    onValueChange = {
                        cannulaFillAmountStr = it
                        cannulaFillAmount = when {
                            it == "" -> null
                            else -> {
                                val d = it.toDoubleOrNull()
                                if (!allowedCannulaFillAmount(d)) {
                                    null
                                } else {
                                    d
                                }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = (cannulaFillAmount ?: 0.0).toFloat(),
                    onValueChange = { v ->
                        // Round to 0.1 step
                        val rounded = (Math.round(v * 10).toDouble() / 10.0)
                        cannulaFillAmount = if (allowedCannulaFillAmount(rounded)) rounded else null
                        cannulaFillAmountStr = "%.1f".format(rounded)
                    },
                    valueRange = 0f..3.0f,
                    steps = 29,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resourceHelper.gs(R.string.fc_quick_amount_heading),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "0.1" to resourceHelper.gs(R.string.fc_quick_amount_0_1),
                        "0.3" to resourceHelper.gs(R.string.fc_quick_amount_0_3),
                        "0.5" to resourceHelper.gs(R.string.fc_quick_amount_0_5),
                        "1.0" to resourceHelper.gs(R.string.fc_quick_amount_1_0)
                    ).forEach { (value, label) ->
                        Button(
                            onClick = {
                                cannulaFillAmountStr = value
                                cannulaFillAmount = value.toDouble()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(text = label)
                        }
                    }
                }
            } else {
                Text(
                    text = resourceHelper.gs(R.string.ca_before_you_start_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resourceHelper.gs(
                        R.string.ca_before_stop_delivery,
                        resourceHelper.gs(R.string.fc_action)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        actions = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AapsSpacing.extraLarge),
                horizontalArrangement = Arrangement.spacedBy(AapsSpacing.large)
            ) {

                if (fillCannulaState.value != null) {
                    if (fillCannulaState.value?.state == FillCannulaStateStreamResponse.FillCannulaState.CANNULA_FILLED) {
                        PrimaryActionButton(
                            text = resourceHelper.gs(R.string.ca_btn_resume_insulin),
                            onClick = { showResumeDialog = true },
                            enabled = deliveryKnown && pumpSuspended,
                            loading = isResuming,
                            modifier = Modifier.weight(1f)
                        )

                        SecondaryActionButton(
                            text = resourceHelper.gs(R.string.common_done),
                            onClick = { showExitWithoutResumeDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                if (deliveryKnown && !pumpSuspended) {
                        SecondaryActionButton(
                            text = resourceHelper.gs(R.string.ca_btn_suspend_insulin),
                            onClick = { showSuspendDialog = true },
                            enabled = !hasActiveNotifications,
                            loading = isSuspending,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PrimaryActionButton(
                        text = if (allowedCannulaFillAmount(cannulaFillAmount))
                            resourceHelper.gs(R.string.fc_btn_fill_cannula_u, cannulaFillAmount!!)
                        else
                            resourceHelper.gs(R.string.fc_title),
                        onClick = {
                            refreshScope.launch {
                                cannulaFillAmount.let {
                                    if (allowedCannulaFillAmount(it)) {
                                        sendPumpCommand(FillCannulaRequest(InsulinUnit.from1To1000(it).toInt()))
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = deliveryKnown && pumpSuspended &&
                            cannulaFillAmount != null &&
                            allowedCannulaFillAmount(cannulaFillAmount) &&
                            !hasActiveNotifications
                    )
                }
            }
        }
    )
}

// Grace window (ms) after a user suspend/resume during which mirror samples do not
// override the optimistic UI state - a sample captured before the command took effect is
// stale and would flip the screen back.
private const val MIRROR_GRACE_MS = 8000L

val fillCannulaScreenCommands = listOf(
    HomeScreenMirrorRequest(),
    TimeSinceResetRequest(),
    LoadStatusRequest()
)

@Preview(showBackground = true)
@Composable
private fun FillCannulaScreenPreview() {
    MaterialTheme() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)
            FillCannulaScreen(
                sendPumpCommands = { _ -> true },
                navigateBack = {},
                resourceHelper = ResourceHelperTest(),
                onStepChanged = {},
                coreCartridgeActionsModel = CoreCartridgeActionsModelTest(),
                aapsLogger = AAPSLoggerTest()
            )
        }
    }
}
