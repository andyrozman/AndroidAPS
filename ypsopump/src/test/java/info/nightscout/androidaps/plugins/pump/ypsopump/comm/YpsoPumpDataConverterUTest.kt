package info.nightscout.androidaps.plugins.pump.ypsopump.comm

import android.util.Log
import info.nightscout.androidaps.logging.AAPSLoggerTest
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.HistoryEntryType
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.modules.junit4.PowerMockRunner
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.experimental.and

@RunWith(PowerMockRunner::class)
class YpsoPumpDataConverterUTest {

    @Mock lateinit var ypsopumpPumpStatus: YpsopumpPumpStatus
    @Mock lateinit var ypsoPumpUtil: YpsoPumpUtil

    lateinit var converter: YpsoPumpDataConverter


    @Before
    fun prepareMock() {
        MockitoAnnotations.initMocks(this)
        //Mockito.`when`(ypsopumpPumpStatus.ypsopumpFirmware).thenReturn(YpsoPumpFirmware.VERSION_1_5)

        converter = YpsoPumpDataConverter(ypsopumpPumpStatus, ypsoPumpUtil, AAPSLoggerTest())
        //..gs(Mockito.anyInt(), Mockito.anyLong())).thenReturn("")
    }

    @Test
    fun convertDate() {
        //E5 07 05 09
        //{"day":9,"hour":0,"minute":0,"month":5,"second":0,"year":-27}
        val decodeDate = converter.decodeDate(byteArrayOf(0xE5.toByte(), 0x07, 0x05, 0x09))

        Assert.assertEquals(9, decodeDate.day)
        Assert.assertEquals(5, decodeDate.month)
        Assert.assertEquals(2021, decodeDate.year)
    }

    @Test
    fun convertTime() {
        // 13 12 2C
        // {"day":0,"hour":19,"minute":18,"month":0,"second":44,"year":0}
        val decodeTime = converter.decodeTime(byteArrayOf(0x13, 0x12, 0x2c))

        Assert.assertEquals(19, decodeTime.hour)
        Assert.assertEquals(18, decodeTime.minute)
        Assert.assertEquals(44, decodeTime.second)


    }

    @Test
    fun decodeMasterSoftwareVersion() {
        // 56 30 32 2E 30 30 2E 32 35 00

        val version = converter.decodeMasterSoftwareVersion(byteArrayOf(0x56, 0x30, 0x32, 0x2E, 0x30, 0x30, 0x2E, 0x32, 0x35, 0x00))


        Log.d("HHH", "DDD:" + version)

        Assert.assertEquals(YpsoPumpFirmware.VERSION_1_5, version)

    }

