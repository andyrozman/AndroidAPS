@file:OptIn(ExperimentalMaterial3Api::class)

package app.aaps.pump.tandem.mobi.ui.data

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.common.test.ResourceHelperTest
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.data.defs.RefreshData
import app.aaps.pump.tandem.common.database.data.defs.DatabaseQueryParameters
import app.aaps.pump.tandem.common.database.data.defs.DatabaseTarget
import app.aaps.pump.tandem.common.database.data.dto.TandemQualifyingEventDto
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.mobi.ui.util.HeaderLineWithBackButton
import app.aaps.pump.tandem.mobi.ui.util.DateTimeInTwoLines
import app.aaps.pump.tandem.mobi.ui.util.intervalOf
import app.aaps.shared.tests.AAPSLoggerTest
import com.jwoglom.pumpx2.pump.messages.response.qualifyingEvent.QualifyingEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Composable
fun QualifyingEvents(
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
    val TAG = LTag.PUMPCOMM

    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    fun refresh() = refreshScope.launch {
        aapsLogger.info(TAG, "Reloading Qualifying events")
        refreshing = true

        ds.dataQELoaded.value = false
        refreshDatabase(DatabaseTarget.QUALIFYING_EVENTS, DatabaseQueryParameters())

        // workaround for indicator not disappearing after
        withContext(Dispatchers.IO) {
            Thread.sleep(250)
        }

        refreshing = false

    }


    LaunchedEffect(intervalOf(60)) {
        aapsLogger.debug(TAG, "Reloading Qualifying events from interval")
        refreshMainAppData(RefreshData.SEMAPHORE_EVENTS)
        refresh()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(isRefreshing = refreshing,
                           state= pullToRefreshState,
                           onRefresh = { refresh() })
    ) {
        PullToRefreshDefaults.Indicator(
            isRefreshing = refreshing,
            state = pullToRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f)
        )

        val qeLoaded = ds.dataQELoaded.observeAsState()
        val events: List<TandemQualifyingEventDto> =
            if (qeLoaded.value == true) ds.dataQE.value ?: emptyList() else emptyList()


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
                            text = resourceHelper.gs(R.string.data_events),
                            onBackClick = navigateBack,
                            resourceHelper = resourceHelper
                        )
                        HorizontalDivider()
                    }
                }

                events.forEach {
                    item {
                        QualifyingEventRow(it)
                    }
                }

            }
        )

    } // Box Scope

}


@Composable
fun QualifyingEventRow(eventDto: TandemQualifyingEventDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            DateTimeInTwoLines(eventDto.dateTime,
                               fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .width(150.dp)
        ) {
            Text(text = eventDto.name.name,
                 fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(text = eventDto.description, fontSize = 12.sp)
        }
    }
    HorizontalDivider(thickness = 1.dp)
}



@Preview(showBackground = true)
@Composable
private fun DefaultPreview_QualifyingEvents() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            app.aaps.pump.tandem.mobi.ui.actions.setUpPreviewState(LocalTandemDataStore.current)

            LocalTandemDataStore.current.dataQE.value!!.add(TandemQualifyingEventDto(dateTime = LocalDateTime.now()
                , name = QualifyingEvent.BASAL_CHANGE, description = "" ))
            LocalTandemDataStore.current.dataQE.value!!.add(TandemQualifyingEventDto(dateTime = LocalDateTime.now()
                , name = QualifyingEvent.HOME_SCREEN_CHANGE, description = "" ))
            LocalTandemDataStore.current.dataQE.value!!.add(TandemQualifyingEventDto(dateTime = LocalDateTime.now()
                , name = QualifyingEvent.BATTERY, description = "Level: 20%" ))
            LocalTandemDataStore.current.dataQELoaded.value = true
            QualifyingEvents(
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


