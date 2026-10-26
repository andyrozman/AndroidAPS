package app.aaps.pump.tandem.common.keys

import app.aaps.core.keys.interfaces.StringNonPreferenceKey
import app.aaps.core.keys.interfaces.StringPreferenceKey

enum class TandemStringNonPreferenceKey(
    override val key: String,
    override val defaultValue: String,
    override val exportable: Boolean) : StringNonPreferenceKey {

    HistorySummaryData("tandem_history_summary", "", true)

}