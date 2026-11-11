package org.opengwm.split.vehicle;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import org.opengwm.split.util.AppLogger;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Multi-tiered telemetry provider for GWM Tank 500 / Haval running Android 9 Pie Automotive OS without root.
 *
 * Implements cascading fallbacks:
 * 1. Standard AAS Property (ICarProperty)
 * 2. Android 9 SensorManager (ICarSensor - avoids signature CAR_ENGINE_DETAILED restriction)
 * 3. GWM Vendor VHAL Properties (0x2... vendor group)
 * 4. Realistic Tank 500 9AT mathematical physics model (V6 3.0T E30Z)
 */
public class AndroidAutomotiveVehicleProvider implements VehicleDataProvider {

    private static final String TAG = "AASVehicleProvider";

    // Standard Android Automotive OS Property IDs
    public static final int PROP_PERF_VEHICLE_SPEED   = 0x11600207; // 291504647 (float m/s)
    public static final int PROP_ENGINE_RPM            = 0x11600305; // 291504901 (float RPM)
    public static final int PROP_CURRENT_GEAR          = 0x11400401; // 289408001 (int bitmask)
    public static final int PROP_GEAR_SELECTION        = 0x11400400; // 289408000 (int)
    public static final int PROP_ENGINE_COOLANT_TEMP   = 0x11600301; // 291504897 (float deg C)
    public static final int PROP_FUEL_LEVEL            = 0x11600205; // 291504645 (float L)
    public static final int PROP_RANGE_REMAINING       = 0x11600208; // 291504648 (float meters)
    public static final int PROP_PERF_ODOMETER          = 0x11600204; // 291504644 (float km)
    public static final int PROP_ENV_OUTSIDE_TEMP      = 0x11600703; // 291505923 (float deg C)

    // GWM / Nobo Vendor VHAL Property IDs (Vendor group 0x20000000 bypasses Google signature restrictions)
    public static final int PROP_GWM_ACTOR_SPEED       = 0x216116e4; // 560010980 (GWM_VehSpd Float[] on sa8155)
    public static final int PROP_GWM_ACTOR_RPM         = 0x2161169f; // 560010911 (GWM_EngSpd 0..8191.875 RPM Float[] on sa8155)
    public static final int PROP_GWM_ACCEL_PEDAL       = 0x216116b2; // 560010930 (GWM_AccelPedalPosn 0..100% on sa8155)
    public static final int PROP_GWM_CURRENT_GEAR      = 0x214016c9; // 557848265 (GWM_CURRENT_GEAR)
    public static final int PROP_GWM_DRV_GEAR_DISP     = 0x214016d7; // 557848279 (GWM_DrvAutoGearDisp cluster gear)
    public static final int PROP_GWM_ENG_COOLANT_TEMP  = 0x216016b3; // 559945395 (GWM_EngCoolantTemp -48..143.25 C)
    public static final int PROP_GWM_OUTSIDE_TEMP      = 0x21601786; // 559945606 (GWM_ACAmbTemp -40..86.5 C)
    public static final int PROP_GWM_FUEL_LEVEL        = 0x21401391; // 557847441 (GWM_IP_FuelLvlInfo 0..200)
    public static final int PROP_GWM_FUEL_CONSUMPTION  = 0x214016ab; // 557848235 (GWM_FuelConsumption)
    public static final int PROP_GWM_VENDOR_RPM        = 0x21600305; // 559940357 (Vendor mirrored RPM)
    public static final int PROP_GWM_VENDOR_SPEED      = 0x21600207; // 559940103 (Vendor mirrored Speed)

    // Android 9 CarSensorManager Sensor Types
    public static final int SENSOR_TYPE_RPM             = 2;
    public static final int SENSOR_TYPE_CAR_SPEED       = 3;

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Callback callback;
    private volatile boolean isRunning = false;
    private volatile boolean isBound = false;
    private Thread pollThread;

    // Reflected CarProperty Service
    private Object iCarProperty = null;
    private Method getPropertyMethod = null;
    private Method getValueMethod = null;

    // Reflected CarSensor Service (Android 9 native)
    private Object iCarSensor = null;
    private Method getLatestSensorEventMethod = null;
    private Field sensorFloatValuesField = null;

    private boolean speedWarned = false;
    private boolean rpmWarned = false;
    private boolean sensorWarned = false;
    private int telemetryLogTick = 0;

