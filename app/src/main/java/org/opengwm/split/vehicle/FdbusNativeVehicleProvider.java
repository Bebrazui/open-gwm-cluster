package org.opengwm.split.vehicle;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.altaysoft.t500_split.NativeLib;
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

    public static boolean isAvailable() {
        return NativeLib.isLibraryLoaded();
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

        AppLogger.i(TAG, "Инициализация нативного FDBus IPC клиента для GWM Tank 500...");

        boolean loaded = NativeLib.loadLibraries();
        if (!loaded) {
            AppLogger.e(TAG, "Ошибка загрузки нативных библиотек: " + NativeLib.getLoadError());
            return;
        }

        carDataInstance = carData.getInstance();
        cfgDataInstance = cfgData.getInstance();

        // Configure cfgData for Tank 500 (Vehicle ID = 5, CAN streaming = true)
        cfgDataInstance.L0 = 5;
        cfgDataInstance.j1 = true;
        cfgDataInstance.n = "com.altaysoft.t500_split";

        carDataInstance.addListener(new carData.TelemetryListener() {
            @Override
            public void onTelemetryUpdate(final VehicleState state) {
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
        });

        // Initialize native reader only ONCE per process lifetime
        synchronized (INIT_LOCK) {
            if (!sNativeInitialized) {
                sNativeInitialized = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AppLogger.i(TAG, "Настройка контекста и запуск nativeInitReader / nativeBindView...");

                            NativeLib.initCfgData(cfgDataInstance);
                            NativeLib.initCarData(carDataInstance);
                            NativeLib.nativeInitStrings(context);
                            NativeLib.nativeSetContext(context);
                            NativeLib.nativeInitReader(carDataInstance);
                            NativeLib.nativeBindView(carDataInstance);

                            AppLogger.i(TAG, "FDBus IPC reader успешно запущен. Профиль Tank 500 (ID=5) активен.");
                        } catch (Throwable t) {
                            AppLogger.e(TAG, "Исключение при запуске нативного FDBus ридера", t);
                        }
                    }
                }, "FDBus-Init-Worker").start();
            }
        }
    }

    @Override
    public synchronized void stopListening() {
        if (this.callback != null) {
            sCallbacks.remove(this.callback);
            this.callback = null;
        }
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
