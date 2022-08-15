package info.nightscout.androidaps.plugins.pump.tandem.comm

import android.content.Context
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.*
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.CommandResponseAbstract
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.CommandResponseInterface
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.DataCommandResponse
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import info.nightscout.shared.sharedPreferences.SP
import javax.inject.Inject

class TandemDataConverter @Inject constructor(
    var aapsLogger: AAPSLogger,
    var sp: SP,
    var pumpUtil: TandemPumpUtil) {

    fun convertMessage(message: Message) : Any? {

        when(message) {

            //is ControlIQInfoV2Response -> return null

            //12 -> return null;

            // Battery Level
            is CurrentBatteryV1Response,
            is CurrentBatteryV2Response    -> return getBatteryResponse(message as CurrentBatteryAbstractResponse)

            // Configuration
            is ControlIQInfoV1Response,
            is ControlIQInfoV2Response     -> return getControlIQEnabled(message)
            is BasalLimitSettingsResponse  -> return getBasalLimit(message)


            else                           -> {
                aapsLogger.warn(LTag.PUMPCOMM, "Can't convert Tandem response Message of type: ${message.opCode()} and class: ${message.javaClass.name}")
                return null
            }


        }

        return null;



    }

    private fun getBasalLimit(message: BasalLimitSettingsResponse): Long {
        return message.basalLimit
    }

    private fun getControlIQEnabled(message: Message): Boolean {
        if (message is ControlIQInfoV1Response)
            return message.closedLoopEnabled
        else if (message is ControlIQInfoV2Response)
            return message.closedLoopEnabled

        return false
    }

    private fun getBatteryResponse(message: CurrentBatteryAbstractResponse): CommandResponseInterface? {
        // TODO calculate value 0-100


        return DataCommandResponse(
            PumpCommandType.GetBatteryStatus, true, null, 80)



    }

}