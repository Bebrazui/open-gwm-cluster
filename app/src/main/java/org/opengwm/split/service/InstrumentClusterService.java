package org.opengwm.split.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import org.opengwm.split.ui.ClusterGaugeView;
import org.opengwm.split.vehicle.MockVehicleProvider;
import org.opengwm.split.vehicle.VehicleDataProvider;
import org.opengwm.split.vehicle.VehicleState;

/**
 * Service that projects custom instrument cluster themes ("Колодцы")
 * directly onto the secondary display (Display ID 2 in Tank 500 / GWM).
 */
public class InstrumentClusterService extends Service {

    private static final String TAG = "OpenGwmCluster";
    public static final String ACTION_START = "org.opengwm.split.action.START_CLUSTER";
    public static final String ACTION_STOP = "org.opengwm.split.action.STOP_CLUSTER";
    public static final String EXTRA_TARGET_DISPLAY_ID = "target_display_id";

    private WindowManager clusterWindowManager;
    private ClusterGaugeView clusterView;
    private final VehicleDataProvider vehicleProvider = new org.opengwm.split.vehicle.AndroidAutomotiveVehicleProvider();
    private boolean isRunning = false;

    public static volatile boolean isServiceRunning = false;

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
            Log.e(TAG, "DisplayManager is not available");
            stopSelf();
            return;
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

        if (targetDisplay == null) {
            // Fallback: look for any secondary non-default display
            Display[] displays = dm.getDisplays();
            for (Display d : displays) {
                if (d.getDisplayId() != Display.DEFAULT_DISPLAY) {
                    targetDisplay = d;
                    break;
                }
            }
        }

        if (targetDisplay == null) {
            Log.w(TAG, "Secondary instrument cluster display not found! Cluster service will stop.");
            stopSelf();
            return;
        }

        Log.i(TAG, "Attaching cluster overlay to display ID: " + targetDisplay.getDisplayId() + " (" + targetDisplay.getName() + ")");

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
            clusterWindowManager.addView(clusterView, params);

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
            Log.e(TAG, "Failed to mount cluster overlay", e);
            stopSelf();
        }
    }

    private void stopCluster() {
        if (!isRunning) return;
        vehicleProvider.stopListening();
        if (clusterWindowManager != null && clusterView != null) {
            try {
                clusterWindowManager.removeView(clusterView);
            } catch (Exception ignored) {}
            clusterView = null;
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
