package org.opengwm.split;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.util.DisplayMetrics;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.opengwm.split.service.InstrumentClusterService;
import org.opengwm.split.ui.ClusterConfig;
import org.opengwm.split.ui.ClusterGaugeView;
import org.opengwm.split.util.AppLogger;
import org.opengwm.split.vehicle.AndroidAutomotiveVehicleProvider;
import org.opengwm.split.vehicle.CanBusSimulator;
import org.opengwm.split.vehicle.TrafficRecorderReplay;
import org.opengwm.split.vehicle.VehicleDataProvider;
import android.content.pm.PackageManager;
import org.opengwm.split.vehicle.VehicleState;
import java.io.File;
import java.util.List;

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

    private TextView chipTargetAll;
    private TextView chipTarget2;
    private TextView chipTarget1;
    private TextView chipTarget0;

    // CAN Bus Simulator (Способ А)
    private TextView tvCanSimStatus;
    private Button btnCanSimDrive;
    private Button btnCanSimFault;
    private Button btnCanSimStress;
    private Button btnCanSimStop;

    // Traffic Recorder & Replay Engine
    private TextView tvTrafficRecorderStatus;
    private Button btnTrafficRecordToggle;
    private Button btnTrafficReplayToggle;
    private Button btnTrafficInjectMock;
    private int mockStepCounter = 0;

    private Spinner spinnerTheme;
    private Button btnPrevTheme;
    private Button btnNextTheme;
    private TextView tvCurrentThemeName;
    private TextView tvThemeCounter;
    private int currentThemeIndex = 0;
    private Spinner spinnerGear;
    private Spinner spinnerMode;
    private Spinner spinnerWidgetLeft;
    private Spinner spinnerWidgetRight;
    private Spinner spinnerWidgetCenter;

    // Cluster Overall Background Stepper
    private Button btnPrevClusterBg;
    private Button btnNextClusterBg;
    private TextView tvCurrentClusterBgName;
    private TextView tvClusterBgCounter;
    private Spinner spinnerClusterBg;
    private View containerCustomBgPick;
    private TextView tvCustomBgPath;
    private Button btnSelectCustomBg;
    private int currentClusterBgIndex = 0;
    private static final int REQUEST_CODE_PICK_BG = 1002;

    // Player Theme & Background Steppers
    private Button btnPrevPlayerTheme;
    private Button btnNextPlayerTheme;
    private TextView tvCurrentPlayerThemeName;
    private TextView tvPlayerThemeCounter;
    private Spinner spinnerPlayerTheme;
    private int currentPlayerThemeIndex = 0;

    private Button btnPrevPlayerBg;
    private Button btnNextPlayerBg;
    private TextView tvCurrentPlayerBgName;
    private TextView tvPlayerBgCounter;
    private Spinner spinnerPlayerBg;
    private int currentPlayerBgIndex = 0;

    private Button btnGoToThemesTab;
    private Switch switchPlayerAlways;
    private TextView tvPlayerPermissionStatus;
    private Button btnPlayerNotificationPermission;
    private Button btnPlayerDemoTrack;

    private TextView tabMain;
    private TextView tabThemes;
    private TextView tabWidgets;
    private TextView tabBus;
    private TextView tvLiveSensorStats;

    private LinearLayout containerTabMain;
    private LinearLayout containerTabThemes;
    private LinearLayout containerTabWidgets;
    private LinearLayout containerTabBus;
    private TextView tabEditor;
    private LinearLayout containerTabEditor;
    private Button btnOpenEditor;

    private TextView tabLogs;
    private LinearLayout containerTabLogs;
    private Button btnOpenLogsQuick;

    // Diagnostics & Log Views
    private TextView tvDiagCarServiceStatus;
    private TextView tvDiagVhalStatus;
    private TextView tvDiagDisplaysStatus;
    private TextView tvDiagOverlayStatus;
    private TextView tvDiagSocketCanStatus;
    private Button btnRefreshDiagStatus;
    private Button btnProbeVhalNow;
    private Button btnCopyLogs;
    private Button btnExportLogsFile;
    private Button btnClearLogs;
    private TextView chipFilterAll;
    private TextView chipFilterErrors;
    private TextView chipFilterBus;
    private TextView chipFilterDisplay;
    private TextView tvLogCounter;
    private ScrollView scrollLogTerminal;
    private TextView tvLogOutput;
    private int currentLogFilter = 0; // 0=All, 1=Errors, 2=Bus/VHAL, 3=Display

    // Interactive Layout Customization Elements
    private TextView chipElemLeftGauge;
    private TextView chipElemRightGauge;
    private TextView chipElemCenterBlock;
    private TextView chipElemWidgetLeft;
    private TextView chipElemWidgetRight;
    private TextView chipElemWidgetCenter;
    private TextView chipElemMediaPlayer;
    private TextView chipElemTopHeader;
    private TextView chipElemBottomFooter;
    private TextView tvEditorActiveElementTitle;

    private TextView tvEditorValX;
    private TextView tvEditorValY;
    private TextView tvEditorValScale;
    private TextView tvEditorValRot;
    private SeekBar sbEditorX;
    private SeekBar sbEditorY;
    private SeekBar sbEditorScale;
    private SeekBar sbEditorRot;
    private Button btnDecX, btnIncX;
    private Button btnDecY, btnIncY;
    private Button btnDecScale, btnIncScale;
    private Button btnDecRot, btnIncRot;

    private Button btnSaveLayout;
    private Button btnResetElement;
    private Button btnResetAllElements;
    private Button btnFullscreenEditor;

    private int currentEditElement = ClusterConfig.ELEM_LEFT_GAUGE;
    private boolean isUpdatingSlidersFromTouch = false;

    private Switch switchHolePunch;
    private Switch switchHolePunchMain;
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

    private final java.util.List<String> themeDisplayNames = new java.util.ArrayList<>();
    private final java.util.List<Integer> themeValues = new java.util.ArrayList<>();

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

        checkAndRequestStoragePermission();
        initViews();
        setupTabNavigation();
        setupEditorControls();
        setupSpinners();
        setupWidgetSpinners();
        setupMediaPlayerControls();
        setupClusterBackgroundControls();
        setupControls();
        setupTargetDisplayControls();
        setupVehicleTelemetry();
        setupDiagnosticsControls();
        AppLogger.i("MainActivity", "Приложение запущено. Устройство: " + Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ", API " + Build.VERSION.SDK_INT + ")");
    }

    private void initViews() {
        gaugePreview = findViewById(R.id.gaugePreview);
        tvStatus = findViewById(R.id.tvStatus);
        tvStatusChip = findViewById(R.id.tvStatusChip);
        tvDisplayList = findViewById(R.id.tvDisplayList);
        tvVehicleTelemetry = findViewById(R.id.tvVehicleTelemetry);
        tvDemoSpeedLabel = findViewById(R.id.tvDemoSpeedLabel);
        tvDemoRpmLabel = findViewById(R.id.tvDemoRpmLabel);
        tvLiveSensorStats = findViewById(R.id.tvLiveSensorStats);

        tabMain = findViewById(R.id.tabMain);
        tabThemes = findViewById(R.id.tabThemes);
        tabWidgets = findViewById(R.id.tabWidgets);
        tabBus = findViewById(R.id.tabBus);
        tabEditor = findViewById(R.id.tabEditor);
        tabLogs = findViewById(R.id.tabLogs);

        containerTabMain = findViewById(R.id.containerTabMain);
        containerTabThemes = findViewById(R.id.containerTabThemes);
        containerTabWidgets = findViewById(R.id.containerTabWidgets);
        containerTabBus = findViewById(R.id.containerTabBus);
        containerTabEditor = findViewById(R.id.containerTabEditor);
        containerTabLogs = findViewById(R.id.containerTabLogs);

        btnOpenEditor = findViewById(R.id.btnOpenEditor);
        if (btnOpenEditor != null) {
            btnOpenEditor.setOnClickListener(v -> selectTab(4));
        }

        btnOpenLogsQuick = findViewById(R.id.btnOpenLogsQuick);
        if (btnOpenLogsQuick != null) {
            btnOpenLogsQuick.setOnClickListener(v -> selectTab(5));
        }

        tvDiagCarServiceStatus = findViewById(R.id.tvDiagCarServiceStatus);
        tvDiagVhalStatus = findViewById(R.id.tvDiagVhalStatus);
        tvDiagDisplaysStatus = findViewById(R.id.tvDiagDisplaysStatus);
        tvDiagOverlayStatus = findViewById(R.id.tvDiagOverlayStatus);
        tvDiagSocketCanStatus = findViewById(R.id.tvDiagSocketCanStatus);
        btnRefreshDiagStatus = findViewById(R.id.btnRefreshDiagStatus);
        btnProbeVhalNow = findViewById(R.id.btnProbeVhalNow);
        btnCopyLogs = findViewById(R.id.btnCopyLogs);
        btnExportLogsFile = findViewById(R.id.btnExportLogsFile);
        btnClearLogs = findViewById(R.id.btnClearLogs);
        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterErrors = findViewById(R.id.chipFilterErrors);
        chipFilterBus = findViewById(R.id.chipFilterBus);
        chipFilterDisplay = findViewById(R.id.chipFilterDisplay);
        tvLogCounter = findViewById(R.id.tvLogCounter);
        scrollLogTerminal = findViewById(R.id.scrollLogTerminal);
        tvLogOutput = findViewById(R.id.tvLogOutput);

        btnToggleCluster = findViewById(R.id.btnToggleCluster);
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay);
        btnAutoSweep = findViewById(R.id.btnAutoSweep);

        chipTargetAll = findViewById(R.id.chipTargetAll);
        chipTarget2 = findViewById(R.id.chipTarget2);
        chipTarget1 = findViewById(R.id.chipTarget1);
        chipTarget0 = findViewById(R.id.chipTarget0);

        tvCanSimStatus = findViewById(R.id.tvCanSimStatus);
        btnCanSimDrive = findViewById(R.id.btnCanSimDrive);
        btnCanSimFault = findViewById(R.id.btnCanSimFault);
        btnCanSimStress = findViewById(R.id.btnCanSimStress);
        btnCanSimStop = findViewById(R.id.btnCanSimStop);

        tvTrafficRecorderStatus = findViewById(R.id.tvTrafficRecorderStatus);
        btnTrafficRecordToggle = findViewById(R.id.btnTrafficRecordToggle);
        btnTrafficReplayToggle = findViewById(R.id.btnTrafficReplayToggle);
        btnTrafficInjectMock = findViewById(R.id.btnTrafficInjectMock);

        spinnerTheme = findViewById(R.id.spinnerTheme);
        btnPrevTheme = findViewById(R.id.btnPrevTheme);
        btnNextTheme = findViewById(R.id.btnNextTheme);
        tvCurrentThemeName = findViewById(R.id.tvCurrentThemeName);
        tvThemeCounter = findViewById(R.id.tvThemeCounter);
        spinnerGear = findViewById(R.id.spinnerGear);
        spinnerMode = findViewById(R.id.spinnerMode);
        spinnerWidgetLeft = findViewById(R.id.spinnerWidgetLeft);
        spinnerWidgetRight = findViewById(R.id.spinnerWidgetRight);
        spinnerWidgetCenter = findViewById(R.id.spinnerWidgetCenter);

        // Cluster Background Stepper
        btnPrevClusterBg = findViewById(R.id.btnPrevClusterBg);
        btnNextClusterBg = findViewById(R.id.btnNextClusterBg);
        tvCurrentClusterBgName = findViewById(R.id.tvCurrentClusterBgName);
        tvClusterBgCounter = findViewById(R.id.tvClusterBgCounter);
        spinnerClusterBg = findViewById(R.id.spinnerClusterBg);
        containerCustomBgPick = findViewById(R.id.containerCustomBgPick);
        tvCustomBgPath = findViewById(R.id.tvCustomBgPath);
        btnSelectCustomBg = findViewById(R.id.btnSelectCustomBg);

        // Player Theme & Background Steppers
        btnPrevPlayerTheme = findViewById(R.id.btnPrevPlayerTheme);
        btnNextPlayerTheme = findViewById(R.id.btnNextPlayerTheme);
        tvCurrentPlayerThemeName = findViewById(R.id.tvCurrentPlayerThemeName);
        tvPlayerThemeCounter = findViewById(R.id.tvPlayerThemeCounter);
        spinnerPlayerTheme = findViewById(R.id.spinnerPlayerTheme);

        btnPrevPlayerBg = findViewById(R.id.btnPrevPlayerBg);
        btnNextPlayerBg = findViewById(R.id.btnNextPlayerBg);
        tvCurrentPlayerBgName = findViewById(R.id.tvCurrentPlayerBgName);
        tvPlayerBgCounter = findViewById(R.id.tvPlayerBgCounter);
        spinnerPlayerBg = findViewById(R.id.spinnerPlayerBg);

        btnGoToThemesTab = findViewById(R.id.btnGoToThemesTab);
        switchPlayerAlways = findViewById(R.id.switchPlayerAlways);
        tvPlayerPermissionStatus = findViewById(R.id.tvPlayerPermissionStatus);
        btnPlayerNotificationPermission = findViewById(R.id.btnPlayerNotificationPermission);
        btnPlayerDemoTrack = findViewById(R.id.btnPlayerDemoTrack);

        switchHolePunch = findViewById(R.id.switchHolePunch);
        switchHolePunchMain = findViewById(R.id.switchHolePunchMain);
        switchSmoothing = findViewById(R.id.switchSmoothing);
        switchDemoMode = findViewById(R.id.switchDemoMode);
        layoutDemoControls = findViewById(R.id.layoutDemoControls);
        sbSpeed = findViewById(R.id.sbSpeed);
        sbRpm = findViewById(R.id.sbRpm);

        boolean holePunchActive = ClusterConfig.isHolePunchEnabled(this);
        if (switchHolePunch != null) switchHolePunch.setChecked(holePunchActive);
        if (switchHolePunchMain != null) switchHolePunchMain.setChecked(holePunchActive);
        if (switchSmoothing != null) switchSmoothing.setChecked(ClusterConfig.isSmoothingEnabled(this));
    }

    private void setupTabNavigation() {
        tabMain.setOnClickListener(v -> selectTab(0));
        tabThemes.setOnClickListener(v -> selectTab(1));
        tabWidgets.setOnClickListener(v -> selectTab(2));
        tabBus.setOnClickListener(v -> selectTab(3));
        if (tabEditor != null) {
            tabEditor.setOnClickListener(v -> selectTab(4));
        }
        if (tabLogs != null) {
            tabLogs.setOnClickListener(v -> selectTab(5));
        }
        selectTab(0);
    }

    private void selectTab(int index) {
        TextView[] tabs = {tabMain, tabThemes, tabWidgets, tabBus, tabEditor, tabLogs};
        LinearLayout[] containers = {containerTabMain, containerTabThemes, containerTabWidgets, containerTabBus, containerTabEditor, containerTabLogs};

        for (int i = 0; i < tabs.length; i++) {
            boolean active = (i == index);
            if (tabs[i] != null) {
                tabs[i].setBackgroundResource(active ? R.drawable.m3_pill_active : R.drawable.m3_pill_inactive);
                tabs[i].setTextColor(Color.parseColor(active ? "#D3E3FD" : "#8B949E"));
            }
            if (containers[i] != null) {
                containers[i].setVisibility(active ? View.VISIBLE : View.GONE);
            }
        }

        if (gaugePreview != null) {
            gaugePreview.setEditMode(index == 4, currentEditElement);
        }

        if (index == 5) {
            updateDiagnosticSummary();
            renderLogs();
        }
    }

    private void setupWidgetSpinners() {
        ArrayAdapter<String> widgetAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ClusterConfig.WIDGET_TITLES);
        widgetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerWidgetLeft.setAdapter(widgetAdapter);
        spinnerWidgetRight.setAdapter(widgetAdapter);
        spinnerWidgetCenter.setAdapter(widgetAdapter);

        spinnerWidgetLeft.setSelection(ClusterConfig.getWidgetLeft(this));
        spinnerWidgetRight.setSelection(ClusterConfig.getWidgetRight(this));
        spinnerWidgetCenter.setSelection(ClusterConfig.getWidgetCenter(this));

        AdapterView.OnItemSelectedListener widgetListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int left = spinnerWidgetLeft.getSelectedItemPosition();
                int right = spinnerWidgetRight.getSelectedItemPosition();
                int center = spinnerWidgetCenter.getSelectedItemPosition();

                gaugePreview.setWidgets(left, right, center);
                InstrumentClusterService.applyConfig(MainActivity.this);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerWidgetLeft.setOnItemSelectedListener(widgetListener);
        spinnerWidgetRight.setOnItemSelectedListener(widgetListener);
        spinnerWidgetCenter.setOnItemSelectedListener(widgetListener);
    }

    private void selectClusterBgByIndex(int index) {
        int count = ClusterConfig.CLUSTER_BG_NAMES.length;
        if (index < 0) index = count - 1;
        if (index >= count) index = 0;
        currentClusterBgIndex = index;

        String name = ClusterConfig.CLUSTER_BG_NAMES[index];
        if (tvCurrentClusterBgName != null) {
            tvCurrentClusterBgName.setText(name);
        }
        if (tvClusterBgCounter != null) {
            tvClusterBgCounter.setText((index + 1) + " / " + count);
        }
        if (spinnerClusterBg != null && spinnerClusterBg.getSelectedItemPosition() != index) {
            spinnerClusterBg.setSelection(index, false);
        }

        if (gaugePreview != null) {
            gaugePreview.setClusterBg(index);
        }
        ClusterConfig.setClusterBg(this, index);
        InstrumentClusterService.applyConfig(this);

        if (containerCustomBgPick != null) {
            containerCustomBgPick.setVisibility(index == ClusterConfig.CLUSTER_BG_CUSTOM ? View.VISIBLE : View.GONE);
        }
    }

    private void selectPlayerThemeByIndex(int index) {
        int count = ClusterConfig.PLAYER_THEME_NAMES.length;
        if (index < 0) index = count - 1;
        if (index >= count) index = 0;
        currentPlayerThemeIndex = index;

        String name = ClusterConfig.PLAYER_THEME_NAMES[index];
        if (tvCurrentPlayerThemeName != null) {
            tvCurrentPlayerThemeName.setText(name);
        }
        if (tvPlayerThemeCounter != null) {
            tvPlayerThemeCounter.setText((index + 1) + " / " + count);
        }
        if (spinnerPlayerTheme != null && spinnerPlayerTheme.getSelectedItemPosition() != index) {
            spinnerPlayerTheme.setSelection(index, false);
        }

        if (gaugePreview != null) {
            gaugePreview.setPlayerTheme(index);
        }
        ClusterConfig.setPlayerTheme(this, index);
        InstrumentClusterService.applyConfig(this);
    }

    private void selectPlayerBgByIndex(int index) {
        int count = ClusterConfig.PLAYER_BG_NAMES.length;
        if (index < 0) index = count - 1;
        if (index >= count) index = 0;
        currentPlayerBgIndex = index;

        String name = ClusterConfig.PLAYER_BG_NAMES[index];
        if (tvCurrentPlayerBgName != null) {
            tvCurrentPlayerBgName.setText(name);
        }
        if (tvPlayerBgCounter != null) {
            tvPlayerBgCounter.setText((index + 1) + " / " + count);
        }
        if (spinnerPlayerBg != null && spinnerPlayerBg.getSelectedItemPosition() != index) {
            spinnerPlayerBg.setSelection(index, false);
        }

        if (gaugePreview != null) {
            gaugePreview.setPlayerBg(index);
        }
        ClusterConfig.setPlayerBg(this, index);
        InstrumentClusterService.applyConfig(this);
    }

    private void setupMediaPlayerControls() {
        // 1. Player Theme Stepper & Spinner
        if (spinnerPlayerTheme != null) {
            ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, ClusterConfig.PLAYER_THEME_NAMES);
            themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPlayerTheme.setAdapter(themeAdapter);

            spinnerPlayerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != currentPlayerThemeIndex) {
                        selectPlayerThemeByIndex(position);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        int initPlayerTheme = ClusterConfig.getPlayerTheme(this);
        selectPlayerThemeByIndex(initPlayerTheme);

        if (btnPrevPlayerTheme != null) {
            btnPrevPlayerTheme.setOnClickListener(v -> selectPlayerThemeByIndex(currentPlayerThemeIndex - 1));
        }
        if (btnNextPlayerTheme != null) {
            btnNextPlayerTheme.setOnClickListener(v -> selectPlayerThemeByIndex(currentPlayerThemeIndex + 1));
        }
        if (tvCurrentPlayerThemeName != null && spinnerPlayerTheme != null) {
            tvCurrentPlayerThemeName.setOnClickListener(v -> spinnerPlayerTheme.performClick());
        }

        // 2. Player Background Stepper & Spinner
        if (spinnerPlayerBg != null) {
            ArrayAdapter<String> bgAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, ClusterConfig.PLAYER_BG_NAMES);
            bgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPlayerBg.setAdapter(bgAdapter);

            spinnerPlayerBg.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != currentPlayerBgIndex) {
                        selectPlayerBgByIndex(position);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        int initPlayerBg = ClusterConfig.getPlayerBg(this);
        selectPlayerBgByIndex(initPlayerBg);

        if (btnPrevPlayerBg != null) {
            btnPrevPlayerBg.setOnClickListener(v -> selectPlayerBgByIndex(currentPlayerBgIndex - 1));
        }
        if (btnNextPlayerBg != null) {
            btnNextPlayerBg.setOnClickListener(v -> selectPlayerBgByIndex(currentPlayerBgIndex + 1));
        }
        if (tvCurrentPlayerBgName != null && spinnerPlayerBg != null) {
            tvCurrentPlayerBgName.setOnClickListener(v -> spinnerPlayerBg.performClick());
        }

        // 3. Link from Tab 2 to Tab 1
        if (btnGoToThemesTab != null) {
            btnGoToThemesTab.setOnClickListener(v -> selectTab(1));
        }

        // 4. Switches and Test buttons
        if (switchPlayerAlways != null) {
            switchPlayerAlways.setChecked(ClusterConfig.isPlayerAlwaysVisible(this));
            switchPlayerAlways.setOnCheckedChangeListener((btn, isChecked) -> {
                ClusterConfig.setPlayerAlwaysVisible(MainActivity.this, isChecked);
                if (gaugePreview != null) gaugePreview.invalidate();
                InstrumentClusterService.applyConfig(MainActivity.this);
            });
        }

        if (btnPlayerNotificationPermission != null) {
            btnPlayerNotificationPermission.setOnClickListener(v -> {
                try {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                } catch (Throwable t) {
                    Toast.makeText(MainActivity.this, "Не удалось открыть настройки: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnPlayerDemoTrack != null) {
            btnPlayerDemoTrack.setOnClickListener(v -> {
                org.opengwm.split.media.MediaTracker.getInstance().loadDemoTrack();
                if (gaugePreview != null) gaugePreview.invalidate();
                Toast.makeText(MainActivity.this, "Демо-трек загружен: Рок-н-Ролла нет — Миша Бурый", Toast.LENGTH_SHORT).show();
            });
        }

        updatePlayerPermissionStatus();
    }

    private void updatePlayerPermissionStatus() {
        if (tvPlayerPermissionStatus == null) return;
        boolean granted = org.opengwm.split.media.MediaTracker.isNotificationAccessGranted(this);
        if (granted) {
            tvPlayerPermissionStatus.setText("Разрешено");
            tvPlayerPermissionStatus.setTextColor(Color.parseColor("#3FB950"));
        } else {
            tvPlayerPermissionStatus.setText("Требуется разрешение");
            tvPlayerPermissionStatus.setTextColor(Color.parseColor("#F85149"));
        }
    }

    private void setupClusterBackgroundControls() {
        if (spinnerClusterBg != null) {
            ArrayAdapter<String> clusterBgAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, ClusterConfig.CLUSTER_BG_NAMES);
            clusterBgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerClusterBg.setAdapter(clusterBgAdapter);

            spinnerClusterBg.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != currentClusterBgIndex) {
                        selectClusterBgByIndex(position);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        int initClusterBg = ClusterConfig.getClusterBg(this);
        selectClusterBgByIndex(initClusterBg);

        if (btnPrevClusterBg != null) {
            btnPrevClusterBg.setOnClickListener(v -> selectClusterBgByIndex(currentClusterBgIndex - 1));
        }
        if (btnNextClusterBg != null) {
            btnNextClusterBg.setOnClickListener(v -> selectClusterBgByIndex(currentClusterBgIndex + 1));
        }
        if (tvCurrentClusterBgName != null && spinnerClusterBg != null) {
            tvCurrentClusterBgName.setOnClickListener(v -> spinnerClusterBg.performClick());
        }

        if (tvCustomBgPath != null) {
            tvCustomBgPath.setText(ClusterConfig.getClusterBgCustomPath(this));
        }
        if (btnSelectCustomBg != null) {
            btnSelectCustomBg.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Выберите фоновое изображение"), REQUEST_CODE_PICK_BG);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_BG && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                java.io.InputStream in = getContentResolver().openInputStream(uri);
                if (in != null) {
                    java.io.File dest = new java.io.File(getExternalFilesDir(null), "custom_cluster_bg.png");
                    java.io.FileOutputStream out = new java.io.FileOutputStream(dest);
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    String path = dest.getAbsolutePath();
                    ClusterConfig.setClusterBgCustomPath(this, path);
                    if (tvCustomBgPath != null) tvCustomBgPath.setText(path);
                    if (gaugePreview != null) {
                        gaugePreview.setClusterBgCustomPath(path);
                    }
                    InstrumentClusterService.applyConfig(this);
                    Toast.makeText(this, "Фоновое изображение установлено!", Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable t) {
                AppLogger.w("MainActivity", "Ошибка загрузки фона: " + t.getMessage());
                Toast.makeText(this, "Ошибка загрузки файла: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectThemeByIndex(int index) {
        if (themeValues == null || themeValues.isEmpty()) return;
        if (index < 0) {
            index = themeValues.size() - 1;
        } else if (index >= themeValues.size()) {
            index = 0;
        }
        currentThemeIndex = index;
        int themeVal = themeValues.get(index);
        String name = themeDisplayNames.get(index);

        if (tvCurrentThemeName != null) {
            tvCurrentThemeName.setText(name);
            tvCurrentThemeName.setSelected(true);
        }
        if (tvThemeCounter != null) {
            tvThemeCounter.setText((index + 1) + " / " + themeValues.size());
        }
        if (spinnerTheme != null && spinnerTheme.getSelectedItemPosition() != index) {
            spinnerTheme.setSelection(index, false);
        }

        if (gaugePreview != null) {
            gaugePreview.setThemeStyle(themeVal);
        }
        ClusterConfig.setTheme(this, themeVal);
        InstrumentClusterService.applyConfig(this);
    }

    private void setupSpinners() {
        // Theme Spinner
        themeDisplayNames.clear();
        themeValues.clear();

        // 1. Modern 3D & Cyber Cockpit Themes
        themeDisplayNames.add("Спорт");
        themeValues.add(ClusterConfig.THEME_SPORT_APEX);

        themeDisplayNames.add("Хронограф");
        themeValues.add(ClusterConfig.THEME_LUXURY_CHRONO);

        themeDisplayNames.add("Кибернетика");
        themeValues.add(ClusterConfig.THEME_CYBER_HEX);

        themeDisplayNames.add("Внедорожный 4x4");
        themeValues.add(ClusterConfig.THEME_TACTICAL_4X4);

        themeDisplayNames.add("Минимализм");
        themeValues.add(ClusterConfig.THEME_MINIMAL_OLED);

        themeDisplayNames.add("Тайкан Arc");
        themeValues.add(ClusterConfig.THEME_TAYCAN_ARC);

        themeDisplayNames.add("MINI Cooper");
        themeValues.add(ClusterConfig.THEME_MINI_COOPER);

        themeDisplayNames.add("Пользовательская");
        themeValues.add(ClusterConfig.THEME_USER_THEME);

        // 2. All 38 Original GWM Split Native Themes from APK assets
        for (int splitId : ClusterConfig.SPLIT_THEME_IDS) {
            themeDisplayNames.add("Split: Тема " + splitId);
            themeValues.add(ClusterConfig.THEME_SPLIT_OFFSET + splitId);
        }

        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, themeDisplayNames);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(themeAdapter);

        int currentTheme = ClusterConfig.getTheme(this);
        int selectedIndex = 6; // Default MINI Cooper
        for (int i = 0; i < themeValues.size(); i++) {
            if (themeValues.get(i) == currentTheme) {
                selectedIndex = i;
                break;
            }
        }
        selectThemeByIndex(selectedIndex);

        if (btnPrevTheme != null) {
            btnPrevTheme.setOnClickListener(v -> selectThemeByIndex(currentThemeIndex - 1));
        }
        if (btnNextTheme != null) {
            btnNextTheme.setOnClickListener(v -> selectThemeByIndex(currentThemeIndex + 1));
        }
        if (tvCurrentThemeName != null && spinnerTheme != null) {
            tvCurrentThemeName.setOnClickListener(v -> spinnerTheme.performClick());
        }

        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != currentThemeIndex) {
                    selectThemeByIndex(position);
                }
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
                intent.putExtra(InstrumentClusterService.EXTRA_TARGET_DISPLAY_ID, ClusterConfig.getTargetDisplay(MainActivity.this));
                startService(intent);
                updateButtonLabels();
            }
        });

        android.widget.CompoundButton.OnCheckedChangeListener holePunchListener = (buttonView, isChecked) -> {
            ClusterConfig.setHolePunchEnabled(MainActivity.this, isChecked);
            gaugePreview.setHolePunch(isChecked);
            if (switchHolePunch != null && switchHolePunch.isChecked() != isChecked) {
                switchHolePunch.setChecked(isChecked);
            }
            if (switchHolePunchMain != null && switchHolePunchMain.isChecked() != isChecked) {
                switchHolePunchMain.setChecked(isChecked);
            }
            InstrumentClusterService.applyConfig(MainActivity.this);
        };

        if (switchHolePunch != null) {
            switchHolePunch.setOnCheckedChangeListener(holePunchListener);
        }
        if (switchHolePunchMain != null) {
            switchHolePunchMain.setOnCheckedChangeListener(holePunchListener);
        }

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

        // CAN Bus Simulator (Способ А) wiring
        CanBusSimulator.getInstance().addListener((mode, actualHz, packetsSent, info) -> {
            if (tvCanSimStatus != null) {
                String hzStr = String.format(java.util.Locale.US, "%.1f Гц", actualHz);
                tvCanSimStatus.setText("Статус: " + info + " | " + hzStr + " | Пакетов: " + packetsSent);
            }
        });

        if (btnCanSimDrive != null) {
            btnCanSimDrive.setOnClickListener(v -> {
                isDemoActive = false;
                if (switchDemoMode != null) switchDemoMode.setChecked(false);
                CanBusSimulator.getInstance().startDriveMode();
                AppLogger.i("MainActivity", "Запущена симуляция CAN-шины 50 Гц (Поездка)");
            });
        }

        if (btnCanSimFault != null) {
            btnCanSimFault.setOnClickListener(v -> {
                isDemoActive = false;
                if (switchDemoMode != null) switchDemoMode.setChecked(false);
                CanBusSimulator.getInstance().injectFault();
                AppLogger.i("MainActivity", "Запущен тест сбоев CAN-шины");
            });
        }

        if (btnCanSimStress != null) {
            btnCanSimStress.setOnClickListener(v -> {
                isDemoActive = false;
                if (switchDemoMode != null) switchDemoMode.setChecked(false);
                CanBusSimulator.getInstance().startStressMode();
                AppLogger.i("MainActivity", "Запущен стресс-тест CAN-шины 200 Гц");
            });
        }

        if (btnCanSimStop != null) {
            btnCanSimStop.setOnClickListener(v -> {
                CanBusSimulator.getInstance().stop();
                AppLogger.i("MainActivity", "Остановлен симулятор CAN-шины");
            });
        }

        // Traffic Recorder & Replay Controls
        if (btnTrafficRecordToggle != null) {
            btnTrafficRecordToggle.setOnClickListener(v -> {
                checkAndRequestStoragePermission();
                if (TrafficRecorderReplay.isRecording()) {
                    TrafficRecorderReplay.stopRecording();
                    btnTrafficRecordToggle.setText("⏺ Запись шины");
                    btnTrafficRecordToggle.setTextColor(Color.parseColor("#FF8888"));
                } else {
                    boolean started = TrafficRecorderReplay.startRecording(this);
                    if (started) {
                        btnTrafficRecordToggle.setText("⏹ Стоп запись");
                        btnTrafficRecordToggle.setTextColor(Color.parseColor("#FFFFFF"));
                    }
                }
            });
        }

        if (btnTrafficReplayToggle != null) {
            btnTrafficReplayToggle.setOnClickListener(v -> {
                checkAndRequestStoragePermission();
                if (TrafficRecorderReplay.isReplaying()) {
                    TrafficRecorderReplay.stopReplay();
                    btnTrafficReplayToggle.setText("▶ Воспроизведение");
                } else {
                    TrafficRecorderReplay.startReplay(this);
                    btnTrafficReplayToggle.setText("⏹ Стоп реплей");
                }
            });
        }

        if (btnTrafficInjectMock != null) {
            btnTrafficInjectMock.setOnClickListener(v -> {
                TrafficRecorderReplay.injectMockPacket(++mockStepCounter);
                Toast.makeText(this, "Тест-пакет #" + mockStepCounter + " инжектирован", Toast.LENGTH_SHORT).show();
            });
        }

        TrafficRecorderReplay.setStatusListener((status, packetCount, isRec, isRep) -> {
            runOnUiThread(() -> {
                if (tvTrafficRecorderStatus != null) {
                    tvTrafficRecorderStatus.setText(status);
                }
                if (btnTrafficRecordToggle != null) {
                    btnTrafficRecordToggle.setText(isRec ? "⏹ Стоп запись" : "⏺ Запись шины");
                    btnTrafficRecordToggle.setTextColor(Color.parseColor(isRec ? "#FFFFFF" : "#FF8888"));
                }
                if (btnTrafficReplayToggle != null) {
                    btnTrafficReplayToggle.setText(isRep ? "⏹ Стоп реплей" : "▶ Воспроизведение");
                }
            });
        });
    }

    private void checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                }, 101);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AppLogger.s("MainActivity", "Разрешение на запись на диск получено");
            } else {
                AppLogger.w("MainActivity", "Разрешение на запись отклонено, запись будет идти в папку приложения");
            }
        }
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

    private void setupTargetDisplayControls() {
        if (chipTargetAll != null) {
            chipTargetAll.setOnClickListener(v -> setTargetDisplayMode(ClusterConfig.DISPLAY_TARGET_ALL_SECONDARY));
        }
        if (chipTarget2 != null) {
            chipTarget2.setOnClickListener(v -> setTargetDisplayMode(2));
        }
        if (chipTarget1 != null) {
            chipTarget1.setOnClickListener(v -> setTargetDisplayMode(1));
        }
        if (chipTarget0 != null) {
            chipTarget0.setOnClickListener(v -> setTargetDisplayMode(0));
        }
        updateTargetDisplayChips();
    }

    private void setTargetDisplayMode(int displayId) {
        ClusterConfig.setTargetDisplay(this, displayId);
        updateTargetDisplayChips();
        updateButtonLabels();
        if (InstrumentClusterService.isServiceRunning) {
            Toast.makeText(this, "Переключение экрана оверлея...", Toast.LENGTH_SHORT).show();
            Intent stopIntent = new Intent(this, InstrumentClusterService.class);
            stopIntent.setAction(InstrumentClusterService.ACTION_STOP);
            startService(stopIntent);

            findViewById(android.R.id.content).postDelayed(() -> {
                Intent startIntent = new Intent(this, InstrumentClusterService.class);
                startIntent.setAction(InstrumentClusterService.ACTION_START);
                startIntent.putExtra(InstrumentClusterService.EXTRA_TARGET_DISPLAY_ID, displayId);
                startService(startIntent);
                updateButtonLabels();
            }, 300);
        }
    }

    private void updateTargetDisplayChips() {
        int current = ClusterConfig.getTargetDisplay(this);
        applyChipState(chipTargetAll, current == ClusterConfig.DISPLAY_TARGET_ALL_SECONDARY);
        applyChipState(chipTarget2, current == 2);
        applyChipState(chipTarget1, current == 1);
        applyChipState(chipTarget0, current == 0);
    }

    private void applyChipState(TextView chip, boolean active) {
        if (chip == null) return;
        chip.setBackgroundResource(active ? R.drawable.m3_pill_active : R.drawable.m3_pill_inactive);
        chip.setTextColor(Color.parseColor(active ? "#D3E3FD" : "#8B949E"));
    }

    private void setupVehicleTelemetry() {
        if (org.opengwm.split.vehicle.FdbusNativeVehicleProvider.isAvailable()) {
            AppLogger.i("MainActivity", "Активирован нативный FDBus IPC провайдер (GA-Server sub-ms CAN stream)");
            vehicleProvider = new org.opengwm.split.vehicle.FdbusNativeVehicleProvider(this);
        } else {
            AppLogger.i("MainActivity", "Нативные библиотеки FDBus недоступны, fallback на AndroidAutomotiveVehicleProvider");
            vehicleProvider = new AndroidAutomotiveVehicleProvider(this);
        }
        vehicleProvider.startListening(new VehicleDataProvider.Callback() {
            @Override
            public void onVehicleStateChanged(final VehicleState state) {
                runOnUiThread(() -> {
                    if (!isDemoActive) {
                        gaugePreview.updateTelemetry(state);
                        long pkts = com.altaysoft.t500_split.carData.packetCount;
                        tvVehicleTelemetry.setText("Шина CAN: " + state.speedKmh + " км/ч | " + (state.rpm > 0 ? state.rpm + " об/мин" : "--") +
                                " | " + (state.gear != null ? state.gear : "--") +
                                " | Пакеты: " + pkts +
                                " | ОЖ: " + (state.targetTempC > -50f ? Math.round(state.targetTempC) + " °C" : "--"));
                        if (tvLiveSensorStats != null) {
                            tvLiveSensorStats.setText(
                                    "Топливо: " + (state.fuelPercent >= 0 ? state.fuelPercent + "%" : (state.fuelLiters >= 0 ? Math.round(state.fuelLiters) + " л" : "--")) +
                                    " | Запас: " + (state.rangeKm >= 0 ? state.rangeKm + " км" : "--") + "\n" +
                                    "АКБ: " + (state.batteryVoltage > 0 ? String.format(java.util.Locale.US, "%.1f В", state.batteryVoltage) : "--") +
                                    " | Одометр: " + (state.odometerKm >= 0 ? state.odometerKm + " км" : "--") + "\n" +
                                    "ОЖ: " + (state.targetTempC > -50f ? Math.round(state.targetTempC) + " °C" : "--") +
                                    " | За бортом: " + (state.outsideTempC > -50f ? ((state.outsideTempC >= 0 ? "+" : "") + Math.round(state.outsideTempC) + " °C") : "--") +
                                    " | Давление: " + (state.tirePressureBar > 0 ? String.format(java.util.Locale.US, "%.1f бар", state.tirePressureBar) : "--")
                            );
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
        updateTargetDisplayChips();
        updateButtonLabels();
        updatePlayerPermissionStatus();
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

            int target = ClusterConfig.getTargetDisplay(this);
            String targetDesc;
            if (target == ClusterConfig.DISPLAY_TARGET_ALL_SECONDARY) {
                targetDesc = "Оба вторичных экрана (ID 1 + 2)";
            } else if (target == 0) {
                targetDesc = "Центральный экран (ID 0 - Тест)";
            } else {
                targetDesc = "Экран ID " + target;
            }

            tvStatus.setText(running ?
                    ("Служба АКТИВНА: вывод на " + targetDesc) :
                    ("Цель вывода: " + targetDesc));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppLogger.removeListener(logListener);
        if (autoSweepAnimator != null) {
            autoSweepAnimator.cancel();
        }
        if (vehicleProvider != null) {
            vehicleProvider.stopListening();
        }
    }

    private void setupEditorControls() {
        chipElemLeftGauge = findViewById(R.id.chipElemLeftGauge);
        chipElemRightGauge = findViewById(R.id.chipElemRightGauge);
        chipElemCenterBlock = findViewById(R.id.chipElemCenterBlock);
        chipElemWidgetLeft = findViewById(R.id.chipElemWidgetLeft);
        chipElemWidgetRight = findViewById(R.id.chipElemWidgetRight);
        chipElemWidgetCenter = findViewById(R.id.chipElemWidgetCenter);
        chipElemMediaPlayer = findViewById(R.id.chipElemMediaPlayer);
        chipElemTopHeader = findViewById(R.id.chipElemTopHeader);
        chipElemBottomFooter = findViewById(R.id.chipElemBottomFooter);
        tvEditorActiveElementTitle = findViewById(R.id.tvEditorActiveElementTitle);

        tvEditorValX = findViewById(R.id.tvEditorValX);
        tvEditorValY = findViewById(R.id.tvEditorValY);
        tvEditorValScale = findViewById(R.id.tvEditorValScale);
        tvEditorValRot = findViewById(R.id.tvEditorValRot);
        sbEditorX = findViewById(R.id.sbEditorX);
        sbEditorY = findViewById(R.id.sbEditorY);
        sbEditorScale = findViewById(R.id.sbEditorScale);
        sbEditorRot = findViewById(R.id.sbEditorRot);

        btnDecX = findViewById(R.id.btnDecX);
        btnIncX = findViewById(R.id.btnIncX);
        btnDecY = findViewById(R.id.btnDecY);
        btnIncY = findViewById(R.id.btnIncY);
        btnDecScale = findViewById(R.id.btnDecScale);
        btnIncScale = findViewById(R.id.btnIncScale);
        btnDecRot = findViewById(R.id.btnDecRot);
        btnIncRot = findViewById(R.id.btnIncRot);

        btnSaveLayout = findViewById(R.id.btnSaveLayout);
        btnResetElement = findViewById(R.id.btnResetElement);
        btnResetAllElements = findViewById(R.id.btnResetAllElements);
        btnFullscreenEditor = findViewById(R.id.btnFullscreenEditor);

        TextView[] chips = {
                chipElemLeftGauge,
                chipElemRightGauge,
                chipElemCenterBlock,
                chipElemWidgetLeft,
                chipElemWidgetRight,
                chipElemWidgetCenter,
                chipElemMediaPlayer,
                chipElemTopHeader,
                chipElemBottomFooter
        };

        for (int i = 0; i < chips.length; i++) {
            final int elemId = i;
            if (chips[i] != null) {
                chips[i].setOnClickListener(v -> selectEditorElement(elemId));
            }
        }

        // Stepper buttons
        if (btnDecX != null && sbEditorX != null) btnDecX.setOnClickListener(v -> sbEditorX.setProgress(sbEditorX.getProgress() - 10));
        if (btnIncX != null && sbEditorX != null) btnIncX.setOnClickListener(v -> sbEditorX.setProgress(sbEditorX.getProgress() + 10));
        if (btnDecY != null && sbEditorY != null) btnDecY.setOnClickListener(v -> sbEditorY.setProgress(sbEditorY.getProgress() - 10));
        if (btnIncY != null && sbEditorY != null) btnIncY.setOnClickListener(v -> sbEditorY.setProgress(sbEditorY.getProgress() + 10));
        if (btnDecScale != null && sbEditorScale != null) btnDecScale.setOnClickListener(v -> sbEditorScale.setProgress(sbEditorScale.getProgress() - 5));
        if (btnIncScale != null && sbEditorScale != null) btnIncScale.setOnClickListener(v -> sbEditorScale.setProgress(sbEditorScale.getProgress() + 5));
        if (btnDecRot != null && sbEditorRot != null) btnDecRot.setOnClickListener(v -> sbEditorRot.setProgress(sbEditorRot.getProgress() - 5));
        if (btnIncRot != null && sbEditorRot != null) btnIncRot.setOnClickListener(v -> sbEditorRot.setProgress(sbEditorRot.getProgress() + 5));

        SeekBar.OnSeekBarChangeListener sliderListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isUpdatingSlidersFromTouch) return;
                applySlidersToCurrentElement();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        if (sbEditorX != null) sbEditorX.setOnSeekBarChangeListener(sliderListener);
        if (sbEditorY != null) sbEditorY.setOnSeekBarChangeListener(sliderListener);
        if (sbEditorScale != null) sbEditorScale.setOnSeekBarChangeListener(sliderListener);
        if (sbEditorRot != null) sbEditorRot.setOnSeekBarChangeListener(sliderListener);

        // Sync touch drag/pinch/rotate from gaugePreview to sliders
        if (gaugePreview != null) {
            gaugePreview.setOnTouchListener((v, event) -> {
                if (gaugePreview.isEditMode()) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            });

            gaugePreview.setOnLayoutChangeListener(new ClusterGaugeView.OnLayoutChangeListener() {
                @Override
                public void onElementSelected(int elementId) {
                    runOnUiThread(() -> selectEditorElement(elementId));
                }

                @Override
                public void onElementLayoutChanged(int elementId, float dx, float dy, float scale, float rot) {
                    runOnUiThread(() -> updateSlidersForElement(dx, dy, scale, rot));
                }
            });
        }

        // Save button
        if (btnSaveLayout != null) {
            btnSaveLayout.setOnClickListener(v -> saveLayoutToConfig());
        }

        // Reset current element button
        if (btnResetElement != null) {
            btnResetElement.setOnClickListener(v -> {
                ClusterConfig.resetElementLayout(this, currentEditElement);
                if (gaugePreview != null) {
                    gaugePreview.setElementLayout(currentEditElement, 0f, 0f, 1.0f, 0f);
                }
                selectEditorElement(currentEditElement);
                android.widget.Toast.makeText(this, "Элемент сброшен в заводские координаты", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        // Reset all button
        if (btnResetAllElements != null) {
            btnResetAllElements.setOnClickListener(v -> {
                ClusterConfig.resetAllLayouts(this);
                if (gaugePreview != null) {
                    gaugePreview.loadLayoutConfig();
                }
                selectEditorElement(currentEditElement);
                android.widget.Toast.makeText(this, "Все элементы сброшены в заводские координаты", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        // Fullscreen editor button
        if (btnFullscreenEditor != null) {
            btnFullscreenEditor.setOnClickListener(v -> openFullscreenEditorDialog());
        }

        selectEditorElement(ClusterConfig.ELEM_LEFT_GAUGE);
    }

    private void selectEditorElement(int elementId) {
        currentEditElement = elementId;
        TextView[] chips = {
                chipElemLeftGauge,
                chipElemRightGauge,
                chipElemCenterBlock,
                chipElemWidgetLeft,
                chipElemWidgetRight,
                chipElemWidgetCenter,
                chipElemMediaPlayer,
                chipElemTopHeader,
                chipElemBottomFooter
        };

        for (int i = 0; i < chips.length; i++) {
            if (chips[i] != null) {
                boolean sel = (i == elementId);
                chips[i].setBackgroundResource(sel ? R.drawable.m3_pill_active : R.drawable.m3_pill_inactive);
                chips[i].setTextColor(Color.parseColor(sel ? "#D3E3FD" : "#8B949E"));
            }
        }

        if (tvEditorActiveElementTitle != null && elementId < ClusterConfig.ELEMENT_NAMES.length) {
            tvEditorActiveElementTitle.setText("НАСТРОЙКА: " + ClusterConfig.ELEMENT_NAMES[elementId].toUpperCase());
        }

        if (gaugePreview != null) {
            gaugePreview.setSelectedElement(elementId);
            ClusterGaugeView.ElementLayout layout = gaugePreview.getElementLayout(elementId);
            updateSlidersForElement(layout.dx, layout.dy, layout.scale, layout.rotation);
        }
    }

    private void updateSlidersForElement(float dx, float dy, float scale, float rot) {
        isUpdatingSlidersFromTouch = true;
        try {
            int progX = Math.round(dx * 1000f) + 300;
            int progY = Math.round(dy * 1000f) + 300;
            int progScale = Math.round((scale - 0.40f) * 100f);
            int progRot = Math.round(rot) + 180;

            if (sbEditorX != null) sbEditorX.setProgress(Math.max(0, Math.min(600, progX)));
            if (sbEditorY != null) sbEditorY.setProgress(Math.max(0, Math.min(600, progY)));
            if (sbEditorScale != null) sbEditorScale.setProgress(Math.max(0, Math.min(200, progScale)));
            if (sbEditorRot != null) sbEditorRot.setProgress(Math.max(0, Math.min(360, progRot)));

            int dxPx = Math.round(dx * 1920f);
            int dyPx = Math.round(dy * 720f);
            int scalePct = Math.round(scale * 100f);

            if (tvEditorValX != null) tvEditorValX.setText((dxPx >= 0 ? "+" : "") + dxPx + " px (" + Math.round(dx * 100f) + "%)");
            if (tvEditorValY != null) tvEditorValY.setText((dyPx >= 0 ? "+" : "") + dyPx + " px (" + Math.round(dy * 100f) + "%)");
            if (tvEditorValScale != null) tvEditorValScale.setText(scalePct + "%");
            if (tvEditorValRot != null) tvEditorValRot.setText(Math.round(rot) + "°");
        } finally {
            isUpdatingSlidersFromTouch = false;
        }
    }

    private void applySlidersToCurrentElement() {
        if (sbEditorX == null || sbEditorY == null || sbEditorScale == null) return;
        float dx = (sbEditorX.getProgress() - 300) / 1000f;
        float dy = (sbEditorY.getProgress() - 300) / 1000f;
        float scale = 0.40f + (sbEditorScale.getProgress() / 100f);
        float rot = (sbEditorRot != null) ? (sbEditorRot.getProgress() - 180f) : 0f;

        int dxPx = Math.round(dx * 1920f);
        int dyPx = Math.round(dy * 720f);
        int scalePct = Math.round(scale * 100f);

        if (tvEditorValX != null) tvEditorValX.setText((dxPx >= 0 ? "+" : "") + dxPx + " px (" + Math.round(dx * 100f) + "%)");
        if (tvEditorValY != null) tvEditorValY.setText((dyPx >= 0 ? "+" : "") + dyPx + " px (" + Math.round(dy * 100f) + "%)");
        if (tvEditorValScale != null) tvEditorValScale.setText(scalePct + "%");
        if (tvEditorValRot != null) tvEditorValRot.setText(Math.round(rot) + "°");

        if (gaugePreview != null) {
            gaugePreview.setElementLayout(currentEditElement, dx, dy, scale, rot);
        }
    }

    private void saveLayoutToConfig() {
        if (gaugePreview != null) {
            for (int i = 0; i < ClusterConfig.ELEM_COUNT; i++) {
                ClusterGaugeView.ElementLayout layout = gaugePreview.getElementLayout(i);
                ClusterConfig.setElementLayout(this, i, layout.dx, layout.dy, layout.scale, layout.rotation);
            }
        }
        InstrumentClusterService.applyConfig(this);
        android.widget.Toast.makeText(this, "Расположение сохранено", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void openFullscreenEditorDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#06080B"));

        ClusterGaugeView fsView = new ClusterGaugeView(this);
        fsView.loadLayoutConfig();
        fsView.setEditMode(true, currentEditElement);
        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        root.addView(fsView, viewParams);

        // Control bar at bottom
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        bar.setPadding(24, 12, 24, 12);
        bar.setBackgroundColor(Color.parseColor("#0E131A"));

        TextView hint = new TextView(this);
        hint.setText("Один палец: перемещение | Два пальца: масштаб и поворот");
        hint.setTextColor(Color.parseColor("#8B949E"));
        hint.setTextSize(12f);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        bar.addView(hint, hintParams);

        Button btnSaveFs = new Button(this);
        btnSaveFs.setText("Сохранить");
        btnSaveFs.setTextColor(Color.parseColor("#FFFFFF"));
        btnSaveFs.setBackgroundResource(R.drawable.m3_button_primary);
        btnSaveFs.setPadding(32, 8, 32, 8);
        btnSaveFs.setOnClickListener(v -> {
            for (int i = 0; i < ClusterConfig.ELEM_COUNT; i++) {
                ClusterGaugeView.ElementLayout l = fsView.getElementLayout(i);
                ClusterConfig.setElementLayout(this, i, l.dx, l.dy, l.scale, l.rotation);
            }
            if (gaugePreview != null) gaugePreview.loadLayoutConfig();
            InstrumentClusterService.applyConfig(this);
            android.widget.Toast.makeText(this, "Расположение сохранено", android.widget.Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        bar.addView(btnSaveFs);

        Button btnClose = new Button(this);
        btnClose.setText("Закрыть");
        btnClose.setTextColor(Color.parseColor("#C9D1D9"));
        btnClose.setBackgroundResource(R.drawable.m3_button_secondary);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        closeParams.leftMargin = 16;
        btnClose.setLayoutParams(closeParams);
        btnClose.setOnClickListener(v -> {
            if (gaugePreview != null) gaugePreview.loadLayoutConfig();
            dialog.dismiss();
        });
        bar.addView(btnClose);

        root.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(root);
        dialog.show();
    }

    // =========================================================================
    // 6. DIAGNOSTICS & SYSTEM LOGS TERMINAL
    // =========================================================================
    private final AppLogger.LogListener logListener = new AppLogger.LogListener() {
        @Override
        public void onNewLog(AppLogger.LogEntry entry) {
            runOnUiThread(() -> {
                if (containerTabLogs != null && containerTabLogs.getVisibility() == View.VISIBLE) {
                    renderLogs();
                } else if (tvLogCounter != null) {
                    tvLogCounter.setText("ЖИВОЙ ТЕРМИНАЛ (" + AppLogger.getEntriesSnapshot().size() + " строк)");
                }
            });
        }

        @Override
        public void onLogsCleared() {
            runOnUiThread(() -> {
                if (tvLogOutput != null) tvLogOutput.setText("Логи очищены.");
                if (tvLogCounter != null) tvLogCounter.setText("ЖИВОЙ ТЕРМИНАЛ (0 строк)");
            });
        }
    };

    private void setupDiagnosticsControls() {
        AppLogger.addListener(logListener);

        if (btnRefreshDiagStatus != null) {
            btnRefreshDiagStatus.setOnClickListener(v -> updateDiagnosticSummary());
        }

        if (btnProbeVhalNow != null) {
            btnProbeVhalNow.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Запуск зондажа VHAL / шины...", Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    if (vehicleProvider instanceof AndroidAutomotiveVehicleProvider) {
                        ((AndroidAutomotiveVehicleProvider) vehicleProvider).probeAllProperties();
                    } else {
                        new AndroidAutomotiveVehicleProvider(MainActivity.this).probeAllProperties();
                    }
                    runOnUiThread(this::updateDiagnosticSummary);
                }).start();
            });
        }

        if (btnCopyLogs != null) {
            btnCopyLogs.setOnClickListener(v -> {
                String allLogs = AppLogger.getAllLogsAsString();
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("OpenGWM Logs", allLogs));
                    Toast.makeText(MainActivity.this, "Логи скопированы в буфер обмена", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnExportLogsFile != null) {
            btnExportLogsFile.setOnClickListener(v -> {
                File f = AppLogger.exportToFile(MainActivity.this);
                if (f != null) {
                    Toast.makeText(MainActivity.this, "Файл сохранен: " + f.getName(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка сохранения файла", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnClearLogs != null) {
            btnClearLogs.setOnClickListener(v -> {
                AppLogger.clear();
                Toast.makeText(MainActivity.this, "Логи очищены", Toast.LENGTH_SHORT).show();
            });
        }

        View.OnClickListener filterClick = v -> {
            if (v == chipFilterAll) currentLogFilter = 0;
            else if (v == chipFilterErrors) currentLogFilter = 1;
            else if (v == chipFilterBus) currentLogFilter = 2;
            else if (v == chipFilterDisplay) currentLogFilter = 3;

            updateFilterPills();
            renderLogs();
        };

        if (chipFilterAll != null) chipFilterAll.setOnClickListener(filterClick);
        if (chipFilterErrors != null) chipFilterErrors.setOnClickListener(filterClick);
        if (chipFilterBus != null) chipFilterBus.setOnClickListener(filterClick);
        if (chipFilterDisplay != null) chipFilterDisplay.setOnClickListener(filterClick);
    }

    private void updateFilterPills() {
        TextView[] chips = {chipFilterAll, chipFilterErrors, chipFilterBus, chipFilterDisplay};
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            boolean sel = (i == currentLogFilter);
            chips[i].setBackgroundResource(sel ? R.drawable.m3_pill_active : R.drawable.m3_pill_inactive);
            chips[i].setTextColor(Color.parseColor(sel ? "#D3E3FD" : "#8B949E"));
        }
    }

    private void updateDiagnosticSummary() {
        boolean overlayOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        if (tvDiagOverlayStatus != null) {
            tvDiagOverlayStatus.setText(overlayOk ? "• Оверлей поверх окон: Разрешено" : "• Оверлей поверх окон: Требуется разрешение");
            tvDiagOverlayStatus.setTextColor(Color.parseColor(overlayOk ? "#3FB950" : "#F85149"));
        }

        boolean isBound = false;
        boolean hasProp = false;
        boolean hasSensor = false;
        if (vehicleProvider instanceof AndroidAutomotiveVehicleProvider) {
            AndroidAutomotiveVehicleProvider aavp = (AndroidAutomotiveVehicleProvider) vehicleProvider;
            isBound = aavp.isServiceBound();
            hasProp = aavp.hasCarProperty();
            hasSensor = aavp.hasCarSensor();
        }

        if (tvDiagCarServiceStatus != null) {
            if (isBound && hasProp) {
                tvDiagCarServiceStatus.setText("• Служба com.android.car: Подключена");
                tvDiagCarServiceStatus.setTextColor(Color.parseColor("#3FB950"));
            } else if (isBound) {
                tvDiagCarServiceStatus.setText("• Служба com.android.car: Ожидание сервиса");
                tvDiagCarServiceStatus.setTextColor(Color.parseColor("#D29922"));
            } else {
                tvDiagCarServiceStatus.setText("• Служба com.android.car: Не подключена");
                tvDiagCarServiceStatus.setTextColor(Color.parseColor("#8B949E"));
            }
        }

        if (tvDiagVhalStatus != null) {
            if (hasProp) {
                tvDiagVhalStatus.setText("• Доступ к VHAL (SA8155): Активен");
                tvDiagVhalStatus.setTextColor(Color.parseColor("#3FB950"));
            } else if (hasSensor) {
                tvDiagVhalStatus.setText("• Доступ к VHAL (SA8155): CarSensorManager");
                tvDiagVhalStatus.setTextColor(Color.parseColor("#D29922"));
            } else {
                tvDiagVhalStatus.setText("• Доступ к VHAL (SA8155): Недоступен");
                tvDiagVhalStatus.setTextColor(Color.parseColor("#8B949E"));
            }
        }

        // Displays Check
        DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = dm != null ? dm.getDisplays() : new Display[0];
        boolean hasSecondary = false;
        StringBuilder sbDisp = new StringBuilder("• Дисплеи в системе (" + displays.length + "): ");
        for (Display d : displays) {
            DisplayMetrics m = new DisplayMetrics();
            d.getRealMetrics(m);
            sbDisp.append("[ID ").append(d.getDisplayId()).append(": ").append(m.widthPixels).append("x").append(m.heightPixels).append("] ");
            if (d.getDisplayId() != Display.DEFAULT_DISPLAY) {
                hasSecondary = true;
            }
        }
        if (tvDiagDisplaysStatus != null) {
            tvDiagDisplaysStatus.setText(sbDisp.toString());
            tvDiagDisplaysStatus.setTextColor(Color.parseColor(hasSecondary ? "#8CE4A8" : "#FFB74D"));
        }

        if (tvDiagSocketCanStatus != null) {
            tvDiagSocketCanStatus.setText("• SocketCAN: " + AndroidAutomotiveVehicleProvider.getCan0Status());
        }
    }

    private void renderLogs() {
        List<AppLogger.LogEntry> snapshot = AppLogger.getEntriesSnapshot();
        if (tvLogCounter != null) {
            tvLogCounter.setText("ЖИВОЙ ТЕРМИНАЛ (" + snapshot.size() + " строк)");
        }

        StringBuilder sb = new StringBuilder();
        int matched = 0;
        for (int i = Math.max(0, snapshot.size() - 250); i < snapshot.size(); i++) {
            AppLogger.LogEntry e = snapshot.get(i);
            boolean pass = true;
            if (currentLogFilter == 1) { // Errors only
                pass = (e.level == AppLogger.Level.ERROR || e.level == AppLogger.Level.WARN);
            } else if (currentLogFilter == 2) { // Bus / VHAL
                pass = e.tag.contains("AAS") || e.tag.contains("CAN") || e.tag.contains("VHAL") || e.message.contains("PROP");
            } else if (currentLogFilter == 3) { // Displays
                pass = e.tag.contains("Cluster") || e.message.contains("Display") || e.message.contains("Screen");
            }

            if (pass) {
                sb.append(e.getFormatted()).append("\n");
                matched++;
            }
        }

        if (matched == 0) {
            sb.append("Нет записей под выбранный фильтр.");
        }

        if (tvLogOutput != null) {
            tvLogOutput.setText(sb.toString());
            if (scrollLogTerminal != null) {
                scrollLogTerminal.post(() -> scrollLogTerminal.fullScroll(View.FOCUS_DOWN));
            }
        }
    }
}
