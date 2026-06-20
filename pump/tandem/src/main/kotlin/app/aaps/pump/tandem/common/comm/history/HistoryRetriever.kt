package app.aaps.pump.tandem.common.comm.history

import android.content.Context
import android.icu.util.GregorianCalendar
import androidx.annotation.VisibleForTesting
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.pump.common.defs.PumpDriverState
import app.aaps.pump.common.defs.PumpUpdateFragmentType
import app.aaps.pump.common.driver.connector.defs.PumpCommandType
import app.aaps.pump.common.events.EventPumpFragmentValuesChanged
import app.aaps.pump.tandem.common.comm.ui.TandemUICommunication
import app.aaps.pump.tandem.common.concurrency.TandemDispatcher
import app.aaps.pump.tandem.common.data.history.HistoryRange
import app.aaps.pump.tandem.common.data.history.HistoryRequestInfo
import app.aaps.pump.tandem.common.data.history.HistorySummaryDto
import app.aaps.pump.tandem.common.database.data.DbDataHandler
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnector
import app.aaps.pump.tandem.common.driver.tandemUiDataStore
import app.aaps.pump.tandem.common.keys.TandemLongNonPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringNonPreferenceKey
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import app.aaps.pump.tandem.mobi.TandemMobiPluginVersion
import com.jwoglom.pumpx2.pump.messages.helpers.Dates
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.HistoryLogRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.HistoryLogStatusRequest
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HistoryLogResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HistoryLogStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLogStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.historyLog.UnknownHistoryLog
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.mutableListOf

/*
    How this works:
    FIRST READ
    - on first read we get min, max (HistoryLogStatusRequest) and create HistorySummary and save it
    - we start retreiving data: we will retrieve 5 x 200 = 1000 records per reading
                                            (reading will be done together with status every 5 minutes)
    - once reading is done we create HistoryRange and add it to missed range

    MAIN RULE
    - when we get data that surpasses 44 days we modify missedRanges and modifiedStartRecord

    SUBSEQUENT READS
    - read min, max (HistoryLogStatusRequest) and check if new records there
    - NO: read missedRanges and read next 1000 items
    - YES: read new data, and remaining range is used to read old data, if there is more than
            1000 new records add new missedRange or modify the current one

    - missedRanges need to be updated regularly...

 */

