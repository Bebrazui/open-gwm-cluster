package org.opengwm.split.vehicle;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Method;

/**
 * Native Android Automotive OS telemetry provider using bound CarService.
 * Avoids SELinux ServiceManager.getService() restrictions on non-system apps.
 */
public class AndroidAutomotiveVehicleProvider implements VehicleDataProvider {

    private static final String TAG = "AASVehicleProvider";

    public static final int PROP_PERF_VEHICLE_SPEED   = 0x11600207; // 291504647 (float m/s)
    public static final int PROP_ENGINE_RPM            = 0x11600305; // 291504901 (float RPM)
    public static final int PROP_CURRENT_GEAR          = 0x11400401; // 289408001 (int)
    public static final int PROP_GEAR_SELECTION        = 0x11400400; // 289408000 (int)
    public static final int PROP_ENGINE_COOLANT_TEMP   = 0x11600301; // 291504897 (float deg C)
    public static final int PROP_GWM_CURRENT_GEAR      = 0x214016c9; // 557848265 (GWM custom gear)

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Callback callback;
    private volatile boolean isRunning = false;
    private volatile boolean isBound = false;
    private Thread pollThread;

    private Object iCarProperty = null;
    private Method getPropertyMethod = null;
    private Method getValueMethod = null;

    private boolean speedWarned = false;
    private boolean rpmWarned = false;

    private int currentSpeed = 0;
    private int currentRpm = 0;
    private String currentGear = "P";
    private float currentTemp = 90.0f;

    public AndroidAutomotiveVehicleProvider() {
        this(null);
    }

    public AndroidAutomotiveVehicleProvider(Context context) {
        this.context = (context != null) ? context.getApplicationContext() : null;
    }

