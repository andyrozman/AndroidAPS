package info.nightscout.aaps.pump.tandem.comm

import android.util.Log
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import timber.log.Timber

class AAPSTimberTree constructor(val aapsLogger: AAPSLogger) : Timber.DebugTree()  {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t==null) {
            when (priority) {
                Log.INFO -> aapsLogger.info(LTag.PUMPBTCOMM, "PumpX2:$tag - ${message}")
                Log.WARN     ->aapsLogger.warn(LTag.PUMPBTCOMM, "PumpX2:$tag - ${message}")
                Log.ERROR -> aapsLogger.error(LTag.PUMPBTCOMM, "PumpX2:$tag - ${message}")
                else -> aapsLogger.debug(LTag.PUMPBTCOMM, "PumpX2:$tag - ${message}")
            }
        } else {
            aapsLogger.error(LTag.PUMPBTCOMM, "PumpX2:$tag - ${message}", t)
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