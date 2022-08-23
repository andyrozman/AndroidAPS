package info.nightscout.androidaps.plugins.pump.tandem.comm

import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import timber.log.Timber
import javax.inject.Inject

class AAPSTimberTree constructor(val aapsLogger: AAPSLogger) : Timber.DebugTree()  {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t==null) {
            aapsLogger.warn(LTag.PUMPBTCOMM, "X2:$tag - ${message}")
            // TODO need to extend this
        } else {
            aapsLogger.error(LTag.PUMPBTCOMM, "X2:$tag - ${message}", t)
        }


        //super.log(priority, "X2:$tag", message, t)
    }

}

