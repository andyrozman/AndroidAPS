package info.nightscout.androidaps.plugins.pump.tandem.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus
import info.nightscout.androidaps.plugins.pump.common.driver.connector.PumpConnectorAbstract
import info.nightscout.androidaps.plugins.pump.common.driver.connector.PumpDummyConnector
import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType
import info.nightscout.androidaps.plugins.pump.common.util.PumpUtil
import info.nightscout.androidaps.plugins.pump.tandem.comm.TandemCommunicationManager
import info.nightscout.shared.logging.AAPSLogger
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
                                              pumpUtil: PumpUtil,
                                              injector: HasAndroidInjector,
                                              var tandemCommunicationManager: TandemCommunicationManager,
                                              aapsLogger: AAPSLogger
): PumpDummyConnector(pumpStatus, pumpUtil, injector, aapsLogger) {

    //var supportedCommandsList: List<PumpCommandType>


    override fun connectToPump(): Boolean {
        //tandemCommunicationManager.onPumpConnected()
        pumpUtil.sleepSeconds(10)
        return true
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