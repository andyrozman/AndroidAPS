package info.nightscout.aaps.pump.tandem.data.defs

import info.nightscout.aaps.pump.common.defs.PumpConfigurationTypeInterface

enum class TandemPumpSettingType : PumpConfigurationTypeInterface {
    CONTROL_IQ_ENABLED,
    BASAL_LIMIT,
    MAX_BOLUS;

    override fun getKey() = name

}