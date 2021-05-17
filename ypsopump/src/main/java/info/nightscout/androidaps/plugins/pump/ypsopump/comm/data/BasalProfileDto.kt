package info.nightscout.androidaps.plugins.pump.ypsopump.comm.data

import java.util.*

class BasalProfileDto(data: DoubleArray?) {
    var basalName: String? = null
    var basalPatterns: DoubleArray?


    fun basalProfileToString(): String {
        val sb = StringBuffer("Basal Profile [")
        for (i in basalPatterns!!.indices) {
            var time = if (i < 10) "0$i" else "" + i
            time += ":00"
            sb.append(String.format(Locale.ROOT, "%s=%.3f, ", time, basalPatterns!![i]))
        }
        sb.append("]")
        return sb.toString()
    }


    override fun toString(): String {
        return if (basalPatterns == null) {
            "Basal Profile [Not Set]"
        } else {
            basalProfileToString()
        }
    }


    init {
        basalPatterns = data
    }
}