package info.nightscout.pump.danars.comm

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.danars.encryption.BleEncryption
import info.nightscout.rx.logging.LTag

class DanaRSPacketHistoryAllHistory(
    injector: HasAndroidInjector,
    from: Long = 0
) : DanaRSPacketHistory(injector, from) {

    init {
        opCode = BleEncryption.DANAR_PACKET__OPCODE_REVIEW__ALL_HISTORY
        aapsLogger.debug(LTag.PUMPCOMM, "New message")
    }

    override val friendlyName: String = "REVIEW__ALL_HISTORY"
}