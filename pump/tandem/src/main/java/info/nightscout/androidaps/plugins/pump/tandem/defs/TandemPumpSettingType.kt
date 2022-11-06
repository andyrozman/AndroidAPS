package info.nightscout.androidaps.plugins.pump.tandem.defs

import info.nightscout.androidaps.plugins.pump.common.defs.PumpConfigurationTypeInterface

enum class TandemPumpSettingType : PumpConfigurationTypeInterface {
    CONTROL_IQ_ENABLED,
    BASAL_LIMIT,
    MAX_BOLUS;

    override fun getKey() = name

}