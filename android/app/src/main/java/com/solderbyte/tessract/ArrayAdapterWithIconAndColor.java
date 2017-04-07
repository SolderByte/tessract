package com.solderbyte.tessract;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ArrayAdapterWithIconAndColor extends ArrayAdapter<String> {

    private final ArrayList<String> items;
    private final ArrayList<Drawable> icons;
    private final ArrayList<Integer> colors;

    public ArrayAdapterWithIconAndColor(Context context, ArrayList<String> items, ArrayList<Drawable> icons, ArrayList<Integer> colors) {
        super(context, R.layout.list_icons_color_picker, R.id.list_color_picker_text, items);
        this.items = items;
        this.icons = icons;
        this.colors = colors;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;

        if (convertView == null) {
            view = super.getView(position, convertView, parent);
        } else {
            view = convertView;
        }

        // Force icon size
        Drawable i = icons.get(position);
        Bitmap bitmap = ((BitmapDrawable) i).getBitmap();
        Drawable icon = new BitmapDrawable(view.getResources(), Bitmap.createScaledBitmap(bitmap, 144, 144, true));

        String appName = items.get(position);

        Integer color = colors.get(position);

        TextView textView = (TextView) view.findViewById(R.id.list_color_picker_text);
        textView.setText(appName);

        ImageView iconView = (ImageView) view.findViewById(R.id.list_color_picker_icon);
        iconView.setImageDrawable(icon);

        ImageView colorView = (ImageView) view.findViewById(R.id.list_color_picker_color);
        colorView.setBackgroundColor(color);

        return view;
    }
}
