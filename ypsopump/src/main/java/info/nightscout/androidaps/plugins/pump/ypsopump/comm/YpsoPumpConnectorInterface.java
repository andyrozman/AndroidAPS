package info.nightscout.androidaps.plugins.pump.ypsopump.comm;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.parameters.CommandParameters;
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response.CommandResponse;

public interface YpsoPumpConnectorInterface {

    boolean connectToPump();

    @NotNull
    CommandResponse executeCommand(@Nullable CommandParameters parameters);

}
