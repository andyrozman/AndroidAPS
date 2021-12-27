package info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response;

import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;

public class SimpleDataCommandResponse extends CommandResponseAbstract {

    Integer integerData;
    Double doubleData;
    String stringData;


    public SimpleDataCommandResponse(YpsoPumpCommandType commandType, boolean success, String errorDescription,
                                     Integer integerData, Double doubleData, String stringData) {
        super(commandType, success, errorDescription);
        this.integerData = integerData;
        this.doubleData = doubleData;
        this.stringData = stringData;
    }


    public static SimpleDataCommandResponse.Builder builder() {
        return new SimpleDataCommandResponse.Builder();
    }


    public static class Builder {

        YpsoPumpCommandType commandType;
        boolean success;
        String errorDescription;
        Integer integerData;
        Double doubleData;
        String stringData;


        Builder() {
        }


        public Builder commandType(YpsoPumpCommandType commandType) {
            this.commandType = commandType;
            return this;
        }


        public Builder success(boolean success) {
            this.success = success;
            return this;
        }


        public Builder errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }


        public Builder integerData(Integer integerData) {
            this.integerData = integerData;
            return this;
        }


        public Builder doubleData(Double doubleData) {
            this.doubleData = doubleData;
            return this;
        }


        public Builder stringData(String stringData) {
            this.stringData = stringData;
            return this;
        }


        public SimpleDataCommandResponse build() {
            return new SimpleDataCommandResponse(commandType, success, errorDescription, this.integerData, this.doubleData, this.stringData);
        }

    }


}
