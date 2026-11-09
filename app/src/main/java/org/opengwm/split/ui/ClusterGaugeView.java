package org.opengwm.split.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import org.opengwm.split.theme.ThemeAssetLoader;
import org.opengwm.split.vehicle.VehicleState;

/**
 * High-End Custom Instrument Cluster View for GWM Tank 500 / Haval.
 * Supports 5 distinct volumetric, 3D-shaded automotive themes:
 * 1. SPORT_APEX: 3D deep tube bezels, F1-style shift lights, dynamic glowing arc trails
 * 2. LUXURY_CHRONO: Bentley/Tank 500 luxury gold chronograph with sub-dials & guilloche
 * 3. CYBER_HEX: Futuristic polygonal HUD with perspective grid & neon segmented bars
 * 4. TACTICAL_4X4: Baja off-road with bolted steel bezels & 3D artificial horizon (inclinometer)
 * 5. MINIMAL_OLED: Scandinavian floating ambient light ribbons on deep obsidian black
 */
public class ClusterGaugeView extends View {

    private int activeTheme = ClusterConfig.THEME_SPORT_APEX;
    private boolean isHolePunchEnabled = false;
    private boolean isSmoothingEnabled = true;

    // Target telemetry
    private float targetSpeed = 0f;
    private float targetRpm = 0f;
    private String gear = "P";
    private float coolantTemp = 90.0f;
    private int cruiseSpeed = 0;
    private String driveMode = "STANDARD 4H";

    // Smoothed values for 60 FPS animation
    private float displaySpeed = 0f;
    private float displayRpm = 0f;

    // Mini-Widgets configuration
    private int widgetLeft = ClusterConfig.WIDGET_FUEL;
    private int widgetRight = ClusterConfig.WIDGET_RANGE;
    private int widgetCenter = ClusterConfig.WIDGET_COOLANT;
    private VehicleState latestState = null;
    private final RectF widgetLeftRect = new RectF();
    private final RectF widgetRightRect = new RectF();
    private final Paint widgetBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint widgetBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint widgetTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Dimensions
    private int viewWidth = 0;
    private int viewHeight = 0;
    private float gaugeRadius = 0f;
    private float centerY = 0f;
    private float leftCenterX = 0f;
    private float rightCenterX = 0f;

