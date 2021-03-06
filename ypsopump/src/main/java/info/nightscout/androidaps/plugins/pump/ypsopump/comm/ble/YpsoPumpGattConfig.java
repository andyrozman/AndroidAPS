package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble;

public class YpsoPumpGattConfig {

    // BASE
    public static final String PREFIX = "669A0C20-0008-969E-E211-FCBE";
    public static final String POSTFIX = "7BC5";
    public static final String POSTFIX_14 = "14" + POSTFIX;
    public static final String POSTFIX_3B = "3B" + POSTFIX;

    public static final String MASTER_SOFTWARE_VERSION_UUID = PREFIX + "B0" + POSTFIX_14;

    public static final String SYSTEM_TIME_UUID = PREFIX + "DD" + POSTFIX_3B;
    public static final String SYSTEM_DATE_UUID = PREFIX + "DC" + POSTFIX_3B;

    public static final String AUTHORIZATION_PASSWORD_UUID = PREFIX + "B2" + POSTFIX_14;

    public static final String PUMP_BASE_SERVICE_VERSION_UUID = PREFIX + "E2" + POSTFIX_3B;
    public static final String SETTING_SERVICE_VERSION_UUID = PREFIX + "E3" + POSTFIX_3B;
    public static final String HISTORY_SERVICE_VERSION_UUID = PREFIX + "E4" + POSTFIX_3B;

    public static final String YPSOPUMP_BT_ADDRESS = "EC:2A:F0"; // normal

    // 11
    private final static String SUPERVISOR_SOFTWARE_VERSION_UUID = PREFIX + "B1" + POSTFIX_14;
    private final static String PAIRING_PASSKEY_UUID = PREFIX + "D6" + POSTFIX_3B;

    private final static String SETTINGS_ID_UUID = PREFIX + "B3" + POSTFIX_14;
    private final static String SETTINGS_VALUE_UUID = PREFIX + "B4" + POSTFIX_14;

    private final static String ALARM_ENTRY_COUNT_UUID = PREFIX + "C8" + POSTFIX_3B;
    private final static String ALARM_ENTRY_INDEX_UUID = PREFIX + "C9" + POSTFIX_3B;
    private final static String ALARM_ENTRY_VALUE_UUID = PREFIX + "CA" + POSTFIX_3B;

    private final static String EVENT_ENTRY_COUNT_UUID = PREFIX + "CB" + POSTFIX_3B;
    private final static String EVENT_ENTRY_INDEX_UUID = PREFIX + "CC" + POSTFIX_3B;
    private final static String EVENT_ENTRY_VALUE_UUID = PREFIX + "CD" + POSTFIX_3B;

    // 15
    private static final String SYSTEM_ENTRY_COUNT_UUID = "86A5A431-D442-2C8D-304B-19EE355571FC";
    private static final String SYSTEM_ENTRY_INDEX_UUID = "381DDCE9-E934-B4AE-E345-EB87283DB426";
    private static final String SYSTEM_ENTRY_VALUE_UUID = "AE3022AF-2EC8-BF88-E64C-DA68C9A3891A";
}
