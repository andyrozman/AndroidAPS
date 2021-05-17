package info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response;

import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;

public class BasalProfileResponse extends CommandResponseAbstract {

    Double[] basalProfile; // 0-23, each with value

    public BasalProfileResponse(YpsoPumpCommandType commandType, boolean success, String errorDescription,
                                Double[] basalProfile) {
        super(commandType, success, errorDescription);
        this.basalProfile = basalProfile;
    }


    public static BasalProfileResponse.Builder builder() {
        return new BasalProfileResponse.Builder();
    }


    public static class Builder {

        YpsoPumpCommandType commandType;
        boolean success;
        String errorDescription;
        Double[] basalProfile;

        Builder() {
        }

        public BasalProfileResponse.Builder commandType(YpsoPumpCommandType commandType) {
            this.commandType = commandType;
            return this;
        }

        public BasalProfileResponse.Builder success(boolean success) {
            this.success = success;
            return this;
        }


        public BasalProfileResponse.Builder errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }


        public BasalProfileResponse.Builder basalProfile(Double[] basalProfile) {
            this.basalProfile = basalProfile;
            return this;
        }

        public BasalProfileResponse build() {
            return new BasalProfileResponse(commandType, success, errorDescription, this.basalProfile);
        }

    }

}
