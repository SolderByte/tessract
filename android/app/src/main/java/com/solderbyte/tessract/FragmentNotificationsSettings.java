package com.solderbyte.tessract;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class FragmentNotificationsSettings extends Fragment {

    // Log tag
    private static final String LOG_TAG = "Tessract:FNSettings";

    // Fragment Interation Listener
    private OnFragmentInteractionListener fragmentInteractionListener;

    // Items
    private static CharSequence[] itemsSelected = {"Action", "Amount", "Duration", "Repeat"};
    private static CharSequence[] itemsActions = {"Off", "On", "Blink", "Pulse"};
    private static CharSequence[] itemsAmounts = {"1", "2", "3", "5"};
    private static CharSequence[] itemsDurations = {"Fastest", "Fast", "Normal", "Slow"};
    private static CharSequence[] itemsRepeats = {"Never", "5 secs", "15 secs", "30 secs"};

    // Store
    private static TessractStore store = null;
    private static String keySettings = "settings";

    // Text views
    private static TextView textViewAction = null;
    private static TextView textViewActionSelected = null;
    private static TextView textViewAmount = null;
    private static TextView textViewAmountSelected = null;
    private static TextView textViewDuration = null;
    private static TextView textViewDurationSelected = null;
    private static TextView textViewRepeat = null;
    private static TextView textViewRepeatSelected = null;

    public FragmentNotificationsSettings() {}

    public static FragmentNotificationsSettings newInstance() {
        Log.d(LOG_TAG, "newInstance");
        FragmentNotificationsSettings fragment = new FragmentNotificationsSettings();

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
        View view = inflater.inflate(R.layout.fragment_notifications_settings, container, false);

        if (fragmentInteractionListener != null) {
            fragmentInteractionListener.onFragmentInteraction(this.getString(R.string.title_notifications_settings));
        }

        // Saved preferences
        store = new TessractStore();
        store.init(this.getContext());

        // UI listeners
        this.createUiListeners(view);
        this.updateUi();

        return view;
    }

    @Override
    public void onDetach() {
        Log.d(LOG_TAG, "onDetach");

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

        // TextViews
        textViewActionSelected = (TextView) view.findViewById(R.id.textview_action_selected);
        textViewAction = (TextView) view.findViewById(R.id.textview_action);
        textViewAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "onClick");

                FragmentNotificationsSettings.this.showDialogItemsSelected(itemsSelected[0], itemsActions);
            }
        });

        textViewAmountSelected = (TextView) view.findViewById(R.id.textview_amount_selected);
        textViewAmount = (TextView) view.findViewById(R.id.textview_amount);
        textViewAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "onClick");
                FragmentNotificationsSettings.this.showDialogItemsSelected(itemsSelected[1], itemsAmounts);
            }
        });

        textViewDurationSelected = (TextView) view.findViewById(R.id.textview_duration_selected);
        textViewDuration = (TextView) view.findViewById(R.id.textview_duration);
        textViewDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "onClick");
                FragmentNotificationsSettings.this.showDialogItemsSelected(itemsSelected[2], itemsDurations);
            }
        });

        textViewRepeatSelected = (TextView) view.findViewById(R.id.textview_repeat_selected);
        textViewRepeat = (TextView) view.findViewById(R.id.textview_repeat);
        textViewRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "onClick");
                FragmentNotificationsSettings.this.showDialogItemsSelected(itemsSelected[3], itemsRepeats);
            }
        });
    }

    private void setTextView(int index, CharSequence selected, CharSequence item) {
        Log.d(LOG_TAG, "setTextView");

        store.setInt(keySettings + "." + selected, index);

        if (selected.equals(itemsSelected[0])) {
            textViewActionSelected.setText(item);
        } else if (selected.equals(itemsSelected[1])) {
            textViewAmountSelected.setText(item);
        } else if (selected.equals(itemsSelected[2])) {
            textViewDurationSelected.setText(item);
        } else if (selected.equals(itemsSelected[3])) {
            textViewRepeatSelected.setText(item);
        }
    }

    private void showDialogItemsSelected(final CharSequence selected, final CharSequence[] items) {
        Log.d(LOG_TAG, "showDialogItemsSelected");

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle(R.string.dialog_select);
        builder.setCancelable(false);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                Log.d(LOG_TAG, "onClick: " + items[index]);

                FragmentNotificationsSettings.this.setTextView(index, selected, items[index]);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateTextView() {
        Log.d(LOG_TAG, "updateTextView");

        int action = store.getInt(keySettings + "." + itemsSelected[0]);
        if (action == store.INT_DEFAULT) {
            action = 2; // Blink
        }

        int amount = store.getInt(keySettings + "." + itemsSelected[1]);
        if (amount == store.INT_DEFAULT) {
            amount = 2; // 3
        }

        int duration = store.getInt(keySettings + "." + itemsSelected[2]);
        if (duration == store.INT_DEFAULT) {
            duration = 2; // Normal
        }

        int repeat = store.getInt(keySettings + "." + itemsSelected[3]);
        if (repeat == store.INT_DEFAULT) {
            repeat = 0; // Never
        }

        this.setTextView(action, itemsSelected[0], itemsActions[action]);
        this.setTextView(amount, itemsSelected[1], itemsAmounts[amount]);
        this.setTextView(duration, itemsSelected[2], itemsDurations[duration]);
        this.setTextView(repeat, itemsSelected[3], itemsRepeats[repeat]);
    }

    private void updateUi() {
        Log.d(LOG_TAG, "updateUi");

        this.updateTextView();
    }
}
