package info.nightscout.androidaps.plugins.pump.common.driver.history

interface PumpHistoryDataProvider {

    fun getData(): List<PumpHistoryEntry>

    /**
     * Get Translation Text
     */
    fun getText(key: PumpHistoryText): String

}

enum class PumpHistoryText {

    PUMP_HISTORY,

    // OLD ONES
    SCAN_TITLE,
    SELECTED_PUMP_TITLE,
    REMOVE_TITLE,
    REMOVE_TEXT,
    NO_SELECTED_PUMP,
    PUMP_CONFIGURATION
}
