package app.aaps.pump.tandem.mobi.ui.util

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.tandem.R
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.control.DismissNotificationRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlarmStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.AlertStatusRequest
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.AlarmStatusResponse.AlarmResponseType
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.AlertStatusResponse.AlertResponseType
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TempRateResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import androidx.compose.material3.Text as Text1
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun Line(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    bold: Boolean = false,
        ) {
    Text1(
            text = text,
            style = style,
            fontWeight = when (bold) {
        true -> FontWeight.Bold
            else -> FontWeight.Normal
    },
    modifier = modifier
    )
}

@Composable
fun Line(
        text:AnnotatedString,
        modifier:Modifier= Modifier,
        style:TextStyle= MaterialTheme.typography.bodyLarge,
        bold: Boolean = false,
        ) {
    Text1(
            text = text,
            style = style,
            fontWeight = when (bold) {
        true -> FontWeight.Bold
            else -> FontWeight.Normal
    },
    modifier = modifier
    )
}

@Composable
fun intervalOf(seconds: Int): Int {
    var value by remember { mutableStateOf(0) }

    DisposableEffect(value) {
        val handler = Handler(Looper.getMainLooper())

        val runnable = {
            value += 1
        }

        handler.postDelayed(runnable, (seconds * 1000).toLong())

        onDispose {
            handler.removeCallbacks(runnable)
        }
    }

    return value
}

@Composable
fun HeaderLine(
    text: String
) {
    Line(text = text, style = MaterialTheme.typography.headlineMedium,
         modifier = Modifier.padding(all = 20.dp))
}


@Composable
fun HeaderLineWithBackButton(
    text: String,
    backgroundColor: Color = MenuDefaults.containerColor,
    onBackClick: () -> Unit,
    resourceHelper: ResourceHelper
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(backgroundColor)
            .padding(horizontal = 8.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(start = 14.dp),
            text = text,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onBackClick,
            modifier = Modifier.padding(end = 8.dp)
        ) {

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = resourceHelper.gs(R.string.common_back)
            )
        }
    }
}

val dateTimeFormatter24h = DateTimeFormatter.ofPattern("dd.MM.y HH:mm:ss")

@Composable
fun DateTimeInTwoLines(localDateTime: LocalDateTime,
                       fontSize: TextUnit = 13.sp,
                       fieldWidth: Dp? = null
) {
    val dt = localDateTime.format(dateTimeFormatter24h)
    if (fieldWidth==null) {
        Text(text = dt, fontSize = fontSize)
    } else {
        Text(text = dt, fontSize = fontSize, modifier = Modifier.width(fieldWidth))
    }
}

@Composable
fun DateTimeInTwoLines(timeInMillis: Long,
                       fontSize: TextUnit = 13.sp,
                       fieldWidth: Dp? = null
) {

    val dt = timeInMillisToLocalDateTime(timeInMillis).format(dateTimeFormatter24h)
    if (fieldWidth==null) {
        Text(text = dt, fontSize = fontSize)
    } else {
        Text(text = dt, fontSize = fontSize, modifier = Modifier.width(fieldWidth))
    }
}


fun timeInMillisToLocalDateTime(timeInMillis: Long): LocalDateTime {
    return Instant.ofEpochMilli(timeInMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}



@Composable
fun DottedDivider(
    modifier: Modifier = Modifier.fillMaxWidth().height(1.dp),
    color: Color = Color.Gray,
    dotRadius: Dp = 2.dp,
    spaceBetween: Dp = 4.dp
) {
    val dotPx = with(LocalDensity.current) { dotRadius.toPx() }
    val spacePx = with(LocalDensity.current) { spaceBetween.toPx() }

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        var x = 0f
        while (x < totalWidth) {
            drawCircle(
                color = color,
                radius = dotPx,
                center = Offset(x + dotPx, size.height / 2)
            )
            x += (dotPx * 2) + spacePx
        }
    }
}


// https://stackoverflow.com/a/72504219
fun Modifier.onFocusSelectAll(textFieldValueState: MutableState<TextFieldValue>): Modifier =
    composed(
        inspectorInfo = debugInspectorInfo {
            name = "textFieldValueState"
            properties["textFieldValueState"] = textFieldValueState
        }
    ) {
        var triggerEffect by remember {
            mutableStateOf<Boolean?>(null)
        }
        if (triggerEffect != null) {
            LaunchedEffect(triggerEffect) {
                val tfv = textFieldValueState.value
                //Timber.d("tfv: $tfv oldValue: ${textFieldValueState.value}")
                textFieldValueState.value = tfv.copy(selection = TextRange(0, tfv.text.length))
            }
        }
        Modifier.onFocusChanged { focusState ->
            if (focusState.isFocused) {
                triggerEffect = triggerEffect?.let { bool ->
                    !bool
                } ?: true
            }
        }
    }


