package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.plugins.pump.common.data.TempBasalPair;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.BasalProfileResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.TemporaryBasalResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware;
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil;

public class YpsoPumpDummyConnector extends YpsoPumpConnectorAbstract {

    YpsopumpPumpStatus pumpStatus = null;

    public YpsoPumpDummyConnector(YpsoPumpUtil ypsopumpUtil, HasAndroidInjector injector, AAPSLogger aapsLogger) {
        super(ypsopumpUtil, injector, aapsLogger);
        pumpStatus = ypsopumpUtil.getYpsopumpPumpStatus();
    }


    @Override
    public boolean connectToPump() {
        pumpUtil.sleepSeconds(10);
        return true;
    }


    @Override
    public void disconnectFromPump() {
        pumpUtil.sleepSeconds(10);
    }


    @Override public YpsoPumpFirmware getFirmwareVersion() {
        return YpsoPumpFirmware.VERSION_1_0;
    }


    public CommandResponse deliverBolus(DetailedBolusInfo detailedBolusInfo) {
        pumpUtil.sleepSeconds(10);
        return CommandResponse.builder().success(true).build();
    }


    @Override
    public TemporaryBasalResponse getTemporaryBasal() {
        pumpUtil.sleepSeconds(10);

        if (System.currentTimeMillis() > pumpStatus.tempBasalEnd) {
            return TemporaryBasalResponse.builder().success(true).tempBasalPair(new TempBasalPair(0, true, 0)).build();
        } else {
            TempBasalPair tempBasalPair = new TempBasalPair();
            tempBasalPair.setInsulinRate(pumpStatus.tempBasalPercent);

            long diff = pumpStatus.tempBasalStart.getTime() - System.currentTimeMillis();

            int diffMin = (int) (diff / (1000 * 60));

            tempBasalPair.setDurationMinutes(pumpStatus.tempBasalDuration - diffMin);

            return TemporaryBasalResponse.builder().success(true).tempBasalPair(tempBasalPair).build();
        }
    }


    @Override
    public CommandResponse setTemporaryBasal(int value, int duration) {
        pumpUtil.sleepSeconds(10);
        return CommandResponse.builder().success(true).build();
    }


    @Override public CommandResponse cancelTemporaryBasal() {
        pumpUtil.sleepSeconds(10);
        return CommandResponse.builder().success(true).build();
    }


    @Override
    public BasalProfileResponse getBasalProfile() {
        pumpUtil.sleepSeconds(10);
        return BasalProfileResponse.builder().success(true).basalProfile(null).build();
    }


    @Override
    public CommandResponse setBasalProfile(Profile profile) {
        pumpUtil.sleepSeconds(10);
        return CommandResponse.builder().success(true).build();
    }

}
