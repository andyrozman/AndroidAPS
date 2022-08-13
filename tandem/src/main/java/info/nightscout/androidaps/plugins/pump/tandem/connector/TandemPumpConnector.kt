package info.nightscout.androidaps.plugins.pump.tandem.connector

import android.content.Context
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus
import info.nightscout.androidaps.plugins.pump.common.driver.connector.PumpConnectorAbstract
import info.nightscout.androidaps.plugins.pump.common.driver.connector.PumpDummyConnector
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.common.util.PumpUtil
import info.nightscout.androidaps.plugins.pump.tandem.comm.TandemCommunicationManager
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpConst
import info.nightscout.androidaps.plugins.pump.tandem.util.TandemPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.sharedPreferences.SP
import javax.inject.Inject

/**
 * TODO
 * All commands that will be supported need to be implemented here (look at PumpConnectorInterface), and they also need
 * to be added to supportedCommandsList.
 *
 * Any command will be used from TandemPumpConnectionManager, if its not used there, then it doesn't need to be
 * implemented.
 *
 *
 */
class TandemPumpConnector @Inject constructor(pumpStatus: PumpStatus,
                                              var context: Context,
                                              var tandemPumpUtil: TandemPumpUtil,
                                              injector: HasAndroidInjector,
                                              var sp: SP,
                                              aapsLogger: AAPSLogger
): PumpDummyConnector(pumpStatus, tandemPumpUtil, injector, aapsLogger) {

    //var supportedCommandsList: List<PumpCommandType>
    var tandemCommunicationManager: TandemCommunicationManager? = null
    var btAddressUsed: String? = null

    override fun connectToPump(): Boolean {
        var createInstance: Boolean = false
        var newBtAddress = sp.getStringOrNull(TandemPumpConst.Prefs.PumpAddress, null)

        if (!btAddressUsed.isNullOrEmpty()) {
            if (btAddressUsed.equals(newBtAddress)) {
                newBtAddress = null
            }
        } else {
            if (newBtAddress.isNullOrEmpty()) {
                return false;
            }
        }

        if (!newBtAddress.isNullOrEmpty()) {
            tandemCommunicationManager = TandemCommunicationManager(
                context = context,
                aapsLogger = aapsLogger,
                sp = sp,
                pumpUtil = tandemPumpUtil,
                btAddress = btAddressUsed!!
            )
        }

        return tandemCommunicationManager!!.connect()


    }

    override fun disconnectFromPump(): Boolean {
        pumpUtil.sleepSeconds(10)
        return true
    }


    init {
        supportedCommandsList = listOf()
    }

    override fun getSupportedCommands(): List<PumpCommandType> {
        return supportedCommandsList
    }

}