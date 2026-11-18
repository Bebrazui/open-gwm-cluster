package org.opengwm.split.vehicle;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import com.altaysoft.t500_split.GwmPushBinder;
import org.opengwm.split.util.AppLogger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance Vehicle CAN Traffic Recorder & Replay Engine.
 * 
 * Captures real onPush(topic, payload) transactions from gwm_adapter to storage,
 * and replays them on any phone/emulator with original timings to reproduce live cluster telemetry.
 */
public class TrafficRecorderReplay {
    private static final String TAG = "TrafficRecorder";
    private static final String FILE_NAME = "gwm_traffic.log";

    public interface StatusListener {
        void onStatusUpdate(String status, int packetCount, boolean isRecording, boolean isReplaying);
    }

    private static final AtomicBoolean isRecording = new AtomicBoolean(false);
    private static final AtomicBoolean isReplaying = new AtomicBoolean(false);
    private static final AtomicInteger packetCount = new AtomicInteger(0);

    private static BufferedWriter logWriter = null;
    private static File activeLogFile = null;
    private static final Object fileLock = new Object();
    private static final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static StatusListener statusListener = null;
    private static Thread replayThread = null;

    public static void setStatusListener(StatusListener listener) {
        statusListener = listener;
    }

    public static boolean isRecording() {
        return isRecording.get();
    }

    public static boolean isReplaying() {
        return isReplaying.get();
    }

    public static int getPacketCount() {
        return packetCount.get();
    }

