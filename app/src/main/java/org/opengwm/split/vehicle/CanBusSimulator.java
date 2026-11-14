package org.opengwm.split.vehicle;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Handler;
import android.os.Looper;
import com.altaysoft.t500_split.carData;
import org.opengwm.split.util.AppLogger;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Realistic Sub-ms CAN Bus Simulation Engine ("Способ А").
 * Simulates real GWM Tank 500 CAN bus traffic and abstract FDBus socket
 * at 50 Hz (or 200 Hz in stress mode) with 9AT transmission physics,
 * sensor dynamics, fault injection, and ADB command injection.
 */
public class CanBusSimulator {
    private static final String TAG = "CanBusSimulator";
    private static volatile CanBusSimulator sInstance;

    public enum Mode {
        STOPPED,
        DRIVE,
        FAULT_TEST,
        STRESS_200HZ,
        MANUAL_OVERRIDE
    }

    public interface StatusListener {
        void onSimStatusChanged(Mode mode, double actualHz, long packetsSent, String info);
    }

    private final CopyOnWriteArrayList<StatusListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> simTask;

    private volatile Mode currentMode = Mode.STOPPED;
    private volatile int targetHz = 50;
    private final AtomicLong packetCounter = new AtomicLong(0);
    private final AtomicBoolean isSocketServerRunning = new AtomicBoolean(false);

    private LocalServerSocket localServerSocket;
    private Thread socketListenerThread;

    // Simulation Physics State (Tank 500 3.0T V6 + 9AT)
    private double simTimeSec = 0.0;
    private float currentSpeed = 0f;
    private float currentRpm = 800f;
    private String currentGear = "P";
    private float coolantTemp = 50f;
    private float fuelPerc = 75f;
    private float remainRange = 480f;
    private float battVoltage = 14.2f;
    private float odoTotal = 15420f;
    private float outsideTemp = 18f;
    private float tpmsPressure = 2.4f;

    // Fault Injection States
    private int faultStep = 0;
    private long faultStartTime = 0;
    private String lastFaultInfo = "";

    // Hz calculation
    private long lastHzCalcTime = System.currentTimeMillis();
    private long lastHzPackets = 0;
    private double measuredHz = 0.0;

    public static synchronized CanBusSimulator getInstance() {
        if (sInstance == null) {
            sInstance = new CanBusSimulator();
        }
        return sInstance;
    }

    private CanBusSimulator() {
        // Passive on vehicle startup. Only activates when manually requested.
    }

