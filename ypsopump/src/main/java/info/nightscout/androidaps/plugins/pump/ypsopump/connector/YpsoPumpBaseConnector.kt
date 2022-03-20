package info.nightscout.androidaps.plugins.pump.ypsopump.connector

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.util.YpsoPumpUtil
import info.nightscout.shared.logging.AAPSLogger

class YpsoPumpBaseConnector(ypsopumpUtil: YpsoPumpUtil?, injector: HasAndroidInjector?, aapsLogger: AAPSLogger?) : YpsoPumpConnectorAbstract(ypsopumpUtil!!, injector!!, aapsLogger!!)