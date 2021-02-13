package info.nightscout.androidaps.plugins.pump.combo.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by andy on 3/17/18.
 */
public class ComboDataUtil {

    Map<String, List<ErrorState>> errorMap = new HashMap<>();

    private static ComboDataUtil comboDataUtil = new ComboDataUtil();


    private ComboDataUtil() {
    }


    public static ComboDataUtil getInstance() {
        return comboDataUtil;
    }


    public void addError(Exception exception) {
        String exceptionMsg = exception.getMessage();

        if (!errorMap.containsKey(exceptionMsg)) {
            List<ErrorState> list = new ArrayList<>();
            list.add(createErrorState(exception));

            errorMap.put(exceptionMsg, list);
        } else {
            errorMap.get(exceptionMsg).add(createErrorState(exception));
        }
    }


    private ErrorState createErrorState(Exception exception) {
        ErrorState errorState = new ErrorState();
        errorState.setException(exception);
        errorState.setTimeInMillis(System.currentTimeMillis());

        return errorState;
    }


    public void clearErrors() {
        if (errorMap != null)
            this.errorMap.clear();
        else
            this.errorMap = new HashMap<>();
    }


    public boolean isErrorPresent() {
        return !this.errorMap.isEmpty();
    }


    public String getErrorString() {
        if (!isErrorPresent()) {
            return "-";
        } else {
            int errorCount = 0;

            for (List<ErrorState> errorStates : errorMap.values()) {
                errorCount += errorStates.size();
            }

            return "" + errorCount;
        }

    }
}
