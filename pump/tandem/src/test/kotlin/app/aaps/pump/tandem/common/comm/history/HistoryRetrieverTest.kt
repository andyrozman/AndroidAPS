package app.aaps.pump.tandem.common.comm.history

import android.content.Context
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.notifications.NotificationManager
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.pump.tandem.common.comm.ui.TandemUICommunication
import app.aaps.pump.tandem.common.concurrency.TandemDispatcher
import app.aaps.pump.tandem.common.data.history.HistoryRange
import app.aaps.pump.tandem.common.data.history.HistoryRequestInfo
import app.aaps.pump.tandem.common.data.history.HistorySummaryDto
import app.aaps.pump.tandem.common.database.data.DbDataHandler
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnector
import app.aaps.pump.tandem.common.keys.TandemLongNonPreferenceKey
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import app.aaps.shared.tests.AAPSLoggerTest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HistoryLogStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.historyLog.AlarmActivatedHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLog
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import app.aaps.pump.tandem.common.concurrency.PumpDispatcherScope
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnectionManager
import java.util.GregorianCalendar
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)  // matches the old MockitoJUnitRunner default
@TestMethodOrder(MethodOrderer.MethodName::class)
class HistoryRetrieverTest {

    @Mock lateinit var tandemUICommunication: TandemUICommunication
    @Mock lateinit var tandemPumpStatus: TandemPumpStatus
    @Mock lateinit var tandemPumpUtil: TandemPumpUtil
    @Mock lateinit var preferences: Preferences
    @Mock lateinit var dbDataHandler: DbDataHandler
    @Mock lateinit var rxBus: RxBus
    @Mock lateinit var context: Context
    @Mock lateinit var tandemPumpConnector: TandemPumpConnector
    @Mock lateinit var uiInteraction: UiInteraction
    @Mock lateinit var notificationManager: NotificationManager
    @Mock lateinit var tandemDispatcher: TandemDispatcher
    @Mock lateinit var historyPostProcessor: HistoryPostProcessor


    val aapsLogger = AAPSLoggerTest()
    val TAG = LTag.PUMP

    lateinit var unitToTest: HistoryRetriever
    lateinit var unitToTestSpy: HistoryRetriever

    val gsonRegular: Gson = GsonBuilder().create()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        //setPrivateField(this.tandemPumpUtil, "gsonRegular", gsonRegular)
        `when`(this.tandemPumpUtil.gsonRegular).thenReturn(gsonRegular)
        `when`(dbDataHandler.getLatestHistorySequenceIds(any())).thenReturn(emptySet())
        `when`(dbDataHandler.getMaxHistorySequenceId()).thenReturn(null)
        `when`(preferences.getIfExists(TandemLongNonPreferenceKey.HistoryResumeUpperSequence)).thenReturn(null)

        this.unitToTest = HistoryRetriever(
            //communication = tandemUICommunication,
            pumpStatus = tandemPumpStatus,
            aapsLogger = aapsLogger,
            pumpUtil = tandemPumpUtil,
            preferences = preferences,
            dbDataHandler = dbDataHandler,
            rxBus = rxBus,
            context = context,
            tandemPumpConnector = tandemPumpConnector,
            uiInteraction = uiInteraction,
            notificationManager = notificationManager,
            tandemDispatcher = tandemDispatcher,
            historyPostProcessor = historyPostProcessor
        )
        // Tests call internal methods directly (processChunkComplete, etc.) without going through
        // downloadHistory(), which is what would otherwise replace `communication` with a freshly
        // constructed instance. Bind the mock here so verify(tandemUICommunication).sendCommand(...)
        // observes the wire calls those internal methods make.
        this.unitToTest.communication = tandemUICommunication

