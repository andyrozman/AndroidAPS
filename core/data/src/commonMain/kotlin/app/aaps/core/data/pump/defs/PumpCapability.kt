package app.aaps.core.data.pump.defs

import app.aaps.core.data.pump.defs.Capability.*

enum class PumpCapability {

    // grouped by pump
    MDI(Bolus),
    VirtualPumpCapabilities(Bolus, ExtendedBolus, TempBasal, BasalProfileSet, Refill, ReplaceBattery),
    ComboCapabilities(Bolus, TempBasal, BasalProfileSet, Refill, ReplaceBattery, TDD, ManualTDDLoad),
    DanaCapabilities(Bolus, ExtendedBolus, TempBasal, BasalProfileSet, Refill, ReplaceBattery, TDD, ManualTDDLoad),
    DanaWithHistoryCapabilities(Bolus, ExtendedBolus, TempBasal, BasalProfileSet, Refill, ReplaceBattery, TDD, ManualTDDLoad),
    InsightCapabilities(Bolus, ExtendedBolus, TempBasal, BasalProfileSet, Refill, ReplaceBattery, BasalRate30min),
    MedtronicCapabilities(Bolus, TempBasal, BasalProfileSet, Refill, ReplaceBattery, TDD),
    OmnipodCapabilities(Bolus, TempBasal, BasalProfileSet, BasalRate30min),
    YpsomedCapabilities(Bolus, ExtendedBolus, TempBasal, BasalProfileSet, Refill, ReplaceBattery, TDD, ManualTDDLoad),  // BasalRates (separately grouped)
    DiaconnCapabilities(Bolus, ExtendedBolus, TempBasal, BasalProfileSet, Refill, ReplaceBattery, TDD, ManualTDDLoad), //
    EopatchCapabilities(Bolus, ExtendedBolus, TempBasal, BasalProfileSet, BasalRate30min),
    MedtrumCapabilities(Bolus, TempBasal, BasalProfileSet, BasalRate30min, TDD), // Technically the pump supports ExtendedBolus, but not implemented (yet)
    CarelevoCapabilities(Bolus, ExtendedBolus, TempBasal, BasalProfileSet, BasalRate30min),
    TandemSlimCapabilities(Refill, ReplaceBattery),
    TandemMobiCapabilities(Bolus, TempBasal, BasalProfileSet, Refill, BasalRate30min),
    // Diaconn set without ReplaceBattery: the Equil wizard fits the charged battery.
    // Refill: users asked for Prime/Fill. The pump can be detached from the base plate, so the prime can go into the air.
    // It is a PRIMING bolus and adds no IOB, so a prime while the pump is attached is insulin the loop does not see.
    // No TDD: the driver cannot read daily totals from the pump (loadTDDs always failed), so AAPS counts them itself.
    EquilCapabilities(Bolus, ExtendedBolus, TempBasal, BasalProfileSet, Refill)
    ;


    var children: ArrayList<Capability> = ArrayList()

    constructor(vararg capabilities: Capability) {
        for (capability in capabilities) {
            children.add(capability)
        }
    }

    fun hasCapability(capability: Capability): Boolean = children.contains(capability)
}