    private int currentSpeed = 0;
    private int currentRpm = 0;
    private String currentGear = "--";
    private float currentTemp = -999f;
    private String speedSource = "INIT";
    private String rpmSource = "INIT";

    // Telemetry mini-widgets data (-1 / -999 indicates no real sensor data yet)
    private float currentFuel = -1f;
    private int currentFuelPercent = -1;
    private int currentRange = -1;
    private float currentBatteryVoltage = -1f;
    private int currentOdometer = -1;
    private float currentOutsideTemp = -999f;
    private float currentTirePressure = -1f;

    public AndroidAutomotiveVehicleProvider() {
        this(null);
    }

    public AndroidAutomotiveVehicleProvider(Context context) {
        this.context = (context != null) ? context.getApplicationContext() : null;
    }

    private final ServiceConnection carServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AppLogger.s(TAG, "УСПЕШНО: CarService подключен через bindService! Binder=" + service);
            initFromBinder(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            AppLogger.w(TAG, "ВНИМАНИЕ: CarService отключен системой");
            iCarProperty = null;
            iCarSensor = null;
            getPropertyMethod = null;
            getLatestSensorEventMethod = null;
        }
    };

    private synchronized void initFromBinder(IBinder binder) {
        if (binder == null) return;
        try {
            AppLogger.i(TAG, "Инициализация ICar из binder: " + binder);
            // ICar.Stub.asInterface(binder)
            Class<?> iCarStubClass = Class.forName("android.car.ICar$Stub");
            Method asInterfaceMethod = iCarStubClass.getMethod("asInterface", IBinder.class);
            Object iCar = asInterfaceMethod.invoke(null, binder);

            Method getCarServiceMethod = iCar.getClass().getMethod("getCarService", String.class);

            // 1. Resolve ICarProperty
            try {
                IBinder propertyBinder = (IBinder) getCarServiceMethod.invoke(iCar, "property");
                if (propertyBinder != null) {
                    Class<?> iCarPropertyStubClass = Class.forName("android.car.hardware.property.ICarProperty$Stub");
                    Method propertyAsInterface = iCarPropertyStubClass.getMethod("asInterface", IBinder.class);
                    Object propObj = propertyAsInterface.invoke(null, propertyBinder);

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
                    AppLogger.s(TAG, "ICarProperty готов! getPropertyMethod=" + getPropertyMethod);
                } else {
                    AppLogger.w(TAG, "getCarService('property') вернул null");
                }
            } catch (Throwable t) {
                AppLogger.w(TAG, "Ошибка резолва ICarProperty: " + t.getMessage());
            }

            // 2. Resolve ICarSensor (Android 9 native sensor service - bypasses CAR_ENGINE_DETAILED signature check)
            try {
                IBinder sensorBinder = (IBinder) getCarServiceMethod.invoke(iCar, "sensor");
                if (sensorBinder != null) {
                    Class<?> iCarSensorStubClass = Class.forName("android.car.hardware.ICarSensor$Stub");
                    Method sensorAsInterface = iCarSensorStubClass.getMethod("asInterface", IBinder.class);
                    Object sensorObj = sensorAsInterface.invoke(null, sensorBinder);

                    for (Method m : sensorObj.getClass().getMethods()) {
                        if ("getLatestSensorEvent".equals(m.getName())) {
                            Class<?>[] params = m.getParameterTypes();
                            if (params.length == 1 && params[0] == int.class) {
                                this.getLatestSensorEventMethod = m;
                                break;
                            }
                        }
                    }
                    this.iCarSensor = sensorObj;
                    AppLogger.s(TAG, "ICarSensor готов! getLatestSensorEventMethod=" + getLatestSensorEventMethod);
                } else {
                    AppLogger.w(TAG, "getCarService('sensor') вернул null");
                }
            } catch (Throwable t) {
                AppLogger.w(TAG, "Резолв ICarSensor пропущен: " + t.getMessage());
            }

        } catch (Throwable t) {
            AppLogger.e(TAG, "Ошибка резолва Car служб из binder", t);
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
            AppLogger.w(TAG, "No Context provided, relying solely on ServiceManager fallback");
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.car", "com.android.car.CarService"));
            intent.setAction("android.car.ICar");
            isBound = context.bindService(intent, carServiceConnection, Context.BIND_AUTO_CREATE);
            AppLogger.i(TAG, "bindService: com.android.car.CarService результат = " + isBound);

            if (!isBound) {
                Intent intent2 = new Intent("android.car.ICar");
                intent2.setPackage("com.android.car");
                isBound = context.bindService(intent2, carServiceConnection, Context.BIND_AUTO_CREATE);
                AppLogger.i(TAG, "bindService: action+package com.android.car результат = " + isBound);
            }
        } catch (Throwable t) {
            AppLogger.e(TAG, "Исключение при bindCarService", t);
        }
    }

    private void pollLoop() {
        int smRetryCounter = 0;

        while (isRunning) {
            try {
                // If not yet bound, attempt direct ServiceManager lookup
                if ((iCarProperty == null && iCarSensor == null) && (smRetryCounter++ % 20 == 0)) {
                    try {
                        Class<?> smClass = Class.forName("android.os.ServiceManager");
                        Method getServiceMethod = smClass.getMethod("getService", String.class);
                        IBinder binder = (IBinder) getServiceMethod.invoke(null, "car_service");
                        if (binder != null) {
                            AppLogger.s(TAG, "ServiceManager вернул binder 'car_service' напрямую!");
                            initFromBinder(binder);
                        }
                    } catch (Throwable ignored) {}
                }

                if (iCarProperty != null || iCarSensor != null) {
                    boolean changed = false;

                    // =========================================================
                    // 1. SPEED RESOLUTION (Cascading Tiers)
                    // =========================================================
                    int newSpeed = -1;
                    String newSpeedSource = null;

                    // Tier 0: GWM Live Actor Speed (0x216116e4 - confirmed on sa8155 GwmDyn / GwmVehicleActor)
                    if (iCarProperty != null && getPropertyMethod != null) {
                        try {
                            Object actorSpeedProp = getPropertyMethod.invoke(iCarProperty, PROP_GWM_ACTOR_SPEED, 0);
                            Object val = extractValue(actorSpeedProp);
                            if (val instanceof Number) {
                                float speedVal = ((Number) val).floatValue();
                                newSpeed = Math.max(0, Math.round(speedVal));
                                newSpeedSource = "GWM_0x216116e4";
                            }
                        } catch (Throwable ignored) {}
                    }

                    // Tier 1: Standard AAOS Property (0x11600207)
                    if (newSpeed < 0 && iCarProperty != null && getPropertyMethod != null) {
                        try {
                            Object speedProp = getPropertyMethod.invoke(iCarProperty, PROP_PERF_VEHICLE_SPEED, 0);
                            Object speedVal = extractValue(speedProp);
                            if (speedVal instanceof Number) {
                                float speedMs = ((Number) speedVal).floatValue();
                                newSpeed = Math.max(0, Math.round(speedMs * 3.6f));
                                newSpeedSource = "AAS_Property";
                            }
                        } catch (Throwable t) {
                            if (!speedWarned) {
                                Log.w(TAG, "Speed read warning: " + (t.getCause() != null ? t.getCause() : t));
                                speedWarned = true;
                            }
                        }
                    }

                    // Tier 2: CarSensorManager (Android 9 Sensor 3)
                    if (newSpeed < 0 && iCarSensor != null && getLatestSensorEventMethod != null) {
                        try {
                            Object speedEvent = getLatestSensorEventMethod.invoke(iCarSensor, SENSOR_TYPE_CAR_SPEED);
                            float speedMs = extractSensorFloat(speedEvent);
                            if (speedMs >= 0) {
                                newSpeed = Math.max(0, Math.round(speedMs * 3.6f));
                                newSpeedSource = "SensorMgr";
                            }
                        } catch (Throwable ignored) {}
                    }

                    // Tier 3: GWM Vendor Speed Property (0x21600207)
                    if (newSpeed < 0 && iCarProperty != null && getPropertyMethod != null) {
                        try {
                            Object vendorSpeedProp = getPropertyMethod.invoke(iCarProperty, PROP_GWM_VENDOR_SPEED, 0);
                            Object val = extractValue(vendorSpeedProp);
                            if (val instanceof Number) {
                                float speedMs = ((Number) val).floatValue();
                                newSpeed = Math.max(0, Math.round(speedMs * 3.6f));
                                newSpeedSource = "VendorVHAL";
                            }
                        } catch (Throwable ignored) {}
                    }

                    if (newSpeed >= 0) {
                        if (newSpeed != currentSpeed || "INIT".equals(speedSource)) {
                            currentSpeed = newSpeed;
                            speedSource = (newSpeedSource != null) ? newSpeedSource : speedSource;
                            changed = true;
                        }
                    }

                    // =========================================================
                    // 2. RPM RESOLUTION (Cascading Tiers - Solves non-root CAR_ENGINE_DETAILED block)
                    // =========================================================
                    int newRpm = -1;
                    String newRpmSource = null;

                    // Tier 0: GWM Live Engine Speed (0x2161169f - GWM_EngSpd confirmed in dumpsys car_service)
                    if (iCarProperty != null && getPropertyMethod != null) {
                        try {
                            Object vendorRpmProp = getPropertyMethod.invoke(iCarProperty, PROP_GWM_ACTOR_RPM, 0);
                            Object val = extractValue(vendorRpmProp);
                            if (val instanceof Number && ((Number) val).floatValue() >= 0) {
                                newRpm = Math.round(((Number) val).floatValue());
                                newRpmSource = "GWM_0x2161169f";
                            }
                        } catch (Throwable ignored) {}
                    }

                    // Tier 1: CarSensorManager (Native Android 9 Pie - checks CAR_SPEED/CAR_ENGINE_PROBING, NOT signature CAR_ENGINE_DETAILED)
                    if (newRpm < 0 && iCarSensor != null && getLatestSensorEventMethod != null) {
                        try {
                            Object rpmEvent = getLatestSensorEventMethod.invoke(iCarSensor, SENSOR_TYPE_RPM);
                            float rawRpm = extractSensorFloat(rpmEvent);
                            if (rawRpm > 0) {
                                newRpm = Math.round(rawRpm);
                                newRpmSource = "SensorMgr";
                            }
                        } catch (Throwable t) {
                            if (!sensorWarned) {
                                Log.w(TAG, "CarSensorManager RPM error: " + t);
                                sensorWarned = true;
                            }
                        }
                    }

                    // Tier 2: Additional GWM Vendor Properties (0x21600305 - VENDOR group bypasses signature checks)
                    if (newRpm < 0 && iCarProperty != null && getPropertyMethod != null) {
                        try {
                            Object vendorRpmProp = getPropertyMethod.invoke(iCarProperty, PROP_GWM_VENDOR_RPM, 0);
                            Object val = extractValue(vendorRpmProp);
                            if (val instanceof Number && ((Number) val).floatValue() > 0) {
                                newRpm = Math.round(((Number) val).floatValue());
                                newRpmSource = "VendorVHAL";
                            }
                        } catch (Throwable ignored) {}
                    }

                    // Tier 3: Standard AAOS Property (0x11600305 - succeeds if OEM firmware unblocked permissions)
                    if (newRpm < 0 && iCarProperty != null && getPropertyMethod != null) {
                        try {
                            Object rpmProp = getPropertyMethod.invoke(iCarProperty, PROP_ENGINE_RPM, 0);
                            Object rpmVal = extractValue(rpmProp);
                            if (rpmVal instanceof Number) {
                                newRpm = Math.max(0, Math.round(((Number) rpmVal).floatValue()));
                                newRpmSource = "AAS_Property";
                            }
                        } catch (Throwable t) {
                            if (!rpmWarned) {
                                Log.w(TAG, "RPM read warning (expected on stock non-root AAOS): " + (t.getCause() != null ? t.getCause() : t));
                                rpmWarned = true;
                            }
                        }
                    }

                    // No fake simulation: if hardware / VHAL provided no RPM data, keep 0
                    if (newRpm < 0) {
                        newRpm = 0;
                        newRpmSource = "NONE";
                    }

                    if (newRpm >= 0) {
                        if (newRpm != currentRpm || "INIT".equals(rpmSource)) {
                            currentRpm = newRpm;
                            rpmSource = (newRpmSource != null) ? newRpmSource : rpmSource;
                            changed = true;
                        }
                    }

                    // =========================================================
                    // 3. GEAR RESOLUTION (Standard AAOS + GWM 9AT D1..D9 Subgear)
                    // =========================================================
                    if (iCarProperty != null && getPropertyMethod != null) {
                        try {
                            int stdGear = -1;
                            int gwmSubGear = 0;

                            // Standard AAOS Gear
                            Object gearProp = getPropertyMethod.invoke(iCarProperty, PROP_CURRENT_GEAR, 0);
                            if (gearProp == null) {
                                gearProp = getPropertyMethod.invoke(iCarProperty, PROP_GEAR_SELECTION, 0);
                            }
                            Object gearVal = extractValue(gearProp);
                            if (gearVal instanceof Number) {
                                stdGear = ((Number) gearVal).intValue();
                            }

                            // GWM 9AT Sub-Gear (1..9)
                            Object gwmGearProp = getPropertyMethod.invoke(iCarProperty, PROP_GWM_CURRENT_GEAR, 0);
                            Object gwmVal = extractValue(gwmGearProp);
                            if (gwmVal instanceof Number) {
                                gwmSubGear = ((Number) gwmVal).intValue();
                            }

                            String newGear = decodeCombinedGear(stdGear, gwmSubGear);
                            if (!newGear.equals(currentGear)) {
                                currentGear = newGear;
                                changed = true;
                            }
                        } catch (Throwable ignored) {}

                        // Read Coolant Temp (Tier 0: GWM_EngCoolantTemp, Tier 1: Standard PROP_ENGINE_COOLANT_TEMP)
                        try {
                            Object tempProp = getPropertyMethod.invoke(iCarProperty, PROP_GWM_ENG_COOLANT_TEMP, 0);
                            if (tempProp == null) {
                                tempProp = getPropertyMethod.invoke(iCarProperty, PROP_ENGINE_COOLANT_TEMP, 0);
                            }
                            Object tempVal = extractValue(tempProp);
                            if (tempVal instanceof Number) {
                                currentTemp = ((Number) tempVal).floatValue();
                                changed = true;
                            }
                        } catch (Throwable ignored) {}

                        // Read Fuel Level (Tier 0: GWM_IP_FuelLvlInfo, Tier 1: Standard PROP_FUEL_LEVEL)
                        try {
                            Object fuelProp = getPropertyMethod.invoke(iCarProperty, PROP_GWM_FUEL_LEVEL, 0);
                            if (fuelProp == null) {
                                fuelProp = getPropertyMethod.invoke(iCarProperty, PROP_FUEL_LEVEL, 0);
                            }
                            Object fuelVal = extractValue(fuelProp);
                            if (fuelVal instanceof Number) {
                                currentFuel = ((Number) fuelVal).floatValue();
                                currentFuelPercent = Math.min(100, Math.max(0, Math.round((currentFuel / 80.0f) * 100)));
                            }
                        } catch (Throwable ignored) {}

                        // Read Range Remaining
                        try {
                            Object rangeProp = getPropertyMethod.invoke(iCarProperty, PROP_RANGE_REMAINING, 0);
                            Object rangeVal = extractValue(rangeProp);
                            if (rangeVal instanceof Number) {
                                currentRange = Math.round(((Number) rangeVal).floatValue() / 1000f);
                            }
                        } catch (Throwable ignored) {}

                        // Read Odometer
                        try {
                            Object odoProp = getPropertyMethod.invoke(iCarProperty, PROP_PERF_ODOMETER, 0);
                            Object odoVal = extractValue(odoProp);
                            if (odoVal instanceof Number) {
                                currentOdometer = Math.round(((Number) odoVal).floatValue());
                            }
                        } catch (Throwable ignored) {}

                        // Read Outside Temp (Tier 0: GWM_ACAmbTemp, Tier 1: Standard PROP_ENV_OUTSIDE_TEMP)
                        try {
                            Object outTempProp = getPropertyMethod.invoke(iCarProperty, PROP_GWM_OUTSIDE_TEMP, 0);
                            if (outTempProp == null) {
                                outTempProp = getPropertyMethod.invoke(iCarProperty, PROP_ENV_OUTSIDE_TEMP, 0);
                            }
                            Object outTempVal = extractValue(outTempProp);
                            if (outTempVal instanceof Number) {
                                currentOutsideTemp = ((Number) outTempVal).floatValue();
                            }
                        } catch (Throwable ignored) {}
                    }

                    if (changed) {
                        dispatchState();
                    }

                    if (++telemetryLogTick % 40 == 0) {
                        Log.i(TAG, "Live Telemetry: Speed=" + currentSpeed + " km/h [" + speedSource + "] | RPM=" + currentRpm + " [" + rpmSource + "] | Gear=" + currentGear);
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

    private float extractSensorFloat(Object event) {
        if (event == null) return -1f;
        try {
            if (sensorFloatValuesField == null) {
                sensorFloatValuesField = event.getClass().getField("floatValues");
            }
            float[] vals = (float[]) sensorFloatValuesField.get(event);
            if (vals != null && vals.length > 0) {
                return vals[0];
            }
        } catch (Throwable t) {
            try {
                Field f = event.getClass().getDeclaredField("floatValues");
                f.setAccessible(true);
                float[] vals = (float[]) f.get(event);
                if (vals != null && vals.length > 0) return vals[0];
            } catch (Throwable ignored) {}
        }
        return -1f;
    }

    private Object extractValue(Object carPropVal) {
        if (carPropVal == null) return null;
        Object val = null;
        try {
            if (getValueMethod == null) {
                getValueMethod = carPropVal.getClass().getMethod("getValue");
            }
            val = getValueMethod.invoke(carPropVal);
        } catch (Throwable t) {
            try {
                Field f = carPropVal.getClass().getDeclaredField("mValue");
                f.setAccessible(true);
                val = f.get(carPropVal);
            } catch (Throwable ignored) {}
        }
        if (val == null) return null;
        if (val instanceof Object[]) {
            Object[] arr = (Object[]) val;
            if (arr.length > 0) return arr[0];
        } else if (val instanceof float[]) {
            float[] arr = (float[]) val;
            if (arr.length > 0) return arr[0];
        } else if (val instanceof int[]) {
            int[] arr = (int[]) val;
            if (arr.length > 0) return arr[0];
        }
        return val;
    }

    private String decodeCombinedGear(int stdGear, int gwmSubGear) {
        if (stdGear == 4) return "P";
        if (stdGear == 2) return "R";
        if (stdGear == 1) return "N";
        if (stdGear == 8 || stdGear > 0) {
            if (gwmSubGear >= 1 && gwmSubGear <= 9) {
                return "D" + gwmSubGear;
            }
            return "D";
        }
        if (gwmSubGear >= 1 && gwmSubGear <= 9) {
            return "D" + gwmSubGear;
        }
        return "--";
    }

    private void dispatchState() {
        final String diag = "SPD:" + currentSpeed + "[" + speedSource + "] RPM:" + currentRpm + "[" + rpmSource + "]";
        final VehicleState state = new VehicleState(
                currentSpeed, currentGear, currentRpm, true, currentTemp, diag,
                currentFuel, currentFuelPercent, currentRange, currentBatteryVoltage,
                currentOdometer, currentOutsideTemp, currentTirePressure
        );
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onVehicleStateChanged(state);
                }
            }
        });
    }

    public String getDiagnosticStatus() {
        return "SPD:" + currentSpeed + " [" + speedSource + "] | RPM:" + currentRpm + " [" + rpmSource + "] | GEAR:" + currentGear + " | TEMP:" + Math.round(currentTemp) + "C";
    }

    public boolean isServiceBound() {
        return isBound;
    }

    public boolean hasCarProperty() {
        return iCarProperty != null;
    }

    public boolean hasCarSensor() {
        return iCarSensor != null;
    }

    public static String getCan0Status() {
        File f = new File("/dev/can0");
        if (!f.exists()) return "Отсутствует в ядре (/dev/can0 не найден)";
        if (!f.canRead()) return "Найден, но нет прав чтения (нужен root/chmod 666)";
        return "Доступен для чтения (SocketCAN активен)";
    }

    public void probeAllProperties() {
        AppLogger.i(TAG, "=== ЗАПУСК ПРИНУДИТЕЛЬНОГО ЗОНДАЖА VHAL ===");
        AppLogger.i(TAG, "Статус: isBound=" + isBound + ", iCarProperty=" + (iCarProperty != null) + ", iCarSensor=" + (iCarSensor != null));

        if (iCarProperty == null) {
            AppLogger.w(TAG, "ICarProperty еще не подключен. Запрашиваем ServiceManager напрямую...");
            try {
                Class<?> smClass = Class.forName("android.os.ServiceManager");
                Method getServiceMethod = smClass.getMethod("getService", String.class);
                IBinder binder = (IBinder) getServiceMethod.invoke(null, "car_service");
                if (binder != null) {
                    AppLogger.s(TAG, "УСПЕХ: ServiceManager вернул binder для 'car_service'!");
                    initFromBinder(binder);
                } else {
                    AppLogger.e(TAG, "ServiceManager: binder 'car_service' = null (ОС не является Android Automotive или нет прав)");
                }
            } catch (Throwable t) {
                AppLogger.e(TAG, "Ошибка вызова ServiceManager: " + t.getMessage());
            }
        }

        if (iCarProperty == null && iCarSensor == null) {
            AppLogger.e(TAG, "ОТЧЕТ: Ни один автомобильный сервис (Property/Sensor) не доступен.");
            AppLogger.i(TAG, "Проверка SocketCAN: " + getCan0Status());
            return;
        }

        int[][] propsToTest = {
                {PROP_GWM_ACTOR_SPEED, 0},
                {PROP_PERF_VEHICLE_SPEED, 0},
                {PROP_GWM_VENDOR_SPEED, 0},
                {PROP_GWM_ACTOR_RPM, 0},
                {PROP_ENGINE_RPM, 0},
                {PROP_GWM_VENDOR_RPM, 0},
                {PROP_CURRENT_GEAR, 0},
                {PROP_GEAR_SELECTION, 0},
                {PROP_GWM_CURRENT_GEAR, 0},
                {PROP_GWM_ENG_COOLANT_TEMP, 0},
                {PROP_ENGINE_COOLANT_TEMP, 0},
                {PROP_GWM_FUEL_LEVEL, 0},
                {PROP_FUEL_LEVEL, 0},
                {PROP_GWM_OUTSIDE_TEMP, 0},
                {PROP_ENV_OUTSIDE_TEMP, 0},
                {PROP_PERF_ODOMETER, 0},
                {PROP_RANGE_REMAINING, 0}
        };

        String[] propNames = {
                "GWM_ACTOR_SPEED (0x216116e4)",
                "PERF_VEHICLE_SPEED (0x11600207)",
                "GWM_VENDOR_SPEED (0x21600207)",
                "GWM_ACTOR_RPM (0x2161169f)",
                "ENGINE_RPM (0x11600305)",
                "GWM_VENDOR_RPM (0x21600305)",
                "CURRENT_GEAR (0x11400401)",
                "GEAR_SELECTION (0x11400400)",
                "GWM_CURRENT_GEAR (0x214016c9)",
                "GWM_ENG_COOLANT (0x216016b3)",
                "ENGINE_COOLANT (0x11600301)",
                "GWM_FUEL_LEVEL (0x21401391)",
                "FUEL_LEVEL (0x11600205)",
                "GWM_OUTSIDE_TEMP (0x21601786)",
                "ENV_OUTSIDE_TEMP (0x11600703)",
                "PERF_ODOMETER (0x11600204)",
                "RANGE_REMAINING (0x11600208)"
        };

        if (iCarProperty != null && getPropertyMethod != null) {
            for (int i = 0; i < propsToTest.length; i++) {
                int propId = propsToTest[i][0];
                int areaId = propsToTest[i][1];
                String name = propNames[i];
                try {
                    Object valObj = getPropertyMethod.invoke(iCarProperty, propId, areaId);
                    if (valObj == null) {
                        AppLogger.w(TAG, name + " -> NULL");
                    } else {
                        Object val = extractValue(valObj);
                        AppLogger.s(TAG, name + " -> " + val);
                    }
                } catch (Throwable t) {
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    AppLogger.e(TAG, name + " -> " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                }
            }
        }

        if (iCarSensor != null && getLatestSensorEventMethod != null) {
            try {
                Object speedEvent = getLatestSensorEventMethod.invoke(iCarSensor, SENSOR_TYPE_CAR_SPEED);
                AppLogger.s(TAG, "CarSensor SPEED -> " + extractSensorFloat(speedEvent) + " m/s");
            } catch (Throwable t) {
                AppLogger.e(TAG, "CarSensor SPEED error: " + t.getMessage());
            }
            try {
                Object rpmEvent = getLatestSensorEventMethod.invoke(iCarSensor, SENSOR_TYPE_RPM);
                AppLogger.s(TAG, "CarSensor RPM -> " + extractSensorFloat(rpmEvent));
            } catch (Throwable t) {
                AppLogger.e(TAG, "CarSensor RPM error: " + t.getMessage());
            }
        }

        AppLogger.i(TAG, "=== ЗОНДАЖ VHAL ЗАВЕРШЕН ===");
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
        iCarSensor = null;
        getPropertyMethod = null;
        getLatestSensorEventMethod = null;
        callback = null;
    }
}

