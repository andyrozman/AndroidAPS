package app.aaps.pump.tandem.common.comm.history

import android.content.Context
import app.aaps.core.data.model.BS
import app.aaps.core.data.model.TE
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
import app.aaps.pump.tandem.common.database.data.entity.TandemSiteChangeEntity
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnector
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import com.jwoglom.pumpx2.pump.messages.response.historyLog.BolusCompletedHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.CannulaFilledHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.CartridgeFilledHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.TubingFilledHistoryLog
import kotlinx.coroutines.runBlocking
import org.junit.platform.commons.util.ReflectionUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class HistoryPostProcessor @Inject constructor(
    val pumpStatus: TandemPumpStatus,
    val aapsLogger: AAPSLogger,
    val pumpSync: PumpSync,
    val persistenceLayer: PersistenceLayer,
    val dbDataHandler: DbDataHandler,
    val tandemPumpUtil: TandemPumpUtil
) {

    var historyPrefix = ""

    companion object {
        val TAG = LTag.PUMPCOMM
    }


    fun setupHistoryPostProcessor(debugHistory: Boolean) {
        historyPrefix = if (debugHistory) "HST: " else ""
    }


    fun postProcessHistory(historyLogs: MutableCollection<HistoryLog>) {

        aapsLogger.error(TAG, "${historyPrefix}PostProcess History (items=${historyLogs.size})")

        for (historyLog in historyLogs) {

            when(historyLog) {
                is CannulaFilledHistoryLog,
                is TubingFilledHistoryLog -> {

                    aapsLogger.error(TAG, "${historyPrefix}PostProcess - NS Cannula Change -> TubingFilledHistoryLog")

                    runBlocking {

                        val listSiteChanges = dbDataHandler.getUnassignedSiteChanges()
                        val timestamp = historyLog.pumpTimeSecInstant.toEpochMilli()
                        val siteChangeRecord = findSiteChangeNearestToHistory(timestamp, listSiteChanges)

                        pumpSync.insertTherapyEventIfNewWithTimestamp(
                            timestamp = timestamp,
                            type = TE.Type.CANNULA_CHANGE,
                            note = null,
                            pumpId = historyLog.sequenceNum,
                            pumpType = pumpStatus.pumpType,
                            pumpSerial = pumpStatus.serialNumber.toString()
                        )

                        if (siteChangeRecord!=null) {

                            aapsLogger.error(TAG, "Site change Record found: $siteChangeRecord")

                            val location = siteChangeRecord.siteLocation?.let {
                                runCatching { TE.Location.valueOf(it) }.getOrNull()
                            }

                            val arrow = siteChangeRecord.siteArrow?.let {
                                runCatching { TE.Arrow.valueOf(it) }.getOrNull()
                            }

                            val therapyEventDataFromTimeList = persistenceLayer.getTherapyEventDataFromTime(
                                timestamp = timestamp,
                                type = TE.Type.CANNULA_CHANGE,
                                ascending = true
                            )

                            val te = therapyEventDataFromTimeList[0]

                            aapsLogger.error(TAG, "Found Theraphy Event and updating it: $te")

                            persistenceLayer.insertOrUpdateTherapyEvent(te.copy(location = location, arrow = arrow))

                            dbDataHandler.updateSiteChangeWithStoredTrue(siteChangeRecord)

                        }
                    }
                }
                is CartridgeFilledHistoryLog -> {

                    aapsLogger.error(TAG, "${historyPrefix}PostProcess - NS Insulin Change -> CartridgeFilledHistoryLog")

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

                        aapsLogger.error(TAG, "${historyPrefix}PostProcess - Bolus - ${historyLog}")

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

                else -> {
                    aapsLogger.error(TAG, "${historyPrefix}Ignored Entry ${historyLog}")
                }

            }
        }

    }


    fun findSiteChangeNearestToHistory(targetTime: Long, items: List<TandemSiteChangeEntity>?): TandemSiteChangeEntity? {
        if (items!=null && items.size>0) {
            val item = items
                .minByOrNull { abs(it.dateTime - targetTime) }
                ?.takeIf {
                    abs(it.dateTime - targetTime) <= 30_000L
                }

            return item
        }
        return null;
    }



}