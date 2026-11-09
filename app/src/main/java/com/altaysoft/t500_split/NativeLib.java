package com.altaysoft.t500_split;

import android.content.Context;
import android.util.Log;

public class NativeLib {
    private static final String TAG = "NativeLib";
    private static boolean isLoaded = false;
    private static String loadError = null;

    static {
        try {
            System.loadLibrary("c++_shared");
            try { System.loadLibrary("crypto"); } catch (Throwable t) { Log.w(TAG, "crypto: " + t.getMessage()); }
            try { System.loadLibrary("ssl"); } catch (Throwable t) { Log.w(TAG, "ssl: " + t.getMessage()); }
            try { System.loadLibrary("curl"); } catch (Throwable t) { Log.w(TAG, "curl: " + t.getMessage()); }
            System.loadLibrary("native-lib");
            isLoaded = true;
            Log.i(TAG, "libnative-lib.so loaded successfully!");
        } catch (Throwable t) {
            loadError = t.getMessage();
            Log.e(TAG, "Failed to load libnative-lib.so", t);
        }
    }

    public static boolean isLoaded() {
        return isLoaded;
    }

    public static String getLoadError() {
        return loadError;
    }

    public static native float approximate(float[][] a0, float a1, float a2, float a3);
    public static native float[] decodeTPMS(String a0);
    public static native byte[] decryptThemeImage(Context a0, byte[] a1);
    public static native float floatClamp(float a0, float a1, float a2);
    public static native String getJavaStringByIndex(int a0);
    public static native int getPrefIntByIndex(int a0);
    public static String getStringByIndex(int a0) { return null; }
    public static native void initCarData(carData a0);
    public static native void initCfgData(cfgData a0);
    public static native void nativeAckWellBack();
    public static native void nativeBindPanel(Context a0);
    public static native void nativeBindView(carData a0);
    public static native String[] nativeGetData(String[] a0, int a1);
    public static native void nativeInitReader(carData a0);
    public static native void nativeInitStrings(Context a0);
    public static native boolean nativeIsReaderRunning();
    public static native boolean nativeIsViewBound();
    public static native boolean nativePropBackgroundLightLvl(int a0);
    public static native boolean nativePropBrkPedFeel(int a0);
    public static native boolean nativePropDriveMode(int a0);
    public static native boolean nativePropDriveModeHut(int a0);
    public static native boolean nativePropEpsSteerMod(int a0);
    public static native boolean nativePropHvacAc(int a0);
    public static native boolean nativePropHvacAuto(int a0);
    public static native boolean nativePropHvacBlow(int a0);
    public static native boolean nativePropHvacDef(int a0);
    public static native boolean nativePropHvacFan(int a0);
    public static native boolean nativePropHvacHeatFl(int a0);
    public static native boolean nativePropHvacHeatFr(int a0);
    public static native boolean nativePropHvacHeatRl(int a0);
    public static native boolean nativePropHvacHeatRr(int a0);
    public static native boolean nativePropHvacInlet(int a0);
    public static native boolean nativePropHvacPwr(int a0);
    public static native boolean nativePropHvacRear(int a0);
    public static native boolean nativePropHvacTemp(float a0);
    public static native boolean nativePropHvacVentFl(int a0);
    public static native boolean nativePropHvacVentFr(int a0);
    public static native boolean nativePropHvacVentRl(int a0);
    public static native boolean nativePropHvacVentRr(int a0);
    public static native boolean nativePropHvacWheel(int a0);
    public static native boolean nativePropHvacWin(int a0);
    public static native boolean nativePropMainVol(int a0);
    public static native boolean nativePropMediaCue(Context a0, String a1, int a2, String a3);
    public static native void nativePropPanelClear();
    public static native void nativePropPanelFeed(String a0);
    public static native void nativeSetContext(Context a0);
    public static native boolean nativeSetVolMedia(int a0);
    public static native boolean nativeSetVolNavi(int a0);
    public static native boolean nativeSetVolPhone(int a0);
    public static native boolean nativeSetVolRing(int a0);
    public static native boolean nativeSetVolVoice(int a0);
    public static native void nativeStopReader();
    public static native void nativeUnbindPanel();
    public static native void nativeUnbindView();
}
