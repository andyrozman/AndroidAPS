package info.nightscout.aaps.pump.tandem.data.defs

enum class TandemHistoryLogType (var typeId: Int) {

    TimeChange(13),
    DateChange(14),
    BG(16),
    CGM(256),
    BolusDelivery(280),
    BolusCompleted(20),
    BolusRequestedMsg1(64),
    BolusRequestedMsg2(65),
    BolusRequestedMsg3(66),
    BolexCompleted(21)
    ;

}