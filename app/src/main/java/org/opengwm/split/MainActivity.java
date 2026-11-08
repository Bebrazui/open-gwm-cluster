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
import android.widget.Button;
import android.widget.SeekBar;
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
    private TextView tvDisplayList;
    private TextView tvVehicleTelemetry;
    private TextView tvDemoSpeedLabel;
    private TextView tvDemoRpmLabel;

    private Button btnToggleCluster;
    private Button btnGrantOverlay;
    private Button btnThemeSport;
    private Button btnThemeLuxury;
    private Button btnThemeCyber;
    private Button btnThemeOffroad;
    private Button btnAutoSweep;

    private Switch switchHolePunch;
    private Switch switchSmoothing;
    private Switch switchDemoMode;
    private SeekBar sbSpeed;
    private SeekBar sbRpm;

    private boolean isDemoActive = false;
    private ValueAnimator autoSweepAnimator = null;

    private int simSpeed = 0;
    private int simRpm = 0;
    private String simGear = "P";
    private String simMode = "STANDARD 4H";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupThemes();
        setupClusterControls();
        setupDemoSimulator();
        setupVehicleTelemetry();
    }

    private void initViews() {
        gaugePreview = findViewById(R.id.gaugePreview);
        tvStatus = findViewById(R.id.tvStatus);
        tvDisplayList = findViewById(R.id.tvDisplayList);
        tvVehicleTelemetry = findViewById(R.id.tvVehicleTelemetry);
        tvDemoSpeedLabel = findViewById(R.id.tvDemoSpeedLabel);
        tvDemoRpmLabel = findViewById(R.id.tvDemoRpmLabel);

        btnToggleCluster = findViewById(R.id.btnToggleCluster);
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay);
        btnThemeSport = findViewById(R.id.btnThemeSport);
        btnThemeLuxury = findViewById(R.id.btnThemeLuxury);
        btnThemeCyber = findViewById(R.id.btnThemeCyber);
        btnThemeOffroad = findViewById(R.id.btnThemeOffroad);
        btnAutoSweep = findViewById(R.id.btnAutoSweep);

        switchHolePunch = findViewById(R.id.switchHolePunch);
        switchSmoothing = findViewById(R.id.switchSmoothing);
        switchDemoMode = findViewById(R.id.switchDemoMode);
        sbSpeed = findViewById(R.id.sbSpeed);
        sbRpm = findViewById(R.id.sbRpm);

        switchHolePunch.setChecked(ClusterConfig.isHolePunchEnabled(this));
        switchSmoothing.setChecked(ClusterConfig.isSmoothingEnabled(this));
    }

    private void setupThemes() {
        View.OnClickListener themeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int theme = ClusterConfig.THEME_SPORT_RED;
                if (v.getId() == R.id.btnThemeLuxury) {
                    theme = ClusterConfig.THEME_LUXURY_GOLD;
                } else if (v.getId() == R.id.btnThemeCyber) {
                    theme = ClusterConfig.THEME_CYBER_NEON;
                } else if (v.getId() == R.id.btnThemeOffroad) {
                    theme = ClusterConfig.THEME_OFFROAD_AMBER;
                }

                gaugePreview.setThemeStyle(theme);
                InstrumentClusterService.applyConfig(MainActivity.this);
                updateThemeButtons(theme);
            }
        };

        btnThemeSport.setOnClickListener(themeClickListener);
        btnThemeLuxury.setOnClickListener(themeClickListener);
        btnThemeCyber.setOnClickListener(themeClickListener);
        btnThemeOffroad.setOnClickListener(themeClickListener);

        updateThemeButtons(ClusterConfig.getTheme(this));
    }

    private void updateThemeButtons(int selectedTheme) {
        btnThemeSport.setAlpha(selectedTheme == ClusterConfig.THEME_SPORT_RED ? 1.0f : 0.6f);
        btnThemeLuxury.setAlpha(selectedTheme == ClusterConfig.THEME_LUXURY_GOLD ? 1.0f : 0.6f);
        btnThemeCyber.setAlpha(selectedTheme == ClusterConfig.THEME_CYBER_NEON ? 1.0f : 0.6f);
        btnThemeOffroad.setAlpha(selectedTheme == ClusterConfig.THEME_OFFROAD_AMBER ? 1.0f : 0.6f);
    }

    private void setupClusterControls() {
        btnGrantOverlay.setOnClickListener(new View.OnClickListener() {
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
                    btnToggleCluster.setText("ЗАПУСТИТЬ ТЕМУ НА ПРИБОРКЕ (DISPLAY 2)");
                    btnToggleCluster.setBackgroundColor(0xFF1976D2);
                    tvStatus.setText("Приборка остановлена");
                } else {
                    Intent intent = new Intent(MainActivity.this, InstrumentClusterService.class);
                    intent.setAction(InstrumentClusterService.ACTION_START);
                    startService(intent);
                    btnToggleCluster.setText("ОСТАНОВИТЬ ТЕМУ ПРИБОРКИ");
                    btnToggleCluster.setBackgroundColor(0xFFD32F2F);
                    tvStatus.setText("Тема активна на Display 2!");
                }
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
    }

    private void setupDemoSimulator() {
        switchDemoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isDemoActive = isChecked;
            if (isChecked) {
                tvStatus.setText("Режим симулятора: используйте ползунки и кнопки передач");
                applyDemoValues();
            } else {
                if (autoSweepAnimator != null) {
                    autoSweepAnimator.cancel();
                }
                tvStatus.setText("Штатный режим: ожидание данных шины VHAL...");
            }
        });

        sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (!isDemoActive) {
                        switchDemoMode.setChecked(true);
                    }
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
                    if (!isDemoActive) {
                        switchDemoMode.setChecked(true);
                    }
                    simRpm = progress;
                    tvDemoRpmLabel.setText("Обороты: " + progress + " RPM");
                    applyDemoValues();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Gear Chips
        int[] gearBtnIds = {
                R.id.btnGearP, R.id.btnGearR, R.id.btnGearN,
                R.id.btnGearD1, R.id.btnGearD2, R.id.btnGearD3,
                R.id.btnGearD4, R.id.btnGearD5, R.id.btnGearD6,
                R.id.btnGearD7, R.id.btnGearD8, R.id.btnGearD9
        };
        for (int id : gearBtnIds) {
            Button btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    if (!isDemoActive) switchDemoMode.setChecked(true);
                    simGear = ((Button) v).getText().toString();
                    applyDemoValues();
                });
            }
        }

        // Mode Chips
        setupModeButton(R.id.btnModeStandard, "STANDARD 4H");
        setupModeButton(R.id.btnModeSport, "SPORT+ 4H");
        setupModeButton(R.id.btnMode4L, "4L ROCK CRAWL");
        setupModeButton(R.id.btnModeEco, "ECO HYBRID");
        setupModeButton(R.id.btnModeMud, "MUD / SAND");

        // Auto Sweep Button (0 -> 160 km/h Launch Control Demo)
        btnAutoSweep.setOnClickListener(v -> startAutoSweepDemo());
    }

    private void setupModeButton(int btnId, String modeName) {
        Button btn = findViewById(btnId);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                if (!isDemoActive) switchDemoMode.setChecked(true);
                simMode = modeName;
                applyDemoValues();
            });
        }
    }

    private void applyDemoValues() {
        gaugePreview.setManualValues(simSpeed, simRpm, simGear, simMode);
        if (InstrumentClusterService.activeClusterView != null) {
            InstrumentClusterService.activeClusterView.setManualValues(simSpeed, simRpm, simGear, simMode);
        }
        tvVehicleTelemetry.setText("Симулятор: " + simSpeed + " км/ч | " + simRpm + " RPM | " + simGear + " | " + simMode);
    }

    private void startAutoSweepDemo() {
        switchDemoMode.setChecked(true);
        if (autoSweepAnimator != null && autoSweepAnimator.isRunning()) {
            autoSweepAnimator.cancel();
        }

        autoSweepAnimator = ValueAnimator.ofFloat(0f, 1f);
        autoSweepAnimator.setDuration(9000);
        autoSweepAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        autoSweepAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();

            // Calculate realistic launch simulation
            float speed = progress * 175f;
            simSpeed = Math.round(speed);

            if (speed < 20) {
                simGear = "D1";
                simRpm = Math.round(1000 + (speed / 20f) * 3500);
            } else if (speed < 45) {
                simGear = "D2";
                simRpm = Math.round(2200 + ((speed - 20) / 25f) * 3200);
            } else if (speed < 75) {
                simGear = "D3";
                simRpm = Math.round(2300 + ((speed - 45) / 30f) * 3100);
            } else if (speed < 110) {
                simGear = "D4";
                simRpm = Math.round(2400 + ((speed - 75) / 35f) * 2800);
            } else if (speed < 145) {
                simGear = "D5";
                simRpm = Math.round(2300 + ((speed - 110) / 35f) * 2500);
            } else {
                simGear = "D6";
                simRpm = Math.round(2200 + ((speed - 145) / 30f) * 2000);
            }

            sbSpeed.setProgress(simSpeed);
            sbRpm.setProgress(simRpm);
            tvDemoSpeedLabel.setText("Скорость: " + simSpeed + " км/ч");
            tvDemoRpmLabel.setText("Обороты: " + simRpm + " RPM");
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
                        tvVehicleTelemetry.setText("Шина VHAL: " + state.speedKmh + " км/ч | " + state.rpm +
                                " RPM | " + state.gear + " | ОЖ: " + Math.round(state.targetTempC) + "°C");
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
        StringBuilder sb = new StringBuilder("Экраны (").append(displays.length).append("): ");
        for (Display d : displays) {
            sb.append("[").append(d.getDisplayId()).append(": ").append(d.getName()).append("] ");
        }
        tvDisplayList.setText(sb.toString().trim());
    }

    private void updateButtonLabels() {
        if (btnToggleCluster != null) {
            if (InstrumentClusterService.isServiceRunning) {
                btnToggleCluster.setText("ОСТАНОВИТЬ ТЕМУ ПРИБОРКИ");
                btnToggleCluster.setBackgroundColor(0xFFD32F2F);
            } else {
                btnToggleCluster.setText("ЗАПУСТИТЬ ТЕМУ НА ПРИБОРКЕ (DISPLAY 2)");
                btnToggleCluster.setBackgroundColor(0xFF1976D2);
            }
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
