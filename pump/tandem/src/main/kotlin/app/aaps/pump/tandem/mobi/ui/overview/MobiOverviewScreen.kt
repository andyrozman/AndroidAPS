package app.aaps.pump.tandem.mobi.ui.overview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.ui.compose.pump.PumpOverviewScreen
import app.aaps.core.ui.R as Rco

@Composable
fun MobiOverviewScreen(
    viewModel: MobiOverviewViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val iconRes = Rco.drawable.ic_tmobi_128

    PumpOverviewScreen(
        state = uiState,
        customContent = {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                contentScale = ContentScale.Fit
            )
        }
    )
}
