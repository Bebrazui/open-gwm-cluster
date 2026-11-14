package org.opengwm.split.theme;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.altaysoft.themecrypt.ThemeCrypt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ThemeAssetLoader {
    private static final String TAG = "ThemeAssetLoader";
    private static final Map<String, Bitmap> BITMAP_CACHE = new HashMap<>();

    public static Bitmap loadSplitThemeBitmap(Context context, int themeId, String component) {
        String assetPath = "split_themes/theme_" + themeId + "_" + component + ".png";
        if (BITMAP_CACHE.containsKey(assetPath)) {
            return BITMAP_CACHE.get(assetPath);
        }
        Bitmap bmp = loadDirectAsset(context, assetPath);
        if (bmp != null) {
            return bmp;
        }

        // Fallbacks without mutual recursion
        if ("needle_spd".equals(component) || "needle_rpm".equals(component)) {
            bmp = loadDirectAsset(context, "split_themes/theme_" + themeId + "_needle.png");
        } else if ("needle".equals(component)) {
            bmp = loadDirectAsset(context, "split_themes/theme_" + themeId + "_needle_spd.png");
        } else if ("needle_small_fuel".equals(component) || "needle_small_temp".equals(component)) {
            bmp = loadDirectAsset(context, "split_themes/theme_" + themeId + "_needle_small.png");
        }

        BITMAP_CACHE.put(assetPath, bmp);
        return bmp;
    }

    private static Bitmap loadDirectAsset(Context context, String assetPath) {
        if (BITMAP_CACHE.containsKey(assetPath)) {
            return BITMAP_CACHE.get(assetPath);
        }
        try {
            InputStream is = context.getAssets().open(assetPath);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            BITMAP_CACHE.put(assetPath, bmp);
            return bmp;
        } catch (Throwable ignored) {
            BITMAP_CACHE.put(assetPath, null);
            return null;
        }
    }

    public static Bitmap loadSplitAsset(Context context, String fileName) {
        return loadDirectAsset(context, "split_themes/" + fileName);
    }

    public static Context getSpoofedContext(Context base) {
        return new ContextWrapper(base.getApplicationContext()) {
            @Override
            public String getPackageName() {
                return "com.altaysoft.t500_split";
            }
            @Override
            public String getOpPackageName() {
                return "com.altaysoft.t500_split";
            }
        };
    }

    public static Bitmap loadDecryptedBitmap(Context context, String assetPath) {
        if (BITMAP_CACHE.containsKey(assetPath)) {
            return BITMAP_CACHE.get(assetPath);
        }

        try {
            InputStream is = context.getAssets().open(assetPath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int read;
            while ((read = is.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            is.close();

            byte[] encryptedData = baos.toByteArray();
            byte[] decrypted = null;

            if (ThemeCrypt.isLoaded()) {
                Context spoofed = getSpoofedContext(context);
                decrypted = ThemeCrypt.decrypt(spoofed, encryptedData);
            }

            if (decrypted == null || decrypted.length == 0) {
                Log.e(TAG, "Failed to decrypt " + assetPath);
                return null;
            }

            // Verify PNG magic
            boolean isPng = decrypted.length > 8 &&
                    (decrypted[0] & 0xFF) == 0x89 &&
                    decrypted[1] == 'P' &&
                    decrypted[2] == 'N' &&
                    decrypted[3] == 'G';

            Log.i(TAG, "Decrypted " + assetPath + " (" + decrypted.length + " bytes), isPng=" + isPng);

            Bitmap bmp = BitmapFactory.decodeByteArray(decrypted, 0, decrypted.length);
            if (bmp != null) {
                BITMAP_CACHE.put(assetPath, bmp);
                Log.i(TAG, "Decoded bitmap: " + bmp.getWidth() + "x" + bmp.getHeight());
            }
            return bmp;
        } catch (Throwable t) {
            Log.e(TAG, "Error loading decrypted asset: " + assetPath, t);
            return null;
        }
    }

    public static void dumpAllDecrypted(Context context) {
        new Thread(() -> {
            try {
                Context spoofed = getSpoofedContext(context);
                File outDir = new File(context.getExternalFilesDir(null), "decrypted_themes");
                if (!outDir.exists()) outDir.mkdirs();

                String[] subdirs = new String[]{"47", "48", "49", "50", "51", "clock", "offroad"};
                for (String dir : subdirs) {
                    try {
                        String[] files = context.getAssets().list("themes/" + dir);
                        if (files == null || files.length == 0) {
                            // Fallback hardcoded for 47 if list() empty
                            if ("47".equals(dir)) {
                                files = new String[]{"theme_47_needle.bin", "theme_47_needle_small.bin", "theme_47_rpm.bin", "theme_47_spd.bin"};
                            }
                        }
                        if (files == null) continue;

                        File subOut = new File(outDir, dir);
                        if (!subOut.exists()) subOut.mkdirs();

                        for (String f : files) {
                            if (!f.endsWith(".bin")) continue;
                            String assetPath = "themes/" + dir + "/" + f;
                            InputStream is = context.getAssets().open(assetPath);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
                            is.close();

                            byte[] dec = ThemeCrypt.decrypt(spoofed, baos.toByteArray());
                            if (dec != null && dec.length > 0) {
                                String outName = f.replace(".bin", ".png");
                                File outFile = new File(subOut, outName);
                                FileOutputStream fos = new FileOutputStream(outFile);
                                fos.write(dec);
                                fos.flush();
                                fos.close();
                                Log.i(TAG, "Exported: " + outFile.getAbsolutePath() + " (" + dec.length + " bytes)");
                            } else {
                                Log.e(TAG, "Decrypt returned null for " + assetPath);
                            }
                        }
                    } catch (Throwable e) {
                        Log.e(TAG, "Failed dir " + dir, e);
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Export failed", t);
            }
        }).start();
    }
}
