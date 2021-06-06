package info.nightscout.androidaps.plugins.pump.ypsopump.data

class BasalProfilePatternDto constructor(var firstIndex: Int = 0,
                                         var lastIndex: Int = 0) {

    var patternName: String = "A"

    // var firstIndex: Int = 0
    // var lastIndex: Int = 0
    var mapOfProfilePattens: MutableMap<Int, EventDto> = mutableMapOf()
}