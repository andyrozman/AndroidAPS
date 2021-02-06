package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.BasalProfileResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.TemporaryBasalResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil;

public abstract class YpsoPumpConnectorAbstract implements YpsoPumpConnectorInterface {

    protected YpsoPumpUtil pumpUtil;
    protected HasAndroidInjector injector;
    protected AAPSLogger aapsLogger;

    CommandResponse unSuccessfulResponse = CommandResponse.builder().success(false).errorDescription("Command not implemented.").build();

    public YpsoPumpConnectorAbstract(YpsoPumpUtil ypsopumpUtil,
                                     HasAndroidInjector injector,
                                     AAPSLogger aapsLogger) {
        this.pumpUtil = ypsopumpUtil;
        this.injector = injector;
        this.aapsLogger = aapsLogger;
    }

    @Override
    public boolean connectToPump() {
        pumpUtil.sleepSeconds(10);
        return true;
    }


    @Override
    public void disconnectFromPump() {

    }

    @Override
    public YpsoPumpFirmware getFirmwareVersion() {
        return YpsoPumpFirmware.VERSION_1_1;
    }

    public CommandResponse deliverBolus(DetailedBolusInfo detailedBolusInfo) {
        return unSuccessfulResponse;
    }


    @Override
    public TemporaryBasalResponse getTemporaryBasal() {
        return null;
    }

    @Override
    public CommandResponse setTemporaryBasal(int value, int duration) {
        return unSuccessfulResponse;
    }

    @Override
    public BasalProfileResponse getBasalProfile() {
        return null;
    }

    @Override
    public CommandResponse setBasalProfile(Profile profile) {
        return unSuccessfulResponse;
    }

    @Override
    public CommandResponse cancelTemporaryBasal() {
        return unSuccessfulResponse;
    }

}
