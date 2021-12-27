package info.nightscout.androidaps.plugins.pump.ypsopump.defs

import androidx.annotation.StringRes
import info.nightscout.androidaps.plugins.pump.ypsopump.R

enum class YpsoPumpCommandType(@param:StringRes val resourceId: Int) {
    SetBolus(R.string.ypsopump_cmd_desc_set_bolus),
    GetFirmwareVersion(R.string.ypsopump_cmd_desc_get_firmware),
    SetTemporaryBasal(R.string.ypsopump_cmd_desc_set_tbr),
    GetTemporaryBasal(R.string.ypsopump_cmd_desc_get_tbr),
    CancelTemporaryBasal(R.string.ypsopump_cmd_desc_cancel_tbr),
    GetSettings(R.string.ypsopump_cmd_desc_get_settings),
    GetBasalProfile(R.string.ypsopump_cmd_desc_get_basal_profile),
    SetBasalProfile(R.string.ypsopump_cmd_desc_set_basal_profile);

}