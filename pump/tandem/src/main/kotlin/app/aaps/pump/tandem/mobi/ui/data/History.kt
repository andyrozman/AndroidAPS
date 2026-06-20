@file:OptIn(ExperimentalMaterial3Api::class)

package app.aaps.pump.tandem.mobi.ui.data

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.common.defs.PumpHistoryEntryGroup
import app.aaps.pump.common.driver.history.PumpHistoryPeriod
import app.aaps.pump.common.test.ResourceHelperTest
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.data.defs.RefreshData
import app.aaps.pump.tandem.common.database.data.defs.DatabaseQueryParameters
import app.aaps.pump.tandem.common.database.data.defs.DatabaseTarget
import app.aaps.pump.tandem.common.database.data.dto.TandemHistoryRecordDto
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.mobi.ui.actions.setUpPreviewState

import app.aaps.pump.tandem.mobi.ui.util.DateTimeInTwoLines
import app.aaps.pump.tandem.mobi.ui.util.HeaderLineWithBackButton
import app.aaps.shared.tests.AAPSLoggerTest
import com.jwoglom.pumpx2.pump.messages.response.historyLog.UnknownHistoryLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun History(
    innerPadding: PaddingValues = PaddingValues(),
    aapsLogger: AAPSLogger,
    refreshDatabase: (DatabaseTarget, DatabaseQueryParameters) -> Unit,
    refreshMainAppData: (RefreshData) -> Unit,
    navigateBack: () -> Unit,
    resourceHelper: ResourceHelper,
    showHeader: Boolean = true
) {

    val ds = LocalTandemDataStore.current
    @Suppress("PropertyName")
    val TAG: LTag = LTag.PUMPCOMM

    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(true) }

    var selectedGroup by remember { mutableStateOf(PumpHistoryEntryGroup.AllNoUnknowns) }
    var selectedTimeRange by remember { mutableStateOf(PumpHistoryPeriod.LAST_3_HOURS) }


    val pullRefreshState = rememberPullToRefreshState()


    fun doDatabaseRefresh() = refreshScope.launch {
        refreshing = true
        ds.dataHistoryLoaded.value = false
        aapsLogger.debug(TAG, "Database Refresh: Group: $selectedGroup  Time: $selectedTimeRange")

        refreshDatabase(
            DatabaseTarget.PUMP_HISTORY, DatabaseQueryParameters(
                groupType = selectedGroup,
                historyTime = selectedTimeRange
            )
        )

        // workaround for indicator not disappearing after
        withContext(Dispatchers.IO) {
            Thread.sleep(250)
        }

        refreshing = false

    }





//    fun waitForLoaded() = refreshScope.launch {
//
//        aapsLogger.info(TAG, "Initial Load of History.")
//        ds.dataHistoryLoaded.value = false
//
//        refreshDatabase(
//            DatabaseTarget.PUMP_HISTORY, DatabaseQueryParameters(
//                groupType = selectedGroup,
//                historyTime = selectedTimeRange
//            )
//        )
//
//        while(true) {
//            if (ds.dataHistoryLoaded.value==true) {
//                break
//            }
//
//            withContext(Dispatchers.IO) {
//                Thread.sleep(250)
//            }
//        }
//
//        aapsLogger.info(TAG, "History loaded.")
//        refreshing = false
//    }

//    LaunchedEffect(intervalOf(60)) {
//        if (firstRun || !refreshingEnabled) {
//            firstRun=false
//            return@LaunchedEffect
//        }
//        aapsLogger.info(TAG, "Reloading Qualifying events from interval")
//        refreshDatabase(DatabaseTarget.QUALIFYING_EVENTS, DatabaseQueryParameters())
//    }

    //var data by remember { mutableStateOf("Loading...") }
    //var isRefreshing by remember { mutableStateOf(false) }

    // LaunchedEffect(refreshing) {
    //     aapsLogger.info(TAG, "LaunchedEffect (refreshing) Reloading History")
    //     //waitForLoaded()
    //     doDatabaseRefresh()
    //     //refreshingEnabled = true
    // }


    LaunchedEffect(Unit) {
        aapsLogger.info(TAG, "LaunchedEffect (Unit) Reloading History")
        //waitForLoaded()
        refreshMainAppData(RefreshData.SEMAPHORE_HISTORY)
        doDatabaseRefresh()
        //refreshingEnabled = true
    }

