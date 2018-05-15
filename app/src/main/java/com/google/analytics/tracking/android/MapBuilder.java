package com.google.analytics.tracking.android;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder {

    private final Map<String, String> map;

    public MapBuilder(Map<String, String> map) {
        this.map = map;
    }

    public Map<String, String> build() {
        return map;
    }

    public static MapBuilder createEvent(final String context, final String event, final String value, final Long time) {
        HashMap<String, String> map = new HashMap<>();
        map.put("context", context);
        map.put("event", event);
        map.put("value", value);
        map.put("time", time == null ? null : time.toString());
        return new MapBuilder(map);
    }

}
