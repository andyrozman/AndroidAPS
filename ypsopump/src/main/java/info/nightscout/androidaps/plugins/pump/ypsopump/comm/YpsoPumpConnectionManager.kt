package info.nightscout.androidaps.plugins.pump.ypsopump.comm

import android.os.SystemClock
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.bus.RxBusWrapper
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.defs.PumpUpdateFragmentType
import info.nightscout.androidaps.plugins.pump.common.events.EventPumpFragmentValuesChanged
import info.nightscout.androidaps.plugins.pump.common.utils.DateTimeUtil
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.GetEvents
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.GetFirmwareVersion
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
import info.nightscout.androidaps.plugins.pump.ypsopump.handlers.YpsoPumpHistoryHandler
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.sharedPreferences.SP
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YpsoPumpConnectionManager @Inject constructor(val pumpStatus: YpsopumpPumpStatus,
                                                    val ypsopumpUtil: YpsoPumpUtil,
                                                    val sp: SP,
                                                    val injector: HasAndroidInjector,
                                                    val aapsLogger: AAPSLogger,
                                                    val rxBus: RxBusWrapper,
                                                    val fabricPrivacy: FabricPrivacy,
                                                    val ypsoPumpBLE: YpsoPumpBLE,
                                                    val ypsoPumpHistoryHandler: YpsoPumpHistoryHandler
) {

    //private val fabricPrivacy: FabricPrivacy
    private val baseConnector // YpsoPumpBaseConnector
        : YpsoPumpConnectorInterface
    private val selectedConnector: YpsoPumpConnectorInterface
    private val disposable = CompositeDisposable()
    private val oldFirmware: YpsoPumpFirmware? = null
    private val currentFirmware: YpsoPumpFirmware? = null
    var inConnectMode = false
    var inDisconnectMode = false

    fun connectToPump(): Boolean {

        if (ypsopumpUtil.driverStatus === PumpDriverState.Ready) {
            return true
        }

        if (inConnectMode)
            return false;

        inConnectMode = true

        // TODO
        val deviceMac = "EC:2A:F0:00:8B:8E"

        ypsoPumpBLE.startConnectToYpsoPump(deviceMac)

        val timeoutTime = System.currentTimeMillis() + (120 * 1000)
        var timeouted = true
        var driverStatus: PumpDriverState? = null

        while (System.currentTimeMillis() < timeoutTime) {
            SystemClock.sleep(5000)

            driverStatus = ypsopumpUtil.driverStatus

            aapsLogger.debug(LTag.PUMPCOMM, "connectToPump: " + driverStatus.name)

            if (driverStatus == PumpDriverState.Ready || driverStatus == PumpDriverState.ErrorCommunicatingWithPump) {
                timeouted = false
                break
            }
        }

        inConnectMode = false
        return true

//         // TODO if initialized use types connection, else use base one
//
// //        Thread thread = new Thread() {
// //            public void run() {
//         println("Thread Running")
//         aapsLogger.debug(LTag.PUMP, "!!!!!! Connect to Pump - Thread running")
//         ypsopumpUtil.driverStatus = PumpDriverState.Connecting
//         ypsopumpUtil.sleepSeconds(15)
//         ypsopumpUtil.driverStatus = PumpDriverState.Connected
//         ypsopumpUtil.sleepSeconds(5)
//         ypsopumpUtil.driverStatus = PumpDriverState.EncryptCommunication
//         ypsopumpUtil.sleepSeconds(5)
//         ypsopumpUtil.driverStatus = PumpDriverState.Ready

//            }
//        };
//
//        thread.start();

    }

    private fun resetFirmwareVersion() {}

    fun determineFirmwareVersion() {
        val command = GetFirmwareVersion(injector)

        command.execute(ypsoPumpBLE)

        if (command.isSuccessful) {
            pumpStatus.ypsopumpFirmware = command.commandResponse!!
            rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Configuration))
        } else {
            aapsLogger.error(LTag.PUMPCOMM, "Problem getting firmware version: " + command.bleCommOperationResult!!.operationResultType.name)
        }

    }

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

        var driverStatus: PumpDriverState? = ypsopumpUtil.driverStatus

        when (driverStatus) {
            PumpDriverState.NotInitialized,
            PumpDriverState.Initialized                -> {
                aapsLogger.warn(LTag.PUMPCOMM, "disconnectFromPump. Pump is in weird state ($driverStatus.name), exiting.")
                return;
            }

            PumpDriverState.ErrorCommunicatingWithPump -> {
                val errorType = ypsopumpUtil.errorType
                aapsLogger.warn(LTag.PUMPCOMM, "disconnectFromPump. Pump is in error ($errorType.name), exiting.")
                return;
            }

            PumpDriverState.Connecting,
            PumpDriverState.EncryptCommunication,
            PumpDriverState.ExecutingCommand,
            PumpDriverState.Busy,
            PumpDriverState.Suspended,
            PumpDriverState.Connected                  -> {
                aapsLogger.warn(LTag.PUMPCOMM, "disconnectFromPump. Pump seems to be in unallowed state ($driverStatus.name), exiting and setting to Sleep.")
                ypsopumpUtil.driverStatus = PumpDriverState.Sleeping
                return;
            }

            PumpDriverState.Disconnecting              -> {
                aapsLogger.warn(LTag.PUMPCOMM, "disconnectFromPump. Pump is already disconnecting, exiting.")
                return;
            }

            PumpDriverState.Sleeping,
            PumpDriverState.Disconnected               -> return

            //PumpDriverState.Ready                      -> TODO()
        }

        if (inDisconnectMode)
            return

        inDisconnectMode = true

        ypsoPumpBLE.disconnectFromYpsoPump()

        val timeoutTime = System.currentTimeMillis() + (120 * 1000)
        var timeouted = true
        //var driverStatus: PumpDriverState? = null

        while (System.currentTimeMillis() < timeoutTime) {
            SystemClock.sleep(5000)

            driverStatus = ypsopumpUtil.driverStatus

            aapsLogger.debug(LTag.PUMPCOMM, "disconnectFromPump: " + driverStatus.name)

            if (driverStatus == PumpDriverState.Disconnected || driverStatus == PumpDriverState.Sleeping) {
                timeouted = false
                break
            }
        }

        if (driverStatus == PumpDriverState.Disconnected) {
            ypsopumpUtil.driverStatus = PumpDriverState.Sleeping
        }

        inDisconnectMode = false

        // //Thread thread = new Thread() {
        // //  public void run() {
        // println("Thread Running")
        // aapsLogger.debug(LTag.PUMP, "Disconnect from Pump - Thread running")
        // ypsopumpUtil.driverStatus = PumpDriverState.Disconnecting
        // ypsopumpUtil.sleepSeconds(20)
        // ypsopumpUtil.driverStatus = PumpDriverState.Sleeping
        // pumpStatus.setLastCommunicationToNow()
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
    fun getTemporaryBasal(): TemporaryBasalResponse? {
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
    fun getRemainingInsulin(): SimpleDataCommandResponse? {
        //selectedConnector.retrieveRemainingInsulin()
        return SimpleDataCommandResponse.builder().errorDescription("Command Npt Supported yet!").build()
    }

    // TODO
    fun getConfiguration(): Unit {
        // TODO
    }

    // TODO
    val batteryStatus: SimpleDataCommandResponse?
        get() =// TODO
            null

    // TODO
    fun getPumpHistory() {

        val pumpStatusValues = pumpStatus.getPumpStatusValuesForSelectedPump()

        if (pumpStatusValues == null) {
            aapsLogger.debug(LTag.PUMP, "PumpStatusValues could not be loaded, skipping history reading.")
            return
        }

        if (true)
            return

        // events
        var targetDate: Long? = null

        if (pumpStatusValues!!.lastEventDate != null) {
            targetDate = DateTimeUtil.getATDWithAddedMinutes(pumpStatusValues.lastEventDate!!, -15)
        }

        var commandEvents = GetEvents(injector, targetDate, null)

        var result = commandEvents.execute(ypsoPumpBLE)

        if (result) {
            // TODO 1. process returned data  2. add it into database 3. update pumpStatusValues
        } else {
            return  // if any of commands fails we stop execution
        }

        // alarms

        // TODO systemEntry we are ignoring this for now

        // TODO write pumpStatusValues
    }

    //
    fun getPumpHistoryAfterEvent() {

        val pumpStatusValues = pumpStatus.getPumpStatusValuesForSelectedPump()

        if (pumpStatusValues == null) {
            aapsLogger.debug(LTag.PUMP, "PumpStatusValues could not be loaded, skipping history reading.")
            return
        }

        // events
        var commandEvents = GetEvents(hasAndroidInjector = injector,
            targetDate = null,
            eventSequenceNumber = pumpStatusValues.lastEventSequenceNumber)

        var result = commandEvents.execute(ypsoPumpBLE)

        if (result) {
            // TODO 1. process returned data  2. add it into database 3. update pumpStatusValues
        } else {
            return  // if any of commands fails we stop execution
        }

        // TODO write pumpStatusValues
    }

    fun getBasalProfile(): Boolean {
        // TODO
        return false;
    }

    init {
        baseConnector = YpsoPumpDummyConnector(pumpStatus, ypsopumpUtil, injector, aapsLogger) //new YpsoPumpBaseConnector(ypsopumpUtil, injector, aapsLogger);
        selectedConnector = baseConnector //new YpsoPumpDummyConnector(ypsopumpUtil, injector, aapsLogger);
        //this.fabricPrivacy = fabricPrivacy
        disposable.add(rxBus
            .toObservable(EventPumpConfigurationChanged::class.java)
            .observeOn(Schedulers.io())
            .subscribe({ event: EventPumpConfigurationChanged? -> resetFirmwareVersion() }) { throwable: Throwable? -> fabricPrivacy.logException(throwable!!) }
        )
    }
}