//    DisposableEffect(Unit) {
//        onDispose {
//            refreshingEnabled = false
//        }
//    }

//    LifecycleStateObserver(lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current, onStop = {
//        refreshScope.cancel()
//    }) {
//        Timber.i("LifecycleStateObserver:reloading Qualifying events from onStart lifecyclestate")
//        refreshDatabase(DatabaseTarget.QUALIFYING_EVENTS, DatabaseQueryParameters())
//    }





    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(isRefreshing = refreshing,
                           state= pullRefreshState,
                           onRefresh = { doDatabaseRefresh() })
        // .pullRefresh(state)
    ) {
        PullToRefreshDefaults.Indicator(
            isRefreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f)
        )

        val historyLoaded = ds.dataHistoryLoaded.observeAsState()
        val historyEntries: List<TandemHistoryRecordDto> =
            if (historyLoaded.value == true) ds.dataHistory.value ?: emptyList() else emptyList()

        var expanded1 by remember { mutableStateOf(false) }
        var expanded2 by remember { mutableStateOf(false) }

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
                            text = resourceHelper.gs(R.string.data_pump_history),
                            onBackClick = navigateBack,
                            resourceHelper = resourceHelper
                        )
                        HorizontalDivider()
                    }
                }

                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                            //.padding(horizontal = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier.weight(1f)
                        ) {

                            ExposedDropdownMenuBox(
                                expanded = expanded1,
                                onExpandedChange = { expanded1 = !expanded1 }
                            ) {
                                TextField(
                                    value = selectedGroup.getDisplayValue(),
                                    onValueChange = {}, // disable manual text editing
                                    readOnly = true,
                                    label = { Text(resourceHelper.gs(R.string.data_history_log_group)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded1) },
                                    modifier = Modifier
                                        .menuAnchor(
                                            type = MenuAnchorType.PrimaryNotEditable,
                                            enabled = true
                                        )
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded1,
                                    onDismissRequest = { expanded1 = false }
                                ) {
                                    PumpHistoryEntryGroup.filteredEntries.forEach { group ->
                                        DropdownMenuItem(
                                            text = { Text(group.getDisplayValue()) },
                                            onClick = {
                                                selectedGroup = group
                                                expanded1 = false
                                                doDatabaseRefresh()
                                                //waitForLoaded()
                                            }
                                        )
                                    }
                                }
                            }
                        } // box 1

                        Box(
                            modifier = Modifier.weight(1f)
                        ) {

                            ExposedDropdownMenuBox(
                                expanded = expanded2,
                                onExpandedChange = { expanded2 = !expanded2 }
                            ) {
                                TextField(
                                    value = selectedTimeRange.getDisplayValue(),
                                    onValueChange = {}, // disable manual text editing
                                    readOnly = true,
                                    label = { Text(resourceHelper.gs(R.string.data_history_time_range)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded2) },
                                    modifier = Modifier
                                        .menuAnchor(
                                            type = MenuAnchorType.PrimaryNotEditable,
                                            enabled = true
                                        )
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded2,
                                    onDismissRequest = { expanded2 = false }
                                ) {
                                    PumpHistoryPeriod.entries.forEach { selectedTime ->
                                        DropdownMenuItem(
                                            text = { Text(selectedTime.getDisplayValue()) },
                                            onClick = {
                                                selectedTimeRange = selectedTime
                                                expanded2 = false
                                                doDatabaseRefresh()
                                                //waitForLoaded()
                                            }
                                        )
                                    }
                                }
                            }
                        } // box2
                    } // row
                }  // item


                historyEntries.forEach {
                    item {
                        HistoryEventRow(historyRecordDto = it, resourceHelper = resourceHelper)
                    }
                }

            }
        )
    } // Box Scope

}


