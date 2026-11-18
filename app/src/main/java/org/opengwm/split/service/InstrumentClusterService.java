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
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.opengwm.split.R;
import org.opengwm.split.ui.ClusterConfig;
import org.opengwm.split.ui.ClusterGaugeView;
import org.opengwm.split.util.AppLogger;
import org.opengwm.split.vehicle.VehicleDataProvider;
import org.opengwm.split.vehicle.VehicleState;

/**
 * Service that projects custom instrument cluster themes ("Колодцы")
 * onto the target display(s) in GWM Tank 500 / SA8155.
 * Uses exact native layout parameters matching the vehicle's QNX hypervisor.
 */
public class InstrumentClusterService extends Service {

    private static final String TAG = "OpenGwmCluster";
    private static final String CHANNEL_ID = "cluster_overlay_service";
    private static final int NOTIFICATION_ID = 2001;

    public static final String ACTION_START = "org.opengwm.split.action.START_CLUSTER";
    public static final String ACTION_STOP = "org.opengwm.split.action.STOP_CLUSTER";
    public static final String EXTRA_TARGET_DISPLAY_ID = "target_display_id";

    // Overlay session on a single display
    private static class DisplayOverlaySession {
        final Display display;
        final WindowManager windowManager;
        final FrameLayout rootLayout;
        final ClusterGaugeView gaugeView;

        DisplayOverlaySession(Display display, WindowManager wm, FrameLayout root, ClusterGaugeView gaugeView) {
            this.display = display;
            this.windowManager = wm;
            this.rootLayout = root;
            this.gaugeView = gaugeView;
        }

        void updateTelemetry(VehicleState state) {
            if (gaugeView != null) {
                gaugeView.updateTelemetry(state);
            }
        }

        void applyConfig(Context context) {
            if (gaugeView != null) {
                gaugeView.setThemeStyle(ClusterConfig.getTheme(context));
                gaugeView.setHolePunch(ClusterConfig.isHolePunchEnabled(context));
                gaugeView.setSmoothing(ClusterConfig.isSmoothingEnabled(context));
                gaugeView.setPlayerTheme(ClusterConfig.getPlayerTheme(context));
                gaugeView.setPlayerBg(ClusterConfig.getPlayerBg(context));
                gaugeView.setWidgets(
                        ClusterConfig.getWidgetLeft(context),
                        ClusterConfig.getWidgetRight(context),
                        ClusterConfig.getWidgetCenter(context)
                );
                gaugeView.loadLayoutConfig();
                gaugeView.postInvalidate();
            }
        }

        void destroy() {
            try {
                if (windowManager != null && rootLayout != null) {
                    windowManager.removeView(rootLayout);
                }
            } catch (Exception ignored) {}
        }
    }

    private final List<DisplayOverlaySession> activeSessions = new CopyOnWriteArrayList<>();
    public static final List<ClusterGaugeView> activeViews = new CopyOnWriteArrayList<>();
    public static volatile ClusterGaugeView activeClusterView = null; // for backwards compatibility
    public static volatile boolean isServiceRunning = false;

    private VehicleDataProvider vehicleProvider;
    private boolean isRunning = false;

