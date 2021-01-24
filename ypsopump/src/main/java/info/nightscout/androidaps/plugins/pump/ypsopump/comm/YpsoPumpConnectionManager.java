package info.nightscout.androidaps.plugins.pump.ypsopump.comm;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.logging.LTag;
import info.nightscout.androidaps.plugins.bus.RxBusWrapper;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.parameters.CommandParameters;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.impl.YpsoPumpBaseConnector;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.impl.YpsoPumpDummyConnector;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoDriverStatus;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware;
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil;
import info.nightscout.androidaps.utils.sharedPreferences.SP;

@Singleton
public class YpsoPumpConnectionManager {

    private final SP sp;
    private final YpsoPumpUtil ypsopumpUtil;
    private final YpsopumpPumpStatus pumpStatus;
    private final HasAndroidInjector injector;
    private final AAPSLogger aapsLogger;
    private final RxBusWrapper rxBus;
    private YpsoPumpBaseConnector baseConnector;
    private YpsoPumpConnectorInterface selectedConnector;

    private YpsoPumpFirmware oldFirmware;
    private YpsoPumpFirmware currentFirmware;

    @Inject
    public YpsoPumpConnectionManager(YpsopumpPumpStatus pumpStatus,
                                     YpsoPumpUtil ypsopumpUtil,
                                     SP sp,
                                     HasAndroidInjector injector,
                                     AAPSLogger aapsLogger,
                                     RxBusWrapper rxBus
    ) {
        this.sp = sp;
        this.pumpStatus = pumpStatus;
        this.ypsopumpUtil = ypsopumpUtil;
        this.injector = injector;
        this.aapsLogger = aapsLogger;
        this.rxBus = rxBus;
        this.baseConnector = new YpsoPumpBaseConnector(ypsopumpUtil, injector, aapsLogger);
        this.selectedConnector = new YpsoPumpDummyConnector(ypsopumpUtil, injector, aapsLogger);
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
        // }
        //};


    }
}
