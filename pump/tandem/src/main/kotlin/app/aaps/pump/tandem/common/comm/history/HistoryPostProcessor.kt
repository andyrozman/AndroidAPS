package app.aaps.pump.tandem.common.comm.history

import android.content.Context
import app.aaps.core.data.model.BS
import app.aaps.core.data.model.TE
import app.aaps.core.data.time.T
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.pump.PumpInsulin
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.utils.DateTimeUtil
import app.aaps.pump.tandem.common.comm.history.HistoryRetriever.Companion.DEBUG_HISTORY
import app.aaps.pump.tandem.common.concurrency.TandemDispatcher
import app.aaps.pump.tandem.common.database.data.DbDataHandler
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnector
import app.aaps.pump.tandem.common.keys.TandemLongNonPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringNonPreferenceKey
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import com.jwoglom.pumpx2.pump.messages.response.historyLog.BolusCompletedHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.CannulaFilledHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.CartridgeFilledHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.TubingFilledHistoryLog
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryPostProcessor @Inject constructor(
    val pumpStatus: TandemPumpStatus,
    val aapsLogger: AAPSLogger,
    val pumpSync: PumpSync,
    val tandemPumpUtil: TandemPumpUtil,
    val preferences: Preferences,
    val persistenceLayer: PersistenceLayer
) {

    var historyPrefix = ""

    companion object {
        val TAG = LTag.PUMPCOMM

        /**
         * How long a parked site location stays valid. Long enough to cover a workflow interrupted
         * by a lost connection, short enough that a forgotten selection is not attached to the next
         * site change days later.
         */
        private val PENDING_SITE_VALIDITY = T.hours(6).msecs()

        /** Allowance for drift between pump time and phone time when ordering the two. */
        private val PENDING_SITE_CLOCK_SKEW = T.hours(1).msecs()
    }


    fun setupHistoryPostProcessor(debugHistory: Boolean) {
        historyPrefix = if (debugHistory) "HST: " else ""
    }


    fun postProcessHistory(historyLogs: MutableCollection<HistoryLog>) {

        aapsLogger.debug(TAG, "${historyPrefix}PostProcess History (items=${historyLogs.size})")

        for (historyLog in historyLogs) {

            when(historyLog) {
                is CannulaFilledHistoryLog -> {

                    aapsLogger.info(TAG, "${historyPrefix}PostProcess - NS Cannula Change")

                    val timestamp = historyLog.pumpTimeSecInstant.toEpochMilli()

                    runBlocking {
                        pumpSync.insertTherapyEventIfNewWithTimestamp(
                            timestamp = timestamp,
                            type = TE.Type.CANNULA_CHANGE,
                            note = null,
                            pumpId = historyLog.sequenceNum,
                            pumpType = pumpStatus.pumpType,
                            pumpSerial = pumpStatus.serialNumber.toString()
                        )
                        // The cannula fill is the actual site insertion, so this is the event that
                        // carries the location the user picked earlier in the cartridge workflow.
                        applyPendingSiteLocation(timestamp)
                    }
                }
                is TubingFilledHistoryLog -> {

                    aapsLogger.info(TAG, "${historyPrefix}PostProcess - NS Cannula Change")

                    runBlocking {
                        pumpSync.insertTherapyEventIfNewWithTimestamp(
                            timestamp = historyLog.pumpTimeSecInstant.toEpochMilli(),
                            type = TE.Type.CANNULA_CHANGE,
                            note = null,
                            pumpId = historyLog.sequenceNum,
                            pumpType = pumpStatus.pumpType,
                            pumpSerial = pumpStatus.serialNumber.toString()
                        )
                    }
                }
                is CartridgeFilledHistoryLog -> {

                    aapsLogger.info(TAG, "${historyPrefix}PostProcess - NS Insulin Change")

                    runBlocking {
                        pumpSync.insertTherapyEventIfNewWithTimestamp(
                            timestamp = historyLog.pumpTimeSecInstant.toEpochMilli(),
                            type = TE.Type.INSULIN_CHANGE,
                            note = null,
                            pumpId = historyLog.sequenceNum,
                            pumpType = pumpStatus.pumpType,
                            pumpSerial = pumpStatus.serialNumber.toString()
                        )
                    }
                }

                is BolusCompletedHistoryLog -> {
                    runBlocking {

                        aapsLogger.info(TAG, "${historyPrefix}PostProcess - Bolus - ${historyLog}")

                        pumpSync.syncBolusWithPumpId(
                            timestamp = historyLog.pumpTimeSecInstant.toEpochMilli(),
                            amount = PumpInsulin(historyLog.insulinDelivered.toDouble()),
                            pumpId = tandemPumpUtil.getPrefixedIdForDb(pumpEventId = historyLog.bolusId.toLong(), isBolus = true),
                            pumpType = pumpStatus.pumpType,
                            pumpSerial = pumpStatus.serialNumber.toString(),
                            type = null
                        )
                    }
                }

            }
        }

    }

    /**
     * Attach the site location picked in the cartridge workflow to the CANNULA_CHANGE event just
     * inserted at [timestamp].
     *
     * The selection is parked in preferences by `CoreCartridgeActionsModel`, because when the user
     * confirms it the pump has not yet reported the cannula fill and the event does not exist.
     * The parked value is dropped once it expires, so a workflow the user abandoned cannot tag an
     * unrelated site change later on.
     */
    private suspend fun applyPendingSiteLocation(timestamp: Long) {

        val selectedAt = preferences.get(TandemLongNonPreferenceKey.PendingSiteSelectedAt)
        if (selectedAt == 0L) return

        if (System.currentTimeMillis() > selectedAt + PENDING_SITE_VALIDITY) {
            aapsLogger.info(TAG, "${historyPrefix}PostProcess - pending site location expired, discarded")
            clearPendingSiteLocation()
            return
        }

        // A history backfill can deliver cannula fills that predate the selection. Those describe
        // earlier site changes, so leave them alone and keep waiting for the matching one. Pump and
        // phone clocks are compared here, hence the tolerance.
        if (timestamp < selectedAt - PENDING_SITE_CLOCK_SKEW) {
            aapsLogger.info(TAG, "${historyPrefix}PostProcess - cannula fill at $timestamp predates site selection, skipped")
            return
        }

        val location = preferences.get(TandemStringNonPreferenceKey.PendingSiteLocation)
            .takeIf { it.isNotEmpty() }
            ?.let { name -> TE.Location.entries.firstOrNull { it.name == name } }
        val arrow = preferences.get(TandemStringNonPreferenceKey.PendingSiteArrow)
            .takeIf { it.isNotEmpty() }
            ?.let { name -> TE.Arrow.entries.firstOrNull { it.name == name } }

        if (location == null && arrow == null) {
            clearPendingSiteLocation()
            return
        }

        val event = persistenceLayer.getTherapyEventDataFromToTime(timestamp, timestamp)
            .firstOrNull { it.type == TE.Type.CANNULA_CHANGE }

        if (event == null) {
            // Keep the selection parked: a later history pass may still deliver the event.
            aapsLogger.warn(TAG, "${historyPrefix}PostProcess - no CANNULA_CHANGE at $timestamp, site location stays pending")
            return
        }

        persistenceLayer.insertOrUpdateTherapyEvent(event.copy(location = location, arrow = arrow))
        aapsLogger.info(TAG, "${historyPrefix}PostProcess - site location $location / arrow $arrow attached to CANNULA_CHANGE")
        clearPendingSiteLocation()
    }

    private fun clearPendingSiteLocation() {
        preferences.put(TandemStringNonPreferenceKey.PendingSiteLocation, "")
        preferences.put(TandemStringNonPreferenceKey.PendingSiteArrow, "")
        preferences.put(TandemLongNonPreferenceKey.PendingSiteSelectedAt, 0L)
    }

}