    public static void applyConfig(Context context) {
        for (ClusterGaugeView view : activeViews) {
            view.setThemeStyle(ClusterConfig.getTheme(context));
            view.setHolePunch(ClusterConfig.isHolePunchEnabled(context));
            view.setSmoothing(ClusterConfig.isSmoothingEnabled(context));
            view.setPlayerTheme(ClusterConfig.getPlayerTheme(context));
            view.setPlayerBg(ClusterConfig.getPlayerBg(context));
            view.setClusterBg(ClusterConfig.getClusterBg(context));
            view.setClusterBgCustomPath(ClusterConfig.getClusterBgCustomPath(context));
            view.setWidgets(
                    ClusterConfig.getWidgetLeft(context),
                    ClusterConfig.getWidgetRight(context),
                    ClusterConfig.getWidgetCenter(context)
            );
            view.loadLayoutConfig();
            view.postInvalidate();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startAsForeground();
        if (org.opengwm.split.vehicle.FdbusNativeVehicleProvider.isAvailable()) {
            vehicleProvider = new org.opengwm.split.vehicle.FdbusNativeVehicleProvider(this);
        } else {
            vehicleProvider = new org.opengwm.split.vehicle.AndroidAutomotiveVehicleProvider(this);
        }
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
                    .setSmallIcon(R.drawable.icon_split)
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

        int targetDisplayId = (intent != null) ?
                intent.getIntExtra(EXTRA_TARGET_DISPLAY_ID, ClusterConfig.getTargetDisplay(this)) :
                ClusterConfig.getTargetDisplay(this);

        startCluster(targetDisplayId);
        return START_STICKY;
    }

    private boolean attachOverlayToDisplay(Display targetDisplay) {
        if (targetDisplay == null) return false;
        try {
            Context appContext = getApplicationContext();
            Context displayContext = appContext.createDisplayContext(targetDisplay);
            WindowManager wm = (WindowManager) displayContext.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                AppLogger.e(TAG, "WindowManager недоступен для дисплея ID: " + targetDisplay.getDisplayId());
                return false;
            }

            DisplayMetrics metrics = new DisplayMetrics();
            targetDisplay.getRealMetrics(metrics);
            int width = metrics.widthPixels > 0 ? metrics.widthPixels : 1920;
            int height = metrics.heightPixels > 0 ? metrics.heightPixels : 720;

            // Strict working flags from GWM Tank 500 decompiled split:
            // 536 = FLAG_NOT_FOCUSABLE (8) | FLAG_NOT_TOUCHABLE (16) | FLAG_LAYOUT_NO_LIMITS (512)
            // gravity = 8388659 (Gravity.TOP | Gravity.START), type = 2038 (TYPE_APPLICATION_OVERLAY)
            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                @SuppressWarnings("deprecation")
                int deprecatedType = WindowManager.LayoutParams.TYPE_PHONE;
                layoutType = deprecatedType;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    width,
                    height,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 0;

            FrameLayout rootLayout = new FrameLayout(displayContext);
            rootLayout.setClipChildren(false);
            rootLayout.setClipToPadding(false);
            rootLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

            ClusterGaugeView gaugeView = new ClusterGaugeView(displayContext);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height, Gravity.TOP | Gravity.START);
            rootLayout.addView(gaugeView, lp);

            wm.addView(rootLayout, params);

            DisplayOverlaySession session = new DisplayOverlaySession(targetDisplay, wm, rootLayout, gaugeView);
            session.applyConfig(this);

            activeSessions.add(session);
            activeViews.add(gaugeView);
            activeClusterView = gaugeView;

            AppLogger.s(TAG, "УСПЕШНО: Оверлей подключен к дисплею ID " + targetDisplay.getDisplayId() + " (" + targetDisplay.getName() + ", " + width + "x" + height + ")");
            return true;
        } catch (Exception e) {
            AppLogger.e(TAG, "Ошибка подключения оверлея к дисплею ID " + targetDisplay.getDisplayId(), e);
            return false;
        }
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

        activeSessions.clear();
        activeViews.clear();
        boolean attachedAny = false;

        if (preferredDisplayId == ClusterConfig.DISPLAY_TARGET_ALL_SECONDARY) {
            // Attach to ALL secondary displays (e.g. Display 1 and Display 2)
            AppLogger.i(TAG, "Режим: Проекция на ВСЕ вторичные экраны (ID 1 + 2)");
            if (displays != null) {
                for (Display d : displays) {
                    if (d.getDisplayId() != Display.DEFAULT_DISPLAY) {
                        if (attachOverlayToDisplay(d)) {
                            attachedAny = true;
                        }
                    }
                }
            }
        } else if (preferredDisplayId >= 0) {
            AppLogger.i(TAG, "Режим: Проекция на выбранный экран ID " + preferredDisplayId);
            Display target = dm.getDisplay(preferredDisplayId);
            if (target != null && attachOverlayToDisplay(target)) {
                attachedAny = true;
            }
        }

        // Auto fallback if preferred display failed or none attached:
        if (!attachedAny) {
            AppLogger.w(TAG, "Основная цель недоступна, запуск автоматического поиска вторичных экранов...");
            // Try Display 2
            Display d2 = dm.getDisplay(2);
            if (d2 != null && attachOverlayToDisplay(d2)) {
                attachedAny = true;
            }
            // Try Display 1
            if (!attachedAny) {
                Display d1 = dm.getDisplay(1);
                if (d1 != null && attachOverlayToDisplay(d1)) {
                    attachedAny = true;
                }
            }
            // Try any secondary display
            if (!attachedAny && displays != null) {
                for (Display d : displays) {
                    if (d.getDisplayId() != Display.DEFAULT_DISPLAY) {
                        if (attachOverlayToDisplay(d)) {
                            attachedAny = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!attachedAny) {
            AppLogger.e(TAG, "ОШИБКА: Не удалось подключить оверлей ни к одному дисплею!");
            stopCluster();
            stopSelf();
            return;
        }

        // Connect vehicle telemetry to all active gauge views
        vehicleProvider.startListening(new VehicleDataProvider.Callback() {
            @Override
            public void onVehicleStateChanged(final VehicleState state) {
                for (DisplayOverlaySession session : activeSessions) {
                    session.updateTelemetry(state);
                }
            }
        });

        isRunning = true;
        isServiceRunning = true;
    }

    private void stopCluster() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        if (!isRunning) return;
        vehicleProvider.stopListening();
        for (DisplayOverlaySession session : activeSessions) {
            session.destroy();
        }
        activeSessions.clear();
        activeViews.clear();
        activeClusterView = null;
        isRunning = false;
        isServiceRunning = false;
        AppLogger.i(TAG, "Служба приборной панели остановлена, все оверлеи удалены");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCluster();
    }
}
