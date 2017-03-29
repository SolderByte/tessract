package com.solderbyte.tessract;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Log tag
    private static final String LOG_TAG = "Tessract:Activity";

    // Applications
    private static ArrayList<String> applicationsList = null;
    private static ArrayList<String> applicationsSavedList = null;

    // Buttons
    private static Button buttonConnect = null;
    private static FloatingActionButton fab = null;

    // Device
    private static String deviceName = null;
    private static String deviceAddress = null;
    private static ArrayList<String> combinedList = null;
    private static ArrayList<String> deviceList = null;

    // ListViews
    private static ListView listViewApplications = null;

    // Permission
    private static final int PERMISSION_ACCESS_COARSE_LOCATION = 100;

    // Progress dialogs
    private static ProgressDialog progressScan = null;
    private static ProgressDialog progressConnect = null;
    private static int progressScanIterations = 0;
    private static int progressScanIncrement = 10;
    private static int progressScanPeriod = 700;

    // States
    private static boolean isConnected = false;

    // Store
    private static TessractStore store = null;

    // TextViews
    private static TextView textViewDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Saved preferences
        store = new TessractStore();
        store.init(this.getApplicationContext());

        // UI listeners
        this.createUiListeners();
        this.updateUi();

        // Check notification access
        this.checkNotificationAccess();

        // Check permissions
        this.checkPermissions();

        // Start service
        this.startServices();

        // Register receivers
        this.registerReceivers();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");

        // Unregister receivers
        this.unregisterReceivers();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.d(LOG_TAG, "onBackPressed");
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(LOG_TAG, "onCreateOptionsMenu");

        // Adds items to the toolbar menu
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(LOG_TAG, "onOptionsItemSelected: " + item.toString());
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Log.d(LOG_TAG, "onNavigationItemSelected: " + item.toString());
        int id = item.getItemId();

        if (id == R.id.nav_scan_bt) {
            // Reset devices array
            combinedList = new ArrayList<String>();
            deviceList = new ArrayList<String>();
            combinedList.clear();
            deviceList.clear();

            this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_SCAN);
            this.showProgressScan();
        } else if (id == R.id.nav_help) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG, "User granted permissions");
                } else {
                    Log.d(LOG_TAG, "User denied permissions");
                }
                return;
            }
        }
    }

    private void checkNotificationAccess() {
        Log.d(LOG_TAG, "checkNotificationAccess");
        ContentResolver contentResolver = this.getContentResolver();
        String notificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = this.getPackageName();

        if (notificationListeners == null || !notificationListeners.contains(packageName)){
            Log.d(LOG_TAG, "Notification Access Disabled");
            this.showNotificationAccess();
        } else {
            Log.d(LOG_TAG, "Notification Access Enabled");
        }
    }

    private void checkPermissions() {
        Log.d(LOG_TAG, "checkPermissions");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "Permissions are needed");

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ACCESS_COARSE_LOCATION);
            } else {
                Log.d(LOG_TAG, "Permissions are granted");
            }
        }
    }

    private void createUiListeners() {
        Log.d(LOG_TAG, "createUiListeners");

        // Buttons
        buttonConnect = (Button) findViewById(R.id.button_connect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceAddress != null) {
                    Log.d(LOG_TAG, "buttonConnect: " + deviceName + ":" + deviceAddress);
                    MainActivity.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_CONNECT, deviceAddress);
                } else {
                    Log.d(LOG_TAG, "buttonConnect: No device set");
                }
            }
        });

        // ListViews
        listViewApplications = (ListView) findViewById(R.id.listview_applications);

        // TextViews
        textViewDevice = (TextView) findViewById(R.id.textview_device);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Floating action button
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.sendIntent(Config.INTENT_APPLICATION, Config.INTENT_APPLICATION_LIST);
            }
        });

        // Drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // Navbar
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.registerReceiver(applicationReceiver, new IntentFilter(Config.INTENT_APPLICATION));
        this.registerReceiver(shutdownReceiver, new IntentFilter(Config.INTENT_SHUTDOWN));
        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Config.INTENT_BLUETOOTH));
    }

    public void sendIntent(String name, String message) {
        Intent msg = new Intent(name);
        msg.putExtra(Config.INTENT_EXTRA_MSG, message);
        this.sendBroadcast(msg);
    }

    public void sendIntent(String name, String message, String data) {
        Intent msg = new Intent(name);
        msg.putExtra(Config.INTENT_EXTRA_MSG, message);
        msg.putExtra(Config.INTENT_EXTRA_DATA, data);
        this.sendBroadcast(msg);
    }

    public void sendIntent(String name, String message, ArrayList<String> list) {
        Intent msg = new Intent(name);
        msg.putExtra(Config.INTENT_EXTRA_MSG, message);
        msg.putStringArrayListExtra(Config.INTENT_EXTRA_DATA, list);
        this.sendBroadcast(msg);
    }

    private void setApplication(String application) {
        Log.d(LOG_TAG, "setApplication: " +  application);

        store.setJSONArray(Config.JSON_APPLICATIONS, application);
    }

    private void setApplicationsList(ArrayList<String> applications) {
        Log.d(LOG_TAG, "setApplicationsList: " + applications.toString());
        applicationsList = applications;
        ArrayList<String> applicationNames = new ArrayList<String>();
        ArrayList<Drawable> icons =  new ArrayList<Drawable>();

        for (int i = 0; i < applications.size(); i++) {
            try {
                JSONObject json = new JSONObject(applications.get(i));
                String packageName = json.getString(Config.JSON_PACKAGE_NAME);
                String applicationName = json.getString(Config.JSON_APPLICATION_NAME);
                applicationNames.add(applicationName);

                // Get icon
                PackageManager pm = this.getPackageManager();
                Drawable icon;

                try {
                    icon = pm.getApplicationIcon(packageName);
                } catch(PackageManager.NameNotFoundException e) {
                    icon = null;
                }

                icons.add(icon);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error: creating JSON " + e);
                e.printStackTrace();
            }
        }

        ArrayAdapterWithIcon adapter = new ArrayAdapterWithIcon(this, applicationNames, icons);

        this.showDialogApplications(adapter);
    }

    private void setDevice(String name, String address) {
        Log.d(LOG_TAG, "setDevice: " + name + ":" + address);
        deviceName = name;
        deviceAddress = address;
        store.setString(Config.DEVICE_NAME, name);
        store.setString(Config.DEVICE_ADDRESS, address);

        this.updateDevice();
    }

    private void setDeviceList(String list) {
        Log.d(LOG_TAG, "setDeviceList: " + list);

        try {
            String type;
            JSONObject json = new JSONObject(list);

            if (json.get(Config.JSON_DEVICE_BOND).equals(Config.DEVICE_BONDS.get(10))) {
                // BOND_NONE
                type = this.getString(R.string.dialog_scan_bt_found);
            } else {
                // BOND_BONDED
                type = this.getString(R.string.dialog_scan_bt_paired);
            }

            combinedList.add(type + ": " + json.get(Config.JSON_DEVICE_NAME));
            deviceList.add(list);

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: creating JSON " + e);
            e.printStackTrace();
        }
    }

    private void setDeviceState(boolean value) {
        Log.d(LOG_TAG, "setDeviceState: " + value);

        isConnected = value;
        store.setBoolean(Config.DEVICE_STATE, value);

        this.updateButton();
    }

    private void showNotificationAccess() {
        Log.d(LOG_TAG, "showNotificationAccess");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.dialog_notification_access_title);
        dialog.setMessage(R.string.dialog_notification_access_message);
        dialog.setCancelable(false);

        dialog.setPositiveButton(R.string.button_open, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                Log.d(LOG_TAG, "showNotificationAccess: open");
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        dialog.setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
                Log.d(LOG_TAG, "showNotificationAccess: close");
            }
        });

        AlertDialog alert = dialog.create();
        alert.show();
    }

    private void showDialogApplications(final ArrayAdapterWithIcon adapter) {
        Log.d(LOG_TAG, "showDialogApplications");
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.dialog_applications_title);
        builder.setCancelable(false);

        if (adapter.isEmpty()) {
            Log.d(LOG_TAG, "empty");
        } else {
            builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int index) {
                    Log.d(LOG_TAG, "App choice: " + index);
                }
            });

            builder.setPositiveButton(this.getString(R.string.button_select), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int index) {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    Log.d(LOG_TAG, "App choice: " + adapter.getItem(selectedPosition));
                    String obj = applicationsList.get(selectedPosition);

                    if (obj != null) {
                        try {
                            JSONObject json = new JSONObject(obj);

                            MainActivity.this.setApplication(json.toString());
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Error: creating JSON " + e);
                            e.printStackTrace();
                        }
                    }

                    dialog.dismiss();
                }
            });
        }

        builder.setNegativeButton(this.getString(R.string.button_close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                Log.d(LOG_TAG, "showDialogApplications: close");

                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDialogColorPicker() {
        Log.d(LOG_TAG, "showDialogColorPicker");
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(false);

        builder.setPositiveButton(this.getString(R.string.button_select), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(this.getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setView(R.layout.dialog_color_picker);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDialogScan() {
        Log.d(LOG_TAG, "showDialogScan");
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.dialog_scan_bt_title);
        builder.setCancelable(false);

        if (combinedList.size() == 0) {
            // Devices not found
            builder.setMessage(this.getString(R.string.dialog_scan_bt_none));
        } else {
            // Devices found
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, combinedList);

            builder.setSingleChoiceItems(arrayAdapter, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int index) {
                    Log.d(LOG_TAG, "Scanned choice: " + index);
                }
            });

            builder.setPositiveButton(this.getString(R.string.button_select), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int index) {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String device = deviceList.get(selectedPosition);
                    Log.d(LOG_TAG, "Scan positive: " + selectedPosition + ", selected: " + device.toString());

                    try {
                        JSONObject json = new JSONObject(device);
                        MainActivity.this.setDevice(json.getString(Config.JSON_DEVICE_NAME), json.getString(Config.JSON_DEVICE_ADDRESS));
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error: creating JSON " + e);
                        e.printStackTrace();
                    }

                    dialog.dismiss();
                }
            });
        }

        builder.setNegativeButton(this.getString(R.string.button_close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                Log.d(LOG_TAG, "showDialogScan: close");

                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showProgressConnect() {
        Log.d(LOG_TAG, "showProgressConnecting");

        progressConnect = new ProgressDialog(MainActivity.this);
        progressConnect.setMessage(this.getString(R.string.progressdialog_connect));
        progressConnect.setCancelable(false);
        progressConnect.setButton(DialogInterface.BUTTON_NEUTRAL, this.getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                Log.d(LOG_TAG, "showProgressConnecting: close");

                MainActivity.this.stopProgressConnect();
            }
        });
        progressConnect.show();
    }

    private void showProgressScan() {
        Log.d(LOG_TAG, "showProgressScan");

        progressScan = new ProgressDialog(MainActivity.this);
        progressScan.setMessage(this.getString(R.string.progressdialog_scan));
        progressScan.setCancelable(false);
        progressScan.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressScan.show();

        progressScanIterations = 0;
        this.startProgressScan();
    }

    private void showSnackbar(String text) {
        Log.d(LOG_TAG, "showSnackbar: " + text);

        CoordinatorLayout CoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        Snackbar.make(CoordinatorLayout, text, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
    }

    private void startProgressScan() {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressScanIterations += 1;

                if (progressScanIterations <= progressScanIncrement) {
                    progressScan.incrementProgressBy(progressScanIncrement);
                    Log.d(LOG_TAG, "Progress scan: " + progressScan.getProgress() + "%");
                    MainActivity.this.startProgressScan();
                } else {
                    Log.d(LOG_TAG, "Progress scan completed");
                }
            }
        }, progressScanPeriod);
    }

    private void startServices() {
        Log.d(LOG_TAG, "startServices");

        Intent serviceIntent = new Intent(this, TessractService.class);
        this.startService(serviceIntent);
    }

    private  void stopProgressConnect() {
        Log.d(LOG_TAG, "stopProgressConnect");

        if (progressConnect != null) {
            progressConnect.dismiss();
        }
    }

    private void stopProgressScan() {
        Log.d(LOG_TAG, "stopProgressScan");

        if (progressScan != null) {
            progressScan.dismiss();
        }
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

    private void updateApplications() {
        Log.d(LOG_TAG, "updateApplications");
        String stored = store.getJSONArray(Config.JSON_APPLICATIONS);
        ArrayList<String> applications = new ArrayList<>();
        JSONArray json;

        try {
            json = new JSONArray(stored);

            for (int i = 0; i < json.length(); i++) {
                String obj = json.get(i).toString();
                applications.add(obj);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: creating JSON " + e);
            e.printStackTrace();
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, applications);

        listViewApplications.setAdapter(arrayAdapter);

        listViewApplications.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "Selected: " + position + " " + id);

                MainActivity.this.showDialogColorPicker();
            }
        });

        listViewApplications.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "Long Selected: " + position + " " + id);
                return true;
            }
        });

        Log.d(LOG_TAG, applications.toString());
    }

    private void updateButton() {
        Log.d(LOG_TAG, "updateButton");
        Boolean state = store.getBoolean(Config.DEVICE_STATE);

        this.updateButtonConnect(state);
    }

    public void updateButtonConnect(boolean value) {
        Log.d(LOG_TAG, "updateButtonConnect: " + value);

        if (value) {
            buttonConnect.setText(this.getString(R.string.button_disconnect));
        } else {
            buttonConnect.setText(this.getString(R.string.button_connect));
        }
    }

    private void updateDevice() {
        Log.d(LOG_TAG, "updateDevice");
        String name = store.getString(Config.DEVICE_NAME);
        String address = store.getString(Config.DEVICE_ADDRESS);

        // Set textViewDevice
        if (name.equals(store.STRING_DEFAULT)) {
            String device = this.getString(R.string.textview_state_default);
            this.updateTextViewDevice(device);
        }
        if (!name.equals(store.STRING_DEFAULT)) {
            deviceName = name;
            this.updateTextViewDevice(deviceName);
        }
        if (!address.equals(store.STRING_DEFAULT)) {
            deviceAddress = address;
        }
    }

    private void updateTextViewDevice(String device) {
        Log.d(LOG_TAG, "updateTextViewDevice: " + device);

        if (textViewDevice != null) {
            textViewDevice.setText(this.getString(R.string.textview_device) + ": " + device);
        }
    }

    private void updateUi() {
        Log.d(LOG_TAG, "updateUi");

        this.updateApplications();
        this.updateButton();
        this.updateDevice();
    }

    private BroadcastReceiver applicationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Config.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "applicationReceiver: " + message);

            if (message.equals(Config.INTENT_APPLICATION_LISTED)) {
                ArrayList<String> apps = intent.getStringArrayListExtra(Config.INTENT_EXTRA_DATA);
                MainActivity.this.setApplicationsList(apps);
            }
        }
    };

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Config.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "bluetoothLeReceiver: " + message);

            if (message.equals(Config.INTENT_BLUETOOTH_CONNECTED)) {
                MainActivity.this.setDeviceState(true);
                MainActivity.this.stopProgressConnect();
                MainActivity.this.showSnackbar(MainActivity.this.getString(R.string.snackbar_connected));
            }
            if (message.equals(Config.INTENT_BLUETOOTH_CONNECTING)) {
                MainActivity.this.showProgressConnect();
            }
            if (message.equals(Config.INTENT_BLUETOOTH_DEVICE)) {
                String list = intent.getStringExtra(Config.INTENT_EXTRA_DATA);
                MainActivity.this.setDeviceList(list);
            }
            if (message.equals(Config.INTENT_BLUETOOTH_DISCONNECTED)) {
                MainActivity.this.setDeviceState(false);
                MainActivity.this.stopProgressConnect();
                MainActivity.this.showSnackbar(MainActivity.this.getString(R.string.snackbar_disconnected));
            }
            if (message.equals(Config.INTENT_BLUETOOTH_SCANNED)) {
                MainActivity.this.stopProgressScan();
                MainActivity.this.showDialogScan();
            }
        }
    };

    private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "shutdownReceiver");

            // Unregister Receivers
            MainActivity.this.unregisterReceivers();

            // Stop self
            MainActivity.this.finish();
        }
    };
}
