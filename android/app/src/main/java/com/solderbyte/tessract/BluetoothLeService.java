package com.solderbyte.tessract;


import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class BluetoothLeService extends Service {

    // Log tag
    private static final String LOG_TAG = "Tessract:BluetoothLe";

    // Bluetooth
    private static BluetoothManager bluetoothManager;
    private static BluetoothAdapter bluetoothAdapter;

    // Hashmaps
    public static HashMap<Integer, String> callbackTypes = new HashMap<Integer, String>() {{
        put(1, "CALLBACK_TYPE_ALL_MATCHES");
        put(2, "CALLBACK_TYPE_FIRST_MATCH");
        put(4, "CALLBACK_TYPE_MATCH_LOST");
    }};
    public static HashMap<Integer, String> deviceBonds = new HashMap<Integer, String>() {{
        put(10, "BOND_NONE");
        put(11, "BOND_BONDING");
        put(12, "BOND_BONDED");
    }};
    public static HashMap<Integer, String> deviceTypes = new HashMap<Integer, String>() {{
        put(0, "DEVICE_TYPE_UNKNOWN");
        put(1, "DEVICE_TYPE_CLASSIC");
        put(2, "DEVICE_TYPE_LE");
        put(3, "DEVICE_TYPE_DUAL");
    }};


    // Scan
    private final static long SCAN_PERIOD = 7000;
    private static Set<BluetoothDevice> pairedDevices;
    private static Set<BluetoothDevice> scannedDevices;

    // States
    private boolean isEnabled = false;
    private boolean isScanning = false;

    // Threads
    private static EnableBluetoothThread enableBluetoothThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand: flag: " + Config.SERVICE_FLAGS.get(flags) + ", ID: " + startId);

        // Initialize sets
        scannedDevices = new LinkedHashSet<BluetoothDevice>();

        // Register receivers
        this.registerReceivers();

        // Setup bluetooth
        this.setupBluetooth();

        return super.onStartCommand(intent, flags, startId);
    }

    public void enableBluetooth() {
        Log.d(LOG_TAG, "enableBluetooth");

        if (!bluetoothAdapter.isEnabled()) {
            enableBluetoothThread = new EnableBluetoothThread();
            enableBluetoothThread.start();
        } else {
            Log.d(LOG_TAG, "BluetoothAdapter is enabled");
        }
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Config.INTENT_BLUETOOTH));
        this.registerReceiver(shutdownReceiver, new IntentFilter(Config.INTENT_SHUTDOWN));
    }

    public void scanBluetooth() {
        Log.d(LOG_TAG, "scanBluetooth: Scanning bluetooth requires permissions");

        if (isEnabled) {
            if (!isScanning) {
                isScanning = true;
                // Start scan
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Log.d(LOG_TAG, "Scanning with startScan: " + SCAN_PERIOD + "ms: Android >= 5.0");

                    BluetoothLeScanner mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                    mBluetoothLeScanner.startScan(bluetoothScanCallback);
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Log.d(LOG_TAG, "Scanning with startLeScan: " + SCAN_PERIOD + "ms: Android < 5.0");

                    bluetoothAdapter.startLeScan(bluetoothLeScanCallback);
                } else {
                    Log.d(LOG_TAG, "Scanning with startDiscovery: " + SCAN_PERIOD + "ms: Android other");
                    // Bluetooth Classic

                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    this.registerReceiver(scanReceiver, filter);

                    bluetoothAdapter.startDiscovery();
                }

                // Stop scan
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "Scanning stopped");
                        isScanning = false;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Log.d(LOG_TAG, "startScan stopped: Android >= 5.0");

                            BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                            bluetoothLeScanner.stopScan(bluetoothScanCallback);
                        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            Log.d(LOG_TAG, "startLeScan stopped: Android < 5.0");

                            bluetoothAdapter.stopLeScan(bluetoothLeScanCallback);
                        } else {
                            Log.d(LOG_TAG, "startDiscovery stopped: Android other");

                            bluetoothAdapter.cancelDiscovery();
                            BluetoothLeService.this.unregisterReceiver(scanReceiver);
                        }

                        BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_SCANNED);
                        // BluetoothLeService.this.setEntries();
                    }
                }, SCAN_PERIOD);

            } else {
                Log.d(LOG_TAG, "Already scanning bluetooth");
            }
        } else {
            Log.d(LOG_TAG, "BluetoothAdapter is not enabled");
        }
    }

    public void setBluetoothEnabled(boolean enabled) {
        Log.d(LOG_TAG, "setBluetoothEnabled");

        isEnabled = enabled;
        if (isEnabled) {
            Log.d(LOG_TAG, "BluetoothAdapter is enabled");

            this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_ENABLED);
        } else {
            Log.d(LOG_TAG, "bluetoothAdapter is not enabled");

            this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_DISABLED);
        }
    }

    public void sendIntent(String name, String message) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(Config.INTENT_EXTRA_MSG, message);
        this.sendBroadcast(msg);
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

    public void setupBluetooth() {
        Log.d(LOG_TAG, "enableBluetooth");

        // Setup bluetooth manager
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) BluetoothLeService.this.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(LOG_TAG, "BluetoothManager failed to getSystemService");
            }
        }

        // Setup bluetooth adapter
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(LOG_TAG, "BluetoothAdapter failed to getAdapter");
        }

        // Check if bluetooth is enabled
        this.setBluetoothEnabled(bluetoothAdapter.isEnabled());
    }

    public void unregisterReceivers() {
        try {
            this.unregisterReceiver(bluetoothLeReceiver);
            this.unregisterReceiver(shutdownReceiver);
        } catch (Exception e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    @SuppressLint("NewApi")
    private ScanCallback bluetoothScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(LOG_TAG, "onScanResult: " + callbackTypes.get(callbackType));

            BluetoothDevice device = result.getDevice();
            if (scannedDevices.add(device)) {
                Log.d(LOG_TAG, device.getName() + " : " + device.getAddress() + " : " + deviceTypes.get(device.getType()) + " : " + deviceBonds.get(device.getBondState()));

                ArrayList<String> list = new ArrayList<String>();
                list.add(device.getName());
                list.add(device.getAddress());
                BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_DEVICE, list);
            }
        }
    };

    private BluetoothAdapter.LeScanCallback bluetoothLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(LOG_TAG, "onLeScan: " + rssi);

            if (scannedDevices.add(device)) {
                Log.d(LOG_TAG, device.getName() + " : " + device.getAddress() + " : " + deviceTypes.get(device.getType()) + " : " + deviceBonds.get(device.getBondState()));

                ArrayList<String> list = new ArrayList<String>();
                list.add(device.getName());
                list.add(device.getAddress());
                BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_DEVICE, list);
            }
        }
    };

    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "scanReceiver");

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (scannedDevices.add(device)) {
                    Log.d(LOG_TAG, device.getName() + " : " + device.getAddress() + " : " + deviceTypes.get(device.getType()) + " : " + deviceBonds.get(device.getBondState()));

                    ArrayList<String> list = new ArrayList<String>();
                    list.add(device.getName());
                    list.add(device.getAddress());
                    BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_DEVICE, list);
                }
            }
        }
    };

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Config.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "bluetoothLeReceiver: " + message);

            if (message.equals(Config.INTENT_BLUETOOTH_SCAN)) {
                BluetoothLeService.this.scanBluetooth();
            }
        }
    };

    private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "shutdownReceiver");

            // Unregister Receivers
            BluetoothLeService.this.unregisterReceivers();

            // Stop self
            BluetoothLeService.this.stopForeground(true);
            BluetoothLeService.this.stopSelf();
        }
    };

    private class EnableBluetoothThread extends Thread {
        public void run() {
            boolean bluetoothEnabled = true;
            long timeStart = Calendar.getInstance().getTimeInMillis();
            Log.d(LOG_TAG, "EnableBluetoothThread: " + timeStart);

            // Wait for bluetooth to be enabled
            bluetoothAdapter.enable();
            while (!bluetoothAdapter.isEnabled()) {
                try {
                    long timeDiff =  Calendar.getInstance().getTimeInMillis() - timeStart;
                    if (timeDiff >= 5000) {
                        bluetoothEnabled = false;
                        break;
                    }
                    Thread.sleep(100L);
                } catch (InterruptedException ie) {
                    // unexpected interruption while enabling bluetooth
                    Thread.currentThread().interrupt(); // restore interrupted flag
                    return;
                }
            }

            BluetoothLeService.this.setBluetoothEnabled(bluetoothEnabled);
        }
    }
}
