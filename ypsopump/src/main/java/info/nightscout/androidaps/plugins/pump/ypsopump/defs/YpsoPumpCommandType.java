package info.nightscout.androidaps.plugins.pump.ypsopump.defs;

import androidx.annotation.StringRes;

import info.nightscout.androidaps.plugins.pump.ypsopump.R;

public enum YpsoPumpCommandType {

    SetBolus(R.string.ypsopump_cmd_desc_set_bolus),
    GetFirmwareVersion(R.string.ypsopump_cmd_desc_get_firmware),
    SetTemporaryBasalRate(R.string.ypsopump_cmd_desc_set_tbr),
    GetTemporaryBasalRate(R.string.ypsopump_cmd_desc_get_tbr),
    GetSettings(R.string.ypsopump_cmd_desc_get_settings),
    GetBasalProfile(R.string.ypsopump_cmd_desc_get_basal_profile),
    SetBasalProfile(R.string.ypsopump_cmd_desc_set_basal_profile)
    ;



    private Integer resourceId;

    YpsoPumpCommandType(@StringRes Integer resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getResourceId() {
        return resourceId;
    }

}
