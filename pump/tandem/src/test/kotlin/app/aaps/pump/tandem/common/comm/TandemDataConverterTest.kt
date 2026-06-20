package app.aaps.pump.tandem.common.comm

import android.content.Context
import android.content.SharedPreferences
import app.aaps.core.interfaces.aps.APS
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.objects.extensions.pureProfileFromJson
import app.aaps.core.objects.profile.ProfileSealed
import app.aaps.pump.common.data.BasalProfileDto
import app.aaps.pump.common.driver.connector.commands.response.DataCommandResponse
import app.aaps.pump.tandem.common.data.IDPSegmentDto
import app.aaps.pump.tandem.common.data.PumpProfileDto
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import app.aaps.shared.impl.sharedPreferences.SPImpl
import app.aaps.shared.impl.utils.DateUtilImpl
import app.aaps.shared.tests.HardLimitsMock
import app.aaps.shared.tests.TestBase
import app.aaps.shared.tests.TestBaseWithProfile
import app.aaps.shared.tests.TestPumpPlugin
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.IDPSegmentResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.IDPSettingsResponse
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TandemDataConverterTest : TestBase() {

    @Mock lateinit var activePlugin: ActivePlugin
    @Mock lateinit var rh: ResourceHelper
    @Mock lateinit var context: Context
    @Mock lateinit var aps: APS
    @Mock lateinit var sp: SP
    @Mock lateinit var tandemPumpStatus: TandemPumpStatus
    @Mock lateinit var tandemPumpUtil : TandemPumpUtil

    @Mock lateinit var sharedPreferences: SharedPreferences


    var dateUtil : DateUtil? = null
    var unitToTest: TandemDataConverter? = null
    val gson : Gson = GsonBuilder().setPrettyPrinting().create()

    @BeforeEach
    fun prepare() {
        val testPumpPlugin = TestPumpPlugin(rh)

        sp = Mockito.mock(SP::class.java)
        tandemPumpUtil = Mockito.mock(TandemPumpUtil::class.java)
        tandemPumpStatus = Mockito.mock(TandemPumpStatus::class.java)


        //sharedPreferences = PreferenceManager(context)
        //sp = SPImpl(sharedPreferences, context)
        //tandemPumpUtil = TandemPumpUtil()

        unitToTest = TandemDataConverter(aapsLogger = aapsLogger,
                              sp = sp,
                              pumpStatus = tandemPumpStatus,
                              pumpUtil = tandemPumpUtil)
        dateUtil = DateUtilImpl(context)
        //sp = SPImpl()
        //hardLimits = HardLimitsMock(sp, preferences, rh)
        `when`(activePlugin.activePump).thenReturn(testPumpPlugin)
        `when`(rh.gs(app.aaps.core.ui.R.string.profile_per_unit)).thenReturn("/U")
        `when`(rh.gs(app.aaps.core.ui.R.string.profile_carbs_per_unit)).thenReturn("g/U")
        `when`(rh.gs(app.aaps.core.ui.R.string.profile_ins_units_per_hour)).thenReturn("U/h")
        `when`(rh.gs(anyInt(), anyString())).thenReturn("")
        `when`(activePlugin.activeAPS).thenReturn(aps)
        `when`(tandemPumpUtil.gson).thenReturn(gson)

    }


    @Test
    fun getTbrMap() {
    }

    @Test
    fun setTbrMap() {
    }

    @Test
    fun convertMessageToValue() {
    }

    @Test
    fun getInsulinStatus() {
    }

    @Test
    fun getTempBasalRate() {
    }

    @Test
    fun getBasalProfileResponse() {

        val settings = IDPSettingsResponse(1, "Basic", 6, 6, 25, true)
        val segments : MutableMap<Int, IDPSegmentResponse> = mutableMapOf()

        segments.put(0, IDPSegmentResponse(1, 0, 0, 800, 0, 0, 0, 0));
        segments.put(1, IDPSegmentResponse(1, 1, 5*60, 900, 0, 0, 0, 0));
        segments.put(2, IDPSegmentResponse(1, 2, 7*60, 1200, 0, 0, 0, 0));
        segments.put(3, IDPSegmentResponse(1, 3, 13*60, 1400, 0, 0, 0, 0));
        segments.put(4, IDPSegmentResponse(1, 4, 18*60, 2200, 0, 0, 0, 0));
        segments.put(5, IDPSegmentResponse(1, 5, 20*60, 3000, 0, 0, 0, 0));

        val profile = PumpProfileDto()

        profile.idpSettingsResponse = settings
        profile.mapSegments = segments


        val idpSegmentsFromProfile: DataCommandResponse<BasalProfileDto?> =
            this.unitToTest!!.getBasalProfileResponse(profile)

        val basalPatterns = idpSegmentsFromProfile.value!!.basalPatterns!!

        assertEquals(basalPatterns[0], 0.8)
        assertEquals(basalPatterns[1], 0.8)
        assertEquals(basalPatterns[2], 0.8)
        assertEquals(basalPatterns[3], 0.8)
        assertEquals(basalPatterns[4], 0.8)
        assertEquals(basalPatterns[5], 0.9)
        assertEquals(basalPatterns[6], 0.9)
        assertEquals(basalPatterns[7], 1.2)
        assertEquals(basalPatterns[8], 1.2)
        assertEquals(basalPatterns[9], 1.2)
        assertEquals(basalPatterns[10], 1.2)
        assertEquals(basalPatterns[11], 1.2)
        assertEquals(basalPatterns[12], 1.2)
        assertEquals(basalPatterns[13], 1.4)
        assertEquals(basalPatterns[14], 1.4)
        assertEquals(basalPatterns[15], 1.4)
        assertEquals(basalPatterns[16], 1.4)
        assertEquals(basalPatterns[17], 1.4)
        assertEquals(basalPatterns[18], 2.2)
        assertEquals(basalPatterns[19], 2.2)
        assertEquals(basalPatterns[20], 3.0)
        assertEquals(basalPatterns[21], 3.0)
        assertEquals(basalPatterns[22], 3.0)
        assertEquals(basalPatterns[23], 3.0)
    }

    private var okProfile = "{\"dia\":\"5\"," +
        "\"carbratio\":[{\"time\":\"00:00\",\"value\":\"30\"}," +
        "          {\"time\":\"08:00\",\"value\":\"50\"}," +
        "          {\"time\":\"12:00\",\"value\":\"20\"}," +
        "          {\"time\":\"19:00\",\"value\":\"60\"}]," +
        "\"sens\":[{\"time\":\"00:00\",\"value\":\"6\"}," +
        "          {\"time\":\"08:00\",\"value\":\"6.5\"}," +
        "          {\"time\":\"12:00\",\"value\":\"7\"}," +
        "          {\"time\":\"15:00\",\"value\":\"7.1\"}," +
        "          {\"time\":\"19:00\",\"value\":\"8\"}" +
        "]," +
        "\"timezone\":\"GMT\"," +
        "\"basal\":[{\"time\":\"00:00\",\"value\":\"0.8\"}," +
        "           {\"time\":\"05:00\",\"value\":\"0.9\"}," +
        "           {\"time\":\"07:00\",\"value\":\"1.2\"}," +
        "           {\"time\":\"13:00\",\"value\":\"1.4\"}," +
        "           {\"time\":\"18:00\",\"value\":\"2.2\"}," +
        "           {\"time\":\"20:00\",\"value\":\"3\"}" +
        "]," +
        "\"target_low\":[{\"time\":\"00:00\",\"value\":\"8\"}," +
        "                 {\"time\":\"06:00\",\"value\":\"6\"}," +
        "                 {\"time\":\"12:00\",\"value\":\"5\"}," +
        "                 {\"time\":\"19:00\",\"value\":\"6\"}]," +
        "\"target_high\":[{\"time\":\"00:00\",\"value\":\"8\"}," +
        "                 {\"time\":\"06:00\",\"value\":\"6\"}," +
        "                 {\"time\":\"12:00\",\"value\":\"5\"}," +
        "                 {\"time\":\"19:00\",\"value\":\"6\"}]," +
        "\"startDate\":\"1970-01-01T00:00:00.000Z\",\"units\":\"mmol\"}"


    @Test
    fun getIDPSegmentsFromProfile() {

        var profile = ProfileSealed.Pure(pureProfileFromJson(JSONObject(okProfile), dateUtil!!)!!, activePlugin)

        System.out.println("Profile ICS: ${profile.getIcsValues()}")

        val idpSegmentsFromProfile = this.unitToTest!!.getIDPSegmentsFromProfile(profile)

        assertTrue { idpSegmentsFromProfile.size == 6 }

            //val segment : IDPSegmentDto = idpSegmentsFromProfile[0]

        val gson : Gson = GsonBuilder().setPrettyPrinting().create();
        val strinJson : String  = gson.toJson(profile.getIsfsMgdlValues())

        System.out.println("ICS: ${strinJson}")

        checkSegment(segment = idpSegmentsFromProfile[0], timeAsHours = 0,
                     basalRate = 800, targetBg = 144, index = 0, sensitivity = 108, carbRatio = 30000)
        checkSegment(segment = idpSegmentsFromProfile[1], timeAsHours = 5,
                     basalRate = 900, targetBg = 144, index = 1, sensitivity = 108, carbRatio = 30000)
        checkSegment(segment = idpSegmentsFromProfile[2], timeAsHours = 7,
                     basalRate = 1200, targetBg = 108, index = 2, sensitivity = 108, carbRatio = 30000)
        checkSegment(segment = idpSegmentsFromProfile[3], timeAsHours = 13,
                     basalRate = 1400, targetBg = 90, index = 3, sensitivity = 126, carbRatio = 20000)
        checkSegment(segment = idpSegmentsFromProfile[4], timeAsHours = 18,
                     basalRate = 2200, targetBg = 90, index = 4, sensitivity = 127, carbRatio = 20000)
        checkSegment(segment = idpSegmentsFromProfile[5], timeAsHours = 20,
                     basalRate = 3000, targetBg = 108, index = 5, sensitivity = 144, carbRatio = 60000)

        // "\"sens\":[{\"time\":\"00:00\",\"value\":\"6\"}," +
        //     "          {\"time\":\"08:00\",\"value\":\"6.5\"}," +
        //     "          {\"time\":\"12:00\",\"value\":\"7\"}," +
        //     "          {\"time\":\"15:00\",\"value\":\"7.1\"}," +
        //     "          {\"time\":\"19:00\",\"value\":\"8\"}" +
        //     "]," +


    }

    fun checkSegment(segment : IDPSegmentDto, index: Int, timeAsHours: Int, basalRate: Int, targetBg: Int, sensitivity: Int, carbRatio: Long) {
        assertEquals(timeAsHours*60, segment.profileStartTime)
        assertEquals(basalRate, segment.profileBasalRate)
        assertEquals(targetBg, segment.profileTargetBG)
        assertEquals(index, segment.segmentIndex)
        assertEquals(sensitivity, segment.profileISF)
        assertEquals(carbRatio, segment.profileCarbRatio)
    }


    @Test
    fun getBatteryResponse() {
    }

    @Test
    fun decodeHistoryLogs() {
    }

    @Test
    fun decodeHistoryLog() {
    }

    @Test
    fun createHistoryLogDto() {
    }

    @Test
    fun getAapsLogger() {
    }

    @Test
    fun setAapsLogger() {
    }



    @Test
    fun getPumpStatus() {
    }

    @Test
    fun setPumpStatus() {
    }

    @Test
    fun getPumpUtil() {
    }

    @Test
    fun setPumpUtil() {
    }
}