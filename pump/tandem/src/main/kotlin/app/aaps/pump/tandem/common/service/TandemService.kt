package app.aaps.pump.tandem.common.service

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.objects.workflow.MetroService
import app.aaps.pump.common.defs.PumpErrorType
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.comm.data.PumpStateX2
import app.aaps.pump.tandem.common.data.PumpConfigurationDto
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnectionManager
import app.aaps.pump.tandem.common.keys.TandemBooleanPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemIntPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import com.jwoglom.pumpx2.pump.PumpState
import dev.zacsweers.metro.Inject

class TandemService : MetroService() {

    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var rh: ResourceHelper
    @Inject lateinit var tandemPumpUtil: TandemPumpUtil
    @Inject lateinit var pumpStatus: TandemPumpStatus
    @Inject lateinit var context: Context
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var tandemPumpConnectionManager: TandemPumpConnectionManager


    inner class LocalBinder : Binder() {
        val serviceInstance: TandemService
            get() = this@TandemService
    }

    var pumpAddress: String? = null

    private val mBinder: IBinder = LocalBinder()

    var configurationValid : Boolean = false
    var connected: Boolean = false
    var isInitialized = connected && configurationValid
    var currentConfiguration: PumpConfigurationDto? = null


    override fun onCreate() {
        super.onCreate()
        aapsLogger.debug(LTag.PUMPCOMM, "Tandem Service newly created")
    }


    fun isMobiConfigurationStillValid(config: PumpConfigurationDto): Boolean {

        if (tandemPumpUtil.preventReconnect) {
            return false
        }

        val hasStoredConfiguration =
            config.pumpAddress.isNotEmpty() &&
                config.pumpBondStatus == 100

        val hasImportableSharedConfiguration =
            config.useSharedConnection &&
                config.validSharedConfiguration &&
                !config.sharedConnectionData.isNullOrEmpty() &&
                config.pumpAddress.isNotEmpty()

        return hasStoredConfiguration || hasImportableSharedConfiguration
    }


