package com.solderbyte.tessract;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Process;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotificationService extends NotificationListenerService {
    // Log tag
    private static final String LOG_TAG = "Tessract:Notification";

    // Applications
    private static ArrayList<String> applicationsAddedList = null;

    // JSON
    public static String jsonPackageName = "packageName";
    public static String jsonApplicationName = "applicationName";
    public static String jsonTitle = "title";
    public static String jsonTicker = "ticker";
    public static String jsonMessage = "message";
    public static String jsonSubMessage = "subMessage";
    public static String jsonSummary = "summary";
    public static String jsonInfo = "info";
    public static String jsonTag = "tag";
    public static String jsonTime = "time";
    public static String jsonId = "id";
    public static String jsonBigText = "bigText";
    public static String jsonText = "text";

    // Notification
    private static String notificationTitle = "android.title";
    private static String notificationText = "android.text";
    private static String notificationSubText = "android.subText";
    private static String notificationSummaryText = "android.summaryText";
    private static String notificationInfoText = "android.infoText";
    private static String notificationReduced = "Reduced content";
    private static String notificationIdTitle = "android:id/title";
    private static String notificationIdBigText = "android:id/big_text";
    private static String notificationIdText= "android:id/text";


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

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        Log.d(LOG_TAG, "onNotificationPosted");

        this.getNotificationData(statusBarNotification);
        this.getNotificationDataFromView(statusBarNotification);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        String packageName = statusBarNotification.getPackageName();
        Log.d(LOG_TAG, "onNotificationRemoved: " + packageName);
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

    private void enableNotificationListenerService() {
        Log.d(LOG_TAG, "enableNotificationListenerService");
        // adb shell dumpsys notification

        // Force start of notification service
        ComponentName thisComponent = new ComponentName(this, NotificationService.class);
        PackageManager packageManager = this.getPackageManager();
        packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    private JSONObject getNotificationData(StatusBarNotification statusBarNotification) {
        Log.d(LOG_TAG, "getNotificationData");
        PackageManager packageManager = this.getPackageManager();
        String packageName = statusBarNotification.getPackageName();
        String applicationName = "";

        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            applicationName = packageManager.getApplicationLabel(applicationInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(LOG_TAG, "Cannot get ApplicationInfo");
        }

        // Notification data
        String title = null;
        String ticker = null;
        String message = null;
        String submessage = null;
        String summary = null;
        String info = null;
        String tag = statusBarNotification.getTag();
        long time = statusBarNotification.getPostTime();
        int id = statusBarNotification.getId();

        // Get ticket text
        try {
            ticker = (String) statusBarNotification.getNotification().tickerText;
        } catch (Exception e) {
            Log.d(LOG_TAG, "Notification does not have tickerText");
        }

        // Get notification API v19
        Notification notification = statusBarNotification.getNotification();
        Bundle notificationExtras = notification.extras;
        int flags = notification.flags;

        //if ((flags & Notification.FLAG_ONGOING_EVENT) != 0) {
        //    return;
        //}

        // Get notification data from extras
        title = notificationExtras.getString(notificationTitle);
        message = notificationExtras.getString(notificationText);
        submessage = notificationExtras.getString(notificationSubText);
        summary = notificationExtras.getString(notificationSummaryText);
        info = notificationExtras.getString(notificationInfoText);

        // Create JSON object
        JSONObject json = null;
        try {
            json = new JSONObject();
            json.put(jsonPackageName, packageName);
            json.put(jsonApplicationName, applicationName);
            json.put(jsonTitle, title);
            json.put(jsonTicker, ticker);
            json.put(jsonMessage, message);
            json.put(jsonSubMessage, submessage);
            json.put(jsonSummary, summary);
            json.put(jsonInfo, info);
            json.put(jsonTag, tag);
            json.put(jsonTime, time);
            json.put(jsonId, id);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: creating JSON " + e);
            e.printStackTrace();
        }

        Log.d(LOG_TAG, "Notification: " + json.toString());
        return json;
    }

    private JSONObject getNotificationDataFromView(StatusBarNotification statusBarNotification) {
        Log.d(LOG_TAG, "getNotificationDataFromView");
        PackageManager packageManager = this.getPackageManager();
        String packageName = statusBarNotification.getPackageName();

        // Get notification API v19
        Notification notification = statusBarNotification.getNotification();

        Resources resources = null;
        try {
            resources = packageManager.getResourcesForApplication(packageName);
        } catch(Exception e){
            Log.e(LOG_TAG, "Failed to getResourcesForApplication: " + e.getMessage());
        }

        if (resources == null) {
            Log.e(LOG_TAG, "No getResourcesForApplication");
            return null;
        }

        int titleId = resources.getIdentifier(notificationIdTitle, null, null);
        int bigTextId = resources.getIdentifier(notificationIdBigText, null, null);
        int textId = resources.getIdentifier(notificationIdText, null, null);

        // Get view
        RemoteViews views = notification.bigContentView;
        if (views == null) {
            views = notification.contentView;
        }
        if (views == null) {
            Log.e(LOG_TAG, "No notification views");
            return null;
        }

        // Get layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
        views.reapply(this.getApplicationContext(), localView);

        // Get Textviews
        TextView textviewTitle = (TextView) localView.findViewById(titleId);
        TextView textviewBigText = (TextView) localView.findViewById(bigTextId);
        TextView textviewText = (TextView) localView.findViewById(textId);

        // Notification data
        String title = null;
        String bigText = null;
        String text = null;

        // Get notification data from views
        if (textviewTitle != null) {
            title = textviewTitle.getText().toString();
        }
        if (textviewBigText != null) {
            bigText = textviewBigText.getText().toString();
        }
        if (textviewText != null) {
            text = textviewText.getText().toString();
        }

        // Create JSON object
        JSONObject json = null;
        try {
            json = new JSONObject();
            json.put(jsonPackageName, packageName);
            json.put(jsonTitle, title);
            json.put(jsonBigText, bigText);
            json.put(jsonText, text);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: creating JSON " + e);
            e.printStackTrace();
        }

        Log.d(LOG_TAG, "Notification: " + json.toString());
        return json;
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.registerReceiver(applicationReceiver, new IntentFilter(Config.INTENT_APPLICATION));
        this.registerReceiver(notificationReceiver, new IntentFilter(Config.INTENT_NOTIFICATION));
        this.registerReceiver(shutdownReceiver, new IntentFilter(Config.INTENT_SHUTDOWN));
    }

    private void sendIntent(String name, String message) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(Config.INTENT_EXTRA_MSG, message);
        this.sendBroadcast(msg);
    }

    private void setApplicationsAdded(String applications) {
        Log.d(LOG_TAG, "setApplicationsSaved: " + applications);
        applicationsAddedList = new ArrayList<>();
        JSONArray json;

        try {
            json = new JSONArray(applications);

            for (int i = 0; i < json.length(); i++) {
                String app = json.get(i).toString();
                applicationsAddedList.add(app);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: creating JSON " + e);
            e.printStackTrace();
        }
    }

    private void unregisterReceivers() {
        Log.d(LOG_TAG, "unregisterReceivers");

        try {
            this.unregisterReceiver(applicationReceiver);
            this.unregisterReceiver(notificationReceiver);
            this.unregisterReceiver(shutdownReceiver);
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

            if (message.equals(Config.INTENT_APPLICATION_SAVED)) {
                String json = intent.getStringExtra(Config.INTENT_EXTRA_DATA);
                NotificationService.this.setApplicationsAdded(json);
            }
        }
    };

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
