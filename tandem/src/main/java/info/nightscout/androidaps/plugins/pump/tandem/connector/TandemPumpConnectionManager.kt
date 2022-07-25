package info.nightscout.androidaps.plugins.pump.tandem.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.interfaces.Profile
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.common.driver.connector.PumpConnectorInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.ResultCommandResponse
import info.nightscout.androidaps.plugins.pump.common.data.DateTimeDto
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.tandem.defs.TandemPumpApiVersion
import info.nightscout.androidaps.plugins.pump.tandem.driver.TandemPumpStatus
import info.nightscout.androidaps.plugins.pump.tandem.event.EventPumpConfigurationChanged
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import info.nightscout.shared.sharedPreferences.SP
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TandemPumpConnectionManager @Inject constructor(
    val pumpStatus: TandemPumpStatus,
    val pumpUtil: TandemPumpUtil,
    val sp: SP,
    val injector: HasAndroidInjector,
    val aapsLogger: AAPSLogger,
    val rxBus: RxBus,
    val fabricPrivacy: FabricPrivacy,
    //val ypsoPumpBLE: YpsoPumpBLE,
    //val ypsoPumpHistoryHandler: YpsoPumpHistoryHandler
) {

    //private val fabricPrivacy: FabricPrivacy
    private val baseConnector // YpsoPumpBaseConnector
        : PumpConnectorInterface
    private val selectedConnector: PumpConnectorInterface
    private val disposable = CompositeDisposable()
    private var oldFirmware: TandemPumpApiVersion? = null
    private var currentFirmware: TandemPumpApiVersion? = null
    var inConnectMode = false
    var inDisconnectMode = false

    // var deviceMac: String? = null
    // var deviceBonded: Boolean = false

    fun connectToPump(): Boolean {

        // TODO handle states
        aapsLogger.debug(LTag.PUMP, "!!!!!! Connect to Pump")
        pumpUtil.driverStatus = PumpDriverState.Connecting
        // pumpUtil.sleepSeconds(15)

        val connected = selectedConnector.connectToPump()

        if (connected) {
            pumpUtil.driverStatus = PumpDriverState.Connected
            pumpUtil.driverStatus = PumpDriverState.Ready
        } else {
            pumpUtil.driverStatus = PumpDriverState.ErrorCommunicatingWithPump
        }

        // pumpUtil.driverStatus = PumpDriverState.Connected
        // pumpUtil.sleepSeconds(5)
        // pumpUtil.driverStatus = PumpDriverState.EncryptCommunication
        // pumpUtil.sleepSeconds(5)
        // pumpUtil.driverStatus = PumpDriverState.Ready


        // Thread thread = new Thread() {
        //     public void run() {
        //         println("Thread Running")
        //         aapsLogger.debug(LTag.PUMP, "!!!!!! Connect to Pump - Thread running")
        //         ypsopumpUtil.driverStatus = PumpDriverState.Connecting
        //         ypsopumpUtil.sleepSeconds(15)
        //         ypsopumpUtil.driverStatus = PumpDriverState.Connected
        //         ypsopumpUtil.sleepSeconds(5)
        //         ypsopumpUtil.driverStatus = PumpDriverState.EncryptCommunication
        //         ypsopumpUtil.sleepSeconds(5)
        //         ypsopumpUtil.driverStatus = PumpDriverState.Ready
        //
        //     }
        // };
        //
        // thread.start();

        return connected
    }


//     fun connectToPump(deviceMac: String, deviceBonded: Boolean): Boolean {
//
//         if (pumpUtil.driverStatus === PumpDriverState.Ready) {
//             return true
//         }
//
//         if (inConnectMode)
//             return false;
//
//         if (deviceMac.isNullOrEmpty() && !deviceBonded) {
//             return false
//         }
//
//         inConnectMode = true
//
//         // TODO
//         //val deviceMac = "EC:2A:F0:00:8B:8E"
//
//         //sp.getString()
//
//         if (!ypsoPumpBLE.startConnectToYpsoPump(deviceMac)) {
//             inConnectMode = false
//             return false
//         }
//
//         val timeoutTime = System.currentTimeMillis() + (120 * 1000)
//         var timeouted = true
//         var driverStatus: PumpDriverState?
//
//         while (System.currentTimeMillis() < timeoutTime) {
//             SystemClock.sleep(5000)
//
//             driverStatus = pumpUtil.driverStatus
//
//             aapsLogger.debug(LTag.PUMPCOMM, "connectToPump: " + driverStatus.name)
//
//             if (driverStatus == PumpDriverState.Ready || driverStatus == PumpDriverState.ErrorCommunicatingWithPump) {
//                 timeouted = false
//                 break
//             }
//         }
//
//         inConnectMode = false
//         return true
//
// //         // TODO if initialized use types connection, else use base one
// //
// // //        Thread thread = new Thread() {
// // //            public void run() {
// //         println("Thread Running")
// //         aapsLogger.debug(LTag.PUMP, "!!!!!! Connect to Pump - Thread running")
// //         ypsopumpUtil.driverStatus = PumpDriverState.Connecting
// //         ypsopumpUtil.sleepSeconds(15)
// //         ypsopumpUtil.driverStatus = PumpDriverState.Connected
// //         ypsopumpUtil.sleepSeconds(5)
// //         ypsopumpUtil.driverStatus = PumpDriverState.EncryptCommunication
// //         ypsopumpUtil.sleepSeconds(5)
// //         ypsopumpUtil.driverStatus = PumpDriverState.Ready
//
// //            }
// //        };
// //
// //        thread.start();
//
//     }

    private fun resetFirmwareVersion() {}

    fun determineFirmwareVersion() {

        // TODO Tandem
    }

//     fun executeCommand(parameters: CommandParameters): ResultCommandResponse {
//
// //        ypsopumpUtil.setDriverStatus(YpsoDriverStatus.Connecting);
// //        ypsopumpUtil.sleepSeconds(5);
// //
// //        if (connectToPump()) {
// //
// //            ypsopumpUtil.setDriverStatus(YpsoDriverStatus.Connected);
// //
// //        } else {
// //
// //            return CommandResponse.builder().success(false).build();
// //
// //        }
//
//         // TODO single only ATM
//         pumpUtil.currentCommand = parameters.commandType
//         pumpUtil.sleepSeconds(40)
//         pumpUtil.driverStatus = PumpDriverState.Connected
//         return true
//     }

    fun disconnectFromPump(): Boolean {

        // TODO handle states
        return selectedConnector.disconnectFromPump()



        // var driverStatus: PumpDriverState? = pumpUtil.driverStatus
        //
        // when (driverStatus) {
        //     PumpDriverState.NotInitialized,
        //     PumpDriverState.Initialized                -> {
        //         aapsLogger.warn(LTag.PUMPCOMM, "disconnectFromPump. Pump is in weird state ($driverStatus.name), exiting.")
        //         return true;
        //     }
        //
        //     PumpDriverState.ErrorCommunicatingWithPump -> {
        //         val errorType = pumpUtil.errorType
        //         aapsLogger.warn(LTag.PUMPCOMM, "disconnectFromPump. Pump is in error ($errorType.name), exiting.")
        //         return true;
        //     }
        //
        //     PumpDriverState.Connecting,
        //     PumpDriverState.EncryptCommunication,
        //     PumpDriverState.ExecutingCommand,
        //     PumpDriverState.Busy,
        //     PumpDriverState.Suspended,
        //     PumpDriverState.Connected                  -> {
        //         aapsLogger.warn(LTag.PUMPCOMM, "disconnectFromPump. Pump seems to be in unallowed state ($driverStatus.name), exiting and setting to Sleep.")
        //         pumpUtil.driverStatus = PumpDriverState.Sleeping
        //         return;
        //     }
        //
        //     PumpDriverState.Disconnecting              -> {
        //         aapsLogger.warn(LTag.PUMPCOMM, "disconnectFromPump. Pump is already disconnecting, exiting.")
        //         return true
        //     }
        //
        //     PumpDriverState.Sleeping,
        //     PumpDriverState.Disconnected,
        //     PumpDriverState.Ready                        -> return true
        //
        //     null -> {}
        // }
        //
        // if (inDisconnectMode)
        //     return true
        //
        // inDisconnectMode = true
        //
        // ypsoPumpBLE.disconnectFromYpsoPump()
        //
        // val timeoutTime = System.currentTimeMillis() + (120 * 1000)
        // var timeouted = true
        // //var driverStatus: PumpDriverState? = null
        //
        // while (System.currentTimeMillis() < timeoutTime) {
        //     SystemClock.sleep(5000)
        //
        //     driverStatus = pumpUtil.driverStatus
        //
        //     aapsLogger.debug(LTag.PUMPCOMM, "disconnectFromPump: " + driverStatus.name)
        //
        //     if (driverStatus == PumpDriverState.Disconnected || driverStatus == PumpDriverState.Sleeping) {
        //         timeouted = false
        //         break
        //     }
        // }
        //
        // if (driverStatus == PumpDriverState.Disconnected) {
        //     pumpUtil.driverStatus = PumpDriverState.Sleeping
        // }
        //
        // inDisconnectMode = false

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

    fun deliverBolus(detailedBolusInfo: DetailedBolusInfo?): ResultCommandResponse? {
        aapsLogger.error(LTag.PUMP, "deliverBolus command is not available!!!")
        pumpUtil.currentCommand = PumpCommandType.SetBolus
        val commandResponse = selectedConnector.sendBolus(detailedBolusInfo!!) // TODO refactor this not to use AAPS object
        pumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    // TODO refactor this not to use AAPS object
    fun getTemporaryBasal(): TempBasalPair? {
        aapsLogger.error(LTag.PUMP, "getTemporaryBasal command is not available!!!")
        pumpUtil.currentCommand = PumpCommandType.GetTemporaryBasal
        val commandResponse = selectedConnector.retrieveTemporaryBasal() // TODO refactor this not to use AAPS object
        pumpUtil.resetDriverStatusToConnected()

        return if (commandResponse.isSuccess) commandResponse.value else null
    }

    fun setTemporaryBasal(value: Int, duration: Int): ResultCommandResponse? {
        aapsLogger.error(LTag.PUMP, "setTemporaryBasal command is not available!!!")
        pumpUtil.currentCommand = PumpCommandType.SetTemporaryBasal
        val commandResponse = selectedConnector.sendTemporaryBasal(value, duration) // TODO refactor this not to use AAPS object
        pumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    fun setBasalProfile(profile: Profile?): ResultCommandResponse? {
        aapsLogger.error(LTag.PUMP, "setBasalProfile command is not available!!!")
        pumpUtil.currentCommand = PumpCommandType.SetBasalProfile
        val commandResponse = selectedConnector.sendBasalProfile(profile!!) // TODO refactor this not to use AAPS object
        pumpUtil.resetDriverStatusToConnected()
        return commandResponse
    }

    fun sendFakeCommand(commandType: PumpCommandType): ResultCommandResponse? {
        pumpUtil.currentCommand = commandType
        pumpUtil.sleepSeconds(10)
        pumpUtil.resetDriverStatusToConnected()
        return ResultCommandResponse(commandType, true, null)
    }

    fun cancelTemporaryBasal(): Boolean {
        pumpUtil.currentCommand = PumpCommandType.CancelTemporaryBasal
        val commandResponse = selectedConnector.cancelTemporaryBasal() // TODO refactor this not to use AAPS object
        pumpUtil.resetDriverStatusToConnected()

        return commandResponse.isSuccess()
    }

    fun getRemainingInsulin(): Double? {
        aapsLogger.error(LTag.PUMP, "getRemainingInsulin command is not available!!!")

        pumpUtil.currentCommand = PumpCommandType.GetRemainingInsulin
        val commandResponse = selectedConnector.retrieveRemainingInsulin()
        pumpUtil.resetDriverStatusToConnected()

        return if (commandResponse.isSuccess()) commandResponse.value else null
    }

    fun getConfiguration() {
        // val command = GetPumpSettings(injector)
        //
        // command.execute(pumpBle = ypsoPumpBLE)
        //
        // if (command.isSuccessful) {
        //     val pumpSettings: YpsoPumpSettingsDto? = command.commandResponse
        //
        //     if (pumpSettings != null) {
        //         pumpStatus.lastConfigurationUpdate = System.currentTimeMillis()
        //
        //         if (pumpSettings.bolusIncStep != null) {
        //             pumpStatus.bolusStep = pumpSettings.bolusIncStep!!
        //             pumpStatus.pumpDescription.bolusStep = pumpStatus.bolusStep
        //         }
        //
        //         if (pumpSettings.basalProfileType != null) {
        //             pumpStatus.activeProfileName = if (pumpSettings.basalProfileType == YpsoBasalProfileType.BASAL_PROFILE_A) "A" else "B"
        //
        //             if (pumpSettings.basalProfileType == YpsoBasalProfileType.BASAL_PROFILE_B) {
        //                 pumpUtil.sendNotification(
        //                     notificationType = YpsoPumpNotificationType.PumpIncorrectBasalProfileSelected,
        //                     "A")
        //             }
        //         }
        //
        //         if (pumpSettings.bolusAmoutLimit != null) {
        //             pumpStatus.maxBolus = pumpSettings.bolusIncStep!!
        //             //pumpStatus.pumpDescription.bolusStep = pumpStatus.bolusStep
        //         }
        //
        //         if (pumpSettings.basalRateLimit != null) {
        //             pumpStatus.maxBasal = pumpSettings.bolusIncStep!!
        //             pumpStatus.pumpDescription.basalMaximumRate = pumpStatus.maxBasal!!
        //         }
        //     }
        //
        // } else {
        //     aapsLogger.error(LTag.PUMPCOMM, "Problem reading configuration.")
        //     return
        // }
    }

    // TODO
    // val batteryStatus: DataCommandResponse?
    //     get() =// TODO
    //         null

    fun getBasalProfile(): DoubleArray? {
        aapsLogger.error(LTag.PUMP, "getBasalProfile command is not available!!!")

        pumpUtil.currentCommand = PumpCommandType.GetBasalProfile
        val commandResponse = selectedConnector.retrieveBasalProfile()
        pumpUtil.resetDriverStatusToConnected()

        return if (commandResponse.isSuccess()) commandResponse.value else null
    }

    fun getTime(): DateTimeDto? {
        aapsLogger.error(LTag.PUMP, "getTime command is not available!!!")

        pumpUtil.currentCommand = PumpCommandType.GetTime
        val commandResponse = selectedConnector.getTime()
        pumpUtil.resetDriverStatusToConnected()

        return if (commandResponse.isSuccess()) commandResponse.value else null
    }

    fun setTime(): DateTimeDto? {
        TODO("Not yet implemented")
    }

    init {
        baseConnector = TandemPumpConnector(pumpStatus, pumpUtil, injector, aapsLogger) //new YpsoPumpBaseConnector(ypsopumpUtil, injector, aapsLogger);
        selectedConnector = baseConnector //new YpsoPumpDummyConnector(ypsopumpUtil, injector, aapsLogger);
        //this.fabricPrivacy = fabricPrivacy
        disposable.add(rxBus
                           .toObservable(EventPumpConfigurationChanged::class.java)
                           .observeOn(Schedulers.io())
                           .subscribe({ _ -> resetFirmwareVersion() }) { throwable: Throwable? -> fabricPrivacy.logException(throwable!!) }
        )
    }
}