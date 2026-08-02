@file:OptIn(ExperimentalMaterial3Api::class)

package app.aaps.pump.tandem.mobi.ui.actions.cartridge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.comm.ui.CoreCartridgeActionsModel
import app.aaps.pump.tandem.mobi.ui.util.HeaderLineWithBackButton
import com.jwoglom.pumpx2.pump.messages.Message
import kotlinx.coroutines.CoroutineScope

@Composable
fun CartridgeWorkflowScreen(
    title: String,
    innerPadding: PaddingValues,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    resourceHelper: ResourceHelper,
    notifications: List<Any>,
    sendPumpCommands: (List<Message>) -> Boolean,
    refreshScope: CoroutineScope,
    showHeader: Boolean = true,
    aapsLogger: AAPSLogger? = null,
    stepIndicator: @Composable () -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
    coreCartridgeActionsModel: CoreCartridgeActionsModel
) {
    val pullRefreshState = rememberPullToRefreshState()
    val isNotificationHidden by coreCartridgeActionsModel.hideNotification.collectAsStateWithLifecycle()


    // var isInSiteSelectionMode by remember { mutableStateOf(false) }
    //
    //
    // val siteLocation by coreCartridgeActionsModel.siteLocation.collectAsStateWithLifecycle()


    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = refreshing,
                state = pullRefreshState,
                onRefresh = onRefresh,
            ),
    ) {
        PullToRefreshDefaults.Indicator(
            isRefreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                ),
        ) {
            if (showHeader) {
                HeaderLineWithBackButton(
                    text = title,
                    onBackClick = onBack,
                    resourceHelper = resourceHelper,
                )
                HorizontalDivider()
            }
            stepIndicator()
            if (notifications.isEmpty() && !isNotificationHidden) {
                if (aapsLogger!=null) {
                    aapsLogger.error(LTag.PUMP, "Notiifcation is empty and notificationHidden=${isNotificationHidden}")
                }
                CartridgeNotificationsPanel(resourceHelper = resourceHelper)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = body,
            )
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = actions,
            )
        }
    }
    if (notifications.isNotEmpty()) {
        CartridgeNotificationsBlockingDialog(
            notifications = notifications,
            sendPumpCommands = sendPumpCommands,
            refreshScope = refreshScope,
            resourceHelper = resourceHelper,
        )
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** "Step N of M" indicator for [CartridgeWorkflowScreen]'s `stepIndicator` slot. */
@Composable
fun WizardStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    resourceHelper: ResourceHelper,
) {
    val progress = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f
    Spacer(modifier = Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = resourceHelper.gs(R.string.ca_step_x_of_y, currentStep, totalSteps),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier,
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}