    public static File getLogFile(Context context) {
        if (activeLogFile != null && activeLogFile.exists()) {
            return activeLogFile;
        }
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir != null && (downloadDir.exists() || downloadDir.mkdirs())) {
            return new File(downloadDir, FILE_NAME);
        }
        File externalFiles = context.getExternalFilesDir(null);
        if (externalFiles != null) {
            return new File(externalFiles, FILE_NAME);
        }
        return new File(context.getFilesDir(), FILE_NAME);
    }

    public static synchronized boolean startRecording(Context context) {
        if (isRecording.get()) return true;
        if (isReplaying.get()) stopReplay();

        try {
            activeLogFile = getLogFile(context);
            synchronized (fileLock) {
                logWriter = new BufferedWriter(new FileWriter(activeLogFile, true), 32768);
                logWriter.write("# GWM Traffic Session Started: " + System.currentTimeMillis() + "\n");
                logWriter.flush();
            }
            packetCount.set(0);
            isRecording.set(true);
            AppLogger.s(TAG, "Запись CAN-трафика начата в: " + activeLogFile.getAbsolutePath());
            notifyStatus("Запись активна...", 0);
            return true;
        } catch (IOException e) {
            AppLogger.e(TAG, "Ошибка открытия файла для записи трафика", e);
            notifyStatus("Ошибка записи: " + e.getMessage(), 0);
            return false;
        }
    }

    public static synchronized void stopRecording() {
        if (!isRecording.get()) return;
        isRecording.set(false);

        writeExecutor.execute(() -> {
            synchronized (fileLock) {
                if (logWriter != null) {
                    try {
                        logWriter.flush();
                        logWriter.close();
                    } catch (IOException ignored) {}
                    logWriter = null;
                }
            }
            int total = packetCount.get();
            AppLogger.s(TAG, "Запись завершена. Сохранено пакетов: " + total + " -> " + (activeLogFile != null ? activeLogFile.getAbsolutePath() : "null"));
            notifyStatus("Остановлен (сохранено " + total + " пак.)", total);
        });
    }

    /**
     * Records an incoming onPush packet from GwmPushBinder.
     * Line format: timestampMs\ttopic\tpayload
     */
    public static void record(final String topic, final String payload) {
        if (!isRecording.get()) return;
        final long now = System.currentTimeMillis();

        writeExecutor.execute(() -> {
            synchronized (fileLock) {
                if (logWriter != null && isRecording.get()) {
                    try {
                        String cleanPayload = payload != null ? payload.replace("\n", "\\n").replace("\r", "") : "";
                        logWriter.write(now + "\t" + topic + "\t" + cleanPayload + "\n");
                        int count = packetCount.incrementAndGet();
                        if (count % 20 == 0) {
                            logWriter.flush();
                            notifyStatus("Запись активна (" + count + " пак.)", count);
                        }
                    } catch (IOException e) {
                        AppLogger.w(TAG, "Ошибка записи строки пакета: " + e.getMessage());
                    }
                }
            }
        });
    }

    public static synchronized void startReplay(final Context context) {
        if (isReplaying.get()) return;
        if (isRecording.get()) stopRecording();

        final File file = getLogFile(context);
        if (!file.exists() || file.length() == 0) {
            AppLogger.w(TAG, "Файл трафика не найден, запускаем генерацию тестового лога...");
            generateSampleTrafficFile(file);
        }

        isReplaying.set(true);
        packetCount.set(0);

        replayThread = new Thread(() -> {
            AppLogger.i(TAG, "Запуск воспроизведения трафика из " + file.getAbsolutePath());
            notifyStatus("Воспроизведение...", 0);

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                long lastPacketTime = -1;
                int count = 0;

                while (isReplaying.get() && (line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split("\t", 3);
                    if (parts.length >= 2) {
                        long ts;
                        try {
                            ts = Long.parseLong(parts[0]);
                        } catch (NumberFormatException e) {
                            ts = System.currentTimeMillis();
                        }
                        String topic = parts[1];
                        String payload = parts.length > 2 ? parts[2].replace("\\n", "\n") : "";

                        if (lastPacketTime > 0) {
                            long diff = ts - lastPacketTime;
                            if (diff > 0 && diff < 500) {
                                try {
                                    Thread.sleep(diff);
                                } catch (InterruptedException e) {
                                    break;
                                }
                            } else if (diff >= 500) {
                                try {
                                    Thread.sleep(50); // Capped pause
                                } catch (InterruptedException e) {
                                    break;
                                }
                            }
                        }
                        lastPacketTime = ts;

                        dispatchPacket(topic, payload);
                        count = packetCount.incrementAndGet();
                        if (count % 15 == 0) {
                            notifyStatus("Воспроизведение (" + count + " пак.)", count);
                        }
                    }
                }

                AppLogger.s(TAG, "Воспроизведение завершено. Обработано пакетов: " + count);
                notifyStatus("Воспроизведение окончено (" + count + " пак.)", count);
            } catch (Exception e) {
                AppLogger.e(TAG, "Ошибка воспроизведения трафика", e);
                notifyStatus("Ошибка реплея: " + e.getMessage(), packetCount.get());
            } finally {
                isReplaying.set(false);
            }
        }, "Traffic-Replay-Worker");
        replayThread.start();
    }

    public static synchronized void stopReplay() {
        if (!isReplaying.get()) return;
        isReplaying.set(false);
        if (replayThread != null) {
            replayThread.interrupt();
            replayThread = null;
        }
        AppLogger.i(TAG, "Воспроизведение остановлено");
        notifyStatus("Остановлен", packetCount.get());
    }

    public static void injectMockPacket(int step) {
        float speed = (step * 7.5f) % 180f;
        int rpm = 800 + ((step * 150) % 5500);
        int gearVal = 1 + ((step / 10) % 8);
        String gear = "D" + gearVal;
        float temp = 85f + (step % 15);

        // Standard Tank 500 signals format pushes
        dispatchPacket("car.basic.vehicle_speed", String.valueOf((int) speed));
        dispatchPacket("car.basic.engine_speed", String.valueOf(rpm));
        dispatchPacket("car.basic.gear_status", gear);
        dispatchPacket("car.basic.coolant_temp", String.valueOf(temp));
        dispatchPacket("car.basic.battery_voltage", "14.5");

        AppLogger.i(TAG, "Инжектирован тестовый пакет: скорость=" + (int)speed + " км/ч, об=" + rpm + ", передача=" + gear);
    }

    private static void dispatchPacket(String topic, String payload) {
        GwmPushBinder binder = GwmPushBinder.getInstance();
        if (binder == null) {
            binder = new GwmPushBinder();
        }
        binder.handlePush(topic, payload);
    }

    private static void generateSampleTrafficFile(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("# Auto-generated GWM Tank 500 Traffic Sample\n");
                long baseTime = System.currentTimeMillis();
                for (int i = 0; i < 200; i++) {
                    long t = baseTime + (i * 50); // 20 Hz
                    float speed = (float) (Math.sin(i * 0.05) * 60 + 60);
                    int rpm = (int) (1200 + Math.sin(i * 0.08) * 2000);
                    int gearNum = 1 + (int)(speed / 20f);
                    if (gearNum > 9) gearNum = 9;
                    writer.write(t + "\tcar.basic.vehicle_speed\t" + Math.round(speed) + "\n");
                    writer.write((t + 5) + "\tcar.basic.engine_speed\t" + rpm + "\n");
                    writer.write((t + 10) + "\tcar.basic.gear_status\tD" + gearNum + "\n");
                    writer.write((t + 15) + "\tcar.basic.coolant_temp\t92\n");
                    writer.write((t + 20) + "\tcar.basic.battery_voltage\t14.6\n");
                    writer.write((t + 25) + "\tcar.basic.slope.status\t[[1,1]]\n");
                    writer.write((t + 30) + "\tcar.basic.title.status\t[[0,1]]\n");
                    writer.write((t + 35) + "\tcar.basic.compass.status\t3\n");
                }
                writer.flush();
            }
            AppLogger.s(TAG, "Сгенерирован реалистичный тестовый лог для домашней проверки: " + file.getAbsolutePath());
        } catch (IOException e) {
            AppLogger.e(TAG, "Не удалось сгенерировать тестовый лог", e);
        }
    }

    private static void notifyStatus(final String text, final int count) {
        if (statusListener != null) {
            mainHandler.post(() -> {
                if (statusListener != null) {
                    statusListener.onStatusUpdate(text, count, isRecording.get(), isReplaying.get());
                }
            });
        }
    }
}
