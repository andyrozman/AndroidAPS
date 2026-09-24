package app.aaps.pump.tandem.common.keys

import app.aaps.core.keys.interfaces.BooleanPreferenceKey
import app.aaps.core.keys.interfaces.IntentPreferenceKey
import app.aaps.core.keys.interfaces.TextRef
import app.aaps.pump.tandem.R

enum class TandemIntentPreferenceKey(
    override val key: String,
    val titleResId: Int = 0,
    val summaryResId: Int? = null,
    override val defaultedBySM: Boolean = false,
    override val showInApsMode: Boolean = true,
    override val showInNsClientMode: Boolean = true,
    override val showInPumpControlMode: Boolean = true,
    override val dependency: BooleanPreferenceKey? = null,
    override val negativeDependency: BooleanPreferenceKey? = null,
    override val hideParentScreenIfHidden: Boolean = false,
    override val exportable: Boolean = false
) : IntentPreferenceKey {

    PumpPairing(key = "pref_tandem_device_selector",
                negativeDependency = TandemBooleanPreferenceKey.UseSharedConnection,
                titleResId = R.string.tandem_pump_configuration,
                summaryResId = R.string.tandem_pump_configuration_subtitle);

    override val title: TextRef = TextRef.AndroidRes(titleResId)
    override val summary: TextRef? = summaryResId?.let { TextRef.AndroidRes(it) }
}