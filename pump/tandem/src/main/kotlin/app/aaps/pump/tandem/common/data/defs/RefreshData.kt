package app.aaps.pump.tandem.common.data.defs

enum class RefreshData {
    SEMAPHORE_HISTORY,
    SEMAPHORE_EVENTS,
    SEMAPHORE_NOTIFICATIONS,
    //@Deprecated("Use other options instead")
    //PUMP_STATUS, // deprecated
    //@Deprecated("Use other options instead")
    //PUMP_INSULIN_LEVEL, // deprecated
    PUMP_CANNULA_CHANGED,
    PUMP_SITE_CHANGED,
    PUMP_STATE_CHANGED,
    REMINDER_CHANGED,
    //QUEUE_DISABLED,
    START_ACTIONS,
    START_DATA
}