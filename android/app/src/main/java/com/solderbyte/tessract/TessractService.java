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
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

public class TessractService extends Service {

    // Log tag
    private static final String LOG_TAG = "Tessract:Service";

    // Notification
    private int notificationId = 873; // (tessract) sum each decimal value of each character
    private Notification notification = null;

    // States
    private boolean isConnected = false;

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
        this.startBluetoothLeService();
        //this.startNotificationService();

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

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.registerReceiver(shutdownReceiver, new IntentFilter(Config.INTENT_SHUTDOWN));
        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Config.INTENT_BLUETOOTH));

        //this.registerReceiver(notificationReceiver, new IntentFilter(Intents.INTENT_NOTIFICATION));
        //this.registerReceiver(uiReceiver, new IntentFilter(Intents.INTENT_UI));
    }

    private void startBluetoothLeService() {
        Log.d(LOG_TAG, "startBluetoothLeService");

        Intent bluetoothLeServiceIntent = new Intent(this, BluetoothLeService.class);
        this.bindService(bluetoothLeServiceIntent, bluetoothLeServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopBluetoothLeService() {
        Log.d(LOG_TAG, "stopBluetoothLeService");

        try {
            this.unbindService(bluetoothLeServiceConnection);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    private void unregisterReceivers() {
        Log.d(LOG_TAG, "unregisterReceivers");

        try {
            this.unregisterReceiver(shutdownReceiver);
            this.unregisterReceiver(bluetoothLeReceiver);
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

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "bluetoothLeReceiver");
        }
    };

    private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "shutdownReceiver");

            // Unregister Receivers
            TessractService.this.unregisterReceivers();

            // Stop services
            TessractService.this.stopBluetoothLeService();

            // Clear notification
            TessractService.this.clearNotification();

            // Stop self
            TessractService.this.stopForeground(true);
            TessractService.this.stopSelf();
        }
    };
}
