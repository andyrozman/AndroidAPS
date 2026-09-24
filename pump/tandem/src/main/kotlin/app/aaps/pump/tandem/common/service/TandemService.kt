package app.aaps.pump.tandem.common.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.pump.BlePreCheck
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.objects.workflow.MetroService
import app.aaps.pump.common.defs.PumpUpdateFragmentType
import app.aaps.pump.common.events.EventPumpFragmentValuesChanged
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.comm.data.PumpStateX2
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnectionManager
import app.aaps.pump.tandem.common.keys.TandemBooleanPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemIntPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import app.aaps.pump.tandem.mobi.TandemMobiPumpPlugin
import com.jwoglom.pumpx2.pump.PumpState
import dev.zacsweers.metro.Inject

class TandemService : MetroService() {

    @Inject lateinit var tandemMobiPumpPlugin: TandemMobiPumpPlugin
    @Inject lateinit var aapsLogger: AAPSLogger
    //@Inject lateinit var tandemPumpConnector: TandemPumpConnector
    @Inject lateinit var rh: ResourceHelper
    @Inject lateinit var sp: SP
    @Inject lateinit var rxBus: RxBus
    @Inject lateinit var tandemPumpUtil: TandemPumpUtil
    @Inject lateinit var pumpStatus: TandemPumpStatus
    @Inject lateinit var context: Context
    @Inject lateinit var blePreCheck: BlePreCheck
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



    override fun onCreate() {
        super.onCreate()
        aapsLogger.debug(LTag.PUMPCOMM, "Tandem Service newly created")
    }


