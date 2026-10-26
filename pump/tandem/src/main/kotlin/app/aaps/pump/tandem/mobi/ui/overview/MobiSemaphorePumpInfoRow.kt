package app.aaps.pump.tandem.mobi.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.aaps.core.ui.compose.pump.PumpInfoComposable
import app.aaps.pump.tandem.common.data.SemaphoreInfoDto
import kotlinx.coroutines.flow.MutableSharedFlow

class MobiSemaphorePumpInfoRow(
    val events: MutableSharedFlow<MobiOverviewEvent>,
    val semaphoreInfo: SemaphoreInfoDto,
    val mapSemaphoreTranslations: Map<String, String>
) : PumpInfoComposable {




    override fun composableContent(): @Composable (() -> Unit) = {

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = mapSemaphoreTranslations["NOTIFICATIONS"]!!,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(enabled = semaphoreInfo.semaphoreNotifications) {
                        events.tryEmit(MobiOverviewEvent.OpenNotification)
                    },
                color = if (semaphoreInfo.semaphoreNotifications) Color.Red
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = mapSemaphoreTranslations["EVENTS"]!!,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable(enabled = semaphoreInfo.semaphoreEvents) {
                        events.tryEmit(MobiOverviewEvent.OpenEvents)
                    },
                color = if (semaphoreInfo.semaphoreEvents) Color.Green
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = mapSemaphoreTranslations["HISTORY"]!!,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(enabled = semaphoreInfo.semaphoreHistory) {
                        events.tryEmit(MobiOverviewEvent.OpenHistory)
                    },
                color = if (semaphoreInfo.semaphoreHistory) Color.Blue
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    override fun hasDividerOnEnd(): Boolean {
        return false
    }

}