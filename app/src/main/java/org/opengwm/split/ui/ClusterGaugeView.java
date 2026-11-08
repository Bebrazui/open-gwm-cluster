package org.opengwm.split.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import org.opengwm.split.vehicle.VehicleState;

/**
 * Custom Instrument Cluster View ("Колодцы") for GWM Tank 500 / Haval.
 * Supports multiple high-end themes (Sport Red, Luxury Gold, Cyber Neon, Off-Road Amber),
 * smooth needle physics, cruise-control indicators, and HolePunch safety masks.
 */
public class ClusterGaugeView extends View {

    private int activeTheme = ClusterConfig.THEME_SPORT_RED;
    private boolean isHolePunchEnabled = true;
    private boolean isSmoothingEnabled = true;

    // Target telemetry
    private float targetSpeed = 0f;
    private float targetRpm = 0f;
    private String gear = "P";
    private float coolantTemp = 90.0f;
    private int cruiseSpeed = 0;
    private String driveMode = "STANDARD 4H";

    // Display smoothed values
    private float displaySpeed = 0f;
    private float displayRpm = 0f;

    // Paints (reused to prevent GC overhead at 60 FPS)
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dialBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickMajorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickMinorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleCapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint redlineArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint modePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cruiseMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePunchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF arcRect = new RectF();
    private final RectF centerBoxRect = new RectF();
    private final Path needlePath = new Path();
    private final Path cruiseMarkerPath = new Path();

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

        // Load saved preferences
        activeTheme = ClusterConfig.getTheme(getContext());
        isHolePunchEnabled = ClusterConfig.isHolePunchEnabled(getContext());
        isSmoothingEnabled = ClusterConfig.isSmoothingEnabled(getContext());
        driveMode = ClusterConfig.getDriveMode(getContext());
        cruiseSpeed = ClusterConfig.getCruiseSpeed(getContext());

