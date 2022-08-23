package info.nightscout.androidaps.plugins.pump.tandem.comm

import android.util.Log
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import timber.log.Timber

class AAPSTimberTree constructor(val aapsLogger: AAPSLogger) : Timber.DebugTree()  {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t==null) {
            when (priority) {
                Log.INFO -> aapsLogger.info(LTag.PUMPBTCOMM, "X2:$tag - ${message}")
                Log.WARN     ->aapsLogger.warn(LTag.PUMPBTCOMM, "X2:$tag - ${message}")
                Log.ERROR -> aapsLogger.error(LTag.PUMPBTCOMM, "X2:$tag - ${message}")
                else -> aapsLogger.debug(LTag.PUMPBTCOMM, "X2:$tag - ${message}")
            }
        } else {
            aapsLogger.error(LTag.PUMPBTCOMM, "X2:$tag - ${message}", t)
        }
    }

}

// class Test : Timber.Tree() {
//
//     override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
//
//
//     }
//
// }