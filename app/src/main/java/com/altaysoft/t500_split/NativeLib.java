package com.altaysoft.t500_split;

import android.content.Context;
import android.util.Log;

public class NativeLib {
    private static final String TAG = "NativeLib";
    private static boolean isLoaded = false;
    private static String loadError = null;

    static {
        loadLibraries();
    }

    public static synchronized boolean loadLibraries() {
        if (isLoaded) return true;
        try {
            try { System.loadLibrary("c++_shared"); } catch (Throwable t) { Log.w(TAG, "c++_shared: " + t.getMessage()); }
            try { System.loadLibrary("crypto"); } catch (Throwable t) { Log.w(TAG, "crypto: " + t.getMessage()); }
            try { System.loadLibrary("ssl"); } catch (Throwable t) { Log.w(TAG, "ssl: " + t.getMessage()); }
            try { System.loadLibrary("curl"); } catch (Throwable t) { Log.w(TAG, "curl: " + t.getMessage()); }
            try { System.loadLibrary("as_ipc0"); } catch (Throwable t) { Log.w(TAG, "as_ipc0: " + t.getMessage()); }
            try { System.loadLibrary("as_ipc1"); } catch (Throwable t) { Log.w(TAG, "as_ipc1: " + t.getMessage()); }
            System.loadLibrary("native-lib");
            isLoaded = true;
            Log.i(TAG, "Native libraries and FDBus client loaded successfully");
            return true;
        } catch (Throwable t) {
            loadError = t.getMessage();
            Log.e(TAG, "Failed to load native-lib: " + t.getMessage(), t);
            return false;
        }
    }

    public static boolean isLibraryLoaded() {
        return isLoaded;
    }

    public static String getLoadError() {
        return loadError;
    }

    // Native JNI Methods registered by JNI_OnLoad:
    public static native float approximate(float[][] fArr, float f, float f2, float f3);
    public static native float[] decodeTPMS(String str);
    public static native byte[] decryptThemeImage(Context context, byte[] bArr);
    public static native float floatClamp(float f, float f2, float f3);
    public static native String getJavaStringByIndex(int i);
    public static native int getPrefIntByIndex(int i);

    public static String getStringByIndex(int i) {
        if (!isLoaded) return "";
        try {
            return getJavaStringByIndex(i);
        } catch (Throwable t) {
            return "";
        }
    }

    public static native void initCarData(carData data);
    public static native void initCfgData(cfgData cfg);
    public static native void nativeAckWellBack();
    public static native void nativeBindPanel(Context context);
    public static native void nativeBindView(carData data);
    public static native String[] nativeGetData(String[] strArr, int i);
    public static native void nativeInitReader(carData data);
    public static native void nativeInitStrings(Context context);
    public static native boolean nativeIsReaderRunning();
    public static native boolean nativeIsViewBound();
    public static native boolean nativePropBackgroundLightLvl(int i);
    public static native boolean nativePropBrkPedFeel(int i);
    public static native boolean nativePropDriveMode(int i);
    public static native boolean nativePropDriveModeHut(int i);
    public static native boolean nativePropEpsSteerMod(int i);
    public static native boolean nativePropHvacAc(int i);
    public static native boolean nativePropHvacAuto(int i);
    public static native boolean nativePropHvacBlow(int i);
    public static native boolean nativePropHvacDef(int i);
    public static native boolean nativePropHvacFan(int i);
    public static native boolean nativePropHvacHeatFl(int i);
    public static native boolean nativePropHvacHeatFr(int i);
    public static native boolean nativePropHvacHeatRl(int i);
    public static native boolean nativePropHvacHeatRr(int i);
    public static native boolean nativePropHvacInlet(int i);
    public static native boolean nativePropHvacPwr(int i);
    public static native boolean nativePropHvacRear(int i);
    public static native boolean nativePropHvacTemp(float f);
    public static native boolean nativePropHvacVentFl(int i);
    public static native boolean nativePropHvacVentFr(int i);
    public static native boolean nativePropHvacVentRl(int i);
    public static native boolean nativePropHvacVentRr(int i);
    public static native boolean nativePropHvacWheel(int i);
    public static native boolean nativePropHvacWin(int i);
    public static native boolean nativePropMainVol(int i);
    public static native boolean nativePropMediaCue(Context context, String str, int i, String str2);
    public static native void nativePropPanelClear();
    public static native void nativePropPanelFeed(String str);
    public static native void nativeSetContext(Context context);
    public static native boolean nativeSetVolMedia(int i);
    public static native boolean nativeSetVolNavi(int i);
    public static native boolean nativeSetVolPhone(int i);
    public static native boolean nativeSetVolRing(int i);
    public static native boolean nativeSetVolVoice(int i);
    public static native void nativeStopReader();
    public static native void nativeUnbindPanel();
    public static native void nativeUnbindView();
}
