package info.nightscout.androidaps.interfaces;

import info.nightscout.androidaps.plugins.pump.common.defs.PumpCapability;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpTempBasalType;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType;

/**
 * Created by mike on 08.12.2016.
 */

public class PumpDescription {
    public PumpType pumpType = PumpType.GenericAAPS;

    public PumpDescription() {
        resetSettings();
    }

    public PumpDescription(PumpType pumpType) {
        this();
        setPumpDescription(pumpType);
    }

    public static final int NONE = 0;
    public static final int PERCENT = 0x01;
    public static final int ABSOLUTE = 0x02;

    public boolean isBolusCapable;
    public double bolusStep;

    public boolean isExtendedBolusCapable;
    public double extendedBolusStep;
    public double extendedBolusDurationStep;
    public double extendedBolusMaxDuration;

    public boolean isTempBasalCapable;
    public int tempBasalStyle;

    public int maxTempPercent;
    public int tempPercentStep;

    public double maxTempAbsolute;
    public double tempAbsoluteStep;

    public int tempDurationStep;
    public boolean tempDurationStep15mAllowed;
    public boolean tempDurationStep30mAllowed;
    public int tempMaxDuration;

    public boolean isSetBasalProfileCapable;
    public double basalStep;
    public double basalMinimumRate;
    public double basalMaximumRate;

    public boolean isRefillingCapable;
    public boolean isBatteryReplaceable;

    public boolean storesCarbInfo;

    public boolean is30minBasalRatesCapable;

    public boolean supportsTDDs;
    public boolean needsManualTDDLoad;

    public boolean hasCustomUnreachableAlertCheck;

    public void resetSettings() {
        isBolusCapable = true;
        bolusStep = 0.1d;

        isExtendedBolusCapable = true;
        extendedBolusStep = 0.1d;
        extendedBolusDurationStep = 30;
        extendedBolusMaxDuration = 12 * 60;

        isTempBasalCapable = true;
        tempBasalStyle = PERCENT;
        maxTempPercent = 200;
        tempPercentStep = 10;
        maxTempAbsolute = 10;
        tempAbsoluteStep = 0.05d;
        tempDurationStep = 60;
        tempMaxDuration = 12 * 60;
        tempDurationStep15mAllowed = false;
        tempDurationStep30mAllowed = false;

        isSetBasalProfileCapable = true;
        basalStep = 0.01d;
        basalMinimumRate = 0.04d;
        basalMaximumRate = 25d;
        is30minBasalRatesCapable = false;

        isRefillingCapable = true;
        isBatteryReplaceable = true;
        storesCarbInfo = true;

        supportsTDDs = false;
        needsManualTDDLoad = true;

        hasCustomUnreachableAlertCheck = false;
    }

