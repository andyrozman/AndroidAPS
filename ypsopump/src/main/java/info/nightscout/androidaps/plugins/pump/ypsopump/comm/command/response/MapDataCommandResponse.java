package info.nightscout.androidaps.plugins.pump.ypsopump.comm.command.response;

import java.util.Map;

import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;

public class MapDataCommandResponse extends CommandResponseAbstract {

    Map<String, Object> mapData;

    public MapDataCommandResponse(YpsoPumpCommandType commandType, boolean success, String errorDescription,
                                  Map<String, Object> mapData) {
        super(commandType, success, errorDescription);
        this.mapData = mapData;
    }


    public static MapDataCommandResponse.Builder builder() {
        return new MapDataCommandResponse.Builder();
    }


    public static class Builder {

        YpsoPumpCommandType commandType;
        boolean success;
        String errorDescription;
        Map<String, Object> mapData;

        Builder() {
        }

        public MapDataCommandResponse.Builder commandType(YpsoPumpCommandType commandType) {
            this.commandType = commandType;
            return this;
        }

        public MapDataCommandResponse.Builder success(boolean success) {
            this.success = success;
            return this;
        }


        public MapDataCommandResponse.Builder errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }


        public MapDataCommandResponse.Builder basalProfile(Map<String, Object> mapData) {
            this.mapData = mapData;
            return this;
        }

        public MapDataCommandResponse build() {
            return new MapDataCommandResponse(commandType, success, errorDescription, this.mapData);
        }

    }


}
