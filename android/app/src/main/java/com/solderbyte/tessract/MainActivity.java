package com.solderbyte.tessract;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Log tag
    private static final String LOG_TAG = "Tessract:Activity";

    // Buttons
    private static Button buttonConnect = null;

    // Device
    private static CharSequence deviceName = null;
    private static CharSequence deviceAddress = null;

    // Store
    private static TessractStore store = null;
    private static Config config = null;

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

        // Start service
        Intent serviceIntent = new Intent(this, TessractService.class);
        this.startService(serviceIntent);

        // Register receivers
        //this.registerReceiver(serviceStopReceiver, new IntentFilter(Intents.INTENT_SERVICE_STOP));
        //this.registerReceiver(bluetoothLeReceiver, new IntentFilter(Intents.INTENT_BLUETOOTH));
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

        } else if (id == R.id.nav_help) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void checkNotificationAccess() {
        Log.d(LOG_TAG, "checkNotificationAccess");
        ContentResolver contentResolver = this.getContentResolver();
        String notificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = this.getPackageName();

        if(notificationListeners == null || !notificationListeners.contains(packageName)){
            Log.d(LOG_TAG, "Notification Access Disabled");
            this.showNotificationAccess();
        }
        else {
            Log.d(LOG_TAG, "Notification Access Enabled");
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

    private void restoreUi() {
        Log.d(LOG_TAG, "restoreUi");
        String name = store.getString(config.DEVICE_NAME);
        String address = store.getString(config.DEVICE_ADDRESS);
        String state = store.getString(config.DEVICE_STATE);

        // Set textViewState
        if(state.equals(store.STRING_DEFAULT)) {
            state = getString(R.string.textview_state_disconnected);
        }
        if(name.equals(store.STRING_DEFAULT)) {
            String device = getString(R.string.textview_state_default);
            this.setTextViewState(state, device);
        }
        if(!name.equals(store.STRING_DEFAULT)) {
            deviceName = name;
            this.setTextViewState(state, deviceName.toString());
        }
        if(!address.equals(store.STRING_DEFAULT)) {
            deviceAddress = address;
        }
    }

    private void setTextViewState(String state, String device) {
        Log.d(LOG_TAG, "setTextViewState: " + state + ": " + device);
        if(textViewState != null) {
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
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        dialog.setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
            }
        });

        AlertDialog alert = dialog.create();
        alert.show();
    }
}
