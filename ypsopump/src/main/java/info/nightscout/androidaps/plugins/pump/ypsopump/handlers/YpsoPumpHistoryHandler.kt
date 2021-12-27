package info.nightscout.androidaps.plugins.pump.ypsopump.handlers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.common.defs.PumpRunningState
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.utils.DateTimeUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.GetAlarms
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.GetEvents
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.GetLastEvent
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoPumpNotificationType
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.LastEntryType
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.YpsoPumpStatusEntry
import info.nightscout.androidaps.plugins.pump.ypsopump.data.*
import info.nightscout.androidaps.plugins.pump.ypsopump.database.HistoryMapper
import info.nightscout.androidaps.plugins.pump.ypsopump.database.HistoryRecordEntity
import info.nightscout.androidaps.plugins.pump.ypsopump.database.YpsoPumpHistory
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YpsoPumpHistoryHandler @Inject constructor(var ypsoPumpHistory: YpsoPumpHistory,
                                                 var pumpSync: PumpSync,
                                                 var aapsLogger: AAPSLogger,
                                                 var pumpStatus: YpsopumpPumpStatus,
                                                 val historyMapper: HistoryMapper,
                                                 var hasAndroidInjector: HasAndroidInjector,
                                                 var pumpUtil: YpsoPumpUtil,
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

    var gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun getPumpHistory() {

        Thread {

            val pumpStatusValues = pumpStatus.getPumpStatusValuesForSelectedPump()

            if (pumpStatusValues == null) {
                aapsLogger.debug(LTag.PUMP, "PumpStatusValues could not be loaded, skipping history reading.")
                return@Thread
            }

            // if (true)
            //     return@Thread

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

            aapsLogger.warn(LTag.PUMPCOMM, "DD: GetEvents")

            val commandEvents = GetEvents(
                hasAndroidInjector = hasAndroidInjector,
                eventSequenceNumber = if (targetSequenceNumber == 0) null else targetSequenceNumber,
                includeEventSequence = true
            )

            val result = commandEvents.execute(ypsoPumpBLE)

            aapsLogger.warn(LTag.PUMPCOMM, "DD: commandEvents.execute ${result}")

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

                    pumpStatusValues.lastEventSequenceNumber = newLastEvent
                    pumpStatusValues.runningEventsSequences = runningSet

                    aapsLogger.warn(LTag.PUMPCOMM, "DD: Data Events size=${dataEvents.size}")
                    //aapsLogger.warn(LTag.PUMPCOMM, "DD: Data Events " + gson.toJson(dataEvents))

                    val events = preProcessHistory(dataEvents)

                    aapsLogger.warn(LTag.PUMPCOMM, "DD: Preprocessed Events size=${events.size}")
                    //aapsLogger.warn(LTag.PUMPCOMM, "DD: Events, after preprocess: " + gson.toJson(events))

                    // // TODO
                    // if (true)
                    //     return@Thread

                    addHistoryToDatabase(
                        entityList = events,
                        sendToPumpSync = true,
                        pumpStatusEntry = pumpStatusValues
                    )

                }

            } else {
                return@Thread  // if any of commands fails we stop execution
            }

            // TODO
            if (true)
                return@Thread

            // alarms

            val commandAlarms = GetAlarms(
                hasAndroidInjector = hasAndroidInjector,
                eventSequenceNumber = if (pumpStatusValues.lastAlarmSequenceNumber == 0) null else pumpStatusValues.lastAlarmSequenceNumber
            )

            val resultAlarms = commandAlarms.execute(ypsoPumpBLE)

            if (resultAlarms) {
                val dataEvents = commandAlarms.commandResponse
                var newLastEvent = 0

                if (dataEvents != null && dataEvents.size != 0) {
                    for (dataEvent in dataEvents) {
                        if (dataEvent.eventSequenceNumber > newLastEvent) {
                            newLastEvent = dataEvent.eventSequenceNumber
                        }
                    }

                    pumpStatusValues.lastAlarmSequenceNumber = newLastEvent

                    addHistoryToDatabase(entityList = dataEvents,
                        sendToPumpSync = false,
                        pumpStatusEntry = pumpStatusValues)
                }
            } else {
                return@Thread  // if any of commands fails we stop execution
            }

            // TODO systemEntry we are ignoring this for now

            pumpStatus.setPumpStatusValues(pumpStatusValues)

        }.start()
    }

    private fun preProcessHistory(dataEventsInput: MutableList<EventDto>): MutableList<EventDto> {

        // TODO check if configuration changed and update it
        // TODO basal profile changed put together...
        // TODO preprocess TBRs for STOP/START of pump

        var dataEvents = preProcessBasalProfiles(dataEventsInput)
        dataEvents = preProcessConfigurationItems(dataEvents)
        dataEvents = preProcessDeliverySuspendItems(dataEvents)

        // if (medtronicHistoryData.hasRelevantConfigurationChanged()) {
        //     scheduleNextRefresh(MedtronicStatusRefreshType.Configuration, -1)
        // }
        // if (medtronicHistoryData.hasPumpTimeChanged()) {
        //     scheduleNextRefresh(MedtronicStatusRefreshType.PumpTime, -1)
        // }
        // if (medtronicPumpStatus.basalProfileStatus !== BasalProfileStatus.NotInitialized
        //     && medtronicHistoryData.hasBasalProfileChanged()) {
        //     medtronicHistoryData.processLastBasalProfileChange(pumpDescription.pumpType, medtronicPumpStatus)
        // }
        // val previousState = pumpState
        // if (medtronicHistoryData.isPumpSuspended()) {
        //     pumpState = PumpDriverState.Suspended
        //     aapsLogger.debug(LTag.PUMP, logPrefix + "isPumpSuspended: true")
        // } else {
        //     if (previousState === PumpDriverState.Suspended) {
        //         pumpState = PumpDriverState.Ready
        //     }
        //     aapsLogger.debug(LTag.PUMP, logPrefix + "isPumpSuspended: false")
        // }

        // TODO 'Delivery Suspend'
        // val suspends: MutableList<TempBasalProcessDTO>
        // suspends = try {
        //     getSuspendRecords()
        // } catch (ex: Exception) {
        //     aapsLogger.error("ProcessHistoryData: Error getting Suspend entries: " + ex.message, ex)
        //     throw ex
        // }
        // aapsLogger.debug(LTag.PUMP, String.format(Locale.ENGLISH, "ProcessHistoryData: 'Delivery Suspend' Processed [count=%d, items=%s]", suspends.size,
        //     gson.toJson(suspends)))
        // if (suspends.isNotEmpty()) {
        //     try {
        //         processSuspends(suspends)  // TODO not tested yet
        //     } catch (ex: Exception) {
        //         aapsLogger.error(LTag.PUMP, "ProcessHistoryData: Error processing Suspends entries: " + ex.message, ex)
        //         throw ex
        //     }
        // }

        return dataEvents

    }

    private fun preProcessBasalProfiles(dataEvents: MutableList<EventDto>): MutableList<EventDto> {
        // TODO
        //val dataEvents = dataEventsInput

        aapsLogger.debug(LTag.PUMPCOMM, "Full Events before: ${dataEvents.size}")

        var listBasalProfiles = dataEvents.stream()
            .filter { pre ->
                pre.entryType == YpsoPumpEventType.BASAL_PROFILE_A_PATTERN_CHANGED ||
                    pre.entryType == YpsoPumpEventType.BASAL_PROFILE_B_PATTERN_CHANGED
            }
            .collect(Collectors.toList())

        aapsLogger.debug(LTag.PUMPCOMM, "Basal Items found: ${listBasalProfiles.size}")

        //var basals: MutableMap<Long, MutableList<EventDto>> = mutableMapOf()

        var basalProfilesNew: MutableList<EventDto> = mutableListOf()

        var listFirstItems: MutableList<Int> = mutableListOf()

        for (basalProfile in listBasalProfiles) {

            var basalProfilePattern = basalProfile.subObject as BasalProfileEntry

            if (basalProfilePattern.hour == 0) {
                listFirstItems.add(basalProfile.eventSequenceNumber)
            }
        }

        aapsLogger.debug(LTag.PUMPCOMM, "Basal first Items found: ${listFirstItems.size}")

        //var listProfiles: MutableList<BasalProfilePatternDto> = mutableListOf()

        for (firstItem in listFirstItems) {

            val profile = BasalProfilePatternDto(firstItem, firstItem + 24)

            for (basalProfile in listBasalProfiles) {
                if (basalProfile.eventSequenceNumber >= profile.firstIndex &&
                    basalProfile.eventSequenceNumber <= profile.lastIndex) {
                    profile.mapOfProfilePattens[basalProfile.value1] = basalProfile
                }
            }

            aapsLogger.debug(LTag.PUMPCOMM, "Basal Profile Items found: ${profile.mapOfProfilePattens.size}")

            if (profile.mapOfProfilePattens.size == 24) {
                val lastPattern = profile.mapOfProfilePattens[23]!!

                val isA = if (lastPattern.entryType == YpsoPumpEventType.BASAL_PROFILE_A_PATTERN_CHANGED) true else false

                val newProfile = EventDto(
                    id = lastPattern.eventSequenceNumber,
                    serial = lastPattern.serial,
                    historyEntryType = lastPattern.historyEntryType,
                    dateTime = lastPattern.dateTime,
                    entryType = if (isA) YpsoPumpEventType.BASAL_PROFILE_A_CHANGED else YpsoPumpEventType.BASAL_PROFILE_B_CHANGED,
                    entryTypeAsInt = if (isA) 4000 else 4001,
                    value1 = lastPattern.value1,
                    value2 = lastPattern.value2,
                    value3 = lastPattern.value3,
                    eventSequenceNumber = lastPattern.eventSequenceNumber,
                    subObject = null,
                    subObject2 = null,
                    created = System.currentTimeMillis(),
                    updated = System.currentTimeMillis()
                )

                val profileSub = BasalProfile(hashMapOf())

                for (i in 0 until 23) {
                    val entry = profile.mapOfProfilePattens[i]!!.subObject as BasalProfileEntry
                    profileSub.profile[i] = entry
                }

                newProfile.subObject = profileSub

                basalProfilesNew.add(newProfile)

            } else {
                aapsLogger.warn(LTag.PUMPCOMM, "Basal Profile incomplete, invalid. Ignoring.")
            }
        }

        //aapsLogger.debug(LTag.PUMPCOMM, "Basal first Items found: ${listFirstItems.size}")

        aapsLogger.debug(LTag.PUMPCOMM, "Basal Profiles found: ${basalProfilesNew.size}")

        dataEvents.removeAll(listBasalProfiles)

        aapsLogger.debug(LTag.PUMPCOMM, "Full Events after remove: ${dataEvents.size}")

        dataEvents.addAll(basalProfilesNew)

        aapsLogger.debug(LTag.PUMPCOMM, "Full Events after addng new profiles: ${dataEvents.size}")

        // 1. we check if basal has changed, if yes, we need:
        //   a.) combine records into one
        //   b.) remove old records
        //   c.) apply records to the driver

        return dataEvents
    }

    private fun preProcessConfigurationItems(dataEvents: MutableList<EventDto>): MutableList<EventDto> {
        //var dataEventsOut = preProcessBasalProfiles(dataEvents)
        // 2. we try to find all other configuration changes and apply them to the system

        val update = pumpStatus.lastConfigurationUpdate

        val listConfiguration = dataEvents.stream()
            .filter { pre ->
                (pre.entryType == YpsoPumpEventType.BOLUS_STEP_CHANGED ||
                    pre.entryType == YpsoPumpEventType.BASAL_PROFILE_SWITCHED ||
                    pre.entryType == YpsoPumpEventType.BOLUS_AMOUNT_CAP_CHANGED ||
                    pre.entryType == YpsoPumpEventType.BASAL_RATE_CAP_CHANGED)
            }
            .collect(Collectors.toList())

        var newUpdateDate = 0L

        for (eventDto in listConfiguration) {

            if (eventDto.dateTime.toATechDate() > update) {
                when (eventDto.entryType) {
                    YpsoPumpEventType.BOLUS_STEP_CHANGED       -> {
                        pumpStatus.bolusStep = eventDto.value1 / 100.0
                        pumpStatus.pumpDescription.bolusStep = pumpStatus.bolusStep
                    }

                    YpsoPumpEventType.BASAL_PROFILE_SWITCHED   -> {
                        val basalProfileConfig = eventDto.subObject as ConfigurationChanged
                        pumpStatus.activeProfileName = basalProfileConfig.value
                        if (!"A".equals(basalProfileConfig.value)) {
                            pumpUtil.sendNotification(
                                notificationType = YpsoPumpNotificationType.PumpIncorrectBasalProfileSelected,
                                "A")
                        }
                    }

                    YpsoPumpEventType.BOLUS_AMOUNT_CAP_CHANGED -> {
                        pumpStatus.maxBolus = eventDto.value1 / 100.0
                        //pumpStatus.pumpDescription.bolusM
                    }

                    YpsoPumpEventType.BASAL_RATE_CAP_CHANGED   -> {
                        pumpStatus.maxBasal = eventDto.value1 / 100.0
                        pumpStatus.pumpDescription.basalMaximumRate = pumpStatus.maxBasal!!
                    }

                    else                                       -> {
                    }
                } // when

                if (newUpdateDate < eventDto.dateTime.toATechDate()) {
                    newUpdateDate = eventDto.dateTime.toATechDate()
                }

            } // if after last update

        }

        if (newUpdateDate > 0L) {
            pumpStatus.lastConfigurationUpdate = newUpdateDate
            pumpStatus.configChanged = true
        }



        return dataEvents
    }

    private fun preProcessDeliverySuspendItems(dataEvents: MutableList<EventDto>): MutableList<EventDto> {
        // TODO

        var listDelivery = dataEvents.stream()
            .filter { pre ->
                pre.entryType == YpsoPumpEventType.DELIVERY_STATUS_CHANGED ||
                    pre.entryType == YpsoPumpEventType.PUMP_MODE_CHANGED
            }
            .collect(Collectors.toList())

        // 1. find any delivery suspend items
        // 2. apply it to the system
        // 3. create TBR records for suspend (retrive data from db if necessarry)

        return dataEvents
    }

    fun getLastEventAndSendItToPumpSync(bolusInfo: DetailedBolusInfo? = null,
                                        tempBasalInfo: TempBasalPair? = null,
                                        profile: Profile? = null) {
        Thread {

            // events
            val commandEvents = GetLastEvent(
                hasAndroidInjector = hasAndroidInjector,
                bolusInfo = bolusInfo,
                tempBasalInfo = tempBasalInfo)

            val result = commandEvents.execute(ypsoPumpBLE)

            if (result) {
                val dataEvents = commandEvents.commandResponse

                if (dataEvents != null && dataEvents.size != 0) {

                    aapsLogger.debug(LTag.PUMP, "Got ${dataEvents.size} items.")

                    if (dataEvents.size == 1) {
                        val eventDto = dataEvents[0]

                        aapsLogger.debug(LTag.PUMP, "Event: " + pumpUtil.gson.toJson(eventDto))

                        if (bolusInfo != null && bolusInfo.bolusType != DetailedBolusInfo.BolusType.NORMAL) {
                            val bolus = eventDto.subObject as Bolus
                            bolus.bolusType = if (bolusInfo.bolusType == DetailedBolusInfo.BolusType.SMB) BolusType.SMB else BolusType.Priming
                            eventDto.subObject = bolus
                        }

                        val entity = historyMapper.domainToEntity(eventDto)

                        sendDataToPumpSync(entity, null)

                    } else {
                        aapsLogger.warn(LTag.PUMP, "There should be only one response returned (${dataEvents.size}).")
                    }

                } else {
                    aapsLogger.warn(LTag.PUMP, "No events found.")
                    return@Thread
                }

            } else {
                aapsLogger.warn(LTag.PUMP, "Problem reading: ${commandEvents.bleCommOperationResult!!.operationResultType}")
                return@Thread  // if any of commands fails we stop execution
            }

        }.start()
    }

    fun addHistoryToDatabase(entityList: List<EventDto>,
                             pumpStatusEntry: YpsoPumpStatusEntry? = null,
                             sendToPumpSync: Boolean = false) {
        var first = true
        var counter: Int = 0
        for (historyRecordEntity in entityList) {
            if (first) {
                // for bolus type
                first = false
            }

            aapsLogger.debug(LTag.PUMP, "DD: Add history to Database: ${counter}\n ${gson.toJson(historyRecordEntity)}")

            val changedItem = ypsoPumpHistory.insertOrUpdate(event = historyRecordEntity)

            if (sendToPumpSync && changedItem != null) {
                sendDataToPumpSync(changedItem, pumpStatusEntry)
            }
        }
    }

    private fun sendDataToPumpSync(entity: HistoryRecordEntity, pumpStatusEntry: YpsoPumpStatusEntry?) {
        Thread {
            val date = DateTimeUtil.toMillisFromATD(entity.date)

            if (entity.bolusRecord != null) {
                val bolusRecord = entity.bolusRecord!!
                if (bolusRecord.bolusType == BolusType.Normal ||
                    bolusRecord.bolusType == BolusType.SMB ||
                    bolusRecord.bolusType == BolusType.Priming) {

                    val bolusType = when (bolusRecord.bolusType) {
                        BolusType.Normal  -> DetailedBolusInfo.BolusType.NORMAL
                        BolusType.SMB     -> DetailedBolusInfo.BolusType.SMB
                        BolusType.Priming -> DetailedBolusInfo.BolusType.PRIMING
                        else              -> DetailedBolusInfo.BolusType.NORMAL
                    }

                    val result = pumpSync.syncBolusWithPumpId(
                        timestamp = date,
                        amount = bolusRecord.immediateAmount!!,
                        type = bolusType,
                        pumpId = entity.eventSequenceNumber.toLong(),
                        pumpType = PumpType.YPSOPUMP,
                        pumpSerial = entity.serial.toString())
                    val insulin = String.format(Locale.ROOT, "%.2f", bolusRecord.immediateAmount!!)
                    aapsLogger.debug(LTag.PUMP, "syncBolusWithPumpId [date=$date, " +
                        "type=${bolusType.name}, pumpId=${entity.eventSequenceNumber}, " +
                        "insulin=$insulin, " +
                        "pumpSerial=${entity.serial}] - Result: $result")
                } else if (bolusRecord.bolusType == BolusType.Extended) {
                    val result = pumpSync.syncExtendedBolusWithPumpId(
                        timestamp = date,
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
                        timestamp = date,
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
                        timestamp = date,
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
                val result = pumpSync.syncTemporaryBasalWithPumpId(
                    timestamp = DateTimeUtil.toMillisFromATD(entity.date),
                    rate = tbrRecord.percent.toDouble(),
                    duration = tbrRecord.minutes * 60L * 1000L,
                    isAbsolute = false,
                    type = tbrRecord.temporaryBasalType,
                    pumpId = entity.eventSequenceNumber.toLong(),
                    pumpType = PumpType.YPSOPUMP,
                    pumpSerial = entity.serial.toString())
                aapsLogger.debug(LTag.PUMP, "syncTemporaryBasalWithPumpId [date=$date, " +
                    "pumpId=${entity.eventSequenceNumber}, rate=${tbrRecord.percent} %%, " +
                    "duration=${tbrRecord.minutes} min, pumpSerial=${entity.serial}] - Result: $result")
            } else if (entity.tddRecord != null) {
                val tdiRecord = entity.tddRecord!!
                val result = pumpSync.createOrUpdateTotalDailyDose(
                    timestamp = date,
                    bolusAmount = tdiRecord.bolus,
                    basalAmount = tdiRecord.basal,
                    totalAmount = tdiRecord.total,
                    pumpId = entity.eventSequenceNumber.toLong(),
                    pumpType = PumpType.YPSOPUMP,
                    pumpSerial = entity.serial.toString())
                aapsLogger.debug(LTag.PUMP, "createOrUpdateTotalDailyDose [date=$date, " +
                    "pumpId=${entity.eventSequenceNumber}, bolus=${tdiRecord.bolus}, " +
                    "basal=${tdiRecord.basal} , total=${tdiRecord.total}, pumpSerial=${entity.serial}] - Result: $result")
            } else if (entity.entryType == YpsoPumpEventType.REWIND) {
                // Rewind (for marking insulin change)
                uploadCareportalEventIfFoundInHistory(
                    historyRecord = entity,
                    lastEntryType = LastEntryType.Rewind,
                    eventType = DetailedBolusInfo.EventType.INSULIN_CHANGE,
                    pumpStatusEntry = pumpStatusEntry!!)
            } else if (entity.entryType == YpsoPumpEventType.PRIMING) {
                // Prime (for resetting autosense)
                uploadCareportalEventIfFoundInHistory(
                    historyRecord = entity,
                    lastEntryType = LastEntryType.Prime,
                    eventType = DetailedBolusInfo.EventType.CANNULA_CHANGE,
                    pumpStatusEntry = pumpStatusEntry!!)
            }
        }.start()

    }

    private fun uploadCareportalEventIfFoundInHistory(historyRecord: HistoryRecordEntity,
                                                      lastEntryType: LastEntryType,
                                                      eventType: DetailedBolusInfo.EventType,
                                                      pumpStatusEntry: YpsoPumpStatusEntry) {
        val lastEntry = pumpStatusEntry.getLastEntry(lastEntryType)

        if (lastEntry == null || historyRecord.eventSequenceNumber > lastEntry) {
            val result = pumpSync.insertTherapyEventIfNewWithTimestamp(
                DateTimeUtil.toMillisFromATD(historyRecord.date),
                eventType, null,
                historyRecord.eventSequenceNumber.toLong(),
                PumpType.YPSOPUMP,
                pumpStatus.serialNumber.toString())

            aapsLogger.debug(LTag.PUMP, "insertTherapyEventIfNewWithTimestamp [date=${historyRecord.date}, " +
                "eventType=${eventType}, pumpId=${historyRecord.eventSequenceNumber}, " +
                "pumpSerial=${pumpStatus.serialNumber}] - Result: ${result}")

            pumpStatusEntry.setLastEntry(lastEntryType, historyRecord.eventSequenceNumber)
        }
    }

    fun readCurrentStatusOfPump() {
        val latestDeliveryStatusChangedEntry = ypsoPumpHistory.getLatestDeliveryStatusChangedEntry()

        if (latestDeliveryStatusChangedEntry != null) {
            val pumpStatusValues = pumpStatus.getPumpStatusValuesForSelectedPump()
            val isRunning = (latestDeliveryStatusChangedEntry.pumpStatusRecord!!.pumpStatusType == PumpStatusType.PumpRunning)
            pumpStatusValues!!.isPumpRunning = isRunning
            pumpStatus.setPumpStatusValues(pumpStatusValues)
            pumpStatus.pumpRunningState = if (isRunning) PumpRunningState.Running else PumpRunningState.Suspended
        }
    }

}