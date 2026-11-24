@file:OptIn(ExperimentalFoundationApi::class)

package app.aaps.pump.tandem.mobi.ui.actions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.common.test.PreviewAAPSLogger
import app.aaps.pump.common.test.PreviewResourceHelper
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.mobi.ui.util.DottedDivider
import app.aaps.pump.tandem.mobi.ui.util.Line2
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.PumpGlobalsRequest
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpGlobalsResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpGlobalsResponse.AnnunciationEnum

import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState

import com.jwoglom.pumpx2.pump.messages.request.control.SetPumpSoundsRequest
import com.jwoglom.pumpx2.pump.messages.request.control.SetPumpSoundsRequest.ChangeBitmask


@Composable
fun SoundConfiguration(innerPadding: PaddingValues = PaddingValues(),
             sendPumpCommands: (List<Message>) -> Boolean,
             aapsLogger: AAPSLogger,
             tandemPumpStatus: TandemPumpStatus? = null,
             resourceHelper: ResourceHelper
             ) {



    val pumpGlobalsResponse by LocalTandemDataStore.current
        .pumpGlobalResponse
        .observeAsState()

    val setPumpSoundsResponse by LocalTandemDataStore.current
        .setPumpSoundsResponse
        .observeAsState()

    @Suppress("LocalVariable")
    val TAG = LTag.PUMPCOMM

    var savePending by remember {
        mutableStateOf(false)
    }

    var originalSettings by remember {
        mutableStateOf<SoundSettings?>(null)
    }

    var soundSettings by remember {
        mutableStateOf<SoundSettings?>(null)
    }


    LaunchedEffect(Unit) {
        aapsLogger.info(TAG, "Get PumpGlobalsRequest on Start")
        sendPumpCommands(listOf(PumpGlobalsRequest()))
    }


    LaunchedEffect(pumpGlobalsResponse) {
        pumpGlobalsResponse?.let {
            val settings = it.toSoundSettings()
            originalSettings = settings
            soundSettings = settings
        }
    }


    LaunchedEffect(setPumpSoundsResponse) {
        if (setPumpSoundsResponse?.isStatusOK() == true) {
            sendPumpCommands(listOf(PumpGlobalsRequest()))
        }
    }


    val hasChanges = soundSettings != null &&
        soundSettings != originalSettings

    LazyColumn(
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp),
        content = {

            item {
                SoundConfigurationRow(
                    label = resourceHelper.gs(R.string.sc_type_general),
                    value = soundSettings?.general,
                    onValueChange = { newValue ->
                        soundSettings = soundSettings?.copy(general = newValue)
                    }
                )
            }

            item {
                SoundConfigurationRow(
                    label = resourceHelper.gs(R.string.sc_type_quick_bolus),
                    value = soundSettings?.quickBolus,
                    onValueChange = { newValue ->
                        soundSettings = soundSettings?.copy(quickBolus = newValue)
                    }
                )
            }

            item {
                SoundConfigurationRow(
                    label = resourceHelper.gs(R.string.sc_type_reminder),
                    value = soundSettings?.reminder,
                    onValueChange = { newValue ->
                        soundSettings = soundSettings?.copy(reminder = newValue)
                    }
                )
            }

            item {
                SoundConfigurationRow(
                    label = resourceHelper.gs(R.string.sc_type_alert),
                    value = soundSettings?.alert,
                    onValueChange = { newValue ->
                        soundSettings = soundSettings?.copy(alert = newValue)
                    }
                )
            }

            item {
                SoundConfigurationRow(
                    label = resourceHelper.gs(R.string.sc_type_alarm),
                    value = soundSettings?.alarm,
                    onValueChange = { newValue ->
                        soundSettings = soundSettings?.copy(alarm = newValue)
                    }
                )
            }

            item {
                Line2(text2 = "\n")
            }

            // item {
            //     Text("Alert Snooze", fontSize = 18.sp)
            //     Text("Allow snoozing alert via triple-press on pump button", fontSize = 12.sp)
            //     // toggle
            //
            // }

            // item {
            //     Text("Snooze Duration", fontSize = 18.sp)
            //
            //     // comboBox
            //
            // }


            item {
                Spacer(modifier = Modifier.height(25.dp))

                Button(
                    enabled = hasChanges,
                    onClick = {
                        val original = originalSettings ?: return@Button
                        val updated = soundSettings ?: return@Button
                        val globals = pumpGlobalsResponse ?: return@Button

                        val request = buildSetPumpSoundsRequest(
                            original,
                            updated,
                            originalCgmAlertA = 0,
                            originalCgmAlertB = 0
                        )

                        request?.let {
                            sendPumpCommands(listOf(it))
                        }
                    }
                ) {
                    Text(resourceHelper.gs(R.string.sr_save))
                }
            }
        }

    )
}


