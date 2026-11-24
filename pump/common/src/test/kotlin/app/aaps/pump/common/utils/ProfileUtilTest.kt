package app.aaps.pump.common.utils

import android.content.Context
import app.aaps.core.data.pump.defs.PumpType
import app.aaps.core.interfaces.aps.APS
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.objects.extensions.pureProfileFromJson
import app.aaps.core.objects.profile.ProfileSealed
import app.aaps.shared.impl.utils.DateUtilImpl
import app.aaps.shared.tests.TestBase
import app.aaps.shared.tests.TestPumpPlugin
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ProfileUtilTest : TestBase() {

    @Mock lateinit var activePlugin: ActivePlugin
    @Mock lateinit var rh: ResourceHelper
    @Mock lateinit var context: Context
    @Mock lateinit var aps: APS
    @Mock lateinit var sp: SP


    var dateUtil : DateUtil? = null

    @BeforeEach
    fun prepare() {
        val testPumpPlugin = TestPumpPlugin(rh)

        sp = Mockito.mock(SP::class.java)
        dateUtil = DateUtilImpl(context)
        `when`(activePlugin.activePump).thenReturn(testPumpPlugin)
        `when`(rh.gs(anyInt(), anyString())).thenReturn("")
        `when`(activePlugin.activeAPS).thenReturn(aps)
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
    fun getBasalProfilesDisplayableAsStringOfArray() {

        var profile = ProfileSealed.Pure(pureProfileFromJson(JSONObject(okProfile), dateUtil!!)!!, activePlugin)


        val profileAsString = ProfileUtil.getBasalProfilesDisplayableAsStringOfArray(profile, PumpType.TANDEM_MOBI_BT)

        System.out.println("Profile: $profileAsString")

        assertEquals("0.800 0.800 0.800 0.800 0.800 0.900 0.900 1.200 1.200 1.200 1.200 1.200 1.200 1.400 1.400 1.400 1.400 1.400 2.200 2.200 3.000 3.000 3.000 3.000", profileAsString)

    }

    @Test
    fun getBasalProfilesDisplayableAsStringOfArrayV2() {

        var profile = ProfileSealed.Pure(pureProfileFromJson(JSONObject(okProfile), dateUtil!!)!!, activePlugin)


        val profileAsString = ProfileUtil.getBasalProfilesDisplayableAsStringOfArrayV2(profile, PumpType.TANDEM_MOBI_BT)

        System.out.println("Profile: $profileAsString")

        assertEquals("0.800 0.800 0.800 0.800 0.800 0.900 0.900 1.200 1.200 1.200 1.200 1.200 1.200 1.400 1.400 1.400 1.400 1.400 2.200 2.200 3.000 3.000 3.000 3.000", profileAsString)

    }


    @Test
    fun getProfilesByHourToString() {
        var resultArray: DoubleArray = doubleArrayOf(0.8, 0.8, 0.8, 0.8, 0.8, 0.9, 0.9, 1.2, 1.2, 1.2, 1.2, 1.2,
                                                     1.2, 1.4, 1.4, 1.4, 1.4, 1.4, 2.2, 2.2, 3.0, 3.0, 3.0, 3.0)

        val profileByHourAsString = ProfileUtil.getProfilesByHourToString(resultArray)

        assertEquals("0.800 0.800 0.800 0.800 0.800 0.900 0.900 1.200 1.200 1.200 1.200 1.200 1.200 1.400 1.400 1.400 1.400 1.400 2.200 2.200 3.000 3.000 3.000 3.000", profileByHourAsString)
    }


}