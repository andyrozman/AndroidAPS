package info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.parameters;

import java.util.List;
import java.util.Objects;

import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;


public class CommandParameters {

    protected boolean singleCommand;
    protected YpsoPumpCommandType commandType;
    protected List<YpsoPumpCommandType> commandTypeList;

//    public CommandParameters(YpsoPumpCommandType commandType) {
//        this.singleCommand = true;
//        this.commandType = commandType;
//    }

//
//    public CommandParameters(List<YpsoPumpCommandType> commands) {
//        this.singleCommand = false;
//        this.commandTypeList = commands;
//    }

    public CommandParameters(boolean singleCommand, YpsoPumpCommandType commandType, List<YpsoPumpCommandType> commandTypeList) {
        this.commandType = commandType;
        this.singleCommand = singleCommand;
        this.commandTypeList = commandTypeList;
    }

    public boolean isSingleCommand() {
        return singleCommand;
    }

    public void setSingleCommand(boolean singleCommand) {
        this.singleCommand = singleCommand;
    }

    public YpsoPumpCommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(YpsoPumpCommandType commandType) {
        this.commandType = commandType;
    }

    public List<YpsoPumpCommandType> getCommandTypeList() {
        return commandTypeList;
    }

    public void setCommandTypeList(List<YpsoPumpCommandType> commandTypeList) {
        this.commandTypeList = commandTypeList;
    }
//
//    public void setCommandType(YpsoPumpCommandType commandType) {
//        this.commandType = commandType;
//        this.singleCommand = true;
//    }
//
//    public void setCommandTypeList(List<YpsoPumpCommandType> commandTypeList) {
//        this.commandTypeList = commandTypeList;
//        this.singleCommand = false;
//    }


//    private Integer bolus;
//    private Integer carbs;
//    private Integer duration;

//    public CommandParameters(Integer bolus,
//                             Integer carbs,
//                             Integer duration
//
//                             ) {
//        this.bolus = bolus;
//        this.carbs = carbs;
//        this.duration = duration;
//    }
//
//    public static CommandParameters.CommandParametersBuilder builder() {
//        return new CommandParameters.CommandParametersBuilder();
//    }
//
//    public static class CommandParametersBuilder {
//
//        private Integer bolus;
//        private Integer carbs;
//        private Integer duration;
//
//        CommandParametersBuilder() {
//        }
//
//        public CommandParameters.CommandParametersBuilder bolus(Integer bolus) {
//            this.bolus = bolus;
//            return this;
//        }
//
//        public CommandParameters.CommandParametersBuilder carbs(Integer carbs) {
//            this.carbs = carbs;
//            return this;
//        }
//
//        public CommandParameters.CommandParametersBuilder duration(Integer duration) {
//            this.duration = duration;
//            return this;
//        }
//
//        public CommandParameters build() {
//            return new CommandParameters(this.bolus,
//                    this.carbs,
//                    this.duration);
//        }
//
//    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandParameters)) return false;
        CommandParameters that = (CommandParameters) o;
        return singleCommand == that.singleCommand &&
                commandType == that.commandType &&
                commandTypeList.equals(that.commandTypeList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(singleCommand, commandType, commandTypeList);
    }


}
