package app.aaps.pump.tandem.common.data.defs

import androidx.annotation.StringRes
import app.aaps.core.interfaces.notifications.NotificationId
import app.aaps.core.interfaces.notifications.NotificationLevel
import app.aaps.pump.common.R as Rc
import app.aaps.pump.common.defs.NotificationTypeInterface
import app.aaps.pump.tandem.R

enum class TandemNotificationType(override var notificationType: NotificationId,
                                  @StringRes override val resourceId: Int,
                                  override val notificationUrgency: NotificationLevel,
                                  override val validMinutes: Int? = null) : NotificationTypeInterface {

    InvalidPairingCodeReconfigure(resourceId = R.string.tandem_notification_wrong_pairing_code,
                                  notificationUrgency = NotificationLevel.URGENT),

    SiteReminder(notificationType = NotificationId.TANDEM_SITE_REMINDER,
                 resourceId = R.string.tandem_notification_site_reminder,
                 notificationUrgency = NotificationLevel.NORMAL),

    DateTimeUpdated(notificationType = NotificationId.INSIGHT_DATE_TIME_UPDATED,
                    resourceId = Rc.string.pump_time_updated,
                    notificationUrgency = NotificationLevel.NORMAL,
                    validMinutes = 60),

    TimeDifferenceTooBig(notificationType = NotificationId.OMNIPOD_TIME_OUT_OF_SYNC,
                         resourceId = Rc.string.pump_time_difference_too_big,
                         notificationUrgency = NotificationLevel.NORMAL,
                         validMinutes = 60),

    TandemPumpSettingsUpdated(notificationType = NotificationId.TANDEM_PUMP_SETTINGS_UPDATED,
                              resourceId =  R.string.tandem_notification_pump_settings_changed,
                              notificationUrgency = NotificationLevel.NORMAL,
                              validMinutes = 60),

    TandemBasalProfileError(notificationType = NotificationId.TANDEM_BASAL_PROFILE_ERROR,
                            resourceId = R.string.tandem_error_profile_only_16_segments,
                            notificationUrgency = NotificationLevel.URGENT,
                            validMinutes = 24*60
    );

    constructor(resourceId: Int, notificationUrgency: NotificationLevel) :
        this(NotificationId.COMBO_PUMP_ALARM, resourceId, notificationUrgency, -1)
}