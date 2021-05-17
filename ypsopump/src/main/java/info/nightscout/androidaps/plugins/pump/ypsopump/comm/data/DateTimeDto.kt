package info.nightscout.androidaps.plugins.pump.ypsopump.comm.data

data class DateTimeDto(var year: Int? = 0,
                       var month: Int? = 0,
                       var day: Int? = 0,
                       var hour: Int? = 0,
                       var minute: Int? = 0,
                       var second: Int? = 0) {

    override fun toString(): String {
        return if (year != null && month != null && day != null && hour != null && minute != null && second != null) {
            "" + zeroPrefixed(day) + "." + zeroPrefixed(month) + "." + year + " " + zeroPrefixed(hour) + ":" + zeroPrefixed(minute) + ":" + zeroPrefixed(second)
        } else
            super.toString()
    }

    fun zeroPrefixed(num: Int?): String {
        return if (num!! < 10)
            "0" + num
        else
            "" + num
    }


}