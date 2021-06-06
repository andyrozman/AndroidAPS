package info.nightscout.androidaps.plugins.pump.ypsopump.defs;

public enum YpsoPumpFirmware {
    VERSION_1_0("Version 1.0", false),
    //VERSION_1_1("Version 1.1", false),
    VERSION_1_5("Version 1.5", false),
    VERSION_1_6("Version 1.6", true),
    Unknown("", false);

    private String description;
    private boolean closedLoopPossible;

    YpsoPumpFirmware(String description, boolean isClosedLoopPossible) {
        this.description = description;
        closedLoopPossible = isClosedLoopPossible;
    }

    public String getDescription() {
        return description;
    }

    public boolean isClosedLoopPossible() {
        return closedLoopPossible;
    }

}