@Singleton
class HistoryRetriever @Inject constructor(
    val pumpStatus: TandemPumpStatus,
    val tandemPumpConnector: TandemPumpConnector,
    val aapsLogger: AAPSLogger,
    val pumpUtil: TandemPumpUtil,
    val preferences: Preferences,
    val rxBus: RxBus,
    var context: Context,
    val dbDataHandler: DbDataHandler,
    val uiInteraction: UiInteraction,
    val notificationManager: NotificationManager,
    val tandemDispatcher: TandemDispatcher,
    val historyPostProcessor: HistoryPostProcessor
) {

    var historyPrefix = ""


    companion object  {
        const val RECORDS_RETRIEVAL_AMOUNT = 1000  // how many record we retrieve in one go
        const val CHUNK_SIZE = 200 // how many records on each call
        const val HISTORY_LIMIT_IN_DAYS = 44 // how many day of history we are getting
        const val SHORT_RECORDS_RETRIEVAL_AMOUNT = 20 // for short readings we get last x entries only
        const val WATCHDOG_TIMEOUT_MS = 30_000L // abort if no history progress/messages for this long
        val TAG = LTag.PUMPCOMM
        const val DEBUG_HISTORY = true
    }

    private var maxDateTimeInSec: Int = 0
    // Read on the TandemPumpOpQueue thread (busy-wait / result read), written on the BLE callback
    // thread (response handlers). @Volatile gives the busy-wait visibility of completion and
    // safe-publishes the collections below, which are otherwise confined to the response handlers.
    @Volatile private var historySummaryDto : HistorySummaryDto? = null
    @Volatile private var currentRequest: HistoryRequestInfo?  = null
    private var listOfRequests = ArrayDeque<HistoryRequestInfo>()
    private var listOfMissingItemsInChunk = ArrayDeque<HistoryRequestInfo>()
    @Volatile private var downloadRunning = false
    private var listOfReturnedItems = mutableListOf<HistoryLog>()

    // added
    lateinit var communication: TandemUICommunication

    var progressAllItems = 1000  // how many items we will retrieve
    var progressPreviousChunksItems = 0 // how many were retrieved in previous chunk run
    var progressCurrentChunk = 0 // how many were retrieved in current chunk run
    var knownLogItemsCount = 0 // how many of log items were not of Unknown type

    var silentDownload = false // silent download is for retrieval of last 40 items (for bolus and TBR actions)


    init {
        historyPrefix = if (DEBUG_HISTORY) "HST: " else ""
    }

    fun downloadHistory(): Boolean {

        if (!TandemMobiPluginVersion.downloadHistory) {
            aapsLogger.info(TAG, "${historyPrefix}History download disabled (by flag)")
            return false
        }

        communication = TandemUICommunication(dataStore = tandemUiDataStore,
                                              pumpStatus = pumpStatus,
                                              pumpUtil = pumpUtil,
                                              aapsLogger= aapsLogger,
                                              uiInteraction = uiInteraction,
                                              notificationManager = notificationManager)

        communication.historyRetriever = this
        this.communication.tandemPumpCommunicationManager = tandemPumpConnector.getCommunicationManager()

        this.silentDownload = false
        val startTime = System.currentTimeMillis()
        val timeoutTime = startTime + ( 60 * 60 * 1000 )
        resetProgress(1000)
        downloadRunning = true
        startDataRetrieval()

        while (downloadRunning) {
            aapsLogger.debug("${historyPrefix}download running")
            pumpUtil.sleepSeconds(5)

            if (timeoutTime < System.currentTimeMillis()) {
                if (communication.messageCount==0) {
                    aapsLogger.error(TAG, "[History] Timeout reached while trying to read history, with no messages read.")
                    return false
                } else {
                    aapsLogger.error(TAG, "[History] Timeout reached while trying to read history, with ${communication.messageCount} messages read.")
                    downloadRunning = false
                    return false
                }
            }
        }

        this.communication.tandemPumpCommunicationManager = null

        setSemaphore()
        endProgress()

        var diffTime = System.currentTimeMillis() - startTime
        diffTime /= 1000

        aapsLogger.info(TAG, "${historyPrefix}Download finished in $diffTime seconds.")

        if (DEBUG_HISTORY)
            dbDataHandler.databaseStatistics() 

        return true
    }


    private fun setSemaphore() {
        aapsLogger.debug("setSemaphore: knownItems=$knownLogItemsCount")

        if (knownLogItemsCount>0) {
            if (!pumpStatus.semaphoreHistory) {
                pumpStatus.semaphoreHistory = true
                pumpStatus.semaphoreNeedsRefresh = true
                //rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Custom_2))
            }
        }
    }

    // this is not used at the moment, but might be needed in the future
    fun downloadHistoryRecentItems(): MutableList<HistoryLog> {

        communication = TandemUICommunication(dataStore = tandemUiDataStore,
                                              pumpStatus = pumpStatus,
                                              pumpUtil = pumpUtil,
                                              aapsLogger= aapsLogger,
                                              uiInteraction = uiInteraction,
                                              notificationManager = notificationManager)

        communication.historyRetriever = this
        this.communication.tandemPumpCommunicationManager = tandemPumpConnector.getCommunicationManager()

        this.silentDownload = true
        val startTime = System.currentTimeMillis()
        resetProgress(1000)
        downloadRunning = true
        startDataRetrieval()

        while(downloadRunning) {
            aapsLogger.debug("${historyPrefix}download running")
            pumpUtil.sleepSeconds(5)
        }

        this.communication.tandemPumpCommunicationManager = null

        setSemaphore()
        endProgress()

        var diffTime = System.currentTimeMillis() - startTime
        diffTime /= 1000

        aapsLogger.info(TAG, "${historyPrefix}Short Download finished in $diffTime seconds.")


        return listOfReturnedItems
    }


    private fun resetProgress(itemsToRetrive: Int?) {
        if (itemsToRetrive!=null) {
            progressAllItems = itemsToRetrive
        }
        progressPreviousChunksItems = 0
        progressCurrentChunk = 0
        knownLogItemsCount = 0
    }

    private fun startProgress() {
        if (!silentDownload) {
            pumpUtil.currentCommand = PumpCommandType.GetHistoryWithParameters
        }
    }

    private fun endProgress() {
        pumpUtil.currentCommand = null
        pumpUtil.driverStatus = PumpDriverState.Connected
        resetProgress(1000)
        pumpUtil.historyProgress = null
    }

    private fun updateRetrievalProgress() {
        val currentProgress = this.progressCurrentChunk + progressPreviousChunksItems
        val currentProgressString = "${currentProgress}/${this.progressAllItems}"
        //aapsLogger.error(TAG, "${historyPrefix}PROGRESS: $currentProgressString")

        if (!silentDownload) {
            pumpUtil.historyProgress = "($currentProgressString)"
        }
    }

    private fun startDataRetrieval()  {
        listOfRequests.clear()
        listOfMissingItemsInChunk.clear()
        currentRequest = null

        val summaryData = preferences.get(TandemStringNonPreferenceKey.HistorySummaryData)
        if (summaryData.isNotBlank()) {
            historySummaryDto = pumpUtil.gsonRegular.fromJson(summaryData, HistorySummaryDto::class.java)
            aapsLogger.debug(TAG, "${historyPrefix}Initial Summary: $historySummaryDto")
        }

        val gc = GregorianCalendar()
        gc.add(GregorianCalendar.DAY_OF_YEAR, -1*HISTORY_LIMIT_IN_DAYS)

        // pump stores time in seconds from 1st Jan 2008
        val timeFromDate =  gc.timeInMillis/1000 - Dates.JANUARY_1_2008_UNIX_EPOCH

        maxDateTimeInSec = timeFromDate.toInt()

        //aapsLogger.error(TAG, "${historyPrefix}Max Time Seconds: ${maxDateTimeInSec}")

        startProgress()

        submitHistoryRequest("historyLogStatus") {
            communication.sendCommand(HistoryLogStatusRequest())
        }
    }

    /**
     * Routes a single history-log wire send through [tandemDispatcher] at
     * [app.aaps.pump.tandem.common.concurrency.Priority.BACKGROUND]. The response arrives
     * asynchronously via the listener callback path, so the op completes as soon as the wire
     * send fires — it does not await the response. The token-bucket rate limit on BACKGROUND
     * throttles the *submit* rate (i.e. how often new chunks kick off); higher-priority ops
     * preempt waiting BACKGROUND submits.
     *
     * Uses the local [communication] (HistoryRetriever's own TandemUICommunication instance,
     * which has its `historyRetriever` field set to forward responses back here) rather than
     * the dispatcher's `sendUiCommand` extension — that one targets the singleton instance,
     * which doesn't know about this retriever.
     */
    private fun submitHistoryRequest(name: String, send: () -> Unit) {
        tandemDispatcher.submitBackground(name) { send() }
    }


    fun receivedStatus(message: HistoryLogStatusResponse) {

        aapsLogger.debug(TAG, "HST: Got LogStatusResponse: $message")

        if (historySummaryDto==null) {

            aapsLogger.debug(TAG, "HST: First read, creating DTO")

            val remainingRange = HistoryRange(message.firstSequenceNum,
                                              message.lastSequenceNum-RECORDS_RETRIEVAL_AMOUNT)

            historySummaryDto = HistorySummaryDto(serialNumber = pumpStatus.serialNumber.toInt(),
                                                  startRecord = message.firstSequenceNum,
                                                  lastRecord = message.lastSequenceNum,
                                                  modifiedStartRecord = message.firstSequenceNum,
                                                  missedRanges = arrayListOf(remainingRange)
            )

            var firstRec = message.lastSequenceNum-RECORDS_RETRIEVAL_AMOUNT

            if (firstRec<0) {
                firstRec = 0
            }

            // get last 1000 records
            listOfRequests.addAll(prepareChunks(firstRec, message.lastSequenceNum))

            // - on first read we get min, max (HistoryLogStatusRequest) and create HistorySummary and save it
            // - we start retreiving data: we will retrieve 5 x 200 = 1000 records per reading
            //     (reading will be done together with status every 5 minutes)
            // - once reading is done we create HistoryRange and add it to missed range

        } else {

            if (silentDownload) {
                prepareForShortHistoryReading(message)
            } else {
                prepareForFullHistoryReading(message)
            }

            // aapsLogger.debug(TAG, "HST: Non-First read")
            //
            // val diff = message.lastSequenceNum - historySummaryDto!!.lastRecord
            //
            // if (diff==0L) {
            //     aapsLogger.debug(TAG, "HST: Non-First read: No new records.")
            //     // no new records
            //     listOfRequests.addAll(getNextChunks(RECORDS_RETRIEVAL_AMOUNT))
            // } else if (diff<RECORDS_RETRIEVAL_AMOUNT) {
            //     aapsLogger.debug(TAG, "HST: Non-First read: Less than $RECORDS_RETRIEVAL_AMOUNT new records.")
            //     // less than 1000 new records
            //     listOfRequests.addAll(prepareChunks(historySummaryDto!!.lastRecord+1,
            //                                         message.lastSequenceNum))
            //
            //     val howMuchToGet = RECORDS_RETRIEVAL_AMOUNT - diff
            //
            //     // take something from missedRanges and update missed Ranger
            //     listOfRequests.addAll(getNextChunks(howMuchToGet.toInt()))
            //
            // } else {
            //     aapsLogger.debug(TAG, "HST: Non-First read: We have more than $RECORDS_RETRIEVAL_AMOUNT new records.")
            //     listOfRequests.addAll(prepareChunks(message.lastSequenceNum-RECORDS_RETRIEVAL_AMOUNT,
            //                                         message.lastSequenceNum))
            //
            //     val remainingRange = HistoryRange(message.firstSequenceNum,
            //                                       message.lastSequenceNum-RECORDS_RETRIEVAL_AMOUNT)
            //
            //     // set remaining into missedRanges
            //     historySummaryDto!!.missedRanges.add(remainingRange)
            // }

            historySummaryDto!!.lastRecord = message.lastSequenceNum

            // SUBSEQUENT READS
            //     - read min, max (HistoryLogStatusRequest) and check if new records there
            // - NO: read missedRanges and read next 1000 items
            // - YES: read new data, and remaining range is used to read old data, if there is more than
            // 1000 new records add new missedRange or modify the current one

        }

        historySummaryDto!!.activeProcessing.addAll(listOfRequests)
        saveSummary()

        resetProgress(howManyItemsInNextChunks(listOfRequests))

        //aapsLogger.error(TAG, "HST: HistorySummary: ${pumpUtil.gsonRegular.toJson(historySummaryDto)}")

        aapsLogger.info(TAG, "HST: List Of Requests: ${pumpUtil.gsonRegular.toJson(listOfRequests)}")

        if (listOfRequests.isEmpty()) {
            aapsLogger.info(TAG, "HST: There is no new records to retrieve")
            this.downloadRunning = false
        } else {
            aapsLogger.info(TAG, "HST: Start first retrieval (listOfRequests=${listOfRequests.size})")
            // start reading
            executeNextLogGet(listOfRequests)
        }
    }


    private fun prepareForFullHistoryReading(message: HistoryLogStatusResponse) {
        aapsLogger.debug(TAG, "HST: Non-First read - Full Reading")

        val diff = message.lastSequenceNum - historySummaryDto!!.lastRecord

        if (diff==0L) {
            aapsLogger.debug(TAG, "HST: Non-First read: No new records.")
            // no new records
            listOfRequests.addAll(getNextChunks(RECORDS_RETRIEVAL_AMOUNT))
        } else if (diff<RECORDS_RETRIEVAL_AMOUNT) {
            aapsLogger.debug(TAG, "HST: Non-First read: Less than $RECORDS_RETRIEVAL_AMOUNT new records.")
            // less than 1000 new records
            listOfRequests.addAll(prepareChunks(historySummaryDto!!.lastRecord+1,
                                                message.lastSequenceNum))

            val howMuchToGet = RECORDS_RETRIEVAL_AMOUNT - diff

            // take something from missedRanges and update missed Ranger
            listOfRequests.addAll(getNextChunks(howMuchToGet.toInt()))

        } else {
            aapsLogger.debug(TAG, "HST: Non-First read: We have more than $RECORDS_RETRIEVAL_AMOUNT new records.")
            listOfRequests.addAll(prepareChunks(message.lastSequenceNum-RECORDS_RETRIEVAL_AMOUNT,
                                                message.lastSequenceNum))

            val remainingRange = HistoryRange(historySummaryDto!!.lastRecord+1,
                                              message.lastSequenceNum-RECORDS_RETRIEVAL_AMOUNT)

            // set remaining into missedRanges
            historySummaryDto!!.missedRanges.add(remainingRange)
        }
    }


    private fun prepareForShortHistoryReading(message: HistoryLogStatusResponse) {
        aapsLogger.error(TAG, "HST: prepareForShortHistoryReading")

        aapsLogger.debug(TAG, "HST: Non-First read - Short Reading")

        val diff = message.lastSequenceNum - historySummaryDto!!.lastRecord

        if (diff==0L) {
            aapsLogger.debug(TAG, "HST: Non-First Short read: No new records.")
            // no new records
        } else if (diff<SHORT_RECORDS_RETRIEVAL_AMOUNT) {
            aapsLogger.debug(TAG, "HST: Non-First Short read: Less than $SHORT_RECORDS_RETRIEVAL_AMOUNT new records.")
            // less than 1000 new records
            listOfRequests.addAll(prepareChunks(historySummaryDto!!.lastRecord+1,
                                                message.lastSequenceNum))
        } else {
            aapsLogger.debug(TAG, "HST: Non-First Short read: We have more than $SHORT_RECORDS_RETRIEVAL_AMOUNT new records.")
            listOfRequests.addAll(prepareChunks(message.lastSequenceNum-SHORT_RECORDS_RETRIEVAL_AMOUNT,
                                                message.lastSequenceNum))

            val remainingRange = HistoryRange(historySummaryDto!!.lastRecord+1,
                                              message.lastSequenceNum-SHORT_RECORDS_RETRIEVAL_AMOUNT)

            // set remaining into missedRanges
            historySummaryDto!!.missedRanges.add(remainingRange)
        }
    }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getNextChunks(howManyEntriesDoWeNeed: Int): MutableList<HistoryRequestInfo> {

        aapsLogger.debug(TAG, "HST: getNextChunks (howManyEntriesDoWeNeed=$howManyEntriesDoWeNeed)")

        val newChunks: MutableList<HistoryRequestInfo> = mutableListOf()

        if (historySummaryDto!!.missedRanges.isEmpty() && historySummaryDto!!.activeProcessing.isEmpty()) {
            aapsLogger.debug(TAG, "HST: getNextChunks: No missed ranges found.")
            return newChunks
        }

        // if we have some activeProcessing records we need to re-add them into missed ranges
        if (historySummaryDto!!.activeProcessing.isNotEmpty()) {
            val missedRangesFromActiveProcessing = getMissedRangesFromActiveProcessing()
            historySummaryDto!!.missedRanges.addAll(missedRangesFromActiveProcessing)
        }

        var finished = false

        while(!finished) {

            val newestRange: HistoryRange? = getNewestRange()

            if (newestRange==null) {
                finished = true
            } else {

                val itemsCount = howManyItemsInNextChunks(newChunks)

                aapsLogger.debug(TAG, "HST: How Many Items In Next Chunks: $itemsCount")

                if (itemsCount<howManyEntriesDoWeNeed) {

                    val needed = howManyEntriesDoWeNeed - itemsCount

                    val newestChuckAmount: Int = newestRange.getItemAmount()

                    aapsLogger.debug(TAG, "HST: Newest Range: $newestRange")

                    if (newestChuckAmount<needed) {
                        newChunks.addAll(prepareChunksFromRange(newestRange))
                        //finished = true
                    } else if (newestChuckAmount==needed) {
                        newChunks.addAll(prepareChunksFromRange(newestRange))
                        finished = true
                    } else {
                        val usePartOfChunk = usePartOfChunk(needed, newestRange)
                        aapsLogger.debug(TAG, "HST: Too big chunk found. Use Part of Chunk: $usePartOfChunk")
                        newChunks.addAll(prepareChunksFromRange(usePartOfChunk))
                        finished = true
                    }

                } else if (itemsCount==howManyEntriesDoWeNeed) {
                    finished = true
                }
            }
        }

        aapsLogger.debug(TAG, "HST: getNextChunks: New Chunks Found: $newChunks")

        return newChunks

    }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun usePartOfChunk(needed: Int, inputRange: HistoryRange): HistoryRange {
        val newMissedRange = HistoryRange(inputRange.start, inputRange.end-needed)
        this.historySummaryDto!!.missedRanges.add(newMissedRange)

        aapsLogger.debug(TAG, "${historyPrefix}New Missed Range: $newMissedRange")

        val newSelectedRange = HistoryRange(inputRange.end-needed+1, inputRange.end)

        aapsLogger.debug(TAG, "${historyPrefix}Returned Range: $newSelectedRange")

        return newSelectedRange
    }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getMissedRangesFromActiveProcessing(): List<HistoryRange> {
        val historyRangeList = this.historySummaryDto!!.activeProcessing.stream()
            .map { item -> HistoryRange(item.startSequence, item.endSequence) }
            .collect(Collectors.toList())

        this.historySummaryDto!!.activeProcessing.clear()

        return historyRangeList
    }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun howManyItemsInNextChunks(list: MutableList<HistoryRequestInfo>) : Int {
        var countItems = 0
        list.stream().forEach{item -> countItems += item.numberOfLogs }
        aapsLogger.debug(TAG, "${historyPrefix}howManyItemsInNextChunks: $countItems")
        return countItems
    }

    private fun debugSummary() {
        //aapsLogger.error(TAG, "${historyPrefix}Summary: active=${historySummaryDto!!.activeProcessing.size}, missed=${historySummaryDto!!.missedRanges.size}")
    }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getNewestRange(): HistoryRange? {
        if (this.historySummaryDto!!.missedRanges.isEmpty()) {
            aapsLogger.debug(TAG, "${historyPrefix}getNewestChunk: No missed ranges found.")
            return null
        } else if (this.historySummaryDto!!.missedRanges.size==1) {
            aapsLogger.debug(TAG, "${historyPrefix}getNewestChunk: One missed range found.")
            val selectedRange = this.historySummaryDto!!.missedRanges[0]
            this.historySummaryDto!!.missedRanges.removeAt(0)
            return selectedRange
        } else {
            var selectedRange : HistoryRange? = null
            for (missedRange in this.historySummaryDto!!.missedRanges) {
                if (selectedRange==null) {
                    selectedRange = missedRange
                } else {
                    if (missedRange.end > selectedRange.end) {
                        selectedRange = missedRange
                    }
                }
            }
            this.historySummaryDto!!.missedRanges.remove(selectedRange)
            aapsLogger.debug(TAG, "${historyPrefix}getNewestChunk: One of many missed ranges found: $selectedRange")
            return selectedRange
        }
    }


    private fun prepareChunksFromRange(historyRange: HistoryRange): MutableList<HistoryRequestInfo> {
        return prepareChunks(historyRange.start, historyRange.end)
    }


    private fun prepareChunks(startRange: Long, endRange: Long): MutableList<HistoryRequestInfo> {
        aapsLogger.debug(TAG, "${historyPrefix}prepareChunks: Start range: $startRange and End range : $endRange")

        var currentEnd = endRange
        val chunksList: MutableList<HistoryRequestInfo> = mutableListOf()

        while (currentEnd > startRange) {
            var currentStart = currentEnd - CHUNK_SIZE +1

            if (currentStart<startRange) {
                currentStart = startRange
            }

            chunksList.add(HistoryRequestInfo(startSequence=currentStart,
                                              endSequence = currentEnd))

            currentEnd -= CHUNK_SIZE
        }

        // aapsLogger.error(TAG, "${historyPrefix}Chunks: ${pumpUtil.gsonRegular.toJson(chunksList)}")

        return chunksList
    }


    private fun saveSummary() {
        //aapsLogger.debug(TAG, "${historyPrefix}Save Summary: $historySummaryDto")
        preferences.put(TandemStringNonPreferenceKey.HistorySummaryData, this.pumpUtil.gsonRegular.toJson(this.historySummaryDto))
        debugSummary()
    }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun executeNextLogGet(queue: ArrayDeque<HistoryRequestInfo>) {
        currentRequest = queue.removeFirst()
        aapsLogger.info(TAG, "${historyPrefix}executeNextLogGet (start=${currentRequest!!.startSequence}, end=${currentRequest!!.endSequence}, count=${currentRequest!!.numberOfLogs})")
        val req = HistoryLogRequest(currentRequest!!.startSequence, currentRequest!!.numberOfLogs)
        submitHistoryRequest("historyLogChunk[${currentRequest!!.startSequence}-${currentRequest!!.endSequence}]") {
            this.communication.sendCommand(req)
        }
    }


    fun receivedLogStreamResponse(message: HistoryLogStreamResponse) {

        aapsLogger.debug(TAG, "${historyPrefix}Received from Stream: numberOfHistoryLogs=${message.numberOfHistoryLogs}, streamId=${message.streamId}, historyLogsSize=${message.historyLogs.size} ")

        this.currentRequest!!.historyLogMap.putAll(message.historyLogs.associateBy { it.sequenceNum })

        this.progressCurrentChunk = currentRequest!!.historyLogMap.size

        //aapsLogger.error(TAG, "${historyPrefix}Number of Logs: $progressCurrentChunk")

        updateRetrievalProgress()

        if (this.currentRequest!!.chunkComplete()) {
            processChunkComplete()
            return
        }

        enableLastMessageWatchdog()

    }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun processChunkComplete() {

        aapsLogger.debug(TAG, "${historyPrefix}processChunkComplete: ${currentRequest!!.historyLogMap.size}")

        disableLastMessageWatchdog()

        if (this.currentRequest!!.chunkHasAllItems()) {
            aapsLogger.info(TAG, "${historyPrefix}Chunk Has All Items: ${currentRequest!!.historyLogMap.size}")
        } else {
            aapsLogger.warn(TAG, "${historyPrefix}Chunk Has MISSING Items: ${currentRequest!!.historyLogMap.size}")
            addMissingItemsInChunk(this.currentRequest!!)
        }

        addToDatabase(this.currentRequest!!)
        removeFromActiveItems(this.currentRequest!!)
        saveSummary()

        this.progressPreviousChunksItems += currentRequest!!.historyLogMap.size
        this.progressCurrentChunk = 0

        if (haveWeReachedHistoryLimit()) {
            this.downloadRunning = false
            this.historySummaryDto!!.modifiedStartRecord = currentRequest!!.startSequence //we change modifiedStartRecord
            this.historySummaryDto!!.activeProcessing.clear()
            this.historySummaryDto!!.missedRanges.clear() // since we have all history needed, we clear missedRanges
            saveSummary()
            return
        }

        if (listOfMissingItemsInChunk.isNotEmpty()) {
            aapsLogger.debug(TAG, "${historyPrefix}Missing items in the chunk: ${pumpUtil.gsonRegular.toJson(listOfMissingItemsInChunk)}")
            executeNextLogGet(listOfMissingItemsInChunk)
        } else {
            if (listOfRequests.isNotEmpty()) {
                aapsLogger.debug(TAG, "${historyPrefix}Next chunk retrieval (listOfRequests=${listOfRequests.size})")
                executeNextLogGet(listOfRequests)
            } else {
                aapsLogger.debug(TAG, "${historyPrefix}No more chunks to get... Exiting")
                this.downloadRunning = false
            }
        }

        // check if first and last received = yes chunk ready
        // the add to db
        // determine missing records (if number != 200)
        //    no:  next chuck
        //    yes: prepare missing records chunks - MRC
        //         repeat retrieval until all MRC here

    }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun addMissingItemsInChunk(currentRequest: HistoryRequestInfo) {
        // if items are missing in chunk, we add them back to activeList and to listOfMissingItemsInChunk
        val historyRangeList: MutableCollection<HistoryRequestInfo> = mutableListOf()
        var startSeq: Long
        var endSeq: Long

        var i = currentRequest.startSequence

        //aapsLogger.error(TAG, "Before While")

        while (i <= currentRequest.endSequence) {

            //aapsLogger.error(TAG, "While $i")

            if (!currentRequest.historyLogMap.containsKey(i)) {
                startSeq = i

                //aapsLogger.error(TAG, "Not found $i")

                for(j in startSeq+1..currentRequest.endSequence) {
                    //aapsLogger.error(TAG, "For $j")
                    if (currentRequest.historyLogMap.containsKey(j)) {
                        //aapsLogger.error(TAG, "Key found $j")
                        endSeq = j-1
                        historyRangeList.add(HistoryRequestInfo(startSeq, endSeq))
                        //aapsLogger.error(TAG, "Add History Range (start=$startSeq, end=$endSeq)")
                        //startSeq = null
                        //endSeq = null
                        i = j-1
                        break
                    }

                    if (j==currentRequest.endSequence) {
                        //aapsLogger.error(TAG, "End Reached ($startSeq-$endSeq)")
                        //aapsLogger.error(TAG, "Add History Range On End (start=$startSeq, end=$endSeq)")
                        historyRangeList.add(HistoryRequestInfo(startSeq, currentRequest.endSequence))
                        i=currentRequest.endSequence
                    }
                }
            }
            i++
        }

        this.historySummaryDto!!.activeProcessing.addAll(historyRangeList)
        saveSummary()

        this.listOfMissingItemsInChunk.addAll(historyRangeList)
    }

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    private fun addToDatabase(historyRequestInfo: HistoryRequestInfo) {
        aapsLogger.debug(TAG, "${historyPrefix}Add History Logs to Database (count=${historyRequestInfo.historyLogMap.values.size}) N/A")

        val listOfRecords: MutableList<HistoryLog> = mutableListOf()

        var historyLog: HistoryLog? = null

        historyPostProcessor.postProcessHistory(historyRequestInfo.historyLogMap.values)

        for (entry in historyRequestInfo.historyLogMap.values) {
            if (historyLog==null) {
                historyLog = entry
            }
            if (entry.pumpTimeSec >= maxDateTimeInSec) {
                listOfRecords.add(entry)

                aapsLogger.error(TAG, "${historyPrefix}Entry:  ${formatter.format(entry.pumpTimeSecInstant)} - ${entry.javaClass.simpleName} (${entry.sequenceNum})")

                if (entry !is UnknownHistoryLog) {
                    knownLogItemsCount++
                }
            }
        }

        //if (historyRequestInfo.)

        if (historyLog!=null) {
            aapsLogger.debug(TAG, "${historyPrefix}Newest entry for database: ${formatter.format(historyLog.pumpTimeSecInstant)} - ${historyLog.javaClass.simpleName}")
        }

        aapsLogger.info(TAG, "${historyPrefix}Add History Logs to Database (filtered_count=${listOfRecords.size},retrieved_count=${historyRequestInfo.historyLogMap.values.size})")
        dbDataHandler.addHistoryLogs(listOfRecords, true)

        if (silentDownload) {
            listOfReturnedItems.addAll(listOfRecords)
        }
    }


    private fun removeFromActiveItems(historyRequestInfo: HistoryRequestInfo) {
        //aapsLogger.error(TAG, "${historyPrefix}Before remove ${historySummaryDto!!.activeProcessing.size}")
        this.historySummaryDto!!.activeProcessing.remove(historyRequestInfo)
        //aapsLogger.error(TAG, "${historyPrefix}After Remove remove ${historySummaryDto!!.activeProcessing.size}")
    }


    private fun haveWeReachedHistoryLimit(): Boolean {

        for (entry in this.currentRequest!!.historyLogMap) {
            if (entry.value.pumpTimeSec < maxDateTimeInSec) {
                aapsLogger.info(TAG, "${historyPrefix}Found entry older than 1.5 months: $entry")
                return true
            }
        }

        aapsLogger.debug(TAG, "${historyPrefix}haveWeReachedHistoryLimit: all entries are newer, returning false")

        return false
    }


    private fun disableLastMessageWatchdog() {
        // TODOX  disableLastMessageWatchdog - we will try without this for now, if needed it will be added in phase 4
        //aapsLogger.error(TAG, "${historyPrefix}disableLastMessageWatchdog not implemented.")
    }


    private fun enableLastMessageWatchdog() {
        // TODOX enableLastMessageWatchdog -  - we will try without this for now, if needed it will be added in phase 4
        //    when message is received we note the time and wait in special thread if timeout is reached
        //    if watchdog already exists, we just extend time
        //aapsLogger.error(TAG, "${historyPrefix}enableLastMessageWatchdog not implemented.")
    }


    fun receivedLogResponse(message: HistoryLogResponse) {
        //aapsLogger.error(TAG, "${historyPrefix}Received LogResponse: $message")
    }

}
