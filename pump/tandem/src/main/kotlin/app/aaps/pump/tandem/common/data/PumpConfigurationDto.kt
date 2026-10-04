package app.aaps.pump.tandem.common.data

data class PumpConfigurationDto(
    val pumpAddress: String,
    var validSharedConfiguration: Boolean,
    var sharedConfigurationAlreadyApplied: Boolean,
    var pumpSerial: String,
    val pumpBondStatus: Int,
    val useSharedConnection: Boolean
)