enum class SoundSaveState {
    IDLE,
    WAITING_ACK,
    VERIFYING
}


private fun buildSetPumpSoundsRequest(
    original: SoundSettings,
    updated: SoundSettings,
    originalCgmAlertA: Int,
    originalCgmAlertB: Int
): SetPumpSoundsRequest? {
    val changes = mutableListOf<ChangeBitmask>()

    if (original.general != updated.general)
        changes.add(ChangeBitmask.GENERAL)

    if (original.quickBolus != updated.quickBolus)
        changes.add(ChangeBitmask.QUICK_BOLUS)

    if (original.reminder != updated.reminder)
        changes.add(ChangeBitmask.REMINDER)

    if (original.alert != updated.alert)
        changes.add(ChangeBitmask.ALERT)

    if (original.alarm != updated.alarm)
        changes.add(ChangeBitmask.ALARM)

    if (changes.isEmpty()) return null

    return SetPumpSoundsRequest(
        updated.quickBolus.id(),
        updated.general.id(),
        updated.reminder.id(),
        updated.alert.id(),
        updated.alarm.id(),
        originalCgmAlertA,
        originalCgmAlertB,
        ChangeBitmask.toBitmask(*changes.toTypedArray())
    )
}



@Composable
fun SoundConfigurationRow(
    label: String,
    value: AnnunciationEnum?,
    onValueChange: (AnnunciationEnum) -> Unit,
    rowModifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 20.dp)
            .then(rowModifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 18.sp)

        Box {
            Text(
                text = value?.name?.replace("_", " ") ?: "Loading...",
                modifier = Modifier.clickable(enabled = value != null) {
                    expanded = true
                },
                fontSize = 18.sp
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AnnunciationEnum.values().forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(option.name.replace("_", " "))
                        },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    DottedDivider(dotRadius = 1.dp, spaceBetween = 6.dp)
}

data class SoundSettings(
    val general: AnnunciationEnum,
    val quickBolus: AnnunciationEnum,
    val reminder: AnnunciationEnum,
    val alert: AnnunciationEnum,
    val alarm: AnnunciationEnum,
)

fun PumpGlobalsResponse.toSoundSettings() = SoundSettings(
    general = buttonAnnun,
    quickBolus = quickBolusAnnun,
    reminder = reminderAnnun,
    alert = alertAnnun,
    alarm = alarmAnnun
)


@Preview(showBackground = true)
@Composable
private fun DefaultPreview_SounConfiguration() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)
            var soundConfig = PumpGlobalsResponse(0, 0, 0, 0, 0, 3, 3, 0, 1, 2, 3, 3)
            LocalTandemDataStore.current.pumpGlobalResponse.value = soundConfig
            SoundConfiguration(
                innerPadding = PaddingValues(),
                sendPumpCommands = { _ -> true },
                tandemPumpStatus = null,
                resourceHelper = PreviewResourceHelper(),
                aapsLogger = PreviewAAPSLogger()
            )
        }
    }
}
