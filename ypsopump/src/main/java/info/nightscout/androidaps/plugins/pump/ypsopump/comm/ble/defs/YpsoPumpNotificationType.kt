package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs

import androidx.annotation.StringRes
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification
import info.nightscout.androidaps.plugins.pump.ypsopump.R

/**
 * Created by andy on 01/06/2021.
 */
enum class YpsoPumpNotificationType(var notificationType: Int,
                                    @StringRes val resourceId: Int,
                                    val notificationUrgency: Int) {

    PumpIncorrectBasalProfileSelected(R.string.pump_settings_error_incorrect_basal_profile_selected, Notification.URGENT),  //
    PumpWrongMaxBolusSet(R.string.pump_settings_error_wrong_max_bolus_set, Notification.NORMAL),  //
    PumpWrongMaxBasalSet(R.string.pump_settings_error_wrong_max_basal_set, Notification.NORMAL),  //
    //PumpWrongTimeUrgent(R.string.medtronic_notification_check_time_date, Notification.URGENT),
    //PumpWrongTimeNormal(R.string.medtronic_notification_check_time_date, Notification.NORMAL),
    //TimeChangeOver24h(Notification.OVER_24H_TIME_CHANGE_REQUESTED, R.string.medtronic_error_pump_24h_time_change_requested, Notification.URGENT);
    ;

    constructor(resourceId: Int, notificationUrgency: Int) : this(Notification.YPSOPUMP_PUMP_ALARM, resourceId, notificationUrgency) {}
}