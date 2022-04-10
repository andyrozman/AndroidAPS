package info.nightscout.androidaps.plugins.pump.ypsopump.util

import info.nightscout.androidaps.plugins.pump.ypsopump.R

/**
 * Created by andy on 5/12/21.
 */
object YpsoPumpConst {

    const val Prefix = "AAPS.YpsoPump."

    object Prefs {
        @JvmField val PumpSerial = R.string.key_ypsopump_serial
        @JvmField val PumpAddress = R.string.key_ypsopump_address
        @JvmField val PumpName = R.string.key_ypsopump_name
        @JvmField val PumpBonded = R.string.key_ypsopump_bonded
        @JvmField val PumpStatusList = R.string.key_ypsopump_status_list
        @JvmField val BolusSize = R.string.key_ypsopump_bolus_size
    }

    object Statistics {

        const val StatsPrefix = "ypsopump_"
        const val FirstPumpStart = Prefix + "first_pump_use"
        const val LastGoodPumpCommunicationTime = Prefix + "lastGoodPumpCommunicationTime"
        const val TBRsSet = StatsPrefix + "tbrs_set"
        const val StandardBoluses = StatsPrefix + "std_boluses_delivered"
        const val SMBBoluses = StatsPrefix + "smb_boluses_delivered" //        public static final String LastPumpHistoryEntry = StatsPrefix + "pump_history_entry";
        //        public static final String LastPrime = StatsPrefix + "last_sent_prime";
        //        public static final String LastRewind = StatsPrefix + "last_sent_rewind";
    }
}