package info.nightscout.androidaps.plugins.pump.tandem.driver

import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpConst
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpApiVersion
import info.nightscout.interfaces.profile.Profile
import info.nightscout.interfaces.pump.defs.PumpDescription
import info.nightscout.interfaces.pump.defs.PumpType
import info.nightscout.pump.common.data.PumpStatus
import info.nightscout.pump.common.defs.BasalProfileStatus
import info.nightscout.pump.core.defs.PumpDeviceState
import info.nightscout.rx.bus.RxBus
import info.nightscout.shared.interfaces.ResourceHelper
import info.nightscout.shared.sharedPreferences.SP
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by andy on 1/4/21.
 */
@Singleton
class TandemPumpStatus @Inject constructor(private val resourceHelper: ResourceHelper,
                                           private val sp: SP,
                                           private val rxBus: RxBus
) : PumpStatus(PumpType.TANDEM_T_SLIM_X2_BT) {

    lateinit var pumpDescription: PumpDescription
    var errorDescription: String? = null
    var tandemPumpFirmware: TandemPumpApiVersion = TandemPumpApiVersion.VERSION_2_1
    var isFirmwareSet = false
    @JvmField var baseBasalRate = 0.0
    var serialNumber: Long = 0
    //var ypsoPumpStatusList: YpsoPumpStatusList? = null


    // statuses
    var pumpDeviceState = PumpDeviceState.NeverContacted

    // TODO determine if this is needed
    var basalProfileStatus = BasalProfileStatus.NotInitialized
    var basalProfile: Profile? = null

    //var connectionStatus = YpsoConnectionStatus.NOT_CONNECTED
    //var settingsServiceVersion: String? = null

    // var lastConfigurationUpdate: Long = 0
    // var configChanged: Boolean = false
    var bolusStep: Double = 0.1
    var maxBolus: Double? = null
    var maxBasal: Double? = null

    //var forceRefreshBasalProfile: Boolean = true
    //var basalProfilePump: BasalProfileDto? = null

    override fun initSettings() {
        activeProfileName = "A"
        reservoirRemainingUnits = 75.0
        reservoirFullUnits = 200
        batteryRemaining = 75
        lastConnection = sp.getLong(TandemPumpConst.Statistics.LastGoodPumpCommunicationTime, 0L)
        lastDataTime = lastConnection
    }

    //var ypsoPumpStatusList: YpsoPumpStatusList? = null

    // fun getPumpStatusValuesForSelectedPump(): YpsoPumpStatusEntry? {
    //     return ypsoPumpStatusList!!.map.get(serialNumber)
    // }
    //
    // fun setPumpStatusValues(entry: YpsoPumpStatusEntry) {
    //     ypsoPumpStatusList!!.map.put(entry.serialNumber, entry)
    //
    // }

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