package app.aaps.pump.tandem.common.keys

import app.aaps.core.keys.interfaces.LongNonPreferenceKey

enum class TandemLongNonPreferenceKey(
    override val key: String,
    override val defaultValue: Long,
    override val exportable: Boolean = true
) : LongNonPreferenceKey {

    TbrsSet(key = "tandem_tbrs_set", defaultValue = 0),
    SmbBoluses(key = "tandem_smb_boluses_delivered", defaultValue = 0),
    StandardBoluses(key = "tandem_std_boluses_delivered", defaultValue = 0),
    FirstPumpUse(key = "tandem_first_pump_use", defaultValue = 0),
    LastGoodPumpCommunicationTime(key = "tandem_lastGoodPumpCommunicationTime", defaultValue = 0L),

    LastTbrId("tandem_last_tbr_id", defaultValue = 0L),
    SiteReminderDateTime(key = "tandem_site_reminder_datetime", defaultValue = 0L)

}
