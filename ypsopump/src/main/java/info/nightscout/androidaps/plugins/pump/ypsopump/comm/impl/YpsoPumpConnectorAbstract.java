package info.nightscout.androidaps.plugins.pump.ypsopump.comm.impl;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.YpsoPumpConnectorInterface;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil;
import info.nightscout.androidaps.utils.sharedPreferences.SP;

public abstract class YpsoPumpConnectorAbstract implements YpsoPumpConnectorInterface {

    private YpsoPumpUtil pumpUtil;
    HasAndroidInjector injector;
    AAPSLogger aapsLogger;

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

}
