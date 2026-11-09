package com.altaysoft.themecrypt;

import android.content.Context;
import android.util.Log;

public final class ThemeCrypt {
    private static final String TAG = "ThemeCrypt";
    private static boolean loaded = false;
    private static boolean loadFailed = false;
    private static String lastError = null;

    static {
        try {
            System.loadLibrary("c++_shared");
            System.loadLibrary("theme_crypt");
            loaded = true;
            Log.i(TAG, "libtheme_crypt.so loaded successfully");
        } catch (Throwable t) {
            loadFailed = true;
            lastError = t.getMessage();
            Log.e(TAG, "Failed to load libtheme_crypt.so", t);
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static String lastError() {
        return lastError;
    }

    public static native byte[] decrypt(Context context, byte[] data);
    public static native byte[] encrypt(Context context, byte[] data);
}
