package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.YpsoPumpBLE
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.operations.BLECommOperationResult

interface BLECommandInterface<T> {

    var commandResponse: T?

    var bleCommOperationResult: BLECommOperationResult?

    fun execute(pumpBle: YpsoPumpBLE): Boolean
}