    // Reusable Paints (0 allocations during onDraw)
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tubeBezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tubeInnerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dialPlatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dialTexturePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickMajorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickMinorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleCapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint redlineArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerBoxBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerBoxBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint modePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint horizonSkyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint horizonGroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePunchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Taycan Cyber Cockpit Reusable Geometry & Paints
    private final RectF bezelRect = new RectF();
    private final RectF pillRect = new RectF();
    private final RectF batteryCardRect = new RectF();
    private final RectF mediaCardRect = new RectF();
    private final Path tpmsBracketPath = new Path();
    private final java.text.SimpleDateFormat clockFormat = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.US);
    private final Paint cockpitBezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cockpitRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statusPillBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint statusPillBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Dynamic Media Player Detection & Metadata
    private boolean isMusicActive = false;
    private String trackTitle = "Synthwave Drive";
    private String trackArtist = "Night Rider";
    private android.media.AudioManager audioManager = null;
    private long lastMusicCheckTime = 0;
    private android.content.BroadcastReceiver mediaReceiver = null;

    // Reusable Geometry & Shapes
    private final RectF arcRect = new RectF();
    private final RectF centerBoxRect = new RectF();
    private final RectF subDialRect = new RectF();
    private final Path needlePath = new Path();
    private final Path needleShadowPath = new Path();
    private final Path hexPath = new Path();
    private final Path horizonClipPath = new Path();

    // Shaders cached per view size
    private RadialGradient dialPlateShader;
    private LinearGradient sportBezelShader;
    private LinearGradient luxuryBezelShader;
    private LinearGradient tacticalBezelShader;

    public ClusterGaugeView(Context context) {
        super(context);
        init();
    }

    public ClusterGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        holePunchPaint.setColor(Color.TRANSPARENT);
        holePunchPaint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));

        activeTheme = ClusterConfig.getTheme(getContext());
        isHolePunchEnabled = ClusterConfig.isHolePunchEnabled(getContext());
        isSmoothingEnabled = ClusterConfig.isSmoothingEnabled(getContext());
        driveMode = ClusterConfig.getDriveMode(getContext());
        cruiseSpeed = ClusterConfig.getCruiseSpeed(getContext());
        widgetLeft = ClusterConfig.getWidgetLeft(getContext());
        widgetRight = ClusterConfig.getWidgetRight(getContext());
        widgetCenter = ClusterConfig.getWidgetCenter(getContext());

        setupPaints();
        initAudioMonitoring();
    }

    public void setWidgets(int left, int right, int center) {
        this.widgetLeft = left;
        this.widgetRight = right;
        this.widgetCenter = center;
        ClusterConfig.setWidgetLeft(getContext(), left);
        ClusterConfig.setWidgetRight(getContext(), right);
        ClusterConfig.setWidgetCenter(getContext(), center);
        invalidate();
    }

    public void setThemeStyle(int theme) {
        this.activeTheme = theme;
        ClusterConfig.setTheme(getContext(), theme);
        rebuildShaders();
        invalidate();
    }

    public int getThemeStyle() {
        return activeTheme;
    }

    public void setHolePunch(boolean enabled) {
        this.isHolePunchEnabled = enabled;
        ClusterConfig.setHolePunchEnabled(getContext(), enabled);
        invalidate();
    }

    public void setSmoothing(boolean enabled) {
        this.isSmoothingEnabled = enabled;
        ClusterConfig.setSmoothingEnabled(getContext(), enabled);
    }

    public void setDriveMode(String mode) {
        this.driveMode = mode;
        ClusterConfig.setDriveMode(getContext(), mode);
        invalidate();
    }

    public void setCruiseSpeed(int spd) {
        this.cruiseSpeed = spd;
        ClusterConfig.setCruiseSpeed(getContext(), spd);
        invalidate();
    }

    public void updateTelemetry(VehicleState state) {
        this.targetSpeed = state.speedKmh;
        this.targetRpm = state.rpm;
        this.gear = state.gear;
        this.coolantTemp = state.targetTempC;
        this.latestState = state;

        if (!isSmoothingEnabled) {
            this.displaySpeed = this.targetSpeed;
            this.displayRpm = this.targetRpm;
        }
        postInvalidateOnAnimation();
    }

    public void setManualValues(float speed, float rpm, String gear, String mode) {
        this.targetSpeed = speed;
        this.targetRpm = rpm;
        if (gear != null) this.gear = gear;
        if (mode != null) this.driveMode = mode;

        if (!isSmoothingEnabled) {
            this.displaySpeed = speed;
            this.displayRpm = rpm;
        }
        postInvalidateOnAnimation();
    }

    public void initAudioMonitoring() {
        if (audioManager == null) {
            try {
                audioManager = (android.media.AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            } catch (Throwable ignored) {}
        }
        if (audioManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                audioManager.registerAudioPlaybackCallback(new android.media.AudioManager.AudioPlaybackCallback() {
                    @Override
                    public void onPlaybackConfigChanged(java.util.List<android.media.AudioPlaybackConfiguration> configs) {
                        boolean active = false;
                        if (configs != null) {
                            for (android.media.AudioPlaybackConfiguration cfg : configs) {
                                if (cfg.getAudioAttributes().getUsage() == android.media.AudioAttributes.USAGE_MEDIA) {
                                    active = true;
                                    break;
                                }
                            }
                        }
                        isMusicActive = active;
                        postInvalidate();
                    }
                }, new android.os.Handler(android.os.Looper.getMainLooper()));
            } catch (Throwable ignored) {}
        }
    }

    public boolean checkIsMusicPlaying() {
        if (audioManager == null) {
            initAudioMonitoring();
        }
        if (audioManager != null) {
            long now = System.currentTimeMillis();
            if (now - lastMusicCheckTime > 400) {
                lastMusicCheckTime = now;
                try {
                    if (audioManager.isMusicActive()) {
                        isMusicActive = true;
                    }
                } catch (Throwable ignored) {}
            }
        }
        return isMusicActive;
    }

    public void setMediaTrackInfo(boolean isPlaying, String title, String artist) {
        this.isMusicActive = isPlaying;
        if (title != null && !title.isEmpty()) this.trackTitle = title;
        if (artist != null && !artist.isEmpty()) this.trackArtist = artist;
        postInvalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initAudioMonitoring();
        registerMediaReceiver();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterMediaReceiver();
    }

    private void registerMediaReceiver() {
        if (mediaReceiver != null) return;
        try {
            mediaReceiver = new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;
                    String track = intent.getStringExtra("track");
                    if (track == null) track = intent.getStringExtra("title");
                    String artist = intent.getStringExtra("artist");

                    if (track != null && !track.isEmpty()) trackTitle = track;
                    if (artist != null && !artist.isEmpty()) trackArtist = artist;

                    if (intent.hasExtra("playing")) {
                        isMusicActive = intent.getBooleanExtra("playing", false);
                    } else if (audioManager != null) {
                        isMusicActive = audioManager.isMusicActive();
                    }
                    postInvalidate();
                }
            };
            android.content.IntentFilter filter = new android.content.IntentFilter();
            filter.addAction("com.android.music.metachanged");
            filter.addAction("com.android.music.playstatechanged");
            filter.addAction("com.android.music.playbackcomplete");
            filter.addAction("com.spotify.music.metadatachanged");
            filter.addAction("com.spotify.music.playbackstatechanged");
            filter.addAction("yandex.music.action.track_changed");
            filter.addAction("com.yandex.music.action.PLAY");
            filter.addAction("com.yandex.music.action.PAUSE");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(mediaReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                getContext().registerReceiver(mediaReceiver, filter);
            }
        } catch (Throwable ignored) {}
    }

    private void unregisterMediaReceiver() {
        if (mediaReceiver != null) {
            try {
                getContext().unregisterReceiver(mediaReceiver);
            } catch (Throwable ignored) {}
            mediaReceiver = null;
        }
    }

    private void setupPaints() {
        tubeBezelPaint.setStyle(Paint.Style.STROKE);
        tubeInnerShadowPaint.setStyle(Paint.Style.STROKE);
        dialPlatePaint.setStyle(Paint.Style.FILL);
        dialTexturePaint.setStyle(Paint.Style.STROKE);

        tickMajorPaint.setStrokeCap(Paint.Cap.ROUND);
        tickMinorPaint.setStrokeCap(Paint.Cap.ROUND);

        textNumberPaint.setTextAlign(Paint.Align.CENTER);
        textNumberPaint.setFakeBoldText(true);

        textValuePaint.setTextAlign(Paint.Align.CENTER);
        textValuePaint.setFakeBoldText(true);

        textUnitPaint.setTextAlign(Paint.Align.CENTER);

        needlePaint.setStyle(Paint.Style.FILL);
        needleShadowPaint.setStyle(Paint.Style.FILL);
        needleShadowPaint.setColor(Color.parseColor("#80000000"));

        needleGlowPaint.setStyle(Paint.Style.STROKE);
        needleCapPaint.setStyle(Paint.Style.FILL);
        needlePinPaint.setStyle(Paint.Style.FILL);

        activeArcPaint.setStyle(Paint.Style.STROKE);
        activeArcPaint.setStrokeCap(Paint.Cap.ROUND);

        redlineArcPaint.setStyle(Paint.Style.STROKE);
        redlineArcPaint.setStrokeCap(Paint.Cap.ROUND);

        centerBoxBgPaint.setStyle(Paint.Style.FILL);
        centerBoxBorderPaint.setStyle(Paint.Style.STROKE);

        gearPaint.setTextAlign(Paint.Align.CENTER);
        gearPaint.setFakeBoldText(true);

        modePaint.setTextAlign(Paint.Align.CENTER);
        modePaint.setFakeBoldText(true);

        subTextPaint.setTextAlign(Paint.Align.CENTER);

        boltPaint.setStyle(Paint.Style.FILL);
        boltPaint.setColor(Color.parseColor("#90A4AE"));

        horizonSkyPaint.setStyle(Paint.Style.FILL);
        horizonGroundPaint.setStyle(Paint.Style.FILL);

        widgetBgPaint.setStyle(Paint.Style.FILL);
        widgetBgPaint.setColor(Color.parseColor("#D90D1310"));
        widgetBorderPaint.setStyle(Paint.Style.STROKE);
        widgetBorderPaint.setStrokeWidth(1.2f);
        widgetBorderPaint.setColor(Color.parseColor("#4080D89D"));
        widgetTextPaint.setTextAlign(Paint.Align.CENTER);
        widgetTextPaint.setColor(Color.parseColor("#A8E6BD"));
        widgetTextPaint.setFakeBoldText(true);

        cockpitBezelPaint.setStyle(Paint.Style.STROKE);
        cockpitRimPaint.setStyle(Paint.Style.STROKE);
        statusPillBgPaint.setStyle(Paint.Style.FILL);
        statusPillBorderPaint.setStyle(Paint.Style.STROKE);
        innerArcPaint.setStyle(Paint.Style.STROKE);
        innerArcPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.viewWidth = w;
        this.viewHeight = h;
        if (w <= 0 || h <= 0) return;

        // Native 1920x720 instrument cluster geometry:
        // Left dial cx=305 (~0.16w), right dial cx=1615 (~0.84w), cy=360 (0.50h), radius=270
        this.leftCenterX = w * 0.16f;
        this.rightCenterX = w * 0.84f;
        this.centerY = h * 0.50f;
        this.gaugeRadius = Math.min(w * 0.142f, h * 0.375f);

        rebuildShaders();
    }

    private void rebuildShaders() {
        if (gaugeRadius <= 0) return;

        // Dial recessed radial shadow
        dialPlateShader = new RadialGradient(
                leftCenterX, centerY, gaugeRadius,
                new int[]{Color.parseColor("#181B24"), Color.parseColor("#0E1017"), Color.parseColor("#06070A")},
                new float[]{0.0f, 0.7f, 1.0f},
                Shader.TileMode.CLAMP
        );

        // 3D Bezel metallic highlights (Light source at top-left, shadow at bottom-right)
        sportBezelShader = new LinearGradient(
                leftCenterX - gaugeRadius, centerY - gaugeRadius,
                leftCenterX + gaugeRadius, centerY + gaugeRadius,
                new int[]{Color.parseColor("#5A6577"), Color.parseColor("#252A33"), Color.parseColor("#121419"), Color.parseColor("#38404D")},
                new float[]{0.0f, 0.4f, 0.8f, 1.0f},
                Shader.TileMode.CLAMP
        );

        luxuryBezelShader = new LinearGradient(
                leftCenterX - gaugeRadius, centerY - gaugeRadius,
                leftCenterX + gaugeRadius, centerY + gaugeRadius,
                new int[]{Color.parseColor("#F5E0A3"), Color.parseColor("#99793D"), Color.parseColor("#3D2B12"), Color.parseColor("#D4AF37")},
                new float[]{0.0f, 0.35f, 0.75f, 1.0f},
                Shader.TileMode.CLAMP
        );

        tacticalBezelShader = new LinearGradient(
                leftCenterX - gaugeRadius, centerY - gaugeRadius,
                leftCenterX + gaugeRadius, centerY + gaugeRadius,
                new int[]{Color.parseColor("#78909C"), Color.parseColor("#37474F"), Color.parseColor("#212121"), Color.parseColor("#546E7A")},
                new float[]{0.0f, 0.35f, 0.75f, 1.0f},
                Shader.TileMode.CLAMP
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = viewWidth;
        int h = viewHeight;
        if (w <= 0 || h <= 0) return;

        // Smooth needle animation physics at 60 FPS
        boolean needsInvalidate = false;
        if (isSmoothingEnabled) {
            float speedDiff = targetSpeed - displaySpeed;
            float rpmDiff = targetRpm - displayRpm;

            if (Math.abs(speedDiff) > 0.08f) {
                displaySpeed += speedDiff * 0.18f;
                needsInvalidate = true;
            } else {
                displaySpeed = targetSpeed;
            }

            if (Math.abs(rpmDiff) > 1.0f) {
                displayRpm += rpmDiff * 0.22f;
                needsInvalidate = true;
            } else {
                displayRpm = targetRpm;
            }
        } else {
            displaySpeed = targetSpeed;
            displayRpm = targetRpm;
        }

        // Draw active theme
        if (activeTheme >= ClusterConfig.THEME_SPLIT_OFFSET) {
            drawSplitBitmapTheme(canvas, w, h, activeTheme - ClusterConfig.THEME_SPLIT_OFFSET);
        } else {
            switch (activeTheme) {
                case ClusterConfig.THEME_LUXURY_CHRONO:
                    drawLuxuryChronoTheme(canvas, w, h);
                    break;
                case ClusterConfig.THEME_CYBER_HEX:
                    drawCyberHexTheme(canvas, w, h);
                    break;
                case ClusterConfig.THEME_TACTICAL_4X4:
                    drawTactical4x4Theme(canvas, w, h);
                    break;
                case ClusterConfig.THEME_MINIMAL_OLED:
                    drawMinimalOledTheme(canvas, w, h);
                    break;
                case ClusterConfig.THEME_TAYCAN_ARC:
                    drawTaycanArcTheme(canvas, w, h);
                    break;
                case ClusterConfig.THEME_USER_THEME:
                    drawUserTheme(canvas, w, h);
                    break;
                case ClusterConfig.THEME_SPORT_APEX:
                default:
                    drawSportApexTheme(canvas, w, h);
                    break;
            }
        }

        // Draw Configurable Telemetry Mini-Widgets
        drawMiniWidgets(canvas);

        // Safe HolePunch Cutout Mask (only on overlay themes, NEVER on full cockpit themes like Taycan)
        if (isHolePunchEnabled && activeTheme != ClusterConfig.THEME_TAYCAN_ARC) {
            drawHolePunchMask(canvas, w, h);
        }

        if (needsInvalidate) {
            postInvalidateOnAnimation();
        }
    }

    // =========================================================================
    // 1. THEME: SPORT APEX (Track Mode, 3D Deep Tubes, F1 Shift Lights)
    // =========================================================================
    private void drawSportApexTheme(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.parseColor("#07080B"));

        // Dials
        draw3DTubeDial(canvas, leftCenterX, centerY, gaugeRadius, sportBezelShader, Color.parseColor("#FF1744"));
        draw3DTubeDial(canvas, rightCenterX, centerY, gaugeRadius, sportBezelShader, Color.parseColor("#FF1744"));

        // Speedometer Left
        drawSpeedometerScale(canvas, leftCenterX, centerY, gaugeRadius, 240, Color.WHITE, Color.parseColor("#FF1744"), true);

        // Tachometer Right
        drawTachometerScale(canvas, rightCenterX, centerY, gaugeRadius, 8000, 5500, Color.WHITE, Color.parseColor("#FF1744"), true);

        // Needles
        drawSportNeedle(canvas, leftCenterX, centerY, gaugeRadius * 0.82f, getSpeedAngle(displaySpeed, 240), Color.parseColor("#FF1744"));
        drawSportNeedle(canvas, rightCenterX, centerY, gaugeRadius * 0.82f, getRpmAngle(displayRpm, 8000), Color.parseColor("#FF1744"));

        // Center Box: Carbon card with F1 shift light bar
        drawSportCenterCard(canvas, w * 0.5f, centerY, gaugeRadius);
    }

    private void draw3DTubeDial(Canvas canvas, float cx, float cy, float radius, Shader bezelShader, int accentColor) {
        // 1. Recessed dial plate with inner shadow
        dialPlatePaint.setShader(dialPlateShader);
        canvas.drawCircle(cx, cy, radius, dialPlatePaint);
        dialPlatePaint.setShader(null);

        // 2. Concentric track texture lines
        dialTexturePaint.setColor(Color.parseColor("#15FFFFFF"));
        dialTexturePaint.setStrokeWidth(1.2f);
        canvas.drawCircle(cx, cy, radius * 0.70f, dialTexturePaint);
        canvas.drawCircle(cx, cy, radius * 0.45f, dialTexturePaint);

        // 3. Inner shadow chamfer (simulates 3D tube depth)
        tubeInnerShadowPaint.setColor(Color.parseColor("#B0000000"));
        tubeInnerShadowPaint.setStrokeWidth(radius * 0.08f);
        canvas.drawCircle(cx, cy, radius * 0.94f, tubeInnerShadowPaint);

        // 4. Volumetric metallic outer bezel ring
        tubeBezelPaint.setShader(bezelShader);
        tubeBezelPaint.setStrokeWidth(radius * 0.07f);
        canvas.drawCircle(cx, cy, radius, tubeBezelPaint);
        tubeBezelPaint.setShader(null);

        // 5. Razor thin accent lip
        tubeBezelPaint.setColor(accentColor);
        tubeBezelPaint.setStrokeWidth(2.5f);
        canvas.drawCircle(cx, cy, radius * 0.90f, tubeBezelPaint);
    }

    private void drawSportNeedle(Canvas canvas, float cx, float cy, float length, float angleDeg, int needleColor) {
        canvas.save();
        canvas.rotate(angleDeg, cx, cy);

        // Needle Drop Shadow (volume)
        needleShadowPath.reset();
        needleShadowPath.moveTo(cx - 16f, cy + 2f);
        needleShadowPath.lineTo(cx + length, cy + 6f);
        needleShadowPath.lineTo(cx - 16f, cy + 10f);
        needleShadowPath.close();
        canvas.drawPath(needleShadowPath, needleShadowPaint);

        // Needle Blade
        needlePath.reset();
        needlePath.moveTo(cx - 18f, cy - 3.5f);
        needlePath.lineTo(cx + length, cy);
        needlePath.lineTo(cx - 18f, cy + 3.5f);
        needlePath.close();

        needlePaint.setColor(needleColor);
        canvas.drawPath(needlePath, needlePaint);

        // Luminous needle core line
        needleGlowPaint.setColor(Color.WHITE);
        needleGlowPaint.setStrokeWidth(2f);
        canvas.drawLine(cx, cy, cx + length * 0.9f, cy, needleGlowPaint);

        // 3D Center Cap (Chrome rim + anodized red center)
        needleCapPaint.setColor(Color.parseColor("#37474F"));
        canvas.drawCircle(cx, cy, 16f, needleCapPaint);
        needleCapPaint.setColor(Color.parseColor("#546E7A"));
        canvas.drawCircle(cx, cy, 14f, needleCapPaint);
        needlePinPaint.setColor(needleColor);
        canvas.drawCircle(cx, cy, 7f, needlePinPaint);

        canvas.restore();
    }

    private void drawSportCenterCard(Canvas canvas, float cx, float cy, float radius) {
        if (isHolePunchEnabled) return;
        float boxW = Math.min(viewWidth * 0.14f, 260f);
        float boxH = radius * 1.25f;
        centerBoxRect.set(cx - boxW / 2, cy - boxH / 2, cx + boxW / 2, cy + boxH / 2);

        centerBoxBgPaint.setColor(Color.parseColor("#10131A"));
        canvas.drawRoundRect(centerBoxRect, 18f, 18f, centerBoxBgPaint);

        centerBoxBorderPaint.setColor(Color.parseColor("#FF1744"));
        centerBoxBorderPaint.setStrokeWidth(2.5f);
        canvas.drawRoundRect(centerBoxRect, 18f, 18f, centerBoxBorderPaint);

        // F1-style Shift Light Bar at top of card
        float ledY = centerBoxRect.top + 22f;
        float ledStartX = cx - 55f;
        float rpmRatio = displayRpm / 8000f;
        for (int i = 0; i < 7; i++) {
            float ledX = ledStartX + i * 18f;
            int ledColor;
            boolean active = (rpmRatio >= (0.4f + i * 0.08f));
            if (i < 3) {
                ledColor = active ? Color.parseColor("#00E676") : Color.parseColor("#1B3E25");
            } else if (i < 5) {
                ledColor = active ? Color.parseColor("#FFEA00") : Color.parseColor("#3E3B1B");
            } else {
                ledColor = active ? Color.parseColor("#FF1744") : Color.parseColor("#3E1B1E");
            }
            boltPaint.setColor(ledColor);
            canvas.drawCircle(ledX, ledY, 5f, boltPaint);
        }

        // Gear display
        gearPaint.setColor(Color.parseColor("#FF1744"));
        gearPaint.setTextSize(54f);
        canvas.drawText(gear, cx, cy + 2f, gearPaint);

        // Drive mode badge
        modePaint.setColor(Color.parseColor("#ECEFF1"));
        modePaint.setTextSize(spToPx(13));
        canvas.drawText(driveMode, cx, cy + 34f, modePaint);

        // Coolant Temp & Boost
        subTextPaint.setColor(Color.parseColor("#90A4AE"));
        subTextPaint.setTextSize(spToPx(11));
        canvas.drawText("COOLANT " + Math.round(coolantTemp) + " °C", cx, cy + 62f, subTextPaint);
    }

    // =========================================================================
    // 2. THEME: LUXURY CHRONO (Bentley / Tank 500 Gold Chronograph)
    // =========================================================================
    private void drawLuxuryChronoTheme(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.parseColor("#060608"));

        // 3D Brushed Gold Bezel
        draw3DTubeDial(canvas, leftCenterX, centerY, gaugeRadius, luxuryBezelShader, Color.parseColor("#C5A059"));
        draw3DTubeDial(canvas, rightCenterX, centerY, gaugeRadius, luxuryBezelShader, Color.parseColor("#C5A059"));

        // Guilloche concentric fine texture
        dialTexturePaint.setColor(Color.parseColor("#22C5A059"));
        dialTexturePaint.setStrokeWidth(1f);
        for (float r = gaugeRadius * 0.35f; r < gaugeRadius * 0.85f; r += 12f) {
            canvas.drawCircle(leftCenterX, centerY, r, dialTexturePaint);
            canvas.drawCircle(rightCenterX, centerY, r, dialTexturePaint);
        }

        // Sub-dial inside Speedometer (Mini Chrono)
        drawChronoSubDial(canvas, leftCenterX, centerY - gaugeRadius * 0.28f, gaugeRadius * 0.28f, "OIL");

        // Sub-dial inside Tachometer (Mini Voltage/Temp)
        drawChronoSubDial(canvas, rightCenterX, centerY - gaugeRadius * 0.28f, gaugeRadius * 0.28f, "BAT");

        // Scales
        drawSpeedometerScale(canvas, leftCenterX, centerY, gaugeRadius, 240, Color.parseColor("#F5E6C8"), Color.parseColor("#C5A059"), false);
        drawTachometerScale(canvas, rightCenterX, centerY, gaugeRadius, 8000, 6000, Color.parseColor("#F5E6C8"), Color.parseColor("#C5A059"), false);

        // Classic Breguet Needles (Gold sword with circular counterweight)
        drawLuxuryNeedle(canvas, leftCenterX, centerY, gaugeRadius * 0.80f, getSpeedAngle(displaySpeed, 240));
        drawLuxuryNeedle(canvas, rightCenterX, centerY, gaugeRadius * 0.80f, getRpmAngle(displayRpm, 8000));

        // Center Luxury Crest & Gear
        drawLuxuryCenterCard(canvas, w * 0.5f, centerY, gaugeRadius);
    }

    private void drawChronoSubDial(Canvas canvas, float cx, float cy, float radius, String label) {
        subDialRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
        tubeBezelPaint.setColor(Color.parseColor("#44C5A059"));
        tubeBezelPaint.setStrokeWidth(1.5f);
        canvas.drawCircle(cx, cy, radius, tubeBezelPaint);

        subTextPaint.setColor(Color.parseColor("#88C5A059"));
        subTextPaint.setTextSize(10f);
        canvas.drawText(label, cx, cy + radius * 0.5f, subTextPaint);
    }

    private void drawLuxuryNeedle(Canvas canvas, float cx, float cy, float length, float angleDeg) {
        canvas.save();
        canvas.rotate(angleDeg, cx, cy);

        // Needle Shadow
        needleShadowPath.reset();
        needleShadowPath.moveTo(cx - 18f, cy + 2f);
        needleShadowPath.lineTo(cx + length, cy + 4f);
        needleShadowPath.lineTo(cx - 18f, cy + 6f);
        needleShadowPath.close();
        canvas.drawPath(needleShadowPath, needleShadowPaint);

        // Sword blade
        needlePath.reset();
        needlePath.moveTo(cx - 20f, cy - 2f);
        needlePath.lineTo(cx + length * 0.7f, cy - 3.5f);
        needlePath.lineTo(cx + length, cy);
        needlePath.lineTo(cx + length * 0.7f, cy + 3.5f);
        needlePath.lineTo(cx - 20f, cy + 2f);
        needlePath.close();

        needlePaint.setColor(Color.parseColor("#E5C158"));
        canvas.drawPath(needlePath, needlePaint);

        // Circular counterweight
        canvas.drawCircle(cx - 14f, cy, 6f, needlePaint);
        canvas.drawCircle(cx - 14f, cy, 3f, dialPlatePaint);

        // Gold center cap
        needleCapPaint.setColor(Color.parseColor("#C5A059"));
        canvas.drawCircle(cx, cy, 14f, needleCapPaint);
        needlePinPaint.setColor(Color.parseColor("#18140B"));
        canvas.drawCircle(cx, cy, 5f, needlePinPaint);

        canvas.restore();
    }

    private void drawLuxuryCenterCard(Canvas canvas, float cx, float cy, float radius) {
        if (isHolePunchEnabled) return;
        float boxW = Math.min(viewWidth * 0.14f, 260f);
        float boxH = radius * 1.25f;
        centerBoxRect.set(cx - boxW / 2, cy - boxH / 2, cx + boxW / 2, cy + boxH / 2);

        centerBoxBgPaint.setColor(Color.parseColor("#0C0E13"));
        canvas.drawRoundRect(centerBoxRect, 18f, 18f, centerBoxBgPaint);

        centerBoxBorderPaint.setColor(Color.parseColor("#5A4725"));
        centerBoxBorderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(centerBoxRect, 18f, 18f, centerBoxBorderPaint);

        // Tank 500 Crown / Luxury line
        tubeBezelPaint.setColor(Color.parseColor("#C5A059"));
        tubeBezelPaint.setStrokeWidth(1.5f);
        canvas.drawLine(cx - 30f, centerBoxRect.top + 28f, cx + 30f, centerBoxRect.top + 28f, tubeBezelPaint);

        // Gear display
        gearPaint.setColor(Color.parseColor("#E5C158"));
        gearPaint.setTextSize(54f);
        canvas.drawText(gear, cx, cy + 6f, gearPaint);

        // Drive Mode
        modePaint.setColor(Color.parseColor("#C5A059"));
        modePaint.setTextSize(spToPx(13));
        canvas.drawText(driveMode, cx, cy + 38f, modePaint);

        // Temperature
        subTextPaint.setColor(Color.parseColor("#8C7853"));
        subTextPaint.setTextSize(spToPx(11));
        canvas.drawText(Math.round(coolantTemp) + " °C  |  TANK 500", cx, cy + 64f, subTextPaint);
    }

    // =========================================================================
    // 3. THEME: CYBER HEX (Futuristic Polygonal EV HUD, Tron Horizon Grid)
    // =========================================================================
    private void drawCyberHexTheme(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.parseColor("#03050C"));

        // Cyberpunk perspective horizon floor in background
        drawCyberFloorGrid(canvas, w, h);

        // Hexagonal / Angular Chamfered Dials
        drawHexGauge(canvas, leftCenterX, centerY, gaugeRadius, Color.parseColor("#00E5FF"));
        drawHexGauge(canvas, rightCenterX, centerY, gaugeRadius, Color.parseColor("#FF007F"));

        // Segmented LED Bar Scales
        drawCyberScale(canvas, leftCenterX, centerY, gaugeRadius, displaySpeed, 240, Color.parseColor("#00E5FF"), "KM/H");
        drawCyberScale(canvas, rightCenterX, centerY, gaugeRadius, displayRpm, 8000, Color.parseColor("#FF007F"), "RPM");

        // Center Holographic HUD Card
        drawCyberCenterCard(canvas, w * 0.5f, centerY, gaugeRadius);
    }

    private void drawCyberFloorGrid(Canvas canvas, int w, int h) {
        dialTexturePaint.setColor(Color.parseColor("#1200E5FF"));
        dialTexturePaint.setStrokeWidth(1.5f);

        float horizonY = h * 0.65f;
        // Horizontal grid lines
        for (float y = horizonY; y < h; y += (y - horizonY + 8f) * 0.45f) {
            canvas.drawLine(0, y, w, y, dialTexturePaint);
        }
        // Perspective rays
        float vanX = w * 0.5f;
        for (int i = -6; i <= 6; i++) {
            canvas.drawLine(vanX, horizonY, vanX + i * (w * 0.12f), h, dialTexturePaint);
        }
    }

    private void drawHexGauge(Canvas canvas, float cx, float cy, float radius, int glowColor) {
        hexPath.reset();
        int sides = 8; // Octagonal chamfer
        for (int i = 0; i < sides; i++) {
            double angle = Math.toRadians(i * 360f / sides + 22.5f);
            float x = (float) (cx + radius * Math.cos(angle));
            float y = (float) (cy + radius * Math.sin(angle));
            if (i == 0) hexPath.moveTo(x, y);
            else hexPath.lineTo(x, y);
        }
        hexPath.close();

        // Dark back plate
        dialPlatePaint.setColor(Color.parseColor("#0A0F1F"));
        canvas.drawPath(hexPath, dialPlatePaint);

        // Glowing outer frame
        tubeBezelPaint.setColor(glowColor);
        tubeBezelPaint.setStrokeWidth(3.5f);
        canvas.drawPath(hexPath, tubeBezelPaint);
    }

    private void drawCyberScale(Canvas canvas, float cx, float cy, float radius, float currentVal, float maxVal, int activeColor, String unit) {
        float startAngle = 135f;
        float sweepAngle = 270f;
        int segments = 28;
        float currentRatio = Math.min(Math.max(currentVal / maxVal, 0f), 1f);

        for (int i = 0; i <= segments; i++) {
            float ratio = (float) i / segments;
            float angle = startAngle + ratio * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isActive = ratio <= currentRatio;

            float innerR = radius * 0.72f;
            float outerR = radius * 0.88f;
            float x1 = (float) (cx + innerR * Math.cos(rad));
            float y1 = (float) (cy + innerR * Math.sin(rad));
            float x2 = (float) (cx + outerR * Math.cos(rad));
            float y2 = (float) (cy + outerR * Math.sin(rad));

            tickMajorPaint.setColor(isActive ? activeColor : Color.parseColor("#1A253A"));
            tickMajorPaint.setStrokeWidth(isActive ? 5.5f : 3f);
            canvas.drawLine(x1, y1, x2, y2, tickMajorPaint);
        }

        // Bold Digital Center Readout
        textValuePaint.setColor(activeColor);
        textValuePaint.setTextSize(46f);
        canvas.drawText(String.valueOf(Math.round(currentVal)), cx, cy + 12f, textValuePaint);

        textUnitPaint.setColor(Color.parseColor("#80FFFFFF"));
        textUnitPaint.setTextSize(14f);
        canvas.drawText(unit, cx, cy + 38f, textUnitPaint);
    }

    private void drawCyberCenterCard(Canvas canvas, float cx, float cy, float radius) {
        if (isHolePunchEnabled) return;
        float boxW = Math.min(viewWidth * 0.14f, 260f);
        float boxH = radius * 1.25f;
        centerBoxRect.set(cx - boxW / 2, cy - boxH / 2, cx + boxW / 2, cy + boxH / 2);

        centerBoxBgPaint.setColor(Color.parseColor("#080D1C"));
        canvas.drawRoundRect(centerBoxRect, 14f, 14f, centerBoxBgPaint);

        centerBoxBorderPaint.setColor(Color.parseColor("#00E5FF"));
        centerBoxBorderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(centerBoxRect, 14f, 14f, centerBoxBorderPaint);

        // Neon HUD top label
        subTextPaint.setColor(Color.parseColor("#00E5FF"));
        subTextPaint.setTextSize(11f);
        canvas.drawText("[ CYBER HYBRID VHAL ]", cx, centerBoxRect.top + 24f, subTextPaint);

        // Gear display (Neon Magenta)
        gearPaint.setColor(Color.parseColor("#FF007F"));
        gearPaint.setTextSize(58f);
        canvas.drawText(gear, cx, cy + 8f, gearPaint);

        // Drive Mode
        modePaint.setColor(Color.parseColor("#00E5FF"));
        modePaint.setTextSize(spToPx(13));
        canvas.drawText(driveMode, cx, cy + 40f, modePaint);

        // Temp
        subTextPaint.setColor(Color.parseColor("#80F0FF"));
        canvas.drawText(Math.round(coolantTemp) + " °C  |  SYS ACTIVE", cx, cy + 64f, subTextPaint);
    }

    // =========================================================================
    // 4. THEME: TACTICAL 4X4 (Baja Tank, Bolted Bezels, 3D Pitch/Roll Horizon)
    // =========================================================================
    private void drawTactical4x4Theme(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.parseColor("#0B0C0E"));

        // Bolted Industrial Steel Bezels
        draw3DTubeDial(canvas, leftCenterX, centerY, gaugeRadius, tacticalBezelShader, Color.parseColor("#FF8F00"));
        draw3DTubeDial(canvas, rightCenterX, centerY, gaugeRadius, tacticalBezelShader, Color.parseColor("#FF8F00"));

        // 12 Industrial Allen Bolts around each bezel
        drawBezelBolts(canvas, leftCenterX, centerY, gaugeRadius * 1.05f);
        drawBezelBolts(canvas, rightCenterX, centerY, gaugeRadius * 1.05f);

        // Military Stencil Scales
        drawSpeedometerScale(canvas, leftCenterX, centerY, gaugeRadius, 240, Color.parseColor("#FFB300"), Color.parseColor("#FF6D00"), true);
        drawTachometerScale(canvas, rightCenterX, centerY, gaugeRadius, 8000, 5000, Color.parseColor("#FFB300"), Color.parseColor("#FF3D00"), true);

        // Aviation Orange Needles
        drawSportNeedle(canvas, leftCenterX, centerY, gaugeRadius * 0.82f, getSpeedAngle(displaySpeed, 240), Color.parseColor("#FF6D00"));
        drawSportNeedle(canvas, rightCenterX, centerY, gaugeRadius * 0.82f, getRpmAngle(displayRpm, 8000), Color.parseColor("#FF6D00"));

        // Center Console: 3D Off-Road Inclinometer / Artificial Horizon
        drawTacticalInclinometerCard(canvas, w * 0.5f, centerY, gaugeRadius);
    }

    private void drawBezelBolts(Canvas canvas, float cx, float cy, float radius) {
        boltPaint.setColor(Color.parseColor("#78909C"));
        for (int i = 0; i < 12; i++) {
            double rad = Math.toRadians(i * 30f);
            float bx = (float) (cx + radius * Math.cos(rad));
            float by = (float) (cy + radius * Math.sin(rad));
            canvas.drawCircle(bx, by, 3.5f, boltPaint);
        }
    }

    private void drawTacticalInclinometerCard(Canvas canvas, float cx, float cy, float radius) {
        if (isHolePunchEnabled) return;
        float boxW = Math.min(viewWidth * 0.14f, 260f);
        float boxH = radius * 1.25f;
        centerBoxRect.set(cx - boxW / 2, cy - boxH / 2, cx + boxW / 2, cy + boxH / 2);

        centerBoxBgPaint.setColor(Color.parseColor("#12141A"));
        canvas.drawRoundRect(centerBoxRect, 16f, 16f, centerBoxBgPaint);

        centerBoxBorderPaint.setColor(Color.parseColor("#FF8F00"));
        centerBoxBorderPaint.setStrokeWidth(2.5f);
        canvas.drawRoundRect(centerBoxRect, 16f, 16f, centerBoxBorderPaint);

        // Miniature 3D Artificial Horizon / Inclinometer (Креномер)
        float horizonW = boxW * 0.82f;
        float horizonH = 34f;
        float hTop = centerBoxRect.top + 20f;
        RectF horizonRect = new RectF(cx - horizonW / 2, hTop, cx + horizonW / 2, hTop + horizonH);

        canvas.save();
        horizonClipPath.reset();
        horizonClipPath.addRoundRect(horizonRect, 8f, 8f, Path.Direction.CW);
        canvas.clipPath(horizonClipPath);

        // Sky & Ground
        horizonSkyPaint.setColor(Color.parseColor("#1A3A5A"));
        horizonGroundPaint.setColor(Color.parseColor("#3D2817"));
        canvas.drawRect(horizonRect.left, horizonRect.top, horizonRect.right, horizonRect.top + horizonH / 2, horizonSkyPaint);
        canvas.drawRect(horizonRect.left, horizonRect.top + horizonH / 2, horizonRect.right, horizonRect.bottom, horizonGroundPaint);

        // Horizon line & level ticks
        tubeBezelPaint.setColor(Color.parseColor("#FFB300"));
        tubeBezelPaint.setStrokeWidth(2f);
        canvas.drawLine(horizonRect.left, horizonRect.top + horizonH / 2, horizonRect.right, horizonRect.top + horizonH / 2, tubeBezelPaint);
        canvas.drawCircle(cx, horizonRect.top + horizonH / 2, 4f, tubeBezelPaint);
        canvas.restore();

        // Gear display
        gearPaint.setColor(Color.parseColor("#FF8F00"));
        gearPaint.setTextSize(50f);
        canvas.drawText(gear, cx, cy + 12f, gearPaint);

        // Mode badge
        modePaint.setColor(Color.parseColor("#FFA000"));
        modePaint.setTextSize(spToPx(13));
        canvas.drawText(driveMode, cx, cy + 42f, modePaint);

        // 4x4 Differential Lock Status
        subTextPaint.setColor(Color.parseColor("#90A4AE"));
        subTextPaint.setTextSize(11f);
        canvas.drawText("[ LOCK: F - C - R ]", cx, cy + 64f, subTextPaint);
    }

    // =========================================================================
    // 5. THEME: MINIMAL OLED (Nordic Clean Floating Arc Ribbons)
    // =========================================================================
    private void drawMinimalOledTheme(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.BLACK); // Pure 100% OLED true black

        // Floating arc ribbons (no heavy outer rings, pure luminous arcs)
        drawMinimalFloatingArc(canvas, leftCenterX, centerY, gaugeRadius, displaySpeed, 240, Color.parseColor("#4D88FF"), "km/h");
        drawMinimalFloatingArc(canvas, rightCenterX, centerY, gaugeRadius, displayRpm, 8000, Color.parseColor("#E0E0E0"), "rpm");

        // Clean Center Typography (only when HolePunch is disabled)
        if (!isHolePunchEnabled) {
            gearPaint.setColor(Color.WHITE);
            gearPaint.setTextSize(64f);
            canvas.drawText(gear, w * 0.5f, centerY + 4f, gearPaint);

            modePaint.setColor(Color.parseColor("#80FFFFFF"));
            modePaint.setTextSize(spToPx(13));
            canvas.drawText(driveMode, w * 0.5f, centerY + 38f, modePaint);

            subTextPaint.setColor(Color.parseColor("#40FFFFFF"));
            subTextPaint.setTextSize(spToPx(11));
            canvas.drawText(Math.round(coolantTemp) + " °C", w * 0.5f, centerY + 62f, subTextPaint);
        }
    }

    private void drawMinimalFloatingArc(Canvas canvas, float cx, float cy, float radius, float currentVal, float maxVal, int arcColor, String unit) {
        arcRect.set(cx - radius * 0.85f, cy - radius * 0.85f, cx + radius * 0.85f, cy + radius * 0.85f);
        float startAngle = 140f;
        float sweepAngle = 260f;

        // Background subtle track
        activeArcPaint.setColor(Color.parseColor("#15FFFFFF"));
        activeArcPaint.setStrokeWidth(6f);
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, activeArcPaint);

        // Active luminous arc
        float ratio = Math.min(Math.max(currentVal / maxVal, 0f), 1f);
        activeArcPaint.setColor(arcColor);
        activeArcPaint.setStrokeWidth(8f);
        canvas.drawArc(arcRect, startAngle, ratio * sweepAngle, false, activeArcPaint);

        // Minimalist digital numbers
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(48f);
        canvas.drawText(String.valueOf(Math.round(currentVal)), cx, cy + 14f, textValuePaint);

        textUnitPaint.setColor(Color.parseColor("#80FFFFFF"));
        textUnitPaint.setTextSize(14f);
        canvas.drawText(unit, cx, cy + 42f, textUnitPaint);
    }

    // =========================================================================
    // Scale Helpers
    // =========================================================================
    private void drawSpeedometerScale(Canvas canvas, float cx, float cy, float radius, int maxSpeed, int tickColor, int activeColor, boolean showGlowArc) {
        float startAngle = 135f;
        float sweepAngle = 270f;

        // Active glowing arc following the needle
        if (showGlowArc) {
            arcRect.set(cx - radius * 0.86f, cy - radius * 0.86f, cx + radius * 0.86f, cy + radius * 0.86f);
            float activeRatio = Math.min(Math.max(displaySpeed / maxSpeed, 0f), 1f);
            activeArcPaint.setColor(activeColor);
            activeArcPaint.setStrokeWidth(5f);
            canvas.drawArc(arcRect, startAngle, activeRatio * sweepAngle, false, activeArcPaint);
        }

        // Ticks
        for (int s = 0; s <= maxSpeed; s += 10) {
            float angle = startAngle + ((float) s / maxSpeed) * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isMajor = (s % 20 == 0);
            float innerR = isMajor ? radius * 0.76f : radius * 0.83f;
            float outerR = radius * 0.89f;

            float x1 = (float) (cx + innerR * Math.cos(rad));
            float y1 = (float) (cy + innerR * Math.sin(rad));
            float x2 = (float) (cx + outerR * Math.cos(rad));
            float y2 = (float) (cy + outerR * Math.sin(rad));

            tickMajorPaint.setColor(isMajor ? tickColor : Color.parseColor("#607D8B"));
            tickMajorPaint.setStrokeWidth(isMajor ? 4f : 2f);
            canvas.drawLine(x1, y1, x2, y2, tickMajorPaint);

            if (isMajor && (s % 40 == 0 || s == 0)) {
                float textR = radius * 0.62f;
                float tx = (float) (cx + textR * Math.cos(rad));
                float ty = (float) (cy + textR * Math.sin(rad) + 6f);
                textNumberPaint.setColor(tickColor);
                textNumberPaint.setTextSize(spToPx(13));
                canvas.drawText(String.valueOf(s), tx, ty, textNumberPaint);
            }
        }

        // Digital Speed in dial center
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(spToPx(32));
        canvas.drawText(String.valueOf(Math.round(displaySpeed)), cx, cy + radius * 0.40f, textValuePaint);

        textUnitPaint.setColor(Color.parseColor("#90A4AE"));
        textUnitPaint.setTextSize(spToPx(11));
        canvas.drawText("KM/H", cx, cy + radius * 0.55f, textUnitPaint);
    }

    private void drawTachometerScale(Canvas canvas, float cx, float cy, float radius, int maxRpm, int redlineRpm, int tickColor, int redlineColor, boolean showGlowArc) {
        float startAngle = 135f;
        float sweepAngle = 270f;

        // Redline Arc
        arcRect.set(cx - radius * 0.86f, cy - radius * 0.86f, cx + radius * 0.86f, cy + radius * 0.86f);
        float redlineStart = startAngle + ((float) redlineRpm / maxRpm) * sweepAngle;
        float redlineSweep = ((float) (maxRpm - redlineRpm) / maxRpm) * sweepAngle;
        redlineArcPaint.setColor(redlineColor);
        redlineArcPaint.setStrokeWidth(7f);
        canvas.drawArc(arcRect, redlineStart, redlineSweep, false, redlineArcPaint);

        // Active sweep arc
        if (showGlowArc) {
            float activeRatio = Math.min(Math.max(displayRpm / maxRpm, 0f), 1f);
            activeArcPaint.setColor(displayRpm >= redlineRpm ? redlineColor : Color.parseColor("#4D88FF"));
            activeArcPaint.setStrokeWidth(5f);
            canvas.drawArc(arcRect, startAngle, activeRatio * sweepAngle, false, activeArcPaint);
        }

        // Ticks
        for (int r = 0; r <= maxRpm; r += 250) {
            float angle = startAngle + ((float) r / maxRpm) * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isMajor = (r % 1000 == 0);
            boolean isRedline = r >= redlineRpm;
            float innerR = isMajor ? radius * 0.76f : radius * 0.83f;
            float outerR = radius * 0.89f;

            float x1 = (float) (cx + innerR * Math.cos(rad));
            float y1 = (float) (cy + innerR * Math.sin(rad));
            float x2 = (float) (cx + outerR * Math.cos(rad));
            float y2 = (float) (cy + outerR * Math.sin(rad));

            tickMajorPaint.setColor(isRedline ? redlineColor : (isMajor ? tickColor : Color.parseColor("#607D8B")));
            tickMajorPaint.setStrokeWidth(isMajor ? 4f : 2f);
            canvas.drawLine(x1, y1, x2, y2, tickMajorPaint);

            if (isMajor) {
                float textR = radius * 0.62f;
                float tx = (float) (cx + textR * Math.cos(rad));
                float ty = (float) (cy + textR * Math.sin(rad) + 6f);
                textNumberPaint.setColor(isRedline ? redlineColor : tickColor);
                textNumberPaint.setTextSize(spToPx(13));
                canvas.drawText(String.valueOf(r / 1000), tx, ty, textNumberPaint);
            }
        }

        // Digital RPM in dial center
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(spToPx(32));
        canvas.drawText(String.valueOf(Math.round(displayRpm)), cx, cy + radius * 0.40f, textValuePaint);

        textUnitPaint.setColor(Color.parseColor("#90A4AE"));
        textUnitPaint.setTextSize(spToPx(11));
        canvas.drawText("RPM", cx, cy + radius * 0.55f, textUnitPaint);
    }

    private float getSpeedAngle(float speed, float maxSpeed) {
        float clamped = Math.min(Math.max(speed, 0f), maxSpeed);
        return 135f + (clamped / maxSpeed) * 270f;
    }

    private float getRpmAngle(float rpm, float maxRpm) {
        float clamped = Math.min(Math.max(rpm, 0f), maxRpm);
        return 135f + (clamped / maxRpm) * 270f;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private void drawHolePunchMask(Canvas canvas, int w, int h) {
        // 1. Center QNX ADAS / Vehicle Visualizer aperture
        // OEM coordinates: x = 610..1310 (w * 0.318f to w * 0.682f), y = 100..665
        float centerLeft = w * 0.318f;
        float centerRight = w * 0.682f;
        float centerTop = h * 0.14f;
        float centerBottom = h * 0.92f;
        RectF centerAperture = new RectF(centerLeft, centerTop, centerRight, centerBottom);
        canvas.drawRoundRect(centerAperture, 18f, 18f, holePunchPaint);

        // 2. Upper telltales & turn signal lamps bar (y: 0..95)
        float topApertureLeft = w * 0.34f;
        float topApertureRight = w * 0.66f;
        RectF topAperture = new RectF(topApertureLeft, 0f, topApertureRight, h * 0.13f);
        canvas.drawRoundRect(topAperture, 12f, 12f, holePunchPaint);

        // 3. Lower factory odometer & status bar (y: 665..720)
        RectF bottomAperture = new RectF(w * 0.34f, h * 0.93f, w * 0.66f, h);
        canvas.drawRect(bottomAperture, holePunchPaint);
    }

    // =========================================================================
    // 6. THEME: TAYCAN ARC (Cyber Electric Cockpit, Image 2 Spec)
    // =========================================================================
    private void drawTaycanArcTheme(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.parseColor("#04070D"));

        // 1. Outer sculpted automotive cockpit bezel & ambient glow
        drawCockpitBezel(canvas, w, h);

        // 2. Top center status bar: ( D ), ECO, Battery [⚡], WiFi, BT
        drawTopStatusBar(canvas, w, h);

        // 3. Center Screen: Topo map placeholder
        drawCenterNavPlaceholder(canvas, w, h);

        // 4. Speedometer Pod Left (cx = leftCenterX, cy = centerY)
        drawTaycanSpeedoArc(canvas, leftCenterX, centerY, gaugeRadius * 0.94f, w, h);

        // 5. Tachometer / Power Pod Right (cx = rightCenterX, cy = centerY)
        drawTaycanTachoArc(canvas, rightCenterX, centerY, gaugeRadius * 0.94f, w, h);

        // 6. Floating M3 Glassmorphic Music Player card
        drawFloatingMusicCard(canvas, w, h);
    }

    private void drawCockpitBezel(Canvas canvas, int w, int h) {
        // Outer metallic chamfered rim around 1920x720 cluster housing
        bezelRect.set(12f, 12f, w - 12f, h - 12f);
        cockpitBezelPaint.setColor(Color.parseColor("#151D28"));
        cockpitBezelPaint.setStrokeWidth(10f);
        canvas.drawRoundRect(bezelRect, 28f, 28f, cockpitBezelPaint);

        cockpitRimPaint.setColor(Color.parseColor("#38485C"));
        cockpitRimPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(bezelRect, 28f, 28f, cockpitRimPaint);

        // Sculpted aerodynamic accent indents (top & bottom center)
        cockpitRimPaint.setColor(Color.parseColor("#223040"));
        cockpitRimPaint.setStrokeWidth(1.5f);
        canvas.drawLine(w * 0.32f, 12f, w * 0.68f, 12f, cockpitRimPaint);
        canvas.drawLine(w * 0.32f, h - 12f, w * 0.68f, h - 12f, cockpitRimPaint);
    }

    private void drawTopStatusBar(Canvas canvas, int w, int h) {
        float barY = h * 0.068f;

        // Drive Mode circle ( D )
        float circleX = w * 0.5f - 130f;
        statusPillBgPaint.setColor(Color.parseColor("#0D47A1"));
        canvas.drawCircle(circleX, barY, 15f, statusPillBgPaint);
        statusPillBorderPaint.setColor(Color.parseColor("#00E5FF"));
        statusPillBorderPaint.setStrokeWidth(1.5f);
        canvas.drawCircle(circleX, barY, 15f, statusPillBorderPaint);

        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(11));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText("D", circleX, barY + 4f, subTextPaint);

        // ECO badge
        float ecoW = 52f;
        float ecoH = 22f;
        pillRect.set(w * 0.5f - 60f - ecoW / 2, barY - ecoH / 2, w * 0.5f - 60f + ecoW / 2, barY + ecoH / 2);
        statusPillBgPaint.setColor(Color.parseColor("#082B18"));
        canvas.drawRoundRect(pillRect, 11f, 11f, statusPillBgPaint);
        statusPillBorderPaint.setColor(Color.parseColor("#00E676"));
        statusPillBorderPaint.setStrokeWidth(1.2f);
        canvas.drawRoundRect(pillRect, 11f, 11f, statusPillBorderPaint);

        subTextPaint.setColor(Color.parseColor("#00E676"));
        canvas.drawText("ECO", w * 0.5f - 60f, barY + 4f, subTextPaint);

        // Battery icon [ ⚡ ]
        float batX = w * 0.5f + 60f;
        batteryCardRect.set(batX - 14f, barY - 8f, batX + 14f, barY + 8f);
        statusPillBorderPaint.setColor(Color.parseColor("#00E676"));
        statusPillBorderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(batteryCardRect, 3f, 3f, statusPillBorderPaint);
        canvas.drawRect(batX + 14f, barY - 4f, batX + 17f, barY + 4f, statusPillBorderPaint);

        statusPillBgPaint.setColor(Color.parseColor("#00E676"));
        canvas.drawRect(batX - 11f, barY - 5f, batX + 9f, barY + 5f, statusPillBgPaint);

        // Wi-Fi icon
        float wifiX = w * 0.5f + 105f;
        statusPillBorderPaint.setColor(Color.parseColor("#80DEEA"));
        statusPillBorderPaint.setStrokeWidth(1.5f);
        canvas.drawArc(new RectF(wifiX - 10f, barY - 8f, wifiX + 10f, barY + 12f), 225f, 90f, false, statusPillBorderPaint);
        canvas.drawArc(new RectF(wifiX - 6f, barY - 4f, wifiX + 6f, barY + 8f), 225f, 90f, false, statusPillBorderPaint);
        canvas.drawCircle(wifiX, barY + 4f, 1.8f, statusPillBgPaint);

        // Bluetooth Rune
        float btX = w * 0.5f + 140f;
        statusPillBorderPaint.setColor(Color.parseColor("#80DEEA"));
        canvas.drawLine(btX, barY - 8f, btX, barY + 8f, statusPillBorderPaint);
        canvas.drawLine(btX - 4f, barY - 4f, btX + 4f, barY + 4f, statusPillBorderPaint);
        canvas.drawLine(btX - 4f, barY + 4f, btX + 4f, barY - 4f, statusPillBorderPaint);
        canvas.drawLine(btX + 4f, barY - 4f, btX, barY - 8f, statusPillBorderPaint);
        canvas.drawLine(btX + 4f, barY + 4f, btX, barY + 8f, statusPillBorderPaint);
    }

    private void drawCenterNavPlaceholder(Canvas canvas, int w, int h) {
        // Nav map vector grid placeholder (used when hole punch is disabled / demo)
        float left = w * 0.33f;
        float right = w * 0.67f;
        float top = h * 0.12f;
        float bottom = h * 0.90f;
        RectF navRect = new RectF(left, top, right, bottom);

        statusPillBgPaint.setColor(Color.parseColor("#08121E"));
        canvas.drawRoundRect(navRect, 18f, 18f, statusPillBgPaint);
        statusPillBorderPaint.setColor(Color.parseColor("#152438"));
        statusPillBorderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(navRect, 18f, 18f, statusPillBorderPaint);

        // Stylized perspective road/topography lines
        statusPillBorderPaint.setColor(Color.parseColor("#102033"));
        canvas.drawLine(w * 0.5f, top + 20f, left + 40f, bottom - 80f, statusPillBorderPaint);
        canvas.drawLine(w * 0.5f, top + 20f, right - 40f, bottom - 80f, statusPillBorderPaint);
        canvas.drawLine(w * 0.5f, top + 20f, w * 0.5f, bottom - 80f, statusPillBorderPaint);
    }

    private void drawFloatingMusicCard(Canvas canvas, int w, int h) {
        boolean playing = checkIsMusicPlaying() || isMusicActive;

        // Floating M3 Glassmorphic Music Player card
        float cardW = Math.min(360f, w * 0.28f);
        float cardH = 56f;
        float cardX = w * 0.5f;
        float cardY = h * 0.82f;
        mediaCardRect.set(cardX - cardW / 2, cardY - cardH / 2, cardX + cardW / 2, cardY + cardH / 2);

        statusPillBgPaint.setColor(playing ? Color.parseColor("#E6121B26") : Color.parseColor("#CC0A1017"));
        canvas.drawRoundRect(mediaCardRect, 14f, 14f, statusPillBgPaint);
        statusPillBorderPaint.setColor(playing ? Color.parseColor("#00E5FF") : Color.parseColor("#25374C"));
        statusPillBorderPaint.setStrokeWidth(playing ? 1.5f : 1.0f);
        canvas.drawRoundRect(mediaCardRect, 14f, 14f, statusPillBorderPaint);

        // Album Art thumbnail (gradient purple / dark slate)
        RectF artRect = new RectF(mediaCardRect.left + 8f, cardY - 20f, mediaCardRect.left + 48f, cardY + 20f);
        statusPillBgPaint.setColor(playing ? Color.parseColor("#7B1FA2") : Color.parseColor("#37474F"));
        canvas.drawRoundRect(artRect, 8f, 8f, statusPillBgPaint);

        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(13));
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(playing ? "♪" : "♫", artRect.centerX(), artRect.centerY() + 5f, subTextPaint);

        // Track title & artist
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(11.5f));
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        String title = (trackTitle != null && !trackTitle.isEmpty()) ? trackTitle : (playing ? "Воспроизведение..." : "Synthwave Drive");
        String artist = (trackArtist != null && !trackArtist.isEmpty()) ? trackArtist : (playing ? "Подключено" : "Night Rider");
        canvas.drawText(title, mediaCardRect.left + 56f, cardY - 4f, subTextPaint);

        subTextPaint.setColor(Color.parseColor("#78909C"));
        subTextPaint.setTextSize(spToPx(9.5f));
        canvas.drawText(artist, mediaCardRect.left + 56f, cardY + 14f, subTextPaint);
        subTextPaint.setTextAlign(Paint.Align.CENTER);

        // Play/Pause & Next icons
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(13));
        canvas.drawText(playing ? "❚❚" : "▶", mediaCardRect.right - 44f, cardY + 5f, subTextPaint);
        canvas.drawText("⏭", mediaCardRect.right - 18f, cardY + 5f, subTextPaint);
    }

    private final RectF splitDstRect = new RectF();
    private final Paint splitBmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    private void drawSplitBitmapTheme(Canvas canvas, int w, int h, int splitId) {
        canvas.drawColor(Color.parseColor("#07090E"));

        float dialSize = gaugeRadius * 2.15f;
        float half = dialSize / 2f;

        // 1. Speedometer Dial (Left)
        Bitmap spdBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "spd");
        if (spdBmp != null) {
            splitDstRect.set(leftCenterX - half, centerY - half, leftCenterX + half, centerY + half);
            canvas.drawBitmap(spdBmp, null, splitDstRect, splitBmpPaint);
        }

        // 2. Tachometer Dial (Right)
        Bitmap rpmBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "rpm");
        if (rpmBmp != null) {
            splitDstRect.set(rightCenterX - half, centerY - half, rightCenterX + half, centerY + half);
            canvas.drawBitmap(rpmBmp, null, splitDstRect, splitBmpPaint);
        }

        // 3. Needles (Theme needle PNGs default to 12 o'clock, rotation zero = top)
        Bitmap needleBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "needle");
        if (needleBmp != null) {
            // Speedometer needle (-135 to +135 deg sweep)
            canvas.save();
            float spdAngle = -135f + (Math.min(Math.max(displaySpeed, 0f), 240f) / 240f) * 270f;
            canvas.rotate(spdAngle, leftCenterX, centerY);
            splitDstRect.set(leftCenterX - half, centerY - half, leftCenterX + half, centerY + half);
            canvas.drawBitmap(needleBmp, null, splitDstRect, splitBmpPaint);
            canvas.restore();

            // Tachometer needle (-135 to +135 deg sweep)
            canvas.save();
            float rpmAngle = -135f + (Math.min(Math.max(displayRpm, 0f), 8000f) / 8000f) * 270f;
            canvas.rotate(rpmAngle, rightCenterX, centerY);
            splitDstRect.set(rightCenterX - half, centerY - half, rightCenterX + half, centerY + half);
            canvas.drawBitmap(needleBmp, null, splitDstRect, splitBmpPaint);
            canvas.restore();
        }

        // 4. Center Area Telemetry (Digital Speed / Mode / Gear)
        gearPaint.setColor(Color.WHITE);
        gearPaint.setTextSize(spToPx(52));
        canvas.drawText(gear, w * 0.5f, centerY - 8f, gearPaint);

        modePaint.setColor(Color.parseColor("#80FFFFFF"));
        modePaint.setTextSize(spToPx(13.5f));
        canvas.drawText(driveMode, w * 0.5f, centerY + 32f, modePaint);

        // Digital Speed under Left Dial
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(spToPx(34));
        canvas.drawText(String.valueOf(Math.round(displaySpeed)), leftCenterX, centerY + gaugeRadius * 0.44f, textValuePaint);
        textUnitPaint.setColor(Color.parseColor("#90A4AE"));
        textUnitPaint.setTextSize(spToPx(11.5f));
        canvas.drawText("KM/H", leftCenterX, centerY + gaugeRadius * 0.58f, textUnitPaint);

        // Digital RPM under Right Dial
        canvas.drawText(String.valueOf(Math.round(displayRpm)), rightCenterX, centerY + gaugeRadius * 0.44f, textValuePaint);
        canvas.drawText("RPM", rightCenterX, centerY + gaugeRadius * 0.58f, textUnitPaint);
    }

    private long lastTapTime = 0;

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            long now = System.currentTimeMillis();

            // 1. Floating Music Card Controls (Taycan theme)
            if (activeTheme == ClusterConfig.THEME_TAYCAN_ARC && mediaCardRect.contains(x, y)) {
                if (x > mediaCardRect.right - 35f) {
                    dispatchMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT);
                } else {
                    toggleMediaPlayback();
                }
                return true;
            }

            // 2. Double-tap in center region toggles HolePunch on the fly!
            if (now - lastTapTime < 350 && Math.abs(x - viewWidth * 0.5f) < viewWidth * 0.22f) {
                isHolePunchEnabled = !isHolePunchEnabled;
                ClusterConfig.setHolePunchEnabled(getContext(), isHolePunchEnabled);
                android.widget.Toast.makeText(getContext(),
                        "Островок (HolePunch): " + (isHolePunchEnabled ? "ВКЛЮЧЕН" : "ВЫКЛЮЧЕН"),
                        android.widget.Toast.LENGTH_SHORT).show();
                invalidate();
                return true;
            }
            lastTapTime = now;
        }
        return super.onTouchEvent(event);
    }

    private void dispatchMediaKey(int keyCode) {
        try {
            android.media.AudioManager am = (android.media.AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                long eventTime = android.os.SystemClock.uptimeMillis();
                android.view.KeyEvent down = new android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_DOWN, keyCode, 0);
                android.view.KeyEvent up = new android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_UP, keyCode, 0);
                am.dispatchMediaKeyEvent(down);
                am.dispatchMediaKeyEvent(up);
            }
        } catch (Throwable ignored) {}
    }

    private void toggleMediaPlayback() {
        dispatchMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
    }

    private void drawTaycanSpeedoArc(Canvas canvas, float cx, float cy, float radius, int w, int h) {
        float startAngle = 135f;
        float sweepAngle = 270f;
        float arcR = radius * 0.86f;
        arcRect.set(cx - arcR, cy - arcR, cx + arcR, cy + arcR);

        // 1. Digital Clock (Top-Left exterior)
        String timeStr = clockFormat.format(new java.util.Date());
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.parseColor("#ECEFF1"));
        subTextPaint.setTextSize(spToPx(17));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(timeStr, w * 0.055f, h * 0.12f, subTextPaint);

        // 2. 4-Wheel TPMS card (Top-Right exterior)
        float tpmsX = cx + arcR * 0.90f;
        float tpmsY = h * 0.08f;
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.parseColor("#78909C"));
        subTextPaint.setTextSize(spToPx(10.5f));

        // Car bracket symbol
        statusPillBorderPaint.setColor(Color.parseColor("#546E7A"));
        statusPillBorderPaint.setStrokeWidth(1.8f);
        canvas.drawArc(new RectF(tpmsX - 16f, tpmsY, tpmsX - 4f, tpmsY + 44f), 100f, 160f, false, statusPillBorderPaint);

        float barVal = (latestState != null) ? latestState.tirePressureBar : 2.5f;
        String p1 = String.format(java.util.Locale.US, "FL: %.1f", barVal);
        String p2 = String.format(java.util.Locale.US, "FR: %.1f", barVal);
        String p3 = String.format(java.util.Locale.US, "RL: %.1f", barVal + 0.1f);
        String p4 = String.format(java.util.Locale.US, "RR: %.1f bar", barVal + 0.1f);
        canvas.drawText(p1, tpmsX, tpmsY + 10f, subTextPaint);
        canvas.drawText(p2, tpmsX, tpmsY + 22f, subTextPaint);
        canvas.drawText(p3, tpmsX, tpmsY + 34f, subTextPaint);
        canvas.drawText(p4, tpmsX, tpmsY + 46f, subTextPaint);

        // 3. Deep Trench Base Track
        activeArcPaint.setColor(Color.parseColor("#0A1420"));
        activeArcPaint.setStrokeWidth(22f);
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, activeArcPaint);

        cockpitRimPaint.setColor(Color.parseColor("#152538"));
        cockpitRimPaint.setStrokeWidth(1.5f);
        canvas.drawArc(new RectF(cx - arcR - 11f, cy - arcR - 11f, cx + arcR + 11f, cy + arcR + 11f), startAngle, sweepAngle, false, cockpitRimPaint);
        canvas.drawArc(new RectF(cx - arcR + 11f, cy - arcR + 11f, cx + arcR - 11f, cy + arcR - 11f), startAngle, sweepAngle, false, cockpitRimPaint);

        // 4. Standby & Active Glowing Neon Sweeps
        // Upper luminous accent sweep (Porsche Taycan signature glow)
        activeArcPaint.setColor(Color.parseColor("#2600E5FF"));
        activeArcPaint.setStrokeWidth(30f);
        canvas.drawArc(arcRect, 215f, 110f, false, activeArcPaint);

        activeArcPaint.setColor(Color.parseColor("#00E5FF"));
        activeArcPaint.setStrokeWidth(14f);
        canvas.drawArc(arcRect, 215f, 110f, false, activeArcPaint);

        // Active dynamic speed sweep
        float speedRatio = Math.min(Math.max(displaySpeed / 240f, 0f), 1f);
        float activeSweep = speedRatio * sweepAngle;
        if (activeSweep > 1f) {
            activeArcPaint.setColor(Color.parseColor("#4400E5FF"));
            activeArcPaint.setStrokeWidth(32f);
            canvas.drawArc(arcRect, startAngle, activeSweep, false, activeArcPaint);

            activeArcPaint.setColor(Color.parseColor("#00E5FF"));
            activeArcPaint.setStrokeWidth(16f);
            canvas.drawArc(arcRect, startAngle, activeSweep, false, activeArcPaint);

            float tipAngle = startAngle + activeSweep;
            double tipRad = Math.toRadians(tipAngle);
            float tipX = (float) (cx + arcR * Math.cos(tipRad));
            float tipY = (float) (cy + arcR * Math.sin(tipRad));
            boltPaint.setColor(Color.WHITE);
            canvas.drawCircle(tipX, tipY, 7f, boltPaint);
        }

        // 5. Ticks & Outer Scale (0, 40, 80, 120, 160, 200, 240 + kW)
        for (int s = 0; s <= 240; s += 10) {
            float angle = startAngle + ((float) s / 240f) * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isMajor = (s % 20 == 0);
            boolean isLabel = (s % 40 == 0 || s == 0);

            float innerR = isMajor ? arcR + 10f : arcR + 14f;
            float outerR = arcR + 20f;
            float x1 = (float) (cx + innerR * Math.cos(rad));
            float y1 = (float) (cy + innerR * Math.sin(rad));
            float x2 = (float) (cx + outerR * Math.cos(rad));
            float y2 = (float) (cy + outerR * Math.sin(rad));

            tickMajorPaint.setColor(s <= displaySpeed ? Color.parseColor("#00E5FF") : Color.parseColor("#263648"));
            tickMajorPaint.setStrokeWidth(isMajor ? 2.5f : 1.2f);
            canvas.drawLine(x1, y1, x2, y2, tickMajorPaint);

            if (isLabel) {
                float textR = arcR + 36f;
                float tx = (float) (cx + textR * Math.cos(rad));
                float ty = (float) (cy + textR * Math.sin(rad) + 4f);
                textNumberPaint.setColor(s <= displaySpeed ? Color.WHITE : Color.parseColor("#607D8B"));
                textNumberPaint.setTextSize(spToPx(10.5f));
                canvas.drawText(String.valueOf(s), tx, ty, textNumberPaint);
            }
        }

        // kW unit at bottom tick
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#607D8B"));
        subTextPaint.setTextSize(spToPx(10));
        canvas.drawText("kW", cx - arcR + 12f, cy + arcR * 0.72f, subTextPaint);

        // 6. Inside Dial Content
        // Top: Odometer / Total
        subTextPaint.setColor(Color.parseColor("#78909C"));
        subTextPaint.setTextSize(spToPx(10.5f));
        canvas.drawText("Odometer", cx, cy - radius * 0.50f, subTextPaint);

        int odoVal = (latestState != null && latestState.odometerKm > 0) ? latestState.odometerKm : 14350;
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(13.5f));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText("Total: " + String.format(java.util.Locale.US, "%,d KM", odoVal), cx, cy - radius * 0.36f, subTextPaint);

        // Center: Digital Speed + KM/H
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(spToPx(52));
        canvas.drawText(String.valueOf(Math.round(displaySpeed)), cx, cy + 8f, textValuePaint);

        textUnitPaint.setColor(Color.parseColor("#00E5FF"));
        textUnitPaint.setTextSize(spToPx(12));
        canvas.drawText("KM/H", cx, cy + 30f, textUnitPaint);

        // Below Center: Trip: 89 KM
        subTextPaint.setColor(Color.parseColor("#80DEEA"));
        subTextPaint.setTextSize(spToPx(12.5f));
        canvas.drawText("Trip: 89 KM", cx, cy + radius * 0.35f, subTextPaint);

        // Bottom Inside: Instant Power Use + Pill [+18 °C]
        subTextPaint.setColor(Color.parseColor("#78909C"));
        subTextPaint.setTextSize(spToPx(10));
        canvas.drawText("Instant Power Use", cx, cy + radius * 0.56f, subTextPaint);

        float pillW = 76f;
        float pillH = 22f;
        float pillCy = cy + radius * 0.74f;
        pillRect.set(cx - pillW / 2, pillCy - pillH / 2, cx + pillW / 2, pillCy + pillH / 2);
        statusPillBgPaint.setColor(Color.parseColor("#0A1826"));
        canvas.drawRoundRect(pillRect, 11f, 11f, statusPillBgPaint);
        statusPillBorderPaint.setColor(Color.parseColor("#1B3346"));
        statusPillBorderPaint.setStrokeWidth(1.2f);
        canvas.drawRoundRect(pillRect, 11f, 11f, statusPillBorderPaint);

        float tempVal = (latestState != null) ? latestState.outsideTempC : 18f;
        subTextPaint.setColor(Color.parseColor("#B0BEC5"));
        subTextPaint.setTextSize(spToPx(10.5f));
        canvas.drawText((tempVal >= 0 ? "+" : "") + Math.round(tempVal) + " °C", cx, pillCy + 4f, subTextPaint);

        // 7. Footer Under Left Dial
        subTextPaint.setTextSize(spToPx(14));
        subTextPaint.setColor(Color.parseColor("#78909C"));
        String rangeStr = ((latestState != null && latestState.rangeKm > 0) ? latestState.rangeKm : 350) + " KM";
        canvas.drawText("Range Left: ", cx - 30f, h - 28f, subTextPaint);
        subTextPaint.setColor(Color.parseColor("#00E5FF"));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(rangeStr, cx + 46f, h - 28f, subTextPaint);
    }

    private void drawTaycanTachoArc(Canvas canvas, float cx, float cy, float radius, int w, int h) {
        float startAngle = 135f;
        float sweepAngle = 270f;
        float arcR = radius * 0.86f;
        arcRect.set(cx - arcR, cy - arcR, cx + arcR, cy + arcR);

        // 1. Exterior Temperatures (Top)
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setTextSize(spToPx(12));
        subTextPaint.setColor(Color.parseColor("#78909C"));
        canvas.drawText("Battery Temp:", w * 0.71f, h * 0.12f, subTextPaint);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setFakeBoldText(true);
        canvas.drawText("38°C", w * 0.71f, h * 0.155f, subTextPaint);

        subTextPaint.setTextAlign(Paint.Align.RIGHT);
        subTextPaint.setColor(Color.parseColor("#78909C"));
        subTextPaint.setFakeBoldText(false);
        canvas.drawText("Motor Temp:", w * 0.95f, h * 0.12f, subTextPaint);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setFakeBoldText(true);
        canvas.drawText("65°C", w * 0.95f, h * 0.155f, subTextPaint);

        // 2. Base Trench Track
        activeArcPaint.setColor(Color.parseColor("#0A1420"));
        activeArcPaint.setStrokeWidth(22f);
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, activeArcPaint);

        cockpitRimPaint.setColor(Color.parseColor("#152538"));
        cockpitRimPaint.setStrokeWidth(1.5f);
        canvas.drawArc(new RectF(cx - arcR - 11f, cy - arcR - 11f, cx + arcR + 11f, cy + arcR + 11f), startAngle, sweepAngle, false, cockpitRimPaint);
        canvas.drawArc(new RectF(cx - arcR + 11f, cy - arcR + 11f, cx + arcR - 11f, cy + arcR - 11f), startAngle, sweepAngle, false, cockpitRimPaint);

        // 3. Upper-Left Glowing Power Arc
        activeArcPaint.setColor(Color.parseColor("#2600E5FF"));
        activeArcPaint.setStrokeWidth(30f);
        canvas.drawArc(arcRect, 195f, 80f, false, activeArcPaint);

        activeArcPaint.setColor(Color.parseColor("#00E5FF"));
        activeArcPaint.setStrokeWidth(14f);
        canvas.drawArc(arcRect, 195f, 80f, false, activeArcPaint);

        // 4. Inner Secondary Regen/Power Arc (Right Flank)
        float innerR = arcR * 0.76f;
        innerArcPaint.setColor(Color.parseColor("#00E5FF"));
        innerArcPaint.setStrokeWidth(8f);
        canvas.drawArc(new RectF(cx - innerR, cy - innerR, cx + innerR, cy + innerR), 330f, 75f, false, innerArcPaint);

        // 5. Active RPM Arc (Cyan to Redline Red)
        float rpmRatio = Math.min(Math.max(displayRpm / 8000f, 0f), 1f);
        float activeSweep = rpmRatio * sweepAngle;
        if (activeSweep > 1f) {
            int glowColor = displayRpm >= 5000 ? Color.parseColor("#44FF1744") : Color.parseColor("#3300E5FF");
            int coreColor = displayRpm >= 5000 ? Color.parseColor("#FF1744") : Color.parseColor("#00E5FF");

            activeArcPaint.setColor(glowColor);
            activeArcPaint.setStrokeWidth(30f);
            canvas.drawArc(arcRect, startAngle, activeSweep, false, activeArcPaint);

            activeArcPaint.setColor(coreColor);
            activeArcPaint.setStrokeWidth(14f);
            canvas.drawArc(arcRect, startAngle, activeSweep, false, activeArcPaint);

            float tipAngle = startAngle + activeSweep;
            double tipRad = Math.toRadians(tipAngle);
            float tipX = (float) (cx + arcR * Math.cos(tipRad));
            float tipY = (float) (cy + arcR * Math.sin(tipRad));
            boltPaint.setColor(Color.WHITE);
            canvas.drawCircle(tipX, tipY, 7f, boltPaint);
        }

        // 6. Ticks and Numbers (0..8)
        for (int r = 0; r <= 8000; r += 250) {
            float angle = startAngle + ((float) r / 8000f) * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isMajor = (r % 1000 == 0);
            boolean isRedline = r >= 5000;

            float inR = isMajor ? arcR + 10f : arcR + 14f;
            float outR = arcR + 20f;
            float x1 = (float) (cx + inR * Math.cos(rad));
            float y1 = (float) (cy + inR * Math.sin(rad));
            float x2 = (float) (cx + outR * Math.cos(rad));
            float y2 = (float) (cy + outR * Math.sin(rad));

            int tickCol = isRedline ? Color.parseColor("#FF1744") : (r <= displayRpm ? Color.parseColor("#00E5FF") : Color.parseColor("#263648"));
            tickMajorPaint.setColor(tickCol);
            tickMajorPaint.setStrokeWidth(isMajor ? 2.5f : 1.2f);
            canvas.drawLine(x1, y1, x2, y2, tickMajorPaint);

            if (isMajor) {
                float textR = arcR + 36f;
                float tx = (float) (cx + textR * Math.cos(rad));
                float ty = (float) (cy + textR * Math.sin(rad) + 4f);
                textNumberPaint.setColor(isRedline ? Color.parseColor("#FF5252") : (r <= displayRpm ? Color.WHITE : Color.parseColor("#607D8B")));
                textNumberPaint.setTextSize(spToPx(11));
                canvas.drawText(String.valueOf(r / 1000), tx, ty, textNumberPaint);
            }
        }

        // kW unit at bottom right tick
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#607D8B"));
        subTextPaint.setTextSize(spToPx(10));
        canvas.drawText("kW", cx + arcR - 12f, cy + arcR * 0.72f, subTextPaint);

        // 7. Inside Dial Content
        // Gear (P in Amber, D in Cyan, R in Red)
        int gearColor = "P".equals(gear) ? Color.parseColor("#FFB300") : ("R".equals(gear) ? Color.parseColor("#FF1744") : Color.parseColor("#00E5FF"));
        gearPaint.setColor(gearColor);
        gearPaint.setTextSize(spToPx(52));
        canvas.drawText(gear, cx, cy - radius * 0.12f, gearPaint);

        // Ready label
        subTextPaint.setColor(Color.parseColor("#CFD8DC"));
        subTextPaint.setTextSize(spToPx(13.5f));
        subTextPaint.setFakeBoldText(false);
        canvas.drawText("Ready", cx, cy + radius * 0.04f, subTextPaint);

        // Battery SOC Capsule Card [ 85% 🔋 ]
        float batCardW = 94f;
        float batCardH = 26f;
        float batCardY = cy + radius * 0.22f;
        batteryCardRect.set(cx - batCardW / 2, batCardY - batCardH / 2, cx + batCardW / 2, batCardY + batCardH / 2);
        statusPillBgPaint.setColor(Color.parseColor("#081827"));
        canvas.drawRoundRect(batteryCardRect, 6f, 6f, statusPillBgPaint);
        statusPillBorderPaint.setColor(Color.parseColor("#00E5FF"));
        statusPillBorderPaint.setStrokeWidth(1.4f);
        canvas.drawRoundRect(batteryCardRect, 6f, 6f, statusPillBorderPaint);

        int socVal = (latestState != null && latestState.fuelPercent > 0) ? latestState.fuelPercent : 85;
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.parseColor("#00E5FF"));
        subTextPaint.setTextSize(spToPx(12.5f));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(socVal + "%", batteryCardRect.left + 10f, batCardY + 4f, subTextPaint);

        // Battery icon glyph with fill bar
        float bIconLeft = batteryCardRect.right - 28f;
        RectF bIcon = new RectF(bIconLeft, batCardY - 6f, bIconLeft + 16f, batCardY + 6f);
        statusPillBorderPaint.setColor(Color.parseColor("#00E5FF"));
        statusPillBorderPaint.setStrokeWidth(1.2f);
        canvas.drawRoundRect(bIcon, 2f, 2f, statusPillBorderPaint);
        canvas.drawRect(bIcon.right, batCardY - 3f, bIcon.right + 2f, batCardY + 3f, statusPillBorderPaint);

        statusPillBgPaint.setColor(Color.parseColor("#00E676"));
        canvas.drawRect(bIcon.left + 2f, bIcon.top + 2f, bIcon.left + 2f + 12f * (socVal / 100f), bIcon.bottom - 2f, statusPillBgPaint);

        // Regen Braking & Pill [ 540 KM ]
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#78909C"));
        subTextPaint.setTextSize(spToPx(10));
        canvas.drawText("Regen Braking", cx, cy + radius * 0.44f, subTextPaint);

        float rPillW = 76f;
        float rPillH = 22f;
        float rPillCy = cy + radius * 0.62f;
        pillRect.set(cx - rPillW / 2, rPillCy - rPillH / 2, cx + rPillW / 2, rPillCy + rPillH / 2);
        statusPillBgPaint.setColor(Color.parseColor("#0A1826"));
        canvas.drawRoundRect(pillRect, 11f, 11f, statusPillBgPaint);
        statusPillBorderPaint.setColor(Color.parseColor("#1B3346"));
        statusPillBorderPaint.setStrokeWidth(1.2f);
        canvas.drawRoundRect(pillRect, 11f, 11f, statusPillBorderPaint);

        subTextPaint.setColor(Color.parseColor("#80DEEA"));
        subTextPaint.setTextSize(spToPx(10.5f));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(((latestState != null && latestState.rangeKm > 0) ? latestState.rangeKm : 540) + " KM", cx, rPillCy + 4f, subTextPaint);

        // 8. Footer Under Right Dial
        subTextPaint.setTextSize(spToPx(14));
        subTextPaint.setColor(Color.parseColor("#78909C"));
        canvas.drawText("Next Charge: ", cx - 30f, h - 28f, subTextPaint);
        subTextPaint.setColor(Color.parseColor("#00E5FF"));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText("50 KM", cx + 46f, h - 28f, subTextPaint);
    }

    // =========================================================================
    // 7. THEME: USER PACK (/sdcard/gwm_split_user_themes/theme1/)
    // =========================================================================
    private android.graphics.Bitmap userSpeedoBmp = null;
    private android.graphics.Bitmap userTachoBmp = null;
    private android.graphics.Bitmap userNeedleBmp = null;
    private android.graphics.Bitmap userBgBmp = null;
    private boolean userThemeLoaded = false;

    private void loadUserThemeBitmaps() {
        if (userThemeLoaded) return;
        userThemeLoaded = true;
        try {
            java.io.File dir = new java.io.File("/storage/emulated/0/gwm_split_user_themes/theme1");
            if (!dir.exists()) {
                dir = new java.io.File("/sdcard/gwm_split_user_themes/theme1");
            }
            if (dir.exists()) {
                java.io.File fSpeedo = new java.io.File(dir, "speedo.png");
                if (fSpeedo.exists()) userSpeedoBmp = android.graphics.BitmapFactory.decodeFile(fSpeedo.getAbsolutePath());
                java.io.File fTacho = new java.io.File(dir, "tacho.png");
                if (fTacho.exists()) userTachoBmp = android.graphics.BitmapFactory.decodeFile(fTacho.getAbsolutePath());
                java.io.File fNeedle = new java.io.File(dir, "needle.png");
                if (fNeedle.exists()) userNeedleBmp = android.graphics.BitmapFactory.decodeFile(fNeedle.getAbsolutePath());
                java.io.File fBg = new java.io.File(dir, "bg.png");
                if (fBg.exists()) userBgBmp = android.graphics.BitmapFactory.decodeFile(fBg.getAbsolutePath());
            }
        } catch (Throwable ignored) {}
    }

    private void drawUserTheme(Canvas canvas, int w, int h) {
        loadUserThemeBitmaps();
        if (userSpeedoBmp != null || userTachoBmp != null) {
            if (userBgBmp != null) {
                canvas.drawBitmap(userBgBmp, null, new RectF(0, 0, w, h), null);
            } else {
                canvas.drawColor(Color.BLACK);
            }
            if (userSpeedoBmp != null) {
                float halfW = gaugeRadius;
                RectF dst = new RectF(leftCenterX - halfW, centerY - halfW, leftCenterX + halfW, centerY + halfW);
                canvas.drawBitmap(userSpeedoBmp, null, dst, null);
            }
            if (userTachoBmp != null) {
                float halfW = gaugeRadius;
                RectF dst = new RectF(rightCenterX - halfW, centerY - halfW, rightCenterX + halfW, centerY + halfW);
                canvas.drawBitmap(userTachoBmp, null, dst, null);
            }
            if (userNeedleBmp != null) {
                float spdAngle = getSpeedAngle(displaySpeed, 240);
                canvas.save();
                canvas.rotate(spdAngle, leftCenterX, centerY);
                canvas.drawBitmap(userNeedleBmp, leftCenterX - userNeedleBmp.getWidth() / 2f, centerY - userNeedleBmp.getHeight() / 2f, null);
                canvas.restore();

                float rpmAngle = getRpmAngle(displayRpm, 8000);
                canvas.save();
                canvas.rotate(rpmAngle, rightCenterX, centerY);
                canvas.drawBitmap(userNeedleBmp, rightCenterX - userNeedleBmp.getWidth() / 2f, centerY - userNeedleBmp.getHeight() / 2f, null);
                canvas.restore();
            } else {
                drawSportNeedle(canvas, leftCenterX, centerY, gaugeRadius * 0.82f, getSpeedAngle(displaySpeed, 240), Color.RED);
                drawSportNeedle(canvas, rightCenterX, centerY, gaugeRadius * 0.82f, getRpmAngle(displayRpm, 8000), Color.RED);
            }
        } else {
            // Placeholder when user theme is empty
            drawTaycanArcTheme(canvas, w, h);
            subTextPaint.setColor(Color.parseColor("#FFD600"));
            subTextPaint.setTextSize(spToPx(12));
            canvas.drawText("ПОЛЬЗОВАТЕЛЬСКАЯ ТЕМА: положите speedo.png, tacho.png в /sdcard/gwm_split_user_themes/theme1/", w * 0.5f, h - 35f, subTextPaint);
        }
    }

    private void drawMiniWidgets(Canvas canvas) {
        if (activeTheme == ClusterConfig.THEME_TAYCAN_ARC) {
            // All telemetry cards & pills are natively integrated into the Taycan cyber cockpit layout
            return;
        }
        if (latestState == null) {
            latestState = new VehicleState((int) displaySpeed, gear, (int) displayRpm, true, coolantTemp);
        }

        widgetTextPaint.setTextSize(spToPx(11));

        // 1. Left Mini-Widget (Under Left Dial / Speedometer)
        if (widgetLeft != ClusterConfig.WIDGET_NONE) {
            String text = latestState.formatWidget(widgetLeft);
            if (!text.isEmpty()) {
                float textW = widgetTextPaint.measureText(text);
                float pillW = Math.max(130f, textW + 32f);
                float pillH = 28f;
                float cy = centerY + gaugeRadius * 0.74f;
                widgetLeftRect.set(leftCenterX - pillW / 2f, cy - pillH / 2f, leftCenterX + pillW / 2f, cy + pillH / 2f);
                canvas.drawRoundRect(widgetLeftRect, pillH / 2f, pillH / 2f, widgetBgPaint);
                canvas.drawRoundRect(widgetLeftRect, pillH / 2f, pillH / 2f, widgetBorderPaint);
                canvas.drawText(text, leftCenterX, cy + 4f, widgetTextPaint);
            }
        }

        // 2. Right Mini-Widget (Under Right Dial / Tachometer)
        if (widgetRight != ClusterConfig.WIDGET_NONE) {
            String text = latestState.formatWidget(widgetRight);
            if (!text.isEmpty()) {
                float textW = widgetTextPaint.measureText(text);
                float pillW = Math.max(130f, textW + 32f);
                float pillH = 28f;
                float cy = centerY + gaugeRadius * 0.74f;
                widgetRightRect.set(rightCenterX - pillW / 2f, cy - pillH / 2f, rightCenterX + pillW / 2f, cy + pillH / 2f);
                canvas.drawRoundRect(widgetRightRect, pillH / 2f, pillH / 2f, widgetBgPaint);
                canvas.drawRoundRect(widgetRightRect, pillH / 2f, pillH / 2f, widgetBorderPaint);
                canvas.drawText(text, rightCenterX, cy + 4f, widgetTextPaint);
            }
        }
    }
}
