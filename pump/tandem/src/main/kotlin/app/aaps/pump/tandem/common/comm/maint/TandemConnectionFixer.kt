package app.aaps.pump.tandem.common.comm.maint

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.pump.tandem.common.driver.connector.TandemPumpConnectionManager
import app.aaps.pump.tandem.mobi.TandemMobiPluginVersion
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.SingleIn



@SingleIn(AppScope::class)
@Inject
class TandemConnectionFixer(
    val aapsLogger: AAPSLogger,
    //val tandemPumpConnectionManager: TandemPumpConnectionManager   ///dagger.Lazy<TandemPumpConnectionManager>
    private val tandemPumpConnectionManagerProvider: Provider<TandemPumpConnectionManager>
){

    val TAG = LTag.PUMPBTCOMM
    @Volatile var running = false

    val enabled = TandemMobiPluginVersion.connectionFixerEnabled

    fun startConnectionFix() {
        if (!enabled) {
            aapsLogger.warn(TAG, "CF: StartConnectionFix disabled - experimental code not yet ready for use")
            return
        }

        aapsLogger.error(TAG, "CF: Start ConnectionFix")

        if (running)
            return

        running = true

        Thread {
            do {
                aapsLogger.error(TAG, "CF: Start ConnectionFix - in run")

                // TODO Metro
                // val connected = tandemPumpConnectionManager.get().connectToPump()
                //
                // if (connected) {
                //     running = false
                // } else {
                //     Thread.sleep(60000) // wait 60 seconds and retry
                // }
            } while (running)

            aapsLogger.error(TAG, "CF: End ConnectionFix - connection fixed")
        }.start()
    }

}
