package com.solderbyte.tessract;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TessractStore {

    // Log tag
    private static final String LOG_TAG = "Tessract:Store";

    // Defaults
    public static final String APPLICATIONS = "applications";
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

    public void deleteJSONArray(String key, int index) {
        ArrayList<String> jsonArray = new ArrayList<>();
        String stored = this.getJSONArray(key);
        JSONArray jsonStored;

        try {
            jsonStored = new JSONArray(stored);

            if (index > jsonStored.length()) {
                return;
            }
            jsonStored.remove(index);

            for (int i = 0; i < jsonStored.length(); i++) {
                String old = jsonStored.get(i).toString();
                jsonArray.add(old);
            }
        } catch (JSONException e) {

        }

        editor.putString(key + ":jsonarray", jsonArray.toString());
        editor.commit();
    }

    public boolean getBoolean(String key) {
        boolean value = preferences.getBoolean(key + ":boolean", BOOEAN_DEFAULT);
        return value;
    }

    public int getInt(String key) {
        return preferences.getInt(key + ":int", INT_DEFAULT);
    }

    public String getJSON(String key) {
        String json = preferences.getString(key + ":json", STRING_DEFAULT);
        return json;
    }

    public String getJSONArray(String key) {
        String json = preferences.getString(key + ":jsonarray", STRING_DEFAULT);
        return json;
    }

    public String getString(String key) {
        String value = preferences.getString(key + ":string", STRING_DEFAULT);
        return value;
    }

    public void setBoolean(String key, boolean value) {
        editor.putBoolean(key + ":boolean", value);
        editor.commit();
    }

    public void setInt(String key, int value) {
        editor.putInt(key + ":int", value);
        editor.commit();
    }

    public void setJSON(String key, String json) {
        editor.putString(key + ":json", json);
        editor.commit();
    }

    public void setJSONArray(String key, String json) {
        ArrayList<String> jsonArray = new ArrayList<>();
        String stored = this.getJSONArray(key);
        JSONArray jsonStored;

        if (stored.equals(STRING_DEFAULT)) {
            jsonArray.add(json);
        } else {
            try {
                jsonStored = new JSONArray(stored);

                for (int i = 0; i < jsonStored.length(); i++) {
                    String old = jsonStored.get(i).toString();
                    jsonArray.add(old);
                }

                jsonArray.add(json);
            } catch (JSONException e) {

            }
        }

        editor.putString(key + ":jsonarray", jsonArray.toString());
        editor.commit();
    }

    public void setString(String key, String value) {
        editor.putString(key + ":string", value);
        editor.commit();
    }

    public void updateJSONArray(String key, String json, int index) {
        ArrayList<String> jsonArray = new ArrayList<>();
        String stored = this.getJSONArray(key);
        JSONArray jsonStored;

        try {
            jsonStored = new JSONArray(stored);

            if (index > jsonStored.length()) {
                return;
            }
            jsonStored.put(index, json);

            for (int i = 0; i < jsonStored.length(); i++) {
                String old = jsonStored.get(i).toString();
                jsonArray.add(old);
            }
        } catch (JSONException e) {

        }

        editor.putString(key + ":jsonarray", jsonArray.toString());
        editor.commit();
    }
}
