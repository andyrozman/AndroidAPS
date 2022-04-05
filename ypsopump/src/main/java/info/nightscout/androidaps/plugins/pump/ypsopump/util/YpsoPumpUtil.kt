package info.nightscout.androidaps.plugins.pump.ypsopump.util

import android.util.Log
import com.google.gson.GsonBuilder
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoPumpNotificationType
import info.nightscout.androidaps.plugins.pump.ypsopump.data.DateTimeDto
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpErrorType
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpStatusChanged
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import java.nio.ByteBuffer
import java.security.InvalidParameterException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.experimental.and
import kotlin.experimental.inv

@Singleton
class YpsoPumpUtil @Inject constructor(
    val aapsLogger: AAPSLogger,
    val rxBus: RxBus,
    val resourceHelper: ResourceHelper,
    val ypsopumpPumpStatus: YpsopumpPumpStatus
) {

    var preventConnect: Boolean = false

    //private var driverStatusInternal: PumpDriverState
    private var pumpCommandType: YpsoPumpCommandType? = null
    var gson = GsonBuilder().setPrettyPrinting().create()



    // var driverStatus: PumpDriverState
    //     get() = workWithStatusAndCommand(StatusChange.GetStatus, null, null) as PumpDriverState?

    // val currentCommand: YpsoPumpCommandType?
    //     get() = workWithStatusAndCommand(StatusChange.GetCommand, null, null) as YpsoPumpCommandType?

    fun resetDriverStatusToConnected() {
        workWithStatusAndCommand(StatusChange.SetStatus, PumpDriverState.Ready, null)
    }

    var driverStatus: PumpDriverState
        get() {
            var stat = workWithStatusAndCommand(StatusChange.GetStatus, null, null) as PumpDriverState
            aapsLogger.debug(LTag.PUMP, "Get driver status: " + stat.name)
            return stat
        }
        set(status) {
            aapsLogger.debug(LTag.PUMP, "Set driver status: " + status.name)
            workWithStatusAndCommand(StatusChange.SetStatus, status, null)
        }

    var currentCommand: YpsoPumpCommandType
        get() = workWithStatusAndCommand(StatusChange.GetCommand, null, null) as YpsoPumpCommandType
        set(currentCommand) {
            aapsLogger.debug(LTag.PUMP, "Set current command: " + currentCommand.name)
            workWithStatusAndCommand(StatusChange.SetCommand, PumpDriverState.ExecutingCommand, currentCommand)
        }

    var errorType: YpsoPumpErrorType?
        get() {
            return workWithStatusAndCommand(StatusChange.GetError, null, null) as YpsoPumpErrorType?
        }
        set(error) {
            workWithStatusAndCommand(StatusChange.SetError, null, null, error)
        }

    // fun getDriverStatus(): PumpDriverState {
    //     return workWithStatusAndCommand(StatusChange.GetStatus, null, null) as PumpDriverState
    // }
    //
    // fun setDriverStatus(status: PumpDriverState) {
    //     aapsLogger.debug(LTag.PUMP, "Set driver status: " + status.name)
    //     workWithStatusAndCommand(StatusChange.SetStatus, status, null)
    // }

    // fun getCurrentCommand(): YpsoPumpCommandType {
    //     return workWithStatusAndCommand(StatusChange.GetCommand, null, null) as YpsoPumpCommandType
    // }

    // fun setCurrentCommand(currentCommand: YpsoPumpCommandType) {
    //     aapsLogger.debug(LTag.PUMP, "Set current command: " + currentCommand.name)
    //     workWithStatusAndCommand(StatusChange.SetCommand, PumpDriverState.ExecutingCommand, currentCommand)
    // }

    @Synchronized
    fun workWithStatusAndCommand(type: StatusChange,
                                 driverStatusIn: PumpDriverState?,
                                 pumpCommandType: YpsoPumpCommandType?,
                                 ypsoPumpErrorType: YpsoPumpErrorType? = null): Any? {

        //aapsLogger.debug(LTag.PUMP, "Status change type: " + type.name() + ", DriverStatus: " + (driverStatus != null ? driverStatus.name() : ""));
        when (type) {
            StatusChange.GetStatus  ->  {
                aapsLogger.debug(LTag.PUMP, "GetStatus: DriverStatus: " + driverStatusInternal);
                return driverStatusInternal
            }
            StatusChange.SetStatus  -> {
                aapsLogger.debug(LTag.PUMP, "SetStatus: DriverStatus Before: " + driverStatusInternal + ", Incoming: " + driverStatusIn);
                driverStatusInternal = driverStatusIn!!
                this.pumpCommandType = null
                aapsLogger.debug(LTag.PUMP, "SetStatus: DriverStatus: " + driverStatusInternal);
                rxBus.send(EventPumpStatusChanged(driverStatusInternal))
            }
            StatusChange.GetCommand -> return this.pumpCommandType
            StatusChange.SetCommand -> {
                driverStatusInternal = driverStatusIn!!
                this.pumpCommandType = pumpCommandType
                rxBus.send(EventPumpStatusChanged(driverStatusInternal))
            }
            StatusChange.GetError   -> return errorTypeInternal
            StatusChange.SetError   -> {
                errorTypeInternal = ypsoPumpErrorType!!
                this.pumpCommandType = null
                driverStatusInternal = PumpDriverState.ErrorCommunicatingWithPump
                rxBus.send(EventPumpStatusChanged(driverStatusInternal))
            }
        }
        return null
    }

    fun sleepSeconds(seconds: Long) {
        try {
            Thread.sleep(seconds * 1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun sleep(miliseconds: Long) {
        try {
            Thread.sleep(miliseconds)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun toDateTimeDto(atechDateTimeIn: Long): DateTimeDto {
        var atechDateTime = atechDateTimeIn
        val year = (atechDateTime / 10000000000L).toInt()
        atechDateTime -= year * 10000000000L
        val month = (atechDateTime / 100000000L).toInt()
        atechDateTime -= month * 100000000L
        val dayOfMonth = (atechDateTime / 1000000L).toInt()
        atechDateTime -= dayOfMonth * 1000000L
        val hourOfDay = (atechDateTime / 10000L).toInt()
        atechDateTime -= hourOfDay * 10000L
        val minute = (atechDateTime / 100L).toInt()
        atechDateTime -= minute * 100L
        val second = atechDateTime.toInt()
        return DateTimeDto(year, month, dayOfMonth, hourOfDay, minute, second)
    }

    enum class StatusChange {
        GetStatus, GetCommand, SetStatus, SetCommand, GetError, SetError
    }

    fun getSettingIdAsArray(settingId: Int): ByteArray {
        val array = ByteArray(8)
        CreateGLB_SAFE_VAR(settingId, array)
        return array
    }

    fun isSame(d1: Double, d2: Double): Boolean {
        val diff = d1 - d2
        return Math.abs(diff) <= 0.000001
    }

    fun isSame(d1: Double, d2: Int): Boolean {
        val diff = d1 - d2
        return Math.abs(diff) <= 0.000001
    }

    fun computeUserLevelPassword(btAddress: String): ByteArray {
        val sourceArray = byteArrayOf(91, 215.toByte(), 16, 102, 1, 242.toByte(), 79, 60, 143.toByte(), 107)
        val array = ByteArray(16)
        val address = btAddress.split(":").toTypedArray()
        Log.d("DD", "BTAddress: $address")
        array[0] = convertIntegerStringToByte(address[0])
        array[1] = convertIntegerStringToByte(address[1])
        array[2] = convertIntegerStringToByte(address[2])
        array[3] = convertIntegerStringToByte(address[3])
        array[4] = convertIntegerStringToByte(address[4])
        array[5] = convertIntegerStringToByte(address[5])
        System.arraycopy(sourceArray, 0, array, 6, 10)
        //        return MD5.Create().ComputeHash(array);

//        MessageDigest.getInstance(MD5)
//
//        DigestUtils.md5Hex(password).toUpperCase();
        var md: MessageDigest?
        return try {
            md = MessageDigest.getInstance("MD5")
            md.update(array)
            md.digest()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            array
        }

        //return array;
    }

    private fun convertIntegerStringToByte(hex: String): Byte {
        //Log.d("DD", "BTAddress Part: " + hex);
        return hex.toInt(16).toByte()
    }



    fun getBytesFromInt16(value: Int): ByteArray {
        val array = getBytesFromInt(value)
        Log.d("HHH", ByteUtil.getHex(array))
        return byteArrayOf(array[3], array[2]) // 2, 3
    }

    fun getBytesFromInt(value: Int): ByteArray {
        return ByteBuffer.allocate(4).putInt(value).array()
    }


    fun byteToInt(data: ByteArray, start: Int, length: Int): Int {
        return if (length == 1) {
            ByteUtil.toInt(data[start])
        } else if (length == 2) {
            ByteUtil.toInt(data[start], data[start + 1], ByteUtil.BitConversion.LITTLE_ENDIAN)
        } else if (length == 3) {
            ByteUtil.toInt(data[start], data[start + 1], data[start + 2], ByteUtil.BitConversion.LITTLE_ENDIAN)
        } else if (length == 4) {
            ByteUtil.toInt(data[start], data[start + 1], data[start + 2], data[start + 3], ByteUtil.BitConversion.LITTLE_ENDIAN)
        } else {
            //aapsLogger.error(LTag.PUMPBTCOMM, "byteToInt, length $length not supported.");
            0
        }
    }

    //public static byte[]
    // TOOO fix
    fun CreateGLB_SAFE_VAR(piValueIn: Int, pbyBuffer: ByteArray) {
        var piValue = piValueIn
        pbyBuffer[0] = piValue.toByte()
        piValue = piValue shr 8
        pbyBuffer[1] = piValue.toByte()
        piValue = piValue shr 8
        pbyBuffer[2] = piValue.toByte()
        piValue = piValue shr 8
        pbyBuffer[3] = piValue.toByte()
        pbyBuffer[4] = pbyBuffer[0].inv()
        pbyBuffer[5] = pbyBuffer[1].inv()
        pbyBuffer[6] = pbyBuffer[2].inv()
        pbyBuffer[7] = pbyBuffer[3].inv()
    }

    fun getValueFromeGLB_SAFE_VAR(paBuffer: ByteArray): Int {
        if (paBuffer[7].inv() and 0xFF.toByte() != paBuffer[3] and 0xFF.toByte() ||
            paBuffer[6].inv() and 0xFF.toByte() != paBuffer[2] and 0xFF.toByte() ||
            paBuffer[5].inv() and 0xFF.toByte() != paBuffer[1] and 0xFF.toByte() ||
            paBuffer[4].inv() and 0xFF.toByte() != paBuffer[0] and 0xFF.toByte()) {
            throw InvalidParameterException("Invalid GLB_SAFE_VAR byte array:$paBuffer")
        }
        return byteToInt(paBuffer, 0, 4)
    }

    fun getBytesFromIntArray2(value: Int): ByteArray {
        val array = ByteBuffer.allocate(4).putInt(value).array()
        return byteArrayOf(array[3], array[2])
    }

    fun sendNotification(notificationType: YpsoPumpNotificationType) {
        val notification = Notification( //
            notificationType.notificationType,  //
            resourceHelper.gs(notificationType.resourceId),  //
            notificationType.notificationUrgency)
        rxBus.send(EventNewNotification(notification))
    }

    fun sendNotification(notificationType: YpsoPumpNotificationType, vararg parameters: Any?) {
        val notification = Notification( //
            notificationType.notificationType,  //
            resourceHelper.gs(notificationType.resourceId, *parameters),  //
            notificationType.notificationUrgency)
        rxBus.send(EventNewNotification(notification))
    }

    companion object {

        const val MAX_RETRY = 2
        //private var driverStatus = PumpDriverState.Sleeping

        @JvmStatic private var driverStatusInternal: PumpDriverState = PumpDriverState.Sleeping
        @JvmStatic private var errorTypeInternal: YpsoPumpErrorType? = null

    }
}