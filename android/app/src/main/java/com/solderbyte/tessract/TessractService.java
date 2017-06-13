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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TessractService extends Service {

    // Log tag
    private static final String LOG_TAG = "Tessract:Service";

    // Application
    private static List<ApplicationInfo> installedApplications = null;
    private static ArrayList<String> filteredApplications = null;
    private static JSONArray fApplications = null;

    // Hashmaps
    public static HashMap<Integer, String> SERVICE_FLAGS = new HashMap<Integer, String>() {{
        put(0, "START_STICKY_COMPATIBILITY");
        put(1, "START_FLAG_REDELIVERY, START_STICKY");
        put(2, "START_FLAG_RETRY, START_NOT_STICKY");
        put(3, "START_REDELIVER_INTENT");
        put(15, "START_CONTINUATION_MASK");
    }};

    // Intents
    public static final String INTENT = "com.solderbyte.service";
    public static final String INTENT_SHUTDOWN = INTENT + ".shutdown";
    public static final String INTENT_STARTED = INTENT + ".started";
    public static final String INTENT_EXTRA_MSG = INTENT + ".message";
    public static final String INTENT_EXTRA_DATA = INTENT + ".data";
    public static final String INTENT_APPLICATION = INTENT + ".application";
    public static final String INTENT_APPLICATION_LIST = INTENT_APPLICATION + ".list";
    public static final String INTENT_APPLICATION_LISTED = INTENT_APPLICATION + ".listed";

    public static final String JSON = INTENT + ".json";
    public static final String JSON_APPLICATIONS = "applications";
    public static final String JSON_PACKAGE_NAME = "packageName";
    public static final String JSON_APPLICATION_NAME = "applicationName";
    public static final String JSON_APPLICATION_COLOR = "applicationColor";
    public static final String JSON_DEVICE_NAME = "deviceName";
    public static final String JSON_DEVICE_ADDRESS = "deviceAddress";
    public static final String JSON_DEVICE_TYPE = "deviceType";
    public static final String JSON_DEVICE_BOND = "deviceBond";

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
        Log.d(LOG_TAG, "onStartCommand: flag: " + SERVICE_FLAGS.get(flags) + ", ID: " + startId);

        // Register receivers
        this.registerReceivers();

        // Create notification
        this.createNotification(false);

        // Start services
        this.startServices();

        // Create package manager
        packageManager = this.getPackageManager();

        this.startForeground(notificationId, notification);
        this.sendIntent(INTENT_STARTED, INTENT_STARTED);
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

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    public void createNotification(boolean connected) {
        Log.d(LOG_TAG, "createNotification: " + connected);

        // Intents
        Intent shutdown =  new Intent(INTENT_SHUTDOWN);
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
            Intent dIntent = new Intent(BluetoothLeService.INTENT);
            dIntent.putExtra(INTENT_EXTRA_MSG, BluetoothLeService.INTENT_BLE_DISCONNECT);
            PendingIntent pConnect = PendingIntent.getBroadcast(this, 0, dIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nBuilder.addAction(R.mipmap.ic_launcher, getString(R.string.notification_disconnect), pConnect);
        } else {
            Intent cIntent = new Intent(BluetoothLeService.INTENT);
            cIntent.putExtra(INTENT_EXTRA_MSG, BluetoothLeService.INTENT_BLE_CONNECT);
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
        fApplications =  new JSONArray();
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
                    json.put(JSON_PACKAGE_NAME, app.packageName);
                    json.put(JSON_APPLICATION_NAME, appName);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error: creating JSON " + e);
                    e.printStackTrace();
                }
                filteredApplications.add(json.toString());
                fApplications.put(json.toString());
            }
        }

        Log.d(LOG_TAG, "Installed application: " + filteredApplications.size());
        Log.d(LOG_TAG, "fApplications: " + fApplications.length());
        this.sendIntent(TessractService.INTENT_APPLICATION, TessractService.INTENT_APPLICATION_LISTED, filteredApplications);
    }

    private void onNotificationPosted(String data) {
        Log.d(LOG_TAG, "onNotificationPosted: " + data);
        JSONObject json = null;
        String rgb = "";

        // Get notification
        try {
            json = new JSONObject(data);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: creating JSON " + e);
            e.printStackTrace();
        }

        // Get application settings
        int appColor = DialogColorPicker.COLOR_DEFAULT;
        rgb = TessractProtocol.colorToHex(appColor);

        // Convert data into bytes
        byte[] bytes = TessractProtocol.toProtocol(2, 2, 2, 0, rgb);

        this.sendIntent(BluetoothLeService.INTENT, BluetoothLeService.INTENT_BLE_WRITE, bytes);
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.registerReceiver(applicationReceiver, new IntentFilter(TessractService.INTENT_APPLICATION));
        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(BluetoothLeService.INTENT));
        this.registerReceiver(notificationReceiver, new IntentFilter(NotificationService.INTENT));
        this.registerReceiver(shutdownReceiver, new IntentFilter(TessractService.INTENT_SHUTDOWN));
    }

    public void sendIntent(String name, String message) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(TessractService.INTENT_EXTRA_MSG, message);
        this.sendBroadcast(msg);
    }

    public void sendIntent(String name, String message, byte[] data) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(TessractService.INTENT_EXTRA_MSG, message);
        msg.putExtra(TessractService.INTENT_EXTRA_DATA, data);
        this.sendBroadcast(msg);
    }

    public void sendIntent(String name, String message, String data) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(TessractService.INTENT_EXTRA_MSG, message);
        msg.putExtra(TessractService.INTENT_EXTRA_DATA, data);
        this.sendBroadcast(msg);
    }

    public void sendIntent(String name, String message, ArrayList<String> data) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(TessractService.INTENT_EXTRA_MSG, message);
        msg.putStringArrayListExtra(TessractService.INTENT_EXTRA_DATA, data);
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
            this.unregisterReceiver(bluetoothLeReceiver);
            this.unregisterReceiver(notificationReceiver);
            this.unregisterReceiver(shutdownReceiver);
        } catch (Exception e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

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

    private BroadcastReceiver applicationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(TessractService.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "applicationReceiver: " + message);

            if (message == null) {
                return;
            }
            if (message.equals(TessractService.INTENT_APPLICATION_LIST)) {
                TessractService.this.listApplications();
            }
        }
    };

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "bluetoothLeReceiver: " + message);

            if (message == null) {
                return;
            }
            if (message.equals(BluetoothLeService.INTENT_BLE_CONNECTED)) {
                TessractService.this.createNotification(true);
            }
            if (message.equals(BluetoothLeService.INTENT_BLE_DISCONNECTED)) {
                TessractService.this.createNotification(false);
            }
        }
    };

    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(NotificationService.INTENT_MESSAGE);
            Log.d(LOG_TAG, "notificationReceiver: " + message);

            if (message.equals(NotificationService.INTENT_NOTIFICATION_POSTED)) {
                String data = intent.getStringExtra(NotificationService.INTENT_DATA);
                TessractService.this.onNotificationPosted(data);
            }
        }
    };

    private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "shutdownReceiver");

            // Unregister Receivers
            TessractService.this.unregisterReceivers();

            // Send shutdown intents
            TessractService.this.sendIntent(MainActivity.INTENT_SHUTDOWN, MainActivity.INTENT_SHUTDOWN);

            // Clear notification
            TessractService.this.clearNotification();

            // Stop self
            TessractService.this.stopForeground(true);
            TessractService.this.stopSelf();
        }
    };
}
