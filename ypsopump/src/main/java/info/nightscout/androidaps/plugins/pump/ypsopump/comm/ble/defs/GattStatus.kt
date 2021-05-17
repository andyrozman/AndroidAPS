package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs

import java.util.*

enum class GattStatus(private val status: Int) {
    GATT_SUCCESS(0),  // A GATT operation completed successfully
    GATT_READ_NOT_PERMITTED(0x2),  // GATT read operation is not permitted
    GATT_WRITE_NOT_PERMITTED(0x3),  // GATT write operation is not permitted
    GATT_INSUFFICIENT_AUTHENTICATION(0x5),  // Insufficient authentication for a given operation
    GATT_REQUEST_NOT_SUPPORTED(0x6),  // The given request is not supported
    GATT_INSUFFICIENT_ENCRYPTION(0xf),  // Insufficient encryption for a given operation
    GATT_INVALID_OFFSET(0x7),  // A read or write operation was requested with an invalid offset
    GATT_INVALID_ATTRIBUTE_LENGTH(0xd),  // A write operation exceeds the maximum length of the attribute
    GATT_CONNECTION_CONGESTED(0x8f),  // A remote device connection is congested.
    GATT_BUG_133(133),  // Weird Bug 133
    GATT_FAILURE(0x101),  // A GATT operation failed, errors other than the above
    UNKNOWN(-1);

    companion object {
        private var mapByStatus: MutableMap<Int, GattStatus>

        fun getByStatusCode(status: Int): GattStatus {
            return if (mapByStatus.containsKey(status)) {
                mapByStatus[status]!!
            } else {
                UNKNOWN
            }
        }

        init {
            mapByStatus = HashMap()
            for (value in values()) {
                mapByStatus[value.status] = value
            }
        }
    }

}