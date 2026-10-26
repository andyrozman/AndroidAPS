package app.aaps.pump.tandem.common.util

import android.util.Log
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import dev.zacsweers.metro.AppScope
import timber.log.Timber
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@Inject
class PumpX2L(val aapsLogger: AAPSLogger) : Timber.DebugTree()  {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        val tagDetails = getTagDetails(tag)

        if (t==null) {
            when (priority) {
                Log.INFO -> aapsLogger.info(tagDetails.className!!, tagDetails.methodName!!, tagDetails.lineNumber, LTag.PUMPBTCOMM, message)
                Log.WARN -> aapsLogger.warn(tagDetails.className!!, tagDetails.methodName!!, tagDetails.lineNumber, LTag.PUMPBTCOMM, message)
                Log.ERROR -> aapsLogger.error(tagDetails.className!!, tagDetails.methodName!!, tagDetails.lineNumber, LTag.PUMPBTCOMM, message)
                else -> aapsLogger.debug(tagDetails.className!!, tagDetails.methodName!!, tagDetails.lineNumber, LTag.PUMPBTCOMM, message)
            }
        } else {
            aapsLogger.error(tagDetails.className!!, tagDetails.methodName!!, tagDetails.lineNumber, LTag.PUMPBTCOMM, message, t)
        }
    }

    override fun createStackElementTag(element: StackTraceElement): String? {
        return "${element.className.substringAfterLast(".")}:#:${element.lineNumber}:#:${element.methodName}"
    }

    fun getTagDetails(tag: String?): TagDetails {
        val tagDetails = TagDetails()

        if (tag==null) {
            tagDetails.className = "Unknown"
            tagDetails.methodName = "unknown()"
            tagDetails.lineNumber = 0
        } else {
            val split = tag.split(":#:")

            if (split.size==1) {
                tagDetails.className = split[0]
                tagDetails.methodName = ""
            } else {
                tagDetails.className = split[0]
                tagDetails.methodName = split[2]

                try {
                    tagDetails.lineNumber = Integer.parseInt(split[1])
                } catch (ex: Exception) {

                }
            }
        }

        tagDetails.className = "X2::" + tagDetails.className

        return tagDetails
    }

}

class TagDetails {
    var className : String? = null
    var methodName: String? = null
    var lineNumber: Int = 0
}