    @Test
    fun test1() {
        // 44 0F 30 28 0A 50 00 1E 00 00 00 01 01 00 00 00 00 A1 BD
        // TBR 80% 0:30h  16:24  13/05/2021
        // ALAARM - Batery Removed 00:03  01/01/2016

        //var paBuffer: ByteArray = byteArrayOf(0x44, 0x0F, 0x30, 0x28, 0x0A, 0x50, 0x00, 0x1E, 0x00, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0xA1.toByte(), 0xBD.toByte())
        var paBuffer: ByteArray = byteArrayOf(0xF3.toByte(), 0xA0.toByte(), 0x2D, 0x28, 0x07, 0x17, 0x00, 0x5A, 0x00, 0x00, 0x00, 0xE7.toByte(), 0x00, 0x00, 0x00, 0x0C, 0x00, 0x53, 0x12)

        //CheckCRC(paBuffer)

        for (byteEntry in paBuffer) {
            // val year = 2000 + (byteEntry and 0xFF.toByte())
            Log.d("HH", "Ev: " + YpsoPumpEventType.getByCode(byteEntry.toInt()))


            //Log.d("HH", "Year: " + year)
            //Log.d("HH", "Year2: " + byteEntry)
        }
        val x = 2

        val year = 2000 + (paBuffer[0 + x] and 0xFF.toByte())
        val month = ByteUtil.toInt(paBuffer[1 + x]) // and 0xFF.toByte()).toInt()
        val day = ByteUtil.toInt((paBuffer[2 + x]))// and 0xFF.toByte()).toInt()
        val hour = ByteUtil.toInt((paBuffer[3 + x]))// and 0xFF.toByte()).toInt()
        val minute = ByteUtil.toInt((paBuffer[4 + x]))// and 0xFF.toByte()).toInt()
        val second = ByteUtil.toInt(paBuffer[5 + x]) // and 0xFF.toByte()).toInt()

        Log.d("D", "" + day + "." + month + "." + year + "  " + hour + ":" + minute + ":" + second)


//        if (cCRC32_YpsopumpImport.ComputeChecksum(byteArrayOf(
//                        paBuffer[3],
//                        paBuffer[2],
//                        paBuffer[1],
//                        paBuffer[0],
//                        paBuffer[7],
//                        paBuffer[6],
//                        paBuffer[5],
//                        paBuffer[4],
//                        paBuffer[11],
//                        paBuffer[10],
//                        paBuffer[9],
//                        paBuffer[8],
//                        paBuffer[15],
//                        paBuffer[14],
//                        paBuffer[13],
//                        paBuffer[12],
//                        0,
//                        0,
//                        0,
//                        paBuffer[16]
//                )) and 0xFFFF != charValueAsNumber) {


//        var piEventCounterNumber = getCharValueAsNumber(paBuffer, 6, 2).toShort();
//        var piEventIndex = getCharValueAsNumber(paBuffer, 8, 2);
//        var piEntryType = getCharValueAsNumber(paBuffer, 10, 1);
//        var entryType = YpsoPumpEventType.getByCode(piEntryType)
//        var piEntryValue1 = getCharValueAsNumber(paBuffer, 11, 2);
//        var piEntryValue2 = getCharValueAsNumber(paBuffer, 13, 2);
//        var piEntryValue3 = getCharValueAsNumber(paBuffer, 15, 2);
//        var poDateTimeLocal = DateTimeDto(year, month, day, hour, minute, second);
        // 19 -> 17

        // 20:09 11/05/2021

//        F3 A0 2D 28 07 17 00 5A 00 00 00 E7 00 00 00 0C 00 53 12
//        F3 A0 2D 28 07 16 00 59 00 00 00 E6 00 00 00 0D 00 10 22
//        F3 A0 2D 28 07 15 00 6E 00 00 00 E5 00 00 00 0E 00 44 48
//        F3 A0 2D 28 07 14 00 6E 00 00 00 E4 00 00 00 0F 00 49 B9
//        F3 A0 2D 28 07 13 00 6E 00 00 00 E3 00 00 00 10 00 9F 14
//        F3 A0 2D 28 07 12 00 6E 00 00 00 E2 00 00 00 11 00 92 E5
//        F3 A0 2D 28 07 11 00 6E 00 00 00 E1 00 00 00 12 00 85 F6
//        F3 A0 2D 28 07 10 00 4B 00 00 00 E0 00 00 00 13 00 71 C8
//        F3 A0 2D 28 07 0F 00 4B 00 00 00 DF 00 00 00 14 00 68 80
//        F3 A0 2D 28 07 0E 00 4B 00 00 00 DE 00 00 00 15 00 65 71
//        F2 A0 2D 28 07 0D 00 5F 00 00 00 DD 00 00 00 16 00 09 67
//        F2 A0 2D 28 07 0C 00 5F 00 00 00 DC 00 00 00 17 00 04 96
//        F2 A0 2D 28 07 0B 00 5F 00 00 00 DB 00 00 00 18 00 19 9C
//        F2 A0 2D 28 07 0A 00 5F 00 00 00 DA 00 00 00 19 00 14 6D
//        F2 A0 2D 28 07 09 00 32 00 00 00 D9 00 00 00 1A 00 7C 50
//        F2 A0 2D 28 07 08 00 32 00 00 00 D8 00 00 00 1B 00 71 A1
//        F2 A0 2D 28 07 07 00 32 00 00 00 D7 00 00 00 1C 00 04 23
//        F2 A0 2D 28 07 06 00 32 00 00 00 D6 00 00 00 1D 00 09 D2
//        F2 A0 2D 28 07 05 00 1E 00 00 00 D5 00 00 00 1E 00 BA 55
//        F2 A0 2D 28 07 04 00 1E 00 00 00 D4 00 00 00 1F 00 B7 A4
//        F2 A0 2D 28 07 03 00 1E 00 00 00 D3 00 00 00 20 00 40 5B
//        F2 A0 2D 28 07 02 00 1E 00 00 00 D2 00 00 00 21 00 4D AA
//        F2 A0 2D 28 07 01 00 1E 00 00 00 D1 00 00 00 22 00 5A B9
//        F2 A0 2D 28 07 00 00 0F 00 00 00 D0 00 00 00 23 00 A3 3F


    }

    //@Test
    fun decodeEvents() {
        val f: File = File("./src/test/resources/events.txt")

        var fileReader: BufferedReader = BufferedReader(FileReader(f))
        var line: String? = null

        Log.d("ZZ", "File: " + f.exists())

        fileReader.forEachLine {
            //Log.d("ZZ", "Event: " + it)
            val data = ByteUtil.createByteArrayFromString(it)
            val event = converter.decodeEvent(data, HistoryEntryType.Event)
            Log.d("ZZ", "Event: " + event)
        }

//        while (line in fileReader.lines()) {
//            Log.d("ZZ", "Event: " + line)
//
////            val data = ByteUtil.createByteArrayFromCompactString(line)
////            val event = converter.decodeEvent(data)
////            Log.d("ZZ", "Event: " + event)
//        }

        Log.d("DDD", "File: " + f.absolutePath + " exists:" + f.exists())
    }

    protected fun getCharValueAsNumber(paBuffer: ByteArray, piLength: Int): Int {
        return getCharValueAsNumber(paBuffer, 0, piLength)
    }

    protected fun getCharValueAsNumber(paBuffer: ByteArray, piStart: Int, piLength: Int): Int {
        var num = 0
        for (num2 in piLength - 1 downTo -1 + 1) {
            num = num shl 8
            num = num.toInt() or (paBuffer[num2 + piStart] and 0xFF.toByte()).toInt()
        }
        return num
    }
}