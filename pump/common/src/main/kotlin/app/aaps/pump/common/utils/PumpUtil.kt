package app.aaps.pump.common.utils

import android.content.Context
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.notifications.NotificationId
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.keys.StringKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.utils.pump.ByteUtil
import app.aaps.pump.common.data.DateTimeDto
import app.aaps.pump.common.defs.NotificationTypeInterface
import app.aaps.pump.common.defs.PumpDriverState
import app.aaps.pump.common.defs.PumpErrorType
import app.aaps.pump.common.driver.connector.commands.data.CustomCommandTypeInterface
import app.aaps.pump.common.driver.connector.defs.PumpCommandType
import app.aaps.pump.common.events.EventPumpDriverStateChanged
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import java.lang.reflect.Type

open class PumpUtil constructor(
    val aapsLogger: AAPSLogger,
    val rxBus: RxBus,
    val context: Context,
    val resourceHelper: ResourceHelper,
    val notificationManager: NotificationManager,
    val preferences: Preferences
) {

    var preventConnect: Boolean = false

    //private var driverStatusInternal: PumpDriverState
    private var pumpCommandType: PumpCommandType? = null
    var gson = GsonBuilder()
        .registerTypeAdapter(DateTime::class.java,
                             JsonSerializer<DateTime?> { json, typeOfSrc, context -> JsonPrimitive(ISODateTimeFormat.dateTime().print(json)) })
        .registerTypeAdapter(
            ByteArray::class.java,
            ByteArrayToStringAdapter())
        .setPrettyPrinting().create()

    var gsonRegular = GsonBuilder().create()

    // var gson: Gson = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss")
    //     .registerTypeAdapter(
    //         ByteArray::class.java,
    //         JsonSerializer { src: ByteArray?, typeOfSrc: Type?, context: JsonSerializationContext? -> JsonPrimitive(String(src!!)) })
    //     .registerTypeAdapter(
    //         ByteArray::class.java,
    //         JsonDeserializer { json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext? -> if (json == null) null else if (json.asString == null) null else json.asString.toByteArray() })
    //     .create()
    //
    class ByteArrayToStringAdapter : JsonSerializer<ByteArray?>, JsonDeserializer<ByteArray?> {
        @Throws(JsonParseException::class) override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): ByteArray {
            return ByteUtil.createByteArrayFromString(json.asString)
        }

        override fun serialize(src: ByteArray?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(ByteUtil.getCompactString(src))
        }
    }

    open fun resetDriverStatusToConnected() {
        workWithStatusAndCommand(StatusChange.SetStatus, PumpDriverState.Ready, null)
    }

    var previousState = PumpDriverState.NotInitialized

    var driverStatus: PumpDriverState
        get() {
            val stat = workWithStatusAndCommand(StatusChange.GetStatus, null, null) as PumpDriverState

            if (previousState!=stat) {
                aapsLogger.debug(LTag.PUMP, "Get driver status: " + stat.name)
                previousState = stat
            }

            return stat
        }
        set(status) {
            aapsLogger.debug(LTag.PUMP, "Set driver status: " + status.name)
            workWithStatusAndCommand(StatusChange.SetStatus, status, null)
        }

    var currentCommand: PumpCommandType?
        get() {
            val returnValue = workWithStatusAndCommand(StatusChange.GetCommand, null, null)
            if (returnValue == null)
                return null
            else
                return returnValue as PumpCommandType
        }
        set(currentCommand) {
            if (currentCommand == null) {
                aapsLogger.debug(LTag.PUMP, "Set current command: to null")
            } else {
                aapsLogger.debug(LTag.PUMP, "Set current command: " + currentCommand.name)
            }
            workWithStatusAndCommand(StatusChange.SetCommand, PumpDriverState.ExecutingCommand, currentCommand)
        }

    var errorType: PumpErrorType?
        get() {
            return workWithStatusAndCommand(StatusChange.GetError, null, null) as PumpErrorType?
        }
        set(error) {
            workWithStatusAndCommand(StatusChange.SetError, null, null, error)
        }

    var customCommandType: CustomCommandTypeInterface? = null

    @Synchronized
    fun workWithStatusAndCommand(
        type: StatusChange,
        driverStatusIn: PumpDriverState?,
        pumpCommandType: PumpCommandType?,
        pumpErrorType: PumpErrorType? = null
    ): Any? {

        //aapsLogger.debug(LTag.PUMP, "Status change type: " + type.name() + ", DriverStatus: " + (driverStatus != null ? driverStatus.name() : ""));
        when (type) {
            StatusChange.GetStatus  -> {
                //aapsLogger.debug(LTag.PUMP, "GetStatus: DriverStatus: " + driverStatusInternal);
                return driverStatusInternal
            }

            StatusChange.SetStatus  -> {
                //aapsLogger.debug(LTag.PUMP, "SetStatus: DriverStatus Before: " + driverStatusInternal + ", Incoming: " + driverStatusIn);
                driverStatusInternal = driverStatusIn!!
                this.pumpCommandType = null
                //aapsLogger.debug(LTag.PUMP, "SetStatus: DriverStatus: " + driverStatusInternal);
                rxBus.send(EventPumpDriverStateChanged(driverStatusInternal))
            }

            StatusChange.GetCommand -> return this.pumpCommandType

            StatusChange.SetCommand -> {
                driverStatusInternal = driverStatusIn!!
                this.pumpCommandType = pumpCommandType
                rxBus.send(EventPumpDriverStateChanged(driverStatusInternal))
            }

            StatusChange.GetError   -> return errorTypeInternal

            StatusChange.SetError   -> {
                errorTypeInternal = pumpErrorType!!
                this.pumpCommandType = null
                driverStatusInternal = PumpDriverState.ErrorCommunicatingWithPump
                rxBus.send(EventPumpDriverStateChanged(driverStatusInternal))
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

    fun fromDpToSize(dpSize: Int): Int {
        val scale = context.resources.displayMetrics.density
        val pixelsFl = ((dpSize * scale) + 0.5f)
        return pixelsFl.toInt()
    }

    enum class StatusChange {
        GetStatus, GetCommand, SetStatus, SetCommand, GetError, SetError
    }



    fun isSame(d1: Double, d2: Double): Boolean {
        val diff = d1 - d2
        return Math.abs(diff) <= 0.000001
    }

    fun isSame(d1: Double, d2: Int): Boolean {
        val diff = d1 - d2
        return Math.abs(diff) <= 0.000001
    }

    fun sendNotification(notificationType: NotificationTypeInterface) {
        // val notification = Notification( //
        //     notificationType.notificationType,  //
        //     resourceHelper.gs(notificationType.resourceId),  //
        //     notificationType.notificationUrgency
        // )

        aapsLogger.error(LTag.PUMP, "NotificationType: $notificationType")

        // rxBus.send(EventNewNotification(notification))
        if (notificationType.validMinutes==null || notificationType.validMinutes == -1) {
            notificationManager.post(
                id = notificationType.notificationType,
                textRes = notificationType.resourceId,
                level = notificationType.notificationUrgency
            )
        } else {
            notificationManager.post(
                id = notificationType.notificationType,
                textRes = notificationType.resourceId,
                level = notificationType.notificationUrgency,
                validMinutes = notificationType.validMinutes!!
            )
        }

        //notificationManager.post(NotificationId.INSIGHT_DATE_TIME_UPDATED, app.aaps.core.ui.R.string.pump_time_updated, validMinutes = 60)

    }

    fun sendNotification(notificationType: NotificationTypeInterface, vararg parameters: Any?) {
        if (notificationType.validMinutes==null || notificationType.validMinutes == -1) {
            notificationManager.post(
                id = notificationType.notificationType,
                textRes = notificationType.resourceId,
                formatArgs = parameters,
                level = notificationType.notificationUrgency
            )
        } else {
            notificationManager.post(
                id = notificationType.notificationType,
                textRes = notificationType.resourceId,
                formatArgs = parameters,
                level = notificationType.notificationUrgency,
                validMinutes = notificationType.validMinutes!!
            )
        }
    }


    fun isAAPSDarkTheme(isSystemDarkTheme: Boolean): Boolean {
        val colorscheme = preferences.get(StringKey.GeneralDarkMode)

        if (colorscheme.equals("dark")) {
            return true
        } else if (colorscheme.equals("light")) {
            return false
        } else {
            return isSystemDarkTheme
        }
    }


    companion object {

        const val MAX_RETRY = 2
        //private var driverStatus = PumpDriverState.Sleeping

        @JvmStatic var driverStatusInternal: PumpDriverState = PumpDriverState.Sleeping
        @JvmStatic private var errorTypeInternal: PumpErrorType? = null

        // var gson: Gson = GsonBuilder()
        //     .registerTypeAdapter(DateTime::class.java,
        //                          JsonSerializer<DateTime?> { json, typeOfSrc, context -> JsonPrimitive(ISODateTimeFormat.dateTime().print(json)) })
        //     .create()

    }
}
