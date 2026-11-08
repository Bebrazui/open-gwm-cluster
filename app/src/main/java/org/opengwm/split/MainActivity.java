package org.opengwm.split;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.opengwm.split.service.FloatingControlsService;
import org.opengwm.split.service.InstrumentClusterService;
import org.opengwm.split.ui.ClusterGaugeView;
import org.opengwm.split.vehicle.MockVehicleProvider;
import org.opengwm.split.vehicle.VehicleDataProvider;
import org.opengwm.split.vehicle.VehicleState;

public class MainActivity extends Activity {

    private final VehicleDataProvider vehicleProvider = new org.opengwm.split.vehicle.AndroidAutomotiveVehicleProvider();
    private ClusterGaugeView gaugePreview;
    private TextView tvStatus;
    private TextView tvDisplayList;
    private TextView tvTelemetry;
    private Button btnToggleCluster;
    private Button btnToggleControls;

    private boolean isControlsRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gaugePreview = findViewById(R.id.gaugePreview);
        tvStatus = findViewById(R.id.tvStatus);
        tvDisplayList = findViewById(R.id.tvDisplayList);
        tvTelemetry = findViewById(R.id.tvVehicleTelemetry);

        Button btnOverlay = findViewById(R.id.btnGrantOverlay);
        btnToggleCluster = findViewById(R.id.btnToggleCluster);
        btnToggleControls = findViewById(R.id.btnToggleControls);

        btnOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                }
            }
        });

        btnToggleCluster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                    tvStatus.setText("Ошибка: сначала выдайте разрешение поверх других окон");
                    return;
                }

                if (InstrumentClusterService.isServiceRunning) {
                    Intent intent = new Intent(MainActivity.this, InstrumentClusterService.class);
                    intent.setAction(InstrumentClusterService.ACTION_STOP);
                    startService(intent);
                    btnToggleCluster.setText("Запустить тему на приборке (Display 2)");
                    tvStatus.setText("Приборка остановлена");
                } else {
                    Intent intent = new Intent(MainActivity.this, InstrumentClusterService.class);
                    intent.setAction(InstrumentClusterService.ACTION_START);
                    startService(intent);
                    btnToggleCluster.setText("Остановить тему приборки");
                    tvStatus.setText("Приборка запущена на Display 2");
                }
            }
        });

        btnToggleControls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FloatingControlsService.class);
                if (isControlsRunning) {
                    stopService(intent);
                    isControlsRunning = false;
                    btnToggleControls.setText("Плавающие кнопки на основном экране");
                } else {
                    startService(intent);
                    isControlsRunning = true;
                    btnToggleControls.setText("Скрыть плавающие кнопки");
                }
            }
        });

        // Start live telemetry feed to preview gauge and text indicator
        vehicleProvider.startListening(new VehicleDataProvider.Callback() {
            @Override
            public void onVehicleStateChanged(final VehicleState state) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (gaugePreview != null) {
                            gaugePreview.updateTelemetry(state);
                        }
                        if (tvTelemetry != null) {
                            tvTelemetry.setText("Telemetry: Speed: " + state.speedKmh + " km/h | RPM: " + state.rpm +
                                    " | Gear: " + state.gear + " | Temp: " + state.targetTempC + "°C");
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        scanDisplays();
        updateButtonLabels();
    }

    private void scanDisplays() {
        DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) return;

        Display[] displays = dm.getDisplays();
        StringBuilder sb = new StringBuilder("Displays (").append(displays.length).append("): ");
        for (Display d : displays) {
            sb.append("[").append(d.getDisplayId()).append(":").append(d.getName()).append("] ");
        }
        tvDisplayList.setText(sb.toString().trim());
    }

    private void updateButtonLabels() {
        if (btnToggleCluster != null) {
            btnToggleCluster.setText(InstrumentClusterService.isServiceRunning ?
                    "Остановить тему приборки" : "Запустить тему на приборке (Display 2)");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        vehicleProvider.stopListening();
    }
}
