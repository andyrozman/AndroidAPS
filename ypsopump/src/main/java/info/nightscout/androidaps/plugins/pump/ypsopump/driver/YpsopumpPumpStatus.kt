package info.nightscout.androidaps.plugins.pump.ypsopump.driver

import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.interfaces.PumpDescription
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus
import info.nightscout.androidaps.plugins.pump.common.defs.BasalProfileStatus
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDeviceState
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.YpsoPumpStatusEntry
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.YpsoPumpStatusList
import info.nightscout.androidaps.plugins.pump.ypsopump.data.BasalProfileDto
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpConst
import info.nightscout.androidaps.utils.resources.ResourceHelper
import info.nightscout.shared.sharedPreferences.SP
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by andy on 1/4/21.
 */
@Singleton
class YpsopumpPumpStatus @Inject constructor(private val resourceHelper: ResourceHelper,
                                             private val sp: SP,
                                             private val rxBus: RxBus
) : PumpStatus(PumpType.YPSOPUMP) {

    lateinit var pumpDescription: PumpDescription
    var errorDescription: String? = null
    var ypsopumpFirmware: YpsoPumpFirmware = YpsoPumpFirmware.VERSION_1_5  // TODO we default to 1.0 here
    var isFirmwareSet = false
    @JvmField var baseBasalRate = 0.0
    var serialNumber: Long? = null
    var ypsoPumpStatusList: YpsoPumpStatusList? = null

    //        rileyLinkUtil.getRileyLinkHistory().add(new RLHistoryItem(pumpDeviceState, RileyLinkTargetDevice.MedtronicPump));
//
//        rxBus.send(new EventRileyLinkDeviceStatusChange(pumpDeviceState));
    //public Double maxBolus;
    //public Double maxBasal;
    // statuses
    var pumpDeviceState = PumpDeviceState.NeverContacted

    //@JvmField var tempBasalStart: Date? = null
    @JvmField var tempBasalPercent = 100
    @JvmField var tempBasalDuration = 0

    //var tempBasalEnd = 0L
    // TODO determine if this is needed
    var basalProfileStatus = BasalProfileStatus.NotInitialized
    var basalProfile: Profile? = null

    //var connectionStatus = YpsoConnectionStatus.NOT_CONNECTED
    var settingsServiceVersion: String? = null

    var lastConfigurationUpdate: Long = 0
    var configChanged: Boolean = false
    var bolusStep: Double = 0.1
    var maxBolus: Double? = null
    var maxBasal: Double? = null

    var forceRefreshBasalProfile: Boolean = true
    var basalProfilePump: BasalProfileDto? = null

    override fun initSettings() {
        activeProfileName = "A"
        reservoirRemainingUnits = 75.0
        reservoirFullUnits = 160
        batteryRemaining = 75
        lastConnection = sp.getLong(YpsoPumpConst.Statistics.LastGoodPumpCommunicationTime, 0L)
        lastDataTime = lastConnection
    }

    //var ypsoPumpStatusList: YpsoPumpStatusList? = null

    fun getPumpStatusValuesForSelectedPump(): YpsoPumpStatusEntry? {
        return ypsoPumpStatusList!!.map.get(serialNumber)
    }

    fun setPumpStatusValues(entry: YpsoPumpStatusEntry) {
        ypsoPumpStatusList!!.map.put(entry.serialNumber, entry)
        
    }

    val basalProfileForHour: Double
        get() {
            if (basalsByHour != null) {
                val c = GregorianCalendar()
                val hour = c[Calendar.HOUR_OF_DAY]
                return basalsByHour!![hour]
            }
            return 0.0
        }

    override val errorInfo: String
        get() = if (errorDescription == null) "-" else errorDescription!!

    init {
        initSettings()
    }
}