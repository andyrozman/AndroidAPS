package info.nightscout.androidaps.plugins.pump.ypsopump.defs;

import androidx.annotation.StringRes;

import info.nightscout.androidaps.events.EventPumpStatusChanged;
import info.nightscout.androidaps.plugins.pump.ypsopump.R;

public enum YpsoDriverStatus {
    Sleeping(null),
    Connecting(R.string.connecting),
    Connected(R.string.connected),
    ExecutingCommand(null),
    Disconnecting(R.string.disconnecting),
    Disconnected(R.string.disconnected),
    ErrorCommunicatingWithPump(null);

    private Integer resourceId;

    YpsoDriverStatus(@StringRes Integer resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getResourceId() {
        return resourceId;
    }

}
