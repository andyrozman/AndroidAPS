package app.aaps.pump.tandem.common.comm.qe

import android.content.Context
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.pump.common.defs.PumpUpdateFragmentType
import app.aaps.pump.common.events.EventPumpFragmentValuesChanged
import app.aaps.pump.tandem.common.comm.TandemDataConverter
import app.aaps.pump.tandem.common.concurrency.CommSuspendGate
import app.aaps.pump.tandem.common.data.defs.QualifyingEventsFilter
import app.aaps.pump.tandem.common.data.defs.QualifyingEventsRange
import app.aaps.pump.tandem.common.database.TandemPumpDatabase
import app.aaps.pump.tandem.common.database.data.DbDataHandler
import app.aaps.pump.tandem.common.database.data.entity.TandemQualifyingEventEntity
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.events.EventDatabaseAddQEData
import app.aaps.pump.tandem.common.events.EventHandleQualifyingEvent
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.tandem.common.util.PumpX2L
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import com.jwoglom.pumpx2.pump.messages.response.qualifyingEvent.QualifyingEvent
import dagger.android.HasAndroidInjector
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QualifyingEventHandler @Inject constructor(
    val tandemPumpStatus: TandemPumpStatus,
    //val dbDataHandler: DbDataHandler,
    val rxBus: RxBus,
    val aapsLogger: AAPSLogger,
    val aapsSchedulers: AapsSchedulers,
    val preferences: Preferences,
    val commSuspendGate: CommSuspendGate
    ) {

    companion object {
        /**
         * How long to pause wire sends after a [QualifyingEvent.PUMP_COMMUNICATIONS_SUSPENDED]
         * event. Matches controlX2's value (the upstream reference Tandem driver). The pump
         * recovers within a few seconds; if not, subsequent QEs will extend the pause.
         */
        private const val COMMS_SUSPENDED_PAUSE_MS = 5_000L
    }

    @Suppress("PropertyName")
    val TAG = LTag.PUMP


    fun handleEventReceivedFromPump(event : EventHandleQualifyingEvent) {

        aapsLogger.info(TAG, "handleEventReceivedFromPump: ${event.events}")

        // Pause queue dispatch when the pump signals its BT buffer is full / comms suspended.
        // PumpOpQueue.Ctx.send awaits CommSuspendGate before issuing the wire write.
        if (QualifyingEvent.PUMP_COMMUNICATIONS_SUSPENDED in event.events) {
            commSuspendGate.pauseSends(COMMS_SUSPENDED_PAUSE_MS, "PUMP_COMMUNICATIONS_SUSPENDED")
        }

        val sb = StringBuilder()
        var first = true
        val listOfEvents : MutableList<TandemQualifyingEventEntity> = mutableListOf()

        for (qualifyingEvent in event.events) {

            listOfEvents.add(TandemQualifyingEventEntity(pumpSerial = tandemPumpStatus.serialNumber.toInt(),
                                                         dateTime = event.dateTime,
                                                         name = qualifyingEvent.name))
            // TODOX description - at the moment we don't add any special details, since that would
            //    require extra reading. Not sure if this is even needed, so letting it of for now
            if (!first) sb.append(", ") else first = false

            sb.append(qualifyingEvent.name)
        }

        rxBus.send(EventDatabaseAddQEData(listOfEvents))

        tandemPumpStatus.semaphoreEvents = true
        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Custom_2))
    }


    fun filterQualifyingEventsAndLimit(qeItems: List<TandemQualifyingEventEntity>, itemLimit: Int): List<TandemQualifyingEventEntity> {
        val filteredQeItems = filterQualifyingEvents(qeItems)

        return if (itemLimit==0) {
            filteredQeItems
        } else {
            if (filteredQeItems.size>15) {
                filteredQeItems.subList(0,14)
            } else {
                filteredQeItems
            }
        }
    }


    fun filterQualifyingEvents(qeItems: List<TandemQualifyingEventEntity>): List<TandemQualifyingEventEntity> {
        val qeFilter = QualifyingEventsFilter.valueOf(preferences.get(TandemStringPreferenceKey.QualifyingEventsFilterPref))

        if (qeFilter==QualifyingEventsFilter.AAPS_RELEVANT) {
            return filterEvents(qeItems)
        } else {
            return qeItems
        }
    }

    fun filterEvents(qeItems: List<TandemQualifyingEventEntity>): List<TandemQualifyingEventEntity> {
        val outList: MutableList<TandemQualifyingEventEntity> = mutableListOf()

        for (entity in qeItems) {
            val event = resolveQualifyingEvent(entity.name) ?: continue
            if (isAapsRelevant(event)) {
                outList.add(entity)
            }
        }

        aapsLogger.info(TAG, "QualifyingEvents filtering [before=${qeItems.size}, after=${outList.size}]")

        return outList
    }

    // Maps old enum names from v1.8.3 to their v1.8.9 equivalents
    private val legacyEventNames = mapOf(
        "SUSPEND_COMM" to QualifyingEvent.PUMP_COMMUNICATIONS_SUSPENDED,
        "ACTIVE_SEGMENT_CHANGE" to QualifyingEvent.ACTIVE_PROFILE_SEGMENT_CHANGE,
    )

    private fun resolveQualifyingEvent(name: String): QualifyingEvent? {
        legacyEventNames[name]?.let { return it }
        return try {
            QualifyingEvent.valueOf(name)
        } catch (_: IllegalArgumentException) {
            aapsLogger.warn(TAG, "Unknown QualifyingEvent name in database: $name")
            null
        }
    }


    fun isAapsRelevant(event: QualifyingEvent): Boolean {
        return when (event) {
            // Notifications that require user attention or AAPS action
            QualifyingEvent.ALERT,                           // Pump alert active (e.g., low reservoir)
            QualifyingEvent.ALARM,                           // Pump alarm active (e.g., occlusion)
            QualifyingEvent.REMINDER,                        // User-configured reminder triggered
            QualifyingEvent.MALFUNCTION,                     // Pump malfunction detected

            // Pump state changes that affect insulin delivery
            QualifyingEvent.PUMP_SUSPEND,                    // Insulin delivery suspended
            QualifyingEvent.PUMP_RESUME,                     // Insulin delivery resumed
            QualifyingEvent.PUMP_RESET,                      // Pump was reset — may affect delivery state
            QualifyingEvent.PUMP_COMMUNICATIONS_SUSPENDED,   // BT comms suspended (renamed from SUSPEND_COMM)

            // Profile and delivery parameter changes AAPS needs to track
            QualifyingEvent.BASAL_CHANGE,                    // Basal rate changed on pump
            QualifyingEvent.BOLUS_CHANGE,                    // Bolus delivered or cancelled
            QualifyingEvent.PROFILE_CHANGE,                  // Active IDP profile changed
            QualifyingEvent.ACTIVE_PROFILE_SEGMENT_CHANGE,   // Active profile time segment changed (renamed from ACTIVE_SEGMENT_CHANGE)
            QualifyingEvent.BOLUS_PERMISSION_REVOKED,        // Bolus permission revoked by pump

            // Status changes relevant to AAPS state tracking
            QualifyingEvent.HOME_SCREEN_CHANGE,              // Pump home screen updated (reflects delivery state)
            QualifyingEvent.TIME_CHANGE,                     // Pump clock changed — affects scheduling
            QualifyingEvent.BATTERY,                         // Battery level changed
            QualifyingEvent.REMAINING_INSULIN,                // Reservoir level changed

            // Settings
            QualifyingEvent.GLOBAL_PUMP_SETTINGS            // Global pump settings changed (display, sound, etc.)
            -> true

            // CGM data — AAPS gets CGM data from its own CGM source, not the pump
            QualifyingEvent.CGM_ALERT,                       // CGM high/low alert
            QualifyingEvent.BG,                              // Blood glucose reading
            QualifyingEvent.CGM_CHANGE,                      // CGM sensor status change

            // Tandem closed-loop status — not relevant when AAPS controls the loop
            // on the pump here as an indicator to stop DIY looping
            QualifyingEvent.BASAL_IQ_STATUS,                 // Basal-IQ algorithm status
            QualifyingEvent.BASAL_IQ,                        // Basal-IQ event
            QualifyingEvent.CONTROL_IQ_INFO,                 // Control-IQ information update
            QualifyingEvent.CONTROL_IQ_SLEEP,                // Control-IQ sleep activity mode

            // Informational events that don't affect AAPS loop decisions
            QualifyingEvent.IOB_CHANGE,                      // Pump-calculated IOB changed (AAPS calculates its own)
            QualifyingEvent.EXTENDED_BOLUS_CHANGE,           // Extended bolus status (not used by AAPS)
            QualifyingEvent.SNOOZE_STATUS,                   // Alert snooze status changed
            QualifyingEvent.PUMPING_STATUS,                  // Pumping mechanism status (motor activity)
            QualifyingEvent.HEARTBEAT                        // Periodic heartbeat signal
            -> false
        }
    }

}