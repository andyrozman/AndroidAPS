package info.nightscout.aaps.pump.tandem.data.defs

import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ApiVersionResponse
import info.nightscout.aaps.pump.common.driver.connector.commands.data.FirmwareVersionInterface

enum class TandemPumpApiVersion(val description: String,
                                val majorVersion: Int,
                                val minorVersion: Int,
                                val isClosedLoopPossible: Boolean,
                                val hasBolus: Boolean = false,
                                val children: Set<TandemPumpApiVersion>? = null) : FirmwareVersionInterface {

    VERSION_0_x("Version 0.x", 0, 0, isClosedLoopPossible = false),
    VERSION_1_x("Version 1.x", 1, 0, isClosedLoopPossible = false),
    VERSION_2_0("Version 2.0", 2, 0, isClosedLoopPossible = false),
    VERSION_2_1("Version 2.1", 2, 1, isClosedLoopPossible = false),
    VERSION_2_2("Version 2.2", 2, 2, isClosedLoopPossible = false),
    VERSION_2_3("Version 2.3", 2, 3, isClosedLoopPossible = false),
    VERSION_2_4("Version 2.4", 2, 4, isClosedLoopPossible = false),
    //VERSION_2_5("Version 2.5", 2, 5, true),
    VERSION_2_5_OR_HIGHER("Version 2.5 or higher", 2, 5, isClosedLoopPossible = false, hasBolus = true),

    // Special versions
    // VERSION_2_2_OR_HIGHER("Version 2.2 or higher", 0, 0, false, setOf(VERSION_2_2, VERSION_2_3, VERSION_2_4, VERSION_2_5_OR_HIGHER)),

    Unknown("", 0, 0, false);

    //constructor()



    fun isSameVersion(requestedVersion: TandemPumpApiVersion) : Boolean {
        return (this == requestedVersion ||
            (requestedVersion.children!=null && requestedVersion.children.contains(this)))
    }


    companion object {

        @JvmStatic
        fun getApiVersionFromResponse(response: ApiVersionResponse): TandemPumpApiVersion {
            return (
                when (response.majorVersion) {
                    0    -> VERSION_0_x
                    1    -> VERSION_1_x

                    2    -> {
                        when (response.minorVersion) {
                            0    -> VERSION_2_0
                            1    -> VERSION_2_1
                            2    -> VERSION_2_2
                            3    -> VERSION_2_3
                            4    -> VERSION_2_4
                            else -> VERSION_2_5_OR_HIGHER
                        }
                    }

                    else -> VERSION_2_5_OR_HIGHER
                })
        }
    }



}