package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs

import java.util.*

enum class YpsoGattService(private val uuid: String?, val description: String) {
    SERVICE_GENERIC_ACCESS("00001800-0000-1000-8000-00805f9b34fb", "Service - Generic Access"),
    SERVICE_GENERIC_ATTRIBUTE("00001800-0000-1000-8000-00805f9b34fb", "Service - Generic Attribute"),
    SERVICE_YPSO_1("fb349b5f-8000-0080-0010-0000adde0000", "Service - Ypso Unknown Service 1"),
    SERVICE_YPSO_2("fb349b5f-8000-0080-0010-0000efbe0000", "Service - Ypso Unknown Service 2"),
    SERVICE_YPSO_3("fb349b5f-8000-0080-0010-0000dec00000", "Service - Ypso Unknown Service 3"),
    UNKNOWN(null, "Unknown Service");

    companion object {

        private var mapByUuid: MutableMap<String, YpsoGattService>

        fun lookup(uuid: UUID): YpsoGattService? {
            return lookup(uuid.toString())
        }

        @JvmStatic
        fun lookup(uuid: String): YpsoGattService? {
            return if (mapByUuid.containsKey(uuid.toLowerCase(Locale.ROOT))) {
                mapByUuid[uuid.toLowerCase(Locale.ROOT)]
            } else {
                UNKNOWN
            }
        }

        @JvmStatic
        fun isYpsoPumpDevice(uuid: UUID): Boolean {
            return mapByUuid.containsKey(uuid.toString())
        }

        // table of names for uuids
        init {
            mapByUuid = HashMap()
            for (value in values()) {
                if (value.uuid != null) {
                    mapByUuid[value.uuid] = value
                }
            }
        }
    }

}