package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations.BLECommOperationResult
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import javax.inject.Inject

abstract class AbstractBLECommand<T>(hasAndroidInjector: HasAndroidInjector) : BLECommandInterface<T> {

    @Inject lateinit var aapsLogger: AAPSLogger
    @Inject lateinit var ypsoPumpUtil: YpsoPumpUtil
    @Inject lateinit var ypsoPumpDataConverter: YpsoPumpDataConverter
    @Inject lateinit var ypsopumpPumpStatus: YpsopumpPumpStatus

    override var commandResponse: T? = null
        get() {
            return field
        }
        set(value) {
            field = value
            aapsLogger.debug(LTag.PUMPBTCOMM, "setCommandResponse: " + ypsoPumpUtil.gson.toJson(value))
        }

    override var bleCommOperationResult: BLECommOperationResult? = null
        get() = field
        set(value) {
            field = value
        }

    abstract fun executeInternal(pumpBle: YpsoPumpBLE): Boolean

    override fun execute(pumpBle: YpsoPumpBLE): Boolean {

        try {
            setStartCommand()

            return executeInternal(pumpBle)

        } finally {
            setEndCommand()
        }

    }

    fun executeBLEReadCommandWithRetry(characteristic: YpsoGattCharacteristic,
                                       pumpBle: YpsoPumpBLE): BLECommOperationResult? {
        var bleCommOperationResult: BLECommOperationResult? = null
        for (retry in 0..YpsoPumpUtil.MAX_RETRY) {
            bleCommOperationResult = pumpBle.readCharacteristicBlocking(characteristic)
            if (bleCommOperationResult.isSuccessful) {
                return bleCommOperationResult
            }
        }
        return bleCommOperationResult
    }

    fun executeBLEWriteCommandWithRetry(characteristic: YpsoGattCharacteristic,
                                        value: ByteArray,
                                        pumpBle: YpsoPumpBLE): BLECommOperationResult? {
        var bleCommOperationResult: BLECommOperationResult? = null
        for (retry in 0..YpsoPumpUtil.MAX_RETRY) {
            bleCommOperationResult = pumpBle.writeCharacteristicBlocking(characteristic, value)
            if (bleCommOperationResult.isSuccessful) {
                return bleCommOperationResult
            }
        }
        return bleCommOperationResult
    }

    protected fun setStartCommand() {
        ypsoPumpUtil.driverStatus = PumpDriverState.ExecutingCommand
        //ypsopumpPumpStatus.connectionStatus = YpsoConnectionStatus.PUMP_COMMAND_RUNNING
    }

    protected fun setEndCommand() {
        ypsoPumpUtil.driverStatus = PumpDriverState.Ready
        //ypsopumpPumpStatus.connectionStatus = YpsoConnectionStatus.PUMP_READY
    }

    val isSuccessful: Boolean
        get() = bleCommOperationResult != null && bleCommOperationResult!!.isSuccessful

    fun getResultAsInt(data: ByteArray): Int {
        //return ByteUtil.toInt(data[3], data[2], data[1], data[0])
        return ByteUtil.toInt(data[0], data[1], data[2], data[3], ByteUtil.BitConversion.LITTLE_ENDIAN)
    }

    override fun toString(): String {
        val sb = StringBuilder(this.javaClass.simpleName + " [")
        sb.append("commandResponse=").append(ypsoPumpUtil.gson.toJson(commandResponse))
        sb.append(", bleCommOperationResult=" + ypsoPumpUtil.gson.toJson(bleCommOperationResult))
        sb.append("]")
        return sb.toString()
    }

    init {
        hasAndroidInjector.androidInjector().inject(this)
    }
}