package com.solderbyte.tessract;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class BluetoothLeService extends Service {

    // Log tag
    private static final String LOG_TAG = "Tessract:BluetoothLe";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand: flag: " + Config.SERVICE_FLAGS.get(flags) + ", ID: " + startId);

        return super.onStartCommand(intent, flags, startId);
    }
}
