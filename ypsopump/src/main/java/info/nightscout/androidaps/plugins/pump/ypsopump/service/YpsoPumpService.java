package info.nightscout.androidaps.plugins.pump.ypsopump.service;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import dagger.android.DaggerService;

public class YpsoPumpService extends DaggerService {

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean isInitialized() {
        return true;
    }

    public class LocalBinder extends Binder {

        public YpsoPumpService getServiceInstance() {
            return YpsoPumpService.this;
        }
    }
}
