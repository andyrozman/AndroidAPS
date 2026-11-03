package app.aaps.pump.tandem.mobi.ui.wizard

import android.bluetooth.BluetoothManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.aaps.core.interfaces.pump.BlePreCheck
import app.aaps.core.ui.compose.metroViewModel
import app.aaps.core.ui.compose.pump.BlePreCheckHost



@Composable
fun TandemPairWizardHost(
    blePreCheck: BlePreCheck,
    onFinish: () -> Unit
) {
    var bleReady by remember { mutableStateOf(false) }

    val wizardViewModel: TandemMobiConnectionWizardViewModel = metroViewModel()
    val navController = rememberNavController()
    var isRePairing by remember {
        mutableStateOf(false)
    }


    BlePreCheckHost(
        blePreCheck = blePreCheck,
        onReady = { bleReady = true },
        onFailed = onFinish
    )

    if (!bleReady) return

    val context = LocalContext.current.applicationContext
    val bluetoothManager =
        context.getSystemService(BluetoothManager::class.java)


    LaunchedEffect(wizardViewModel) {
        wizardViewModel.initializeBLEScanner(
            bluetoothManager?.adapter
        )
    }


    // Determine start destination based on existing pairing
    val startDestination = if (!isRePairing && wizardViewModel.hasExistingPairing()) {
        // Load existing pump info into state and start at existing pump screen
        wizardViewModel.loadExistingPumpInfo()
        WizardRoutes.EXISTING_PUMP
    } else {
        WizardRoutes.INTRODUCTION
    }

    WizardContent(
        navController = navController,
        viewModel = wizardViewModel,
        startDestination = startDestination,
        onFinish = onFinish,
        onCreatePairingManager = { address -> wizardViewModel.createPairingManager(address) },
        onRequestPumpDisconnect = {
            wizardViewModel.tearDownExistingPump(
                onDone = {
                    navController.navigate(WizardRoutes.INTRODUCTION) {
                        popUpTo(WizardRoutes.EXISTING_PUMP) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    )

}


@Composable
private fun WizardContent(
    navController: NavHostController,
    viewModel: TandemMobiConnectionWizardViewModel,
    startDestination: String,
    onFinish: () -> Unit,
    onCreatePairingManager: (String) -> Unit,
    onRequestPumpDisconnect: () -> Unit
) {
    Scaffold(
        topBar = {
            Column {
                // FIXME Not sure if this will be enough
                // Text(
                //     text = stringResource(R.string.tandem_wizard_title),
                //     style = MaterialTheme.typography.titleLarge,
                //     modifier = Modifier
                //         .fillMaxWidth()
                //         .padding(16.dp),
                //     textAlign = TextAlign.Center
                // )
                // Progress indicator based on current route
                val currentRoute = navController.currentBackStackEntryFlow.collectAsState(initial = navController.currentBackStackEntry)
                val progress = calculateProgress(currentRoute.value?.destination?.route)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            WizardNavHost(
                navController = navController,
                viewModel = viewModel,
                startDestination = startDestination,
                onFinish = onFinish,
                onCreatePairingManager = onCreatePairingManager,
                onRequestPumpDisconnect = onRequestPumpDisconnect
            )
        }
    }
}

private fun calculateProgress(route: String?): Float {
    return when (route) {
        WizardRoutes.EXISTING_PUMP -> 0.0f
        WizardRoutes.INTRODUCTION -> 0.0f
        WizardRoutes.DEVICE_LIST -> 0.25f
        WizardRoutes.ENTER_PIN -> 0.5f
        WizardRoutes.PAIRING, WizardRoutes.ERROR -> 0.75f
        WizardRoutes.COMPLETE -> 1.0f
        else -> 0.0f
    }
}
