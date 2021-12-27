package info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response;


import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;

//@Setter
//@Getter
//@SuperBuilder
//@NoArgsConstructor
//@AllArgsConstructor
public class TemporaryBasalResponse extends CommandResponseAbstract {

    private TempBasalPair tempBasalPair;

    public TemporaryBasalResponse(YpsoPumpCommandType commandType, boolean success, String errorDescription,
                                  TempBasalPair tempBasalPair) {
        super(commandType, success, errorDescription);
        this.tempBasalPair = tempBasalPair;
    }


    public static TemporaryBasalResponse.Builder builder() {
        return new TemporaryBasalResponse.Builder();
    }

    public TempBasalPair getTempBasalPair() {
        return tempBasalPair;
    }

    public void setTempBasalPair(TempBasalPair tempBasalPair) {
        this.tempBasalPair = tempBasalPair;
    }


    public static class Builder {

        YpsoPumpCommandType commandType;
        boolean success;
        String errorDescription;
        TempBasalPair tempBasalPair;

        Builder() {
        }

        public TemporaryBasalResponse.Builder commandType(YpsoPumpCommandType commandType) {
            this.commandType = commandType;
            return this;
        }

        public TemporaryBasalResponse.Builder success(boolean success) {
            this.success = success;
            return this;
        }


        public TemporaryBasalResponse.Builder errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }

        public TemporaryBasalResponse.Builder tempBasalPair(TempBasalPair tempBasalPair) {
            this.tempBasalPair = tempBasalPair;
            return this;
        }

        public TemporaryBasalResponse build() {
            return new TemporaryBasalResponse(commandType, success, errorDescription, this.tempBasalPair);
        }

    }


}
