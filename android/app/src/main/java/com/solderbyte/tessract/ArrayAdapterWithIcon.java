package com.solderbyte.tessract;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

public class ArrayAdapterWithIcon extends ArrayAdapter<String> {

    private final ArrayList<String> items;
    private final ArrayList<Drawable> icons;


    public ArrayAdapterWithIcon(Context context, ArrayList<String> items, ArrayList<Drawable> icons) {
        super(context, R.layout.dialog_list_icons, R.id.dialog_list_text, items);
        this.items = items;
        this.icons = icons;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;

        if (convertView == null) {
            view = super.getView(position, convertView, parent);
        } else {
            view = convertView;
        }

        Drawable icon = icons.get(position);

        ImageView iconView = (ImageView) view.findViewById(R.id.dialog_list_icon);
        iconView.setAdjustViewBounds(true);
        iconView.setMaxWidth(iconView.getMaxWidth());
        iconView.setMaxHeight(iconView.getMaxHeight());
        iconView.setImageDrawable(icon);

        return view;
    }
}
