package com.solderbyte.tessract;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
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
    private static ArrayList<String> applicationsAddedList = null;

    // Buttons
    private static Button buttonConnect = null;
    private static FloatingActionButton fab = null;

    // Color picker
    private static ArrayList<Integer> colors = null;
    private static ArrayList<SeekBar> seekBars = null;
    private static ArrayList<EditText> editTexts = null;

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
    private static ProgressDialog progressApplications = null;
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
        } else if (id == R.id.nav_device) {
            Intent intent = new Intent(this, ActivityDevice.class);
            ActivityOptions options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out);
            this.startActivity(intent, options.toBundle());
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
            this.showDialogNotificationAccess();
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
        buttonConnect = (Button) this.findViewById(R.id.button_connect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceAddress != null) {
                    Log.d(LOG_TAG, "buttonConnect: " + deviceName + ":" + deviceAddress);

                    if (isConnected) {
                        MainActivity.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_DISCONNECT, deviceAddress);
                    } else {
                        MainActivity.this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_CONNECT, deviceAddress);
                    }
                } else {
                    Log.d(LOG_TAG, "buttonConnect: No device set");
                }
            }
        });

        // ListViews
        listViewApplications = (ListView) this.findViewById(R.id.listview_applications);

        // TextViews
        textViewDevice = (TextView) this.findViewById(R.id.textview_device);

        // Toolbar
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Floating action button
        fab = (FloatingActionButton) this.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.showProgressApplications();
                MainActivity.this.sendIntent(Config.INTENT_APPLICATION, Config.INTENT_APPLICATION_LIST);
            }
        });

        // Drawer
        DrawerLayout drawer = (DrawerLayout) this.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // Navbar
        NavigationView navigationView = (NavigationView) this.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void deleteApplicationByIndex(int index) {
        Log.d(LOG_TAG, "deleteApplication: " + index);

        store.deleteJSONArray(Config.JSON_APPLICATIONS, index);
        this.updateUi();
    }

    private JSONObject getApplicationByIndex(int index) {
        Log.d(LOG_TAG, "getApplicationByIndex");
        JSONArray json = this.getApplications();
        JSONObject app = new JSONObject();

        try {
            if (index > json.length()) {
                return null;
            }

            app = new JSONObject(json.get(index).toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: getting JSON by index" + e);
            e.printStackTrace();
        }


        return app;
    }

    private Drawable getApplicationIcon(String packageName) {
        Log.v(LOG_TAG, "getApplicationIcon: " + packageName);
        // Get icon
        PackageManager pm = this.getPackageManager();
        Drawable icon;

        try {
            icon = pm.getApplicationIcon(packageName);
        } catch(PackageManager.NameNotFoundException e) {
            icon = null;
        }

        return icon;
    }

    private JSONArray getApplications() {
        Log.d(LOG_TAG, "getApplications");
        String stored = store.getJSONArray(Config.JSON_APPLICATIONS);
        JSONArray json = new JSONArray();

        try {
            json = new JSONArray(stored);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: creating JSON " + e);
            e.printStackTrace();
        }

        return json;
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.registerReceiver(applicationReceiver, new IntentFilter(Config.INTENT_APPLICATION));
        this.registerReceiver(shutdownReceiver, new IntentFilter(Config.INTENT_SHUTDOWN));
        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Config.INTENT_BLUETOOTH));
        this.registerReceiver(serviceReceiver, new IntentFilter(Config.INTENT_SERVICE));
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

    private void setApplications(String applications) {
        Log.d(LOG_TAG, "setApplications: " +  applications);

        store.setJSONArray(Config.JSON_APPLICATIONS, applications);
        this.updateUi();
    }

    private void setApplicationsByIndex(String applications, int index) {
        Log.d(LOG_TAG, "setApplicationsByIndex: " +  applications + " " + index);

        store.updateJSONArray(Config.JSON_APPLICATIONS, applications, index);
        this.updateUi();
    }

    private void setApplicationsList(ArrayList<String> applications) {
        Log.d(LOG_TAG, "setApplicationsList: " + applications.toString());
        applicationsList = applications;
        ArrayList<String> applicationNames = new ArrayList<String>();
        ArrayList<Drawable> icons = new ArrayList<Drawable>();

        for (int i = 0; i < applicationsList.size(); i++) {
            try {
                // Get application and package names
                JSONObject json = new JSONObject(applicationsList.get(i));
                String packageName = json.getString(Config.JSON_PACKAGE_NAME);
                String applicationName = json.getString(Config.JSON_APPLICATION_NAME);
                applicationNames.add(applicationName);

                // Get icon
                Drawable icon = this.getApplicationIcon(packageName);
                icons.add(icon);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error: creating JSON " + e);
                e.printStackTrace();
            }
        }

        // Create an adapter with icons
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

    private void showDialogApplications(final ArrayAdapterWithIcon adapter) {
        Log.d(LOG_TAG, "showDialogApplications");
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.dialog_applications_title);
        builder.setCancelable(false);

        if (adapter.isEmpty()) {
            Log.d(LOG_TAG, "showDialogApplications empty adapter");
        } else {
            // Set the adapter
            builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });

            builder.setPositiveButton(this.getString(R.string.button_select), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Get the position of selected app
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    Log.d(LOG_TAG, "showDialogApplications: " + adapter.getItem(selectedPosition));
                    String app = applicationsList.get(selectedPosition);

                    if (app != null) {
                        try {
                            // Create JSON and set default color for app
                            JSONObject json = new JSONObject(app);
                            json.put(Config.JSON_APPLICATION_COLOR, Config.COLOR_DEFAULT);

                            MainActivity.this.setApplications(json.toString());
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
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showDialogApplications: close");

                dialog.dismiss();
            }
        });

        // Dismiss progress dialog
        this.stopProgressApplications();

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDialogColorPicker(final int index) {
        Log.d(LOG_TAG, "showDialogColorPicker");
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_color_picker, (ViewGroup) findViewById(R.id.colorpicker_layout));
        int color = Config.COLOR_DEFAULT;

        // Get application
        JSONObject app = this.getApplicationByIndex(index);

        try {
            color = app.getInt(Config.JSON_APPLICATION_COLOR);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: getting JSON " + e);
            e.printStackTrace();
        }

        // Set color
        final int red = Color.red(color);
        final int green = Color.green(color);
        final int blue = Color.blue(color);
        colors = new ArrayList<Integer>() {{
            add(red);
            add(green);
            add(blue);
        }};
        seekBars = new ArrayList<SeekBar>();
        editTexts = new ArrayList<EditText>();
        View viewColor = null;

        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(false);

        builder.setPositiveButton(this.getString(R.string.button_select), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showDialogColorPicker: " + colors.get(Config.COLOR_RED) + " " + colors.get(Config.COLOR_GREEN) + " " + colors.get(Config.COLOR_BLUE));

                int color = Color.rgb(colors.get(Config.COLOR_RED), colors.get(Config.COLOR_GREEN), colors.get(Config.COLOR_BLUE));

                ArrayList<String> applications = new ArrayList<>();
                JSONObject app = MainActivity.this.getApplicationByIndex(index);

                try {
                    app.put(Config.JSON_APPLICATION_COLOR, color);

                    MainActivity.this.setApplicationsByIndex(app.toString(), index);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error: creating JSON " + e);
                    e.printStackTrace();
                }

                dialog.dismiss();
            }
        });

        builder.setNegativeButton(this.getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showDialogColorPicker: cancel");

                dialog.dismiss();
            }
        });

        // Create dialog
        builder.setView(layout);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Get layout elements
        viewColor = layout.findViewById(R.id.view_color);
        SeekBar seekBarRed = (SeekBar) layout.findViewById(R.id.seekbar_red);
        SeekBar seekBarGreen = (SeekBar) layout.findViewById(R.id.seekbar_green);
        SeekBar seekBarBlue = (SeekBar) layout.findViewById(R.id.seekbar_blue);
        EditText editTextRed = (EditText) layout.findViewById(R.id.edittext_red);
        EditText editTextGreen = (EditText) layout.findViewById(R.id.edittext_green);
        EditText editTextBlue = (EditText) layout.findViewById(R.id.edittext_blue);
        EditText editTextHex = (EditText) layout.findViewById(R.id.edittext_hex);

        seekBars.add(seekBarRed);
        seekBars.add(seekBarGreen);
        seekBars.add(seekBarBlue);
        editTexts.add(editTextRed);
        editTexts.add(editTextGreen);
        editTexts.add(editTextBlue);

        // Restore previous color
        this.updateColorPickerView(viewColor);
        this.updateColorPickerHex(editTextHex);
        this.updateColorPickerSeekbar(seekBars.get(Config.COLOR_RED), red);
        this.updateColorPickerSeekbar(seekBars.get(Config.COLOR_GREEN), green);
        this.updateColorPickerSeekbar(seekBars.get(Config.COLOR_BLUE), blue);
        this.updateColorPickerEditText(editTexts.get(Config.COLOR_RED), red);
        this.updateColorPickerEditText(editTexts.get(Config.COLOR_GREEN), green);
        this.updateColorPickerEditText(editTexts.get(Config.COLOR_BLUE), blue);

        // Set color picker listener
        this.updateColorPicker(viewColor, seekBarRed, editTextRed, editTextHex, Config.COLOR_RED);
        this.updateColorPicker(viewColor, seekBarGreen,editTextGreen, editTextHex, Config.COLOR_GREEN);
        this.updateColorPicker(viewColor, seekBarBlue, editTextBlue, editTextHex, Config.COLOR_BLUE);;
    }

    private void showDialogNotificationAccess() {
        Log.d(LOG_TAG, "showDialogNotificationAccess");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.dialog_notification_access_title);
        dialog.setMessage(R.string.dialog_notification_access_message);
        dialog.setCancelable(false);

        dialog.setPositiveButton(R.string.button_open, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showDialogNotificationAccess: open");
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        dialog.setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showDialogNotificationAccess: close");
            }
        });

        AlertDialog alert = dialog.create();
        alert.show();
    }

    private void showDialogRemoveApplication(final int index) {
        Log.d(LOG_TAG, "showDialogRemoveApplication: " + index);
        ArrayList<String> applicationNames = new ArrayList<String>();
        ArrayList<Drawable> icons = new ArrayList<Drawable>();
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.dialog_applications_remove_title);
        builder.setCancelable(false);

        // Get application
        JSONObject app = this.getApplicationByIndex(index);

        try {
            String applicationName = app.getString(Config.JSON_APPLICATION_NAME);
            String packageName = app.getString(Config.JSON_PACKAGE_NAME);

            Drawable icon = this.getApplicationIcon(packageName);

            applicationNames.add(applicationName);
            icons.add(icon);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: getting JSON " + e);
            e.printStackTrace();
        }

        // Create an adapter with icons
        ArrayAdapterWithIcon adapter = new ArrayAdapterWithIcon(this, applicationNames, icons);

        builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        builder.setPositiveButton(R.string.button_remove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showDialogRemoveApplication: remove");

                MainActivity.this.deleteApplicationByIndex(index);
            }
        });
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showDialogRemoveApplication: cancel");
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
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
                public void onClick(DialogInterface dialog, int which) {}
            });

            builder.setPositiveButton(this.getString(R.string.button_select), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Get the position of selected device
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String device = deviceList.get(selectedPosition);
                    Log.d(LOG_TAG, "showDialogScan: " + device.toString());

                    try {
                        // Create JSON
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
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showDialogScan: close");

                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showProgressApplications() {
        Log.d(LOG_TAG, "showProgressApplications");

        progressApplications = new ProgressDialog(MainActivity.this);
        progressApplications.setMessage(this.getString(R.string.progressdialog_applications));
        progressApplications.setCancelable(false);

        progressApplications.show();
    }

    private void showProgressConnect() {
        Log.d(LOG_TAG, "showProgressConnecting");

        progressConnect = new ProgressDialog(MainActivity.this);
        progressConnect.setMessage(this.getString(R.string.progressdialog_connect));
        progressConnect.setCancelable(false);
        progressConnect.setButton(DialogInterface.BUTTON_NEUTRAL, this.getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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

    private void stopProgressApplications() {
        Log.d(LOG_TAG, "stopProgressApplications");

        if (progressApplications != null) {
            progressApplications.dismiss();
        }
    }

    private void stopProgressConnect() {
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
            this.unregisterReceiver(serviceReceiver);
        } catch (Exception e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    private void updateApplications() {
        Log.d(LOG_TAG, "updateApplications");
        applicationsAddedList = new ArrayList<>();
        JSONArray json = this.getApplications();
        ArrayList<String> applicationNames = new ArrayList<String>();
        ArrayList<Drawable> applicationIcons = new ArrayList<Drawable>();
        ArrayList<Integer> applicationColors = new ArrayList<Integer>();

        // Get a list of saved applications
        try {
            for (int i = 0; i < json.length(); i++) {
                String app = json.get(i).toString();
                JSONObject temp = new JSONObject(app);
                applicationsAddedList.add(app);

                String appName = temp.getString(Config.JSON_APPLICATION_NAME);
                Drawable appIcon = this.getApplicationIcon(temp.getString(Config.JSON_PACKAGE_NAME));
                int appColor = temp.getInt(Config.JSON_APPLICATION_COLOR);

                applicationNames.add(appName);
                applicationIcons.add(appIcon);
                applicationColors.add(appColor);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error: creating JSON " + e);
            e.printStackTrace();
        }

        // Broadcast list of saved applications
        this.sendIntent(Config.INTENT_APPLICATION, Config.INTENT_APPLICATION_SAVED, json.toString());

        // Create an adapter with icons and color
        ArrayAdapterWithIconAndColor adapter = new ArrayAdapterWithIconAndColor(this, applicationNames, applicationIcons, applicationColors);

        listViewApplications.setAdapter(adapter);

        listViewApplications.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "updateApplications: " + applicationsAddedList.get(position));

                MainActivity.this.showDialogColorPicker(position);
            }
        });

        listViewApplications.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "updateApplications long: " + applicationsAddedList.get(position));

                MainActivity.this.showDialogRemoveApplication(position);
                return true;
            }
        });
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

    private void updateColorPicker(final View viewColor, SeekBar seekBar, final EditText editText, final EditText editTextHex, final int index) {

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                colors.set(index, progress);
                MainActivity.this.updateColorPickerEditText(editTexts.get(index), progress);
                MainActivity.this.updateColorPickerHex(editTextHex);
                MainActivity.this.updateColorPickerView(viewColor);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    MainActivity.this.updateColorPickerSeekbar(seekBars.get(index), Integer.parseInt(s.toString()));
                }  catch (Exception e) {
                    Log.w(LOG_TAG, "Error: parsing integer" + e);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        editTextHex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int color = Color.parseColor("#" + s.toString());
                    MainActivity.this.updateColorPickerSeekbar(seekBars.get(Config.COLOR_RED), Color.red(color));
                    MainActivity.this.updateColorPickerSeekbar(seekBars.get(Config.COLOR_GREEN), Color.green(color));
                    MainActivity.this.updateColorPickerSeekbar(seekBars.get(Config.COLOR_BLUE), Color.blue(color));
                } catch (Exception e) {
                    Log.w(LOG_TAG, "Error: parsing color" + e);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateColorPickerEditText(EditText editText, int value) {
        editText.setText("");
        editText.append(Integer.toString(value));
    }

    private void updateColorPickerHex(EditText editTextHex) {
        String red = Integer.toHexString(colors.get(Config.COLOR_RED));
        String green = Integer.toHexString(colors.get(Config.COLOR_GREEN));
        String blue = Integer.toHexString(colors.get(Config.COLOR_BLUE));

        // Zero padding
        if (red.length() < 2) {
            red = "0" + red;
        }
        if (green.length() < 2) {
            green = "0" + green;
        }
        if (blue.length() < 2) {
            blue = "0" + blue;
        }

        editTextHex.setText(red + green + blue);
    }

    private void updateColorPickerSeekbar(SeekBar seekBar, int value) {
        seekBar.setProgress(value);
    }

    private void updateColorPickerView(View viewColor) {
        viewColor.setBackgroundColor(Color.rgb(colors.get(Config.COLOR_RED), colors.get(Config.COLOR_GREEN), colors.get(Config.COLOR_BLUE)));
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

    private void updateBluetoothStatus() {
        Log.d(LOG_TAG, "updateBluetoothStatus");

        this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_STATUS);
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
        this.updateBluetoothStatus();
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

    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Config.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "serviceReceiver: " + message);

            if (message == null) {
                Log.w(LOG_TAG, "received null");
                return;
            }
            if (message.equals(Config.INTENT_SERVICE_BLUETOOTH_STARTED)) {
                MainActivity.this.updateBluetoothStatus();
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
