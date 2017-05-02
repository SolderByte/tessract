package com.solderbyte.tessract;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;

import java.util.ArrayList;

public class DialogColorPicker extends DialogFragment {

    // Log tag
    private static final String LOG_TAG = "Tessract:DColorPicker";

    // Colors
    public static final int COLOR_RED = 0;
    public static final int COLOR_GREEN = 1;
    public static final int COLOR_BLUE = 2;
    public static final int COLOR_DEFAULT = -12627531;

    // Color picker
    private static ArrayList<Integer> colors = null;
    private static ArrayList<SeekBar> seekBars = null;
    private static ArrayList<EditText> editTexts = null;

    // Keys
    public static String colorKey = "color";

    // Strings
    private static String positiveButton = "Select";
    private static String negativeButton = "Cancel";

    // Listener
    public interface OnColorSelectedListener {
       void onColorSelected(int color);
    }

    private OnColorSelectedListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.listener = (OnColorSelectedListener) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement listener");
        }
    }

    public DialogColorPicker() {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateDialog");

        // Get layout
        LayoutInflater inflater = (LayoutInflater) this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_color_picker, (ViewGroup) this.getActivity().findViewById(R.id.colorpicker_layout));

        // Set color
        int color = COLOR_DEFAULT;
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            color = bundle.getInt(colorKey);
        }
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

        // Create dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        this.setCancelable(false);

        builder.setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showDialogColorPicker: " + colors.get(COLOR_RED) + " " + colors.get(COLOR_GREEN) + " " + colors.get(COLOR_BLUE));

                int color = Color.rgb(colors.get(COLOR_RED), colors.get(COLOR_GREEN), colors.get(COLOR_BLUE));
                listener.onColorSelected(color);

                dialog.dismiss();
            }
        });

        builder.setNegativeButton(negativeButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(LOG_TAG, "showDialogColorPicker: cancel");

                dialog.dismiss();
            }
        });

        // Set view
        builder.setView(layout);

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
        this.updateColorPickerSeekbar(seekBars.get(COLOR_RED), red);
        this.updateColorPickerSeekbar(seekBars.get(COLOR_GREEN), green);
        this.updateColorPickerSeekbar(seekBars.get(COLOR_BLUE), blue);
        this.updateColorPickerEditText(editTexts.get(COLOR_RED), red);
        this.updateColorPickerEditText(editTexts.get(COLOR_GREEN), green);
        this.updateColorPickerEditText(editTexts.get(COLOR_BLUE), blue);

        // Set color picker listener
        this.updateColorPicker(viewColor, seekBarRed, editTextRed, editTextHex, COLOR_RED);
        this.updateColorPicker(viewColor, seekBarGreen,editTextGreen, editTextHex, COLOR_GREEN);
        this.updateColorPicker(viewColor, seekBarBlue, editTextBlue, editTextHex, COLOR_BLUE);;

        return builder.create();
    }

    private void updateColorPicker(final View viewColor, SeekBar seekBar, final EditText editText, final EditText editTextHex, final int index) {

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                colors.set(index, progress);
                DialogColorPicker.this.updateColorPickerEditText(editTexts.get(index), progress);
                DialogColorPicker.this.updateColorPickerHex(editTextHex);
                DialogColorPicker.this.updateColorPickerView(viewColor);
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
                    DialogColorPicker.this.updateColorPickerSeekbar(seekBars.get(index), Integer.parseInt(s.toString()));
                }  catch (Exception e) {
                    if (!e.getMessage().equals("For input string: \"\"")) {
                        Log.w(LOG_TAG, "Error: parsing integer " + e);
                    }
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
                    DialogColorPicker.this.updateColorPickerSeekbar(seekBars.get(COLOR_RED), Color.red(color));
                    DialogColorPicker.this.updateColorPickerSeekbar(seekBars.get(COLOR_GREEN), Color.green(color));
                    DialogColorPicker.this.updateColorPickerSeekbar(seekBars.get(COLOR_BLUE), Color.blue(color));
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
        String red = Integer.toHexString(colors.get(COLOR_RED));
        String green = Integer.toHexString(colors.get(COLOR_GREEN));
        String blue = Integer.toHexString(colors.get(COLOR_BLUE));

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
        viewColor.setBackgroundColor(Color.rgb(colors.get(COLOR_RED), colors.get(COLOR_GREEN), colors.get(COLOR_BLUE)));
    }
}
