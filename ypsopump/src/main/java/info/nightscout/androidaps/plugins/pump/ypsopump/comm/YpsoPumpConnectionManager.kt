package info.nightscout.androidaps.plugins.pump.ypsopump.comm

import android.os.SystemClock
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.defs.PumpUpdateFragmentType
import info.nightscout.androidaps.plugins.pump.common.events.EventPumpFragmentValuesChanged
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.GetBasalProfile
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.GetDateTime
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.GetFirmwareVersion
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command.GetPumpSettings
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoBasalProfileType
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoPumpNotificationType
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.parameters.CommandParameters
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.SimpleDataCommandResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.TemporaryBasalResponse
import info.nightscout.androidaps.plugins.pump.ypsopump.connector.YpsoPumpConnectorInterface
import info.nightscout.androidaps.plugins.pump.ypsopump.connector.YpsoPumpDummyConnector
import info.nightscout.androidaps.plugins.pump.ypsopump.data.BasalProfileDto
import info.nightscout.androidaps.plugins.pump.ypsopump.data.DateTimeDto
import info.nightscout.androidaps.plugins.pump.ypsopump.data.YpsoPumpSettingsDto
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpConfigurationChanged
import info.nightscout.androidaps.plugins.pump.ypsopump.handlers.YpsoPumpHistoryHandler
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import info.nightscout.shared.sharedPreferences.SP
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YpsoPumpConnectionManager @Inject constructor(
    val pumpStatus: YpsopumpPumpStatus,
    val pumpUtil: YpsoPumpUtil,
    val sp: SP,
    val injector: HasAndroidInjector,
    val aapsLogger: AAPSLogger,
    val rxBus: RxBus,
    val fabricPrivacy: FabricPrivacy,
    val ypsoPumpBLE: YpsoPumpBLE,
    val ypsoPumpHistoryHandler: YpsoPumpHistoryHandler
) {

    //private val fabricPrivacy: FabricPrivacy
    private val baseConnector // YpsoPumpBaseConnector
        : YpsoPumpConnectorInterface
    private val selectedConnector: YpsoPumpConnectorInterface
    private val disposable = CompositeDisposable()
    private var oldFirmware: YpsoPumpFirmware? = null
    private var currentFirmware: YpsoPumpFirmware? = null
    var inConnectMode = false
    var inDisconnectMode = false

    // var deviceMac: String? = null
    // var deviceBonded: Boolean = false

    fun connectToPump(deviceMac: String, deviceBonded: Boolean): Boolean {

        if (pumpUtil.driverStatus === PumpDriverState.Ready) {
            return true
        }

        if (inConnectMode)
            return false;

        if (deviceMac.isNullOrEmpty() && !deviceBonded) {
            return false
        }

        inConnectMode = true

        // TODO
        //val deviceMac = "EC:2A:F0:00:8B:8E"

        //sp.getString()

        if (!ypsoPumpBLE.startConnectToYpsoPump(deviceMac!!)) {
            inConnectMode = false
            return false
        }

        val timeoutTime = System.currentTimeMillis() + (120 * 1000)
        var timeouted = true
        var driverStatus: PumpDriverState? = null

        while (System.currentTimeMillis() < timeoutTime) {
            SystemClock.sleep(5000)

            driverStatus = pumpUtil.driverStatus

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
            pumpStatus.isFirmwareSet = true
            this.currentFirmware = pumpStatus.ypsopumpFirmware
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
        pumpUtil.currentCommand = parameters.commandType
        pumpUtil.sleepSeconds(40)
        pumpUtil.driverStatus = PumpDriverState.Connected
        return CommandResponse.builder().success(true).build()
    }

    fun disconnectFromPump() {

        var driverStatus: PumpDriverState? = pumpUtil.driverStatus

        when (driverStatus) {
            PumpDriverState.NotInitialized,
            PumpDriverState.Initialized                -> {
                aapsLogger.warn(LTag.PUMPCOMM, "disconnectFromPump. Pump is in weird state ($driverStatus.name), exiting.")
                return;
            }

            PumpDriverState.ErrorCommunicatingWithPump -> {
                val errorType = pumpUtil.errorType
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
                pumpUtil.driverStatus = PumpDriverState.Sleeping
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

            driverStatus = pumpUtil.driverStatus

            aapsLogger.debug(LTag.PUMPCOMM, "disconnectFromPump: " + driverStatus.name)

            if (driverStatus == PumpDriverState.Disconnected || driverStatus == PumpDriverState.Sleeping) {
                timeouted = false
                break
            }
        }

        if (driverStatus == PumpDriverState.Disconnected) {
            pumpUtil.driverStatus = PumpDriverState.Sleeping
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
        aapsLogger.error(LTag.PUMP, "deliverBolus command is not available!!!")
        pumpUtil.currentCommand = YpsoPumpCommandType.SetBolus
        val commandResponse = selectedConnector.sendBolus(detailedBolusInfo!!) // TODO refactor this not to use AAPS object
        pumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    // TODO refactor this not to use AAPS object
    fun getTemporaryBasal(): TemporaryBasalResponse? {
        pumpUtil.currentCommand = YpsoPumpCommandType.GetTemporaryBasal
        val commandResponse = selectedConnector.retrieveTemporaryBasal() // TODO refactor this not to use AAPS object
        pumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    fun setTemporaryBasal(value: Int, duration: Int): CommandResponse? {
        aapsLogger.error(LTag.PUMP, "setTemporaryBasal command is not available!!!")
        pumpUtil.currentCommand = YpsoPumpCommandType.SetTemporaryBasal
        val commandResponse = selectedConnector.sendTemporaryBasal(value, duration) // TODO refactor this not to use AAPS object
        pumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    fun setBasalProfile(profile: Profile?): CommandResponse? {
        aapsLogger.error(LTag.PUMP, "setBasalProfile command is not available!!!")
        pumpUtil.currentCommand = YpsoPumpCommandType.SetBasalProfile
        val commandResponse = selectedConnector.sendBasalProfile(profile!!) // TODO refactor this not to use AAPS object
        pumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    fun sendFakeCommand(commandType: YpsoPumpCommandType): CommandResponse? {
        pumpUtil.currentCommand = commandType
        pumpUtil.sleepSeconds(10)
        pumpUtil.resetDriverStatusToConnected()
        return CommandResponse.builder().success(true).build()
    }

    fun cancelTemporaryBasal(): CommandResponse? {
        pumpUtil.currentCommand = YpsoPumpCommandType.CancelTemporaryBasal
        val commandResponse = selectedConnector.cancelTemporaryBasal() // TODO refactor this not to use AAPS object
        pumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    // TODO
    fun getRemainingInsulin(): SimpleDataCommandResponse? {
        //selectedConnector.retrieveRemainingInsulin()
        return SimpleDataCommandResponse.builder().errorDescription("Command Npt Supported yet!").build()
    }

    fun getConfiguration() {
        val command = GetPumpSettings(injector)

        command.execute(pumpBle = ypsoPumpBLE)

        if (command.isSuccessful) {
            val pumpSettings: YpsoPumpSettingsDto? = command.commandResponse

            if (pumpSettings != null) {
                pumpStatus.lastConfigurationUpdate = System.currentTimeMillis()

                if (pumpSettings.bolusIncStep != null) {
                    pumpStatus.bolusStep = pumpSettings.bolusIncStep!!
                    pumpStatus.pumpDescription.bolusStep = pumpStatus.bolusStep
                }

                if (pumpSettings.basalProfileType != null) {
                    pumpStatus.activeProfileName = if (pumpSettings.basalProfileType == YpsoBasalProfileType.BASAL_PROFILE_A) "A" else "B"

                    if (pumpSettings.basalProfileType == YpsoBasalProfileType.BASAL_PROFILE_B) {
                        pumpUtil.sendNotification(
                            notificationType = YpsoPumpNotificationType.PumpIncorrectBasalProfileSelected,
                            "A")
                    }
                }

                if (pumpSettings.bolusAmoutLimit != null) {
                    pumpStatus.maxBolus = pumpSettings.bolusIncStep!!
                    //pumpStatus.pumpDescription.bolusStep = pumpStatus.bolusStep
                }

                if (pumpSettings.basalRateLimit != null) {
                    pumpStatus.maxBasal = pumpSettings.bolusIncStep!!
                    pumpStatus.pumpDescription.basalMaximumRate = pumpStatus.maxBasal!!
                }
            }

        } else {
            aapsLogger.error(LTag.PUMPCOMM, "Problem reading configuration.")
            return
        }
    }

    // TODO
    val batteryStatus: SimpleDataCommandResponse?
        get() =// TODO
            null

    fun getBasalProfile(): Boolean {

        val command = GetBasalProfile(injector)

        command.execute(pumpBle = ypsoPumpBLE)

        if (command.isSuccessful) {
            val basalProfile: BasalProfileDto? = command.commandResponse

            if (basalProfile != null) {
                pumpStatus.basalsByHour = basalProfile.basalPatterns
                return true
            } else {
                aapsLogger.error(LTag.PUMPCOMM, "Null basal profile.")
            }
        } else {
            aapsLogger.error(LTag.PUMPCOMM, "Problem reading basal profile.")
        }

        return false;
    }

    fun getTime(): DateTimeDto? {
        val command = GetDateTime(injector)

        command.execute(pumpBle = ypsoPumpBLE)

        if (command.isSuccessful) {
            return command.commandResponse
        } else {
            aapsLogger.error(LTag.PUMPCOMM, "Problem getting Time from Pump.")
            return null
        }
    }

    fun setTime(): DateTimeDto? {
        TODO("Not yet implemented")
    }

    init {
        baseConnector = YpsoPumpDummyConnector(pumpStatus, pumpUtil, injector, aapsLogger) //new YpsoPumpBaseConnector(ypsopumpUtil, injector, aapsLogger);
        selectedConnector = baseConnector //new YpsoPumpDummyConnector(ypsopumpUtil, injector, aapsLogger);
        //this.fabricPrivacy = fabricPrivacy
        disposable.add(rxBus
                           .toObservable(EventPumpConfigurationChanged::class.java)
                           .observeOn(Schedulers.io())
                           .subscribe({ _ -> resetFirmwareVersion() }) { throwable: Throwable? -> fabricPrivacy.logException(throwable!!) }
        )
    }
}