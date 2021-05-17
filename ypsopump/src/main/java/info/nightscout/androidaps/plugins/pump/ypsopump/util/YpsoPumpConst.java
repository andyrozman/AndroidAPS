package info.nightscout.androidaps.plugins.pump.ypsopump.util;


import info.nightscout.androidaps.plugins.pump.ypsopump.R;

/**
 * Created by andy on 5/12/18.
 */

public class YpsoPumpConst {

    static final String Prefix = "AAPS.YpsoPump.";

    public static class Prefs {
        public static final int PumpSerial = R.string.key_ypsopump_serial;
        public static final int PumpAddress = R.string.key_ypsopump_address;
        public static final int PumpName = R.string.key_ypsopump_name;

    }

    public class Statistics {

        public static final String StatsPrefix = "ypsopump_";
        public static final String FirstPumpStart = Prefix + "first_pump_use";
        public static final String LastGoodPumpCommunicationTime = Prefix + "lastGoodPumpCommunicationTime";
        public static final String TBRsSet = StatsPrefix + "tbrs_set";
        public static final String StandardBoluses = StatsPrefix + "std_boluses_delivered";
        public static final String SMBBoluses = StatsPrefix + "smb_boluses_delivered";
//        public static final String LastPumpHistoryEntry = StatsPrefix + "pump_history_entry";
//        public static final String LastPrime = StatsPrefix + "last_sent_prime";
//        public static final String LastRewind = StatsPrefix + "last_sent_rewind";
    }

}
