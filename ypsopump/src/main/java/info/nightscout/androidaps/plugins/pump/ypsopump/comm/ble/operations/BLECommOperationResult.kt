package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations

/**
 * Created by geoff on 5/26/16.
 */
class BLECommOperationResult {

    var value: ByteArray? = null

    var resultCode = 0
        get() = field

    val isSuccessful: Boolean
        get() = resultCode == RESULT_SUCCESS

    companion object {
        const val RESULT_NONE = 0
        const val RESULT_SUCCESS = 1
        const val RESULT_TIMEOUT = 2
        const val RESULT_BUSY = 3
        const val RESULT_INTERRUPTED = 4
        const val RESULT_NOT_CONFIGURED = 5
    }
}