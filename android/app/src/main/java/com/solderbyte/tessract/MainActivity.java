package com.solderbyte.tessract;

import android.Manifest;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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

public class MainActivity extends AppCompatActivity implements
    FragmentDevice.OnFragmentInteractionListener,
    FragmentNotifications.OnFragmentInteractionListener,
    FragmentNotificationsSettings.OnFragmentInteractionListener,
    FragmentHelp.OnFragmentInteractionListener,
    NavigationView.OnNavigationItemSelectedListener {

    // Log tag
    private static final String LOG_TAG = "Tessract:Activity";

    // Buttons
    private static FloatingActionButton fab = null;

    // Intents
    public static final String INTENT = "com.solderbyte.mainactivity";
    public static final String INTENT_SHUTDOWN = INTENT + ".shutdown";
    public static final String INTENT_EXTRA_MSG = INTENT + ".message";
    public static final String INTENT_EXTRA_DATA = INTENT + ".data";

    // Permission
    private static final int PERMISSION_ACCESS_COARSE_LOCATION = 100;

    // Store
    private static TessractStore store = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");

        // Super
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        // Setup Fragments
        FragmentTransaction fragmentTransaction = this.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, new FragmentDevice());
        fragmentTransaction.commit();

        // Saved preferences
        store = new TessractStore();
        store.init(this.getApplicationContext());

        // UI listeners
        this.createUiListeners();

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
        this.getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onFragmentInteraction(String title) {
        Log.d(LOG_TAG, "onFragmentInteraction");

        // Replace action bar title
        this.getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(LOG_TAG, "onOptionsItemSelected: " + item.toString());
        int id = item.getItemId();

        if (id == R.id.menu_main) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Log.d(LOG_TAG, "onNavigationItemSelected: " + item.toString());
        int id = item.getItemId();

        // Select a fragment
        Fragment fragment = null;
        if (id == R.id.nav_device) {
            fragment = new FragmentDevice();
        } else if (id == R.id.nav_notifications) {
            fragment = new FragmentNotifications();
        } else if (id == R.id.nav_notifications_settings) {
          fragment = new FragmentNotificationsSettings();
        } else if (id == R.id.nav_help) {
            fragment =  new FragmentHelp();
        }

        // Set the fragment
        if (fragment != null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.frame_layout, fragment);
            fragmentTransaction.commit();
        }

        DrawerLayout drawer = (DrawerLayout) this.findViewById(R.id.drawer_layout);
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

        // Toolbar
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Floating action button
        fab = (FloatingActionButton) this.findViewById(R.id.fab);
        fab.hide();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {}
        });

        // Drawer
        DrawerLayout drawer = (DrawerLayout) this.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // Navbar
        NavigationView navigationView = (NavigationView) this.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_device);
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
                MainActivity.this.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
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

    private void startServices() {
        Log.d(LOG_TAG, "startServices");

        Intent serviceIntent = new Intent(this, TessractService.class);
        this.startService(serviceIntent);
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.registerReceiver(shutdownReceiver, new IntentFilter(INTENT_SHUTDOWN));
    }

    private void unregisterReceivers() {
        Log.d(LOG_TAG, "unregisterReceivers");

        try {
            this.unregisterReceiver(shutdownReceiver);
        } catch (Exception e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

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
