package com.solderbyte.tessract;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

public class DialogScannedDevices extends DialogFragment {

    // Log tag
    private static final String LOG_TAG = "Tessract:DScannedDevice";

    // Keys
    public static String LIST_KEY = "color";

    // Listener
    public interface OnSelectedListener {
        void onSelected(int index);
    }

    private OnSelectedListener listener;

    public DialogScannedDevices() {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateDialog");

        // Get list
        ArrayList<String> scannedDevices = new ArrayList<>();
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            scannedDevices = bundle.getStringArrayList(LIST_KEY);
        }

        // Create dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setTitle(R.string.dialog_scan_title);
        this.setCancelable(false);

        if (scannedDevices.size() == 0) {
            // Devices not found
            builder.setMessage(this.getString(R.string.dialog_scan_none));
        } else {
            // Devices found
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_list_item_activated_1, scannedDevices);

            builder.setSingleChoiceItems(arrayAdapter, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });

            builder.setPositiveButton(this.getString(R.string.button_select), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Get the position of selected device
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    Log.d(LOG_TAG, "DialogScannedDevices: " + selectedPosition);

                    if (listener != null) {
                        listener.onSelected(selectedPosition);
                    }

                    dialog.dismiss();
                }
            });
        }

        builder.setNegativeButton(this.getString(R.string.button_close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "DialogScannedDevices: close");

                dialog.dismiss();
            }
        });

        return builder.create();
    }

    public void setOnSelectedListener(OnSelectedListener onSelectedListener) {
        this.listener = onSelectedListener;
    }
}
