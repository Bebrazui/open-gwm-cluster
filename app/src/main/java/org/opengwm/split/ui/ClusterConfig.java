package org.opengwm.split.ui;

import android.content.Context;
import android.content.SharedPreferences;

public class ClusterConfig {

    private static final String PREF_NAME = "gwm_cluster_prefs";

    public static final String KEY_THEME = "pref_theme";
    public static final String KEY_HOLE_PUNCH = "pref_hole_punch";
    public static final String KEY_SMOOTHING = "pref_smoothing";
    public static final String KEY_DRIVE_MODE = "pref_drive_mode";
    public static final String KEY_CRUISE_SPEED = "pref_cruise_speed";
    public static final String KEY_DEMO_MODE = "pref_demo_mode";

    public static final int THEME_SPORT_RED = 0;
    public static final int THEME_LUXURY_GOLD = 1;
    public static final int THEME_CYBER_NEON = 2;
    public static final int THEME_OFFROAD_AMBER = 3;

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getTheme(Context context) {
        return getPrefs(context).getInt(KEY_THEME, THEME_SPORT_RED);
    }

    public static void setTheme(Context context, int theme) {
        getPrefs(context).edit().putInt(KEY_THEME, theme).apply();
    }

    public static boolean isHolePunchEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_HOLE_PUNCH, true);
    }

    public static void setHolePunchEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_HOLE_PUNCH, enabled).apply();
    }

    public static boolean isSmoothingEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_SMOOTHING, true);
    }

    public static void setSmoothingEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_SMOOTHING, enabled).apply();
    }

    public static String getDriveMode(Context context) {
        return getPrefs(context).getString(KEY_DRIVE_MODE, "STANDARD 4H");
    }

    public static void setDriveMode(Context context, String mode) {
        getPrefs(context).edit().putString(KEY_DRIVE_MODE, mode).apply();
    }

    public static int getCruiseSpeed(Context context) {
        return getPrefs(context).getInt(KEY_CRUISE_SPEED, 0);
    }

    public static void setCruiseSpeed(Context context, int speed) {
        getPrefs(context).edit().putInt(KEY_CRUISE_SPEED, speed).apply();
    }

    public static boolean isDemoMode(Context context) {
        return getPrefs(context).getBoolean(KEY_DEMO_MODE, false);
    }

    public static void setDemoMode(Context context, boolean demo) {
        getPrefs(context).edit().putBoolean(KEY_DEMO_MODE, demo).apply();
    }
}
