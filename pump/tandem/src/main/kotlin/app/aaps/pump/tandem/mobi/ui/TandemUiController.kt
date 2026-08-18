package app.aaps.pump.tandem.mobi.ui

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.pump.tandem.common.comm.ui.TandemUICommunication
import app.aaps.pump.tandem.common.concurrency.TandemDispatcher
import app.aaps.pump.tandem.common.data.defs.RefreshData
import app.aaps.pump.tandem.common.database.data.DbDataHandler
import app.aaps.pump.tandem.common.database.data.defs.DatabaseQueryParameters
import app.aaps.pump.tandem.common.database.data.defs.DatabaseTarget
import app.aaps.pump.tandem.common.database.data.dto.TandemQualifyingEventDto
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnector
import app.aaps.pump.tandem.common.driver.tandemUiDataStore
import app.aaps.pump.tandem.common.keys.TandemLongNonPreferenceKey
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.response.qualifyingEvent.QualifyingEvent
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class TandemUiController @Inject constructor(
    var aapsLogger: AAPSLogger,
    var tandemPumpStatus: TandemPumpStatus,
    var tandemPumpUtil: TandemPumpUtil,
    var preferences: Preferences,
    var uiInteraction: app.aaps.core.interfaces.ui.UiInteraction,
    var dbDataHandler: DbDataHandler,
    var notificationManager: NotificationManager,
    var tandemPumpConnector: TandemPumpConnector,
    var tandemDispatcher: TandemDispatcher
)   {

    // TODO fix logs (I set a lot of them to error so that I can easily see what is happening)

    var TAG = LTag.PUMPCOMM

    val ds = tandemUiDataStore

    val setOfActions : MutableSet<RefreshData> = mutableSetOf()

    lateinit var tandemUICommunication: TandemUICommunication


    fun createTandemUiCommunication() {
        tandemUICommunication = TandemUICommunication(dataStore = tandemUiDataStore,
                                                      pumpStatus = tandemPumpStatus,
                                                      pumpUtil = tandemPumpUtil,
                                                      aapsLogger= aapsLogger,
                                                      uiInteraction = uiInteraction,
                                                      notificationManager = notificationManager)

        this.tandemUICommunication.tandemPumpCommunicationManager = tandemPumpConnector.getCommunicationManager()
    }

    /**
     * Suppresses AAPS auto-reconnect for the duration of a long-running pump-owning UI workflow.
     * Only cartridge change (and its sub-steps fill-tubing / fill-cannula) sets this — quick
     * read-only browsing in Actions / Data does not, since those sends are serialized through
     * [PumpOpQueue] at USER_INITIATED priority and AAPS Loop can safely interleave.
     */
    fun setCartridgeChangeMode(active: Boolean) {
        // tandemPumpUtil.preventConnect = active
    }

    fun disposeTandemUiCommunication(disposeType: AdditionalConfigurationScreens) {
        if (disposeType==AdditionalConfigurationScreens.Actions) {
            aapsLogger.error(LTag.PUMP, "Actions window was closed. Sending event to refresh.")

            //aapsLogger.error(LTag.PUMP, "Reminder Date found")

            // if (ds.reminderDateTimeUpdated.value == true) {
            //     aapsLogger.error(TAG, "Reminder Date Time: ${ds.reminderDateTime.value}")
            //
            //     preferences.put(TandemLongNonPreferenceKey.SiteReminderDateTime, ds.reminderDateTime.value!!)
            //     tandemPumpStatus.tandemSiteReminder = ds.reminderDateTime.value!!
            // }

            // we might be able to specify more exactly what here happens but for now this is ok, see DataActivity and method refreshMainAppData
            // tandemPumpUtil.refreshPumpStatus(listOf(RefreshData.PUMP_STATUS,
            //                                         RefreshData.PUMP_INSULIN_LEVEL))
            val list: MutableList<RefreshData> = mutableListOf()
            list.addAll(setOfActions)

            tandemPumpUtil.refreshPumpStatus(list)

            setOfActions.clear()

        } else if (disposeType== AdditionalConfigurationScreens.Data) {
            aapsLogger.info(LTag.PUMP, "Data Activity was closed. Sending event to refresh.")
            tandemPumpUtil.refreshPumpStatus(listOf(RefreshData.SEMAPHORE_EVENTS))
        }

        this.tandemUICommunication.tandemPumpCommunicationManager = null
        tandemPumpUtil.preventConnect = false // TODO remove replaced with preventueueExecution

        aapsLogger.error(LTag.PUMP, "QQ Prevent Queue Execution reset on Mobi (exiting Actions/Data)")
        tandemPumpStatus.preventQueueExecution = false
    }

    enum class AdditionalConfigurationScreens {
        Actions,
        Data
    }


    fun sendPumpCommands(msgs: List<Message>): Boolean {

        if (tandemUiDataStore.pumpConnected.value==false) {
            aapsLogger.warn(TAG, "sendPumpCommands not possible, because pump is not yet connected")
            return false
        }

        val sb = StringBuilder()

        for (msg in msgs) {
            sb.append(", ${msg.javaClass.name}")
        }

        val listText = sb.substring(2)

        aapsLogger.warn(TAG, "PumpCommands to Send [commands=${listText}]")

        if (!::tandemUICommunication.isInitialized) {
            createTandemUiCommunication()
        } else {
            if (this.tandemUICommunication.tandemPumpCommunicationManager==null) {
                this.tandemUICommunication.tandemPumpCommunicationManager = tandemPumpConnector.getCommunicationManager()
            }
        }

        // Each UI-initiated wire send is queued at USER_INITIATED priority — jumps ahead of
        // background AAPS-loop work so the user's tap doesn't wait. Fire-and-forget: the op
        // completes once the wire send fires; responses arrive asynchronously via the listener
        // path (TandemUICommunication.onReceiveMessage).
        for (msg in msgs) {
            // TODO this doesn't work - jwoglom
            // tandemDispatcher.submitUser("ui:${msg.javaClass.simpleName}") {
            //     this.tandemUICommunication.sendCommand(msg)
            // }
            this.tandemUICommunication.sendCommand(msg)
        }

        return true

    }


    fun refreshMainAppData(refreshData: RefreshData) {
        when(refreshData) {
            RefreshData.SEMAPHORE_HISTORY       -> {
                tandemPumpStatus.semaphoreHistory = false
                tandemPumpStatus.semaphoreNeedsRefresh = true
            }
            RefreshData.SEMAPHORE_EVENTS        -> {
                tandemPumpStatus.semaphoreEvents = false
                tandemPumpStatus.semaphoreNeedsRefresh = true
            }
            RefreshData.SEMAPHORE_NOTIFICATIONS -> {
                tandemPumpStatus.semaphoreNotifications = false
                tandemPumpStatus.semaphoreNeedsRefresh = true
            }
            RefreshData.PUMP_STATE_CHANGED,
            RefreshData.PUMP_SITE_CHANGED,
            RefreshData.PUMP_CANNULA_CHANGED -> {
                setOfActions.add(refreshData)
            }
            RefreshData.REMINDER_CHANGED -> {

                if (ds.reminderDateTimeUpdated.value!=null && ds.reminderDateTimeUpdated.value!!) {

                    val value = ds.reminderDateTime.value
                    tandemPumpStatus.tandemSiteReminder = value

                    aapsLogger.info(TAG, "Reminder Changed. Saving value in preferences: ${value}")

                    if (value != null) {
                        preferences.put(TandemLongNonPreferenceKey.SiteReminderDateTime, value)
                    }
                } else {
                    aapsLogger.debug(TAG, "Reminder Hasn't updated")
                }
            }
            RefreshData.START_ACTIONS       -> {
                tandemPumpUtil.preventConnect = true
                tandemPumpStatus.preventQueueExecution = true
                ds.reminderDateTime.value = tandemPumpStatus.tandemSiteReminder
            }
            RefreshData.START_DATA       -> {
                tandemPumpUtil.preventConnect = true
                tandemPumpStatus.preventQueueExecution = true
            }
        }
    }


    fun refreshDatabase(databaseTarget: DatabaseTarget, queryParameters: DatabaseQueryParameters) {
        val jsonParamVal = tandemPumpUtil.gson.toJson(queryParameters)

        aapsLogger.debug(TAG, "refreshDatabase: called with target=${databaseTarget.name} and parameters=$jsonParamVal")

        when(databaseTarget) {
            DatabaseTarget.QUALIFYING_EVENTS -> {

                val currentQEItemsBlocking = dbDataHandler.getCurrentQEItemsBlocking();

                val list: MutableList<TandemQualifyingEventDto> = mutableListOf()

                for (entity in currentQEItemsBlocking) {
                    val instantTime = java.time.Instant.ofEpochMilli(entity.dateTime)

                    val eventDto = TandemQualifyingEventDto(
                        dateTime = LocalDateTime.ofInstant(instantTime, ZoneId.systemDefault()),
                        name = QualifyingEvent.valueOf(entity.name),
                        description = if (entity.description==null) "" else  entity.description!!
                    )

                    list.add(eventDto)
                }


                val list2 = ds.dataQE.value!!

                list2.clear()
                list2.addAll(list)

                ds.dataQELoaded.value = true

                aapsLogger.info(TAG, "QE Items ${list2.size}")

            }
            DatabaseTarget.PUMP_HISTORY      -> {

                val list = dbDataHandler.getHistoryRecords(queryParameters)

                val list2 = ds.dataHistory.value!!

                list2.clear()
                list2.addAll(list)

                aapsLogger.info(TAG, "History Items ${list2.size}")

                ds.dataHistoryLoaded.value = true
            }
        }

    }

}