package info.nightscout.androidaps.plugins.pump.ypsopump.comm

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.parameters.CommandParameters
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.SimpleDataCommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.TemporaryBasalResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.connector.YpsoPumpConnectorInterface
import info.nightscout.androidaps.plugins.pump.ypsopump.connector.YpsoPumpDummyConnector
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpConfigurationChanged
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.sharedPreferences.SP
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YpsoPumpConnectionManager @Inject constructor(private val pumpStatus: YpsopumpPumpStatus,
                                                    private val ypsopumpUtil: YpsoPumpUtil,
                                                    private val sp: SP,
                                                    private val injector: HasAndroidInjector,
                                                    private val aapsLogger: AAPSLogger,
                                                    private val rxBus: RxBusWrapper,
                                                    fabricPrivacy: FabricPrivacy
) {

    private val fabricPrivacy: FabricPrivacy
    private val baseConnector // YpsoPumpBaseConnector
        : YpsoPumpConnectorInterface
    private val selectedConnector: YpsoPumpConnectorInterface
    private val disposable = CompositeDisposable()
    private val oldFirmware: YpsoPumpFirmware? = null
    private val currentFirmware: YpsoPumpFirmware? = null
    var inConnectMode = false

    fun connectToPump(): Boolean {

        if (ypsopumpUtil.driverStatus === PumpDriverState.Ready) {
            return true
        }

        if (inConnectMode)
            return false;

        inConnectMode = true

        // if (inConnectMode)
        //     return false;
        //
        // inConnectMode = true
        //
        //
        // inConnectMode = if (!inConnectMode) {
        //     if (ypsopumpUtil.driverStatus === PumpDriverState.Connected) {
        //         return true
        //     }
        //     true
        // } else {
        //     return false
        // }

        // if (ypsopumpUtil.driverStatus === PumpDriverState.Connected) {
        //     return true
        // }


        // TODO if initialized use types connection, else use base one

//        Thread thread = new Thread() {
//            public void run() {
        println("Thread Running")
        aapsLogger.debug(LTag.PUMP, "!!!!!! Connect to Pump - Thread running")
        ypsopumpUtil.driverStatus = PumpDriverState.Connecting
        ypsopumpUtil.sleepSeconds(15)
        ypsopumpUtil.driverStatus = PumpDriverState.Connected
        ypsopumpUtil.sleepSeconds(5)
        ypsopumpUtil.driverStatus = PumpDriverState.EncryptCommunication
        ypsopumpUtil.sleepSeconds(5)
        ypsopumpUtil.driverStatus = PumpDriverState.Ready

//            }
//        };
//
//        thread.start();
        inConnectMode = false
        return true
    }

    private fun resetFirmwareVersion() {}

    fun determineFirmwareVersion() {}

    fun executeCommand(parameters: CommandParameters): CommandResponse {

//        ypsopumpUtil.setDriverStatus(YpsoDriverStatus.Connecting);
//        ypsopumpUtil.sleepSeconds(5);
//
//        if (connectToPump()) {
//
//            ypsopumpUtil.setDriverStatus(YpsoDriverStatus.Connected);
//
//        } else {
//
//            return CommandResponse.builder().success(false).build();
//
//        }

        // TODO single only ATM
        ypsopumpUtil.currentCommand = parameters.commandType
        ypsopumpUtil.sleepSeconds(40)
        ypsopumpUtil.driverStatus = PumpDriverState.Connected
        return CommandResponse.builder().success(true).build()
    }

    fun disconnectFromPump() {

        //Thread thread = new Thread() {
        //  public void run() {
        println("Thread Running")
        aapsLogger.debug(LTag.PUMP, "Disconnect from Pump - Thread running")
        ypsopumpUtil.driverStatus = PumpDriverState.Disconnecting
        ypsopumpUtil.sleepSeconds(20)
        ypsopumpUtil.driverStatus = PumpDriverState.Sleeping
        pumpStatus.setLastCommunicationToNow()
        // }
        //};
    }

    fun deliverBolus(detailedBolusInfo: DetailedBolusInfo?): CommandResponse? {
        ypsopumpUtil.currentCommand = YpsoPumpCommandType.SetBolus
        val commandResponse = selectedConnector.sendBolus(detailedBolusInfo!!) // TODO refactor this not to use AAPS object
        ypsopumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    // TODO refactor this not to use AAPS object
    val temporaryBasal: TemporaryBasalResponse?
        get() {
            ypsopumpUtil.currentCommand = YpsoPumpCommandType.GetTemporaryBasal
            val commandResponse = selectedConnector.retrieveTemporaryBasal() // TODO refactor this not to use AAPS object
            ypsopumpUtil.resetDriverStatusToConnected()
            return commandResponse
        }

    fun setTemporaryBasal(value: Int, duration: Int): CommandResponse? {
        ypsopumpUtil.currentCommand = YpsoPumpCommandType.SetTemporaryBasal
        val commandResponse = selectedConnector.sendTemporaryBasal(value, duration) // TODO refactor this not to use AAPS object
        ypsopumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    fun setBasalProfile(profile: Profile?): CommandResponse? {
        ypsopumpUtil.currentCommand = YpsoPumpCommandType.SetBasalProfile
        val commandResponse = selectedConnector.sendBasalProfile(profile!!) // TODO refactor this not to use AAPS object
        ypsopumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    fun cancelTemporaryBasal(): CommandResponse? {
        ypsopumpUtil.currentCommand = YpsoPumpCommandType.CancelTemporaryBasal
        val commandResponse = selectedConnector.cancelTemporaryBasal() // TODO refactor this not to use AAPS object
        ypsopumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    // TODO
    val remainingInsulin: SimpleDataCommandResponse?
        get() =// TODO
            selectedConnector.retrieveRemainingInsulin()

    // TODO
    val configuration: Unit
        get() {
            // TODO
        }

    // TODO
    val batteryStatus: SimpleDataCommandResponse?
        get() =// TODO
            null

    // TODO
    val pumpHistory: Unit
        get() {
            // TODO
        }
    val basalProfile: Boolean
        get() = false

    init {
        baseConnector = YpsoPumpDummyConnector(ypsopumpUtil, injector, aapsLogger) //new YpsoPumpBaseConnector(ypsopumpUtil, injector, aapsLogger);
        selectedConnector = baseConnector //new YpsoPumpDummyConnector(ypsopumpUtil, injector, aapsLogger);
        this.fabricPrivacy = fabricPrivacy
        disposable.add(rxBus
            .toObservable(EventPumpConfigurationChanged::class.java)
            .observeOn(Schedulers.io())
            .subscribe({ event: EventPumpConfigurationChanged? -> resetFirmwareVersion() }) { throwable: Throwable? -> fabricPrivacy.logException(throwable!!) }
        )
    }
}