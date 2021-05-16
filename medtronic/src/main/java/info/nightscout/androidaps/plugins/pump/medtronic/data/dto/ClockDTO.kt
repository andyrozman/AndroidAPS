package info.nightscout.androidaps.plugins.pump.medtronic.data.dto

import org.joda.time.LocalDateTime

/**
 * Created by andy on 2/27/19.
 */
class ClockDTO {
    var localDeviceTime: LocalDateTime? = null
    var pumpTime: LocalDateTime? = null
    var timeDifference = 0
}