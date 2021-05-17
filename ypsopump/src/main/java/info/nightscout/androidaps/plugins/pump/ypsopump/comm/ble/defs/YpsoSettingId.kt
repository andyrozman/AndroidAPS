package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs

enum class YpsoSettingId(val id: Int) {

    BasalProfileSelection(1),
    BolusIncrementStepSelection(3),
    RemainingLifetime(5),
    BasalPatternA(14),
    BasalPatternB(38),
    BolusAmountLimit(112),
    BasalRateLimit(113);

}