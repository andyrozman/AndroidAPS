package app.aaps.pump.tandem.common.keys

import app.aaps.core.keys.interfaces.BooleanPreferenceKey
import app.aaps.core.keys.interfaces.IntPreferenceKey
import app.aaps.core.keys.interfaces.TextRef
import app.aaps.pump.tandem.R

enum class TandemIntPreferenceKey(
    override val key: String,
    override val defaultValue: Int,
    val titleResId: Int = 0,
    val summaryResId: Int? = null,
    override val min: Int = Int.MIN_VALUE,
    override val max: Int = Int.MAX_VALUE,
    override val calculatedDefaultValue: Boolean = false,
    override val engineeringModeOnly: Boolean = false,
    override val defaultedBySM: Boolean = false,
    override val showInApsMode: Boolean = true,
    override val showInNsClientMode: Boolean = true,
    override val showInPumpControlMode: Boolean = true,
    override val dependency: BooleanPreferenceKey? = null,
    override val negativeDependency: BooleanPreferenceKey? = null,
    override val hideParentScreenIfHidden: Boolean = false,
    override val exportable: Boolean = true
) : IntPreferenceKey {

    MaxBasal(key = "pref_tandem_max_basal",
             defaultValue = 15,
             min = 1,
             max = 15,
             titleResId = R.string.tandem_pump_max_basal),

    MaxBolus(key ="pref_tandem_max_bolus",
             defaultValue = 25,
             min = 1,
             max = 25,
             titleResId = R.string.tandem_pump_max_bolus),

    PumpPairStatus(key = "pref_tandem_pair_status",
                   defaultValue = -1,
                   titleResId = R.string.tandem_pump_pair_status);

    override val title: TextRef = TextRef.AndroidRes(titleResId)
    override val summary: TextRef? = summaryResId?.let { TextRef.AndroidRes(it) }

}