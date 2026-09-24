package app.aaps.pump.tandem.common.util

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.keys.interfaces.BooleanPreferenceKey
import app.aaps.core.keys.interfaces.IntPreferenceKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.keys.interfaces.StringPreferenceKey
import app.aaps.core.utils.pump.ByteUtil
import app.aaps.pump.common.defs.PumpDriverState
import app.aaps.pump.common.events.EventPumpDriverStateChanged
import com.jwoglom.pumpx2.pump.messages.helpers.Dates
import app.aaps.pump.common.utils.PumpUtil
import app.aaps.pump.tandem.common.data.defs.RefreshData
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.events.EventRefreshPumpData
import app.aaps.pump.tandem.common.keys.TandemIntPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.common.events.EventPumpConnectionParametersChanged
import com.jwoglom.pumpx2.pump.PumpState
import com.jwoglom.pumpx2.pump.bluetooth.TandemBluetoothHandler
import com.jwoglom.pumpx2.pump.messages.Message
import dev.zacsweers.metro.AppScope
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@Inject
class TandemPumpUtil(
    aapsLogger: AAPSLogger,
    rxBus: RxBus,
    context: Context,
    resourceHelper: ResourceHelper,
    notificationManager: NotificationManager,
    preferences: Preferences,
    var tandemPumpStatus: TandemPumpStatus,

): PumpUtil(aapsLogger, rxBus, context, resourceHelper, notificationManager, preferences) {

    fun getTimeFromPumpAsEpochMillis(pumpTime: Long): Long {
        return Dates.fromJan12008EpochSecondsToDate(pumpTime).toEpochMilli();
    }


    override fun resetDriverStatusToConnected() {
        workWithStatusAndCommand(StatusChange.SetStatus, PumpDriverState.Connected, null)
    }


    fun getIntPreferenceOrDefault(intPreferenceKey: IntPreferenceKey, defaultValue: Int? =null): Int {
        return if (preferences.getIfExists(intPreferenceKey)==null)
            defaultValue ?: intPreferenceKey.defaultValue
        else
            preferences.get(intPreferenceKey)
    }


    fun getStringPreferenceOrDefault(stringPreferenceKey: StringPreferenceKey, defaultValue: String? =null): String {
        return if (preferences.getIfExists(stringPreferenceKey)==null)
            defaultValue ?: stringPreferenceKey.defaultValue
        else
            preferences.get(stringPreferenceKey)
    }


    fun getStringPreferenceOrDefaultOrNull(stringPreferenceKey: StringPreferenceKey, defaultValue: String? =null): String? {
        return if (preferences.getIfExists(stringPreferenceKey)==null)
            defaultValue ?: stringPreferenceKey.defaultValue
        else
            preferences.get(stringPreferenceKey)
    }


    fun getBooleanPreferenceOrDefault(booleanPreferenceKey: BooleanPreferenceKey, defaultValue: Boolean? =null): Boolean {
        return if (preferences.getIfExists(booleanPreferenceKey)==null)
            defaultValue ?: booleanPreferenceKey.defaultValue
        else
            preferences.get(booleanPreferenceKey)
    }

    // fun isSame(d1: Double, d2: Double): Boolean {
    //     val diff = d1 - d2
    //     return Math.abs(diff) <= 0.000001
    // }
    //
    // fun isSame(d1: Double, d2: Int): Boolean {
    //     val diff = d1 - d2
    //     return Math.abs(diff) <= 0.000001
    // }



    fun refreshPumpStatus(data: List<RefreshData>) {
        rxBus.send(EventRefreshPumpData(data))
    }


    init {
        driverStatusInternal = PumpDriverState.Connecting
    }


    var historyProgress: String? = null
        get() {
            return field
        }
        set(status) {
            field = status
            rxBus.send(EventPumpDriverStateChanged(if (status==null) PumpDriverState.Connected
                                                   else PumpDriverState.ExecutingCommand))
        }

    /**
     * Clear all pairing data to allow re-pairing with a pump
     * This can be called without needing a TandemPairingManager instance
     */
    /**
     * Forcibly tears down the pumpx2 TandemBluetoothHandler singleton: cancels
     * any live peripheral connection, stops the BluetoothCentralManager, and
     * clears the static singleton so the next getInstance() call constructs a
     * fresh handler bound to whichever TandemPump asks for it.
     *
     * Steps 1–3 (Handler neutralization, peripheral cancel, central stop) are
     * still done here because pumpx2's resetInstance() only nulls the static
     * field — it does not tear down the live BLE state.
     */
    fun forceResetBluetoothHandler(handler: TandemBluetoothHandler?) {
        try {
            handler?.let { h ->
                // 1. Neutralize the handler's internal android.os.Handler BEFORE cancelling
                //    the connection. blessed dispatches onDisconnectedPeripheral back to the
                //    main Looper, and its default path posts a 250ms immediateConnectToPeripheral
                //    runnable on this handler. If we only call removeCallbacksAndMessages, the
                //    post-cancellation disconnect callback still races in afterwards and enqueues
                //    NEW reconnect runnables on the live handler, reviving the orphan.
                //
                //    Instead, reflection-swap the final `handler` field with a dead Handler
                //    whose sendMessageAtTime always returns false. Any subsequent postDelayed
                //    from the stale peripheralCallback / centralManagerCallback becomes a no-op.
                try {
                    val handlerField = TandemBluetoothHandler::class.java.getDeclaredField("handler")
                    handlerField.isAccessible = true
                    val androidHandler = handlerField.get(h) as? android.os.Handler
                    androidHandler?.removeCallbacksAndMessages(null)

                    val inert = object : android.os.Handler(android.os.Looper.getMainLooper()) {
                        override fun sendMessageAtTime(msg: android.os.Message, uptimeMillis: Long): Boolean = false
                    }
                    handlerField.set(h, inert)
                    aapsLogger.info(LTag.PUMPBTCOMM, "forceResetBluetoothHandler: neutralized internal Handler")
                } catch (e: Exception) {
                    aapsLogger.error(LTag.PUMPBTCOMM, "Failed to neutralize TandemBluetoothHandler.handler", e)
                }

                // 2. Cancel every live peripheral so blessed drops the actual GATT link.
                //    onDisconnectedPeripheral will fire after this, but its postDelayed
                //    reconnect is now a silent no-op thanks to step 1.
                h.central?.let { central ->
                    for (peripheral in central.connectedPeripherals) {
                        aapsLogger.info(LTag.PUMPBTCOMM, "forceResetBluetoothHandler: cancelling connection to ${peripheral.address}")
                        central.cancelConnection(peripheral)
                    }
                }

                // 3. Close the central itself (stopScan + close).
                try {
                    h.stop()
                } catch (e: IllegalArgumentException) {
                    aapsLogger.error(LTag.PUMPBTCOMM, "Ignoring IllegalArgumentException during handler.stop()", e)
                }
            }
        } catch (e: Exception) {
            aapsLogger.error(LTag.PUMPBTCOMM, "Error while tearing down TandemBluetoothHandler", e)
        }

        // 4. Clear the static singleton so the next getInstance() constructs a fresh handler
        //    bound to whichever TandemPump asks for it.
        try {
            TandemBluetoothHandler.resetInstance()
            aapsLogger.info(LTag.PUMPBTCOMM, "TandemBluetoothHandler singleton cleared")
        } catch (e: Exception) {
            aapsLogger.error(LTag.PUMPBTCOMM, "Failed to reset TandemBluetoothHandler singleton", e)
        }
    }

    /**
     * Removes the Android-level BT bond for the given pump address. Uses reflection to
     * reach BluetoothDevice.removeBond() which is @hide but publicly reachable on all
     * Android versions. Call this BEFORE clearing the address pref, otherwise there's
     * nothing to unbond against. Async — returns quickly; actual unbond arrives via
     * ACTION_BOND_STATE_CHANGED broadcast.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun removeAndroidBond(btAddress: String?): Boolean {
        if (btAddress.isNullOrEmpty()) {
            aapsLogger.info(LTag.PUMPBTCOMM, "removeAndroidBond: no address provided, skipping")
            return false
        }
        return try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter
            if (adapter == null) {
                aapsLogger.warn(LTag.PUMPBTCOMM, "removeAndroidBond: BluetoothAdapter unavailable")
                return false
            }
            val device = adapter.getRemoteDevice(btAddress)
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                aapsLogger.info(LTag.PUMPBTCOMM, "removeAndroidBond: $btAddress not bonded (state=${device.bondState}), skipping")
                return false
            }
            val method = device.javaClass.getMethod("removeBond")
            val ok = method.invoke(device) as Boolean
            aapsLogger.info(LTag.PUMPBTCOMM, "removeAndroidBond: $btAddress removeBond queued=$ok")
            ok
        } catch (e: Exception) {
            aapsLogger.error(LTag.PUMPBTCOMM, "removeAndroidBond failed for $btAddress", e)
            false
        }
    }

    fun clearAllPairingData() {
        aapsLogger.info(LTag.PUMPCOMM, "TandemPumpUtil: Clearing all pairing data for re-pairing")

        // Clear preferences
        preferences.put(TandemIntPreferenceKey.PumpPairStatus, -1)
        preferences.put(TandemStringPreferenceKey.PumpAddress, "")
        preferences.put(TandemStringPreferenceKey.PumpPairCode, "")
        preferences.put(TandemStringPreferenceKey.PumpSerial, "")
        preferences.put(TandemStringPreferenceKey.PumpName, "")
        preferences.put(TandemStringPreferenceKey.PumpVersionResponse, "")
        preferences.put(TandemStringPreferenceKey.PumpApiVersion, "")

        // Clear PumpX2 library state
        try {
            PumpState.resetState(context)
            aapsLogger.info(LTag.PUMPCOMM, "TandemPumpUtil: PumpState cleared successfully")
        } catch (e: Exception) {
            aapsLogger.error(LTag.PUMPCOMM, "TandemPumpUtil: Error clearing PumpState", e)
        }

        // Reset pump status
        tandemPumpStatus.serialNumber = 0L
        tandemPumpStatus.errorDescription = ""

        // Notify UI and service
        rxBus.send(EventPumpConnectionParametersChanged())

        aapsLogger.info(LTag.PUMPCOMM, "TandemPumpUtil: All pairing data cleared successfully")
    }


    val TBR_PREFIX: Int = 100000000;

    val BOLUS_PREFIX: Int = 500000000;

    // we don't use real pumpId, because each of items would have a lot of entries, but each of boluses or tbr has
    // also uniqueId, which is the one we use, prefixed, so that we differentiate
    fun getPrefixedIdForDb(pumpEventId: Long, isBolus: Boolean): Long {
        val newNumber = if (isBolus) BOLUS_PREFIX else TBR_PREFIX
        return newNumber + pumpEventId
    }



    companion object {

        const val MAX_RETRY = 2


    }
}
