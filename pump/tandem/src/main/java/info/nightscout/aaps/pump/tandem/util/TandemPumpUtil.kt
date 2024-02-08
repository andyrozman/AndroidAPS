package info.nightscout.aaps.pump.tandem.util

import android.content.Context
import android.util.Log
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.utils.pump.ByteUtil
import com.jwoglom.pumpx2.pump.messages.helpers.Dates

import info.nightscout.aaps.pump.tandem.driver.TandemPumpStatus
import info.nightscout.pump.common.utils.PumpUtil
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TandemPumpUtil @Inject constructor(
    aapsLogger: AAPSLogger,
    rxBus: RxBus,
    context: Context,
    resourceHelper: ResourceHelper,
    val tandemPumpStatus: TandemPumpStatus
): PumpUtil(aapsLogger, rxBus, context, resourceHelper) {



    fun getTimeFromPumpAsEpochMillis(pumpTime: Long): Long {
        return Dates.fromJan12008EpochSecondsToDate(pumpTime).toEpochMilli();
    }


    //var preventConnect: Boolean = false

    //private var driverStatusInternal: PumpDriverState
    //private var pumpCommandType: PumpCommandType? = null
    //var gson = GsonBuilder().setPrettyPrinting().create()
    //var gsonRegular = GsonBuilder().create()



    // var driverStatus: PumpDriverState
    //     get() = workWithStatusAndCommand(StatusChange.GetStatus, null, null) as PumpDriverState?

    // val currentCommand: YpsoPumpCommandType?
    //     get() = workWithStatusAndCommand(StatusChange.GetCommand, null, null) as YpsoPumpCommandType?

    // fun resetDriverStatusToConnected() {
    //     workWithStatusAndCommand(StatusChange.SetStatus, PumpDriverState.Ready, null)
    // }
    //
    // var driverStatus: PumpDriverState
    //     get() {
    //         var stat = workWithStatusAndCommand(StatusChange.GetStatus, null, null) as PumpDriverState
    //         aapsLogger.debug(LTag.PUMP, "Get driver status: " + stat.name)
    //         return stat
    //     }
    //     set(status) {
    //         aapsLogger.debug(LTag.PUMP, "Set driver status: " + status.name)
    //         workWithStatusAndCommand(StatusChange.SetStatus, status, null)
    //     }

    // var currentCommand: PumpCommandType?
    //     get() {
    //         val returnValue = workWithStatusAndCommand(StatusChange.GetCommand, null, null)
    //         if (returnValue == null)
    //             return null
    //         else
    //             return returnValue as PumpCommandType
    //     }
    //     set(currentCommand) {
    //         if (currentCommand == null) {
    //             aapsLogger.debug(LTag.PUMP, "Set current command: to null")
    //         } else {
    //             aapsLogger.debug(LTag.PUMP, "Set current command: " + currentCommand.name)
    //         }
    //         workWithStatusAndCommand(StatusChange.SetCommand, PumpDriverState.ExecutingCommand, currentCommand)
    //     }

    // var errorType: YpsoPumpErrorType?
    //     get() {
    //         return workWithStatusAndCommand(StatusChange.GetError, null, null) as YpsoPumpErrorType?
    //     }
    //     set(error) {
    //         workWithStatusAndCommand(StatusChange.SetError, null, null, error)
    //     }

    // fun getDriverStatus(): PumpDriverState {
    //     return workWithStatusAndCommand(StatusChange.GetStatus, null, null) as PumpDriverState
    // }
    //
    // fun setDriverStatus(status: PumpDriverState) {
    //     aapsLogger.debug(LTag.PUMP, "Set driver status: " + status.name)
    //     workWithStatusAndCommand(StatusChange.SetStatus, status, null)
    // }



    // fun isSame(d1: Double, d2: Double): Boolean {
    //     val diff = d1 - d2
    //     return Math.abs(diff) <= 0.000001
    // }
    //
    // fun isSame(d1: Double, d2: Int): Boolean {
    //     val diff = d1 - d2
    //     return Math.abs(diff) <= 0.000001
    // }

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

    // fun getBytesFromInt16(value: Int): ByteArray {
    //     val array = getBytesFromInt(value)
    //     Log.d("HHH", ByteUtil.getHex(array))
    //     return byteArrayOf(array[3], array[2]) // 2, 3
    // }

    fun getBytesFromInt(value: Int): ByteArray {
        return ByteBuffer.allocate(4).putInt(value).array()
    }

    fun byteToInt(data: ByteArray, start: Int, length: Int): Int {
        return if (length == 1) {
            ByteUtil.toInt(data[start], null, null, null, ByteUtil.BitConversion.LITTLE_ENDIAN)
        } else if (length == 2) {
            ByteUtil.toInt(data[start], data[start + 1], null, null, ByteUtil.BitConversion.LITTLE_ENDIAN)
        } else if (length == 3) {
            ByteUtil.toInt(data[start], data[start + 1], data[start + 2], null, ByteUtil.BitConversion.LITTLE_ENDIAN)
        } else if (length == 4) {
            ByteUtil.toInt(data[start], data[start + 1], data[start + 2], data[start + 3], ByteUtil.BitConversion.LITTLE_ENDIAN)
        } else {
            //aapsLogger.error(LTag.PUMPBTCOMM, "byteToInt, length $length not supported.");
            0
        }
    }



    fun getBytesFromIntArray2(value: Int): ByteArray {
        val array = ByteBuffer.allocate(4).putInt(value).array()
        return byteArrayOf(array[3], array[2])
    }


    companion object {

        const val MAX_RETRY = 2


    }
}
