package info.nightscout.aaps.pump.tandem.util

import info.nightscout.androidaps.plugins.pump.tandem.R

/**
 * Created by andy on 14/07/2022.
 */
object TandemPumpConst {

    const val Prefix = "AAPS.Tandem."

    object Prefs {

        @JvmField val PumpSerial = R.string.key_tandem_serial
        @JvmField val PumpAddress = R.string.key_tandem_address
        @JvmField val PumpName = R.string.key_tandem_name
        @JvmField val PumpPairStatus = R.string.key_tandem_pair_status
        @JvmField val PumpPairCode = R.string.key_tandem_pair_code
        @JvmField val PumpApiVersion = R.string.key_tandem_api_version
        @JvmField val PumpVersionResponse = R.string.key_tandem_name




    //        @JvmField val PumpError = R.string.key_tandem_pair_status
        //@JvmField val PumpBonded = R.string.key_ypsopump_bonded
        //@JvmField val PumpStatusList = R.string.key_ypsopump_status_list
        //@JvmField val BolusSize = R.string.key_ypsopump_bolus_size
    }

    object Statistics {

        const val StatsPrefix = "tandem_"
        const val FirstPumpStart = Prefix + "first_pump_use"
        const val LastGoodPumpCommunicationTime = Prefix + "lastGoodPumpCommunicationTime"
        const val TBRsSet = StatsPrefix + "tbrs_set"
        const val StandardBoluses = StatsPrefix + "std_boluses_delivered"
        const val SMBBoluses = StatsPrefix + "smb_boluses_delivered"
    //        public static final String LastPumpHistoryEntry = StatsPrefix + "pump_history_entry";
        //        public static final String LastPrime = StatsPrefix + "last_sent_prime";
        //        public static final String LastRewind = StatsPrefix + "last_sent_rewind";
    }
}