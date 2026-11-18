package com.altaysoft.themecrypt;

import android.content.Context;

/**
 * Clean-Room stub for ThemeCrypt.
 * All themes in open-gwm-split are pre-decrypted PNG assets located in split_themes/.
 * Proprietary libtheme_crypt.so has been removed.
 */
public final class ThemeCrypt {
    private static final String TAG = "ThemeCrypt";

    public static boolean isLoaded() {
        return false;
    }

    public static String lastError() {
        return "Clean-Room build: proprietary theme decrypter removed";
    }

    public static byte[] decrypt(Context context, byte[] data) {
        return null;
    }

    public static byte[] encrypt(Context context, byte[] data) {
        return null;
    }
}
