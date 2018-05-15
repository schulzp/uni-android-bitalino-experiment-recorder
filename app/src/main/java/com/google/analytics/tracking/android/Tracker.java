package com.google.analytics.tracking.android;

import android.util.Log;

import java.util.Map;

public class Tracker {

    public String getName() {
        return null;
    }

    public void send(Map<String, String> params) {
        Log.d("TRACKER", "send " + params);
    }

    public String get(String key) {
        return null;
    }

    public void set(String key, String value) {

    }

}
