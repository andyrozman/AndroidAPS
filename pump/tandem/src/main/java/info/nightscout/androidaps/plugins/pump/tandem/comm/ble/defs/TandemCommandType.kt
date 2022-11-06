package info.nightscout.androidaps.plugins.pump.tandem.comm.ble.defs

import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.CurrentBatteryV1Request

enum class TandemCommandType (val request: Message) {

    GetBatteryStatus(CurrentBatteryV1Request()),

        ;


}