        applyTheme(activeTheme);
    }

    public void setThemeStyle(int theme) {
        this.activeTheme = theme;
        ClusterConfig.setTheme(getContext(), theme);
        applyTheme(theme);
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

    public void applyTheme(int theme) {
        switch (theme) {
            case ClusterConfig.THEME_LUXURY_GOLD:
                // Tank 500 Luxury Chronograph
                bgPaint.setColor(Color.parseColor("#07080A"));
                dialBgPaint.setColor(Color.parseColor("#0C0E14"));
                ringOuterPaint.setColor(Color.parseColor("#C5A059"));
                ringOuterPaint.setStyle(Paint.Style.STROKE);
                ringOuterPaint.setStrokeWidth(7f);
                ringInnerPaint.setColor(Color.parseColor("#5A4725"));
                ringInnerPaint.setStyle(Paint.Style.STROKE);
                ringInnerPaint.setStrokeWidth(3f);
                tickMajorPaint.setColor(Color.parseColor("#F5E6C8"));
                tickMajorPaint.setStrokeWidth(4.5f);
                tickMinorPaint.setColor(Color.parseColor("#7A6848"));
                tickMinorPaint.setStrokeWidth(2f);
                textNumberPaint.setColor(Color.parseColor("#DFC69A"));
                textValuePaint.setColor(Color.parseColor("#FFF8E7"));
                textUnitPaint.setColor(Color.parseColor("#A89575"));
                needlePaint.setColor(Color.parseColor("#E5C158"));
                needleCapPaint.setColor(Color.parseColor("#C5A059"));
                needlePinPaint.setColor(Color.parseColor("#151821"));
                redlineArcPaint.setColor(Color.parseColor("#C57B30"));
                redlineArcPaint.setStyle(Paint.Style.STROKE);
                redlineArcPaint.setStrokeWidth(7f);
                centerBoxPaint.setColor(Color.parseColor("#0E1118"));
                centerBorderPaint.setColor(Color.parseColor("#5A4725"));
                centerBorderPaint.setStyle(Paint.Style.STROKE);
                centerBorderPaint.setStrokeWidth(3f);
                gearPaint.setColor(Color.parseColor("#E5C158"));
                modePaint.setColor(Color.parseColor("#C5A059"));
                tempPaint.setColor(Color.parseColor("#A89575"));
                cruiseMarkerPaint.setColor(Color.parseColor("#FFD54F"));
                break;

            case ClusterConfig.THEME_CYBER_NEON:
                // Futuristic Cyberpunk EV
                bgPaint.setColor(Color.parseColor("#03060F"));
                dialBgPaint.setColor(Color.parseColor("#070D1F"));
                ringOuterPaint.setColor(Color.parseColor("#00E5FF"));
                ringOuterPaint.setStyle(Paint.Style.STROKE);
                ringOuterPaint.setStrokeWidth(6f);
                ringInnerPaint.setColor(Color.parseColor("#FF007F"));
                ringInnerPaint.setStyle(Paint.Style.STROKE);
                ringInnerPaint.setStrokeWidth(3f);
                tickMajorPaint.setColor(Color.parseColor("#00E5FF"));
                tickMajorPaint.setStrokeWidth(4.5f);
                tickMinorPaint.setColor(Color.parseColor("#007080"));
                tickMinorPaint.setStrokeWidth(2f);
                textNumberPaint.setColor(Color.parseColor("#80F0FF"));
                textValuePaint.setColor(Color.parseColor("#00E5FF"));
                textUnitPaint.setColor(Color.parseColor("#00ACC1"));
                needlePaint.setColor(Color.parseColor("#00E5FF"));
                needleCapPaint.setColor(Color.parseColor("#18FFFF"));
                needlePinPaint.setColor(Color.parseColor("#FF007F"));
                redlineArcPaint.setColor(Color.parseColor("#FF007F"));
                redlineArcPaint.setStyle(Paint.Style.STROKE);
                redlineArcPaint.setStrokeWidth(8f);
                centerBoxPaint.setColor(Color.parseColor("#081024"));
                centerBorderPaint.setColor(Color.parseColor("#00E5FF"));
                centerBorderPaint.setStyle(Paint.Style.STROKE);
                centerBorderPaint.setStrokeWidth(3f);
                gearPaint.setColor(Color.parseColor("#FF007F"));
                modePaint.setColor(Color.parseColor("#00E5FF"));
                tempPaint.setColor(Color.parseColor("#80F0FF"));
                cruiseMarkerPaint.setColor(Color.parseColor("#00E5FF"));
                break;

            case ClusterConfig.THEME_OFFROAD_AMBER:
                // Tank 4x4 Desert Edition
                bgPaint.setColor(Color.parseColor("#0F1114"));
                dialBgPaint.setColor(Color.parseColor("#171A21"));
                ringOuterPaint.setColor(Color.parseColor("#455A64"));
                ringOuterPaint.setStyle(Paint.Style.STROKE);
                ringOuterPaint.setStrokeWidth(8f);
                ringInnerPaint.setColor(Color.parseColor("#FF8F00"));
                ringInnerPaint.setStyle(Paint.Style.STROKE);
                ringInnerPaint.setStrokeWidth(4f);
                tickMajorPaint.setColor(Color.parseColor("#FFB300"));
                tickMajorPaint.setStrokeWidth(5f);
                tickMinorPaint.setColor(Color.parseColor("#785B22"));
                tickMinorPaint.setStrokeWidth(2.5f);
                textNumberPaint.setColor(Color.parseColor("#FFD54F"));
                textValuePaint.setColor(Color.parseColor("#FFE082"));
                textUnitPaint.setColor(Color.parseColor("#FFB300"));
                needlePaint.setColor(Color.parseColor("#FF6D00"));
                needleCapPaint.setColor(Color.parseColor("#37474F"));
                needlePinPaint.setColor(Color.parseColor("#FF6D00"));
                redlineArcPaint.setColor(Color.parseColor("#FF3D00"));
                redlineArcPaint.setStyle(Paint.Style.STROKE);
                redlineArcPaint.setStrokeWidth(9f);
                centerBoxPaint.setColor(Color.parseColor("#1A1E26"));
                centerBorderPaint.setColor(Color.parseColor("#FF8F00"));
                centerBorderPaint.setStyle(Paint.Style.STROKE);
                centerBorderPaint.setStrokeWidth(3.5f);
                gearPaint.setColor(Color.parseColor("#FFA000"));
                modePaint.setColor(Color.parseColor("#FFB300"));
                tempPaint.setColor(Color.parseColor("#FFD54F"));
                cruiseMarkerPaint.setColor(Color.parseColor("#FF6D00"));
                break;

            case ClusterConfig.THEME_SPORT_RED:
            default:
                // AMG / RS Track Redline
                bgPaint.setColor(Color.parseColor("#08090C"));
                dialBgPaint.setColor(Color.parseColor("#12141A"));
                ringOuterPaint.setColor(Color.parseColor("#262D38"));
                ringOuterPaint.setStyle(Paint.Style.STROKE);
                ringOuterPaint.setStrokeWidth(7f);
                ringInnerPaint.setColor(Color.parseColor("#D50000"));
                ringInnerPaint.setStyle(Paint.Style.STROKE);
                ringInnerPaint.setStrokeWidth(3.5f);
                tickMajorPaint.setColor(Color.parseColor("#FFFFFF"));
                tickMajorPaint.setStrokeWidth(4.5f);
                tickMinorPaint.setColor(Color.parseColor("#546E7A"));
                tickMinorPaint.setStrokeWidth(2f);
                textNumberPaint.setColor(Color.parseColor("#ECEFF1"));
                textValuePaint.setColor(Color.WHITE);
                textUnitPaint.setColor(Color.parseColor("#90A4AE"));
                needlePaint.setColor(Color.parseColor("#FF1744"));
                needleCapPaint.setColor(Color.parseColor("#37474F"));
                needlePinPaint.setColor(Color.parseColor("#FF1744"));
                redlineArcPaint.setColor(Color.parseColor("#FF1744"));
                redlineArcPaint.setStyle(Paint.Style.STROKE);
                redlineArcPaint.setStrokeWidth(8f);
                centerBoxPaint.setColor(Color.parseColor("#12151E"));
                centerBorderPaint.setColor(Color.parseColor("#D50000"));
                centerBorderPaint.setStyle(Paint.Style.STROKE);
                centerBorderPaint.setStrokeWidth(3f);
                gearPaint.setColor(Color.parseColor("#00E676"));
                modePaint.setColor(Color.parseColor("#FF5252"));
                tempPaint.setColor(Color.parseColor("#90CAF9"));
                cruiseMarkerPaint.setColor(Color.parseColor("#00E5FF"));
                break;
        }

        // Common text alignments and sizes
        textNumberPaint.setTextAlign(Paint.Align.CENTER);
        textNumberPaint.setTextSize(17f);
        textNumberPaint.setFakeBoldText(true);

        textValuePaint.setTextAlign(Paint.Align.CENTER);
        textValuePaint.setTextSize(38f);
        textValuePaint.setFakeBoldText(true);

        textUnitPaint.setTextAlign(Paint.Align.CENTER);
        textUnitPaint.setTextSize(14f);

        gearPaint.setTextAlign(Paint.Align.CENTER);
        gearPaint.setTextSize(58f);
        gearPaint.setFakeBoldText(true);

        modePaint.setTextAlign(Paint.Align.CENTER);
        modePaint.setTextSize(16f);
        modePaint.setFakeBoldText(true);

        tempPaint.setTextAlign(Paint.Align.CENTER);
        tempPaint.setTextSize(15f);

        needlePaint.setStyle(Paint.Style.FILL);
        needleCapPaint.setStyle(Paint.Style.FILL);
        needlePinPaint.setStyle(Paint.Style.FILL);
        cruiseMarkerPaint.setStyle(Paint.Style.FILL);
    }

    public void updateTelemetry(VehicleState state) {
        this.targetSpeed = state.speedKmh;
        this.targetRpm = state.rpm;
        this.gear = state.gear;
        this.coolantTemp = state.targetTempC;

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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Smooth needle animation physics (interpolates toward target values at 60 FPS)
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

        // Draw solid background
        canvas.drawRect(0, 0, w, h, bgPaint);

        float gaugeRadius = Math.min(w * 0.22f, h * 0.42f);
        float centerY = h * 0.52f;
        float leftCenterX = w * 0.25f;
        float rightCenterX = w * 0.75f;

        // 1. Draw Speedometer (Left Dial)
        drawSpeedometer(canvas, leftCenterX, centerY, gaugeRadius);

        // 2. Draw Tachometer (Right Dial)
        drawTachometer(canvas, rightCenterX, centerY, gaugeRadius);

        // 3. Draw Center Display (Gear, 4x4 Drive Mode, Temp)
        drawCenterInfo(canvas, w * 0.5f, centerY, gaugeRadius);

        // 4. Safe HolePunch Cutout Mask (Protects physical turn signals & icons)
        if (isHolePunchEnabled) {
            drawHolePunchMask(canvas, w, h);
        }

        if (needsInvalidate) {
            postInvalidateOnAnimation();
        }
    }

    private void drawSpeedometer(Canvas canvas, float cx, float cy, float radius) {
        // Dial body
        canvas.drawCircle(cx, cy, radius, dialBgPaint);
        canvas.drawCircle(cx, cy, radius, ringOuterPaint);
        canvas.drawCircle(cx, cy, radius * 0.94f, ringInnerPaint);

        float startAngle = 135f;
        float sweepAngle = 270f;
        int maxSpeed = 240;

        // Ticks and scale numerals
        for (int s = 0; s <= maxSpeed; s += 10) {
            float angle = startAngle + ((float) s / maxSpeed) * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isMajor = (s % 20 == 0);
            float innerRadius = isMajor ? radius * 0.77f : radius * 0.85f;
            float outerRadius = radius * 0.91f;

            float x1 = (float) (cx + innerRadius * Math.cos(rad));
            float y1 = (float) (cy + innerRadius * Math.sin(rad));
            float x2 = (float) (cx + outerRadius * Math.cos(rad));
            float y2 = (float) (cy + outerRadius * Math.sin(rad));

            canvas.drawLine(x1, y1, x2, y2, isMajor ? tickMajorPaint : tickMinorPaint);

            if (isMajor && (s % 40 == 0 || s == 0)) {
                float textRadius = radius * 0.63f;
                float tx = (float) (cx + textRadius * Math.cos(rad));
                float ty = (float) (cy + textRadius * Math.sin(rad) + 6f);
                canvas.drawText(String.valueOf(s), tx, ty, textNumberPaint);
            }
        }

        // Cruise speed indicator bug
        if (cruiseSpeed > 0 && cruiseSpeed <= maxSpeed) {
            float cAngle = startAngle + ((float) cruiseSpeed / maxSpeed) * sweepAngle;
            drawCruiseMarker(canvas, cx, cy, radius * 0.94f, cAngle);
        }

        // Sweeping needle
        float clampedSpeed = Math.min(Math.max(displaySpeed, 0), maxSpeed);
        float needleAngle = startAngle + (clampedSpeed / maxSpeed) * sweepAngle;
        drawNeedle(canvas, cx, cy, radius * 0.81f, needleAngle);

        // Digital Speed in center of dial
        canvas.drawText(String.valueOf(Math.round(displaySpeed)), cx, cy + radius * 0.38f, textValuePaint);
        canvas.drawText("KM/H", cx, cy + radius * 0.54f, textUnitPaint);
    }

    private void drawTachometer(Canvas canvas, float cx, float cy, float radius) {
        // Dial body
        canvas.drawCircle(cx, cy, radius, dialBgPaint);
        canvas.drawCircle(cx, cy, radius, ringOuterPaint);
        canvas.drawCircle(cx, cy, radius * 0.94f, ringInnerPaint);

        float startAngle = 135f;
        float sweepAngle = 270f;
        int maxRpm = 8000;

        // Redline Arc (from 5500 to 8000 RPM)
        arcRect.set(cx - radius * 0.89f, cy - radius * 0.89f, cx + radius * 0.89f, cy + radius * 0.89f);
        float redlineStart = startAngle + (5500f / maxRpm) * sweepAngle;
        float redlineSweep = (2500f / maxRpm) * sweepAngle;
        canvas.drawArc(arcRect, redlineStart, redlineSweep, false, redlineArcPaint);

        // Ticks and scale numerals
        for (int r = 0; r <= maxRpm; r += 250) {
            float angle = startAngle + ((float) r / maxRpm) * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isMajor = (r % 1000 == 0);
            float innerRadius = isMajor ? radius * 0.77f : radius * 0.85f;
            float outerRadius = radius * 0.91f;

            float x1 = (float) (cx + innerRadius * Math.cos(rad));
            float y1 = (float) (cy + innerRadius * Math.sin(rad));
            float x2 = (float) (cx + outerRadius * Math.cos(rad));
            float y2 = (float) (cy + outerRadius * Math.sin(rad));

            canvas.drawLine(x1, y1, x2, y2, isMajor ? tickMajorPaint : tickMinorPaint);

            if (isMajor) {
                float textRadius = radius * 0.63f;
                float tx = (float) (cx + textRadius * Math.cos(rad));
                float ty = (float) (cy + textRadius * Math.sin(rad) + 6f);
                canvas.drawText(String.valueOf(r / 1000), tx, ty, textNumberPaint);
            }
        }

        // Sweeping needle
        float clampedRpm = Math.min(Math.max(displayRpm, 0), maxRpm);
        float needleAngle = startAngle + (clampedRpm / maxRpm) * sweepAngle;
        drawNeedle(canvas, cx, cy, radius * 0.81f, needleAngle);

        // Digital RPM readout in center of dial
        canvas.drawText(String.valueOf(Math.round(displayRpm)), cx, cy + radius * 0.38f, textValuePaint);
        canvas.drawText("RPM x1000", cx, cy + radius * 0.54f, textUnitPaint);
    }

    private void drawNeedle(Canvas canvas, float cx, float cy, float length, float angleDeg) {
        canvas.save();
        canvas.rotate(angleDeg, cx, cy);

        needlePath.reset();
        needlePath.moveTo(cx - 16f, cy - 3.5f);
        needlePath.lineTo(cx + length, cy);
        needlePath.lineTo(cx - 16f, cy + 3.5f);
        needlePath.close();

        canvas.drawPath(needlePath, needlePaint);
        canvas.drawCircle(cx, cy, 14f, needleCapPaint);
        canvas.drawCircle(cx, cy, 6f, needlePinPaint);

        canvas.restore();
    }

    private void drawCruiseMarker(Canvas canvas, float cx, float cy, float markerRadius, float angleDeg) {
        canvas.save();
        canvas.rotate(angleDeg, cx, cy);

        cruiseMarkerPath.reset();
        cruiseMarkerPath.moveTo(cx + markerRadius, cy);
        cruiseMarkerPath.lineTo(cx + markerRadius + 14f, cy - 8f);
        cruiseMarkerPath.lineTo(cx + markerRadius + 14f, cy + 8f);
        cruiseMarkerPath.close();

        canvas.drawPath(cruiseMarkerPath, cruiseMarkerPaint);
        canvas.restore();
    }

    private void drawCenterInfo(Canvas canvas, float cx, float cy, float radius) {
        float boxW = radius * 1.55f;
        float boxH = radius * 1.65f;
        centerBoxRect.set(cx - boxW / 2, cy - boxH / 2, cx + boxW / 2, cy + boxH / 2);

        canvas.drawRoundRect(centerBoxRect, 20f, 20f, centerBoxPaint);
        canvas.drawRoundRect(centerBoxRect, 20f, 20f, centerBorderPaint);

        // Gear display (P, R, N, D, D1..D9)
        canvas.drawText(gear, cx, cy - 22f, gearPaint);

        // Drive Mode
        canvas.drawText(driveMode, cx, cy + 26f, modePaint);

        // Coolant Temp
        canvas.drawText("TEMP: " + Math.round(coolantTemp) + " °C", cx, cy + 68f, tempPaint);
    }

    private void drawHolePunchMask(Canvas canvas, int w, int h) {
        // Upper center window for factory turn signals, high-beam, and alert icons
        float maskW = w * 0.36f;
        float maskH = h * 0.17f;
        float left = (w - maskW) / 2f;
        RectF topAperture = new RectF(left, 0f, left + maskW, maskH);
        canvas.drawRoundRect(topAperture, 20f, 20f, holePunchPaint);

        // Bottom status line cutout (factory total mileage / odometer)
        float bottomH = h * 0.10f;
        RectF bottomAperture = new RectF(w * 0.34f, h - bottomH, w * 0.66f, h);
        canvas.drawRect(bottomAperture, holePunchPaint);
    }
}
