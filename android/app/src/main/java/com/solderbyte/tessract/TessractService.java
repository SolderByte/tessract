package com.solderbyte.tessract;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TessractService extends Service {

    // Log tag
    private static final String LOG_TAG = "Tessract:Service";

    // Application
    private static List<ApplicationInfo> installedApplications = null;
    private static ArrayList<String> filteredApplications = null;

    // Notification
    private static int notificationId = 873; // (tessract) sum each decimal value of each character
    private static Notification notification = null;

    // Package
    private static PackageManager packageManager = null;

    // States
    private static boolean isConnected = false;

    // White list
    private static String elements[] = {
        "com.google.android.apps.walletnfcrel",
        "com.google.android.calendar",
        "com.android.chrome",
        "com.google.android.apps.docs",
        "com.google.android.apps.tachyon",
        "com.google.android.videos",
        "com.google.android.music",
        "com.android.vending",
        "com.google.android.talk",
        "com.google.android.apps.maps",
        "com.android.mms",
        "com.android.dialer",
        "com.google.android.gm",
        "com.google.android.youtube",
        "com.google.android",
        "com.android.deskclock",
        "com.android.email",
        "com.asus.email",
        "com.whatsapp"
    };
    private static Set<String> whiteList = new HashSet(Arrays.asList(elements));

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand: flag: " + Config.SERVICE_FLAGS.get(flags) + ", ID: " + startId);

        // Register receivers
        this.registerReceivers();

        // Create notification
        this.createNotification(false);

        // Start services
        this.startServices();

        // Create package manager
        packageManager = this.getPackageManager();

        this.startForeground(notificationId, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");

        // Unregister receivers
        this.unregisterReceivers();

        super.onDestroy();
    }

    public void clearNotification() {
        Log.d(LOG_TAG, "clearNotification");

        NotificationManager nManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        nManager.cancel(notificationId);
    }

    public void createNotification(boolean connected) {
        Log.d(LOG_TAG, "createNotification: " + connected);

        // Intents
        Intent shutdown =  new Intent(Config.INTENT_SHUTDOWN);
        Intent startActivity = new Intent(this, MainActivity.class);
        PendingIntent startIntent = PendingIntent.getActivity(this, 0, startActivity, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent shutdownIntent = PendingIntent.getBroadcast(this, 0, shutdown, PendingIntent.FLAG_UPDATE_CURRENT);

        // Build notification
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        nBuilder.setSmallIcon(R.mipmap.ic_launcher);
        nBuilder.setContentTitle(getString(R.string.notification_title));

        // Set content text
        if (connected || isConnected) {
            nBuilder.setContentText(getString(R.string.notification_connected));
        } else {
            nBuilder.setContentText(getString(R.string.notification_disconnected));
        }

        // Notification options
        nBuilder.setContentIntent(startIntent);
        nBuilder.setAutoCancel(true);
        nBuilder.setOngoing(true);

        // Notification actions
        nBuilder.addAction(R.mipmap.ic_launcher, getString(R.string.notification_stop), shutdownIntent);
        if (connected) {
            Intent dIntent = new Intent(Config.INTENT_BLUETOOTH);
            dIntent.putExtra(Config.INTENT_EXTRA_MSG, Config.INTENT_BLUETOOTH_DISCONNECT);
            PendingIntent pConnect = PendingIntent.getBroadcast(this, 0, dIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nBuilder.addAction(R.mipmap.ic_launcher, getString(R.string.notification_disconnect), pConnect);
        } else {
            Intent cIntent = new Intent(Config.INTENT_BLUETOOTH);
            cIntent.putExtra(Config.INTENT_EXTRA_MSG, Config.INTENT_BLUETOOTH_CONNECT);
            PendingIntent pConnect = PendingIntent.getBroadcast(this, 0, cIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nBuilder.addAction(R.mipmap.ic_launcher, getString(R.string.notification_connect), pConnect);
        }

        // Sets an ID for the notification
        NotificationManager nManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        notification = nBuilder.build();
        nManager.notify(notificationId, notification);
    }

    private void listApplications() {
        Log.d(LOG_TAG, "listApplications");

        // Get list of installed applications
        filteredApplications = new ArrayList<String>();
        if (installedApplications == null) {
            installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        }

        // Sort the list
        Collections.sort(installedApplications, new ApplicationInfo.DisplayNameComparator(packageManager));

        for (ApplicationInfo app : installedApplications) {
            String appName = (String) packageManager.getApplicationLabel(app);

            // Filter out system apps
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 1 || whiteList.contains(app.packageName)) {

                JSONObject json = new JSONObject();
                try {
                    json.put(Config.JSON_PACKAGE_NAME, app.packageName);
                    json.put(Config.JSON_APPLICATION_NAME, appName);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error: creating JSON " + e);
                    e.printStackTrace();
                }
                filteredApplications.add(json.toString());
            }
        }

        Log.d(LOG_TAG, "Installed application: " + filteredApplications.size());
        this.sendIntent(Config.INTENT_APPLICATION, Config.INTENT_APPLICATION_LISTED, filteredApplications);
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.registerReceiver(applicationReceiver, new IntentFilter(Config.INTENT_APPLICATION));
        this.registerReceiver(shutdownReceiver, new IntentFilter(Config.INTENT_SHUTDOWN));
        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Config.INTENT_BLUETOOTH));

        //this.registerReceiver(notificationReceiver, new IntentFilter(Intents.INTENT_NOTIFICATION));
        //this.registerReceiver(uiReceiver, new IntentFilter(Intents.INTENT_UI));
    }

    public void sendIntent(String name, String message, String data) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(Config.INTENT_EXTRA_MSG, message);
        msg.putExtra(Config.INTENT_EXTRA_DATA, data);
        this.sendBroadcast(msg);
    }

    public void sendIntent(String name, String message, ArrayList<String> data) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(Config.INTENT_EXTRA_MSG, message);
        msg.putStringArrayListExtra(Config.INTENT_EXTRA_DATA, data);
        this.sendBroadcast(msg);
    }

    private void startServices() {
        Log.d(LOG_TAG, "registerReceivers");

        Intent serviceIntent = new Intent(this, BluetoothLeService.class);
        this.startService(serviceIntent);
        Intent notificationIntent = new Intent(this, NotificationService.class);
        this.startService(notificationIntent);
    }

    private void unregisterReceivers() {
        Log.d(LOG_TAG, "unregisterReceivers");

        try {
            this.unregisterReceiver(applicationReceiver);
            this.unregisterReceiver(shutdownReceiver);
            this.unregisterReceiver(bluetoothLeReceiver);
        } catch (Exception e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    private BroadcastReceiver applicationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Config.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "applicationReceiver: " + message);

            if (message.equals(Config.INTENT_APPLICATION_LIST)) {
                TessractService.this.listApplications();
            }
        }
    };

    private ServiceConnection bluetoothLeServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(LOG_TAG, "onServiceConnected: " + name + ", " + service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(LOG_TAG, "onServiceDisconnected: " + name);
        }
    };

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Config.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "bluetoothLeReceiver: " + message);
        }
    };

    private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "shutdownReceiver");

            // Unregister Receivers
            TessractService.this.unregisterReceivers();

            // Clear notification
            TessractService.this.clearNotification();

            // Stop self
            TessractService.this.stopForeground(true);
            TessractService.this.stopSelf();
        }
    };
}
