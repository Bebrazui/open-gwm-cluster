package org.opengwm.split.vehicle;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Method;

/**
 * Native Android Automotive OS telemetry provider.
 * Connects directly to the car's system `car_service` (android.car.ICar)
 * and reads VHAL properties (PERF_VEHICLE_SPEED, ENGINE_RPM, CURRENT_GEAR).
 */
public class AndroidAutomotiveVehicleProvider implements VehicleDataProvider {

    private static final String TAG = "AASVehicleProvider";

    // Standard Android Automotive VHAL Property IDs
    public static final int PROP_PERF_VEHICLE_SPEED = 0x11600207; // 291504647 (float m/s)
    public static final int PROP_ENGINE_RPM          = 0x11600305; // 291504901 (float RPM)
    public static final int PROP_CURRENT_GEAR        = 0x11400401; // 289408001 (int)
    public static final int PROP_GEAR_SELECTION      = 0x11400400; // 289408000 (int)

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Callback callback;
    private volatile boolean isRunning = false;
    private Thread pollThread;

    private int currentSpeed = 0;
    private int currentRpm = 0;
    private String currentGear = "P";
    private float currentTemp = 90.0f;

    @Override
    public void startListening(Callback callback) {
        this.callback = callback;
        this.isRunning = true;

        pollThread = new Thread(new Runnable() {
            @Override
            public void run() {
                readCarServiceLoop();
            }
        }, "AAS-CarService-Poll");
        pollThread.start();
    }

    private void readCarServiceLoop() {
        Object iCarProperty = null;
        Method getPropertyMethod = null;
        Method getValueMethod = null;

        while (isRunning && iCarProperty == null) {
            try {
                // 1. Get `car_service` binder via ServiceManager
                Class<?> smClass = Class.forName("android.os.ServiceManager");
                Method getServiceMethod = smClass.getMethod("getService", String.class);
                IBinder carServiceBinder = (IBinder) getServiceMethod.invoke(null, "car_service");

                if (carServiceBinder == null) {
                    Log.w(TAG, "car_service binder not available yet, retrying...");
                    Thread.sleep(1000);
                    continue;
                }

                // 2. ICar.Stub.asInterface(carServiceBinder)
                Class<?> iCarStubClass = Class.forName("android.car.ICar$Stub");
                Method asInterfaceMethod = iCarStubClass.getMethod("asInterface", IBinder.class);
                Object iCar = asInterfaceMethod.invoke(null, carServiceBinder);

                // 3. iCar.getCarService("property")
                Method getCarServiceMethod = iCar.getClass().getMethod("getCarService", String.class);
                IBinder propertyBinder = (IBinder) getCarServiceMethod.invoke(iCar, "property");

                // 4. ICarProperty.Stub.asInterface(propertyBinder)
                Class<?> iCarPropertyStubClass = Class.forName("android.car.hardware.property.ICarProperty$Stub");
                Method propertyAsInterface = iCarPropertyStubClass.getMethod("asInterface", IBinder.class);
                iCarProperty = propertyAsInterface.invoke(null, propertyBinder);

                // 5. getProperty(int propId, int areaId)
                getPropertyMethod = iCarProperty.getClass().getMethod("getProperty", int.class, int.class);

                Log.i(TAG, "Successfully connected to Android Automotive car_service / ICarProperty!");
            } catch (Exception e) {
                Log.e(TAG, "Failed to connect to car_service, will retry in 2s", e);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }

        // Fast poll loop (every 50ms = 20Hz for fluid gauge animation)
        while (isRunning && iCarProperty != null) {
            try {
                boolean changed = false;

                // Read Speed (m/s -> km/h)
                Object speedProp = getPropertyMethod.invoke(iCarProperty, PROP_PERF_VEHICLE_SPEED, 0);
                if (speedProp != null) {
                    if (getValueMethod == null) {
                        getValueMethod = speedProp.getClass().getMethod("getValue");
                    }
                    Object speedVal = getValueMethod.invoke(speedProp);
                    if (speedVal instanceof Number) {
                        float speedMs = ((Number) speedVal).floatValue();
                        currentSpeed = Math.max(0, Math.round(speedMs * 3.6f));
                        changed = true;
                    }
                }

                // Read Engine RPM
                Object rpmProp = getPropertyMethod.invoke(iCarProperty, PROP_ENGINE_RPM, 0);
                if (rpmProp != null) {
                    Object rpmVal = getValueMethod.invoke(rpmProp);
                    if (rpmVal instanceof Number) {
                        currentRpm = Math.max(0, Math.round(((Number) rpmVal).floatValue()));
                        changed = true;
                    }
                }

                // Read Current Gear
                Object gearProp = getPropertyMethod.invoke(iCarProperty, PROP_CURRENT_GEAR, 0);
                if (gearProp == null) {
                    gearProp = getPropertyMethod.invoke(iCarProperty, PROP_GEAR_SELECTION, 0);
                }
                if (gearProp != null) {
                    Object gearVal = getValueMethod.invoke(gearProp);
                    if (gearVal instanceof Number) {
                        int g = ((Number) gearVal).intValue();
                        currentGear = decodeGear(g);
                        changed = true;
                    }
                }

                if (changed) {
                    dispatchState();
                }

                Thread.sleep(50);
            } catch (Exception e) {
                Log.w(TAG, "Error polling car property: " + e.getMessage());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private String decodeGear(int g) {
        // Standard Android Automotive VehicleGear values:
        // 1 = NEUTRAL, 2 = REVERSE, 4 = PARK, 8 = DRIVE
        switch (g) {
            case 4:
                return "P";
            case 2:
                return "R";
            case 1:
                return "N";
            case 8:
                return "D";
            case 16: return "M1";
            case 32: return "M2";
            case 64: return "M3";
            case 128: return "M4";
            default:
                return (g > 0) ? "D" : "P";
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
        callback = null;
    }
}
