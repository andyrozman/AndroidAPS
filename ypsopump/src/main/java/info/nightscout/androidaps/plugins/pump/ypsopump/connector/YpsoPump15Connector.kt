package info.nightscout.androidaps.plugins.pump.ypsopump.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.shared.logging.AAPSLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Deprecated("Probably wont be used")
class YpsoPump15Connector @Inject constructor(pumpUtil: YpsoPumpUtil,
                                              injector: HasAndroidInjector,
                                              aapsLogger: AAPSLogger) : YpsoPumpConnectorAbstract(pumpUtil, injector, aapsLogger) {






}