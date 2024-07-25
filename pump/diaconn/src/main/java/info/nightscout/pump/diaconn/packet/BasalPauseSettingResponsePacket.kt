package info.nightscout.pump.diaconn.packet

import dagger.android.HasAndroidInjector
import info.nightscout.pump.diaconn.DiaconnG8Pump
import info.nightscout.rx.logging.LTag

import javax.inject.Inject

/**
 * BasalPauseSettingResponsePacket
 */
class BasalPauseSettingResponsePacket(
    injector: HasAndroidInjector
) : DiaconnG8Packet(injector ) {

    @Inject lateinit var diaconnG8Pump: DiaconnG8Pump
    var result = 0

    init {
        msgType = 0x83.toByte()
        aapsLogger.debug(LTag.PUMPCOMM, "BasalPauseSettingResponsePacket Init")
    }

    override fun handleMessage(data: ByteArray?) {
        val defectCheck = defect(data)
        if (defectCheck != 0) {
            aapsLogger.debug(LTag.PUMPCOMM, "BasalPauseSettingResponsePacket Got some Error")
            failed = true
            return
        } else failed = false

        val bufferData = prefixDecode(data)
        result =  getByteToInt(bufferData)

        if(!isSuccSettingResponseResult(result)) {
            diaconnG8Pump.resultErrorCode = result
            failed = true
            return
        }

        diaconnG8Pump.otpNumber =  getIntToInt(bufferData)
        aapsLogger.debug(LTag.PUMPCOMM, "Result --> ${result}")
        aapsLogger.debug(LTag.PUMPCOMM, "otpNumber --> ${diaconnG8Pump.otpNumber}")
    }

    override fun getFriendlyName(): String {
        return "PUMP_BASAL_PAUSE_SETTING_RESPONSE"
    }
}