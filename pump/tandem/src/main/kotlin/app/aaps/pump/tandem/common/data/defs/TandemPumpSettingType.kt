package app.aaps.pump.tandem.common.data.defs

import app.aaps.pump.common.defs.PumpConfigurationTypeInterface

enum class TandemPumpSettingType : PumpConfigurationTypeInterface {
    CONTROL_IQ_ENABLED,
    BASAL_LIMIT,
    MAX_BOLUS,
    QUICK_BOLUS,
    BASAL_IQ_ENABLED,
    PUMP_FEATURES_1,
    PUMP_FEATURES_2
    ;

    override fun getKey() = name

}