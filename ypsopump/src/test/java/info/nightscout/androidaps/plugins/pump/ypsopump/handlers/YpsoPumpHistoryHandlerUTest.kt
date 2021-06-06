package info.nightscout.androidaps.plugins.pump.ypsopump.handlers

import android.util.Log
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.interfaces.PumpSync
import info.nightscout.androidaps.logging.AAPSLoggerTest
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpDataConverter
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.data.EventDto
import info.nightscout.androidaps.plugins.pump.ypsopump.data.HistoryEntryType
import info.nightscout.androidaps.plugins.pump.ypsopump.database.HistoryMapper
import info.nightscout.androidaps.plugins.pump.ypsopump.database.YpsoPumpHistory
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.internal.WhiteboxImpl
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

@RunWith(PowerMockRunner::class)
class YpsoPumpHistoryHandlerUTest {

    //var converter = YpsoPumpDataConverter()

    @Mock lateinit var ypsopumpPumpStatus: YpsopumpPumpStatus
    @Mock lateinit var ypsoPumpUtil: YpsoPumpUtil
    @Mock lateinit var historyMapper: HistoryMapper
    @Mock lateinit var hasAndroidInjector: HasAndroidInjector
    @Mock lateinit var ypsoPumpBLE: YpsoPumpBLE
    @Mock lateinit var ypsoPumpHistory: YpsoPumpHistory
    @Mock lateinit var pumpSync: PumpSync

    lateinit var converter: YpsoPumpDataConverter
    var aapsLogger = AAPSLoggerTest()
    lateinit var handler: YpsoPumpHistoryHandler

    @Before
    fun prepareMock() {
        MockitoAnnotations.initMocks(this)
        //Mockito.`when`(ypsopumpPumpStatus.ypsopumpFirmware).thenReturn(YpsoPumpFirmware.VERSION_1_5)

        handler = YpsoPumpHistoryHandler(ypsoPumpHistory = ypsoPumpHistory,
            pumpSync = pumpSync,
            aapsLogger = aapsLogger,
            pumpStatus = ypsopumpPumpStatus,
            historyMapper = historyMapper,
            hasAndroidInjector = hasAndroidInjector,
            pumpUtil = ypsoPumpUtil,
            ypsoPumpBLE = ypsoPumpBLE
        )

        converter = YpsoPumpDataConverter(ypsopumpPumpStatus, ypsoPumpUtil, aapsLogger)
    }

    @Test
    fun preProcessHistory() {

    }

    @Test
    fun preProcessBasalProfiles() {

        var events = readEvents()

        val msg = WhiteboxImpl.invokeMethod<List<EventDto>>(handler, "preProcessBasalProfiles", events)

        Assert.assertEquals(33, msg.size)
        //preProcessConfigurationItems
    }

    @Test
    fun preProcessDeliverySuspendItems_4() {
        var list: MutableList<EventDto> = mutableListOf(getEntry(4))
        aapsLogger.debug(LTag.PUMP, "preProcessDeliverySuspendItems_4: " + list)
    }

    @Test
    fun preProcessDeliverySuspendItems_3() {
        var list: MutableList<EventDto> = mutableListOf(getEntry(3))
        aapsLogger.debug(LTag.PUMP, "preProcessDeliverySuspendItems_3: " + list)
    }

    @Test
    fun preProcessDeliverySuspendItems_43() {
        var list: MutableList<EventDto> = mutableListOf(getEntry(4), getEntry(3))
        aapsLogger.debug(LTag.PUMP, "preProcessDeliverySuspendItems_43: " + list)
    }

    @Test
    fun preProcessDeliverySuspendItems_432() {
        var list: MutableList<EventDto> = mutableListOf(getEntry(4), getEntry(3), getEntry(2))
        aapsLogger.debug(LTag.PUMP, "preProcessDeliverySuspendItems_432: " + list)
    }

    @Test
    fun preProcessDeliverySuspendItems_321() {
        var list: MutableList<EventDto> = mutableListOf(getEntry(3), getEntry(2), getEntry(1))
        aapsLogger.debug(LTag.PUMP, "preProcessDeliverySuspendItems_321: " + list)
    }

    @Test
    fun preProcessDeliverySuspendItems_4321() {
        // 2, 1, 21, 212, 2121
        var list: MutableList<EventDto> = mutableListOf(getEntry(4), getEntry(3), getEntry(2), getEntry(1))
        aapsLogger.debug(LTag.PUMP, "preProcessDeliverySuspendItems_4321: " + list)
    }

    @Test
    fun preProcessDeliverySuspendItems_4231() {
        // 2, 1, 21, 212, 2121
        var list: MutableList<EventDto> = mutableListOf(getEntry(4), getEntry(2), getEntry(3), getEntry(1))
        aapsLogger.debug(LTag.PUMP, "preProcessDeliverySuspendItems_4231: " + list)
    }

    fun getEntry(index: Int): EventDto {
        var byteArray: ByteArray? = null
        when (index) {
            4 -> byteArray = ByteUtil.createByteArrayFromString("D9 30 2F 28 0E 0A 00 00 00 00 00 FB 00 00 00 03 00 19 96")
            3 -> byteArray = ByteUtil.createByteArrayFromString("D3 30 2F 28 0E 03 00 00 00 00 00 F9 00 00 00 04 00 21 30")
            2 -> byteArray = ByteUtil.createByteArrayFromString("39 BA 26 28 0E 03 00 00 00 00 00 AC 00 00 00 2E 00 FA D0")
            1 -> byteArray = ByteUtil.createByteArrayFromString("53 AA 26 28 0E 0A 00 00 00 00 00 99 00 00 00 30 00 42 8F")
        }
        // D9 30 2F 28 0E 0A 00 00 00 00 00 FB 00 00 00 03 00 19 96
        // D3 30 2F 28 0E 03 00 00 00 00 00 F9 00 00 00 04 00 21 30
        // 39 BA 26 28 0E 03 00 00 00 00 00 AC 00 00 00 2E 00 FA D0
        // 53 AA 26 28 0E 0A 00 00 00 00 00 99 00 00 00 30 00 42 8F

        return converter.decodeEvent(byteArray!!, HistoryEntryType.Event)!!

    }

    fun readEvents(): MutableList<EventDto> {

        val f: File = File("./src/test/resources/events.txt")

        var fileReader: BufferedReader = BufferedReader(FileReader(f))
        var line: String? = null

        Log.d("ZZ", "File: " + f.exists())

        var eventList: MutableList<EventDto> = mutableListOf()

        fileReader.forEachLine {
            //Log.d("ZZ", "Event: " + it)
            val data = ByteUtil.createByteArrayFromString(it)
            val event = converter.decodeEvent(data, HistoryEntryType.Event)
            //aapsLogger.debug(LTag.PUMP, "Event: " + event)
            eventList.add(event!!)
        }

        return eventList
    }

}