package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble

import android.bluetooth.*
import android.content.Context
import android.os.SystemClock
import com.google.gson.Gson
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil
import info.nightscout.androidaps.plugins.pump.common.utils.ThreadUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.BLECommOperationResultType
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.GattStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattService.Companion.isYpsoPumpDevice
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattService.Companion.lookup
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations.BLECommOperation
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations.BLECommOperationResult
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations.CharacteristicReadOperation
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations.CharacteristicWriteOperation
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpErrorType
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.utils.sharedPreferences.SP
import org.apache.commons.lang3.StringUtils
import java.util.*
import java.util.concurrent.Semaphore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YpsoPumpBLE @Inject constructor(
    var aapsLogger: AAPSLogger,
    var sp: SP,
    var context: Context,
    var pumpStatus: YpsopumpPumpStatus,
    var ypsoPumpUtil: YpsoPumpUtil) {

    var gson = Gson()
    var bluetoothAdapter: BluetoothAdapter? = null
    var ypsopumpDevice: BluetoothDevice? = null
    var bluetoothGattCallback: BluetoothGattCallback

    private var gattDebugEnabled = true
    private var manualDisconnect = false
    private var bluetoothConnectionGatt: BluetoothGatt? = null
    private val characteristicMap: MutableMap<String, BluetoothGattCharacteristic> = HashMap()
    private var mCurrentOperation: BLECommOperation? = null
    private val gattOperationSema = Semaphore(1, true)

    var deviceMac = "EC:2A:F0:00:8B:8E"

    init {
        bluetoothGattCallback = object : BluetoothGattCallback() {

            override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                                 characteristic: BluetoothGattCharacteristic) {
                super.onCharacteristicChanged(gatt, characteristic)
                if (gattDebugEnabled) {
                    aapsLogger.debug(LTag.PUMPBTCOMM, "onCharacteristicChanged "
                        + YpsoGattCharacteristic.lookup(characteristic.uuid) + " "
                        + ByteUtil.getHex(characteristic.value))
//                    if (characteristic.getUuid().equals(UUID.fromString(YpsoGattAttributes.CHARA_RADIO_RESPONSE_COUNT))) {
//                        aapsLogger.debug(LTag.PUMPBTCOMM, "Response Count is " + ByteUtil.shortHexString(characteristic.getValue()));
//                    }
                }
//                if (radioResponseCountNotified != null) {
//                    radioResponseCountNotified.run();
//                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt,
                                              characteristic: BluetoothGattCharacteristic, status: Int) {
                super.onCharacteristicRead(gatt, characteristic, status)
                val gattStatus = GattStatus.getByStatusCode(status)
                val statusMessage = getGattStatusMessage(status)
                if (gattDebugEnabled) {
                    aapsLogger.debug(LTag.PUMPBTCOMM, ThreadUtil.sig() + "onCharacteristicRead ("
                        + YpsoGattCharacteristic.lookup(characteristic.uuid) + ") " + statusMessage + ":"
                        + ByteUtil.getHex(characteristic.value))
                }
                mCurrentOperation!!.gattOperationCompletionCallback(characteristic.uuid, characteristic.value, gattStatus)
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt,
                                               characteristic: BluetoothGattCharacteristic, status: Int) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                //val uuidString = YpsoGattCharacteristic.lookup(characteristic.uuid)
                if (gattDebugEnabled) {

                    aapsLogger.debug(LTag.PUMPBTCOMM, ThreadUtil.sig() + "onCharacteristicWrite ("
                        + YpsoGattCharacteristic.lookup(characteristic.uuid) + ") " + getGattStatusMessage(status) + ":"
                        + ByteUtil.shortHexString(characteristic.value))
                }
                val gattStatus = GattStatus.getByStatusCode(status)
                mCurrentOperation!!.gattOperationCompletionCallback(characteristic.uuid, characteristic.value, gattStatus)
            }

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)

                // https://github.com/NordicSemiconductor/puck-central-android/blob/master/PuckCentral/app/src/main/java/no/nordicsemi/puckcentral/bluetooth/gatt/GattManager.java#L117
                if (status == 133) {
                    aapsLogger.error(LTag.PUMPBTCOMM, "Got the status 133 bug, closing gatt")
                    disconnect()
                    SystemClock.sleep(500)
                    return
                }
                if (gattDebugEnabled) {
                    val stateMessage: String
                    stateMessage = if (newState == BluetoothProfile.STATE_CONNECTED) {
                        "CONNECTED"
                    } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                        "CONNECTING"
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        "DISCONNECTED"
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                        "DISCONNECTING"
                    } else {
                        "UNKNOWN newState ($newState)"
                    }
                    aapsLogger.warn(LTag.PUMPBTCOMM, "onConnectionStateChange " + getGattStatusMessage(status) + " " + stateMessage)
                }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //pumpStatus.connectionStatus = YpsoConnectionStatus.CONNECTED
                        discoverServices()
                        //rileyLinkUtil.sendBroadcastMessage(RileyLinkConst.Intents.BluetoothConnected, context);
                    } else {
                        aapsLogger.debug(LTag.PUMPBTCOMM, String.format(Locale.ENGLISH, "BT State connected, GATT status %d (%s)", status, getGattStatusMessage(status)))
                    }
                } else if (newState == BluetoothProfile.STATE_CONNECTING ||  //
                    newState == BluetoothProfile.STATE_DISCONNECTING) {
                    aapsLogger.debug(LTag.PUMPBTCOMM, String.format(Locale.ENGLISH, "We are in %s state.", if (status == BluetoothProfile.STATE_CONNECTING) "Connecting" else "Disconnecting"))
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                    //rileyLinkUtil.sendBroadcastMessage(RileyLinkConst.Intents.RileyLinkDisconnected, context);
                    if (manualDisconnect) close()
                    aapsLogger.warn(LTag.PUMPBTCOMM, "YpsoPump Disconnected.")
                    ypsoPumpUtil.driverStatus = PumpDriverState.Disconnected
                } else {
                    aapsLogger.warn(LTag.PUMPBTCOMM, String.format(Locale.ENGLISH, "Some other state: (status=%d, newState=%d)", status, newState))
                }
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                super.onDescriptorWrite(gatt, descriptor, status)
                if (gattDebugEnabled) {
                    aapsLogger.warn(LTag.PUMPBTCOMM, "onDescriptorWrite " + YpsoGattCharacteristic.lookup(descriptor.uuid) + " "
                        + getGattStatusMessage(status) + " written: " + ByteUtil.getHex(descriptor.value))
                }
                //mCurrentOperation.gattOperationCompletionCallback(descriptor.getUuid(), descriptor.getValue());
            }

            override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                super.onDescriptorRead(gatt, descriptor, status)
                //mCurrentOperation.gattOperationCompletionCallback(descriptor.getUuid(), descriptor.getValue());
                if (gattDebugEnabled) {
                    aapsLogger.warn(LTag.PUMPBTCOMM, "onDescriptorRead " + getGattStatusMessage(status) + " status " + descriptor)
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                if (gattDebugEnabled) {
                    aapsLogger.warn(LTag.PUMPBTCOMM, "onMtuChanged $mtu status $status")
                }
            }

            override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
                super.onReadRemoteRssi(gatt, rssi, status)
                if (gattDebugEnabled) {
                    aapsLogger.warn(LTag.PUMPBTCOMM, "onReadRemoteRssi " + getGattStatusMessage(status) + ": " + rssi)
                }
            }

            override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
                super.onReliableWriteCompleted(gatt, status)
                if (gattDebugEnabled) {
                    aapsLogger.warn(LTag.PUMPBTCOMM, "onReliableWriteCompleted status $status")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val services = gatt.services
                    var ypsoPumpFound = false
                    for (service in services) {
                        //val uuidService = service.uuid
                        if (gattDebugEnabled) {
                            debugService(service, 0)
                        }
                    }
                    for (service in services) {
                        if (isYpsoPumpDevice(service.uuid)) {
                            ypsoPumpFound = true
                        }
                        val characteristics = service.characteristics
                        for (characteristic in characteristics) {
                            characteristicMap[characteristic.uuid.toString()] = characteristic
                        }
                    }

                    aapsLogger.debug(LTag.PUMPBTCOMM, "Found " + characteristicMap.size + " characteristics.")

                    if (gattDebugEnabled) {
                        aapsLogger.warn(LTag.PUMPBTCOMM, "onServicesDiscovered " + getGattStatusMessage(status))
                    }

                    aapsLogger.info(LTag.PUMPBTCOMM, "Gatt device is YpsoPump device: $ypsoPumpFound")

                    if (ypsoPumpFound) {
                        ypsoPumpUtil.driverStatus = PumpDriverState.Connected
                        //pumpStatus.connectionStatus = YpsoConnectionStatus.PUMP_CONNECTED
                        Thread(Runnable { encryptCommunication() }).start()
                    } else {
                        ypsoPumpUtil.errorType = YpsoPumpErrorType.DeviceIsNotPump
                        aapsLogger.error(LTag.PUMPBTCOMM, "Device found, but it doesn't seem like Ypso Pump device.")
//                        mIsConnected = false;
//                        rileyLinkServiceData.setServiceState(RileyLinkServiceState.RileyLinkError,
//                                RileyLinkError.DeviceIsNotRileyLink);
                    }
                } else {
                    aapsLogger.debug(LTag.PUMPBTCOMM, "onServicesDiscovered " + getGattStatusMessage(status))
                    //rileyLinkUtil.sendBroadcastMessage(RileyLinkConst.Intents.RileyLinkGattFailed, context);
                }
            }
        }
    }

    fun startConnectToYpsoPump(btAddress: String) {
        ypsoPumpUtil.driverStatus = PumpDriverState.Connecting
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // device doesn't support bluetooth
            ypsoPumpUtil.errorType = YpsoPumpErrorType.NoBluetoothAdapter
            return;
        }

        // if (bluetoothAdapter!!.isEnabled) {
        //     ypsoPumpUtil.errorType = YpsoPumpErrorType.BluetoothDisabled
        //     return;
        // }

        aapsLogger.debug(LTag.PUMPBTCOMM, "State: " + bluetoothAdapter!!.state + ", enabled: " + bluetoothAdapter!!.isEnabled)

        aapsLogger.debug(LTag.PUMPBTCOMM, "Ypso Pump address: $btAddress")
        // Must verify that this is a valid MAC, or crash.
        ypsopumpDevice = bluetoothAdapter!!.getRemoteDevice(btAddress)
        // if this succeeds, we get a connection state change callback?

        aapsLogger.debug(LTag.PUMPBTCOMM, "State: " + bluetoothAdapter!!.state + ", enabled: " + bluetoothAdapter!!.isEnabled)

        if (ypsopumpDevice != null) {
            connectGatt()
        } else {
            ypsoPumpUtil.errorType = YpsoPumpErrorType.ConfiguredPumpNotFound
            aapsLogger.error(LTag.PUMPBTCOMM, "YpsoPump device not found with address: $btAddress")
        }
    }

    fun connectGatt() {
        if (ypsopumpDevice == null) {
            aapsLogger.error(LTag.PUMPBTCOMM, "YpsoPump device is null, can't do connectGatt.")
            return
        }
        bluetoothConnectionGatt = ypsopumpDevice!!.connectGatt(context, true, bluetoothGattCallback)
        // , BluetoothDevice.TRANSPORT_LE
        if (bluetoothConnectionGatt == null) {
            ypsoPumpUtil.errorType = YpsoPumpErrorType.FailedToConnectToBleDevice
            aapsLogger.error(LTag.PUMPBTCOMM, "Failed to connect to Bluetooth Low Energy device at " + bluetoothAdapter!!.address)
        } else {
            if (gattDebugEnabled) {
                aapsLogger.debug(LTag.PUMPBTCOMM, "Gatt Connected.")
            }
            val deviceName = bluetoothConnectionGatt!!.device.name
            aapsLogger.debug(LTag.PUMPBTCOMM, "Device connected: $deviceName")
            if (StringUtils.isNotEmpty(deviceName)) {
                // Update stored name upon connecting (also for backwards compatibility for device where a name was not yet stored)
                //sp.putString(RileyLinkConst.Prefs.RileyLinkName, deviceName);
            } else {
                //sp.remove(RileyLinkConst.Prefs.RileyLinkName);
            }

//            rileyLinkServiceData.rileyLinkName = deviceName;
//            rileyLinkServiceData.rileyLinkAddress = bluetoothConnectionGatt.getDevice().getAddress();
        }
    }

    fun debugService(service: BluetoothGattService, indentCount: Int) {
        val indentString = StringUtils.repeat(' ', indentCount)
        val uuidService = service.uuid
        if (gattDebugEnabled) {
            val uuidServiceString = uuidService.toString()
            val stringBuilder = StringBuilder()
            stringBuilder.append(indentString)
            stringBuilder.append(lookup(uuidServiceString))
            stringBuilder.append(" ($uuidServiceString)")

            for (character in service.characteristics) {
                val uuidCharacteristicString = character.uuid.toString()
                stringBuilder.append("\n    ")
                stringBuilder.append(indentString)
                stringBuilder.append(" - " + YpsoGattCharacteristic.lookup(uuidCharacteristicString).description)
                stringBuilder.append(" ($uuidCharacteristicString)")
            }

            stringBuilder.append("\n\n")
            aapsLogger.warn(LTag.PUMPBTCOMM, stringBuilder.toString())
            val includedServices = service.includedServices
            for (serviceI in includedServices) {
                debugService(serviceI, indentCount + 4)
            }
        }
    }

    fun discoverServices(): Boolean {
        if (bluetoothConnectionGatt == null) {
            // shouldn't happen, but if it does we exit
            return false
        }
        return if (bluetoothConnectionGatt!!.discoverServices()) {
            aapsLogger.warn(LTag.PUMPBTCOMM, "Starting to discover GATT Services.")
            true
        } else {
            aapsLogger.error(LTag.PUMPBTCOMM, "Cannot discover GATT Services.")
            false
        }
    }

    fun disconnectFromYpsoPump() {
        ypsoPumpUtil.driverStatus = PumpDriverState.Disconnecting
        disconnect()
    }

    fun disconnect() {
        //pumpStatus.connectionStatus = YpsoConnectionStatus.DISCONNECTING
        //mIsConnected = false;
        aapsLogger.warn(LTag.PUMPBTCOMM, "Closing GATT connection")
        // Close old conenction
        if (bluetoothConnectionGatt != null) {
            // Not sure if to disconnect or to close first..
            bluetoothConnectionGatt!!.disconnect()
            manualDisconnect = true
        }
    }

    private fun getGattStatusMessage(status: Int): String {
        val statusMessage: String
        statusMessage = if (status == BluetoothGatt.GATT_SUCCESS) {
            "SUCCESS"
        } else if (status == BluetoothGatt.GATT_FAILURE) {
            "FAILED"
        } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
            "NOT PERMITTED"
        } else if (status == 133) {
            "Found the strange 133 bug"
        } else {
            "UNKNOWN ($status)"
        }
        return statusMessage
    }

    fun close() {
        if (bluetoothConnectionGatt != null) {
            bluetoothConnectionGatt!!.close()
            bluetoothConnectionGatt = null
        }
    }

    private fun encryptCommunication(): Boolean {
        ypsoPumpUtil.driverStatus = PumpDriverState.EncryptCommunication
        //pumpStatus.connectionStatus = YpsoConnectionStatus.PUMP_ENCRYPTION
        val pwd = ypsoPumpUtil.computeUserLevelPassword(deviceMac)
        val bleCommOperationResult = writeCharacteristicBlocking(
            YpsoGattCharacteristic.AUTHORIZATION_PASSWORD,
            pwd)
        if (bleCommOperationResult.isSuccessful) {
            ypsoPumpUtil.driverStatus = PumpDriverState.Ready
            //pumpStatus.connectionStatus = YpsoConnectionStatus.PUMP_READY
        } else {
            ypsoPumpUtil.errorType = YpsoPumpErrorType.EncryptionFailed
            //pumpStatus.connectionStatus = YpsoConnectionStatus.PUMP_ENCRYPTION_ERROR
        }
        // TODO remove
        aapsLogger.debug("Connection Status: " + ypsoPumpUtil.driverStatus.name)

        return bleCommOperationResult.isSuccessful
    }

    fun writeCharacteristicBlocking(gattCharacteristic: YpsoGattCharacteristic, value: ByteArray?): BLECommOperationResult {
        val rval = BLECommOperationResult()
        if (bluetoothConnectionGatt != null) {
            rval.value = value
            try {
                gattOperationSema.acquire()
                SystemClock.sleep(1) // attempting to yield thread, to make sequence of events easier to follow
            } catch (e: InterruptedException) {
                aapsLogger.error(LTag.PUMPBTCOMM, "writeCharacteristic_blocking: interrupted waiting for gattOperationSema")
                return rval
            }
            if (mCurrentOperation != null) {
                rval.operationResultType = BLECommOperationResultType.RESULT_BUSY
            } else {
                val chara = characteristicMap[gattCharacteristic.uuid]
                aapsLogger.debug(LTag.PUMPBTCOMM, "Characteristic: " + chara)
                if (chara == null) {
                    // Catch if the service is not supported by the BLE device
                    // GGW: Tue Jul 12 01:14:01 UTC 2016: This can also happen if the
                    // app that created the bluetoothConnectionGatt has been destroyed/created,
                    // e.g. when the user switches from portrait to landscape.
                    rval.operationResultType = BLECommOperationResultType.RESULT_NONE
                    aapsLogger.error(LTag.PUMPBTCOMM, "BT Device not supported")
                    // TODO: 11/07/2016 UI update for user
                    // xyz rileyLinkServiceData.setServiceState(RileyLinkServiceState.BluetoothError, RileyLinkError.NoBluetoothAdapter);
                } else {
                    mCurrentOperation = CharacteristicWriteOperation(aapsLogger, bluetoothConnectionGatt!!, chara, value)
                    mCurrentOperation!!.execute(this)
                    if (mCurrentOperation!!.timedOut) {
                        rval.operationResultType = BLECommOperationResultType.RESULT_TIMEOUT
                    } else if (mCurrentOperation!!.interrupted) {
                        rval.operationResultType = BLECommOperationResultType.RESULT_INTERRUPTED
                    } else {
                        rval.operationResultType = BLECommOperationResultType.RESULT_SUCCESS
                    }
                }
                mCurrentOperation = null
                gattOperationSema.release()
            }
        } else {
            aapsLogger.error(LTag.PUMPBTCOMM, "writeCharacteristic_blocking: not configured!")
            rval.operationResultType = BLECommOperationResultType.RESULT_NOT_CONFIGURED
        }
        return rval
    }

    fun readCharacteristicBlocking(gattCharacteristic: YpsoGattCharacteristic): BLECommOperationResult {
        val rval = BLECommOperationResult()
        if (bluetoothConnectionGatt != null) {
            try {
                gattOperationSema.acquire()
                SystemClock.sleep(1) // attempting to yield thread, to make sequence of events easier to follow
            } catch (e: InterruptedException) {
                aapsLogger.error(LTag.PUMPBTCOMM, "readCharacteristic_blocking: Interrupted waiting for gattOperationSema")
                return rval
            }
            if (mCurrentOperation != null) {
                rval.operationResultType = BLECommOperationResultType.RESULT_BUSY
            } else {
                val chara = characteristicMap[gattCharacteristic.uuid]
                if (chara == null) {
                    // Catch if the service is not supported by the BLE device
                    rval.operationResultType = BLECommOperationResultType.RESULT_NONE
                    aapsLogger.error(LTag.PUMPBTCOMM, "BT Device not supported")
                    // TODO: 11/07/2016 UI update for user
                    // xyz rileyLinkServiceData.setServiceState(RileyLinkServiceState.BluetoothError, RileyLinkError.NoBluetoothAdapter);
                } else {
                    mCurrentOperation = CharacteristicReadOperation(aapsLogger, bluetoothConnectionGatt!!, chara)
                    mCurrentOperation!!.execute(this)
                    if (mCurrentOperation!!.timedOut) {
                        rval.operationResultType = BLECommOperationResultType.RESULT_TIMEOUT
                    } else if (mCurrentOperation!!.interrupted) {
                        rval.operationResultType = BLECommOperationResultType.RESULT_INTERRUPTED
                    } else {
                        rval.operationResultType = BLECommOperationResultType.RESULT_SUCCESS
                        rval.value = mCurrentOperation!!.value
                    }
                }
            }
            mCurrentOperation = null
            gattOperationSema.release()
        } else {
            aapsLogger.error(LTag.PUMPBTCOMM, "readCharacteristic_blocking: not configured!")
            rval.operationResultType = BLECommOperationResultType.RESULT_NOT_CONFIGURED
        }
        return rval
    }

}