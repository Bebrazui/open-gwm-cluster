package org.opengwm.split.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import org.opengwm.split.ui.ClusterGaugeView;
import org.opengwm.split.util.AppLogger;
import org.opengwm.split.vehicle.MockVehicleProvider;
import org.opengwm.split.vehicle.VehicleDataProvider;
import org.opengwm.split.vehicle.VehicleState;

/**
 * Service that projects custom instrument cluster themes ("Колодцы")
 * directly onto the secondary display (Display ID 2 in Tank 500 / GWM).
 */
public class InstrumentClusterService extends Service {

    private static final String TAG = "OpenGwmCluster";
    private static final String CHANNEL_ID = "cluster_overlay_service";
    private static final int NOTIFICATION_ID = 2001;

    public static final String ACTION_START = "org.opengwm.split.action.START_CLUSTER";
    public static final String ACTION_STOP = "org.opengwm.split.action.STOP_CLUSTER";
    public static final String EXTRA_TARGET_DISPLAY_ID = "target_display_id";

    private WindowManager clusterWindowManager;
    private ClusterGaugeView clusterView;
    private VehicleDataProvider vehicleProvider;
    private boolean isRunning = false;

    public static volatile boolean isServiceRunning = false;
    public static volatile ClusterGaugeView activeClusterView = null;

    public static void applyConfig(Context context) {
        if (activeClusterView != null) {
            activeClusterView.setThemeStyle(org.opengwm.split.ui.ClusterConfig.getTheme(context));
            activeClusterView.setHolePunch(org.opengwm.split.ui.ClusterConfig.isHolePunchEnabled(context));
            activeClusterView.setSmoothing(org.opengwm.split.ui.ClusterConfig.isSmoothingEnabled(context));
            activeClusterView.setWidgets(
                    org.opengwm.split.ui.ClusterConfig.getWidgetLeft(context),
                    org.opengwm.split.ui.ClusterConfig.getWidgetRight(context),
                    org.opengwm.split.ui.ClusterConfig.getWidgetCenter(context)
            );
            activeClusterView.loadLayoutConfig();
            activeClusterView.postInvalidate();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startAsForeground();
        vehicleProvider = new org.opengwm.split.vehicle.AndroidAutomotiveVehicleProvider(this);
    }

    private void startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Instrument Cluster Overlay",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);
            }
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("OpenGWM Cluster")
                    .setContentText("Оверлей приборной панели активен")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .setOngoing(true)
                    .build();
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopCluster();
            stopSelf();
            return START_NOT_STICKY;
        }

        int targetDisplayId = (intent != null) ? intent.getIntExtra(EXTRA_TARGET_DISPLAY_ID, -1) : -1;
        startCluster(targetDisplayId);
        return START_STICKY;
    }

    private void startCluster(int preferredDisplayId) {
        if (isRunning) return;

        DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) {
            AppLogger.e(TAG, "DisplayManager недоступен в системе");
            stopSelf();
            return;
        }

        Display[] displays = dm.getDisplays();
        AppLogger.i(TAG, "Запуск службы приборной панели. Найдено экранов: " + (displays != null ? displays.length : 0));
        if (displays != null) {
            for (Display d : displays) {
                DisplayMetrics metrics = new DisplayMetrics();
                d.getRealMetrics(metrics);
                AppLogger.i(TAG, "  Экран ID " + d.getDisplayId() + ": " + d.getName() + " (" + metrics.widthPixels + "x" + metrics.heightPixels + ")");
            }
        }

        Display targetDisplay = null;
        if (preferredDisplayId >= 0) {
            targetDisplay = dm.getDisplay(preferredDisplayId);
        }

        // Auto-detect GWM instrument cluster display if not explicitly specified
        if (targetDisplay == null) {
            // Tank 500 default is display 2
            targetDisplay = dm.getDisplay(2);
        }

        if (targetDisplay == null && displays != null) {
            // Fallback: look for any secondary non-default display
            for (Display d : displays) {
                if (d.getDisplayId() != Display.DEFAULT_DISPLAY) {
                    targetDisplay = d;
                    break;
                }
            }
        }

        if (targetDisplay == null) {
            AppLogger.e(TAG, "ОШИБКА: Вторичный дисплей приборной панели (Display ID 2) не найден! Служба остановлена.");
            stopCluster();
            stopSelf();
            return;
        }

        AppLogger.s(TAG, "Подключение оверлея к дисплею ID: " + targetDisplay.getDisplayId() + " (" + targetDisplay.getName() + ")");

        try {
            Context displayContext = createDisplayContext(targetDisplay);
            clusterWindowManager = (WindowManager) displayContext.getSystemService(Context.WINDOW_SERVICE);

            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                @SuppressWarnings("deprecation")
                int deprecatedType = WindowManager.LayoutParams.TYPE_PHONE;
                layoutType = deprecatedType;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );

            clusterView = new ClusterGaugeView(displayContext);
            activeClusterView = clusterView;
            clusterWindowManager.addView(clusterView, params);
            AppLogger.s(TAG, "УСПЕШНО: Оверлей шкал добавлен в WindowManager дисплея ID " + targetDisplay.getDisplayId());

            // Connect vehicle telemetry to gauges
            vehicleProvider.startListening(new VehicleDataProvider.Callback() {
                @Override
                public void onVehicleStateChanged(VehicleState state) {
                    if (clusterView != null) {
                        clusterView.updateTelemetry(state);
                    }
                }
            });

            isRunning = true;
            isServiceRunning = true;
        } catch (Exception e) {
            AppLogger.e(TAG, "Ошибка вывода оверлея на экран", e);
            stopCluster();
            stopSelf();
        }
    }

    private void stopCluster() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        if (!isRunning) return;
        vehicleProvider.stopListening();
        if (clusterWindowManager != null && clusterView != null) {
            try {
                clusterWindowManager.removeView(clusterView);
            } catch (Exception ignored) {}
            clusterView = null;
            activeClusterView = null;
        }
        isRunning = false;
        isServiceRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCluster();
    }
}
