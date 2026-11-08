package org.opengwm.split;

import android.animation.ValueAnimator;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import org.opengwm.split.service.InstrumentClusterService;
import org.opengwm.split.ui.ClusterConfig;
import org.opengwm.split.ui.ClusterGaugeView;
import org.opengwm.split.vehicle.AndroidAutomotiveVehicleProvider;
import org.opengwm.split.vehicle.VehicleDataProvider;
import org.opengwm.split.vehicle.VehicleState;

public class MainActivity extends Activity {

    private VehicleDataProvider vehicleProvider;
    private ClusterGaugeView gaugePreview;
    private TextView tvStatus;
    private TextView tvStatusChip;
    private TextView tvDisplayList;
    private TextView tvVehicleTelemetry;
    private TextView tvDemoSpeedLabel;
    private TextView tvDemoRpmLabel;

    private Button btnToggleCluster;
    private Button btnGrantOverlay;
    private Button btnAutoSweep;

    private Spinner spinnerTheme;
    private Spinner spinnerGear;
    private Spinner spinnerMode;

    private Switch switchHolePunch;
    private Switch switchSmoothing;
    private Switch switchDemoMode;
    private LinearLayout layoutDemoControls;
    private SeekBar sbSpeed;
    private SeekBar sbRpm;

    private boolean isDemoActive = false;
    private ValueAnimator autoSweepAnimator = null;

    private int simSpeed = 0;
    private int simRpm = 0;
    private String simGear = "P";
    private String simMode = "STANDARD 4H";

    private final String[] themeNames = {
            "Спорт (Redline)",
            "Классика (Luxury Gold)",
            "Кибернетика (Cyber Neon)",
            "Внедорожный (4x4 Amber)"
    };

    private final String[] gearOptions = {
            "P", "R", "N", "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9"
    };

    private final String[] modeOptions = {
            "STANDARD 4H", "SPORT+ 4H", "4L ROCK CRAWL", "ECO HYBRID", "MUD / SAND"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinners();
        setupControls();
        setupVehicleTelemetry();
    }

    private void initViews() {
        gaugePreview = findViewById(R.id.gaugePreview);
        tvStatus = findViewById(R.id.tvStatus);
        tvStatusChip = findViewById(R.id.tvStatusChip);
        tvDisplayList = findViewById(R.id.tvDisplayList);
        tvVehicleTelemetry = findViewById(R.id.tvVehicleTelemetry);
        tvDemoSpeedLabel = findViewById(R.id.tvDemoSpeedLabel);
        tvDemoRpmLabel = findViewById(R.id.tvDemoRpmLabel);

        btnToggleCluster = findViewById(R.id.btnToggleCluster);
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay);
        btnAutoSweep = findViewById(R.id.btnAutoSweep);

        spinnerTheme = findViewById(R.id.spinnerTheme);
        spinnerGear = findViewById(R.id.spinnerGear);
        spinnerMode = findViewById(R.id.spinnerMode);

        switchHolePunch = findViewById(R.id.switchHolePunch);
        switchSmoothing = findViewById(R.id.switchSmoothing);
        switchDemoMode = findViewById(R.id.switchDemoMode);
        layoutDemoControls = findViewById(R.id.layoutDemoControls);
        sbSpeed = findViewById(R.id.sbSpeed);
        sbRpm = findViewById(R.id.sbRpm);

