package com.solderbyte.tessract;


import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Process;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import java.util.List;

public class NotificationService extends NotificationListenerService {
    // Log tag
    private static final String LOG_TAG = "Tessract:Notification";

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate");

        // Register receivers
        this.registerReceivers();

        // Check notification listener service
        this.checkNotificationListenerService();

        this.sendIntent(Config.INTENT_SERVICE, Config.INTENT_SERVICE_NOTIFICATION_STARTED);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");

        // Unregister receivers
        this.unregisterReceivers();

        super.onDestroy();
    }

    private void checkNotificationListenerService() {
        Log.d(LOG_TAG, "checkNotificationListenerService");
        boolean isNotificationListenerRunning = false;

        ComponentName thisComponent = new ComponentName(this, NotificationService.class);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null) {
            Log.d(LOG_TAG, "Running services is null");
            return;
        }

        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.service.equals(thisComponent)) {
                Log.d(LOG_TAG, "checkNotificationListenerService service - pid: " + service.pid + ", currentPID: " + Process.myPid() + ", clientPackage: " + service.clientPackage + ", clientCount: " + service.clientCount + ", clientLabel: " + ((service.clientLabel == 0) ? "0" : "(" + getResources().getString(service.clientLabel) + ")"));
                if (service.pid == Process.myPid() /*&& service.clientCount > 0 && !TextUtils.isEmpty(service.clientPackage)*/) {
                    isNotificationListenerRunning = true;
                }
            }
        }

        if (isNotificationListenerRunning) {
            Log.d(LOG_TAG, "NotificationListenerService is running");
            return;
        }

        Log.d(LOG_TAG, "NotificationListenerService is not running");
        this.enableNotificationListenerService();
    }

    public void enableNotificationListenerService() {
        Log.d(LOG_TAG, "enableNotificationListenerService");
        // adb shell dumpsys notification

        // Force start of notification service
        ComponentName thisComponent = new ComponentName(this, NotificationService.class);
        PackageManager packageManager = this.getPackageManager();
        packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.registerReceiver(notificationReceiver, new IntentFilter(Config.INTENT_NOTIFICATION));
        this.registerReceiver(shutdownReceiver, new IntentFilter(Config.INTENT_SHUTDOWN));
    }

    public void sendIntent(String name, String message) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(Config.INTENT_EXTRA_MSG, message);
        this.sendBroadcast(msg);
    }

    private void unregisterReceivers() {
        Log.d(LOG_TAG, "unregisterReceivers");

        try {
            this.unregisterReceiver(notificationReceiver);
            this.unregisterReceiver(shutdownReceiver);
        } catch (Exception e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Config.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "notificationReceiver: " + message);

        }
    };

    private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "shutdownReceiver");

            // Unregister Receivers
            NotificationService.this.unregisterReceivers();

            // Stop self
            NotificationService.this.stopSelf();
        }
    };
}
