package info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response;

import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;

public class CommandResponse extends CommandResponseAbstract {

    public CommandResponse(YpsoPumpCommandType commandType, boolean success, String errorDescription) {
        super(commandType, success, errorDescription);
    }


    public static CommandResponse.CommandResponseBuilder builder() {
        return new CommandResponse.CommandResponseBuilder();
    }

    public static class CommandResponseBuilder {

        YpsoPumpCommandType commandType;
        boolean success;
        String errorDescription;

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


        public CommandResponse.CommandResponseBuilder errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }

        public CommandResponse build() {
            return new CommandResponse(this.commandType,
                    this.success, this.errorDescription);
        }

    }


}