        // Real production code routes wire sends through tandemDispatcher.submitBackground { ... }.
        // The default Mockito mock swallows the lambda; stub it to invoke the block inline with a
        // fake scope so the verify(tandemUICommunication).sendCommand(...) assertions reach what
        // the production code is actually doing.
        val fakeScope = object : PumpDispatcherScope {
            override val pumpConnectionManager: TandemPumpConnectionManager
                get() = error("not used in HistoryRetriever tests")
            override val tandemUICommunication: TandemUICommunication
                get() = this@HistoryRetrieverTest.tandemUICommunication
        }
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val block = invocation.arguments.last() as PumpDispatcherScope.() -> Unit
            fakeScope.block()
            Unit
        }.whenever(tandemDispatcher).submitBackground(any(), any(), any())

        this.unitToTestSpy = spy(this.unitToTest)
    }

    @Test
    fun getNewestRange_WithNoMissedRanges() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.missedRanges.clear()
        val chuck = unitToTest.getNewestRange()

        assertNull(chuck)
    }


    @Test
    fun getNewestRange_WithMultipleMissedRanges() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.missedRanges.clear()
        historySummaryDto.missedRanges.add(HistoryRange(0, 200))
        historySummaryDto.missedRanges.add(HistoryRange(500, 2000))
        historySummaryDto.missedRanges.add(HistoryRange(2100, 3000))

        val chuck = unitToTest.getNewestRange()

        assertNotNull(chuck)
        assertHistoryRange(chuck, 2100, 3000)
        assertEquals(2, historySummaryDto.missedRanges.size)
    }


    @Test
    fun getNewestRange_WithSingleMissedRanges() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.missedRanges.clear()
        historySummaryDto.missedRanges.add(HistoryRange(2100, 3000))

        val chuck = unitToTest.getNewestRange()

        assertNotNull(chuck)
        assertHistoryRange(chuck, 2100, 3000)
        assertEquals(0, historySummaryDto.missedRanges.size)
    }


    @Test
    fun getNextChunks_MissedRangeTheSame() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.missedRanges.clear()
        historySummaryDto.missedRanges.add(HistoryRange(2100, 2599))

        val chunks = unitToTest.getNextChunks(500)

//        val historyRequestInfo: HistoryRequestInfo = chuck[0]

        System.out.println("Chunks: $chunks")

        assertEquals(3, chunks.size)
//        assertEquals(2100, historyRequestInfo.startSequence)
//        assertEquals(3000, historyRequestInfo.endSequence)

        assertHistoryRequestInfo(chunks, 2400, 2599)
        assertHistoryRequestInfo(chunks, 2200, 2399)
        assertHistoryRequestInfo(chunks, 2100, 2199)

        assertEquals(0, historySummaryDto.missedRanges.size)

    }


    @Test
    fun getNextChunks_MissedRangeTooBig() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.missedRanges.clear()
        historySummaryDto.missedRanges.add(HistoryRange(2000, 3200))

        // MutableList<HistoryRequestInfo>

        val chunks = unitToTest.getNextChunks(500)

        System.out.println("Chunks: $chunks")
        //val historyRequestInfo: HistoryRequestInfo = chuck[0]

        assertEquals(3, chunks.size)
        assertHistoryRequestInfo(chunks, 3001, 3200)
        assertHistoryRequestInfo(chunks, 2801, 3000)
        assertHistoryRequestInfo(chunks, 2701, 2800)

        System.out.println("Missed Range: ${historySummaryDto.missedRanges}")

        assertMissedRange(historySummaryDto, 2000, 2700)


//        assertEquals(2100, historyRequestInfo.startSequence)
//        assertEquals(3000, historyRequestInfo.endSequence)
//        assertHistoryRequestInfo(chuck, 2000, 2499)
//        assertMissedRange(historySummaryDto, 2499, 3000)

        //assertEquals(1, chuck[0])
        //assertEquals(0, historySummaryDto.missedRanges.size)

        //Assert.fail()
    }


    @Test
    fun getNextChunks_MissedRangeTooSmall() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.missedRanges.clear()
        historySummaryDto.missedRanges.add(HistoryRange(2100, 2399))

        // MutableList<HistoryRequestInfo>

        val chunks = unitToTest.getNextChunks(500)

        //val historyRequestInfo: HistoryRequestInfo = chuck[0]

        System.out.println("Chunks: $chunks")

        assertEquals(2, chunks.size)
