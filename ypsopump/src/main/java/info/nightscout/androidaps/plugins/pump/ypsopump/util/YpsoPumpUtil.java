package info.nightscout.androidaps.plugins.pump.ypsopump.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

import info.nightscout.androidaps.logging.AAPSLogger;
import info.nightscout.androidaps.logging.LTag;
import info.nightscout.androidaps.plugins.bus.RxBusWrapper;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState;
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil;
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpCommandType;
import info.nightscout.androidaps.plugins.pump.ypsopump.driver.YpsopumpPumpStatus;
import info.nightscout.androidaps.plugins.pump.ypsopump.event.EventPumpStatusChanged;

public class YpsoPumpUtil {

    public static final int MAX_RETRY = 2;
    private final AAPSLogger aapsLogger;
    private final RxBusWrapper rxBus;
    private final YpsopumpPumpStatus ypsopumpPumpStatus;

    private static PumpDriverState driverStatus = PumpDriverState.Sleeping;
    private YpsoPumpCommandType pumpCommandType;
    public Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Inject
    public YpsoPumpUtil(
            AAPSLogger aapsLogger,
            RxBusWrapper rxBus,
            YpsopumpPumpStatus ypsopumpPumpStatus
    ) {
        this.aapsLogger = aapsLogger;
        this.rxBus = rxBus;
        this.ypsopumpPumpStatus = ypsopumpPumpStatus;
    }

    public YpsopumpPumpStatus getYpsopumpPumpStatus() {
        return this.ypsopumpPumpStatus;
    }


    public PumpDriverState getDriverStatus() {
        return (PumpDriverState) workWithStatusAndCommand(StatusChange.GetStatus, null, null);
    }

    public YpsoPumpCommandType getCurrentCommand() {
        return (YpsoPumpCommandType) workWithStatusAndCommand(StatusChange.GetCommand, null, null);
    }

    public void resetDriverStatusToConnected() {
        workWithStatusAndCommand(StatusChange.SetStatus, PumpDriverState.Connected, null);
    }

    public void setDriverStatus(PumpDriverState status) {
        aapsLogger.debug(LTag.PUMP, "Set driver status: " + status.name());
        workWithStatusAndCommand(StatusChange.SetStatus, status, null);
    }

    public void setCurrentCommand(YpsoPumpCommandType currentCommand) {
        aapsLogger.debug(LTag.PUMP, "Set current command: " + currentCommand.name());
        workWithStatusAndCommand(StatusChange.SetCommand, PumpDriverState.ExecutingCommand, currentCommand);
    }

    public synchronized Object workWithStatusAndCommand(StatusChange type, PumpDriverState driverStatusIn, YpsoPumpCommandType pumpCommandType) {

        //aapsLogger.debug(LTag.PUMP, "Status change type: " + type.name() + ", DriverStatus: " + (driverStatus != null ? driverStatus.name() : ""));

        switch (type) {

            case GetStatus:
                //aapsLogger.debug(LTag.PUMP, "GetStatus: DriverStatus: " + driverStatus);
                return driverStatus;

            case GetCommand:
                return this.pumpCommandType;

            case SetStatus: {
                //aapsLogger.debug(LTag.PUMP, "SetStatus: DriverStatus: " + driverStatus + ", Incoming: " + driverStatusIn);
                driverStatus = driverStatusIn;
                this.pumpCommandType = null;
                //aapsLogger.debug(LTag.PUMP, "SetStatus: DriverStatus: " + driverStatus);
                rxBus.send(new EventPumpStatusChanged());
            }
            break;

            case SetCommand: {
                this.driverStatus = driverStatusIn;
                this.pumpCommandType = pumpCommandType;
                rxBus.send(new EventPumpStatusChanged());
            }
            break;
        }

        return null;
    }


