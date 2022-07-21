package info.nightscout.androidaps.plugins.pump.common.defs

import info.nightscout.androidaps.plugins.general.overview.notifications.Notification
import info.nightscout.androidaps.plugins.pump.common.R

/**
 * Created by andy on 10/15/18.
 */
enum class PumpBaseNotificationType(override var notificationType: Int = -1,
                                    override var resourceId: Int,
                                    override var notificationUrgency: Int) : PumpNotificationType {

    PumpUnreachable(Notification.RILEYLINK_CONNECTION, R.string.medtronic_pump_status_pump_unreachable, Notification.NORMAL),  //
    PumpWrongTimeUrgent(-1, R.string.medtronic_notification_check_time_date, Notification.URGENT),
    PumpWrongTimeNormal(-1, R.string.medtronic_notification_check_time_date, Notification.NORMAL),
    TimeChangeOver24h(Notification.OVER_24H_TIME_CHANGE_REQUESTED, R.string.medtronic_error_pump_24h_time_change_requested, Notification.URGENT);

    //constructor(resourceId: Int, notificationUrgency: Int) : this(Notification.MEDTRONIC_PUMP_ALARM, resourceId, notificationUrgency)
}