//        assertEquals(2100, historyRequestInfo.startSequence)
//        assertEquals(3000, historyRequestInfo.endSequence)

        //assertEquals(1, chuck[0])
        //assertEquals(0, historySummaryDto.missedRanges.size)

        assertHistoryRequestInfo(chunks, 2200, 2399)
        assertHistoryRequestInfo(chunks, 2100, 2199)
        //assertHistoryRequestInfo(chunks, 2500, 2599)

        assertEquals(0, historySummaryDto.missedRanges.size)

    }


    @Test
    fun getNextChunks_2MissedRangesUsed() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.missedRanges.clear()
        historySummaryDto.missedRanges.add(HistoryRange(2100, 2399))
        historySummaryDto.missedRanges.add(HistoryRange(1500, 2000))

        // MutableList<HistoryRequestInfo>

        val chunks = unitToTest.getNextChunks(500)

        //val historyRequestInfo: HistoryRequestInfo = chuck[0]

        System.out.println("Chunks: $chunks")

        assertEquals(3, chunks.size)
//        assertEquals(2100, historyRequestInfo.startSequence)
//        assertEquals(3000, historyRequestInfo.endSequence)

        //assertEquals(1, chuck[0])
        //assertEquals(0, historySummaryDto.missedRanges.size)

        assertHistoryRequestInfo(chunks, 2200, 2399)
        assertHistoryRequestInfo(chunks, 2100, 2199)
        assertHistoryRequestInfo(chunks, 1801, 2000)
        //assertHistoryRequestInfo(chunks, 2500, 2599)

        assertEquals(1, historySummaryDto.missedRanges.size)
        assertMissedRange(historySummaryDto, 1500, 1800)
    }


    @Test
    fun getNextChunks_2MissedRangesUsedOneFromActive() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.activeProcessing.add(HistoryRequestInfo(1500, 2000))

        historySummaryDto.missedRanges.clear()
        historySummaryDto.missedRanges.add(HistoryRange(2100, 2399))
        //historySummaryDto.missedRanges.add(HistoryRange(1500, 2000))

        // MutableList<HistoryRequestInfo>

        val chunks = unitToTest.getNextChunks(500)

        //val historyRequestInfo: HistoryRequestInfo = chuck[0]

        System.out.println("Chunks: $chunks")

        assertEquals(3, chunks.size)
//        assertEquals(2100, historyRequestInfo.startSequence)
//        assertEquals(3000, historyRequestInfo.endSequence)

        //assertEquals(1, chuck[0])
        //assertEquals(0, historySummaryDto.missedRanges.size)

        assertHistoryRequestInfo(chunks, 2200, 2399)
        assertHistoryRequestInfo(chunks, 2100, 2199)
        assertHistoryRequestInfo(chunks, 1801, 2000)
        //assertHistoryRequestInfo(chunks, 2500, 2599)

        assertEquals(1, historySummaryDto.missedRanges.size)
        assertMissedRange(historySummaryDto, 1500, 1800)
    }



    @Test
    fun getNextChunks_NoneFound() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.missedRanges.clear()

        val chuck = unitToTest.getNextChunks(500)

        assertEquals(0, chuck.size)
    }


    @Test
    fun getMissedRangesFromActiveProcessing() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.activeProcessing.add(HistoryRequestInfo(0, 10))
        historySummaryDto.activeProcessing.add(HistoryRequestInfo(45, 60))

        val missedRanges = unitToTest.getMissedRangesFromActiveProcessing()

        assertEquals(0, historySummaryDto.activeProcessing.size)
        assertMissedRange(missedRanges, 0, 10)
        assertMissedRange(missedRanges, 45, 60)
    }





    @Test
    fun usePartOfChunk() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        val chunk = unitToTest.usePartOfChunk(5, HistoryRange(0, 10))

        //assertNotNull(chunk)
