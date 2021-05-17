package info.nightscout.androidaps.plugins.pump.ypsopump.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil

class YpsoPumpBaseConnector(ypsopumpUtil: YpsoPumpUtil?, injector: HasAndroidInjector?, aapsLogger: AAPSLogger?) : YpsoPumpConnectorAbstract(ypsopumpUtil!!, injector!!, aapsLogger!!)