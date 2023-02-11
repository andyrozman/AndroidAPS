package info.nightscout.androidaps.plugins.pump.tandem.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.aaps.pump.common.driver.connector.commands.response.DataCommandResponse
import info.nightscout.aaps.pump.common.driver.connector.commands.response.ResultCommandResponse

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