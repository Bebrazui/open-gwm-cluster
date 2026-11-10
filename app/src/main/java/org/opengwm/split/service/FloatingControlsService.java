package org.opengwm.split.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import org.opengwm.split.R;
import org.opengwm.split.vehicle.MockVehicleProvider;
import org.opengwm.split.vehicle.VehicleDataProvider;
import org.opengwm.split.vehicle.VehicleState;

public class FloatingControlsService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private VehicleDataProvider vehicleProvider;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        vehicleProvider = new org.opengwm.split.vehicle.AndroidAutomotiveVehicleProvider(this);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        initOverlay();
        startTelemetry();
    }

    private void initOverlay() {
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_panel, null);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            @SuppressWarnings("deprecation")
            int deprecatedType = WindowManager.LayoutParams.TYPE_PHONE;
            layoutType = deprecatedType;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        setupDrag(floatingView.findViewById(R.id.btnDragHandle));
        setupActions(floatingView);

        windowManager.addView(floatingView, params);
    }

    private void setupDrag(View dragHandle) {
        if (dragHandle == null) return;
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (params == null) return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void setupActions(View root) {
        Button btnBack = root.findViewById(R.id.btnBack);
        Button btnHome = root.findViewById(R.id.btnHome);
        Button btnSplit = root.findViewById(R.id.btnSplit);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CarAccessibilityService.performBack();
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CarAccessibilityService.performHome();
            }
        });

        btnSplit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CarAccessibilityService.toggleSplitScreen();
            }
        });
    }

    private void startTelemetry() {
        final TextView tvSpeed = floatingView.findViewById(R.id.tvMiniSpeed);
        vehicleProvider.startListening(new VehicleDataProvider.Callback() {
            @Override
            public void onVehicleStateChanged(final VehicleState state) {
                if (tvSpeed != null) {
                    tvSpeed.post(new Runnable() {
                        @Override
                        public void run() {
                            tvSpeed.setText(state.speedKmh + " km/h [" + state.gear + "]");
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        vehicleProvider.stopListening();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }
}
