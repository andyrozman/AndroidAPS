package info.nightscout.androidaps.plugins.pump.ypsopump.defs

import androidx.annotation.StringRes
import info.nightscout.androidaps.plugins.pump.ypsopump.R

@Deprecated("Deprecated class. Remove.")
enum class YpsoDriverStatus(@param:StringRes val resourceId: Int?) {
    Sleeping(null),
    Connecting(R.string.connecting),
    Connected(R.string.connected),
    ExecutingCommand(null),
    Disconnecting(R.string.disconnecting),
    Disconnected(R.string.disconnected),
    Error(null);

}