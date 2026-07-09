package app.aaps.pump.tandem.common.keys

import app.aaps.core.keys.PreferenceType
import app.aaps.core.keys.interfaces.BooleanPreferenceKey
import app.aaps.core.keys.interfaces.StringPreferenceKey
import app.aaps.pump.tandem.R
import app.aaps.pump.tandem.common.data.defs.QualifyingEventsFilter
import app.aaps.pump.tandem.common.data.defs.QualifyingEventsRange
import app.aaps.pump.tandem.common.data.defs.QuickBolusType

enum class TandemStringPreferenceKey(
    override val key: String,
    override val defaultValue: String,
    override val titleResId: Int = 0,
    override val summaryResId: Int? = null,
    override val preferenceType: PreferenceType = PreferenceType.TEXT_FIELD,
    override val entries: Map<String, Int> = emptyMap(),
    override val defaultedBySM: Boolean = false,
    override val showInApsMode: Boolean = true,
    override val showInNsClientMode: Boolean = true,
    override val showInPumpControlMode: Boolean = true,
    override val dependency: BooleanPreferenceKey? = null,
    override val negativeDependency: BooleanPreferenceKey? = null,
    override val hideParentScreenIfHidden: Boolean = false,
    override val isPassword: Boolean = false,
    override val isPin: Boolean = false,
    override val exportable: Boolean = true
) : StringPreferenceKey {

    PumpSerial("pref_tandem_serial", ""),
    PumpAddress("pref_tandem_address", ""),
    PumpName("pref_tandem_name", ""),
    SharedConnectionData(key="pref_tandem_shared_connection_data",
                         defaultValue = "",
                         titleResId = R.string.tandem_cfg_shared_connection_data,
                         dependency = TandemBooleanPreferenceKey.UseSharedConnection, isPassword = true),
    PumpPairCode("pref_tandem_pair_code", ""),
    PumpApiVersion("pref_tandem_api_version", ""),
    PumpVersionResponse("pref_tandem_pump_version", ""),

    PumpHistorySummary("pref_tandem_history_summary", ""),

    QualifyingEventsFilterPref(key = "pref_tandem_qe_filter",
                               defaultValue = QualifyingEventsFilter.ALL.name,
                               preferenceType = PreferenceType.LIST,
                               titleResId = R.string.data_qe_filter_description,
                               entries = mapOf(
                                   QualifyingEventsFilter.ALL.name to QualifyingEventsFilter.ALL.friendlyName,
                                   QualifyingEventsFilter.AAPS_RELEVANT.name to QualifyingEventsFilter.AAPS_RELEVANT.friendlyName
                               )
    ),

    QualifyingEventsRangePref(key = "pref_tandem_qe_range",
                              defaultValue = QualifyingEventsRange.LAST_15_ITEMS.name,
                              preferenceType = PreferenceType.LIST,
                              titleResId = R.string.data_qe_range_description,
                              entries = mapOf(
                                  QualifyingEventsRange.LAST_15_ITEMS.name to QualifyingEventsRange.LAST_15_ITEMS.friendlyName,
                                  QualifyingEventsRange.LAST_3_HOURS.name to QualifyingEventsRange.LAST_3_HOURS.friendlyName,
                                  QualifyingEventsRange.LAST_6_HOURS.name to QualifyingEventsRange.LAST_6_HOURS.friendlyName,
                                  QualifyingEventsRange.LAST_12_HOURS.name to QualifyingEventsRange.LAST_12_HOURS.friendlyName,
                                  QualifyingEventsRange.LAST_24_HOURS.name to QualifyingEventsRange.LAST_24_HOURS.friendlyName
                              )
    ),

    QuickBolusTypePref(key = "pref_tandem_quick_bolus",
                       defaultValue = QuickBolusType.DISABLED.name,
                       preferenceType = PreferenceType.LIST,
                       titleResId = R.string.pump_quick_bolus_description,
                       entries = mapOf(
                           QuickBolusType.DISABLED.name to QuickBolusType.DISABLED.friendlyName,
                           QuickBolusType.UNITS_0_5.name to QuickBolusType.UNITS_0_5.friendlyName,
                           QuickBolusType.UNITS_1_0.name to QuickBolusType.UNITS_1_0.friendlyName,
                           QuickBolusType.UNITS_2_O.name to QuickBolusType.UNITS_2_O.friendlyName,
                           QuickBolusType.UNITS_5_0.name to QuickBolusType.UNITS_5_0.friendlyName,
                           QuickBolusType.CARBS_2G.name to QuickBolusType.CARBS_2G.friendlyName,
                           QuickBolusType.CARBS_5G.name to QuickBolusType.CARBS_5G.friendlyName,
                           QuickBolusType.CARBS_10G.name to QuickBolusType.CARBS_10G.friendlyName,
                           QuickBolusType.CARBS_15G.name to QuickBolusType.CARBS_15G.friendlyName
                       )
    )



    // Encoding(
    // key = "pref_medtronic_encoding",
    // defaultValue = "medtronic_pump_encoding_4b6b_rileylink",
    // titleResId = R.string.medtronic_pump_encoding_title,
    // preferenceType = PreferenceType.LIST,
    // entries = mapOf(
    // "medtronic_pump_encoding_4b6b_local" to R.string.medtronic_pump_encoding_4b6b_local,
    // "medtronic_pump_encoding_4b6b_rileylink" to R.string.medtronic_pump_encoding_4b6b_rileylink
    // )
    // )







}