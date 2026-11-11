package org.opengwm.split.util;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * High-performance in-memory ring logger with real-time UI subscriptions
 * and file export capabilities for in-vehicle GWM Tank 500 diagnostics.
 */
public class AppLogger {

    public enum Level {
        INFO,
        WARN,
        ERROR,
        SUCCESS
    }

    public static class LogEntry {
        public final long timestamp;
        public final Level level;
        public final String tag;
        public final String message;

        public LogEntry(Level level, String tag, String message) {
            this.timestamp = System.currentTimeMillis();
            this.level = level;
            this.tag = tag;
            this.message = message;
        }

        public String getFormatted() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
            return sdf.format(new Date(timestamp)) + " [" + level.name() + "] " + tag + ": " + message;
        }
    }

    public interface LogListener {
        void onNewLog(LogEntry entry);
        void onLogsCleared();
    }

    private static final int MAX_ENTRIES = 800;
    private static final LinkedList<LogEntry> entries = new LinkedList<>();
    private static final List<LogListener> listeners = new ArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Object lock = new Object();

    public static void i(String tag, String message) {
        log(Level.INFO, tag, message);
        Log.i(tag, message);
    }

    public static void w(String tag, String message) {
        log(Level.WARN, tag, message);
        Log.w(tag, message);
    }

    public static void e(String tag, String message) {
        log(Level.ERROR, tag, message);
        Log.e(tag, message);
    }

    public static void e(String tag, String message, Throwable tr) {
        String fullMsg = message + (tr != null ? (" | Exc: " + tr.getClass().getSimpleName() + ": " + tr.getMessage()) : "");
        log(Level.ERROR, tag, fullMsg);
        Log.e(tag, fullMsg, tr);
    }

    public static void s(String tag, String message) {
        log(Level.SUCCESS, tag, message);
        Log.i(tag, "SUCCESS: " + message);
    }

    private static void log(final Level level, final String tag, final String message) {
        final LogEntry entry = new LogEntry(level, tag, message);
        synchronized (lock) {
            if (entries.size() >= MAX_ENTRIES) {
                entries.removeFirst();
            }
            entries.addLast(entry);
        }

        if (!listeners.isEmpty()) {
            mainHandler.post(() -> {
                synchronized (lock) {
                    for (LogListener l : listeners) {
                        try {
                            l.onNewLog(entry);
                        } catch (Throwable ignored) {}
                    }
                }
            });
        }
    }

    public static void addListener(LogListener listener) {
        synchronized (lock) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public static void removeListener(LogListener listener) {
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    public static List<LogEntry> getEntriesSnapshot() {
        synchronized (lock) {
            return new ArrayList<>(entries);
        }
    }

    public static String getAllLogsAsString() {
        StringBuilder sb = new StringBuilder();
        synchronized (lock) {
            for (LogEntry e : entries) {
                sb.append(e.getFormatted()).append("\n");
            }
        }
        return sb.toString();
    }

    public static void clear() {
        synchronized (lock) {
            entries.clear();
        }
        mainHandler.post(() -> {
            synchronized (lock) {
                for (LogListener l : listeners) {
                    try {
                        l.onLogsCleared();
                    } catch (Throwable ignored) {}
                }
            }
        });
    }

    public static File exportToFile(Context context) {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        File logFile = new File(downloadDir, "opengwm_diag_" + sdf.format(new Date()) + ".txt");

        FileWriter writer = null;
        try {
            writer = new FileWriter(logFile);
            writer.write("=== OpenGWM Tank 500 Diagnostics Log ===\n");
            writer.write("Date: " + new Date().toString() + "\n");
            writer.write("App Version: 1.0\n");
            writer.write("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ", API " + android.os.Build.VERSION.SDK_INT + ")\n");
            writer.write("----------------------------------------\n\n");
            writer.write(getAllLogsAsString());
            writer.flush();
            s("AppLogger", "Logs exported to " + logFile.getAbsolutePath());
            return logFile;
        } catch (IOException e) {
            e("AppLogger", "Failed to export logs: " + e.getMessage());
            return null;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
