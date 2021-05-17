package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs

enum class YpsoConnectionStatus {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    PUMP_CONNECTED,
    PUMP_ENCRYPTION,
    PUMP_ENCRYPTION_ERROR,
    PUMP_COMMAND_RUNNING,
    PUMP_READY,
    PUMP_NOT_FOUND,
    DISCONNECTING,
    DISCONNECTED
}