    fun prepareConfiguration(): PumpConfigurationDto {

        val useSharedConnection: Boolean = tandemPumpUtil.getBooleanPreferenceOrDefault(
            booleanPreferenceKey = TandemBooleanPreferenceKey.UseSharedConnection,
            defaultValue = true)

        var validSharedConfiguration = true
        var sharedConfigurationAlreadyApplied = false
        var pumpAddress = ""
        var pumpSerial = ""
        var pumpBondStatus = 0
        var sharedConnectionString: String? = null

        if (useSharedConnection) {
            aapsLogger.info(LTag.PUMP, "PumpConfig:prepareConfiguration: Shared Connection Use")

            sharedConnectionString = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.SharedConnectionData, null)
            //sp.getStringOrNull(TandemPumpConst.Prefs.SharedConnectionData, null)

            aapsLogger.info(LTag.PUMP, "PumpConfig: Shared Connection: $sharedConnectionString")

            if (sharedConnectionString.isNullOrEmpty()) {
                aapsLogger.warn(LTag.PUMP, "PumpConfig:prepareConfiguration: Shared Connection Use: Data empty")
                validSharedConfiguration = false
            } else {

                try {
                    val sharedConnectionData : PumpStateX2 = tandemPumpUtil.gson.fromJson(sharedConnectionString, PumpStateX2::class.java)

                    if (sharedConnectionData.jpakeDerivedSecret.isEmpty()) {
                        aapsLogger.warn(LTag.PUMP, "PumpConfig: Shared Connection Use: Data NOT Valid - falling back to regular pairing data")
                        // Don't set notFound = true here - allow fallback to regular pairing data below
                        validSharedConfiguration = false
                    } else {

                        if (isSharedConfigurationAlreadyApplied(sharedConnectionData)) {
                            aapsLogger.info(LTag.PUMP, "PumpConfig: Shared Connection looks like it is the same. No setting of this information.")
                            sharedConfigurationAlreadyApplied = true

                            pumpAddress = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.PumpAddress, "")!!
                            pumpBondStatus = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.PumpPairStatus, 0)
                        } else {
                            aapsLogger.info(LTag.PUMP, "PumpConfig: Setting Shared Connection Data. NEW")

                            sharedConfigurationAlreadyApplied = true

                            pumpAddress = sharedConnectionData.savedBluetoothMAC

                            if (!sharedConnectionData.pumpSerialNum.isNullOrEmpty()) {
                                pumpSerial = sharedConnectionData.pumpSerialNum
                            }
                        }
                    }
                } catch (e: Exception) {
                    aapsLogger.error(LTag.PUMP, "PumpConfig: Failed to parse Shared Connection Data: ${e.message} - falling back to regular pairing data", e)
                    // Don't set notFound = true here - allow fallback to regular pairing data below
                    validSharedConfiguration = false
                }
            } // else
        }

        if (pumpAddress.isEmpty()) {
            pumpAddress = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.PumpAddress, "")!!
            pumpBondStatus = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.PumpPairStatus, 0)
        }

        return PumpConfigurationDto(pumpAddress = pumpAddress,
                                    pumpSerial = pumpSerial,
                                    validSharedConfiguration = validSharedConfiguration,
                                    sharedConfigurationAlreadyApplied = sharedConfigurationAlreadyApplied,
                                    pumpBondStatus = pumpBondStatus,
                                    useSharedConnection = useSharedConnection,
                                    sharedConnectionData = sharedConnectionString
        )
    }


    fun connectWithConnectionData(newConfig: PumpConfigurationDto): Boolean {

        if (!isMobiConfigurationStillValid(newConfig)) {
            configurationValid = false

            tandemPumpUtil.setError(errorType = PumpErrorType.ConfiguredPumpNotFound,
                details = rh.gs(R.string.tandem_error_not_bonded))

            aapsLogger.warn(LTag.PUMP, "PumpConfig: Invalid connection configuration")

            return false
        }

        val importSharedConfiguration =
            newConfig.useSharedConnection &&
                newConfig.validSharedConfiguration &&
                !newConfig.sharedConfigurationAlreadyApplied &&
                !newConfig.sharedConnectionData.isNullOrEmpty()

        if (importSharedConfiguration) {

            if (isConnected()) {
                aapsLogger.warn(LTag.PUMP, "PumpConfig: Cannot import shared configuration while connected")
                return false
            }

            try {
                val sharedConnectionString = newConfig.sharedConnectionData

                val sharedConnectionData = tandemPumpUtil.gson.fromJson(
                    sharedConnectionString, PumpStateX2::class.java)

                // Validate before modifying stored credentials.
                require(sharedConnectionData.jpakeDerivedSecret.isNotEmpty())
                require(sharedConnectionData.savedBluetoothMAC.isNotEmpty())

                val serialNumber = sharedConnectionData.pumpSerialNum
                    ?.takeIf { it.isNotEmpty() }
                    ?.toLong()

                // Import into the underlying pump library.
                PumpState.importState(context, sharedConnectionString)

                // Update AAPS preferences.
                preferences.put(TandemStringPreferenceKey.PumpAddress, sharedConnectionData.savedBluetoothMAC)

                preferences.put(TandemStringPreferenceKey.PumpPairCode, sharedConnectionData.pairingCode)

                if (serialNumber != null) {
                    preferences.put(TandemStringPreferenceKey.PumpSerial, serialNumber.toString())
                    pumpStatus.serialNumber = serialNumber
                }

                // Set the paired status last.
                preferences.put(TandemIntPreferenceKey.PumpPairStatus, 100)

                aapsLogger.info(LTag.PUMP, "PumpConfig: Shared connection configuration imported")

            } catch (e: Exception) {
                configurationValid = false

                aapsLogger.error(LTag.PUMP, "PumpConfig: Failed to import shared connection configuration",e)

                return false
            }
        }

        // Validate the credentials actually stored in AAPS.
        val storedAddress = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(
            stringPreferenceKey = TandemStringPreferenceKey.PumpAddress, defaultValue = "")

        val storedBondStatus = tandemPumpUtil.getIntPreferenceOrDefault(
            intPreferenceKey = TandemIntPreferenceKey.PumpPairStatus, defaultValue = 0)

        if (storedAddress.isNullOrEmpty() ||
            storedBondStatus != 100 ||
            tandemPumpUtil.preventReconnect
        ) {
            configurationValid = false

            tandemPumpUtil.setError(errorType = PumpErrorType.ConfiguredPumpNotFound,
                details = rh.gs(R.string.tandem_error_not_bonded))

            return false
        }

        pumpAddress = storedAddress
        configurationValid = true

        tandemPumpUtil.clearError()

        // Record the configuration actually applied to AAPS.
        currentConfiguration = PumpConfigurationDto(
            pumpAddress = storedAddress,
            pumpSerial = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(
                stringPreferenceKey = TandemStringPreferenceKey.PumpSerial, defaultValue = "") ?: "",
            validSharedConfiguration = newConfig.validSharedConfiguration,
            sharedConfigurationAlreadyApplied = importSharedConfiguration || newConfig.sharedConfigurationAlreadyApplied,
            pumpBondStatus = storedBondStatus,
            useSharedConnection = newConfig.useSharedConnection,
            sharedConnectionData = newConfig.sharedConnectionData
        )

        return connectToPump()
    }


    fun reconnectWithDifferentConnectionData(newConfig: PumpConfigurationDto): Boolean {

        aapsLogger.info(LTag.PUMP, "PumpConfig: Reconnecting with different connection data")

        // Disconnect before changing credentials.
        if (isConnected()) {
            disconnectFromPump()
        }

        // Make sure the previous connection has actually stopped.
        if (isConnected()) {
            aapsLogger.warn(LTag.PUMP, "PumpConfig: Cannot apply new configuration while still connected")
            return false
        }

        // Validate, apply credentials if necessary, and connect.
        return connectWithConnectionData(newConfig)
    }


    fun isSharedConfigurationAlreadyApplied(sharedConnectionData: PumpStateX2): Boolean {

        // check our internal stuff
        val address = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(
            stringPreferenceKey = TandemStringPreferenceKey.PumpAddress, defaultValue = null)
        val pairCode = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(
            stringPreferenceKey = TandemStringPreferenceKey.PumpPairCode, defaultValue = null)


        return (
            tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.PumpPairStatus, 0)==100 &&
            address!=null && address.equals(sharedConnectionData.savedBluetoothMAC) &&
            pairCode!=null && pairCode.equals(sharedConnectionData.pairingCode) &&
            PumpState.getJpakeDerivedSecret(context).equals(sharedConnectionData.jpakeDerivedSecret) &&
            PumpState.getJpakeServerNonce(context).equals(sharedConnectionData.jpakeServerNonce) &&
            PumpState.getSavedBluetoothMAC(context).equals(sharedConnectionData.savedBluetoothMAC) &&
            PumpState.getPairingCode(context).equals(sharedConnectionData.pairingCode)
            )
    }


    fun connectToPump(): Boolean {
        if (!this.tandemPumpConnectionManager.isConnected()) {
            val status = this.tandemPumpConnectionManager.connectToPump()
            aapsLogger.info(LTag.PUMP, "SRV: connected=$status")
            return status
        } else {
            return true
        }
    }


    fun disconnectFromPump() {
        if (this.tandemPumpConnectionManager.isConnected()) {
            val status = this.tandemPumpConnectionManager.disconnectFromPump()
            aapsLogger.info(LTag.PUMP, "SRV: disconnectFromPump: connected=$status")
        }
    }


    fun isConnected(): Boolean  {
        return this.tandemPumpConnectionManager!=null && this.tandemPumpConnectionManager.isConnected()
    }


    override fun onBind(p0: Intent?): IBinder {
        return mBinder
    }
}