// https://developer.android.com/jetpack/compose/side-effects#disposableeffect
@Composable
fun LifecycleStateObserver(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onStop: () -> Unit,   // Send the 'stopped' analytics event
    onStart: () -> Unit, // Send the 'started' analytics event
) {
    // Safely update the current lambdas when a new one is provided
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnStop by rememberUpdatedState(onStop)

    // If `lifecycleOwner` changes, dispose and reset the effect
    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                currentOnStart()
            } else if (event == Lifecycle.Event.ON_STOP) {
                currentOnStop()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecimalOutlinedText(
    title: String,
    value: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimalPlaces: Int = 2
) {
    val error = false


    var textFieldValue = remember {
        mutableStateOf(TextFieldValue(value ?: ""))
    }

    LaunchedEffect (value) {
        textFieldValue.value = textFieldValue.value.copy(text = value ?: "")
    }

    OutlinedTextField(
        value = textFieldValue.value,
        onValueChange = {
            textFieldValue.value = it
            val text = it.text
            var filtered = text.filter { (it in '0'..'9') || it == '.' }
            val dotIndex = filtered.lastIndexOf('.')
            if (dotIndex >= 0 && filtered.length - dotIndex > decimalPlaces + 1) {
                filtered = filtered.substring(0, dotIndex + decimalPlaces + 1)
            }
            //error = (filtered.toDoubleOrNull() == null)
            onValueChange(filtered)
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None,
                                          autoCorrectEnabled = false,
                                          keyboardType = KeyboardType.Number,
                                          imeAction = ImeAction.Unspecified
        ),
        label = { Text(text = title) },
        isError = error,
//        colors = if (isSystemInDarkTheme())
//            TextFieldDefaults.outlinedTextFieldColors( focusedTextColor = Color.DarkGray,  focusedPlaceholderColor = Color.DarkGray)
//        else TextFieldDefaults.outlinedTextFieldColors(),
        modifier = modifier.fillMaxWidth().onFocusSelectAll(textFieldValue)
    )
}


fun twoDecimalPlaces(decimal: Double): String {
    return String.format("%.2f", decimal)
}

// fun prettyDuration(seconds: Long?): String {
//     val minutes = seconds?.div(60)
//     println("Pretty Duration: ${minutes}")
//     return "${minutes?.div(60)}h${minutes?.rem(60)}m"
// }

val zoneId = ZoneId.systemDefault()
val formatterTime = DateTimeFormatter.ofPattern("HH:mm")

fun prettyTime(startTime: Instant?): String {
    if (startTime==null) {
        return "??"
    }

    val zonedDateTime = startTime.atZone(zoneId)

    return zonedDateTime.format(formatterTime)
}

fun remainingTime(secondsDuration: Long?, startTime: Instant?, resourceHelper: ResourceHelper): String {
    if (secondsDuration==null || startTime==null) {
        return "?"
    }

    val endTime = startTime.plusSeconds(secondsDuration)

    val duration = Duration.between(Instant.now(), endTime)

    return resourceHelper.gs(R.string.ui_a_min_ago, duration.toMinutes())

}


fun compactTBRDisplay(tempRateResponse: TempRateResponse?, resourceHelper: ResourceHelper): String {
    if (tempRateResponse==null) {
        return "?"
    }

    return if (tempRateResponse.active) {
        "${tempRateResponse.percentage}%  (${remainingTime(tempRateResponse.duration,tempRateResponse.startTimeInstant, resourceHelper)})"
    } else {
        " - "
    }
}

@Composable
fun AlertBanner(
    notifications: List<Any>,
    sendPumpCommands: (List<Message>) -> Boolean,
    refreshScope: CoroutineScope
) {
    // Filter for alerts and alarms only
    val alarms = notifications.filterIsInstance<AlarmResponseType>()
    val alerts = notifications.filterIsInstance<AlertResponseType>()

    // Track which notification is being dismissed
    var dismissingItem by remember { mutableStateOf<Any?>(null) }

    // Dismiss handler for alerts
    fun dismissAlert(alert: AlertResponseType) {
        dismissingItem = alert
        refreshScope.launch {
            sendPumpCommands(listOf(
                DismissNotificationRequest(
                    DismissNotificationRequest.NotificationType.ALERT,
                    alert.bitmask().toLong()
                )
            ))
            // Immediately refresh notifications after dismissing
            delay(500.milliseconds) // Small delay for dismiss to process
            sendPumpCommands(listOf(AlertStatusRequest(), AlarmStatusRequest()))
            dismissingItem = null
        }
    }

    // Dismiss handler for alarms
    fun dismissAlarm(alarm: AlarmResponseType) {
        dismissingItem = alarm
        refreshScope.launch {
            sendPumpCommands(listOf(
                DismissNotificationRequest(
                    DismissNotificationRequest.NotificationType.ALARM,
                    alarm.bitmask().toLong()
                )
            ))
            // Immediately refresh notifications after dismissing
            delay(500.milliseconds) // Small delay for dismiss to process
            sendPumpCommands(listOf(AlertStatusRequest(), AlarmStatusRequest()))
            dismissingItem = null
        }
    }

    // Combine alerts and alarms for display (show up to 3 total, alarms first)
    val totalCount = alerts.size + alarms.size
    val displayAlarms = alarms.take(3)
    val displayAlerts = if (displayAlarms.size < 3) alerts.take(3 - displayAlarms.size) else emptyList()
    val remainingCount = totalCount - displayAlarms.size - displayAlerts.size

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Dismiss notifications before continuing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            displayAlarms.forEach { alarm ->
                NotificationDismissPill(
                    label = "ALARM",
                    name = alarm.name,
                    isDismissing = dismissingItem == alarm,
                    onDismiss = { dismissAlarm(alarm) },
                )
            }
            displayAlerts.forEach { alert ->
                NotificationDismissPill(
                    label = "ALERT",
                    name = alert.name,
                    isDismissing = dismissingItem == alert,
                    onDismiss = { dismissAlert(alert) },
                )
            }
            if (remainingCount > 0) {
                Text(
                    text = "+ $remainingCount more notification${if (remainingCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/** Single dismissible alert/alarm row. Reusable inside or outside [AlertBanner]. */
@Composable
fun NotificationDismissPill(
    label: String,
    name: String,
    isDismissing: Boolean,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp),
                enabled = !isDismissing
            ) {
                if (isDismissing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** Render dismissible pills for the alerts/alarms in [notifications], no surrounding Card. */
@Composable
fun NotificationDismissPills(
    notifications: List<Any>,
    sendPumpCommands: (List<Message>) -> Boolean,
    refreshScope: CoroutineScope,
) {
    val alarms = notifications.filterIsInstance<AlarmResponseType>()
    val alerts = notifications.filterIsInstance<AlertResponseType>()

    var dismissingItem by remember { mutableStateOf<Any?>(null) }

    fun dismissAlert(alert: AlertResponseType) {
        dismissingItem = alert
        refreshScope.launch {
            sendPumpCommands(listOf(
                DismissNotificationRequest(
                    DismissNotificationRequest.NotificationType.ALERT,
                    alert.bitmask().toLong()
                )
            ))
            delay(500.milliseconds)
            sendPumpCommands(listOf(AlertStatusRequest(), AlarmStatusRequest()))
            dismissingItem = null
        }
    }

    fun dismissAlarm(alarm: AlarmResponseType) {
        dismissingItem = alarm
        refreshScope.launch {
            sendPumpCommands(listOf(
                DismissNotificationRequest(
                    DismissNotificationRequest.NotificationType.ALARM,
                    alarm.bitmask().toLong()
                )
            ))
            delay(500)
            sendPumpCommands(listOf(AlertStatusRequest(), AlarmStatusRequest()))
            dismissingItem = null
        }
    }

    val totalCount = alerts.size + alarms.size
    val displayAlarms = alarms.take(3)
    val displayAlerts = if (displayAlarms.size < 3) alerts.take(3 - displayAlarms.size) else emptyList()
    val remainingCount = totalCount - displayAlarms.size - displayAlerts.size

    Column(modifier = Modifier.fillMaxWidth()) {
        displayAlarms.forEach { alarm ->
            NotificationDismissPill(
                label = "ALARM",
                name = alarm.name,
                isDismissing = dismissingItem == alarm,
                onDismiss = { dismissAlarm(alarm) },
            )
        }
        displayAlerts.forEach { alert ->
            NotificationDismissPill(
                label = "ALERT",
                name = alert.name,
                isDismissing = dismissingItem == alert,
                onDismiss = { dismissAlert(alert) },
            )
        }
        if (remainingCount > 0) {
            Text(
                text = "+ $remainingCount more notification${if (remainingCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