@Composable
fun HistoryEventRow(historyRecordDto: TandemHistoryRecordDto, resourceHelper: ResourceHelper) {

    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { showDialog = true }
            .height(46.dp)
            .padding(horizontal = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .width(80.dp),
            //contentAlignment = Alignment.Center
        ) {
            DateTimeInTwoLines(historyRecordDto.pumpTime,
                               fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .width(120.dp)
        ) {
            Text(text = historyRecordDto.name,
                 fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(text = if (historyRecordDto.description==null) "" else historyRecordDto.description!!,
                 fontSize = 12.sp)
        }

        if (showDialog) {
            HistoryEntryDisplay(
                title = resourceHelper.gs(R.string.data_pump_history_log_details_title),
                historyRecordDto = historyRecordDto,
                onDismiss = { showDialog = false },
                resourceHelper = resourceHelper
            )
        }


    }
    HorizontalDivider(thickness = 1.dp)
}




@Preview(showBackground = true)
@Composable
private fun DefaultPreview_History() {
    MaterialTheme() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)

            LocalTandemDataStore.current.dataHistory.value!!.add(TandemHistoryRecordDto(pumpTime = System.currentTimeMillis(),
                name = "Basal Change", sequenceId = 57475847, historyLog = UnknownHistoryLog(), group = PumpHistoryEntryGroup.Basal ))
            LocalTandemDataStore.current.dataHistory.value!!.add(TandemHistoryRecordDto(pumpTime = System.currentTimeMillis(),
                name = "Start TBR", sequenceId = 57475847, historyLog = UnknownHistoryLog(), group = PumpHistoryEntryGroup.Basal ))
            LocalTandemDataStore.current.dataHistory.value!!.add(TandemHistoryRecordDto(pumpTime = System.currentTimeMillis(),
                name = "Bolus", sequenceId = 57475847, description = "Immediate Bolus: 12 U", historyLog = UnknownHistoryLog(), group = PumpHistoryEntryGroup.Basal ))
            LocalTandemDataStore.current.dataHistory.value!!.add(TandemHistoryRecordDto(pumpTime = System.currentTimeMillis(),
                name = "Basal Change", sequenceId = 57475847, historyLog = UnknownHistoryLog(), group = PumpHistoryEntryGroup.Basal ))
            LocalTandemDataStore.current.dataHistoryLoaded.value = true
            History(
                innerPadding = PaddingValues(),
                navigateBack = { },
                aapsLogger = AAPSLoggerTest(),
                refreshDatabase = { _,_ ->},
                resourceHelper = ResourceHelperTest(),
                refreshMainAppData = {}
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun DefaultPreview_HistoryDark() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)

            LocalTandemDataStore.current.dataHistory.value!!.add(TandemHistoryRecordDto(pumpTime = System.currentTimeMillis(),
                                                                                        name = "Basal Change", sequenceId = 57475847, historyLog = UnknownHistoryLog(), group = PumpHistoryEntryGroup.Basal ))
            LocalTandemDataStore.current.dataHistory.value!!.add(TandemHistoryRecordDto(pumpTime = System.currentTimeMillis(),
                                                                                        name = "Start TBR", sequenceId = 57475847, historyLog = UnknownHistoryLog(), group = PumpHistoryEntryGroup.Basal ))
            LocalTandemDataStore.current.dataHistory.value!!.add(TandemHistoryRecordDto(pumpTime = System.currentTimeMillis(),
                                                                                        name = "Bolus", sequenceId = 57475847, description = "Immediate Bolus: 12 U", historyLog = UnknownHistoryLog(), group = PumpHistoryEntryGroup.Basal ))
            LocalTandemDataStore.current.dataHistory.value!!.add(TandemHistoryRecordDto(pumpTime = System.currentTimeMillis(),
                                                                                        name = "Basal Change", sequenceId = 57475847, historyLog = UnknownHistoryLog(), group = PumpHistoryEntryGroup.Basal ))
            LocalTandemDataStore.current.dataHistoryLoaded.value = true
            History(
                innerPadding = PaddingValues(),
                navigateBack = { },
                aapsLogger = AAPSLoggerTest(),
                refreshDatabase = { _,_ ->},
                resourceHelper = ResourceHelperTest(),
                refreshMainAppData = {}
            )
        }
    }
}