    public void setPumpDescription(PumpType pumpType) {
        resetSettings();
        this.pumpType = pumpType;

        PumpCapability pumpCapability = pumpType.getPumpCapability();

        isBolusCapable = pumpCapability.hasCapability(PumpCapability.Bolus);
        bolusStep = pumpType.getBolusSize();

        isExtendedBolusCapable = pumpCapability.hasCapability(PumpCapability.ExtendedBolus);
        extendedBolusStep = pumpType.getExtendedBolusSettings().getStep();
        extendedBolusDurationStep = pumpType.getExtendedBolusSettings().getDurationStep();
        extendedBolusMaxDuration = pumpType.getExtendedBolusSettings().getMaxDuration();

        isTempBasalCapable = pumpCapability.hasCapability(PumpCapability.TempBasal);

        if (pumpType.getPumpTempBasalType() == PumpTempBasalType.Percent) {
            tempBasalStyle = PERCENT;
            maxTempPercent = pumpType.getTbrSettings().getMaxDose().intValue();
            tempPercentStep = (int) pumpType.getTbrSettings().getStep();
        } else {
            tempBasalStyle = ABSOLUTE;
            maxTempAbsolute = pumpType.getTbrSettings().getMaxDose();
            tempAbsoluteStep = pumpType.getTbrSettings().getStep();
        }

        tempDurationStep = pumpType.getTbrSettings().getDurationStep();
        tempMaxDuration = pumpType.getTbrSettings().getMaxDuration();

        tempDurationStep15mAllowed = pumpType.getSpecialBasalDurations()
                .hasCapability(PumpCapability.BasalRate_Duration15minAllowed);
        tempDurationStep30mAllowed = pumpType.getSpecialBasalDurations()
                .hasCapability(PumpCapability.BasalRate_Duration30minAllowed);

        isSetBasalProfileCapable = pumpCapability.hasCapability(PumpCapability.BasalProfileSet);
        basalStep = pumpType.getBaseBasalStep();
        basalMinimumRate = pumpType.getBaseBasalMinValue();

        isRefillingCapable = pumpCapability.hasCapability(PumpCapability.Refill);
        isBatteryReplaceable = pumpCapability.hasCapability(PumpCapability.ReplaceBattery);
        storesCarbInfo = pumpCapability.hasCapability(PumpCapability.StoreCarbInfo);

        supportsTDDs = pumpCapability.hasCapability(PumpCapability.TDD);
        needsManualTDDLoad = pumpCapability.hasCapability(PumpCapability.ManualTDDLoad);

        is30minBasalRatesCapable = pumpCapability.hasCapability(PumpCapability.BasalRate30min);

        hasCustomUnreachableAlertCheck = pumpType.getHasCustomUnreachableAlertCheck();
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("PumpDescription{");
        sb.append("pumpType=").append(pumpType);
        sb.append(", isBolusCapable=").append(isBolusCapable);
        sb.append(", bolusStep=").append(bolusStep);
        sb.append(", isExtendedBolusCapable=").append(isExtendedBolusCapable);
        sb.append(", extendedBolusStep=").append(extendedBolusStep);
        sb.append(", extendedBolusDurationStep=").append(extendedBolusDurationStep);
        sb.append(", extendedBolusMaxDuration=").append(extendedBolusMaxDuration);
        sb.append(", isTempBasalCapable=").append(isTempBasalCapable);
        sb.append(", tempBasalStyle=").append(tempBasalStyle);
        sb.append(", maxTempPercent=").append(maxTempPercent);
        sb.append(", tempPercentStep=").append(tempPercentStep);
        sb.append(", maxTempAbsolute=").append(maxTempAbsolute);
        sb.append(", tempAbsoluteStep=").append(tempAbsoluteStep);
        sb.append(", tempDurationStep=").append(tempDurationStep);
        sb.append(", tempDurationStep15mAllowed=").append(tempDurationStep15mAllowed);
        sb.append(", tempDurationStep30mAllowed=").append(tempDurationStep30mAllowed);
        sb.append(", tempMaxDuration=").append(tempMaxDuration);
        sb.append(", isSetBasalProfileCapable=").append(isSetBasalProfileCapable);
        sb.append(", basalStep=").append(basalStep);
        sb.append(", basalMinimumRate=").append(basalMinimumRate);
        sb.append(", basalMaximumRate=").append(basalMaximumRate);
        sb.append(", isRefillingCapable=").append(isRefillingCapable);
        sb.append(", isBatteryReplaceable=").append(isBatteryReplaceable);
        sb.append(", storesCarbInfo=").append(storesCarbInfo);
        sb.append(", is30minBasalRatesCapable=").append(is30minBasalRatesCapable);
        sb.append(", supportsTDDs=").append(supportsTDDs);
        sb.append(", needsManualTDDLoad=").append(needsManualTDDLoad);
        sb.append(", hasCustomUnreachableAlertCheck=").append(hasCustomUnreachableAlertCheck);
        sb.append('}');
        return sb.toString();
    }
}