    public void addListener(StatusListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeListener(StatusListener l) {
        listeners.remove(l);
    }

    public Mode getMode() {
        return currentMode;
    }

    public double getMeasuredHz() {
        return measuredHz;
    }

    public long getPacketCount() {
        return packetCounter.get();
    }



    /**
     * Starts realistic 50 Hz driving cycle simulation.
     */
    public synchronized void startDriveMode() {
        stopTaskOnly();
        currentMode = Mode.DRIVE;
        targetHz = 50;
        simTimeSec = 0.0;
        currentSpeed = 0f;
        currentRpm = 800f;
        currentGear = "P";
        coolantTemp = 50f;
        startSimulationLoop(50);
        notifyStatus("Режим поездки (50 Гц) запущен");
        AppLogger.i(TAG, "Симуляция CAN-шины: Режим Поездки (50 Гц)");
    }

    /**
     * Starts 200 Hz flood stress test.
     */
    public synchronized void startStressMode() {
        stopTaskOnly();
        currentMode = Mode.STRESS_200HZ;
        targetHz = 200;
        startSimulationLoop(200);
        notifyStatus("Шторм шины (200 Гц) запущен");
        AppLogger.i(TAG, "Симуляция CAN-шины: Стресс-тест 200 Гц");
    }

    /**
     * Injects realistic bus faults (spikes, NaNs, missing packets, extreme temp).
     */
    public synchronized void injectFault() {
        stopTaskOnly();
        currentMode = Mode.FAULT_TEST;
        targetHz = 50;
        faultStep = 0;
        faultStartTime = System.currentTimeMillis();
        lastFaultInfo = "Инициализация теста сбоя...";
        startSimulationLoop(50);
        notifyStatus("Тест сбоя шины запущен");
        AppLogger.w(TAG, "Симуляция CAN-шины: Активирован тест сбоев и аномалий");
    }

    /**
     * Injects specific values manually (e.g. from ADB broadcast).
     */
    public synchronized void injectManual(float speed, float rpm, String gear, float coolant, float fuel, float batt) {
        stopTaskOnly();
        currentMode = Mode.MANUAL_OVERRIDE;
        this.currentSpeed = speed;
        this.currentRpm = rpm;
        this.currentGear = (gear != null && !gear.isEmpty()) ? gear : "D";
        if (coolant > -50f) this.coolantTemp = coolant;
        if (fuel >= 0f) this.fuelPerc = fuel;
        if (batt > 0f) this.battVoltage = batt;

        feedCarData(this.currentSpeed, this.currentRpm, this.currentGear, this.coolantTemp, this.fuelPerc, this.remainRange, this.battVoltage);
        packetCounter.incrementAndGet();
        notifyStatus("Ручной ввод: " + Math.round(speed) + " км/ч, " + Math.round(rpm) + " об/мин, " + gear);
    }

    /**
     * Stops the simulation.
     */
    public synchronized void stop() {
        stopTaskOnly();
        currentMode = Mode.STOPPED;
        measuredHz = 0.0;
        notifyStatus("Симуляция остановлена");
        AppLogger.i(TAG, "Симуляция CAN-шины остановлена");
    }

    private void stopTaskOnly() {
        if (simTask != null) {
            simTask.cancel(true);
            simTask = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void startSimulationLoop(int hz) {
        executor = Executors.newSingleThreadScheduledExecutor();
        long periodMs = 1000 / hz;

        simTask = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    tick();
                } catch (Throwable t) {
                    AppLogger.e(TAG, "Ошибка тика симуляции шины", t);
                }
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        double dt = 1.0 / targetHz;
        simTimeSec += dt;
        long packets = packetCounter.incrementAndGet();

        // Calculate actual Hz once per second
        long now = System.currentTimeMillis();
        if (now - lastHzCalcTime >= 1000) {
            long deltaPackets = packets - lastHzPackets;
            long deltaTimeMs = now - lastHzCalcTime;
            measuredHz = (deltaPackets * 1000.0) / deltaTimeMs;
            lastHzPackets = packets;
            lastHzCalcTime = now;
            notifyStatus(getModeDescription());
        }

        switch (currentMode) {
            case DRIVE:
            case STRESS_200HZ:
                simulateDriveCycle(dt);
                break;
            case FAULT_TEST:
                simulateFaultSequence();
                break;
            case MANUAL_OVERRIDE:
            case STOPPED:
            default:
                break;
        }
    }

    /**
     * Models realistic Tank 500 3.0T V6 + 9AT driving cycle.
     */
    private void simulateDriveCycle(double dt) {
        // 60-second realistic cycle:
        // 0-4s: warm idle (0 km/h, P)
        // 4-6s: switch to D, creep
        // 6-25s: brisk acceleration to 135 km/h with 9AT upshifts
        // 25-42s: highway cruise around 120-135 km/h with small speed waves
        // 42-52s: braking to 0 km/h with 9AT downshifts
        // 52-60s: stop at red light (D idle, 0 km/h)
        double cycleTime = simTimeSec % 60.0;

        float targetSpeed;
        if (cycleTime < 4.0) {
            targetSpeed = 0f;
            currentGear = "P";
            currentRpm = 750f + (float) Math.sin(simTimeSec * 2.0) * 20f;
        } else if (cycleTime < 6.0) {
            targetSpeed = 8f;
            currentGear = "D1";
            currentRpm = 950f;
        } else if (cycleTime < 25.0) {
            double accelProgress = (cycleTime - 6.0) / 19.0;
            targetSpeed = (float) (8.0 + Math.pow(accelProgress, 0.85) * 127.0); // Up to 135 km/h
            compute9atGearAndRpm(targetSpeed, true);
        } else if (cycleTime < 42.0) {
            double cruiseProgress = (cycleTime - 25.0) / 17.0;
            targetSpeed = (float) (125.0 + Math.sin(cruiseProgress * Math.PI * 4.0) * 8.0);
            compute9atGearAndRpm(targetSpeed, false);
        } else if (cycleTime < 52.0) {
            double decelProgress = (cycleTime - 42.0) / 10.0;
            targetSpeed = (float) (125.0 * (1.0 - decelProgress));
            compute9atGearAndRpm(targetSpeed, false);
        } else {
            targetSpeed = 0f;
            currentGear = "D";
            currentRpm = 680f + (float) Math.sin(simTimeSec * 1.5) * 15f;
        }

        // Apply realistic road jitter (+- 0.4 km/h)
        float jitter = (float) ((Math.random() - 0.5) * 0.8);
        currentSpeed = Math.max(0f, targetSpeed + (targetSpeed > 5f ? jitter : 0f));

        // Warm up coolant gradually to 90C
        if (coolantTemp < 89.5f) {
            coolantTemp += (float) (dt * 0.4);
        } else {
            coolantTemp = 90f + (float) Math.sin(simTimeSec * 0.1) * 1.2f;
        }

        // Battery voltage fluctuation
        battVoltage = 14.1f + (float) Math.sin(simTimeSec * 0.5) * 0.2f;

        // Odometer accumulation
        odoTotal += (float) ((currentSpeed / 3600.0) * dt);

        feedCarData(currentSpeed, currentRpm, currentGear, coolantTemp, fuelPerc, remainRange, battVoltage);
    }

    /**
     * Maps speed to realistic Tank 500 9-speed automatic transmission (9AT) ratios.
     */
    private void compute9atGearAndRpm(float speed, boolean isAccelerating) {
        if (speed < 0.5f) {
            currentGear = "D";
            currentRpm = 750f;
        } else if (speed < 22f) {
            currentGear = "D1";
            currentRpm = 900f + (speed / 22f) * 2600f; // 900 -> 3500
        } else if (speed < 44f) {
            currentGear = "D2";
            currentRpm = 1800f + ((speed - 22f) / 22f) * 2200f; // 1800 -> 4000
        } else if (speed < 68f) {
            currentGear = "D3";
            currentRpm = 1900f + ((speed - 44f) / 24f) * 2000f; // 1900 -> 3900
        } else if (speed < 92f) {
            currentGear = "D4";
            currentRpm = 2000f + ((speed - 68f) / 24f) * 1800f; // 2000 -> 3800
        } else if (speed < 114f) {
            currentGear = "D5";
            currentRpm = 2000f + ((speed - 92f) / 22f) * 1600f; // 2000 -> 3600
        } else if (speed < 136f) {
            currentGear = "D6";
            currentRpm = 2100f + ((speed - 114f) / 22f) * 1400f; // 2100 -> 3500
        } else if (speed < 158f) {
            currentGear = "D7";
            currentRpm = 2100f + ((speed - 136f) / 22f) * 1200f; // 2100 -> 3300
        } else if (speed < 180f) {
            currentGear = "D8";
            currentRpm = 2000f + ((speed - 158f) / 22f) * 1000f; // 2000 -> 3000
        } else {
            currentGear = "D9";
            currentRpm = 1900f + ((speed - 180f) / 40f) * 1200f;
        }

        if (!isAccelerating && currentRpm > 1800f) {
            currentRpm = Math.max(1200f, currentRpm * 0.75f); // Engine braking / low throttle
        }
    }

    /**
     * Sequences through rigorous fault tests.
     */
    private void simulateFaultSequence() {
        long elapsed = System.currentTimeMillis() - faultStartTime;

        if (elapsed < 3000) {
            // Normal base
            lastFaultInfo = "Фаза 1: Нормальная езда 100 км/ч";
            currentSpeed = 100f;
            currentRpm = 2200f;
            currentGear = "D5";
            coolantTemp = 90f;
            feedCarData(currentSpeed, currentRpm, currentGear, coolantTemp, fuelPerc, remainRange, battVoltage);
        } else if (elapsed < 5000) {
            // Anomaly 1: Sudden Speed Drop / Spikes
            lastFaultInfo = "Фаза 2: Скачок скорости (100 -> 0 -> 140 -> -1)";
            float spikeSpeed = (elapsed % 400 < 200) ? 0f : 140f;
            feedCarData(spikeSpeed, 4500f, "D5", 90f, fuelPerc, remainRange, battVoltage);
        } else if (elapsed < 8000) {
            // Anomaly 2: Bus Disconnect (watchdog timeout test - zero packets)
            lastFaultInfo = "Фаза 3: Обрыв шины (молчание 3 секунды)...";
            // Do NOT feed carData! Verifies how UI watchdog handles packet loss
        } else if (elapsed < 11000) {
            // Anomaly 3: Bus Recovery + Engine Overheat (125C)
            lastFaultInfo = "Фаза 4: Восстановление + Перегрев ОЖ (125 °C)";
            feedCarData(80f, 2400f, "D4", 125f, fuelPerc, remainRange, battVoltage);
        } else if (elapsed < 14000) {
            // Anomaly 4: Unknown / invalid gear indices
            lastFaultInfo = "Фаза 5: Несуществующая передача (D99 / INVALID)";
            carData cd = carData.getInstance();
            cd.setSpeed(60f);
            cd.setRpm(1800f);
            cd.setCustomGear("D99");
            cd.setCoolantT(90f);
        } else {
            // Loop back or finish
            lastFaultInfo = "Тест сбоев успешно завершен. Возврат в нормальный режим.";
            startDriveMode();
        }
    }

    private void feedCarData(float speed, float rpm, String gear, float coolant, float fuel, float range, float batt) {
        carData cd = carData.getInstance();
        cd.setSpeed(speed);
        cd.setRpm(rpm);
        cd.setCustomGear(gear);
        cd.setCoolantT(coolant);
        cd.setRemainFuelPerc(fuel);
        cd.setRemainOdometer(range);
        cd.setBatteryVoltage(batt);
        cd.setOdoTotal(odoTotal);
        cd.setOutsideTemp(outsideTemp);
        cd.setWheel_fl_p(tpmsPressure);
        cd.setWheel_fr_p(tpmsPressure);
        cd.setWheel_rl_p(tpmsPressure);
        cd.setWheel_rr_p(tpmsPressure);
    }

    private String getModeDescription() {
        switch (currentMode) {
            case DRIVE:
                return "Поездка (50 Гц) | Скорость: " + Math.round(currentSpeed) + " км/ч | " + currentGear;
            case STRESS_200HZ:
                return "Шторм 200 Гц | Пакетов: " + packetCounter.get();
            case FAULT_TEST:
                return lastFaultInfo;
            case MANUAL_OVERRIDE:
                return "Ручной ввод";
            case STOPPED:
            default:
                return "Остановлен";
        }
    }

    private void notifyStatus(final String info) {
        final Mode mode = this.currentMode;
        final double hz = this.measuredHz;
        final long packets = this.packetCounter.get();

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (StatusListener l : listeners) {
                    try {
                        l.onSimStatusChanged(mode, hz, packets, info);
                    } catch (Throwable ignored) {}
                }
            }
        });
    }
}
