@file:OptIn(ExperimentalMaterial3Api::class)

package app.aaps.pump.tandem.mobi.ui.actions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.common.test.PreviewAAPSLogger
import app.aaps.pump.common.test.PreviewResourceHelper
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.data.defs.RefreshData
import app.aaps.core.ui.R as Rco
import app.aaps.pump.tandem.common.comm.ui.ResumeFailure
import app.aaps.pump.tandem.common.comm.ui.ResumeProgress
import app.aaps.pump.tandem.common.comm.ui.evaluateResume
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.mobi.ui.util.AlertBanner
import app.aaps.pump.tandem.mobi.ui.util.HeaderLineWithBackButton
import app.aaps.pump.tandem.mobi.ui.util.intervalOf
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.control.ResumePumpingRequest
import com.jwoglom.pumpx2.pump.messages.request.control.SuspendPumpingRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlarmStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlertStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.HomeScreenMirrorRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StartStopInsulinScreen(
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
    var isStartingOrStopping by remember { mutableStateOf(false) }
    var resumeFailure by remember { mutableStateOf<ResumeFailure?>(null) }

    val pumpRunningState = ds.pumpRunningState.observeAsState()
    val tempRateActive = ds.tempRateActive.observeAsState()

    fun fetchData() {
        sendPumpCommands(startStopInsulinScreenCommands)
    }

    fun refresh() = refreshScope.launch {
        aapsLogger.info(TAG, "reloading StartStopInsulinScreen with force")
        refreshing = true

        fetchData()

        withContext(Dispatchers.IO) {
            Thread.sleep(250)
        }

        refreshing = false
    }

    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(intervalOf(60)) {
        aapsLogger.info(TAG, "reloading StartStopInsulinScreen from interval")
        refresh()
    }

    // Poll for alerts and alarms immediately on screen open
    LaunchedEffect(Unit) {
        aapsLogger.info(TAG, "Initial alert/alarm poll on StartStopInsulinScreen")
        sendPumpCommands(listOf(AlertStatusRequest(), AlarmStatusRequest()))
    }

    // Poll for alerts and alarms every 10 seconds
    LaunchedEffect(intervalOf(10)) {
        aapsLogger.info(TAG, "Periodic alert/alarm poll on StartStopInsulinScreen")
        sendPumpCommands(listOf(AlertStatusRequest(), AlarmStatusRequest()))
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


        // Observe notification bundle for alerts/alarms
        val notificationBundle = ds.notificationBundle.observeAsState()
        val notifications: List<Any> = notificationBundle.value?.get()?.toList() ?: emptyList()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            HeaderLineWithBackButton(
                text = resourceHelper.gs(R.string.ca_stop_start_insulin),
                onBackClick = navigateBack,
                resourceHelper = resourceHelper
            )
            HorizontalDivider()

            // Alert/Alarm banner
            if (notifications.isNotEmpty()) {
                AlertBanner(
                    notifications = notifications,
                    sendPumpCommands = sendPumpCommands,
                    refreshScope = refreshScope,
                    resourceHelper = resourceHelper
                )
            }

            // Status text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (pumpRunningState.value) {
                    PumpRunningState.Suspended -> {
                        Text(text = resourceHelper.gs(R.string.ca_resume_all_insulin_deliveries))
                    }
                    PumpRunningState.Running -> {
                        if (tempRateActive.value == true) {
                            Text(text = resourceHelper.gs(R.string.ca_before_stopping_stop_tbr))
                        } else {
                            Text(text = resourceHelper.gs(R.string.ca_suspend_all_insulin_deliveries))
                        }
                    }
                    else -> {
                        Text(text = resourceHelper.gs(R.string.ca_stop_resume_insulin_deliveries))
                    }
                }
            }

            resumeFailure?.let { failure ->
                ResumeFailureCard(
                    failure = failure,
                    onDismiss = { resumeFailure = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            // Spacer to push button to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Action button at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (pumpRunningState.value) {
                    PumpRunningState.Suspended -> {
                        Button(
                            onClick = {
                                isStartingOrStopping = true
                                resumeFailure = null
                                val before = ds.resumePumpingResult.value
                                if (!sendPumpCommands(listOf(ResumePumpingRequest()))) {
                                    resumeFailure = ResumeFailure.NOT_SENT
                                    isStartingOrStopping = false
                                } else {
                                    refreshScope.launch {
                                        var progress = ResumeProgress.WAITING
                                        var attempt = 0
                                        while (progress == ResumeProgress.WAITING && attempt < 5) {
                                            delay(1000)
                                            // Only an answer newer than our request counts
                                            val result = ds.resumePumpingResult.value?.takeIf { it !== before }
                                            progress = evaluateResume(result, ds.pumpRunningState.value == PumpRunningState.Running)
                                            if (progress == ResumeProgress.WAITING) sendPumpCommands(listOf(HomeScreenMirrorRequest()))
                                            attempt++
                                        }
                                        when (progress) {
                                            ResumeProgress.DONE    -> navigateBack()
                                            ResumeProgress.REFUSED -> resumeFailure = ResumeFailure.PUMP_REFUSED
                                            ResumeProgress.WAITING -> resumeFailure = ResumeFailure.NO_ANSWER
                                        }
                                        refreshMainAppData(RefreshData.PUMP_STATE_CHANGED)
                                        isStartingOrStopping = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = notifications.isEmpty() && !isStartingOrStopping,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Green.copy(alpha = 0.8F)
                            )
                        ) {
                            if (isStartingOrStopping) {
                                CircularProgressIndicator()
                            } else {
                                Text(
                                    text = resourceHelper.gs(R.string.ca_resume_insulin),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                    PumpRunningState.Running -> {
                        Button(
                            onClick = {
                                isStartingOrStopping = true
                                sendPumpCommands(listOf(SuspendPumpingRequest()))
                                refreshScope.launch {
                                    repeat(5) {
                                        if (pumpRunningState.value == PumpRunningState.Suspended) {
                                            return@repeat
                                        }
                                        withContext(Dispatchers.IO) {
                                            Thread.sleep(1000)
                                        }
                                        sendPumpCommands(listOf(HomeScreenMirrorRequest()))
                                    }
                                    navigateBack()
                                    refreshMainAppData(RefreshData.PUMP_STATE_CHANGED)
                                    isStartingOrStopping = false
                                }
                            },
                            enabled = tempRateActive.value != true && notifications.isEmpty() && !isStartingOrStopping,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red.copy(alpha = 0.8F)
                            )
                        ) {
                            if (isStartingOrStopping) {
                                CircularProgressIndicator()
                            } else {
                                Text(
                                    text = resourceHelper.gs(R.string.ca_stop_insulin_small),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                    else -> {
                        // Unknown state - no action available
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumeFailureCard(
    failure: ResumeFailure,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (failure) {
        ResumeFailure.NOT_SENT     -> R.string.ca_resume_failed_not_sent
        ResumeFailure.PUMP_REFUSED -> R.string.ca_resume_failed_refused
        ResumeFailure.NO_ANSWER    -> R.string.ca_resume_failed_no_answer
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ca_resume_failed_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(message),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ca_resume_failed_contact_support),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(Rco.string.ok))
            }
        }
    }
}

val startStopInsulinScreenCommands = listOf(
    HomeScreenMirrorRequest()
)

@Preview(showBackground = true)
@Composable
private fun StartStopInsulinScreenPreview_Running() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)
            StartStopInsulinScreen(
                sendPumpCommands = { _ -> true },
                navigateBack = {},
                resourceHelper = PreviewResourceHelper(),
                aapsLogger = PreviewAAPSLogger(),
                refreshMainAppData = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartStopInsulinScreenPreview_Suspended() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)
            LocalTandemDataStore.current.pumpRunningState.value = PumpRunningState.Suspended
            StartStopInsulinScreen(
                sendPumpCommands = { _ -> true },
                navigateBack = {},
                resourceHelper = PreviewResourceHelper(),
                aapsLogger = PreviewAAPSLogger(),
                refreshMainAppData = {}
            )
        }
    }
}
