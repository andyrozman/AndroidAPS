package info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response;

import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;

public class CommandResponseAbstract {

    private YpsoPumpCommandType commandType;
    private boolean success;
    private String errorDescription;


    public CommandResponseAbstract(YpsoPumpCommandType commandType, boolean success, String errorDescription) {
        this.commandType = commandType;
        this.success = success;
        this.errorDescription = errorDescription;
    }


    public YpsoPumpCommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(YpsoPumpCommandType commandType) {
        this.commandType = commandType;
    }


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

}
