package app.aaps.pump.tandem.mobi.ui.wizard

import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.comm.maint.TandemPairingManager
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.tandem.common.util.PumpX2L
import app.aaps.pump.tandem.common.util.TandemPumpUtil

import androidx.compose.runtime.collectAsState
import app.aaps.core.ui.compose.MetroAppCompatActivity
import app.aaps.pump.common.events.EventPumpForceDisconnect
import dev.zacsweers.metro.Inject

/**
 * Jetpack Compose-based wizard for pairing Tandem Mobi pump
 * Follows the pattern of Omnipod Dash activation wizard
 */
class TandemMobiConnectionWizardActivity : MetroAppCompatActivity() {

    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var aapsSchedulers: AapsSchedulers
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var tandemPumpUtil: TandemPumpUtil
    @Inject lateinit var pumpStatus: TandemPumpStatus
    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var pumpX2L: PumpX2L
    @Inject lateinit var pumpSync: PumpSync

    private lateinit var viewModel: TandemMobiConnectionWizardViewModel
    private var pairingManager: TandemPairingManager? = null
    private var needsPairingReset = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel
        viewModel = TandemMobiConnectionWizardViewModel(
            aapsLogger = aapsLogger,
            rxBus = rxBus,
            aapsSchedulers = aapsSchedulers,
            preferences = preferences,
            tandemPumpUtil = tandemPumpUtil
        )

        // Initialize BLE scanner in ViewModel
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?
        viewModel.initializeBLEScanner(bluetoothManager?.adapter)

        // Check if this is a re-pairing request
        val isRePairing = intent.getBooleanExtra(EXTRA_IS_RE_PAIRING, false)
        if (isRePairing) {
            needsPairingReset = true
            tearDownExistingPump(onDone = { viewModel.startRePairing() })
        }

        // Determine start destination based on existing pairing
        val startDestination = if (!isRePairing && viewModel.hasExistingPairing()) {
            // Load existing pump info into state and start at existing pump screen
            viewModel.loadExistingPumpInfo()
            WizardRoutes.EXISTING_PUMP
        } else {
            WizardRoutes.INTRODUCTION
        }

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                WizardContent(
                    navController = navController,
                    viewModel = viewModel,
                    startDestination = startDestination,
                    onFinish = { finish() },
                    onCreatePairingManager = { address -> createPairingManager(address) },
                    onRequestPumpDisconnect = { handleExistingPumpRemoval() }
                )
            }
        }
    }

    private fun createPairingManager(btAddress: String): TandemPairingManager? {
        if (btAddress.isEmpty()) {
            return pairingManager
        }

        pairingManager?.let {
            if (it.btAddress == btAddress) {
                return it
            }
            it.shutdownPairingManager()
        }

        // Create a dummy PumpBLEConfigActivity interface for the pairing manager
       // val dummyActivity = object : PumpBLEConfigActivity() {}

        pairingManager = TandemPairingManager(
            context = this,
            aapsLogger = aapsLogger,
            preferences = preferences,
            tandemPumpUtil = tandemPumpUtil,
            btAddress = btAddress,
            resourceHelper = resourceHelper,
            rxBus = rxBus,
            pumpStatus = pumpStatus,
            pumpSync = pumpSync,
            //activity = dummyActivity,
            pumpX2L = pumpX2L,
            aapsSchedulers = aapsSchedulers
        ).also { manager ->
            viewModel.setPairingManager(manager)
            // Note: Pairing data already cleared by handleExistingPumpRemoval if needed
            // No need to clear again here
        }

        needsPairingReset = false

        return pairingManager
    }

    override fun onDestroy() {
        super.onDestroy()
        pairingManager?.shutdownPairingManager()
    }

    /**
     * Handle removal of existing pump: disconnect and clear pairing data
     */
    private fun handleExistingPumpRemoval() {
        tearDownExistingPump(onDone = {
            needsPairingReset = true
            // Note: wizard state is reset by viewModel.onConfirmRemoveExistingPump() with isRePairing=false
        })
    }

    /**
     * Full teardown of any previously-paired pump: disconnect the live BLE session,
     * remove the Android-level BT bond, and clear AAPS / pumpx2 pairing state.
     * Shared between the "remove existing pump" flow and the EXTRA_IS_RE_PAIRING entry.
     */
    private fun tearDownExistingPump(onDone: () -> Unit) {
        aapsLogger.info(LTag.PUMP, "Tearing down existing pump session")

        // Capture the address BEFORE clearAllPairingData wipes it.
        val oldAddress = preferences.get(TandemStringPreferenceKey.PumpAddress)

        // Ask the service to disconnect first. This routes through
        // TandemPumpConnector.disconnectFromPump → TandemCommunicationManager.disconnect
        // → TandemPumpUtil.forceResetBluetoothHandler (neutralize + cancel + stop + null singleton),
        // and nulls the connector's cached manager/address.
        rxBus.send(EventPumpForceDisconnect())

        // Let the disconnect propagate through the rx chain, then remove the OS bond
        // and clear prefs / PumpState / pumpStatus.
        Handler(Looper.getMainLooper()).postDelayed({
            tandemPumpUtil.removeAndroidBond(oldAddress)
            tandemPumpUtil.clearAllPairingData()
            aapsLogger.info(LTag.PUMP, "Existing pump torn down, ready for new pairing")
            onDone()
        }, 1000)
    }

    companion object {
        const val EXTRA_IS_RE_PAIRING = "is_re_pairing"
    }
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
                Text(
                    text = stringResource(R.string.tandem_wizard_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
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
