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

import java.util.Arrays;
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
    private static HashMap<Integer, String> callbackTypes = new HashMap<Integer, String>() {{
        put(1, "CALLBACK_TYPE_ALL_MATCHES");
        put(2, "CALLBACK_TYPE_FIRST_MATCH");
        put(4, "CALLBACK_TYPE_MATCH_LOST");
    }};
    private static HashMap<Integer, String> deviceBonds = new HashMap<Integer, String>() {{
        put(10, "BOND_NONE");
        put(11, "BOND_BONDING");
        put(12, "BOND_BONDED");
    }};
    private static HashMap<Integer, String> deviceTypes = new HashMap<Integer, String>() {{
        put(0, "DEVICE_TYPE_UNKNOWN");
        put(1, "DEVICE_TYPE_CLASSIC");
        put(2, "DEVICE_TYPE_LE");
        put(3, "DEVICE_TYPE_DUAL");
    }};
    private static HashMap<Integer, String> gattCharacteristicPermission = new HashMap<Integer, String>() {{
        put(1, "PERMISSION_READ");
        put(2, "PERMISSION_READ_ENCRYPTED");
        put(4, "PERMISSION_READ_ENCRYPTED_MITM");
        put(16, "PERMISSION_WRITE");
        put(32, "PERMISSION_WRITE_ENCRYPTED");
        put(64, "PERMISSION_WRITE_ENCRYPTED_MITM");
        put(128, "PERMISSION_WRITE_SIGNED");
        put(256, "PERMISSION_WRITE_SIGNED_MITM");
    }};
    private static HashMap<Integer, String> gattCharacteristicProperty = new HashMap<Integer, String>() {{
        put(1, "PROPERTY_BROADCAST");
        put(2, "PROPERTY_READ");
        put(4, "PROPERTY_WRITE_NO_RESPONSE");
        put(8, "PROPERTY_WRITE");
        put(16, "PROPERTY_NOTIFY");
        put(32, "PROPERTY_INDICATE");
        put(64, "PROPERTY_SIGNED_WRITE");
        put(128, "PROPERTY_EXTENDED_PROPS");
    }};
    private static HashMap<Integer, String> gattCharacteristicWriteType = new HashMap<Integer, String>() {{
        put(1, "WRITE_TYPE_NO_RESPONSE");
        put(2, "WRITE_TYPE_DEFAULT");
        put(4, "WRITE_TYPE_SIGNED");
    }};
    private static HashMap<Integer, String> gattServiceType = new HashMap<Integer, String>() {{
        put(0, "SERVICE_TYPE_PRIMARY");
        put(1, "SERVICE_TYPE_SECONDARY");
    }};
    private static HashMap<Integer, String> gattState = new HashMap<Integer, String>() {{
        put(0, "STATE_DISCONNECTED");
        put(1, "STATE_CONNECTING");
        put(2, "STATE_CONNECTED");
        put(3, "STATE_DISCONNECTING");
    }};
    private static HashMap<Integer, String> gattStatus = new HashMap<Integer, String>() {{
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
    public static HashMap<Integer, String> DEVICE_BONDS = new HashMap<Integer, String>() {{
        put(10, "BOND_NONE");
        put(11, "BOND_BONDING");
        put(12, "BOND_BONDED");
    }};
    public static HashMap<Integer, String> SERVICE_FLAGS = new HashMap<Integer, String>() {{
        put(0, "START_STICKY_COMPATIBILITY");
        put(1, "START_FLAG_REDELIVERY, START_STICKY");
        put(2, "START_FLAG_RETRY, START_NOT_STICKY");
        put(3, "START_REDELIVER_INTENT");
        put(15, "START_CONTINUATION_MASK");
    }};

    // Intents
    public static String INTENT = "com.solderbyte.bluetooth";
    public static String INTENT_SHUTDOWN = INTENT + ".shutdown";
    public static String INTENT_STARTED = INTENT + ".started";
    public static String INTENT_EXTRA_MSG = INTENT + ".message";
    public static String INTENT_EXTRA_DATA = INTENT + ".data";
    public static String INTENT_BLE = INTENT + ".ble";
    public static String INTENT_BLE_SCAN = INTENT_BLE + ".scan";
    public static String INTENT_BLE_SCANNED = INTENT_BLE + ".scanned";
    public static String INTENT_BLE_DEVICE = INTENT_BLE + ".device";
    public static String INTENT_BLE_CONNECT = INTENT_BLE + ".connect";
    public static String INTENT_BLE_CONNECTED = INTENT_BLE + ".connected";
    public static String INTENT_BLE_CONNECTING = INTENT_BLE + ".connecting";
    public static String INTENT_BLE_DISCONNECT = INTENT_BLE + ".disconnect";
    public static String INTENT_BLE_DISCONNECTED = INTENT_BLE + ".disconnected";
    public static String INTENT_BLE_CHARACTERISTIC = INTENT_BLE + ".characteristic";
    public static String INTENT_BLE_ENABLED = INTENT_BLE + ".enabled";
    public static String INTENT_BLE_DISABLED = INTENT_BLE + ".disabled";
    public static String INTENT_BLE_STATUS = INTENT_BLE + ".status";
    public static String INTENT_BLE_WRITE = INTENT_BLE + ".write";

    // Pin
    private static String pin = "123456";
    private static byte[] bondPin = pin.getBytes();

    // Scan
    private final static long scanPeriod = 7000;
    private static Set<BluetoothDevice> pairedDevices;
    private static Set<BluetoothDevice> scannedDevices;

    // States
    private static boolean isConnected = false;
    private static boolean isEnabled = false;
    private static boolean isScanning = false;
    private static boolean isSending = false;

    // Strings
    public static final String JSON_DEVICE_NAME = "deviceName";
    public static final String JSON_DEVICE_ADDRESS = "deviceAddress";
    public static final String JSON_DEVICE_TYPE = "deviceType";
    public static final String JSON_DEVICE_BOND = "deviceBond";

    // Transport
    private static int bluetoothMtu = 20;
    private static byte[] bytesTxBuffer = null;

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
        Log.d(LOG_TAG, "onStartCommand: flag: " + SERVICE_FLAGS.get(flags) + ", ID: " + startId);

        // Initialize sets
        scannedDevices = new LinkedHashSet<BluetoothDevice>();

        // Register receivers
        this.registerReceivers();

        // Setup bluetooth
        this.setupBluetooth();

        this.sendIntent(INTENT, INTENT_STARTED);
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
                BluetoothLeService.this.sendIntent(INTENT, INTENT_BLE_CONNECTED);

                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(LOG_TAG, "Disconnected");
                isConnected = false;

                BluetoothLeService.this.sendIntent(INTENT, INTENT_BLE_DISCONNECTED);

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

                        BluetoothLeService.this.sendIntent(INTENT, INTENT_BLE_CHARACTERISTIC);
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

    public void connectBluetoothLe(String data) {
        Log.d(LOG_TAG, "connectBluetoothLe: " + data);
        String address = null;

        try {
            // Create JSON
            JSONObject json = new JSONObject(data);
            address = json.getString(JSON_DEVICE_ADDRESS);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "connectBluetoothLe Error: creating JSON " + e);
            e.printStackTrace();
        }

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
                this.connectBluetoothLeForce(data);
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

        this.sendIntent(INTENT, INTENT_BLE_CONNECTING);
    }

    public void connectBluetoothLeForce(String data) {
        Log.d(LOG_TAG, "connectBluetoothLeForce: " + data);
        String address = null;

        try {
            // Create JSON
            JSONObject json = new JSONObject(data);
            address = json.getString(JSON_DEVICE_ADDRESS);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "connectBluetoothLe Error: creating JSON " + e);
            e.printStackTrace();
        }
        bluetoothAddress = address;

        if (bluetoothAddress != null) {
            this.disconnectBluetoothLe();
            this.connectBluetoothLe(data);
        } else {
            Log.d(LOG_TAG, "bluetoothAddress is null");
        }
    }

    public  void disconnectBluetoothLe() {
        Log.d(LOG_TAG, "disconnectBluetoothLe");

        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        } else {
            Log.d(LOG_TAG, "BluetoothGatt is null");
        }

        this.sendIntent(INTENT, INTENT_BLE_DISCONNECTED);
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
                        json.put(BluetoothLeService.JSON_DEVICE_NAME, device.getName());
                        json.put(BluetoothLeService.JSON_DEVICE_ADDRESS, device.getAddress());
                        json.put(BluetoothLeService.JSON_DEVICE_TYPE, deviceTypes.get(device.getType()));
                        json.put(BluetoothLeService.JSON_DEVICE_BOND, deviceBonds.get(device.getBondState()));
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error: creating JSON " + e);
                        e.printStackTrace();
                    }

                    BluetoothLeService.this.sendIntent(BluetoothLeService.INTENT, BluetoothLeService.INTENT_BLE_DEVICE, json.toString());
                }
            }
            else {
                Log.d(LOG_TAG, "No getBondedDevices");
            }
        } else {
            Log.d(LOG_TAG, "BluetoothAdapter is not enabled");
        }
    }

    public void getBluetoothStatus() {
        Log.d(LOG_TAG, "getBluetoothStatus");

        if (isConnected) {
            this.sendIntent(INTENT, INTENT_BLE_CONNECTED);
        } else {
            this.sendIntent(INTENT, INTENT_BLE_DISCONNECTED);
        }
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(INTENT));
        this.registerReceiver(shutdownReceiver, new IntentFilter(INTENT_SHUTDOWN));
    }

    public void scanBluetooth() {
        Log.d(LOG_TAG, "scanBluetooth: Scanning bluetooth requires permissions");

        if (isEnabled) {
            if (!isScanning) {
                isScanning = true;
                // Start scan
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Log.d(LOG_TAG, "Scanning with startScan: " + scanPeriod + "ms: Android >= 5.0");

                    BluetoothLeScanner mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                    mBluetoothLeScanner.startScan(bluetoothScanCallback);
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Log.d(LOG_TAG, "Scanning with startLeScan: " + scanPeriod + "ms: Android < 5.0");

                    bluetoothAdapter.startLeScan(bluetoothLeScanCallback);
                } else {
                    Log.d(LOG_TAG, "Scanning with startDiscovery: " + scanPeriod + "ms: Android other");
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

                        BluetoothLeService.this.sendIntent(BluetoothLeService.INTENT, BluetoothLeService.INTENT_BLE_SCANNED);
                    }
                }, scanPeriod);

            } else {
                Log.d(LOG_TAG, "Already scanning bluetooth");
            }
        } else {
            Log.d(LOG_TAG, "BluetoothAdapter is not enabled");
        }
    }

    public void sendIntent(String name, String message) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(INTENT_EXTRA_MSG, message);
        this.sendBroadcast(msg);
    }

    public void sendIntent(String name, String message, String data) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(INTENT_EXTRA_MSG, message);
        msg.putExtra(INTENT_EXTRA_DATA, data);
        this.sendBroadcast(msg);
    }

    public void setBluetoothEnabled(boolean enabled) {
        Log.d(LOG_TAG, "setBluetoothEnabled");

        isEnabled = enabled;
        if (isEnabled) {
            Log.d(LOG_TAG, "BluetoothAdapter is enabled");

            this.sendIntent(INTENT, INTENT_BLE_ENABLED);
        } else {
            Log.d(LOG_TAG, "bluetoothAdapter is not enabled");

            this.sendIntent(INTENT, INTENT_BLE_DISABLED);
        }
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
        Log.d(LOG_TAG, "unregisterReceivers");

        try {
            this.unregisterReceiver(bluetoothLeReceiver);
            this.unregisterReceiver(shutdownReceiver);
        } catch (Exception e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    public void writeBluetoothLe(byte[] bytes) {
        Log.d(LOG_TAG, "writeBluetoothLe");

        if (bluetoothGattCharacteristic != null) {
            // Chunk data
            if (bytes.length > bluetoothMtu) {
                byte[] bytesToSend = Arrays.copyOfRange(bytes, 0, bluetoothMtu);
                bytesTxBuffer = Arrays.copyOfRange(bytes, bluetoothMtu, bytes.length);
                isSending = true;
                bytes = bytesToSend;
            }

            // Set data
            if (bluetoothGattCharacteristic.setValue(bytes)) {
                this.writeBluetoothCharacteristic(bluetoothGattCharacteristic);
                //this.setCharacteristicNotification(bluetoothGattCharacteristic, true);
            } else {
                Log.d(LOG_TAG, "BluetoothGattCharacteristic could not be set");
            }
        } else {
            Log.d(LOG_TAG, "BluetoothGattCharacteristic is null");
        }
    }

    public void writeBluetoothCharacteristic(BluetoothGattCharacteristic characteristic) {
        Log.d(LOG_TAG, "writeBluetoothCharacteristic");

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.d(LOG_TAG, "BluetoothAdapter is null or BluetoothGatt is null");
            return;
        }

        bluetoothGatt.writeCharacteristic(characteristic);
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
                    json.put(BluetoothLeService.JSON_DEVICE_NAME, device.getName());
                    json.put(BluetoothLeService.JSON_DEVICE_ADDRESS, device.getAddress());
                    json.put(BluetoothLeService.JSON_DEVICE_TYPE, deviceTypes.get(device.getType()));
                    json.put(BluetoothLeService.JSON_DEVICE_BOND, deviceBonds.get(device.getBondState()));
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error: creating JSON " + e);
                    e.printStackTrace();
                }

                BluetoothLeService.this.sendIntent(BluetoothLeService.INTENT, BluetoothLeService.INTENT_BLE_DEVICE, json.toString());
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
                    json.put(BluetoothLeService.JSON_DEVICE_NAME, device.getName());
                    json.put(BluetoothLeService.JSON_DEVICE_ADDRESS, device.getAddress());
                    json.put(BluetoothLeService.JSON_DEVICE_TYPE, deviceTypes.get(device.getType()));
                    json.put(BluetoothLeService.JSON_DEVICE_BOND, deviceBonds.get(device.getBondState()));
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error: creating JSON " + e);
                    e.printStackTrace();
                }

                BluetoothLeService.this.sendIntent(BluetoothLeService.INTENT, BluetoothLeService.INTENT_BLE_DEVICE, json.toString());
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
                        json.put(BluetoothLeService.JSON_DEVICE_NAME, device.getName());
                        json.put(BluetoothLeService.JSON_DEVICE_ADDRESS, device.getAddress());
                        json.put(BluetoothLeService.JSON_DEVICE_TYPE, deviceTypes.get(device.getType()));
                        json.put(BluetoothLeService.JSON_DEVICE_BOND, deviceBonds.get(device.getBondState()));
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error: creating JSON " + e);
                        e.printStackTrace();
                    }

                    BluetoothLeService.this.sendIntent(BluetoothLeService.INTENT, BluetoothLeService.INTENT_BLE_DEVICE, json.toString());
                }
            }
        }
    };

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(BluetoothLeService.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "bluetoothLeReceiver: " + message);

            if (message == null) {
                return;
            }
            if (message.equals(BluetoothLeService.INTENT_BLE_SCAN)) {
                BluetoothLeService.this.getBluetoothPaired();
                BluetoothLeService.this.scanBluetooth();
            }
            if (message.equals(INTENT_BLE_CONNECT)) {
                String data = intent.getStringExtra(INTENT_EXTRA_DATA);
                BluetoothLeService.this.connectBluetoothLe(data);
            }
            if (message.equals(INTENT_BLE_DISCONNECT)) {
                BluetoothLeService.this.disconnectBluetoothLe();
            }
            if (message.equals(INTENT_BLE_STATUS)) {
                BluetoothLeService.this.getBluetoothStatus();
            }
            if (message.equals(INTENT_BLE_WRITE)) {
                byte[] data = intent.getByteArrayExtra(INTENT_EXTRA_DATA);
                BluetoothLeService.this.writeBluetoothLe(data);
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