//        assertEquals(0, chunk.startSequence)
//        assertEquals(4, chunk.endSequence)

        assertHistoryRange(chunk, 6, 10)

        assertEquals(1, historySummaryDto.missedRanges.size)
//        assertEquals(5, historySummaryDto.missedRanges[0].start)
//        assertEquals(10, historySummaryDto.missedRanges[0].end)

        assertMissedRange(historySummaryDto, 0, 5)

    }

    @Test
    fun processChunkComplete_WithNextGet() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        var currentRequest: HistoryRequestInfo = HistoryRequestInfo(
            startSequence = 0,
            endSequence = 10
        )

        historySummaryDto.activeProcessing.add(currentRequest)

        val historyLogs = createMapOfHistoryLogs(HistoryLogsParameter.Any, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        currentRequest.historyLogMap.putAll(historyLogs)

        setMaxDateTimeInSec()

        setPrivateField(unitToTest, "currentRequest", currentRequest)

        var listOfRequests = ArrayDeque<HistoryRequestInfo>()
        listOfRequests.add(currentRequest)
        setPrivateField(unitToTest, "listOfRequests", listOfRequests)


        unitToTest.processChunkComplete()

        verify(dbDataHandler).addHistoryLogs(any(), any())
        verify(tandemUICommunication).sendCommand(any())
        //verify(unitToTestSpy).executeNextLogGet(any())

        //Assert.fail()
    }


    @Test
    fun processChunkComplete_NoNext() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        var currentRequest: HistoryRequestInfo = HistoryRequestInfo(
            startSequence = 0,
            endSequence = 10
        )

        historySummaryDto.activeProcessing.add(currentRequest)

        val historyLogs = createMapOfHistoryLogs(HistoryLogsParameter.Any, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        currentRequest.historyLogMap.putAll(historyLogs)

        setMaxDateTimeInSec()

        setPrivateField(unitToTest, "currentRequest", currentRequest)

//        var listOfRequests = ArrayDeque<HistoryRequestInfo>()
//        listOfRequests.add(currentRequest)
//        setPrivateField(unitToTest, "listOfRequests", listOfRequests)


        unitToTest.processChunkComplete()

        verify(dbDataHandler).addHistoryLogs(any(), any())
        //verify(tandemUICommunication).sendCommand(any())
        //verify(unitToTestSpy).executeNextLogGet(any())

        //Assert.fail()
    }


    @Test
    fun processChunkComplete_NotFullChunk() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        var currentRequest: HistoryRequestInfo = HistoryRequestInfo(
            startSequence = 0,
            endSequence = 10
        )

        historySummaryDto.activeProcessing.add(currentRequest)

        val historyLogs = createMapOfHistoryLogs(HistoryLogsParameter.Any, 0, 1, 2, 5, 6, 7, 8, 9, 10)

        currentRequest.historyLogMap.putAll(historyLogs)

        setMaxDateTimeInSec()

        setPrivateField(unitToTest, "currentRequest", currentRequest)

