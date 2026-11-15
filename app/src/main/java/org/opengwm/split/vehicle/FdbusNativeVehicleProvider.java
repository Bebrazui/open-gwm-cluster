package org.opengwm.split.vehicle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import androidx.lifecycle.LiveData;
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

        // Listen for carData telemetry dispatch
        carDataInstance.addListener(new carData.TelemetryListener() {
            @Override
            public void onTelemetryUpdate(final VehicleState state) {
                notifyCallbacks(state);
            }
        });

        // Direct LiveData listeners for speed, RPM, gear, coolant in case native lib posts directly
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

        // Initialize native reader only ONCE per process lifetime
        synchronized (INIT_LOCK) {
            if (!sNativeInitialized) {
                sNativeInitialized = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

                            AppLogger.i(TAG, "Дешифрование FDBus строковой таблицы (nativeInitStrings)...");
                            NativeLib.nativeInitStrings(context);

                            AppLogger.i(TAG, "Заполнение эндпоинтов и дескрипторов FDBus в cfgData...");
                            populateCfgStrings(cfgDataInstance);

                            NativeLib.initCfgData(cfgDataInstance);
                            NativeLib.initCarData(carDataInstance);

                            carDataInstance.setStartTime((int) (System.currentTimeMillis() / 1000));
                            carDataInstance.setKIv("0");
                            carDataInstance.setK0("0");

                            NativeLib.nativeSetContext(context);
                            NativeLib.nativeInitReader(carDataInstance);
                            NativeLib.nativeBindView(carDataInstance);

                            boolean readerRunning = NativeLib.nativeIsReaderRunning();
                            boolean viewBound = NativeLib.nativeIsViewBound();
                            AppLogger.i(TAG, "FDBus IPC reader статус: readerRunning=" + readerRunning + ", viewBound=" + viewBound);
                        } catch (Throwable t) {
                            AppLogger.e(TAG, "Исключение при запуске нативного FDBus ридера", t);
                        }
                    }
                }, "FDBus-Init-Worker").start();
            }
        }
    }

    private static void populateCfgStrings(cfgData cfg) {
        try {
            cfg.m = NativeLib.getStringByIndex(149);
            cfg.n = NativeLib.getStringByIndex(125);
            cfg.o = NativeLib.getStringByIndex(40);
            cfg.p = NativeLib.getStringByIndex(219);
            cfg.q = NativeLib.getStringByIndex(147);
            cfg.r = NativeLib.getStringByIndex(215);
            cfg.s = NativeLib.getStringByIndex(168);
            cfg.t = NativeLib.getStringByIndex(201);
            cfg.u = NativeLib.getStringByIndex(232);
            cfg.v = NativeLib.getStringByIndex(175);
            cfg.w = NativeLib.getStringByIndex(207);
            cfg.x = NativeLib.getStringByIndex(134);
            cfg.y = NativeLib.getStringByIndex(49);
            cfg.z = NativeLib.getStringByIndex(100);
            cfg.A = NativeLib.getStringByIndex(5);
            cfg.B = NativeLib.getStringByIndex(51);
            cfg.C = NativeLib.getStringByIndex(15);
            cfg.D = NativeLib.getStringByIndex(235);
            cfg.E = NativeLib.getStringByIndex(53);
            cfg.F = NativeLib.getStringByIndex(132);
            cfg.G = NativeLib.getStringByIndex(189);
            cfg.H = NativeLib.getStringByIndex(248);
            cfg.I = NativeLib.getStringByIndex(247);
            cfg.J = NativeLib.getStringByIndex(22);
            cfg.K = NativeLib.getStringByIndex(7);
            cfg.L = NativeLib.getStringByIndex(239);
            cfg.M = NativeLib.getStringByIndex(244);
            cfg.N = NativeLib.getStringByIndex(128);
            cfg.O = NativeLib.getStringByIndex(114);
            cfg.P = NativeLib.getStringByIndex(3);
            cfg.Q = NativeLib.getStringByIndex(159);
            cfg.R = NativeLib.getStringByIndex(18);
            cfg.S = NativeLib.getStringByIndex(127);
            cfg.T = NativeLib.getStringByIndex(198);
            cfg.U = NativeLib.getStringByIndex(157);
            cfg.V = NativeLib.getStringByIndex(66);
            cfg.W = NativeLib.getStringByIndex(126);
            cfg.X = NativeLib.getStringByIndex(214);
            cfg.Y = NativeLib.getStringByIndex(121);
            cfg.Z = NativeLib.getStringByIndex(94);
            cfg.a0 = NativeLib.getStringByIndex(98);
            cfg.b0 = NativeLib.getStringByIndex(227);
            cfg.c0 = NativeLib.getStringByIndex(41);
            cfg.d0 = NativeLib.getStringByIndex(236);
            cfg.e0 = NativeLib.getStringByIndex(153);
            cfg.f0 = NativeLib.getStringByIndex(186);
            cfg.g0 = NativeLib.getStringByIndex(77);
            cfg.h0 = NativeLib.getStringByIndex(62);
            cfg.i0 = NativeLib.getStringByIndex(173);
            cfg.j0 = NativeLib.getStringByIndex(123);
            cfg.k0 = NativeLib.getStringByIndex(6);
            cfg.l0 = NativeLib.getStringByIndex(111);
            cfg.m0 = NativeLib.getStringByIndex(28);
            cfg.n0 = NativeLib.getStringByIndex(234);
            cfg.o0 = NativeLib.getStringByIndex(70);
            cfg.p0 = NativeLib.getStringByIndex(109);
            AppLogger.i(TAG, "cfgData строковые дескрипторы FDBus успешно инициализированы (m=" + cfg.m + ", n=" + cfg.n + ", B=" + cfg.B + ")");
        } catch (Throwable t) {
            AppLogger.e(TAG, "Ошибка заполнения cfgData строк", t);
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