    public void sleepSeconds(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void sleep(long miliseconds) {
        try {
            Thread.sleep(miliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static boolean isSame(Double d1, Double d2) {
        double diff = d1 - d2;

        return (Math.abs(diff) <= 0.000001);
    }

    public static boolean isSame(Double d1, Integer d2) {
        double diff = d1 - d2;

        return (Math.abs(diff) <= 0.000001);
    }

    private enum StatusChange {
        GetStatus,
        GetCommand,
        SetStatus,
        SetCommand
    }


    public static byte[] computeUserLevelPassword(String btAddress) {
        byte[] sourceArray = new byte[]{91, (byte) 215, 16, 102, 1, (byte) 242, 79, 60, (byte) 143, 107};
        byte[] array = new byte[16];
        String[] address = btAddress.split(":");
        Log.d("DD", "BTAddress: " + address);
        array[0] = convertIntegerStringToByte(address[0]);
        array[1] = convertIntegerStringToByte(address[1]);
        array[2] = convertIntegerStringToByte(address[2]);
        array[3] = convertIntegerStringToByte(address[3]);
        array[4] = convertIntegerStringToByte(address[4]);
        array[5] = convertIntegerStringToByte(address[5]);
        System.arraycopy(sourceArray, 0, array, 6, 10);
//        return MD5.Create().ComputeHash(array);

//        MessageDigest.getInstance(MD5)
//
//        DigestUtils.md5Hex(password).toUpperCase();

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(array);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return array;
        }


        //return array;
    }

    private static byte convertIntegerStringToByte(String hex) {
        //Log.d("DD", "BTAddress Part: " + hex);
        return (new Integer(Integer.parseInt(hex, 16))).byteValue();
    }


    public static byte[] getSettingIdAsArray(int settingId) {
        byte[] array = new byte[8];
        CreateGLB_SAFE_VAR(settingId, array);
        return array;
    }

    public static byte[] getBytesFromInt16(int value) {
        byte[] array = getBytesFromInt(value);
        Log.d("HHH", ByteUtil.getHex(array));
        return new byte[]{array[3], array[2]}; // 2, 3
    }

    public static byte[] getBytesFromInt(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    //public static byte[]


    // TOOO fix
    public static void CreateGLB_SAFE_VAR(int piValue, byte[] pbyBuffer) {
        pbyBuffer[0] = (byte) piValue;
        piValue >>= 8;
        pbyBuffer[1] = (byte) piValue;
        piValue >>= 8;
        pbyBuffer[2] = (byte) piValue;
        piValue >>= 8;
        pbyBuffer[3] = (byte) piValue;
        pbyBuffer[4] = (byte) (~pbyBuffer[0]);
        pbyBuffer[5] = (byte) (~pbyBuffer[1]);
        pbyBuffer[6] = (byte) (~pbyBuffer[2]);
        pbyBuffer[7] = (byte) (~pbyBuffer[3]);
    }

    public static int getValueFromeGLB_SAFE_VAR(byte[] paBuffer) {
        if ((~paBuffer[7] & 0xFF) != (paBuffer[3] & 0xFF) || (~paBuffer[6] & 0xFF) != (paBuffer[2] & 0xFF) || (~paBuffer[5] & 0xFF) != (paBuffer[1] & 0xFF) || (~paBuffer[4] & 0xFF) != (paBuffer[0] & 0xFF)) {
            throw new InvalidParameterException("Invalid GLB_SAFE_VAR byte array:" + paBuffer);
        }
        return byteToInt(paBuffer, 0, 4);
    }


    public static int byteToInt(byte[] data, int start, int length) {
        if (length == 1) {
            return ByteUtil.toInt(data[start]);
        } else if (length == 2) {
            return ByteUtil.toInt(data[start], data[start + 1], ByteUtil.BitConversion.LITTLE_ENDIAN);
        } else if (length == 3) {
            return ByteUtil.toInt(data[start], data[start + 1], data[start + 2], ByteUtil.BitConversion.LITTLE_ENDIAN);
        } else if (length == 4) {
            return ByteUtil.toInt(data[start], data[start + 1], data[start + 2], data[start + 3], ByteUtil.BitConversion.LITTLE_ENDIAN);
        } else {
            //aapsLogger.error(LTag.PUMPBTCOMM, "byteToInt, length $length not supported.");
            return 0;
        }
    }


    public static byte[] getBytesFromIntArray2(int value) {
        byte[] array = ByteBuffer.allocate(4).putInt(value).array();
        return new byte[]{array[3], array[2]};
    }

}
