@file:OptIn(ExperimentalMaterial3Api::class)

package app.aaps.pump.tandem.mobi.ui.actions.cartridge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.data.defs.RefreshData
import app.aaps.pump.tandem.common.data.defs.SiteReminderPreset
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.common.driver.TandemPumpStatus

import app.aaps.pump.tandem.mobi.ui.util.HeaderLineWithBackButton
import app.aaps.pump.tandem.mobi.ui.util.LifecycleStateObserver
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import kotlinx.coroutines.cancel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import app.aaps.core.ui.R as Rco

@Composable
fun SiteReminder(innerPadding: PaddingValues = PaddingValues(),
                 navigateBack: () -> Unit,
                 aapsLogger: AAPSLogger,
                 resourceHelper: ResourceHelper,
                 pumpStatusData: TandemPumpStatus,
                 refreshMainAppData: (RefreshData) -> Unit,
                 showHeader: Boolean = true
) {

    val ds = LocalTandemDataStore.current

    var pickedDate by remember { mutableStateOf(LocalDate.now()) }
    var pickedTime by remember { mutableStateOf(LocalTime.now()) }
    val currentDate = LocalDate.now()
    val locale = Locale.getDefault()

    val formaterDate = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
    }

    val formaterTime = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }

    val formattedDate by remember { derivedStateOf { formaterDate.format(pickedDate) } }
    val formattedTime by remember { derivedStateOf { formaterTime.format(pickedTime) } }

    val dateDialogState = rememberMaterialDialogState()
    val timeDialogState = rememberMaterialDialogState()

    var expanded1 by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf(SiteReminderPreset.IN_3_DAYS) }
    var hasChanged by remember { mutableStateOf(false) }

    val refreshScope = rememberCoroutineScope()

    LifecycleStateObserver(lifecycleOwner = LocalLifecycleOwner.current, onStop = {
        refreshScope.cancel()
    }) {
        aapsLogger.debug(LTag.PUMP, "Load Reminder Date from saved (if available)")
        if (pumpStatusData.tandemSiteReminder!=null) {
            aapsLogger.debug(LTag.PUMP, "Reminder Date found: ${pumpStatusData.tandemSiteReminder}")
            val localDateTime = Instant.ofEpochMilli(pumpStatusData.tandemSiteReminder!!)
                .atZone(ZoneId.systemDefault()).toLocalDateTime()

            pickedDate = localDateTime.toLocalDate()
            pickedTime = localDateTime.toLocalTime()
        } else {
            aapsLogger.debug(LTag.PUMP, "Reminder Date NOT found.")
        }
    }


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
                        text = resourceHelper.gs(R.string.sr_title),
                        onBackClick = navigateBack,
                        resourceHelper = resourceHelper
                    )
                    HorizontalDivider()
                }
            }

            item {

                Spacer(modifier = Modifier.height(25.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp, end = 40.dp)
                        .height(46.dp)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = resourceHelper.gs(R.string.sr_reminder_description))
                }

                Spacer(modifier = Modifier.height(25.dp))
            }


            item {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp, end = 40.dp)
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
                                label = { Text(resourceHelper.gs(R.string.sr_site_change_presets)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded1) },
                                modifier = Modifier
                                    .menuAnchor(
                                        type = ExposedDropdownMenuAnchorType.Companion.PrimaryNotEditable,
                                        enabled = true)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded1,
                                onDismissRequest = { expanded1 = false }
                            ) {
                                SiteReminderPreset.entries.forEach { group ->
                                    DropdownMenuItem(
                                        text = { Text(group.getDisplayValue()) },
                                        onClick = {
                                            selectedGroup = group
                                            expanded1 = false
                                        }
                                    )
                                }
                            }
                        }
                    } // box 1
                } // row
            }  // item


            item {

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(onClick = {
                        val hours = selectedGroup.hours
                        var dateTime = LocalDateTime.now()
                        dateTime = dateTime.plusHours(hours.toLong())

                        pickedDate = dateTime.toLocalDate()
                        pickedTime = dateTime.toLocalTime()
                        hasChanged = true;

                    }, modifier = Modifier.width(200.dp)) {
                        Text(text = resourceHelper.gs(R.string.sr_apply_preset))
                    }
                }

                Spacer(modifier = Modifier.height(25.dp))
            }

            item {

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(onClick = {
                        dateDialogState.show()
                    }, modifier = Modifier.width(200.dp)) {
                        Text(text = resourceHelper.gs(R.string.sr_adjust_date))
                    }

                    if (hasChanged) {
                        Text(text = formattedDate, color = Color.Blue)
                    } else {
                        Text(text = formattedDate)
                    }


                    Spacer(modifier = Modifier.height(25.dp))

                    Button(onClick = {
                        timeDialogState.show()
                    }, modifier = Modifier.width(200.dp)) {
                        Text(text = resourceHelper.gs(R.string.sr_adjust_time))
                    }

                    if (hasChanged) {
                        Text(text = formattedTime, color = Color.Blue)
                    } else {
                        Text(text = formattedTime)
                    }


                    Spacer(modifier = Modifier.height(25.dp))

                    Button(onClick = {
                        hasChanged = false

                        val ldt = LocalDateTime.of(pickedDate, pickedTime)
                        val offsetNow: ZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())

                        ds.reminderDateTime.value = ldt.toInstant(offsetNow).toEpochMilli()
                        ds.reminderDateTimeUpdated.value = true

                        refreshMainAppData(RefreshData.REMINDER_CHANGED)

                    }, modifier = Modifier.width(200.dp)) {
                        Text(text = resourceHelper.gs(R.string.sr_save))
                    }
                }

                MaterialDialog(
                    dialogState = dateDialogState,
                    buttons = {
                        positiveButton(text = resourceHelper.gs(Rco.string.ok))
                        negativeButton(text = resourceHelper.gs(Rco.string.cancel))
                    }
                ) {
                    datepicker(
                        initialDate = pickedDate,
                        title = resourceHelper.gs(R.string.sr_adjust_date),
                       allowedDateValidator = {
                           it.isAfter(currentDate) || it.isEqual(currentDate)
                       }
                    ) {
                        if (!pickedDate.isEqual(it)) {
                            pickedDate = it
                            hasChanged = true
                        }
                    }
                }
                MaterialDialog(
                    dialogState = timeDialogState,
                    buttons = {
                        positiveButton(text = resourceHelper.gs(Rco.string.ok))
                        negativeButton(text = resourceHelper.gs(Rco.string.cancel))
                    }
                ) {
                    timepicker(
                        initialTime = pickedTime,
                        title = resourceHelper.gs(R.string.sr_adjust_time)
                    ) {
                        pickedTime = it

                        if (!pickedTime.isAfter(it ) && !pickedTime.isBefore(it)) {
                            pickedTime = it
                            hasChanged = true
                        }
                    }
                }
            }
        }

    )
}



//
// @Preview(showBackground = true)
// @Composable
// private fun DefaultPreview_PumpInfo() {
//     MaterialTheme() {
//         Surface(
//             modifier = Modifier.fillMaxSize(),
//             color = Color.White,
//         ) {
//             setUpPreviewState(LocalTandemDataStore.current)
//
//             SiteReminder(
//                 innerPadding = PaddingValues(),
//                 navigateBack = { },
//                 resourceHelper = ResourceHelperTest(),
//                 refreshMainAppData = {},
//                 pumpStatusData = TandemPumpStatus(),
//                 aapsLogger = AAPSLoggerTest()
//             )
//         }
//     }
// }
