package org.opengwm.split.ui;

import android.content.Context;
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
    private boolean isHolePunchEnabled = true;
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

        // Draw Configurable Telemetry Mini-Widgets
        drawMiniWidgets(canvas);

        // Safe HolePunch Cutout Mask (protects factory indicators & odometer)
        if (isHolePunchEnabled) {
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
    // 6. THEME: TAYCAN ARC (Theme 51 Vector Neon Sweeps, Electric Pulse)
    // =========================================================================
    private void drawTaycanArcTheme(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.parseColor("#05070C"));

        // Speedometer Arc Left (cx = leftCenterX, cy = centerY)
        drawTaycanSpeedoArc(canvas, leftCenterX, centerY, gaugeRadius * 0.92f);

        // Tachometer / Power Arc Right (cx = rightCenterX, cy = centerY)
        drawTaycanTachoArc(canvas, rightCenterX, centerY, gaugeRadius * 0.92f);

        // Center HUD when HolePunch is disabled
        if (!isHolePunchEnabled) {
            float boxW = Math.min(viewWidth * 0.14f, 260f);
            float boxH = gaugeRadius * 1.25f;
            centerBoxRect.set(w * 0.5f - boxW / 2, centerY - boxH / 2, w * 0.5f + boxW / 2, centerY + boxH / 2);

            centerBoxBgPaint.setColor(Color.parseColor("#090E18"));
            canvas.drawRoundRect(centerBoxRect, 14f, 14f, centerBoxBgPaint);

            centerBoxBorderPaint.setColor(Color.parseColor("#00E5FF"));
            centerBoxBorderPaint.setStrokeWidth(2f);
            canvas.drawRoundRect(centerBoxRect, 14f, 14f, centerBoxBorderPaint);

            subTextPaint.setColor(Color.parseColor("#00E5FF"));
            subTextPaint.setTextSize(spToPx(11));
            canvas.drawText("PORSCHE TAYCAN ARC", w * 0.5f, centerBoxRect.top + 24f, subTextPaint);

            gearPaint.setColor(Color.parseColor("#00E5FF"));
            gearPaint.setTextSize(54f);
            canvas.drawText(gear, w * 0.5f, centerY + 10f, gearPaint);

            modePaint.setColor(Color.WHITE);
            modePaint.setTextSize(spToPx(13));
            canvas.drawText(driveMode, w * 0.5f, centerY + 42f, modePaint);

            subTextPaint.setColor(Color.parseColor("#78909C"));
            canvas.drawText(Math.round(coolantTemp) + " °C", w * 0.5f, centerY + 66f, subTextPaint);
        }
    }

    private void drawTaycanSpeedoArc(Canvas canvas, float cx, float cy, float radius) {
        float startAngle = 135f;
        float sweepAngle = 270f;
        float arcR = radius * 0.82f;
        arcRect.set(cx - arcR, cy - arcR, cx + arcR, cy + arcR);

        // 1. Inactive background track
        activeArcPaint.setColor(Color.parseColor("#121929"));
        activeArcPaint.setStrokeWidth(14f);
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, activeArcPaint);

        // 2. Active luminous sweep arc
        float speedRatio = Math.min(Math.max(displaySpeed / 240f, 0f), 1f);
        float activeSweep = speedRatio * sweepAngle;

        if (activeSweep > 0.5f) {
            // Neon Glow Layer
            activeArcPaint.setColor(Color.parseColor("#3300E5FF"));
            activeArcPaint.setStrokeWidth(24f);
            canvas.drawArc(arcRect, startAngle, activeSweep, false, activeArcPaint);

            // Core Neon Gradient Arc
            activeArcPaint.setColor(Color.parseColor("#00E5FF"));
            activeArcPaint.setStrokeWidth(14f);
            canvas.drawArc(arcRect, startAngle, activeSweep, false, activeArcPaint);

            // Bright Tip
            float tipAngle = startAngle + activeSweep;
            double tipRad = Math.toRadians(tipAngle);
            float tipX = (float) (cx + arcR * Math.cos(tipRad));
            float tipY = (float) (cy + arcR * Math.sin(tipRad));
            boltPaint.setColor(Color.WHITE);
            canvas.drawCircle(tipX, tipY, 7f, boltPaint);
        }

        // 3. Ticks and Numbers (drawn OUTSIDE the arc to prevent center clutter)
        for (int s = 0; s <= 240; s += 10) {
            float angle = startAngle + ((float) s / 240f) * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isMajor = (s % 20 == 0);
            boolean isLabel = (s % 40 == 0 || s == 0);

            float innerR = isMajor ? arcR + 8f : arcR + 12f;
            float outerR = arcR + 18f;
            float x1 = (float) (cx + innerR * Math.cos(rad));
            float y1 = (float) (cy + innerR * Math.sin(rad));
            float x2 = (float) (cx + outerR * Math.cos(rad));
            float y2 = (float) (cy + outerR * Math.sin(rad));

            tickMajorPaint.setColor(s <= displaySpeed ? Color.parseColor("#00E5FF") : Color.parseColor("#263238"));
            tickMajorPaint.setStrokeWidth(isMajor ? 2.5f : 1.2f);
            canvas.drawLine(x1, y1, x2, y2, tickMajorPaint);

            if (isLabel) {
                float textR = arcR + 34f;
                float tx = (float) (cx + textR * Math.cos(rad));
                float ty = (float) (cy + textR * Math.sin(rad) + 4f);
                textNumberPaint.setColor(s <= displaySpeed ? Color.WHITE : Color.parseColor("#546E7A"));
                textNumberPaint.setTextSize(spToPx(10));
                canvas.drawText(String.valueOf(s), tx, ty, textNumberPaint);
            }
        }

        // 4. Large Digital Speed Readout in Center (clean open space)
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(spToPx(50));
        canvas.drawText(String.valueOf(Math.round(displaySpeed)), cx, cy + 2f, textValuePaint);

        textUnitPaint.setColor(Color.parseColor("#00E5FF"));
        textUnitPaint.setTextSize(spToPx(12));
        canvas.drawText("KM/H", cx, cy + 28f, textUnitPaint);
    }

    private void drawTaycanTachoArc(Canvas canvas, float cx, float cy, float radius) {
        float startAngle = 135f;
        float sweepAngle = 270f;
        float arcR = radius * 0.82f;
        arcRect.set(cx - arcR, cy - arcR, cx + arcR, cy + arcR);

        // 1. Inactive background track
        activeArcPaint.setColor(Color.parseColor("#121929"));
        activeArcPaint.setStrokeWidth(14f);
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, activeArcPaint);

        // 2. Redline segment (5500..8000 RPM)
        float redlineStart = startAngle + (5500f / 8000f) * sweepAngle;
        float redlineSweep = (2500f / 8000f) * sweepAngle;
        redlineArcPaint.setColor(Color.parseColor("#33FF1744"));
        redlineArcPaint.setStrokeWidth(14f);
        canvas.drawArc(arcRect, redlineStart, redlineSweep, false, redlineArcPaint);

        // 3. Active RPM Arc
        float rpmRatio = Math.min(Math.max(displayRpm / 8000f, 0f), 1f);
        float activeSweep = rpmRatio * sweepAngle;

        if (activeSweep > 0.5f) {
            int glowColor = displayRpm >= 5500 ? Color.parseColor("#44FF1744") : Color.parseColor("#3300E5FF");
            int coreColor = displayRpm >= 5500 ? Color.parseColor("#FF1744") : Color.parseColor("#00E5FF");

            activeArcPaint.setColor(glowColor);
            activeArcPaint.setStrokeWidth(24f);
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

        // 4. Ticks and Numbers (drawn OUTSIDE the arc to prevent center clutter)
        for (int r = 0; r <= 8000; r += 250) {
            float angle = startAngle + ((float) r / 8000f) * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isMajor = (r % 1000 == 0);
            boolean isRedline = r >= 5500;

            float innerR = isMajor ? arcR + 8f : arcR + 12f;
            float outerR = arcR + 18f;
            float x1 = (float) (cx + innerR * Math.cos(rad));
            float y1 = (float) (cy + innerR * Math.sin(rad));
            float x2 = (float) (cx + outerR * Math.cos(rad));
            float y2 = (float) (cy + outerR * Math.sin(rad));

            int tickCol = isRedline ? Color.parseColor("#FF1744") : (r <= displayRpm ? Color.parseColor("#00E5FF") : Color.parseColor("#263238"));
            tickMajorPaint.setColor(tickCol);
            tickMajorPaint.setStrokeWidth(isMajor ? 2.5f : 1.2f);
            canvas.drawLine(x1, y1, x2, y2, tickMajorPaint);

            if (isMajor) {
                float textR = arcR + 34f;
                float tx = (float) (cx + textR * Math.cos(rad));
                float ty = (float) (cy + textR * Math.sin(rad) + 4f);
                textNumberPaint.setColor(isRedline ? Color.parseColor("#FF5252") : (r <= displayRpm ? Color.WHITE : Color.parseColor("#546E7A")));
                textNumberPaint.setTextSize(spToPx(11));
                canvas.drawText(String.valueOf(r / 1000), tx, ty, textNumberPaint);
            }
        }

        // 5. Gear Readout in Center (clean open space)
        int gearColor = "P".equals(gear) ? Color.parseColor("#FFB300") : ("R".equals(gear) ? Color.parseColor("#FF1744") : Color.parseColor("#00E5FF"));
        gearPaint.setColor(gearColor);
        gearPaint.setTextSize(spToPx(46));
        canvas.drawText(gear, cx, cy - 2f, gearPaint);

        textUnitPaint.setColor(Color.parseColor("#90A4AE"));
        textUnitPaint.setTextSize(spToPx(12));
        canvas.drawText(Math.round(displayRpm) + " RPM", cx, cy + 26f, textUnitPaint);
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
