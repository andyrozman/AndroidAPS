package app.aaps.pump.tandem.common.keys

import app.aaps.core.keys.interfaces.BooleanPreferenceKey
import app.aaps.core.keys.interfaces.TextRef
import app.aaps.pump.tandem.R

enum class TandemBooleanPreferenceKey(
    override val key: String,
    override val defaultValue: Boolean,
    val titleResId: Int = 0,
    val summaryResId: Int? = null,
    override val calculatedDefaultValue: Boolean = false,
    override val engineeringModeOnly: Boolean = false,
    override val defaultedBySM: Boolean = false,
    override val showInApsMode: Boolean = true,
    override val showInNsClientMode: Boolean = true,
    override val showInPumpControlMode: Boolean = true,
    override val dependency: BooleanPreferenceKey? = null,
    override val negativeDependency: BooleanPreferenceKey? = null,
    override val exportable: Boolean = true,
    override val hideParentScreenIfHidden: Boolean = false,
) : BooleanPreferenceKey {

    DisplayDriverVersion(key = "pref_tandem_display_driver_version", defaultValue = true,
        titleResId = R.string.tandem_cfg_display_driver_version,
        summaryResId = R.string.tandem_cfg_display_driver_version_summary
    ),
    UseSharedConnection(key = "pref_tandem_use_shared_connection", defaultValue = false,
                        titleResId = R.string.tandem_cfg_use_shared_connection,
                        summaryResId = R.string.tandem_cfg_use_shared_connection_summary),

    ShowCargoOfUnknownEntries(key ="pref_tandem_show_unknowns_cargo", defaultValue = false,
                              titleResId = R.string.tandem_cfg_show_cargo_of_unknown_logs,
                              summaryResId = R.string.tandem_cfg_show_cargo_of_unknown_logs_summary),

    AutoConfirmLowBasalDelivery(key ="pref_tandem_auto_confirm_low_basal_delivery", defaultValue = false,
                                titleResId = R.string.tandem_cfg_auto_confirm_low_basal_delivery,
                                summaryResId = R.string.tandem_cfg_auto_confirm_low_basal_delivery_summary);

    override val title: TextRef = TextRef.AndroidRes(titleResId)
    override val summary: TextRef? = summaryResId?.let { TextRef.AndroidRes(it) }

}