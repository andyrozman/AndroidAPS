package info.nightscout.aaps.pump.tandem.data.defs

/**
 * This enum is used only for versioned Requests, so that we can dynamically determine them
 */
enum class TandemCommandType {
    ControlIQInfo,
    CurrentBattery

}