    fun checkPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permission = ContextCompat.checkSelfPermission(getApplicationContext(),
                                                               Manifest.permission.BLUETOOTH_SCAN)
            //return permission == PackageManager.PERMISSION_GRANTED;
        }
    }


    fun hasConfigurationChanged(): Boolean {
        // TODO if parameters haven't changed we don't
        return false;
    }

    fun reconnectWithDifferentConnectionData() {
        // TODO reconnectWithDifferentConnectionData - implement
        //  rework this, if data changed we check if connected if yes disconnect, then validate
        //   paramteres and if ok, connect to the pump
        // if (tandemService!!.validateParameters() && !tandemService!!.isConnected()) {
        //     tandemService!!.connectToPump()
        // }


        // val value: String? = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.SharedConnectionData, null)
        //     //sp.getStringOrNull(R.string.key_tandem_shared_connection_data, null)
        // aapsLogger.info(LTag.PUMP, "TANDEMDBG: Received event that connection data changed. Reconnecting to pump")
        // if (value!=null && value.isNotEmpty()) {
        //     if (tandemService!!.validateParameters() && !tandemService!!.isConnected()) {
        //         tandemService!!.connectToPump()
        //     }
        // }

    }


    fun validateParameters(): Boolean {

        checkPermission()

        var pumpConfigured = false

        val useSharedConnection: Boolean = tandemPumpUtil.getBooleanPreferenceOrDefault(TandemBooleanPreferenceKey.UseSharedConnection, true)
            //sp.getBoolean(TandemPumpConst.Prefs.UseSharedConnection, true)

        if (useSharedConnection) {
            aapsLogger.info(LTag.PUMP, "PumpConfig: Shared Connection Use")

            val sharedConnectionString = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.SharedConnectionData, null)
                //sp.getStringOrNull(TandemPumpConst.Prefs.SharedConnectionData, null)

            aapsLogger.info(LTag.PUMP, "PumpConfig: Shared Connection: ${sharedConnectionString}")

            if (sharedConnectionString.isNullOrEmpty()) {
                aapsLogger.warn(LTag.PUMP, "PumpConfig: Shared Connection Use: Data empty - falling back to regular pairing data")
                // Don't set notFound = true here - allow fallback to regular pairing data below
            } else {

                //aapsLogger.debug("Shared Connection String")

                try {
                    val sharedConnectionData : PumpStateX2 = tandemPumpUtil.gson.fromJson(sharedConnectionString, PumpStateX2::class.java)

                    if (sharedConnectionData.jpakeDerivedSecret.isEmpty()) {
                        aapsLogger.warn(LTag.PUMP, "PumpConfig: Shared Connection Use: Data NOT Valid - falling back to regular pairing data")
                        // Don't set notFound = true here - allow fallback to regular pairing data below
                    } else {

                        if (isSharedConfigurationAlreadyApplied(sharedConnectionData)) {
                            aapsLogger.info(LTag.PUMP, "PumpConfig: Shared Connection looks like it is the same. No setting of this information.")
                        } else {
                            aapsLogger.info(LTag.PUMP, "PumpConfig: Setting Shared Connection Data. NEW")

                            preferences.put(TandemIntPreferenceKey.PumpPairStatus, 100)
                            //sp.putInt(TandemPumpConst.Prefs.PumpPairStatus, 100)
                            //sp.putString(TandemPumpConst.Prefs.PumpAddress, sharedConnectionData.savedBluetoothMAC)
                            preferences.put(TandemStringPreferenceKey.PumpAddress, sharedConnectionData.savedBluetoothMAC)
                            //sp.getString(TandemPumpConst.Prefs.PumpPairCode, sharedConnectionData.pairingCode)
                            preferences.put(TandemStringPreferenceKey.PumpPairCode, sharedConnectionData.pairingCode)

                            if (!sharedConnectionData.pumpSerialNum.isNullOrEmpty()) {
                                preferences.put(TandemStringPreferenceKey.PumpSerial, "" + sharedConnectionData.pumpSerialNum)
                                //sp.putString(TandemPumpConst.Prefs.PumpSerial, "" + sharedConnectionData.pumpSerialNum)
                                pumpStatus.serialNumber = (sharedConnectionData.pumpSerialNum.toLong())
                            }

                            PumpState.importState(context, sharedConnectionString)
                        }
                    }
                } catch (e: Exception) {
                    aapsLogger.error(LTag.PUMP, "PumpConfig: Failed to parse Shared Connection Data: ${e.message} - falling back to regular pairing data", e)
                    // Don't set notFound = true here - allow fallback to regular pairing data below
                }
            } // else

            // Only return false if we explicitly require shared connection AND it's not found
            // Otherwise, fall through to check regular pairing data
            // Note: Removed the early return here to allow fallback to regular pairing
        }


        pumpAddress = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.PumpAddress, "")
            //sp.getString(TandemPumpConst.Prefs.PumpAddress, "")

        val pumpBondStatus = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.PumpPairStatus, -1)
            //sp.getInt(TandemPumpConst.Prefs.PumpPairStatus, -1)


        aapsLogger.debug(LTag.PUMP, "PumpConfig: Pump Mobi [address=$pumpAddress,bondStatus=$pumpBondStatus]")

        pumpConfigured = (pumpAddress!!.isNotEmpty() &&
            pumpBondStatus == 100 &&
            !tandemPumpUtil.preventConnect)

        aapsLogger.debug(LTag.PUMP, "Service: Validation of parameters - Pump Configured: $pumpConfigured")

        if (!pumpConfigured) {
            pumpStatus.errorDescription = rh.gs(R.string.tandem_error_not_bonded)
            rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.None))
        } else {
            if (!pumpStatus.errorDescription.isNullOrEmpty()) {
                pumpStatus.errorDescription = null
                rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Configuration))
            }
        }

        this.configurationValid = pumpConfigured

        aapsLogger.info(LTag.PUMP, "SRV: configurationValid=$configurationValid")

        return pumpConfigured

    }

    fun isSharedConfigurationAlreadyApplied(sharedConnectionData: PumpStateX2): Boolean {

        // check our internal stuff
        val address = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.PumpAddress, null)
            //sp.getStringOrNull(TandemPumpConst.Prefs.PumpAddress, null)
        val pairCode = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.PumpPairCode, null)
            //sp.getStringOrNull(TandemPumpConst.Prefs.PumpPairCode, null)


        return (//sp.getInt(TandemPumpConst.Prefs.PumpPairStatus, 0)==100
            tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.PumpPairStatus, 0)==100 &&
            address!=null && address.equals(sharedConnectionData.savedBluetoothMAC) &&
            pairCode!=null && pairCode.equals(sharedConnectionData.pairingCode) &&
            PumpState.getJpakeDerivedSecret(context).equals(sharedConnectionData.jpakeDerivedSecret) &&
            PumpState.getJpakeServerNonce(context).equals(sharedConnectionData.jpakeServerNonce) &&
            PumpState.getSavedBluetoothMAC(context).equals(sharedConnectionData.savedBluetoothMAC) &&
            PumpState.getPairingCode(context).equals(sharedConnectionData.pairingCode)
            //PumpState.getPumpAPIVersion(context).equals(sharedConnectionData.savedBluetoothMAC) &&
            )
    }






    fun connectToPump(): Boolean {
        if (!this.tandemPumpConnectionManager.isConnected()) {
            val status = this.tandemPumpConnectionManager.connectToPump()
            aapsLogger.info(LTag.PUMP, "SRV: connected=$status")
            return status
        } else {
            return true;
        }
    }

    fun disconnectFromPump() {
        if (this.tandemPumpConnectionManager.isConnected()) {
            val status = this.tandemPumpConnectionManager.disconnectFromPump()
            aapsLogger.info(LTag.PUMP, "SRV: disconnectFromPump: connected=$status")
        }
    }


    fun isConnected(): Boolean  {
        return this.tandemPumpConnectionManager.isConnected()
    }

    override fun onBind(p0: Intent?): IBinder {
        return mBinder
    }
}