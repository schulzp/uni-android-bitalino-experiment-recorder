package com.google.analytics.tracking.android;

import android.app.Activity;
import android.content.Context;

public class EasyTracker extends Tracker {

        public void activityStart(Activity activity) { }

        public void activityStop(Activity activity) { }

        public void dispatchLocalHits() { }

        private static final EasyTracker instance = new EasyTracker();

        public static EasyTracker getInstance(Context context) {
                return instance;
        }

        public static void setResourcePackageName(String name) { }

}