//        var listOfRequests = ArrayDeque<HistoryRequestInfo>()
//        listOfRequests.add(currentRequest)
//        setPrivateField(unitToTest, "listOfRequests", listOfRequests)


        unitToTest.processChunkComplete()

        verify(dbDataHandler).addHistoryLogs(any(), any())
        verify(tandemUICommunication).sendCommand(any())
        //verify(unitToTestSpy).executeNextLogGet(any())

        //Assert.fail()
    }


    @Test
    fun processChunkComplete_HistoyRecords() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        var currentRequest: HistoryRequestInfo = HistoryRequestInfo(
            startSequence = 0,
            endSequence = 10
        )

        historySummaryDto.activeProcessing.add(currentRequest)

        val historyLogs = createMapOfHistoryLogs(HistoryLogsParameter.LongBackDate, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        currentRequest.historyLogMap.putAll(historyLogs)

        setMaxDateTimeInSec()

        setPrivateField(unitToTest, "currentRequest", currentRequest)

//        var listOfRequests = ArrayDeque<HistoryRequestInfo>()
//        listOfRequests.add(currentRequest)
//        setPrivateField(unitToTest, "listOfRequests", listOfRequests)


        unitToTest.processChunkComplete()

        verify(dbDataHandler).addHistoryLogs(any(), any())
        //verify(tandemUICommunication).sendCommand(any())
        //verify(unitToTestSpy).executeNextLogGet(any())

        //Assert.fail()
    }


    private fun setMaxDateTimeInSec() {
        val gc = GregorianCalendar()
        gc.add(GregorianCalendar.DAY_OF_YEAR, -1*44)

        val maxDateTimeInSec = (gc.timeInMillis/1000).toInt()
        setPrivateField(this.unitToTest, "maxDateTimeInSec", maxDateTimeInSec)

    }


    @Test
    fun receivedStatus_FirstRead() {

        var historyLogStatusResponse = HistoryLogStatusResponse(10, 0, 9)

        unitToTest.receivedStatus(historyLogStatusResponse)
        verify(tandemUICommunication).sendCommand(any())
        //Assert.fail()
    }


    @Test
    fun receivedStatus_NextRead_NoNew() {

        val historySummaryDto = createHistorySummaryAndSetToClass()
        historySummaryDto.startRecord = 0
        historySummaryDto.lastRecord = 9

        var historyLogStatusResponse = HistoryLogStatusResponse(10, 0, 9)

        unitToTest.receivedStatus(historyLogStatusResponse)
        //verify(tandemUICommunication).sendCommand(any())
        //Assert.fail()
    }

    @Test
    fun receivedStatus_NextRead_DiffSmaller() {

        val historySummaryDto = createHistorySummaryAndSetToClass()
        historySummaryDto.startRecord = 0
        historySummaryDto.lastRecord = 9

        var historyLogStatusResponse = HistoryLogStatusResponse(790, 10, 800)

        unitToTest.receivedStatus(historyLogStatusResponse)
        verify(tandemUICommunication).sendCommand(any())

        assertEquals(0, historySummaryDto.missedRanges.size)
        //Assert.fail()
    }

    @Test
    fun receivedStatus_NextRead_DiffBigger() {

        val historySummaryDto = createHistorySummaryAndSetToClass()
        historySummaryDto.startRecord = 0
        historySummaryDto.lastRecord = 9

        var historyLogStatusResponse = HistoryLogStatusResponse(1190, 10, 1200)

        unitToTest.receivedStatus(historyLogStatusResponse)
        verify(tandemUICommunication).sendCommand(any())

        // diff (1200-9=1191) >= RECORDS_RETRIEVAL_AMOUNT: last 1000 fetched now, older records
        // deferred into one missed range.
        assertEquals(1, historySummaryDto.missedRanges.size)
        //Assert.fail()
    }


    @Test
    fun addMissingItems_InChunk_All() {
        val historyRequestInfo = HistoryRequestInfo(
            startSequence = 0,
            endSequence = 20,
            numberOfLogs = 20
        )

        val historySummaryDto = createHistorySummaryAndSetToClass()

        unitToTest.addMissingItemsInChunk(historyRequestInfo)

        assertEquals(1, historySummaryDto.activeProcessing.size)
        assertActiveProcessing(historySummaryDto, 0, 20)
    }

    @Test
    fun addMissingItems_InChunk_Some() {
        val historyRequestInfo = HistoryRequestInfo(
            startSequence = 0,
            endSequence = 20,
            numberOfLogs = 20
        )

        historyRequestInfo.historyLogMap.putAll(createMapOfHistoryLogs(HistoryLogsParameter.Any, 0,1,2,3,4,5,6,12,13,14,15,19,20))

        val historySummaryDto = createHistorySummaryAndSetToClass()

        unitToTest.addMissingItemsInChunk(historyRequestInfo)

        assertEquals(2, historySummaryDto.activeProcessing.size)

        assertActiveProcessing(historySummaryDto, 7, 11)
        assertActiveProcessing(historySummaryDto, 16, 18)
    }



    @Test
    fun howManyItemsInNextChunks() {
        val historySummaryDto = createHistorySummaryAndSetToClass()

        historySummaryDto.missedRanges.clear()

        val historyRequestInfo = HistoryRequestInfo(
            startSequence = 200,
            endSequence = 399
        )

        val items = unitToTest.howManyItemsInNextChunks(mutableListOf(historyRequestInfo, historyRequestInfo))

        assertEquals(400, items)
//        assertEquals(2100, historyRequestInfo.startSequence)
//        assertEquals(3000, historyRequestInfo.endSequence)

        //assertEquals(1, chuck[0])
        //assertEquals(0, historySummaryDto.missedRanges.size)

        //Assert.fail()
    }


    fun createHistorySummaryAndSetToClass(): HistorySummaryDto {
        val historySummaryDto: HistorySummaryDto = HistorySummaryDto(10044, 0, 0, 25000)
        setPrivateField(unitToTest, "historySummaryDto", historySummaryDto)

        return historySummaryDto
    }


    fun setPrivateField(obj: Any, fieldName: String, value: Any?) {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }

    private fun assertMissedRange(missedRanges: List<HistoryRange>, start: Int, end: Int) {
        val with7 = missedRanges.stream()
            .filter { item -> item.start == start.toLong() }
            .findFirst()

        assertEquals(true, with7.isPresent)
        assertEquals(end.toLong(), with7.get().end)
    }

    private fun assertMissedRange(historySummaryDto: HistorySummaryDto, start: Int, end: Int) {
        val with7 = historySummaryDto.missedRanges.stream()
            .filter { item -> item.start == start.toLong() }
            .findFirst()

        assertEquals(true, with7.isPresent)
        assertEquals(end.toLong(), with7.get().end)
    }

    private fun assertActiveProcessing(historySummaryDto: HistorySummaryDto, start: Int, end: Int) {
        val with7 = historySummaryDto.activeProcessing.stream()
            .filter { item -> item.startSequence == start.toLong() }
            .findFirst()

        assertEquals(true, with7.isPresent)
        assertEquals(end.toLong(), with7.get().endSequence)
    }

    private fun assertHistoryRange(historyRange: HistoryRange, start: Int, end: Int) {
        assertNotNull(historyRange)
        assertEquals(start.toLong(), historyRange.start)
        assertEquals(end.toLong(), historyRange.end)
    }

    private fun assertHistoryRequestInfo(historyRequestInfoList: MutableList<HistoryRequestInfo>, start: Int, end: Int) {
        val historyRequestInfo = historyRequestInfoList.stream()
            .filter { item -> item.startSequence == start.toLong() }
            .findFirst()

        assertTrue { historyRequestInfo.isPresent }
        assertHistoryRequestInfo(historyRequestInfo.get(), start, end)
    }




    private fun assertHistoryRequestInfo(historyRequestInfo: HistoryRequestInfo, start: Int, end: Int) {
        assertNotNull(historyRequestInfo)
        assertEquals(start.toLong(), historyRequestInfo.startSequence)
        assertEquals(end.toLong(), historyRequestInfo.endSequence)
    }

    private fun createMapOfHistoryLogs(logParameter: HistoryLogsParameter = HistoryLogsParameter.Any, vararg values: Long): Map<Long, HistoryLog> {
        val mapOfEntries: MutableMap<Long, HistoryLog> = mutableMapOf()

        val gc = GregorianCalendar()
        gc.add(GregorianCalendar.MINUTE, (-1) * (values.size * 5))

        if (logParameter==HistoryLogsParameter.LongBackDate) {
            gc.add(GregorianCalendar.MONTH, -2)
        }

        var pumpTimeSec = (gc.timeInMillis/1000).toInt()

        for (v in values) {
            mapOfEntries.put(v, AlarmActivatedHistoryLog(pumpTimeSec.toLong(), v, 1))
            pumpTimeSec += (5*60)
        }

        return mapOfEntries
    }

    enum class  HistoryLogsParameter {
        Any,
        LongBackDate

    }


}