        switchHolePunch.setChecked(ClusterConfig.isHolePunchEnabled(this));
        switchSmoothing.setChecked(ClusterConfig.isSmoothingEnabled(this));
    }

    private void setupSpinners() {
        // Theme Spinner
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, themeNames);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(themeAdapter);
        spinnerTheme.setSelection(ClusterConfig.getTheme(this));

        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                gaugePreview.setThemeStyle(position);
                InstrumentClusterService.applyConfig(MainActivity.this);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Gear Spinner
        ArrayAdapter<String> gearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, gearOptions);
        gearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGear.setAdapter(gearAdapter);
        spinnerGear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                simGear = gearOptions[position];
                applyDemoValues();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Mode Spinner
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modeOptions);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(modeAdapter);
        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                simMode = modeOptions[position];
                applyDemoValues();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupControls() {
        btnGrantOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            }
        });

        btnToggleCluster.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                tvStatus.setText("Требуется разрешение на отображение поверх других окон");
                return;
            }

            if (InstrumentClusterService.isServiceRunning) {
                Intent intent = new Intent(MainActivity.this, InstrumentClusterService.class);
                intent.setAction(InstrumentClusterService.ACTION_STOP);
                startService(intent);
                updateButtonLabels();
            } else {
                Intent intent = new Intent(MainActivity.this, InstrumentClusterService.class);
                intent.setAction(InstrumentClusterService.ACTION_START);
                startService(intent);
                updateButtonLabels();
            }
        });

        switchHolePunch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            gaugePreview.setHolePunch(isChecked);
            InstrumentClusterService.applyConfig(MainActivity.this);
        });

        switchSmoothing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            gaugePreview.setSmoothing(isChecked);
            InstrumentClusterService.applyConfig(MainActivity.this);
        });

        // Demo Simulator Controls
        switchDemoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isDemoActive = isChecked;
            layoutDemoControls.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                applyDemoValues();
            } else {
                if (autoSweepAnimator != null) {
                    autoSweepAnimator.cancel();
                }
            }
        });

        sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    simSpeed = progress;
                    tvDemoSpeedLabel.setText("Скорость: " + progress + " км/ч");
                    applyDemoValues();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbRpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    simRpm = progress;
                    tvDemoRpmLabel.setText("Обороты: " + progress + " об/мин");
                    applyDemoValues();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnAutoSweep.setOnClickListener(v -> startAutoSweepDemo());
    }

    private void applyDemoValues() {
        if (!isDemoActive) return;
        gaugePreview.setManualValues(simSpeed, simRpm, simGear, simMode);
        if (InstrumentClusterService.activeClusterView != null) {
            InstrumentClusterService.activeClusterView.setManualValues(simSpeed, simRpm, simGear, simMode);
        }
        tvVehicleTelemetry.setText("Симуляция: " + simSpeed + " км/ч | " + simRpm + " об/мин | " + simGear + " | " + simMode);
    }

    private void startAutoSweepDemo() {
        if (autoSweepAnimator != null && autoSweepAnimator.isRunning()) {
            autoSweepAnimator.cancel();
        }

        autoSweepAnimator = ValueAnimator.ofFloat(0f, 1f);
        autoSweepAnimator.setDuration(8000);
        autoSweepAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        autoSweepAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            float speed = progress * 165f;
            simSpeed = Math.round(speed);

            if (speed < 20) {
                simGear = "D1";
                simRpm = Math.round(900 + (speed / 20f) * 3400);
            } else if (speed < 45) {
                simGear = "D2";
                simRpm = Math.round(2100 + ((speed - 20) / 25f) * 3100);
            } else if (speed < 75) {
                simGear = "D3";
                simRpm = Math.round(2200 + ((speed - 45) / 30f) * 2900);
            } else if (speed < 110) {
                simGear = "D4";
                simRpm = Math.round(2300 + ((speed - 75) / 35f) * 2700);
            } else if (speed < 145) {
                simGear = "D5";
                simRpm = Math.round(2200 + ((speed - 110) / 35f) * 2400);
            } else {
                simGear = "D6";
                simRpm = Math.round(2100 + ((speed - 145) / 20f) * 2000);
            }

            sbSpeed.setProgress(simSpeed);
            sbRpm.setProgress(simRpm);
            tvDemoSpeedLabel.setText("Скорость: " + simSpeed + " км/ч");
            tvDemoRpmLabel.setText("Обороты: " + simRpm + " об/мин");
            applyDemoValues();
        });
        autoSweepAnimator.start();
    }

    private void setupVehicleTelemetry() {
        vehicleProvider = new AndroidAutomotiveVehicleProvider(this);
        vehicleProvider.startListening(new VehicleDataProvider.Callback() {
            @Override
            public void onVehicleStateChanged(final VehicleState state) {
                runOnUiThread(() -> {
                    if (!isDemoActive) {
                        gaugePreview.updateTelemetry(state);
                        tvVehicleTelemetry.setText("Шина: " + state.speedKmh + " км/ч | " + state.rpm +
                                " об/мин | " + state.gear + " | ОЖ: " + Math.round(state.targetTempC) + " °C");
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
        StringBuilder sb = new StringBuilder();
        boolean hasSecondary = false;
        for (Display d : displays) {
            sb.append("[").append(d.getDisplayId()).append(": ").append(d.getName()).append("] ");
            if (d.getDisplayId() != Display.DEFAULT_DISPLAY) {
                hasSecondary = true;
            }
        }
        tvDisplayList.setText("Экраны (" + displays.length + "): " + sb.toString().trim());
        tvStatusChip.setText(hasSecondary ? "Вторичный экран доступен" : "Ожидание экрана");
    }

    private void updateButtonLabels() {
        if (btnToggleCluster != null) {
            boolean running = InstrumentClusterService.isServiceRunning;
            btnToggleCluster.setText(running ? "Остановить панель приборов" : "Запустить на приборной панели");
            tvStatus.setText(running ? "Служба активна: трансляция на вторичный дисплей" :
                    "Трансляция шкал на экран приборной панели (Display ID 2)");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoSweepAnimator != null) {
            autoSweepAnimator.cancel();
        }
        if (vehicleProvider != null) {
            vehicleProvider.stopListening();
        }
    }
}
