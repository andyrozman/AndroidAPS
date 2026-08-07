package app.aaps.pump.tandem.common.keys

import app.aaps.core.keys.interfaces.StringNonPreferenceKey
import app.aaps.core.keys.interfaces.StringPreferenceKey

enum class TandemStringNonPreferenceKey(
    override val key: String,
    override val defaultValue: String,
    override val exportable: Boolean) : StringNonPreferenceKey {

    HistorySummaryData("tandem_history_summary", "", true),

    /**
     * Site location picked in the cartridge workflow, parked until the pump reports the matching
     * `CannulaFilledHistoryLog`. Holds a [app.aaps.core.data.model.TE.Location] name, empty when none is pending.
     */
    PendingSiteLocation("tandem_pending_site_location", "", false),

    /**
     * Site arrow picked in the cartridge workflow, parked alongside [PendingSiteLocation].
     * Holds a [app.aaps.core.data.model.TE.Arrow] name, empty when none is pending.
     */
    PendingSiteArrow("tandem_pending_site_arrow", "", false)

}