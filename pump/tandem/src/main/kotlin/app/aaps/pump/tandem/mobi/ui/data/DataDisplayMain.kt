@file:OptIn(ExperimentalMaterial3Api::class)
package app.aaps.pump.tandem.mobi.ui.data

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackupTable
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.common.test.ResourceHelperTest
import app.aaps.pump.tandem.R

import app.aaps.pump.tandem.common.comm.ui.TandemUIDataStore
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.common.driver.tandemUiDataStore

import app.aaps.pump.tandem.mobi.ui.util.HeaderLine
import app.aaps.pump.tandem.mobi.ui.util.intervalOf
import app.aaps.shared.tests.AAPSLoggerTest
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlarmStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlertStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.MalfunctionStatusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@Composable
fun DataDisplayMain(
    innerPadding: PaddingValues = PaddingValues(),
    sendPumpCommands: (List<Message>) -> Boolean,
    //refreshMainAppData: (RefreshData) -> Unit,
    aapsLogger: AAPSLogger,
    navigateToPumpHistory: () -> Unit,
    navigateToEvents: () -> Unit,
    navigateToNotifications: () -> Unit,
    resourceHelper: ResourceHelper,
    showHeader: Boolean = true
) {

    val ds = LocalTandemDataStore.current
    @Suppress("PropertyName")
    val TAG = LTag.PUMPCOMM


    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(true) }
    val notificationsPresent by ds.notificationsPresent.observeAsState(false)


    fun refresh() = refreshScope.launch {
        aapsLogger.info(TAG, "Reloading Main Data display")
        refreshing = true

        sendPumpCommands(dataCommands)

        withContext(Dispatchers.IO) {
            Thread.sleep(250)
        }

        refreshing = false
    }

    val pullRefreshState = rememberPullToRefreshState()


    LaunchedEffect(intervalOf(60)) {
        aapsLogger.info(TAG, "reloading Actions from interval")
        refresh()
    }




    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(isRefreshing = refreshing,
                           state= pullRefreshState,
                           onRefresh = { refresh() })
        // .pullRefresh(state)
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
                        HeaderLine(text = resourceHelper.gs(R.string.data_data))
                        HorizontalDivider()
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
                                text = resourceHelper.gs(R.string.data_notifications), color = if (notificationsPresent) Color.Red else Color.Unspecified
                            )
                            },
                            supportingContent = {
                                Text(text = resourceHelper.gs(R.string.data_notifications_desc), color = if (notificationsPresent) Color.Red else Color.Unspecified)
                            },
                            leadingContent = {
                                if (notificationsPresent) {
                                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                                } else {
                                    Icon(Icons.Filled.NotificationsNone, contentDescription = null)
                                }
                            },
                            modifier = Modifier.clickable {
                                navigateToNotifications()
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
                            headlineContent = { Text(text = resourceHelper.gs(R.string.data_events))
                            },
                            supportingContent = {
                                Text(text = resourceHelper.gs(R.string.data_events_desc))
                            },
                            leadingContent = {
                                Icon(Icons.Filled.EventAvailable, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                navigateToEvents()
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
                            headlineContent = { Text(text = resourceHelper.gs(R.string.data_pump_history))
                            },
                            supportingContent = {
                                Text(text = resourceHelper.gs(R.string.data_pump_history_desc))
                            },
                            leadingContent = {
                                Icon(Icons.Filled.BackupTable, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                navigateToPumpHistory()
                            }
                        )
                    }
                }
            }
        )
    }
}
val dataCommands = listOf(
    //*NotificationBundle.allRequests().toTypedArray()
    AlertStatusRequest(),
    AlarmStatusRequest(),
    MalfunctionStatusRequest(),
)

@Preview(showBackground = true)
@Composable
private fun DataDisplayPreview_NoNotification() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)
            DataDisplayMain(
                sendPumpCommands = { _ -> true},
                //refreshDatabase = { _,_ -> },
                aapsLogger = AAPSLoggerTest(),
                navigateToNotifications = {},
                navigateToPumpHistory = {},
                navigateToEvents = {},
                resourceHelper = ResourceHelperTest(),
                //refreshMainAppData = {}
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun DataDisplayPreview_WithNotification() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)
            tandemUiDataStore.notificationsPresent.value = true
            DataDisplayMain(
                sendPumpCommands = { _ -> true},
                //refreshDatabase = { _,_ -> },
                aapsLogger = AAPSLoggerTest(),
                navigateToNotifications = {},
                navigateToPumpHistory = {},
                navigateToEvents = {},
                resourceHelper = ResourceHelperTest(),
                //refreshMainAppData = {}
            )
        }
    }
}



//
//@Preview(showBackground = true)
//@Composable
//private fun DefaultPreviewInsulinActive_StopMenuOpen() {
//    MaterialTheme() {
//        Surface(
//            modifier = Modifier.fillMaxSize(),
//            color = Color.White,
//        ) {
//            setUpPreviewState(LocalTandemDataStore.current)
//            Actions(
//                sendMessage = { _, _ -> },
//                sendPumpCommands = { _ -> true},
//                _suspendInsulinMenuState = true,
//                //openTempRateWindow = {},
//                //navigateToCgmActions = {},
//                aapsLogger = AAPSLoggerTest(),
//                navigateToCartridgeActions = {},
//                navigateToPumpInfo = {}
//            )
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//private fun DefaultPreviewInsulinSuspended() {
//    MaterialTheme() {
//        Surface(
//            modifier = Modifier.fillMaxSize(),
//            color = Color.White,
//        ) {
//            setUpPreviewState(LocalTandemDataStore.current)
//            LocalTandemDataStore.current.basalStatus.value = BasalStatus.PUMP_SUSPENDED
//            Actions(
//                sendMessage = { _, _ -> },
//                sendPumpCommands = { _ -> true},
//                //openTempRateWindow = {},
//                //navigateToCgmActions = {},
//                aapsLogger = AAPSLoggerTest(),
//                navigateToCartridgeActions = {},
//                navigateToPumpInfo = {}
//            )
//        }
//    }
//}


fun setUpPreviewState(ds: TandemUIDataStore) {
    ds.pumpConnected.value = true
    ds.pumpLastConnectionTimestamp.value = Instant.now().minusSeconds(120)
}