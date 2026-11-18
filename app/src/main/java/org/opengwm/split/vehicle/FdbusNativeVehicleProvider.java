package org.opengwm.split.vehicle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import androidx.lifecycle.LiveData;
import com.altaysoft.t500_split.carData;
import com.altaysoft.t500_split.cfgData;
import org.opengwm.split.util.AppLogger;

/**
 * High-speed CAN Bus telemetry provider using GWM's native FDBus IPC broker (GA-Server).
 * Bypasses restricted Android Automotive VHAL permissions (no root required)
 * and streams speed, RPM, transmission gear, and temperatures with sub-millisecond latency.
 */
public class FdbusNativeVehicleProvider implements VehicleDataProvider {

    private static final String TAG = "FdbusNativeProvider";

    private static final Object INIT_LOCK = new Object();
    private static volatile boolean sNativeInitialized = false;
    private static final java.util.concurrent.CopyOnWriteArrayList<Callback> sCallbacks = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    private final Context context;
    private Callback callback;
    private volatile boolean isRunning = false;
    private carData carDataInstance;
    private cfgData cfgDataInstance;
    private android.content.BroadcastReceiver adapterStartupReceiver = null;

    public static boolean isAvailable() {
        return true;
    }

    public FdbusNativeVehicleProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public synchronized void startListening(Callback callback) {
        this.callback = callback;
        if (callback != null && !sCallbacks.contains(callback)) {
            sCallbacks.add(callback);
        }
        if (isRunning) {
            return;
        }
        isRunning = true;

        AppLogger.i(TAG, "Инициализация Clean-Room Java GwmAdapter клиента для GWM Tank 500...");

        // Connect directly to GWM Adapter Service in ServiceManager
        com.altaysoft.t500_split.GwmPushBinder.registerToAdapter(context);

        // Listen for GWM Adapter restarts or ACC_ON events
        if (adapterStartupReceiver == null) {
            adapterStartupReceiver = new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, android.content.Intent intent) {
                    AppLogger.i(TAG, "GWM Adapter событие получено: " + (intent != null ? intent.getAction() : "null"));
                    com.altaysoft.t500_split.GwmPushBinder.registerToAdapter(context);
                }
            };
            android.content.IntentFilter filter = new android.content.IntentFilter();
            filter.addAction("com.gwm.intent.action.ADAPTER_SERVICE_START");
            filter.addAction("com.gwm.intent.action.ACC_ON");
            filter.addAction("com.beantechs.adapterservice.startup");
            try {
                context.registerReceiver(adapterStartupReceiver, filter);
            } catch (Throwable t) {
                AppLogger.w(TAG, "registerReceiver adapterStartup: " + t.getMessage());
            }
        }

        carDataInstance = carData.getInstance();
        cfgDataInstance = cfgData.getInstance();

        // Configure cfgData for Tank 500 (Vehicle ID = 5, CAN streaming = true)
        cfgDataInstance.L0 = 5;
        cfgDataInstance.j1 = true;
        cfgDataInstance.n = "com.altaysoft.t500_split";

        // Listen for carData telemetry dispatch
        carDataInstance.addListener(new carData.TelemetryListener() {
            @Override
            public void onTelemetryUpdate(final VehicleState state) {
                notifyCallbacks(state);
            }
        });

        // Direct LiveData listeners for speed, RPM, gear, coolant in case of direct posts
        try {
            carDataInstance.T.setChangeListener(new LiveData.ValueChangeListener<Object>() {
                @Override
                public void onChanged(Object val) {
                    if (val instanceof Number) {
                        carDataInstance.speed = ((Number) val).floatValue();
                        carDataInstance.dispatchTelemetry();
                    }
                }
            });
            carDataInstance.U.setChangeListener(new LiveData.ValueChangeListener<Object>() {
                @Override
                public void onChanged(Object val) {
                    if (val instanceof Number) {
                        carDataInstance.rpm = ((Number) val).floatValue();
                        carDataInstance.dispatchTelemetry();
                    }
                }
            });
            carDataInstance.Y.setChangeListener(new LiveData.ValueChangeListener<Object>() {
                @Override
                public void onChanged(Object val) {
                    if (val instanceof Number) {
                        carDataInstance.currentGear = ((Number) val).intValue();
                        carDataInstance.dispatchTelemetry();
                    }
                }
            });
            carDataInstance.F.setChangeListener(new LiveData.ValueChangeListener<Object>() {
                @Override
                public void onChanged(Object val) {
                    if (val instanceof Number) {
                        carDataInstance.coolantTemp = ((Number) val).floatValue();
                        carDataInstance.dispatchTelemetry();
                    }
                }
            });
        } catch (Throwable t) {
            AppLogger.w(TAG, "Ошибка регистрации LiveData наблюдателей: " + t.getMessage());
        }
    }

    private void notifyCallbacks(final VehicleState state) {
        if (sCallbacks.isEmpty()) return;
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (Callback cb : sCallbacks) {
                    try {
                        cb.onVehicleStateChanged(state);
                    } catch (Throwable ignored) {}
                }
            }
        });
    }

    @Override
    public synchronized void stopListening() {
        if (this.callback != null) {
            sCallbacks.remove(this.callback);
            this.callback = null;
        }
        if (adapterStartupReceiver != null) {
            try {
                context.unregisterReceiver(adapterStartupReceiver);
            } catch (Throwable ignored) {}
            adapterStartupReceiver = null;
        }
        com.altaysoft.t500_split.GwmPushBinder.unregisterFromAdapter(context);

        if (!isRunning) {
            return;
        }
        isRunning = false;
        AppLogger.i(TAG, "Отключение слушателя от FDBus IPC reader (активных подписчиков: " + sCallbacks.size() + ")");
    }

    public VehicleState getCurrentState() {
        if (carDataInstance != null) {
            int fuelPct = (carDataInstance.remainFuelPerc >= 0f) ? Math.round(carDataInstance.remainFuelPerc) : -1;
            int range = (carDataInstance.remainOdometer >= 0f) ? Math.round(carDataInstance.remainOdometer) : -1;
            float batt = (carDataInstance.batteryVoltage > 0f) ? carDataInstance.batteryVoltage : -1f;
            int odo = (carDataInstance.odoTotal >= 0f) ? Math.round(carDataInstance.odoTotal) : -1;
            float tpms = (carDataInstance.wheel_fl_p > 0f) ? carDataInstance.wheel_fl_p : -1f;

            return new VehicleState(
                    Math.round(carDataInstance.speed),
                    carData.formatGear(carDataInstance.currentGear),
                    Math.round(carDataInstance.rpm),
                    true,
                    carDataInstance.coolantTemp,
                    "FDBus-Live",
                    -1f,
                    fuelPct,
                    range,
                    batt,
                    odo,
                    carDataInstance.outsideTemp,
                    tpms
            );
        }
        return new VehicleState(0, "P", 0, false, -999f, "FDBus-Idle", -1f, -1, -1, -1f, -1, -999f, -1f);
    }
}
