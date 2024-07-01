package com.example.signapp;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlmacenamientoNotificacion {
    private static final String PREFS_NAME = "notifications";
    private static final String KEY_NOTIFICATIONS = "notifications";

    public static void addNotification(Context context, String notification) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> notifications = prefs.getStringSet(KEY_NOTIFICATIONS, new HashSet<>());
        notifications.add(notification);
        prefs.edit().putStringSet(KEY_NOTIFICATIONS, notifications).apply();
    }

    public static List<String> getNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> notifications = prefs.getStringSet(KEY_NOTIFICATIONS, new HashSet<>());
        return new ArrayList<>(notifications);
    }
}
