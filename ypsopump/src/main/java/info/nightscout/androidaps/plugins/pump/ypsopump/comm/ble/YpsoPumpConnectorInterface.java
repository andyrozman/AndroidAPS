package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble;

import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.BasalProfileResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.MapDataCommandResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.SimpleDataCommandResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.TemporaryBasalResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpFirmware;

public interface YpsoPumpConnectorInterface {

    boolean connectToPump();

    void disconnectFromPump();

    YpsoPumpFirmware getFirmwareVersion();

    CommandResponse deliverBolus(DetailedBolusInfo detailedBolusInfo);

    TemporaryBasalResponse getTemporaryBasal();

    CommandResponse setTemporaryBasal(int value, int duration);

    CommandResponse cancelTemporaryBasal();

    BasalProfileResponse getBasalProfile();

    CommandResponse setBasalProfile(Profile profile);

    SimpleDataCommandResponse getRemainingInsulin();

    MapDataCommandResponse getConfiguration();

    SimpleDataCommandResponse getBatteryStatus();

    void getPumpHistory();

}
