package info.nightscout.androidaps.plugins.pump.tandem.defs

import info.nightscout.androidaps.plugins.pump.common.driver.connector.command.data.FirmwareVersionInterface

enum class TandemPumpApiVersion(val description: String,
                                val majorVersion: Int,
                                val minorVersion: Int,
                                val isClosedLoopPossible: Boolean) : FirmwareVersionInterface {

    VERSION_2_1("Version 2.1", 2, 0, false),
    VERSION_2_2("Version 2.2", 2, 2, false),
    //VERSION_1_6("Version 1.6", true),
    Unknown("", 0, 0, false);



}