package info.nightscout.androidaps.plugins.pump.ypsopump.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YpsoPump15Connector @Inject constructor(pumpUtil: YpsoPumpUtil,
                                              injector: HasAndroidInjector,
                                              aapsLogger: AAPSLogger) : YpsoPumpConnectorAbstract(pumpUtil, injector, aapsLogger) {






}