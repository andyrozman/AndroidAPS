package info.nightscout.androidaps.plugins.pump.common.driver.connector.command.response;

import info.nightscout.androidaps.plugins.pump.common.driver.connector.defs.PumpCommandType;

public class ResultCommandResponse extends CommandResponseAbstract<Boolean> {

    public ResultCommandResponse(PumpCommandType commandType, boolean success, String errorDescription) {
        super(commandType, success, errorDescription, success);
    }

    public ResultCommandResponse cloneWithNewCommandType(PumpCommandType pumpCommandType) {
        return new ResultCommandResponse(pumpCommandType, isSuccess(), getErrorDescription());
    }


//    public static CommandResponse.CommandResponseBuilder builder() {
//        return new CommandResponse.CommandResponseBuilder();
//    }

//    public static abstract class CommandResponseBuilder<E> {
//
//        PumpCommandType commandType;
//        boolean success;
//        String errorDescription;
//        E value;
//
//        CommandResponseBuilder() {
//        }
//
//        public CommandResponse.CommandResponseBuilder commandType(PumpCommandType commandType) {
//            this.commandType = commandType;
//            return this;
//        }
//
//        public CommandResponse.CommandResponseBuilder success(boolean success) {
//            this.success = success;
//            return this;
//        }
//
//
//        public CommandResponse.CommandResponseBuilder errorDescription(String errorDescription) {
//            this.errorDescription = errorDescription;
//            return this;
//        }
//
//        public CommandResponse build() {
//            return new CommandResponse(this.commandType,
//                    this.success, this.errorDescription, this.value);
//        }
//
//    }


}
