package info.nightscout.androidaps.plugins.pump.ypsopump.comm;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.logging.LTag;
import info.nightscout.androidaps.plugins.bus.RxBusWrapper;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpConnectorInterface;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpDummyConnector;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.parameters.CommandParameters;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.SimpleDataCommandResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.TemporaryBasalResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoDriverStatus;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware;
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus;
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpConfigurationChanged;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil;
import info.nightscout.androidaps.utils.FabricPrivacy;
import info.nightscout.androidaps.utils.sharedPreferences.SP;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

@Singleton
public class YpsoPumpConnectionManager {

    private final SP sp;
    private final YpsoPumpUtil ypsopumpUtil;
    private final YpsopumpPumpStatus pumpStatus;
    private final HasAndroidInjector injector;
    private final AAPSLogger aapsLogger;
    private final RxBusWrapper rxBus;
    private final FabricPrivacy fabricPrivacy;

    private YpsoPumpConnectorInterface baseConnector;  // YpsoPumpBaseConnector
    private YpsoPumpConnectorInterface selectedConnector;

    private final CompositeDisposable disposable = new CompositeDisposable();

    private YpsoPumpFirmware oldFirmware;
    private YpsoPumpFirmware currentFirmware;

    @Inject
    public YpsoPumpConnectionManager(YpsopumpPumpStatus pumpStatus,
                                     YpsoPumpUtil ypsopumpUtil,
                                     SP sp,
                                     HasAndroidInjector injector,
                                     AAPSLogger aapsLogger,
                                     RxBusWrapper rxBus,
                                     FabricPrivacy fabricPrivacy
    ) {
        this.sp = sp;
        this.pumpStatus = pumpStatus;
        this.ypsopumpUtil = ypsopumpUtil;
        this.injector = injector;
        this.aapsLogger = aapsLogger;
        this.rxBus = rxBus;
        this.baseConnector = new YpsoPumpDummyConnector(ypsopumpUtil, injector, aapsLogger); //new YpsoPumpBaseConnector(ypsopumpUtil, injector, aapsLogger);
        this.selectedConnector = this.baseConnector; //new YpsoPumpDummyConnector(ypsopumpUtil, injector, aapsLogger);
        this.fabricPrivacy = fabricPrivacy;

        disposable.add(rxBus
                .toObservable(EventPumpConfigurationChanged.class)
                .observeOn(Schedulers.io())
                .subscribe(event -> {
                    resetFirmwareVersion();
                }, fabricPrivacy::logException)
        );
    }

    boolean inConnectMode = false;

    public boolean connectToPump() {

        if (!inConnectMode) {
            if (ypsopumpUtil.getDriverStatus() == YpsoDriverStatus.Connected) {
                return true;
            }
            inConnectMode = true;
        } else {
            return false;
        }

        // TODO if initialized use types connection, else use base one

//        Thread thread = new Thread() {
//            public void run() {
        System.out.println("Thread Running");
        aapsLogger.debug(LTag.PUMP, "!!!!!! Connect to Pump - Thread running");

        ypsopumpUtil.setDriverStatus(YpsoDriverStatus.Connecting);
        ypsopumpUtil.sleepSeconds(15);

        ypsopumpUtil.setDriverStatus(YpsoDriverStatus.Connected);

//            }
//        };
//
//        thread.start();
        inConnectMode = false;

        return true;
    }

    private void resetFirmwareVersion() {
    }


    public void determineFirmwareVersion() {


    }


    public CommandResponse executeCommand(CommandParameters parameters) {

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

        ypsopumpUtil.setCurrentCommand(parameters.getCommandType());

        ypsopumpUtil.sleepSeconds(40);


        ypsopumpUtil.setDriverStatus(YpsoDriverStatus.Connected);

        return CommandResponse.builder().success(true).build();
    }


    public void disconnectFromPump() {

        //Thread thread = new Thread() {
        //  public void run() {
        System.out.println("Thread Running");
        aapsLogger.debug(LTag.PUMP, "Disconnect from Pump - Thread running");

        ypsopumpUtil.setDriverStatus(YpsoDriverStatus.Disconnecting);
        ypsopumpUtil.sleepSeconds(20);

        ypsopumpUtil.setDriverStatus(YpsoDriverStatus.Sleeping);

        pumpStatus.setLastCommunicationToNow();
        // }
        //};


    }

    public CommandResponse deliverBolus(DetailedBolusInfo detailedBolusInfo) {

        ypsopumpUtil.setCurrentCommand(YpsoPumpCommandType.SetBolus);

        CommandResponse commandResponse = this.selectedConnector.deliverBolus(detailedBolusInfo); // TODO refactor this not to use AAPS object

        ypsopumpUtil.resetDriverStatusToConnected();

        return commandResponse;
    }

    public TemporaryBasalResponse getTemporaryBasal() {

        ypsopumpUtil.setCurrentCommand(YpsoPumpCommandType.GetTemporaryBasal);

        TemporaryBasalResponse commandResponse = this.selectedConnector.getTemporaryBasal(); // TODO refactor this not to use AAPS object

        ypsopumpUtil.resetDriverStatusToConnected();

        return commandResponse;
    }


    public CommandResponse setTemporaryBasal(int value, int duration) {

        ypsopumpUtil.setCurrentCommand(YpsoPumpCommandType.SetTemporaryBasal);

        CommandResponse commandResponse = this.selectedConnector.setTemporaryBasal(value, duration); // TODO refactor this not to use AAPS object

        ypsopumpUtil.resetDriverStatusToConnected();

        return commandResponse;
    }


    public CommandResponse setBasalProfile(Profile profile) {

        ypsopumpUtil.setCurrentCommand(YpsoPumpCommandType.SetBasalProfile);

        CommandResponse commandResponse = this.selectedConnector.setBasalProfile(profile); // TODO refactor this not to use AAPS object

        ypsopumpUtil.resetDriverStatusToConnected();

        return commandResponse;
    }

    public CommandResponse cancelTemporaryBasal() {

        ypsopumpUtil.setCurrentCommand(YpsoPumpCommandType.CancelTemporaryBasal);

        CommandResponse commandResponse = this.selectedConnector.cancelTemporaryBasal(); // TODO refactor this not to use AAPS object

        ypsopumpUtil.resetDriverStatusToConnected();

        return commandResponse;

    }

    public SimpleDataCommandResponse getRemainingInsulin() {

        // TODO
        SimpleDataCommandResponse commandResponse = this.selectedConnector.getRemainingInsulin();

    }

    public void getConfiguration() {
        // TODO
    }

    public SimpleDataCommandResponse getBatteryStatus() {
        // TODO
        return null;
    }

    public void getPumpHistory() {
        // TODO
    }
}
