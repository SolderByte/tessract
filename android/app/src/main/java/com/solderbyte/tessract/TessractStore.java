package com.solderbyte.tessract;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class TessractStore {

    // Log tag
    private static final String LOG_TAG = "Tessract:Store";

    // Defaults
    public static final String STRING_DEFAULT = "default";
    public static final boolean BOOEAN_DEFAULT = false;
    public static final int INT_DEFAULT = 0;

    // Store
    private SharedPreferences preferences = null;
    private Editor editor = null;

    public TessractStore() {}

    public void init(Context context) {
        Log.d(LOG_TAG, "init");
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();
    }

    public boolean getBoolean(String key) {
        boolean value = preferences.getBoolean(key + ":boolean", BOOEAN_DEFAULT);
        return value;
    }

    public int getInt(String key) {
        return preferences.getInt(key + ":int", INT_DEFAULT);
    }

    public String getString(String key) {
        String value = preferences.getString(key + ":string", STRING_DEFAULT);
        return value;
    }

    public void saveBoolean(String key, boolean value) {
        editor.putBoolean(key + ":boolean", value);
        editor.commit();
    }

    public void saveInt(String key, int value) {
        editor.putInt(key + ":int", value);
        editor.commit();
    }

    public void saveString(String key, String value) {
        editor.putString(key + ":string", value);
        editor.commit();
    }
}
