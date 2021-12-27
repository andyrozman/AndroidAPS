package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations

import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.BLECommOperationResultType

class BLECommOperationResult {

    var value: ByteArray? = null

    var operationResultType : BLECommOperationResultType = BLECommOperationResultType.RESULT_NONE
        get() = field

    val isSuccessful: Boolean
        get() = operationResultType == BLECommOperationResultType.RESULT_SUCCESS

}