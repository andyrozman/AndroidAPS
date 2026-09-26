@file:OptIn(ExperimentalFoundationApi::class)

package app.aaps.pump.tandem.mobi.ui.actions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.common.test.PreviewResourceHelper
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.driver.LocalTandemDataStore
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.mobi.ui.util.DottedDivider
import app.aaps.pump.tandem.mobi.ui.util.HeaderLineWithBackButton
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpVersionResponse

@Composable
fun PumpInfo(innerPadding: PaddingValues = PaddingValues(),
             navigateBack: () -> Unit,
             tandemPumpStatus: TandemPumpStatus? = null,
             resourceHelper: ResourceHelper,
             showHeader: Boolean = true,
             navigateToDebugCommands: () -> Unit = {}
             ) {

    val pumpInfo = LocalTandemDataStore.current.pumpVersionResponse.value
    val apiVersion = LocalTandemDataStore.current.apiVersionResponse.value
    val isMobi = if (tandemPumpStatus==null) true else tandemPumpStatus.tandemPumpFirmware.isMobi()


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
                        text = resourceHelper.gs(R.string.pi_title),
                        onBackClick = navigateBack,
                        resourceHelper = resourceHelper
                    )
                    HorizontalDivider()
                }
            }

            item {
                PumpInfoRow(
                    label = resourceHelper.gs(R.string.pump_serial_number),
                    value = "${pumpInfo?.serialNum}",
                    rowModifier = Modifier.combinedClickable(
                        onClick = {},
                        onDoubleClick = { navigateToDebugCommands() },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
                )
            }

            item {
                PumpInfoRow(label= resourceHelper.gs(R.string.pi_pump_sw), value="${apiVersion?.majorVersion}.${apiVersion?.minorVersion}"    /*pumpInfo.pumpRev*/)
            }

            item {
                PumpInfoRow(label="ARM S/W " + resourceHelper.gs(R.string.pi_version), value="${pumpInfo?.armSwVer}")
            }

            item {
                PumpInfoRow(label=resourceHelper.gs(R.string.pi_sw_part_num), value="${pumpInfo?.partNum}")
            }

            item {
                PumpInfoRow("ConfigA Bits", value="0x%08X".format(pumpInfo?.configABits))
            }

            item {
                PumpInfoRow("ConfigB Bits", value="0x%08X".format(pumpInfo?.configBBits))
            }

            item {
                PumpInfoRow(label= resourceHelper.gs(R.string.pi_pump_model), value=if (isMobi) "t:Mobi (${pumpInfo?.modelNum})" else "t:Slim X2 (${pumpInfo?.modelNum})")
            }

            item {
                PumpInfoRow(label= resourceHelper.gs(R.string.pi_pcba_serial), value="${pumpInfo?.pcbaSN}")
            }


        }

    )
}


@Composable
fun PumpInfoRow(label: String, value: String, rowModifier: Modifier = Modifier) {
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
        Text(text = value, fontSize = 18.sp)
    }
    DottedDivider(    dotRadius = 1.dp,
                      spaceBetween = 6.dp,)
}


@Preview(showBackground = true)
@Composable
private fun DefaultPreview_PumpInfo() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
        ) {
            setUpPreviewState(LocalTandemDataStore.current)
            var pumpVersion = PumpVersionResponse(3628697757L, 0L, 0L, 0L, 1226976L, 1013045L, "0", 232700077L, "0", 1004000L)
            LocalTandemDataStore.current.pumpVersionResponse.value = pumpVersion
            PumpInfo(
                innerPadding = PaddingValues(),
                //navController = null,
                navigateBack = { },
                tandemPumpStatus = null,
                resourceHelper = PreviewResourceHelper()
            )
        }
    }
}
