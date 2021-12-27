package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs

import java.util.*

enum class YpsoGattCharacteristic(val uuid: String, val description: String) {
    // SERVICE_GENERIC_ACCESS: 00001800-0000-1000-8000-00805f9b34fb
    SERVICE_CHANGED("00002a05-0000-1000-8000-00805f9b34fb", "Service Changed"),

    // SERVICE_GENERIC_ATTRIBUTE: 00001801-0000-1000-8000-00805f9b34fb
    DEVICE_NAME("00002a00-0000-1000-8000-00805f9b34fb", "Device Name"),
    APPEARANCE("00002a01-0000-1000-8000-00805f9b34fb", "Appearance"),
    SOFTWARE_REVISION_STRING("00002a28-0000-1000-8000-00805f9b34fb", "Software Revision String"),
    MANUFACTURER_NAME_STRING("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String"),

    // SERVICE_YPSO_1: fb349b5f-8000-0080-0010-0000adde0000
    MASTER_SOFTWARE_VERSION("669a0c20-0008-969e-e211-fcbeb0147bc5", "Master Software Version"),
    SUPERVISOR_SOFTWARE_VERSION("669a0c20-0008-969e-e211-fcbeb1147bc5", "Supervisor Software Version"),
    AUTHORIZATION_PASSWORD("669a0c20-0008-969e-e211-fcbeb2147bc5", "Authorization Password"),
    PAIRING_PASSKEY("669a0c20-0008-969e-e211-fcbed63b7bc5", "Pairing Password"),
    PUMP_BASE_SERVICE_VERSION("669a0c20-0008-969e-e211-fcbee23b7bc5", "Pump Base Service Version"),

    // SERVICE_YPSO_2: fb349b5f-8000-0080-0010-0000efbe0000
    SETTINGS_ID("669a0c20-0008-969e-e211-fcbeb3147bc5", "Settings Id"),
    SETTINGS_VALUE("669a0c20-0008-969e-e211-fcbeb4147bc5", "Settings Value"),
    SYSTEM_DATE("669a0c20-0008-969e-e211-fcbedc3b7bc5", "System Date"),
    SYSTEM_TIME("669a0c20-0008-969e-e211-fcbedd3b7bc5", "System Time"),
    SETTING_SERVICE_VERSION("669a0c20-0008-969e-e211-fcbee33b7bc5", "Settings Service Version"),

    // SERVICE_YPSO_3: fb349b5f-8000-0080-0010-0000dec00000
    ALARM_ENTRY_COUNT("669a0c20-0008-969e-e211-fcbec83b7bc5", "Alarm Entry Count"),
    ALARM_ENTRY_INDEX("669a0c20-0008-969e-e211-fcbec93b7bc5", "Alarm Entry Index"),
    ALARM_ENTRY_VALUE("669a0c20-0008-969e-e211-fcbeca3b7bc5", "Alarm Entry Value"),
    EVENT_ENTRY_COUNT("669a0c20-0008-969e-e211-fcbecb3b7bc5", "Event Entry Count"),
    EVENT_ENTRY_INDEX("669a0c20-0008-969e-e211-fcbecc3b7bc5", "Event Entry Index"),
    EVENT_ENTRY_VALUE("669a0c20-0008-969e-e211-fcbecd3b7bc5", "Event Entry Value"),

    UNDEFINED_001("669a0c20-0008-969e-e211-fcbece3b7bc5", "Undefined 001"),
    UNDEFINED_002("669a0c20-0008-969e-e211-fcbecf3b7bc5", "Undefined 002"),
    UNDEFINED_003("669a0c20-0008-969e-e211-fcbed03b7bc5", "Undefined 003"), // write ??
    HISTORY_SERVICE_VERSION("669a0c20-0008-969e-e211-fcbee43b7bc5", "History Service Version"),

    UNDEFINED_004("449753d3-3cc5-7594-ae4f-8d4efa2af4fd", "Undefined 004"),
    UNDEFINED_005("db75669b-e6cc-f6a4-1f4b-0becaeb81f34", "Undefined 005"),
    UNDEFINED_006("813a4e12-e1e2-47a0-2544-22664b3c9e62", "Undefined 006"),

    SYSTEM_ENTRY_COUNT("86a5a431-d442-2c8d-304b-19ee355571fc", "System Entry Count"),
    SYSTEM_ENTRY_INDEX("381ddce9-e934-b4ae-e345-eb87283db426", "System Entry Index"),
    SYSTEM_ENTRY_VALUE("ae3022af-2ec8-bf88-e64c-da68c9a3891a", "System Entry Value"),
    UNKNOWN("", "Unknown Characteristic");

    companion object {

        //
        const val YPSOPUMP_BT_ADDRESS = "EC:2A:F0" // aYpsoPumpBle extends aBleDevice

        private var mapByUuid: HashMap<String, YpsoGattCharacteristic>

        fun lookup(uuid: UUID): YpsoGattCharacteristic {
            return lookup(uuid.toString())
        }

        fun lookup(uuid: String): YpsoGattCharacteristic {
            return if (mapByUuid.containsKey(uuid.lowercase())) {
                mapByUuid[uuid.lowercase()]!!
            } else {
                UNKNOWN
            }
        }

        // table of names for uuids
        init {
            mapByUuid = HashMap()
            for (value in values()) {
                if (value.uuid.isNotBlank()) {
                    mapByUuid[value.uuid] = value
                }
            }
        }
    }

}