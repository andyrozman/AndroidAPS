package app.aaps.pump.tandem.common.driver.connector.def

import androidx.annotation.StringRes
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.pump.common.driver.connector.commands.data.CustomCommandTypeInterface
import app.aaps.pump.tandem.R

enum class TandemCustomCommand(@StringRes var resourceId: Int) : CustomCommandTypeInterface {

    SET_MAX_BOLUS(R.string.tandem_custom_command_set_max_bolus),
    SET_MAX_BASAL(R.string.tandem_custom_command_set_max_basal),
    SET_CONTROL_IQ(R.string.tandem_custom_command_disable_control_iq),
    GET_PUMP_INFO(R.string.tandem_custom_command_get_pump_info),
    GET_ALERTS(R.string.tandem_custom_command_get_alerts),
    GET_ALARMS(R.string.tandem_custom_command_get_alarms),
    //GET_QUICK_BOLUS(R.string.tandem_custom_command_get_quick_bolus),
    DISMISS_ALERT(R.string.tandem_custom_command_dismiss_alert),
    SET_QUICK_BOLUS(R.string.tandem_custom_command_set_quick_bolus),
    GET_MALFUNCTIONS(R.string.tandem_custom_command_get_malfunctions),
    SET_PUMP_SOUNDS(R.string.tandem_custom_command_set_pump_sounds)
    ;

    var descriptionInternal : String? = null

    override fun getKey() = name
    override fun getDescription() = descriptionInternal!!

    companion object {

        @JvmStatic
        fun doTranslation(rh: ResourceHelper) {
            for (entry in entries) {
                entry.descriptionInternal = rh.gs(entry.resourceId)
            }
        }
    }

}