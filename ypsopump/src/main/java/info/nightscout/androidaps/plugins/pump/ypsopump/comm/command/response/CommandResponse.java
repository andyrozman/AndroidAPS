package info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response;

import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;

public class CommandResponse {

    YpsoPumpCommandType commandType;
    boolean success;
    boolean enacted;
    boolean error;
    String errorDescription;
    boolean cantConnectToPump;

    public CommandResponse(YpsoPumpCommandType commandType, boolean success, boolean enacted, boolean error, String errorDescription, boolean cantConnectToPump) {
        this.commandType = commandType;
        this.success = success;
        this.enacted = enacted;
        this.error = error;
        this.errorDescription = errorDescription;
        this.cantConnectToPump = cantConnectToPump;
    }


    public static CommandResponse.CommandResponseBuilder builder() {
        return new CommandResponse.CommandResponseBuilder();
    }


    public static class CommandResponseBuilder {

        YpsoPumpCommandType commandType;
        boolean success;
        boolean enacted;
        boolean error;
        String errorDescription;
        boolean cantConnectToPump;

        CommandResponseBuilder() {
        }

        public CommandResponse.CommandResponseBuilder commandType(YpsoPumpCommandType commandType) {
            this.commandType = commandType;
            return this;
        }

        public CommandResponse.CommandResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public CommandResponse.CommandResponseBuilder enacted(boolean enacted) {
            this.enacted = enacted;
            return this;
        }

        public CommandResponse.CommandResponseBuilder error(boolean error) {
            this.error = error;
            return this;
        }

        public CommandResponse.CommandResponseBuilder errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }

        public CommandResponse.CommandResponseBuilder cantConnectToPump(boolean cantConnectToPump) {
            this.cantConnectToPump = cantConnectToPump;
            return this;
        }

        public CommandResponse build() {
            return new CommandResponse(this.commandType,
                    this.success, this.enacted, this.error, this.errorDescription, this.cantConnectToPump);
        }

    }


}
