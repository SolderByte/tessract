package com.solderbyte.tessract;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivityDevice extends AppCompatActivity implements  DialogColorPicker.OnColorSelectedListener {

    // Log tag
    private static final String LOG_TAG = "Tessract:Device";

    // Button
    private static Button buttonSet = null;

    // Color
    private static int color = 0;

    // Color picker
    private static ImageView colorPicker = null;

    // Items
    private static CharSequence[] itemsSelected = {"Action", "Amount", "Duration", "Repeat"};
    private static CharSequence[] itemsActions = {"Off", "On", "Blink", "Pulse"};
    private static CharSequence[] itemsAmounts = {"1", "2", "3", "5"};
    private static CharSequence[] itemsDurations = {"Fastest", "Fast", "Normal", "Slow"};
    private static CharSequence[] itemsRepeats = {"Never", "5 secs", "15 secs", "30 secs"};

    // Text views
    private static TextView textViewAction = null;
    private static TextView textViewActionSelected = null;
    private static TextView textViewAmount = null;
    private static TextView textViewAmountSelected = null;
    private static TextView textViewDuration = null;
    private static TextView textViewDurationSelected = null;
    private static TextView textViewRepeat = null;
    private static TextView textViewRepeatSelected = null;

    // Store
    private static TessractStore store = null;
    private static String storeKey = "device";

    @Override
    public void onColorSelected(int c) {
        Log.d(LOG_TAG, "onColorSelected: " + c);

        color = c;
        store.setInt(Config.DEVICE_COLOR, c);
        this.updateUi();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        // Saved preferences
        store = new TessractStore();
        store.init(this.getApplicationContext());

        // UI listeners
        this.createUiListeners();
        this.updateUi();
    }

    private void createUiListeners() {
        Log.d(LOG_TAG, "createUiListeners");

        buttonSet =  (Button) this.findViewById(R.id.button_set);
        buttonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "set");
                ActivityDevice.this.set();
            }
        });

        colorPicker = (ImageView) this.findViewById(R.id.color_picker_color);
        colorPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "onClick");

                Bundle arguments = new Bundle();
                arguments.putInt(DialogColorPicker.colorKey, color);
                DialogColorPicker colorPicker = new DialogColorPicker();
                colorPicker.setArguments(arguments);
                colorPicker.show(ActivityDevice.this.getSupportFragmentManager(), LOG_TAG);
            }
        });

        textViewActionSelected = (TextView) this.findViewById(R.id.textview_action_selected);
        textViewAmountSelected = (TextView) this.findViewById(R.id.textview_amount_selected);
        textViewDurationSelected = (TextView) this.findViewById(R.id.textview_duration_selected);
        textViewRepeatSelected = (TextView) this.findViewById(R.id.textview_repeat_selected);

        textViewAction = (TextView) this.findViewById(R.id.textview_action);
        textViewAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "onClick");
                ActivityDevice.this.showDialog(itemsSelected[0], itemsActions);
            }
        });

        textViewAmount = (TextView) this.findViewById(R.id.textview_amount);
        textViewAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "onClick");
                ActivityDevice.this.showDialog(itemsSelected[1], itemsAmounts);
            }
        });

        textViewDuration = (TextView) this.findViewById(R.id.textview_duration);
        textViewDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "onClick");
                ActivityDevice.this.showDialog(itemsSelected[2], itemsDurations);
            }
        });

        textViewRepeat = (TextView) this.findViewById(R.id.textview_repeat);
        textViewRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "onClick");
                ActivityDevice.this.showDialog(itemsSelected[3], itemsRepeats);
            }
        });
    }

    public void sendIntent(String name, String message, byte[] data) {
        Log.v(LOG_TAG, "sendIntent:" + name + " : " + message);

        Intent msg = new Intent(name);
        msg.putExtra(Config.INTENT_EXTRA_MSG, message);
        msg.putExtra(Config.INTENT_EXTRA_DATA, data);
        this.sendBroadcast(msg);
    }

    private void set() {
        Log.d(LOG_TAG, "set");

        int action = store.getInt(storeKey + "." + itemsSelected[0]);
        int amount = store.getInt(storeKey + "." + itemsSelected[1]);
        int duration = store.getInt(storeKey + "." + itemsSelected[2]);
        int repeat = store.getInt(storeKey + "." + itemsSelected[3]);
        String rgb = TessractProtocol.colorToHex(color);

        byte[] bytes = TessractProtocol.toProtocol(action, amount, duration, repeat, rgb);

        this.sendIntent(Config.INTENT_BLUETOOTH, Config.INTENT_BLUETOOTH_WRITE, bytes);
    }

    private void setTextView(int index, CharSequence selected, CharSequence item) {
        Log.d(LOG_TAG, "setTextView: " + index + " " + selected + " " + item);

        store.setInt(storeKey + "." + selected, index);

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

    private void showDialog(final CharSequence selected, final CharSequence[] items) {
        Log.d(LOG_TAG, "showDialog");

        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityDevice.this);
        builder.setTitle(R.string.dialog_select);
        builder.setCancelable(false);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                Log.d(LOG_TAG, "onClick: " + items[index]);

                ActivityDevice.this.setTextView(index, selected, items[index]);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateDeviceColor() {
        Log.d(LOG_TAG, "updateDeviceColor");

        color = store.getInt(Config.DEVICE_COLOR);

        colorPicker = (ImageView) this.findViewById(R.id.color_picker_color);
        colorPicker.setBackgroundColor(color);
    }

    private void updateTextViews() {
        Log.d(LOG_TAG, "updateTextViews");

        int action = store.getInt(storeKey + "." + itemsSelected[0]);
        int amount = store.getInt(storeKey + "." + itemsSelected[1]);
        int duration = store.getInt(storeKey + "." + itemsSelected[2]);
        int repeat = store.getInt(storeKey + "." + itemsSelected[3]);

        this.setTextView(action, itemsSelected[0], itemsActions[action]);
        this.setTextView(amount, itemsSelected[1], itemsAmounts[amount]);
        this.setTextView(duration, itemsSelected[2], itemsDurations[duration]);
        this.setTextView(repeat, itemsSelected[3], itemsRepeats[repeat]);
    }

    private void updateUi() {
        Log.d(LOG_TAG, "updateUi");

        this.updateDeviceColor();
        this.updateTextViews();
    }
}
