package info.nightscout.androidaps.plugins.pump.ypsopump.util;

import javax.inject.Inject;

import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.logging.LTag;
import info.nightscout.androidaps.plugins.bus.RxBusWrapper;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoDriverStatus;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus;
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpStatusChanged;

public class YpsoPumpUtil {

    private final AAPSLogger aapsLogger;
    private final RxBusWrapper rxBus;
    private final YpsopumpPumpStatus ypsopumpPumpStatus;

    private static YpsoDriverStatus driverStatus = YpsoDriverStatus.Sleeping;
    private YpsoPumpCommandType pumpCommandType;

    @Inject
    public YpsoPumpUtil(
            AAPSLogger aapsLogger,
            RxBusWrapper rxBus,
            YpsopumpPumpStatus ypsopumpPumpStatus
    ) {
        this.aapsLogger = aapsLogger;
        this.rxBus = rxBus;
        this.ypsopumpPumpStatus = ypsopumpPumpStatus;
    }


    public YpsoDriverStatus getDriverStatus() {
        return (YpsoDriverStatus) workWithStatusAndCommand(StatusChange.GetStatus, null, null);
    }

    public YpsoPumpCommandType getCurrentCommand() {
        return (YpsoPumpCommandType) workWithStatusAndCommand(StatusChange.GetCommand, null, null);
    }

    public void setDriverStatus(YpsoDriverStatus status) {
        aapsLogger.debug(LTag.PUMP, "Set driver status: " + status.name());
        workWithStatusAndCommand(StatusChange.SetStatus, status, null);
    }

    public void setCurrentCommand(YpsoPumpCommandType currentCommand) {
        aapsLogger.debug(LTag.PUMP, "Set current command: " + currentCommand.name());
        workWithStatusAndCommand(StatusChange.SetCommand, YpsoDriverStatus.ExecutingCommand, currentCommand);
    }

    public synchronized Object workWithStatusAndCommand(StatusChange type, YpsoDriverStatus driverStatusIn, YpsoPumpCommandType pumpCommandType) {

        //aapsLogger.debug(LTag.PUMP, "Status change type: " + type.name() + ", DriverStatus: " + (driverStatus != null ? driverStatus.name() : ""));

        switch (type) {

            case GetStatus:
                aapsLogger.debug(LTag.PUMP, "GetStatus: DriverStatus: " + driverStatus);
                return driverStatus;

            case GetCommand:
                return this.pumpCommandType;

            case SetStatus: {
                aapsLogger.debug(LTag.PUMP, "SetStatus: DriverStatus: " + driverStatus + ", Incoming: " + driverStatusIn);
                driverStatus = driverStatusIn;
                this.pumpCommandType = null;
                aapsLogger.debug(LTag.PUMP, "SetStatus: DriverStatus: " + driverStatus);
                rxBus.send(new EventPumpStatusChanged());
            }
            break;

            case SetCommand: {
                this.driverStatus = driverStatusIn;
                this.pumpCommandType = pumpCommandType;
                rxBus.send(new EventPumpStatusChanged());
            }
            break;
        }

        return null;
    }


    public void sleepSeconds(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void sleep(long miliseconds) {
        try {
            Thread.sleep(miliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private enum StatusChange {
        GetStatus,
        GetCommand,
        SetStatus,
        SetCommand
    }


}
