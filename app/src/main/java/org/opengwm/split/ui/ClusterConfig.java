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
    public static final String KEY_WIDGET_LEFT = "pref_widget_left";
    public static final String KEY_WIDGET_RIGHT = "pref_widget_right";
    public static final String KEY_WIDGET_CENTER = "pref_widget_center";

    // Custom Interactive Layout Elements
    public static final int ELEM_LEFT_GAUGE = 0;
    public static final int ELEM_RIGHT_GAUGE = 1;
    public static final int ELEM_CENTER_BLOCK = 2;
    public static final int ELEM_WIDGET_LEFT = 3;
    public static final int ELEM_WIDGET_RIGHT = 4;
    public static final int ELEM_WIDGET_CENTER = 5;
    public static final int ELEM_MEDIA_PLAYER = 6;
    public static final int ELEM_TOP_HEADER = 7;
    public static final int ELEM_BOTTOM_FOOTER = 8;
    public static final int ELEM_COUNT = 9;

    public static final String[] ELEMENT_NAMES = {
            "Левый колодец",
            "Правый колодец",
            "Центр / Карта",
            "Левый виджет",
            "Правый виджет",
            "Нижний виджет",
            "Плеер музыки",
            "Верхняя строка",
            "Нижняя строка"
    };

    // Telemetry Mini-Widgets
    public static final int WIDGET_NONE = 0;           // Выключено
    public static final int WIDGET_FUEL = 1;           // Остаток топлива (л / %)
    public static final int WIDGET_RANGE = 2;          // Запас хода (км)
    public static final int WIDGET_BATTERY = 3;        // Напряжение АКБ (В)
    public static final int WIDGET_ODOMETER = 4;       // Общий пробег (Одометр)
    public static final int WIDGET_COOLANT = 5;        // Температура ОЖ (°C)
    public static final int WIDGET_OUTSIDE_TEMP = 6;   // Температура за бортом (°C)
    public static final int WIDGET_TIRE_PRESSURE = 7;  // Давление в шинах (бар)

    public static final String[] WIDGET_TITLES = {
            "Выключено",
            "Остаток топлива (л / %)",
            "Запас хода (км)",
            "Напряжение АКБ (В)",
            "Общий пробег (Одометр)",
            "Температура ОЖ (°C)",
            "Температура за бортом (°C)",
            "Давление в шинах (TPMS)"
    };

    public static final int THEME_SPORT_APEX = 0;       // Track / AMG / RS with 3D deep tube bezels & shift lights
    public static final int THEME_LUXURY_CHRONO = 1;    // Bentley / Tank 500 Luxury Chronograph with sub-dials & guilloche
    public static final int THEME_CYBER_HEX = 2;        // Futuristic polygonal HUD / EV Tron bars & perspective grid
    public static final int THEME_TACTICAL_4X4 = 3;     // Baja / Expedition with 3D pitch-roll artificial horizon & bolted bezels
    public static final int THEME_MINIMAL_OLED = 4;     // Scandinavian floating arc ribbons on deep obsidian black
    public static final int THEME_TAYCAN_ARC = 5;       // Porsche Taycan / Theme 51 dual glowing neon sweep arcs
    public static final int THEME_USER_THEME = 6;       // Custom community theme pack from /sdcard/gwm_split_user_themes/
    public static final int THEME_MINI_COOPER = 7;      // MINI Cooper F56 overlapping chrome twin-gauge with LCD & fuel ladder
    public static final int THEME_SPLIT_OFFSET = 100;   // Base offset for original Split themes (100 + id)

    public static final int[] SPLIT_THEME_IDS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
        20, 21, 22, 23, 24, 25, 27, 28, 29, 30, 31, 32, 33, 34, 35, 39, 40, 41
    };

    // Legacy aliases for backward compatibility
    public static final int THEME_SPORT_RED = THEME_SPORT_APEX;
    public static final int THEME_LUXURY_GOLD = THEME_LUXURY_CHRONO;
    public static final int THEME_CYBER_NEON = THEME_CYBER_HEX;
    public static final int THEME_OFFROAD_AMBER = THEME_TACTICAL_4X4;

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getTheme(Context context) {
        return getPrefs(context).getInt(KEY_THEME, THEME_TAYCAN_ARC);
    }

    public static void setTheme(Context context, int theme) {
        getPrefs(context).edit().putInt(KEY_THEME, theme).apply();
    }

    public static boolean isHolePunchEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_HOLE_PUNCH, false);
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

    public static int getWidgetLeft(Context context) {
        return getPrefs(context).getInt(KEY_WIDGET_LEFT, WIDGET_FUEL);
    }

    public static void setWidgetLeft(Context context, int widget) {
        getPrefs(context).edit().putInt(KEY_WIDGET_LEFT, widget).apply();
    }

    public static int getWidgetRight(Context context) {
        return getPrefs(context).getInt(KEY_WIDGET_RIGHT, WIDGET_RANGE);
    }

    public static void setWidgetRight(Context context, int widget) {
        getPrefs(context).edit().putInt(KEY_WIDGET_RIGHT, widget).apply();
    }

    public static int getWidgetCenter(Context context) {
        return getPrefs(context).getInt(KEY_WIDGET_CENTER, WIDGET_COOLANT);
    }

    public static void setWidgetCenter(Context context, int widget) {
        getPrefs(context).edit().putInt(KEY_WIDGET_CENTER, widget).apply();
    }

    public static float getElementDx(Context context, int elementId) {
        return getPrefs(context).getFloat("elem_" + elementId + "_dx", 0.0f);
    }

    public static float getElementDy(Context context, int elementId) {
        return getPrefs(context).getFloat("elem_" + elementId + "_dy", 0.0f);
    }

    public static float getElementScale(Context context, int elementId) {
        return getPrefs(context).getFloat("elem_" + elementId + "_scale", 1.0f);
    }

    public static float getElementRotation(Context context, int elementId) {
        return getPrefs(context).getFloat("elem_" + elementId + "_rot", 0.0f);
    }

    public static void setElementLayout(Context context, int elementId, float dx, float dy, float scale, float rot) {
        getPrefs(context).edit()
                .putFloat("elem_" + elementId + "_dx", dx)
                .putFloat("elem_" + elementId + "_dy", dy)
                .putFloat("elem_" + elementId + "_scale", scale)
                .putFloat("elem_" + elementId + "_rot", rot)
                .apply();
    }

    public static void setElementLayout(Context context, int elementId, float dx, float dy, float scale) {
        setElementLayout(context, elementId, dx, dy, scale, getElementRotation(context, elementId));
    }

    public static void resetElementLayout(Context context, int elementId) {
        setElementLayout(context, elementId, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    public static void resetAllLayouts(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        for (int i = 0; i < ELEM_COUNT; i++) {
            editor.putFloat("elem_" + i + "_dx", 0.0f)
                  .putFloat("elem_" + i + "_dy", 0.0f)
                  .putFloat("elem_" + i + "_scale", 1.0f)
                  .putFloat("elem_" + i + "_rot", 0.0f);
        }
        editor.apply();
    }
}
