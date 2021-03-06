package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil;

public class YpsoPumpBaseConnector extends YpsoPumpConnectorAbstract {

    public YpsoPumpBaseConnector(YpsoPumpUtil ypsopumpUtil, HasAndroidInjector injector, AAPSLogger aapsLogger) {
        super(ypsopumpUtil, injector, aapsLogger);
    }


}