    private final ServiceConnection carServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Connected to CarService via bound Intent! Binder=" + service);
            initFromBinder(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "CarService disconnected");
            iCarProperty = null;
            getPropertyMethod = null;
        }
    };

    private synchronized void initFromBinder(IBinder binder) {
        if (binder == null) return;
        try {
            // ICar.Stub.asInterface(binder)
            Class<?> iCarStubClass = Class.forName("android.car.ICar$Stub");
            Method asInterfaceMethod = iCarStubClass.getMethod("asInterface", IBinder.class);
            Object iCar = asInterfaceMethod.invoke(null, binder);

            // iCar.getCarService("property")
            Method getCarServiceMethod = iCar.getClass().getMethod("getCarService", String.class);
            IBinder propertyBinder = (IBinder) getCarServiceMethod.invoke(iCar, "property");

            // ICarProperty.Stub.asInterface(propertyBinder)
            Class<?> iCarPropertyStubClass = Class.forName("android.car.hardware.property.ICarProperty$Stub");
            Method propertyAsInterface = iCarPropertyStubClass.getMethod("asInterface", IBinder.class);
            Object propObj = propertyAsInterface.invoke(null, propertyBinder);

            // Find getProperty(int, int)
            for (Method m : propObj.getClass().getMethods()) {
                if ("getProperty".equals(m.getName())) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 2 && params[0] == int.class && params[1] == int.class) {
                        this.getPropertyMethod = m;
                        break;
                    }
                }
            }
            this.iCarProperty = propObj;
            Log.i(TAG, "ICarProperty interface ready for live polling! getPropertyMethod=" + getPropertyMethod);
        } catch (Throwable t) {
            Log.e(TAG, "Error resolving ICarProperty from binder", t);
        }
    }

    @Override
    public void startListening(Callback callback) {
        this.callback = callback;
        this.isRunning = true;

        bindCarService();

        pollThread = new Thread(new Runnable() {
            @Override
            public void run() {
                pollLoop();
            }
        }, "AAS-CarService-Poll");
        pollThread.start();
    }

    private void bindCarService() {
        if (context == null) {
            Log.w(TAG, "No Context provided, relying solely on ServiceManager fallback");
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.car", "com.android.car.CarService"));
            intent.setAction("android.car.ICar");
            isBound = context.bindService(intent, carServiceConnection, Context.BIND_AUTO_CREATE);
            Log.i(TAG, "bindService explicit component result = " + isBound);

            if (!isBound) {
                Intent intent2 = new Intent("android.car.ICar");
                intent2.setPackage("com.android.car");
                isBound = context.bindService(intent2, carServiceConnection, Context.BIND_AUTO_CREATE);
                Log.i(TAG, "bindService action+package result = " + isBound);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Exception during bindCarService", t);
        }
    }

    private void pollLoop() {
        int smRetryCounter = 0;

        while (isRunning) {
            try {
                // If not yet bound, attempt direct ServiceManager lookup
                if (iCarProperty == null && (smRetryCounter++ % 20 == 0)) {
                    try {
                        Class<?> smClass = Class.forName("android.os.ServiceManager");
                        Method getServiceMethod = smClass.getMethod("getService", String.class);
                        IBinder binder = (IBinder) getServiceMethod.invoke(null, "car_service");
                        if (binder != null) {
                            initFromBinder(binder);
                        }
                    } catch (Throwable ignored) {}
                }

                if (iCarProperty != null && getPropertyMethod != null) {
                    boolean changed = false;

                    // Read Speed
                    try {
                        Object speedProp = getPropertyMethod.invoke(iCarProperty, PROP_PERF_VEHICLE_SPEED, 0);
                        Object speedVal = extractValue(speedProp);
                        if (speedVal instanceof Number) {
                            float speedMs = ((Number) speedVal).floatValue();
                            int newSpeed = Math.max(0, Math.round(speedMs * 3.6f));
                            if (newSpeed != currentSpeed) {
                                currentSpeed = newSpeed;
                                changed = true;
                            }
                        }
                    } catch (Throwable t) {
                        if (!speedWarned) {
                            Log.w(TAG, "Speed read warning: " + (t.getCause() != null ? t.getCause() : t));
                            speedWarned = true;
                        }
                    }

                    // Read Engine RPM
                    try {
                        Object rpmProp = getPropertyMethod.invoke(iCarProperty, PROP_ENGINE_RPM, 0);
                        Object rpmVal = extractValue(rpmProp);
                        if (rpmVal instanceof Number) {
                            int newRpm = Math.max(0, Math.round(((Number) rpmVal).floatValue()));
                            if (newRpm != currentRpm) {
                                currentRpm = newRpm;
                                changed = true;
                            }
                        }
                    } catch (Throwable t) {
                        if (!rpmWarned) {
                            Log.w(TAG, "RPM read warning: " + (t.getCause() != null ? t.getCause() : t));
                            rpmWarned = true;
                        }
                    }

                    // Read Current Gear
                    try {
                        Object gearProp = getPropertyMethod.invoke(iCarProperty, PROP_CURRENT_GEAR, 0);
                        if (gearProp == null) {
                            gearProp = getPropertyMethod.invoke(iCarProperty, PROP_GEAR_SELECTION, 0);
                        }
                        if (gearProp == null) {
                            gearProp = getPropertyMethod.invoke(iCarProperty, PROP_GWM_CURRENT_GEAR, 0);
                        }
                        Object gearVal = extractValue(gearProp);
                        if (gearVal instanceof Number) {
                            String newGear = decodeGear(((Number) gearVal).intValue());
                            if (!newGear.equals(currentGear)) {
                                currentGear = newGear;
                                changed = true;
                            }
                        }
                    } catch (Throwable ignored) {}

                    // Read Coolant Temp
                    try {
                        Object tempProp = getPropertyMethod.invoke(iCarProperty, PROP_ENGINE_COOLANT_TEMP, 0);
                        Object tempVal = extractValue(tempProp);
                        if (tempVal instanceof Number) {
                            currentTemp = ((Number) tempVal).floatValue();
                            changed = true;
                        }
                    } catch (Throwable ignored) {}

                    if (changed) {
                        dispatchState();
                    }
                }

                Thread.sleep(50);
            } catch (InterruptedException ie) {
                break;
            } catch (Throwable e) {
                Log.w(TAG, "Poll error: " + e.getMessage());
            }
        }
    }

    private Object extractValue(Object carPropVal) {
        if (carPropVal == null) return null;
        try {
            if (getValueMethod == null) {
                getValueMethod = carPropVal.getClass().getMethod("getValue");
            }
            return getValueMethod.invoke(carPropVal);
        } catch (Throwable t) {
            try {
                java.lang.reflect.Field f = carPropVal.getClass().getDeclaredField("mValue");
                f.setAccessible(true);
                return f.get(carPropVal);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private String decodeGear(int g) {
        switch (g) {
            case 4: return "P";
            case 2: return "R";
            case 1: return "N";
            case 8: return "D";
            case 16: return "M1";
            case 32: return "M2";
            case 64: return "M3";
            case 128: return "M4";
            default: return (g > 0) ? "D" : "P";
        }
    }

    private void dispatchState() {
        final VehicleState state = new VehicleState(currentSpeed, currentGear, currentRpm, true, currentTemp);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onVehicleStateChanged(state);
                }
            }
        });
    }

    @Override
    public void stopListening() {
        isRunning = false;
        if (pollThread != null) {
            pollThread.interrupt();
            pollThread = null;
        }
        if (context != null && isBound) {
            try {
                context.unbindService(carServiceConnection);
            } catch (Throwable ignored) {}
            isBound = false;
        }
        iCarProperty = null;
        getPropertyMethod = null;
        callback = null;
    }
}
