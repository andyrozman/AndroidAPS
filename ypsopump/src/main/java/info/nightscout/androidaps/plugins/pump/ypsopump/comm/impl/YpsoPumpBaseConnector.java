package info.nightscout.androidaps.plugins.pump.ypsopump.comm.impl;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.parameters.CommandParameters;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse;
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil;

public class YpsoPumpBaseConnector extends YpsoPumpConnectorAbstract {

    public YpsoPumpBaseConnector(YpsoPumpUtil ypsopumpUtil, HasAndroidInjector injector, AAPSLogger aapsLogger) {
        super(ypsopumpUtil, injector, aapsLogger);
    }

    @NotNull @Override public CommandResponse executeCommand(@Nullable CommandParameters parameters) {

        //switch(parameters.)



        return null;
    }
}
