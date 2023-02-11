package info.nightscout.androidaps.plugins.pump.tandem.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.DataCommandResponse
import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response.ResultCommandResponse

import info.nightscout.androidaps.plugins.pump.tandem.comm.ble.defs.TandemCommandType

class TandemCommand<E>(var injector: HasAndroidInjector,
                    var tandemCommandType: TandemCommandType) {

    init {

    }





    fun executeWithData() : DataCommandResponse<E>? {
        return null
    }


    fun executeWithResult() : ResultCommandResponse? {
        return null
    }


}