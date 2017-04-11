package com.solderbyte.tessract;


import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class BluetoothLeService extends Service {

    // Log tag
    private static final String LOG_TAG = "Tessract:BluetoothLe";

    // Bluetooth
    private static BluetoothManager bluetoothManager;
    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothGatt bluetoothGatt;
    private static BluetoothGattDescriptor bluetoothGattDescriptor;
    private static BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private static String bluetoothAddress;

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
    public static HashMap<Integer, String> gattCharacteristicPermission = new HashMap<Integer, String>() {{
        put(1, "PERMISSION_READ");
        put(2, "PERMISSION_READ_ENCRYPTED");
        put(4, "PERMISSION_READ_ENCRYPTED_MITM");
        put(16, "PERMISSION_WRITE");
        put(32, "PERMISSION_WRITE_ENCRYPTED");
        put(64, "PERMISSION_WRITE_ENCRYPTED_MITM");
        put(128, "PERMISSION_WRITE_SIGNED");
        put(256, "PERMISSION_WRITE_SIGNED_MITM");
    }};
    public static HashMap<Integer, String> gattCharacteristicProperty = new HashMap<Integer, String>() {{
        put(1, "PROPERTY_BROADCAST");
        put(2, "PROPERTY_READ");
        put(4, "PROPERTY_WRITE_NO_RESPONSE");
        put(8, "PROPERTY_WRITE");
        put(16, "PROPERTY_NOTIFY");
        put(32, "PROPERTY_INDICATE");
        put(64, "PROPERTY_SIGNED_WRITE");
        put(128, "PROPERTY_EXTENDED_PROPS");
    }};
    public static HashMap<Integer, String> gattCharacteristicWriteType = new HashMap<Integer, String>() {{
        put(1, "WRITE_TYPE_NO_RESPONSE");
        put(2, "WRITE_TYPE_DEFAULT");
        put(4, "WRITE_TYPE_SIGNED");
    }};
    public static HashMap<Integer, String> gattServiceType = new HashMap<Integer, String>() {{
        put(0, "SERVICE_TYPE_PRIMARY");
        put(1, "SERVICE_TYPE_SECONDARY");
    }};
    public static HashMap<Integer, String> gattState = new HashMap<Integer, String>() {{
        put(0, "STATE_DISCONNECTED");
        put(1, "STATE_CONNECTING");
        put(2, "STATE_CONNECTED");
        put(3, "STATE_DISCONNECTING");
    }};
    public static HashMap<Integer, String> gattStatus = new HashMap<Integer, String>() {{
        put(0, "GATT_SUCCESS");
        put(2, "GATT_READ_NOT_PERMITTED");
        put(3, "GATT_WRITE_NOT_PERMITTED");
        put(5, "GATT_INSUFFICIENT_AUTHENTICATION");
        put(6, "GATT_REQUEST_NOT_SUPPORTED");
        put(7, "GATT_INVALID_OFFSET");
        put(8, "LINK_LOSS");
        put(13, "GATT_INVALID_ATTRIBUTE_LENGTH");
        put(22, "GATT_CONN_TERMINATE_LOCAL_HOST");
        put(15, "GATT_INSUFFICIENT_ENCRYPTION");
        put(133, "GATT_ERROR");
        put(143, "GATT_CONNECTION_CONGESTED");
        put(257, "GATT_FAILURE");
    }};

    // Pin
    private static String pin = "123456";
    private static byte[] bondPin = pin.getBytes();

    // Scan
    private final static long SCAN_PERIOD = 7000;
    private static Set<BluetoothDevice> pairedDevices;
    private static Set<BluetoothDevice> scannedDevices;

    // States
    private boolean isConnected = false;
    private boolean isEnabled = false;
    private boolean isScanning = false;

    // Threads
    private static EnableBluetoothThread enableBluetoothThread;

    // UUID
    private static String bluetoothSuffix = "-0000-1000-8000-00805f9b34fb";
    private static UUID genericAccessUuid = UUID.fromString("00001800" + bluetoothSuffix);
    private static UUID genericAttributeUuid = UUID.fromString("00001801" + bluetoothSuffix);
    private static UUID deviceNameUuid = UUID.fromString("00002A00" + bluetoothSuffix);
    private static UUID tessractService = UUID.fromString("000FFE0" + bluetoothSuffix);
    private static UUID tessractCharacteristic = UUID.fromString("000FFE1" + bluetoothSuffix);
    private static UUID tessractDescriptor = UUID.fromString("0002902" + bluetoothSuffix);

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

        this.sendIntent(Config.INTENT_SERVICE, Config.INTENT_SERVICE_BLUETOOTH_STARTED);
        return super.onStartCommand(intent, flags, startId);
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(LOG_TAG, "onCharacteristicChanged");

            byte[] bytes = characteristic.getValue();
            String value = new String(bytes);

            Log.d(LOG_TAG, "onCharacteristicChanged: " + value);
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(LOG_TAG, "onCharacteristicRead: " + gattStatus.get(status));

            byte[] bytes = characteristic.getValue();
            String value = new String(bytes);

            Log.d(LOG_TAG, "onCharacteristicRead: " + value);
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(LOG_TAG, "onCharacteristicWrite: " + gattStatus.get(status));
        }
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(LOG_TAG, "onConnectionStateChange: " + status + " - " +gattStatus.get(status) + ":" + gattState.get(newState));

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(LOG_TAG, "Connected");
                isConnected = true;

                //BluetoothLeService.this.stopReconnectBle();
                BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_CONNECTED);

                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(LOG_TAG, "Disconnected");
                isConnected = false;

                BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_DISCONNECTED);

                //if(!isReconnecting) {
                //    BluetoothLeService.this.reconnectBle();
                //}
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(LOG_TAG, "onServicesDiscovered: " + gattStatus.get(status));

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Iterate over available GATT Services.
                for (BluetoothGattService gattService : gatt.getServices()) {
                    String uuid = gattService.getUuid().toString();
                    String type = gattServiceType.get(gattService.getType());

                    Log.d(LOG_TAG, "gattService type: " + type);
                    Log.d(LOG_TAG, "gattService uuid: " + uuid);
                    Log.d(LOG_TAG, "should match____: " + tessractService.toString());

                    // Find tessract service and characteristic
                    if (uuid.equals(tessractService.toString())) {
                        Log.d(LOG_TAG, "Service Found");

                        bluetoothGattCharacteristic = gattService.getCharacteristic(tessractCharacteristic);
                        gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);

                        bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(tessractDescriptor);
                        bluetoothGattDescriptor.setValue(bluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(bluetoothGattDescriptor);

                        BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_CHARACTERISTIC);
                    }

                    for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                        gatt.readCharacteristic(gattCharacteristic);
                        String cuuid = gattCharacteristic.getUuid().toString();
                        int instanceId = gattCharacteristic.getInstanceId();
                        int permissions = gattCharacteristic.getPermissions();
                        int properties = gattCharacteristic.getProperties();
                        byte[] value = gattCharacteristic.getValue();
                        int writeType = gattCharacteristic.getWriteType();

                        Log.d(LOG_TAG, "gattCharacteristic Uuid: " + cuuid);
                        Log.d(LOG_TAG, "gattCharacteristic InstanceId: " + instanceId);
                        Log.d(LOG_TAG, "gattCharacteristic Permissions: " + permissions + " - " + gattCharacteristicPermission.get(permissions));
                        Log.d(LOG_TAG, "gattCharacteristic Properties: " + properties + " - " + gattCharacteristicProperty.get(properties));
                        Log.d(LOG_TAG, "gattCharacteristic Value: " + value);
                        Log.d(LOG_TAG, "gattCharacteristic WriteType: " + gattCharacteristicWriteType.get(writeType));
                    }
                }
            } else {
                Log.d(LOG_TAG, "onServicesDiscovered: None");
            }
        }
    };

    public void connectBluetooth(String address) {
        Log.d(LOG_TAG, "connectBluetooth: " + address);

        if (bluetoothAdapter == null || address == null) {
            Log.d(LOG_TAG, "BluetoothAdapter is null");
            return;
        }
        if (address == null) {
            Log.d(LOG_TAG, "Address is null");
            return;
        }

        // Force a connection
        if (bluetoothGatt != null) {
            if (bluetoothGatt.connect()) {
                this.connectBluetoothForce(address);
                return;
            } else {
                Log.d(LOG_TAG, "Could not force connect");
                return;
            }
        }

        // Setup a new connection
        bluetoothAddress = address;
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(bluetoothAddress);
        if (device == null) {
            Log.d(LOG_TAG, "BluetoothDevice is null");
            return;
        }

        // Connect to the device
        Log.d(LOG_TAG, "Connecting...");
        device.setPin(bondPin);
        device.createBond();

        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);

        this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_CONNECTING);
    }

    public void connectBluetoothForce(String address) {
        Log.d(LOG_TAG, "connectBluetoothForce: " + address);
        bluetoothAddress = address;

        if (bluetoothAddress != null) {
            this.disconnectBluetooth();
            this.connectBluetooth(address);
        } else {
            Log.d(LOG_TAG, "bluetoothAddress is null");
        }
    }

    public  void disconnectBluetooth() {
        Log.d(LOG_TAG, "disconnectBluetooth");

        if (bluetoothGatt == null) {
            Log.d(LOG_TAG, "BluetoothGatt is null");
            return;
        }
        bluetoothGatt.disconnect();
        bluetoothGatt.close();
        bluetoothGatt = null;
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

    public void getBluetoothPaired() {
        Log.d(LOG_TAG, "getBluetoothPaired");

        if (isEnabled) {
            pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    Log.d(LOG_TAG, device.getName() + " : " + device.getAddress() + " : " + deviceTypes.get(device.getType()) + " : " + deviceBonds.get(device.getBondState()));

                    JSONObject json = new JSONObject();
                    try {
                        json.put(Config.JSON_DEVICE_NAME, device.getName());
                        json.put(Config.JSON_DEVICE_ADDRESS, device.getAddress());
                        json.put(Config.JSON_DEVICE_TYPE, deviceTypes.get(device.getType()));
                        json.put(Config.JSON_DEVICE_BOND, deviceBonds.get(device.getBondState()));
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error: creating JSON " + e);
                        e.printStackTrace();
                    }

                    BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_DEVICE, json.toString());
                }
            }
            else {
                Log.d(LOG_TAG, "No getBondedDevices");
            }
        } else {
            Log.d(LOG_TAG, "BluetoothAdapter is not enabled");
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

                if (device.getName() == null) {
                    return;
                }

                JSONObject json = new JSONObject();
                try {
                    json.put(Config.JSON_DEVICE_NAME, device.getName());
                    json.put(Config.JSON_DEVICE_ADDRESS, device.getAddress());
                    json.put(Config.JSON_DEVICE_TYPE, deviceTypes.get(device.getType()));
                    json.put(Config.JSON_DEVICE_BOND, deviceBonds.get(device.getBondState()));
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error: creating JSON " + e);
                    e.printStackTrace();
                }

                BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_DEVICE, json.toString());
            }
        }
    };

    private BluetoothAdapter.LeScanCallback bluetoothLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(LOG_TAG, "onLeScan: " + rssi);

            if (scannedDevices.add(device)) {
                Log.d(LOG_TAG, device.getName() + " : " + device.getAddress() + " : " + deviceTypes.get(device.getType()) + " : " + deviceBonds.get(device.getBondState()));

                if (device.getName() == null) {
                    return;
                }

                JSONObject json = new JSONObject();
                try {
                    json.put(Config.JSON_DEVICE_NAME, device.getName());
                    json.put(Config.JSON_DEVICE_ADDRESS, device.getAddress());
                    json.put(Config.JSON_DEVICE_TYPE, deviceTypes.get(device.getType()));
                    json.put(Config.JSON_DEVICE_BOND, deviceBonds.get(device.getBondState()));
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error: creating JSON " + e);
                    e.printStackTrace();
                }

                BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_DEVICE, json.toString());
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

                    if (device.getName() == null) {
                        return;
                    }

                    JSONObject json = new JSONObject();
                    try {
                        json.put(Config.JSON_DEVICE_NAME, device.getName());
                        json.put(Config.JSON_DEVICE_ADDRESS, device.getAddress());
                        json.put(Config.JSON_DEVICE_TYPE, deviceTypes.get(device.getType()));
                        json.put(Config.JSON_DEVICE_BOND, deviceBonds.get(device.getBondState()));
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error: creating JSON " + e);
                        e.printStackTrace();
                    }

                    BluetoothLeService.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_DEVICE, json.toString());
                }
            }
        }
    };

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Config.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "bluetoothLeReceiver: " + message);

            if (message.equals(Config.INTENT_BLUETOOTH_CONNECT)) {
                String address = intent.getStringExtra(Config.INTENT_EXTRA_DATA);
                BluetoothLeService.this.connectBluetooth(address);
            }
            if (message.equals(Config.INTENT_BLUETOOTH_DISCONNECT)) {
                BluetoothLeService.this.disconnectBluetooth();
            }
            if (message.equals(Config.INTENT_BLUETOOTH_SCAN)) {
                BluetoothLeService.this.getBluetoothPaired();
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
