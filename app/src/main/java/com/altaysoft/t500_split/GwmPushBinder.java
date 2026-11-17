package com.altaysoft.t500_split;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import org.opengwm.split.util.AppLogger;

/**
 * IPC Push Binder Callback for GWM Adapter Service (gwm_adapter).
 * 
 * In GWM Tank 500, com.gwm.android.adapter.server pushes real-time CAN bus telemetry
 * and vehicle events to registered client apps by calling the binder callback.
 * 
 * libnative-lib.so (0xb5e8c) dynamically registers the native JNI implementation of:
 *   public native void onPush(String topic, String payload);
 * pointing to function 0xadf14.
 */
public class GwmPushBinder extends Binder implements IInterface {
    private static final String TAG = "GwmPushBinder";
    public static final String DESCRIPTOR = "com.altaysoft.t500_split.GwmPushBinder";
    public static final int TRANSACTION_ON_PUSH = IBinder.FIRST_CALL_TRANSACTION; // 1

    private static volatile GwmPushBinder sInstance = null;

    public static GwmPushBinder getInstance() {
        return sInstance;
    }

    public GwmPushBinder() {
        super();
        sInstance = this;
        try {
            this.attachInterface(this, DESCRIPTOR);
        } catch (Throwable t) {
            AppLogger.w(TAG, "attachInterface: " + t.getMessage());
        }
        AppLogger.i(TAG, "GwmPushBinder instance created successfully");
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    /**
     * Native implementation registered by libnative-lib.so via JNI RegisterNatives (at 0xadf14).
     * When vehicle signals arrive from gwm_adapter, this native function parses the CAN signals
     * and directly updates carData (speed, RPM, gear, coolant temp, etc.).
     */
    public native void onPush(String topic, String payload);

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == IBinder.INTERFACE_TRANSACTION) {
            if (reply != null) {
                reply.writeString(DESCRIPTOR);
            }
            return true;
        }

        String topic = null;
        String payload = null;

        if (data != null) {
            int originalPos = data.dataPosition();
            try {
                // Attempt standard read
                String first = data.readString();
                if (first != null && (first.contains(".") || first.contains("Gwm") || first.contains("Binder") || first.contains("Service"))) {
                    // It was an interface token header, read topic and payload
                    topic = data.readString();
                    payload = data.readString();
                } else {
                    topic = first;
                    payload = data.readString();
                }
            } catch (Throwable t) {
                try {
                    // Fallback: reset and try without token
                    data.setDataPosition(originalPos);
                    topic = data.readString();
                    payload = data.readString();
                } catch (Throwable t2) {
                    AppLogger.w(TAG, "Error parsing Parcel in onTransact: " + t2.getMessage());
                }
            }
        }

        AppLogger.d(TAG, "onTransact code=" + code + " topic=" + topic + " payload=" + (payload != null ? (payload.length() > 60 ? payload.substring(0, 60) + "..." : payload) : "null"));

        if (topic != null) {
            try {
                org.opengwm.split.vehicle.TrafficRecorderReplay.record(topic, payload);
            } catch (Throwable ignored) {}
            try {
                onPush(topic, payload != null ? payload : "");
            } catch (Throwable t) {
                AppLogger.e(TAG, "Exception in native onPush(" + topic + ")", t);
            }
        }

        if (reply != null) {
            try {
                reply.writeNoException();
            } catch (Throwable ignored) {}
        }
        return true;
    }
}
