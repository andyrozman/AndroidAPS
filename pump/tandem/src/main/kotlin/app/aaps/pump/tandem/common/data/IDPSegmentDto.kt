package app.aaps.pump.tandem.common.data

import com.google.common.base.Preconditions
import com.jwoglom.pumpx2.pump.messages.helpers.Bytes
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.IDPSegmentResponse
import kotlin.math.pow

// TODO(jwoglom): merge with https://github.com/jwoglom/pumpX2/blob/main/messages/src/main/java/com/jwoglom/pumpx2/pump/messages/response/currentStatus/IDPSegmentResponse.java
class IDPSegmentDto {

    var idpId = 0
    var segmentIndex = 0
    var profileStartTime = 0
    var profileBasalRate = 0
    var profileCarbRatio: Long = 0
    var profileTargetBG = 0
    var profileISF = 0
    var statusId = 0

    // fun IDPSegmentResponse() {
    // }


    fun test() {
        //IDPSegmentResponse
    }


    // fun getIdpId(): Int {
    //     return this.idpId
    // }
    //
    // fun getSegmentIndex(): Int {
    //     return this.segmentIndex
    // }
    //
    // fun getProfileStartTime(): Int {
    //     return this.profileStartTime
    // }
    //
    // fun getProfileBasalRate(): Int {
    //     return this.profileBasalRate
    // }
    //
    // fun getProfileCarbRatio(): Long {
    //     return this.profileCarbRatio
    // }
    //
    // fun getProfileTargetBG(): Int {
    //     return this.profileTargetBG
    // }
    //
    // fun getProfileISF(): Int {
    //     return this.profileISF
    // }
    //
    // fun getStatusId(): Int {
    //     return this.statusId
    // }
    //
    // fun getStatus(): Set<IDPSegmentStatus?> {
    //     return IDPSegmentStatus.fromBitmask(this.statusId)
    // }
    //
    // static
    // enum class IDPSegmentStatus(val id: Int) {
    //     BASAL_RATE(1),
    //     CARB_RATIO(2),
    //     TARGET_BG(4),
    //     CORRECTION_FACTOR(8);
    //
    //     companion object {
    //
    //         fun fromBitmask(mask: Int): Set<IDPSegmentStatus?> {
    //             val items: MutableSet<IDPSegmentStatus?> = HashSet<Any?>()
    //             val var2 = entries.toTypedArray()
    //             val var3 = var2.size
    //
    //             for (var4 in 0 until var3) {
    //                 val status = var2[var4]
    //                 if ((mask and status.id) != 0) {
    //                     items.add(status)
    //                 }
    //             }
    //
    //             return items
    //         }
    //
    //         fun toBitmask(vararg items: IDPSegmentStatus): Int {
    //             var mask = 0
    //             val var2: Array<IDPSegmentStatus> = items
    //             val var3 = items.size
    //
    //             for (var4 in 0 until var3) {
    //                 val item = var2[var4]
    //                 mask = (mask.toDouble() + 2.0.pow(item.id.toDouble())).toInt()
    //             }
    //
    //             return mask
    //         }
    //     }
    // }

}