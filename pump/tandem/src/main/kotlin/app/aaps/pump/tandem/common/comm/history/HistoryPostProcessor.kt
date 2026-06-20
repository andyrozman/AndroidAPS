package app.aaps.pump.tandem.common.comm.history

import android.content.Context
import app.aaps.core.data.model.BS
import app.aaps.core.data.model.TE
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.pump.PumpInsulin
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.utils.DateTimeUtil
import app.aaps.pump.tandem.common.concurrency.TandemDispatcher
import app.aaps.pump.tandem.common.database.data.DbDataHandler
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnector
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import com.jwoglom.pumpx2.pump.messages.response.historyLog.BolusCompletedHistoryLog
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
    val tandemPumpUtil: TandemPumpUtil
) {

    companion object {
        val TAG = LTag.PUMPCOMM
    }

    fun postProcessHistory(historyLogs: MutableCollection<HistoryLog>) {

        aapsLogger.error(TAG, "HST: PostProcess History (items=${historyLogs.size})")

        for (historyLog in historyLogs) {

            when(historyLog) {

                is TubingFilledHistoryLog -> {

                    aapsLogger.error(TAG, "HST: PostProcess - NS Cannula Change")

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

                    aapsLogger.error(TAG, "HST: PostProcess - NS Insulin Change")

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

                        aapsLogger.error(TAG, "HST: PostProcess - Bolus - ${historyLog}")

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

}