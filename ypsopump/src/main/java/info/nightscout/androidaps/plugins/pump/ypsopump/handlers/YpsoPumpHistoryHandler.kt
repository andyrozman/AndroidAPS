package info.nightscout.androidaps.plugins.pump.ypsopump.handlers

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType
import info.nightscout.androidaps.plugins.pump.common.utils.DateTimeUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.GetEvents
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.BolusType
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.EventDto
import info.nightscout.androidaps.plugins.pump.ypsopump.database.HistoryRecordEntity
import info.nightscout.androidaps.plugins.pump.ypsopump.database.YpsoPumpHistory
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YpsoPumpHistoryHandler @Inject constructor(var ypsoPumpHistory: YpsoPumpHistory,
                                                 var pumpSync: PumpSync,
                                                 var aapsLogger: AAPSLogger,
                                                 var pumpStatus: YpsopumpPumpStatus,
                                                 var hasAndroidInjector: HasAndroidInjector,
                                                 var ypsoPumpBLE: YpsoPumpBLE) {

    /**
     * On First read
     *  - we need to determine the status of pump, so we need to get PUMP_MODE_CHANGED
     *  - read from last record and all SequenceNumbers that are running
     *
     * On Every 5 minutes read:
     *  - read all new records, and any records that were running in last read
     *  - we process new entries, and make sure if we get PUMP_MODE_CHANGED
     *
     * After Action read:
     *  - read new entries and send them to pumpSync
     *  - reset 5 minutes read
     */

    fun getPumpHistory() {

        Thread {

            val pumpStatusValues = pumpStatus.getPumpStatusValuesForSelectedPump()

            // TODO runningEventSequences

            if (pumpStatusValues == null) {
                aapsLogger.debug(LTag.PUMP, "PumpStatusValues could not be loaded, skipping history reading.")
                return@Thread
            }

            if (true)
                return@Thread

            // events
            var lastSequenceNumber: Int? = pumpStatusValues.lastEventSequenceNumber
            var targetSequenceNumber: Int? = lastSequenceNumber

            if (lastSequenceNumber == null) { // this would be null only on first time
                lastSequenceNumber = 0
                targetSequenceNumber = 0
            }

            var runningSequences = pumpStatusValues.runningEventsSequences

            if (runningSequences == null) {
                pumpStatusValues.runningEventsSequences = mutableSetOf()
            } else {
                if (lastSequenceNumber != 0) {
                    for (sequence in runningSequences) {
                        if (targetSequenceNumber!! > sequence) {
                            targetSequenceNumber = sequence
                        }
                    }
                }
            }

            val commandEvents = GetEvents(
                hasAndroidInjector = hasAndroidInjector,
                eventSequenceNumber = if (targetSequenceNumber == 0) null else targetSequenceNumber,
                includeEventSequence = true)

            val result = commandEvents.execute(ypsoPumpBLE)

            if (result) {
                val dataEvents = commandEvents.commandResponse
                var newLastEvent = 0
                var runningSet: MutableSet<Int> = mutableSetOf()

                if (dataEvents != null && dataEvents.size != 0) {
                    for (dataEvent in dataEvents) {
                        if (dataEvent.eventSequenceNumber > newLastEvent) {
                            newLastEvent = dataEvent.eventSequenceNumber
                        }

                        if (YpsoPumpEventType.isRunningEvent(dataEvent.entryType)) {
                            runningSet.add(dataEvent.eventSequenceNumber)
                        }
                    }

                    pumpStatusValues.runningEventsSequences = runningSet

                    processNewHistoryList(dataEvents)

                }

                // TODO 1. process returned data  2. add it into database 3. update pumpStatusValues
            } else {
                return@Thread  // if any of commands fails we stop execution
            }

            // alarms

            // TODO systemEntry we are ignoring this for now

            pumpStatus.setPumpStatusValues(pumpStatusValues)

        }.start()
    }

    fun getPumpHistoryAfterAction() {
        //
    }

    fun processNewHistoryList(entityList: List<EventDto>, sendToPumpSync: Boolean = false) {
        var first = true
        for (historyRecordEntity in entityList) {
            ypsoPumpHistory.insertOrUpdate(entity = historyRecordEntity)
            insertOrUpdate(historyRecordEntity)
        }
    }

    fun processList(entityList: List<HistoryRecordEntity>) {
        var first = true
        for (historyRecordEntity in entityList) {
            ypsoPumpHistory.insertOrUpdate(entity = historyRecordEntity)
            insertOrUpdate(historyRecordEntity)
        }
    }

    private fun sendDataToPumpSync(entity: HistoryRecordEntity) {
        // TODO logging
        Thread {
            val date = DateTimeUtil.toMillisFromATD(entity.date)

            if (entity.bolusRecord != null) {
                val bolusRecord = entity.bolusRecord
                if (bolusRecord!!.bolusType == BolusType.Normal) {
                    // TODO SMB support
                    val result = pumpSync.syncBolusWithPumpId(
                        timestamp = date,
                        amount = bolusRecord.immediateAmount!!,
                        type = DetailedBolusInfo.BolusType.NORMAL,
                        pumpId = entity.eventSequenceNumber.toLong(),
                        pumpType = PumpType.YPSOPUMP,
                        pumpSerial = entity.serial.toString())
                    val insulin = String.format(Locale.ROOT, "%.2f", bolusRecord.immediateAmount!!)
                    aapsLogger.debug(LTag.PUMP, "syncBolusWithPumpId [date=$date, " +
                        "type=NORMAL, pumpId=${entity.eventSequenceNumber}, " +
                        "insulin=$insulin, " +
                        "pumpSerial=${entity.serial}] - Result: $result")
                } else if (bolusRecord.bolusType == BolusType.Extended) {
                    val result = pumpSync.syncExtendedBolusWithPumpId(
                        timestamp = DateTimeUtil.toMillisFromATD(entity.date),
                        amount = bolusRecord.extendedAmount!!,
                        duration = (bolusRecord.durationMin!! * 60L * 1000L),
                        isEmulatingTB = false,
                        pumpId = entity.eventSequenceNumber.toLong(),
                        pumpType = PumpType.YPSOPUMP,
                        pumpSerial = entity.serial.toString())
                    val insulin = String.format(Locale.ROOT, "%.2f", bolusRecord.extendedAmount!!)
                    aapsLogger.debug(LTag.PUMP, "syncExtendedBolusWithPumpId [date=$date, " +
                        "type=EXTENDED, pumpId=${entity.eventSequenceNumber}, " +
                        "insulin=$insulin, " +
                        "pumpSerial=${entity.serial}] - Result: $result")
                } else {
                    val result = pumpSync.syncBolusWithPumpId(
                        timestamp = DateTimeUtil.toMillisFromATD(entity.date),
                        amount = bolusRecord.immediateAmount!!,
                        type = DetailedBolusInfo.BolusType.NORMAL,
                        pumpId = entity.eventSequenceNumber.toLong(),
                        pumpType = PumpType.YPSOPUMP,
                        pumpSerial = entity.serial.toString())
                    val insulinNo = String.format(Locale.ROOT, "%.2f", bolusRecord.immediateAmount!!)
                    aapsLogger.debug(LTag.PUMP, "syncBolusWithPumpId [date=$date, " +
                        "type=NORMAL, pumpId=${entity.eventSequenceNumber}, " +
                        "insulin=$insulinNo, " +
                        "pumpSerial=${entity.serial}] - Result: $result")
                    val result2 = pumpSync.syncExtendedBolusWithPumpId(
                        timestamp = DateTimeUtil.toMillisFromATD(entity.date),
                        amount = bolusRecord.extendedAmount!!,
                        duration = (bolusRecord.durationMin!! * 60L * 1000L),
                        isEmulatingTB = false,
                        pumpId = entity.eventSequenceNumber.toLong(),
                        pumpType = PumpType.YPSOPUMP,
                        pumpSerial = entity.serial.toString())
                    val insulinEx = String.format(Locale.ROOT, "%.2f", bolusRecord.extendedAmount!!)
                    aapsLogger.debug(LTag.PUMP, "syncExtendedBolusWithPumpId [date=$date, " +
                        "type=EXTENDED, pumpId=${entity.eventSequenceNumber}, " +
                        "insulin=$insulinEx, " +
                        "pumpSerial=${entity.serial}] - Result: $result2")
                }
            } else if (entity.temporaryBasalRecord != null) {
                val tbrRecord = entity.temporaryBasalRecord!!
                pumpSync.syncTemporaryBasalWithPumpId(
                    timestamp = DateTimeUtil.toMillisFromATD(entity.date),
                    rate = tbrRecord.percent.toDouble(),
                    duration = tbrRecord.minutes * 60L * 1000L,
                    isAbsolute = false,
                    type = PumpSync.TemporaryBasalType.NORMAL,
                    pumpId = entity.eventSequenceNumber.toLong(),
                    pumpType = PumpType.YPSOPUMP,
                    pumpSerial = entity.serial.toString())
            } else if (entity.tdiRecord != null) {
                val tdiRecord = entity.tdiRecord!!
                pumpSync.createOrUpdateTotalDailyDose(
                    timestamp = DateTimeUtil.toMillisFromATD(entity.date),
                    bolusAmount = tdiRecord.bolus,
                    basalAmount = tdiRecord.basal,
                    totalAmount = tdiRecord.total,
                    pumpId = entity.eventSequenceNumber.toLong(),
                    pumpType = PumpType.YPSOPUMP,
                    pumpSerial = entity.serial.toString())
            }
        }.start()

    }

    fun readCurrentStatusOfPump() {
        TODO("Not yet implemented")
    }

}