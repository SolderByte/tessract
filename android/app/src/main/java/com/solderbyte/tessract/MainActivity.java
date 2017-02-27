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
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Log tag
    private static final String LOG_TAG = "Tessract:Activity";

    // Buttons
    private static Button buttonConnect = null;

    // Device
    private static CharSequence deviceName = null;
    private static CharSequence deviceAddress = null;
    private static ArrayList<String> deviceList = null;
    private static ArrayList<String> deviceScannedList = null;
    private static ArrayList<String> devicePairedList = null;

    // Permission
    private static final int PERMISSION_ACCESS_COARSE_LOCATION = 100;

    // Progress dialogs
    private static ProgressDialog progressScan = null;
    private static int progressScanIterations = 0;
    private static int progressScanIncrement = 10;
    private static int progressScanPeriod = 700;

    // Store
    private static TessractStore store = null;

    // TextViews
    private static TextView textViewState = null;

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
        this.restoreUi();

        // Check notification access
        this.checkNotificationAccess();

        // Check permissions
        this.checkPermissions();

        // Start service
        Intent serviceIntent = new Intent(this, TessractService.class);
        this.startService(serviceIntent);

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
            deviceList = null;
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

        // TextViews
        textViewState = (TextView) findViewById(R.id.textview_state);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Floating action button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Implement me!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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

        this.registerReceiver(shutdownReceiver, new IntentFilter(Config.INTENT_SHUTDOWN));
        this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Config.INTENT_BLUETOOTH));
    }

    private void restoreUi() {
        Log.d(LOG_TAG, "restoreUi");
        String name = store.getString(Config.DEVICE_NAME);
        String address = store.getString(Config.DEVICE_ADDRESS);
        String state = store.getString(Config.DEVICE_STATE);

        // Set textViewState
        if (state.equals(store.STRING_DEFAULT)) {
            state = getString(R.string.textview_state_disconnected);
        }
        if (name.equals(store.STRING_DEFAULT)) {
            String device = getString(R.string.textview_state_default);
            this.setTextViewState(state, device);
        }
        if (!name.equals(store.STRING_DEFAULT)) {
            deviceName = name;
            this.setTextViewState(state, deviceName.toString());
        }
        if (!address.equals(store.STRING_DEFAULT)) {
            deviceAddress = address;
        }
    }

    public void sendIntent(String name, String message) {
        Intent msg = new Intent(name);
        msg.putExtra(Config.INTENT_EXTRA_MSG, message);
        this.sendBroadcast(msg);
    }

    private void setTextViewState(String state, String device) {
        Log.d(LOG_TAG, "setTextViewState: " + state + ": " + device);

        if (textViewState != null) {
            textViewState.setText(state + ": " + device);
        }
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

    private void showDialogScan() {
        Log.d(LOG_TAG, "showDialogScan");
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.dialog_scan_bt_title);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceList);

        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                Log.d(LOG_TAG, "index:" + index);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showProgressScan() {
        Log.d(LOG_TAG, "showProgressScan");

        progressScan = new ProgressDialog(MainActivity.this);
        progressScan.setMessage(getString(R.string.progressdialog_scan));
        progressScan.setCancelable(false);
        progressScan.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressScan.show();

        progressScanIterations = 0;
        this.startProgressScan();
    }

    private void stopProgressScan() {
        Log.d(LOG_TAG, "stopProgressScan");

        if(progressScan != null) {
            progressScan.dismiss();
        }
    }

    private void startProgressScan() {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressScanIterations += 1;

                if (progressScanIterations <= progressScanIncrement) {
                    progressScan.incrementProgressBy(progressScanIncrement);
                    Log.d(LOG_TAG, "Progress scan: " + progressScan.getProgress());
                    MainActivity.this.startProgressScan();
                } else {
                    Log.d(LOG_TAG, "Progress scan completed");
                }
            }
        }, progressScanPeriod);
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

    private void updateDeviceList(ArrayList<String> list) {
        Log.d(LOG_TAG, "updateDeviceList: " + list.get(0));

        if (deviceList == null) {
            deviceList = new ArrayList<String>();
        }
        deviceList.add(list.get(0));
    }

    private BroadcastReceiver bluetoothLeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Config.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "bluetoothLeReceiver: " + message);

            if (message.equals(Config.INTENT_BLUETOOTH_SCANNED)) {
                MainActivity.this.stopProgressScan();
                MainActivity.this.showDialogScan();
            }
            if (message.equals(Config.INTENT_BLUETOOTH_DEVICE)) {
                ArrayList<String> list = intent.getStringArrayListExtra(Config.INTENT_EXTRA_DATA);
                Log.d(LOG_TAG, list.toString());
                updateDeviceList(list);
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
