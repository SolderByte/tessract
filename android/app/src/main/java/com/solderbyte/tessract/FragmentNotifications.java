package com.solderbyte.tessract;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class FragmentNotifications extends Fragment {

    // Log tag
    private static final String LOG_TAG = "Tessract:FNotifications";

    // Buttons
    private static FloatingActionButton fab = null;

    // Fragment Interation Listener
    private FragmentNotifications.OnFragmentInteractionListener fragmentInteractionListener;

    // Intents
    public static String INTENT = "com.solderbyte";
    public static String INTENT_EXTRA_MSG = INTENT + ".message";
    public static String INTENT_EXTRA_DATA = INTENT + ".data";

    // Progress dialogs
    private static ProgressDialog progressApplications = null;

    // Store
    private static TessractStore store = null;
    private static String keyDeviceName = "device.name";

    public FragmentNotifications() {}

    public static FragmentNotifications newInstance() {
        Log.d(LOG_TAG, "newInstance");
        FragmentNotifications fragment = new FragmentNotifications();

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
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        if (fragmentInteractionListener != null) {
            fragmentInteractionListener.onFragmentInteraction(this.getString(R.string.title_notifications));
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

        // Hide fab
        this.toggleFab(false);

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

    private void createUiListeners(View view) {
        Log.d(LOG_TAG, "createUiListeners");

        // Buttons
        fab = (FloatingActionButton) this.getActivity().findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "fab");

                FragmentNotifications.this.startProgressApplications();
                FragmentNotifications.this.sendIntent(TessractService.INTENT_APPLICATION, TessractService.INTENT_APPLICATION_LIST);
            }
        });
        this.toggleFab(true);
    }

    private void setApplicationsList(ArrayList<String> data) {
        Log.d(LOG_TAG, "setApplicationsList");
    }

    private void sendIntent(String name, String message) {
        Log.v(LOG_TAG, "sendIntent: " + message);

        String intentMsg = INTENT_EXTRA_MSG;

        if (name.equals(TessractService.INTENT_APPLICATION)) {
            intentMsg = TessractService.INTENT_EXTRA_MSG;
        }

        Intent msg = new Intent(name);
        msg.putExtra(intentMsg, message);
        this.getContext().sendBroadcast(msg);
    }

    private void startProgressApplications() {
        Log.d(LOG_TAG, "startProgressApplications");

        progressApplications = new ProgressDialog(this.getContext());
        progressApplications.setMessage(this.getString(R.string.progressdialog_applications));
        progressApplications.setCancelable(false);

        progressApplications.show();
    }

    private void stopProgressApplications() {
        Log.d(LOG_TAG, "stopProgressApplications");

        if (progressApplications != null) {
            progressApplications.dismiss();
        }
    }

    private void toggleFab(boolean value) {
        Log.d(LOG_TAG, "toggleFab");

        if (fab != null) {
            if (value) {
                fab.show();
            } else {
                fab.hide();
            }
        }
    }

    private void updateUi() {
        Log.d(LOG_TAG, "updateUi");
    }

    private void registerReceivers() {
        Log.d(LOG_TAG, "registerReceivers");

        this.getContext().registerReceiver(applicationReceiver, new IntentFilter(TessractService.INTENT_APPLICATION));
    }

    private void unregisterReceivers() {
        Log.d(LOG_TAG, "unregisterReceivers");

        try {
            this.getContext().unregisterReceiver(applicationReceiver);
        } catch (Exception e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    private BroadcastReceiver applicationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(TessractService.INTENT_EXTRA_MSG);
            Log.d(LOG_TAG, "applicationReceiver: " + message);

            if (message == null) {
                return;
            }
            if (message.equals(TessractService.INTENT_APPLICATION_LISTED)) {
                FragmentNotifications.this.stopProgressApplications();
                ArrayList<String> data = intent.getStringArrayListExtra(INTENT_EXTRA_DATA);
                FragmentNotifications.this.setApplicationsList(data);
            }
        }
    };
}
