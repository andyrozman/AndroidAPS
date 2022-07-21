package info.nightscout.androidaps.plugins.pump.common.driver.scheduler

import info.nightscout.androidaps.interfaces.ActivePlugin
import info.nightscout.androidaps.interfaces.Pump
import info.nightscout.androidaps.interfaces.ResourceHelper
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus
import info.nightscout.androidaps.plugins.pump.common.defs.PumpBaseNotificationType
import info.nightscout.androidaps.plugins.pump.common.utils.PumpUtil
import info.nightscout.shared.logging.AAPSLogger
import java.lang.RuntimeException
import javax.inject.Singleton
import kotlin.collections.HashMap

@Singleton
class PumpCommandSchedulerUtil(var aapsLogger: AAPSLogger,
                                var rxBus: RxBus,
                                val resourceHelper: ResourceHelper,
                                var activePlugin: ActivePlugin
                                ) {

    //var hasTimeDateOrTimeZoneChanged: Boolean = false



    private val statusRefreshMap: MutableMap<PumpStatusRefreshType, Long> = mutableMapOf()

    var schedulerSettings: PumpCommandSchedulerSettings? = null
    var schedulerCapable: PumpCommandSchedulerCapable? = null
    var pumpDriver : Pump? = null
    var pumpStatus: PumpStatus? = null
    var pumpUtil: PumpUtil? = null

    //var isPumpNotReachable: Boolean = false


    @Synchronized
    private fun workWithStatusRefresh(action: StatusRefreshAction,  //
                                      statusRefreshType: PumpStatusRefreshType?,  //
                                      time: Long?): Map<PumpStatusRefreshType, Long>? {
        return when (action) {
            StatusRefreshAction.Add     -> {
                statusRefreshMap[statusRefreshType!!] = time!!
                null
            }

            StatusRefreshAction.GetData -> {
                HashMap(statusRefreshMap)
            }

        }
    }

    fun refreshAnyStatusThatNeedsToBeRefreshed() {

        checkPlugin()

        val statusRefresh = workWithStatusRefresh(StatusRefreshAction.GetData, null,
                                                  null)!!
        if (!doWeHaveAnyStatusNeededRefreshing(statusRefresh)) {
            return
        }
        var resetTime = false

        if (schedulerSettings!!.canPumpBeUnreachable()) {
            if (schedulerCapable!!.isPumpNotReachable) {
                aapsLogger.error("Pump unreachable.")
                pumpUtil!!.sendNotification(PumpBaseNotificationType.PumpUnreachable, resourceHelper, rxBus)
                return
            }
            pumpUtil!!.dismissNotification(PumpBaseNotificationType.PumpUnreachable, rxBus)
        }

        var timeAlreadySet: Boolean = false
        val refreshTypesNeededToReschedule: MutableSet<PumpStatusRefreshType> = mutableSetOf()

        if (schedulerCapable!!.hasTimeDateOrTimeZoneChanged) {
            schedulerCapable!!.checkTimeAndOptionallySetTime()

            // read time if changed, set new time
            schedulerCapable!!.hasTimeDateOrTimeZoneChanged = false
            timeAlreadySet = true
        }

        // execute

        for ((key, value) in statusRefresh) {
            if (value > 0 && System.currentTimeMillis() > value) {
                when (key) {
                    PumpStatusRefreshType.PumpHistory                                                -> {
                        schedulerCapable!!.executeScheduledCommand(key)
                        //readPumpHistory()
                    }

                    PumpStatusRefreshType.PumpTime                                                   -> {
                        if (!timeAlreadySet) {
                            schedulerCapable!!.checkTimeAndOptionallySetTime()
                        }
                        refreshTypesNeededToReschedule.add(key)
                        resetTime = true
                    }

                    PumpStatusRefreshType.BatteryStatus,
                    PumpStatusRefreshType.RemainingInsulin -> {
                        schedulerCapable!!.executeScheduledCommand(key)
                        //rileyLinkMedtronicService?.medtronicUIComm?.executeCommand(key.getCommandType(medtronicUtil.medtronicPumpModel)!!)
                        refreshTypesNeededToReschedule.add(key)
                        resetTime = true
                    }

                    PumpStatusRefreshType.Configuration                                              -> {
                        schedulerCapable!!.executeScheduledCommand(key)
                        //rileyLinkMedtronicService?.medtronicUIComm?.executeCommand(key.getCommandType(medtronicUtil.medtronicPumpModel)!!)
                        resetTime = true
                    }
                }
            }

            // reschedule
            for (refreshType2 in refreshTypesNeededToReschedule) {
                scheduleNextRefresh(refreshType2)
            }
        }

        if (resetTime) pumpStatus!!.setLastCommunicationToNow()
    }

    fun doWeHaveAnyStatusNeededRefreshing(): Boolean {
        val statusRefresh = workWithStatusRefresh(StatusRefreshAction.GetData, null,
                                                  null)!!
        return doWeHaveAnyStatusNeededRefreshing(statusRefresh)
    }


    private fun doWeHaveAnyStatusNeededRefreshing(statusRefresh: Map<PumpStatusRefreshType, Long>): Boolean {
        for ((_, value) in statusRefresh) {
            if (value > 0 && System.currentTimeMillis() > value) {
                return true
            }
        }
        return schedulerCapable!!.hasTimeDateOrTimeZoneChanged
    }

    fun scheduleNextRefresh(refreshType: PumpStatusRefreshType, additionalTimeInMinutes: Int = 0) {
        checkPlugin()
        when (refreshType) {
            PumpStatusRefreshType.RemainingInsulin              -> {
                val remaining = pumpStatus!!.reservoirRemainingUnits
                val min: Int = if (remaining > 50) 4 * 60 else if (remaining > 20) 60 else 15
                workWithStatusRefresh(StatusRefreshAction.Add, refreshType, pumpUtil!!.getTimeInFutureFromMinutes(min))
            }

            PumpStatusRefreshType.PumpTime,
            PumpStatusRefreshType.Configuration,
            PumpStatusRefreshType.BatteryStatus,
            PumpStatusRefreshType.PumpHistory -> {
                workWithStatusRefresh(StatusRefreshAction.Add, refreshType,
                                      getRefreshTime(refreshType, additionalTimeInMinutes))
            }
        }
    }

    private fun getRefreshTime(refreshAction: PumpStatusRefreshType, additionalTimeInMinutes: Int): Long {
        val refreshTime = schedulerSettings!!.getRefreshTime(refreshAction)
        return pumpUtil!!.getTimeInFutureFromMinutes(refreshTime + additionalTimeInMinutes)
    }


    // private fun checkTimeAndOptionallySetTime() {
    //     aapsLogger.info(LTag.PUMP, "MedtronicPumpPlugin::checkTimeAndOptionallySetTime - Start")
    //     setRefreshButtonEnabled(false)
    //     if (isPumpNotReachable) {
    //         aapsLogger.debug(LTag.PUMP, "MedtronicPumpPlugin::checkTimeAndOptionallySetTime - Pump Unreachable.")
    //         setRefreshButtonEnabled(true)
    //         return
    //     }
    //     medtronicUtil.dismissNotification(MedtronicNotificationType.PumpUnreachable, rxBus)
    //     rileyLinkMedtronicService?.medtronicUIComm?.executeCommand(MedtronicCommandType.GetRealTimeClock)
    //     var clock = medtronicUtil.pumpTime
    //     if (clock == null) { // retry
    //         rileyLinkMedtronicService?.medtronicUIComm?.executeCommand(MedtronicCommandType.GetRealTimeClock)
    //         clock = medtronicUtil.pumpTime
    //     }
    //     if (clock == null) return
    //     val timeDiff = abs(clock.timeDifference)
    //     if (timeDiff > 20) {
    //         if (clock.localDeviceTime.year <= 2015 || timeDiff <= 24 * 60 * 60) {
    //             aapsLogger.info(LTag.PUMP, String.format(Locale.ENGLISH, "MedtronicPumpPlugin::checkTimeAndOptionallySetTime - Time difference is %d s. Set time on pump.", timeDiff))
    //             rileyLinkMedtronicService?.medtronicUIComm?.executeCommand(MedtronicCommandType.SetRealTimeClock)
    //             if (clock.timeDifference == 0) {
    //                 val notification = Notification(Notification.INSIGHT_DATE_TIME_UPDATED, rh.gs(R.string.pump_time_updated), Notification.INFO, 60)
    //                 rxBus.send(EventNewNotification(notification))
    //             }
    //         } else {
    //             if (clock.localDeviceTime.year > 2015) {
    //                 aapsLogger.error(String.format(Locale.ENGLISH, "MedtronicPumpPlugin::checkTimeAndOptionallySetTime - Time difference over 24h requested [diff=%d s]. Doing nothing.", timeDiff))
    //                 medtronicUtil.sendNotification(MedtronicNotificationType.TimeChangeOver24h, rh, rxBus)
    //             }
    //         }
    //     } else {
    //         aapsLogger.info(LTag.PUMP, String.format(Locale.ENGLISH, "MedtronicPumpPlugin::checkTimeAndOptionallySetTime - Time difference is %d s. Do nothing.", timeDiff))
    //     }
    //     scheduleNextRefresh(PumpStatusRefreshType.PumpTime, 0)
    // }
    //
    // private fun setRefreshButtonEnabled(enabled: Boolean) {
    //     rxBus.send(EventRefreshButtonState(enabled))
    // }

    fun checkPlugin(throwException: Boolean = true) {
        if (pumpDriver==null || !pumpDriver!!.equals(activePlugin.activePump)) {
            pumpDriver = activePlugin.activePump
            if (pumpDriver is PumpCommandSchedulerCapable) {
                schedulerCapable = activePlugin.activePump as PumpCommandSchedulerCapable
                schedulerSettings = schedulerCapable!!.getSchedulerSettings()
                pumpUtil = schedulerCapable!!.getPumpUtil()
                pumpStatus = schedulerCapable!!.getPumpStatus()
            }
        }
        if (throwException) {
            if (schedulerCapable == null || schedulerSettings == null) {
                throw RuntimeException("You can use PumpCommandScheduler only with PumpPlugin supporting it (implementing PumpCommandSchedulerCapable).")
            }
        }
    }

    init {
        checkPlugin(false)
    }



}


private enum class StatusRefreshAction {
    Add,  //
    GetData
}