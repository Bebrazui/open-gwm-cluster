package com.altaysoft.t500_split;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import org.opengwm.split.util.AppLogger;

/**
 * IPC Push Binder Callback & Telemetry Engine for GWM Adapter Service (gwm_adapter).
 * 
 * In GWM Tank 500, com.gwm.android.adapter.server pushes real-time CAN bus telemetry
 * (speed, RPM, transmission gear, coolant temp, pitch/roll, compass) by calling
 * callback.transact(1, data, reply, 0).
 */
public class GwmPushBinder extends Binder implements IInterface {
    private static final String TAG = "GwmPushBinder";
    public static final String DESCRIPTOR = "com.altaysoft.t500_split.GwmPushBinder";
    public static final String ADAPTER_DESCRIPTOR = "com.gwm.android.adapter.IGwmAdapterService";
    public static final int TRANSACTION_ON_PUSH = IBinder.FIRST_CALL_TRANSACTION; // 1
    public static final int TRANSACTION_REGISTER_CALLBACK = 3;
    public static final int TRANSACTION_UNREGISTER_CALLBACK = 4;

    private static volatile GwmPushBinder sInstance = null;

    public static synchronized GwmPushBinder getInstance() {
        if (sInstance == null) {
            sInstance = new GwmPushBinder();
        }
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
     * Connects to GWM Adapter Service in ServiceManager and registers this callback.
     */
    public static synchronized boolean registerToAdapter(Context context) {
        if (context == null) return false;
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            java.lang.reflect.Method getService = smClass.getMethod("getService", String.class);
            IBinder adapter = (IBinder) getService.invoke(null, "gwm_adapter");
            if (adapter == null) {
                AppLogger.w(TAG, "gwm_adapter не найден в ServiceManager");
                return false;
            }

            GwmPushBinder binder = getInstance();
            String currentPkg = context.getPackageName();

            // Attempt 1: register with current app package name
            boolean ok = performRegistration(adapter, currentPkg, binder);
            if (ok) {
                AppLogger.s(TAG, "УСПЕХ: gwm_adapter зарегистрирован для пакета " + currentPkg);
                return true;
            }

            // Attempt 2: fallback to legacy package name
            if (!"com.altaysoft.t500_split".equals(currentPkg)) {
                ok = performRegistration(adapter, "com.altaysoft.t500_split", binder);
                if (ok) {
                    AppLogger.s(TAG, "УСПЕХ: gwm_adapter зарегистрирован для legacy пакета com.altaysoft.t500_split");
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            AppLogger.e(TAG, "Ошибка регистрации в gwm_adapter: " + t.getMessage(), t);
            return false;
        }
    }

    private static boolean performRegistration(IBinder adapter, String pkgName, IBinder binder) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ADAPTER_DESCRIPTOR);
            data.writeString(pkgName);
            data.writeStringArray(new String[0]); // empty array = wildcard subscribe to all signals
            data.writeStrongBinder(binder);
            boolean res = adapter.transact(TRANSACTION_REGISTER_CALLBACK, data, reply, 0);
            reply.readException();
            return res;
        } catch (Throwable t) {
            AppLogger.w(TAG, "transact(3) с пакетом " + pkgName + " вернул ошибку: " + t.getMessage());
            return false;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    /**
     * Unregisters callback from gwm_adapter.
     */
    public static synchronized void unregisterFromAdapter(Context context) {
        if (context == null) return;
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            java.lang.reflect.Method getService = smClass.getMethod("getService", String.class);
            IBinder adapter = (IBinder) getService.invoke(null, "gwm_adapter");
            if (adapter == null) return;

            GwmPushBinder binder = getInstance();
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(ADAPTER_DESCRIPTOR);
                data.writeString(context.getPackageName());
                data.writeStrongBinder(binder);
                adapter.transact(TRANSACTION_UNREGISTER_CALLBACK, data, reply, 0);
                reply.readException();
                AppLogger.i(TAG, "gwm_adapter callback успешно разрегистрирован");
            } finally {
                data.recycle();
                reply.recycle();
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Direct Java telemetry parser and dispatcher for live GWM Tank 500 signals.
     */
    public void handlePush(String topic, String payload) {
        if (topic == null || payload == null) return;
        carData cd = carData.getInstance();
        String clean = payload.trim();

        // Strip JSON packaging if present (e.g. {"speed": 60.0})
        if (clean.startsWith("{") && clean.endsWith("}")) {
            int colon = clean.indexOf(':');
            if (colon >= 0) {
                clean = clean.substring(colon + 1, clean.length() - 1).replace("\"", "").trim();
            }
        }

        try {
            if ("car.basic.vehicle_speed".equals(topic) || "car.speed".equals(topic) || "speed".equalsIgnoreCase(topic)) {
                float spd = Float.parseFloat(clean);
                cd.setSpeed(spd);
            } else if ("car.basic.engine_speed".equals(topic) || "car.engine.rpm".equals(topic) || "rpm".equalsIgnoreCase(topic)) {
                float rpm = Float.parseFloat(clean);
                cd.setRpm(rpm);
            } else if ("car.basic.gear_status".equals(topic) || "car.transmission.gear".equals(topic) || "gear".equalsIgnoreCase(topic)) {
                if (clean.startsWith("D") || clean.startsWith("d")) {
                    try {
                        int g = Integer.parseInt(clean.substring(1));
                        cd.setGearStatus(4); // D
                        cd.setCurrentGear(g);
                        cd.setCustomGear("D" + g);
                    } catch (Exception e) {
                        cd.setGearStatus(4);
                        cd.setCustomGear("D");
                    }
                } else if (clean.equalsIgnoreCase("P")) {
                    cd.setGearStatus(1);
                    cd.setCurrentGear(0);
                    cd.setCustomGear("P");
                } else if (clean.equalsIgnoreCase("R")) {
                    cd.setGearStatus(2);
                    cd.setCurrentGear(0);
                    cd.setCustomGear("R");
                } else if (clean.equalsIgnoreCase("N")) {
                    cd.setGearStatus(3);
                    cd.setCurrentGear(0);
                    cd.setCustomGear("N");
                } else {
                    int rawGear = Integer.parseInt(clean);
                    // Map Tank 500 raw adapter gear codes:
                    // 4 = P (Park)
                    // 2 = R (Reverse)
                    // 0 = N (Neutral)
                    // 3 = D (Drive)
                    switch (rawGear) {
                        case 4:
                            cd.setGearStatus(1);
                            cd.setCurrentGear(0);
                            cd.setCustomGear("P");
                            break;
                        case 2:
                            cd.setGearStatus(2);
                            cd.setCurrentGear(0);
                            cd.setCustomGear("R");
                            break;
                        case 0:
                            cd.setGearStatus(3);
                            cd.setCurrentGear(0);
                            cd.setCustomGear("N");
                            break;
                        case 3:
                            cd.setGearStatus(4);
                            cd.setCurrentGear(1);
                            cd.setCustomGear("D");
                            break;
                        default:
                            cd.setGearStatus(rawGear);
                            cd.setCurrentGear(rawGear);
                            break;
                    }
                }
            } else if ("car.basic.battery_voltage".equals(topic) || "battery_voltage".equals(topic) || "batt".equalsIgnoreCase(topic)) {
                float v = Float.parseFloat(clean);
                cd.setBatteryVoltage(v);
            } else if ("car.basic.coolant_temp".equals(topic) || "car.engine.coolant_temp".equals(topic) || "coolant_temp".equals(topic)) {
                float t = Float.parseFloat(clean);
                cd.setCoolantT(t);
            } else if ("car.basic.slope.status".equals(topic) || topic.contains("slope")) {
                float pitch = parseMatrixFirst(clean);
                cd.setDrPitch(pitch);
            } else if ("car.basic.title.status".equals(topic) || topic.contains("title") || topic.contains("roll")) {
                float roll = parseMatrixFirst(clean);
                cd.setDrRoll(roll);
            } else if ("car.basic.compass.status".equals(topic) || topic.contains("compass")) {
                float heading = parseMatrixFirst(clean);
                cd.setDrHeading(heading);
            }
        } catch (Throwable t) {
            AppLogger.w(TAG, "Ошибка разбора сигнала topic=" + topic + " val=" + clean + ": " + t.getMessage());
        }
    }

    private static float parseMatrixFirst(String raw) {
        if (raw == null || raw.isEmpty()) return 0f;
        String s = raw.replace("[", "").replace("]", "").replace(" ", "").trim();
        if (s.isEmpty()) return 0f;
        String[] parts = s.split(",");
        try {
            return Float.parseFloat(parts[0]);
        } catch (Throwable t) {
            return 0f;
        }
    }

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
                String first = data.readString();
                if (first != null && (first.contains(".") || first.contains("Gwm") || first.contains("Binder") || first.contains("Service"))) {
                    topic = data.readString();
                    payload = data.readString();
                } else {
                    topic = first;
                    payload = data.readString();
                }
            } catch (Throwable t) {
                try {
                    data.setDataPosition(originalPos);
                    topic = data.readString();
                    payload = data.readString();
                } catch (Throwable t2) {
                    AppLogger.w(TAG, "Error parsing Parcel in onTransact: " + t2.getMessage());
                }
            }
        }

        if (topic != null) {
            try {
                org.opengwm.split.vehicle.TrafficRecorderReplay.record(topic, payload);
            } catch (Throwable ignored) {}

            // Process directly in Java clean-room telemetry parser
            handlePush(topic, payload != null ? payload : "");
        }

        if (reply != null) {
            try {
                reply.writeNoException();
            } catch (Throwable ignored) {}
        }
        return true;
    }
}
