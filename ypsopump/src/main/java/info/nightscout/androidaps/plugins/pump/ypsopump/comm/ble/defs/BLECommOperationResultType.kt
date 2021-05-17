package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs

enum class BLECommOperationResultType(private val code: Int) {
    UNKNOWN(-1),
    RESULT_NONE(0),
    RESULT_SUCCESS(1),
    RESULT_TIMEOUT(2),
    RESULT_BUSY(3),
    RESULT_INTERRUPTED(4),
    RESULT_NOT_CONFIGURED(5);

    companion object {

        private var mapByCode: MutableMap<Int, BLECommOperationResultType>

        fun getByCode(status: Int): BLECommOperationResultType? {
            return if (mapByCode.containsKey(status)) {
                mapByCode[status]
            } else {
                UNKNOWN
            }
        }

        init {
            mapByCode = HashMap()
            for (value in values()) {
                mapByCode[value.code] = value
            }
        }
    }

}