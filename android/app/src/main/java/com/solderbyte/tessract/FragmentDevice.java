package com.solderbyte.tessract;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class FragmentDevice extends Fragment {

    // Log tag
    private static final String LOG_TAG = "Tessract:FDevice";

    // Buttons
    private static Button buttonConnect = null;
    private static Button buttonScan = null;

    // Color
    private static int deviceColor = 0;

    // Device
    private static ArrayList<String> deviceList = null;
    private static ArrayList<String> scannedList = null;
    private static String deviceName = null;
    private static String deviceAddress = null;
    private static boolean isConnected = false;

    // Fragment Interation Listener
    private OnFragmentInteractionListener fragmentInteractionListener;

    // Image Views
    private static ImageView colorPicker = null;

    // Intents
    public static String INTENT = "com.solderbyte";
    public static String INTENT_EXTRA_MSG = INTENT + ".message";
    public static String INTENT_EXTRA_DATA = INTENT + ".data";

    // Progress dialogs
    private static ProgressDialog progressScan = null;
    private static ProgressDialog progressConnect = null;
    private static int progressScanIterations = 0;
    private static int progressScanIncrement = 10;
    private static int progressScanPeriod = 700;

    // Store
    private static TessractStore store = null;
    private static String keyDeviceName = "device.name";
    private static String keyDeviceAddress = "device.address";
    private static String keyDeviceColor = "device.color";

    // TextViews
    private static TextView textViewDevice = null;

    public FragmentDevice() {}

    public static FragmentDevice newInstance() {
        Log.d(LOG_TAG, "newInstance");

        FragmentDevice fragment = new FragmentDevice();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        Log.d(LOG_TAG, "onAttach");

        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            fragmentInteractionListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");

        // Inflate layout
        View view = inflater.inflate(R.layout.fragment_device, container, false);

        if (fragmentInteractionListener != null) {
            fragmentInteractionListener.onFragmentInteraction(this.getString(R.string.title_device));
        }

        // Saved preferences
        store = new TessractStore();
        store.init(this.getContext());

        // UI listeners
        this.createUiListeners(view);
        this.updateUi();

        // Register receivers
        this.registerReceivers();

        return view;
    }

    @Override
    public void onDetach() {
        Log.d(LOG_TAG, "onDetach");

        // Unregister receivers
        this.unregisterReceivers();

        super.onDetach();
        fragmentInteractionListener = null;
    }

    public void onButtonPressed(String title) {
        Log.d(LOG_TAG, "onButtonPressed");

        if (fragmentInteractionListener != null) {
            fragmentInteractionListener.onFragmentInteraction(title);
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String title);
    }

    public void addDevice(String data) {
        Log.d(LOG_TAG, "addDevice: " + data);

        try {
            String type;
            JSONObject json = new JSONObject(data);

            if (json.get(BluetoothLeService.JSON_DEVICE_BOND).equals(BluetoothLeService.DEVICE_BONDS.get(10))) {
                // BOND_NONE
                type = this.getString(R.string.dialog_scan_found);
            } else {
                // BOND_BONDED
                type = this.getString(R.string.dialog_scan_paired);
            }

            scannedList.add(type + ": " + json.get(BluetoothLeService.JSON_DEVICE_NAME));
            deviceList.add(data);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "addDevice Error: creating JSON " + e);
            e.printStackTrace();
        }
    }

    private void connect() {
        if (deviceAddress != null) {
            Log.d(LOG_TAG, "connect: " + deviceName + ":" + deviceAddress);

            JSONObject json = new JSONObject();
            try {
                json.put(BluetoothLeService.JSON_DEVICE_NAME, deviceName);
                json.put(BluetoothLeService.JSON_DEVICE_ADDRESS, deviceAddress);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "connect Error: creating JSON " + e);
                e.printStackTrace();
                return;
            }

            Log.d(LOG_TAG, json.toString());
            if (isConnected) {
                FragmentDevice.this.sendIntent(BluetoothLeService.INTENT, BluetoothLeService.INTENT_BLE_DISCONNECT, json.toString());
            } else {
                FragmentDevice.this.sendIntent(BluetoothLeService.INTENT, BluetoothLeService.INTENT_BLE_CONNECT, json.toString());
            }
        } else {
            Log.d(LOG_TAG, "connect: No deviceAddress");
        }
    }
    private void createUiListeners(View view) {
        Log.d(LOG_TAG, "createUiListeners");

        // Buttons
        buttonConnect = (Button) view.findViewById(R.id.button_connect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "buttonConnect");

                FragmentDevice.this.connect();
            }
        });

        buttonScan = (Button) view.findViewById(R.id.button_scan);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "buttonScan");

                FragmentDevice.this.scan();
            }
        });

        colorPicker = (ImageView) view.findViewById(R.id.imageview_color);
        colorPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle arguments = new Bundle();
                arguments.putInt(DialogColorPicker.COLOR_KEY, deviceColor);
                DialogColorPicker dialogColorPicker = new DialogColorPicker();
                dialogColorPicker.setArguments(arguments);
                dialogColorPicker.setOnColorSelectedListener(new DialogColorPicker.OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color) {
                        Log.d(LOG_TAG, "Color: " + color);

                        FragmentDevice.this.setColor(color);
                    }
                });
                dialogColorPicker.show(FragmentDevice.this.getFragmentManager(), LOG_TAG);
            }
        });

        // TextViews
        textViewDevice = (TextView) view.findViewById(R.id.textview_device);
    }

    private void scan() {
        Log.d(LOG_TAG, "scan");

        // Reset devices array
        deviceList = new ArrayList<String>();
        scannedList = new ArrayList<String>();
        deviceList.clear();
        scannedList.clear();

        this.sendIntent(BluetoothLeService.INTENT, BluetoothLeService.INTENT_BLE_SCAN);
        this.startProgressScan();
    }

    private void sendIntent(String name, String message) {
        Log.v(LOG_TAG, "sendIntent: " + message);

        String intentMsg = INTENT_EXTRA_MSG;

        if (name.equals(BluetoothLeService.INTENT)) {
            intentMsg = BluetoothLeService.INTENT_EXTRA_MSG;
        }

        Intent msg = new Intent(name);
        msg.putExtra(intentMsg, message);
        this.getContext().sendBroadcast(msg);
    }

    private void sendIntent(String name, String message, String data) {
        Log.v(LOG_TAG, "sendIntent: " + message);

        String intentMsg = INTENT_EXTRA_MSG;
        String intentData = INTENT_EXTRA_DATA;

        if (name.equals(BluetoothLeService.INTENT)) {
            intentMsg = BluetoothLeService.INTENT_EXTRA_MSG;
            intentData = BluetoothLeService.INTENT_EXTRA_DATA;
        }

        Intent msg = new Intent(name);
        msg.putExtra(intentMsg, message);
        msg.putExtra(intentData, data);
        this.getContext().sendBroadcast(msg);
    }

    private void setColor(int color) {
        Log.d(LOG_TAG, "setColor");

        deviceColor = color;
        store.setInt(keyDeviceColor, color);

        this.updateColor();
    }

    private void setDevice(String name, String address) {
        Log.d(LOG_TAG, "setDevice");

        deviceName = name;
        deviceAddress = address;
        store.setString(keyDeviceName, name);
        store.setString(keyDeviceAddress, address);

        this.updateDevice();
    }

    private void showDialogScannedDevices() {
        Log.d(LOG_TAG, "showDialogScannedDevices");

        Bundle arguments = new Bundle();
        arguments.putStringArrayList(DialogScannedDevices.LIST_KEY, scannedList);

        DialogScannedDevices dialogScannedDevices = new DialogScannedDevices();
        dialogScannedDevices.setArguments(arguments);
        dialogScannedDevices.setOnSelectedListener(new DialogScannedDevices.OnSelectedListener() {
            @Override
            public void onSelected(int index) {
                String device = deviceList.get(index);
                Log.d(LOG_TAG, "showDialogScannedDevices selected: " + device);

                try {
                    // Create JSON
                    JSONObject json = new JSONObject(device);
                    FragmentDevice.this.setDevice(json.getString(BluetoothLeService.JSON_DEVICE_NAME), json.getString(BluetoothLeService.JSON_DEVICE_ADDRESS));
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "showDialogScannedDevices Error: creating JSON " + e);
                    e.printStackTrace();
                }
            }
        });
        dialogScannedDevices.show(FragmentDevice.this.getFragmentManager(), LOG_TAG);
    }

    private void startProgressConnect() {
        Log.d(LOG_TAG, "startProgressConnect");

        progressConnect = new ProgressDialog(FragmentDevice.this.getContext());
        progressConnect.setMessage(this.getString(R.string.progressdialog_connect));
        progressConnect.setCancelable(false);
        progressConnect.setButton(DialogInterface.BUTTON_NEUTRAL, this.getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showProgressConnecting: close");

                FragmentDevice.this.stopProgressConnect();
            }
        });
        progressConnect.show();
    }

    private void startProgressScan() {
        Log.d(LOG_TAG, "startProgressScan");

        progressScan = new ProgressDialog(FragmentDevice.this.getContext());
        progressScan.setMessage(this.getString(R.string.progressdialog_scan));
        progressScan.setCancelable(false);
        progressScan.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressScan.show();

        progressScanIterations = 0;
        this.startProgressScanProgress();
    }

    private void startProgressScanProgress() {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressScanIterations += 1;

                if (progressScanIterations <= progressScanIncrement) {
                    progressScan.incrementProgressBy(progressScanIncrement);
                    Log.d(LOG_TAG, "Progress scan: " + progressScan.getProgress() + "%");
                    FragmentDevice.this.startProgressScanProgress();
                } else {
                    Log.d(LOG_TAG, "Progress scan completed");
                }
            }
        }, progressScanPeriod);
    }

    private void stopProgressConnect() {
        Log.d(LOG_TAG, "stopProgressConnect");

        if (progressConnect != null) {
            progressConnect.dismiss();
        }

        // FIXME: sendIntent to stop connecting
    }

    private void stopProgressScan() {
        Log.d(LOG_TAG, "stopProgressScan");

        if (progressScan != null) {
            progressScan.dismiss();
        }
    }

    private void updateColor() {
        Log.d(LOG_TAG, "updateColor");

        int color = store.getInt(keyDeviceColor);

        if (color != store.INT_DEFAULT) {
            deviceColor = color;
        } else {
            deviceColor = DialogColorPicker.COLOR_DEFAULT;
        }

        if (colorPicker != null) {
            colorPicker.setBackgroundColor(deviceColor);
        }
    }

    private void updateDevice() {
        Log.d(LOG_TAG, "updateDevice");

        String name = store.getString(keyDeviceName);
        String address = store.getString(keyDeviceAddress);

        // update UI textview_device
        if (!name.equals(store.STRING_DEFAULT)) {
            deviceName = name;
        } else {
            deviceName = this.getString(R.string.textview_device_default);
        }
        if (!address.equals(store.STRING_DEFAULT)) {
            deviceAddress = address;
        }

        if (textViewDevice != null) {
            textViewDevice.setText(this.getString(R.string.textview_device) + ": " + deviceName);
        }
    }

    private void updateUi() {
        Log.d(LOG_TAG, "updateUi");

        this.updateDevice();
        this.updateColor();
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.getContext().registerReceiver(bluetoothLeReceiver, new IntentFilter(BluetoothLeService.INTENT));
    }

    private void unregisterReceivers() {
        Log.d(LOG_TAG, "unregisterReceivers");

        try {
            this.getContext().unregisterReceiver(bluetoothLeReceiver);
        } catch (Exception e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(BluetoothLeService.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "bluetoothLeReceiver: " + message);

            if (message == null) {
                return;
            }
            if (message.equals(BluetoothLeService.INTENT_BLE_CONNECTING)) {
                FragmentDevice.this.startProgressConnect();
            }
            if (message.equals(BluetoothLeService.INTENT_BLE_CONNECTED)) {
                FragmentDevice.this.stopProgressConnect();
            }
            if (message.equals(BluetoothLeService.INTENT_BLE_DISCONNECTED)) {
                FragmentDevice.this.stopProgressConnect();
            }
            if (message.equals(BluetoothLeService.INTENT_BLE_SCANNED)) {
                FragmentDevice.this.stopProgressScan();
                FragmentDevice.this.showDialogScannedDevices();
            }
            if (message.equals(BluetoothLeService.INTENT_BLE_DEVICE)) {
                String data = intent.getStringExtra(BluetoothLeService.INTENT_EXTRA_DATA);
                FragmentDevice.this.addDevice(data);
            }
        }
    };
}
