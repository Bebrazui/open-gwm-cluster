package org.opengwm.split.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import org.opengwm.split.vehicle.VehicleState;

/**
 * Custom Instrument Cluster View ("Колодцы").
 * Renders dual-gauge instrument panel (Speedometer + Tachometer) with center telemetry.
 */
public class ClusterGaugeView extends View {

    private int speedKmh = 0;
    private int rpm = 0;
    private String gear = "P";
    private float coolantTemp = 90.0f;
    private boolean isHolePunchEnabled = true;

    // Paints (reused to prevent GC overhead)
    private final Paint dialBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickMajorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickMinorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleCapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint redlineArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePunchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF arcRect = new RectF();
    private final Path needlePath = new Path();

    public ClusterGaugeView(Context context) {
        super(context);
        init();
    }

    public ClusterGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dialBgPaint.setColor(Color.parseColor("#101217"));
        dialBgPaint.setStyle(Paint.Style.FILL);

        ringPaint.setColor(Color.parseColor("#263238"));
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(6f);

        tickMajorPaint.setColor(Color.parseColor("#ECEFF1"));
        tickMajorPaint.setStrokeWidth(4f);
        tickMajorPaint.setStrokeCap(Paint.Cap.ROUND);

        tickMinorPaint.setColor(Color.parseColor("#607D8B"));
        tickMinorPaint.setStrokeWidth(2f);
        tickMinorPaint.setStrokeCap(Paint.Cap.ROUND);

        textNumberPaint.setColor(Color.parseColor("#B0BEC5"));
        textNumberPaint.setTextAlign(Paint.Align.CENTER);
        textNumberPaint.setTextSize(16f);

        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextAlign(Paint.Align.CENTER);
        textValuePaint.setTextSize(36f);
        textValuePaint.setFakeBoldText(true);

        textUnitPaint.setColor(Color.parseColor("#90A4AE"));
        textUnitPaint.setTextAlign(Paint.Align.CENTER);
        textUnitPaint.setTextSize(14f);

        needlePaint.setColor(Color.parseColor("#FF3D00"));
        needlePaint.setStyle(Paint.Style.FILL);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);

        needleCapPaint.setColor(Color.parseColor("#CFD8DC"));
        needleCapPaint.setStyle(Paint.Style.FILL);

        redlineArcPaint.setColor(Color.parseColor("#D50000"));
        redlineArcPaint.setStyle(Paint.Style.STROKE);
        redlineArcPaint.setStrokeWidth(8f);

        centerBoxPaint.setColor(Color.parseColor("#151922"));
        centerBoxPaint.setStyle(Paint.Style.FILL);

        holePunchPaint.setColor(Color.TRANSPARENT);
        holePunchPaint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));

        // Hardware layer is required for PorterDuff.Mode.CLEAR masking
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public void updateTelemetry(VehicleState state) {
        this.speedKmh = state.speedKmh;
        this.rpm = state.rpm;
        this.gear = state.gear;
        this.coolantTemp = state.targetTempC;
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Background
        canvas.drawColor(Color.parseColor("#08090C"));

        float gaugeRadius = Math.min(w * 0.22f, h * 0.42f);
        float centerY = h * 0.52f;
        float leftCenterX = w * 0.25f;
        float rightCenterX = w * 0.75f;

        // 1. Draw Left Dial: Speedometer (0 - 240 km/h)
        drawSpeedometer(canvas, leftCenterX, centerY, gaugeRadius);

        // 2. Draw Right Dial: Tachometer (0 - 8000 RPM)
        drawTachometer(canvas, rightCenterX, centerY, gaugeRadius);

        // 3. Draw Center Info Screen (Gear, Drive Mode, Temp)
        drawCenterInfo(canvas, w * 0.5f, centerY, gaugeRadius);

        // 4. Hole Punch Mask (Aperture for turn signals and warning lights)
        if (isHolePunchEnabled) {
            drawHolePunchMask(canvas, w, h);
        }
    }

    private void drawSpeedometer(Canvas canvas, float cx, float cy, float radius) {
        // Dial body & outer ring
        canvas.drawCircle(cx, cy, radius, dialBgPaint);
        canvas.drawCircle(cx, cy, radius, ringPaint);

        // Angles: from 135 deg to 405 deg (sweep = 270 deg)
        float startAngle = 135f;
        float sweepAngle = 270f;
        int maxSpeed = 240;

        // Draw ticks
        for (int speed = 0; speed <= maxSpeed; speed += 10) {
            float angle = startAngle + ((float) speed / maxSpeed) * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isMajor = (speed % 20 == 0);
            float innerRadius = isMajor ? radius * 0.78f : radius * 0.86f;
            float outerRadius = radius * 0.92f;

            float x1 = (float) (cx + innerRadius * Math.cos(rad));
            float y1 = (float) (cy + innerRadius * Math.sin(rad));
            float x2 = (float) (cx + outerRadius * Math.cos(rad));
            float y2 = (float) (cy + outerRadius * Math.sin(rad));

            canvas.drawLine(x1, y1, x2, y2, isMajor ? tickMajorPaint : tickMinorPaint);

            if (isMajor && (speed % 40 == 0 || speed == 0)) {
                float textRadius = radius * 0.65f;
                float tx = (float) (cx + textRadius * Math.cos(rad));
                float ty = (float) (cy + textRadius * Math.sin(rad) + 6f);
                canvas.drawText(String.valueOf(speed), tx, ty, textNumberPaint);
            }
        }

        // Draw sweeping needle
        float needleSpeed = Math.min(Math.max(speedKmh, 0), maxSpeed);
        float needleAngle = startAngle + (needleSpeed / maxSpeed) * sweepAngle;
        drawNeedle(canvas, cx, cy, radius * 0.82f, needleAngle);

        // Digital Speed in Center
        canvas.drawText(String.valueOf(speedKmh), cx, cy + radius * 0.38f, textValuePaint);
        canvas.drawText("KM/H", cx, cy + radius * 0.54f, textUnitPaint);
    }

    private void drawTachometer(Canvas canvas, float cx, float cy, float radius) {
        // Dial body & outer ring
        canvas.drawCircle(cx, cy, radius, dialBgPaint);
        canvas.drawCircle(cx, cy, radius, ringPaint);

        float startAngle = 135f;
        float sweepAngle = 270f;
        int maxRpm = 8000;

        // Redline arc (from 6000 to 8000 RPM)
        arcRect.set(cx - radius * 0.90f, cy - radius * 0.90f, cx + radius * 0.90f, cy + radius * 0.90f);
        float redlineStart = startAngle + (6000f / maxRpm) * sweepAngle;
        float redlineSweep = (2000f / maxRpm) * sweepAngle;
        canvas.drawArc(arcRect, redlineStart, redlineSweep, false, redlineArcPaint);

        // Draw ticks
        for (int r = 0; r <= maxRpm; r += 250) {
            float angle = startAngle + ((float) r / maxRpm) * sweepAngle;
            double rad = Math.toRadians(angle);
            boolean isMajor = (r % 1000 == 0);
            float innerRadius = isMajor ? radius * 0.78f : radius * 0.86f;
            float outerRadius = radius * 0.92f;

            float x1 = (float) (cx + innerRadius * Math.cos(rad));
            float y1 = (float) (cy + innerRadius * Math.sin(rad));
            float x2 = (float) (cx + outerRadius * Math.cos(rad));
            float y2 = (float) (cy + outerRadius * Math.sin(rad));

            canvas.drawLine(x1, y1, x2, y2, isMajor ? tickMajorPaint : tickMinorPaint);

            if (isMajor) {
                float textRadius = radius * 0.65f;
                float tx = (float) (cx + textRadius * Math.cos(rad));
                float ty = (float) (cy + textRadius * Math.sin(rad) + 6f);
                canvas.drawText(String.valueOf(r / 1000), tx, ty, textNumberPaint);
            }
        }

        // Draw needle
        float needleRpm = Math.min(Math.max(rpm, 0), maxRpm);
        float needleAngle = startAngle + (needleRpm / maxRpm) * sweepAngle;
        drawNeedle(canvas, cx, cy, radius * 0.82f, needleAngle);

        // RPM readout & label
        canvas.drawText(String.valueOf(rpm), cx, cy + radius * 0.38f, textValuePaint);
        canvas.drawText("RPM x1000", cx, cy + radius * 0.54f, textUnitPaint);
    }

    private void drawNeedle(Canvas canvas, float cx, float cy, float length, float angleDeg) {
        canvas.save();
        canvas.rotate(angleDeg, cx, cy);

        needlePath.reset();
        needlePath.moveTo(cx, cy - 5f);
        needlePath.lineTo(cx + length, cy);
        needlePath.lineTo(cx, cy + 5f);
        needlePath.close();

        canvas.drawPath(needlePath, needlePaint);
        canvas.drawCircle(cx, cy, 14f, needleCapPaint);
        canvas.drawCircle(cx, cy, 6f, dialBgPaint);

        canvas.restore();
    }

    private void drawCenterInfo(Canvas canvas, float cx, float cy, float radius) {
        float boxW = radius * 1.5f;
        float boxH = radius * 1.6f;
        RectF box = new RectF(cx - boxW / 2, cy - boxH / 2, cx + boxW / 2, cy + boxH / 2);
        canvas.drawRoundRect(box, 16f, 16f, centerBoxPaint);

        // Gear display (large P/R/N/D)
        Paint gearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gearPaint.setColor(Color.parseColor("#00E676"));
        gearPaint.setTextAlign(Paint.Align.CENTER);
        gearPaint.setTextSize(54f);
        gearPaint.setFakeBoldText(true);
        canvas.drawText(gear, cx, cy - 20f, gearPaint);

        // Mode label
        Paint modePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        modePaint.setColor(Color.parseColor("#B0BEC5"));
        modePaint.setTextAlign(Paint.Align.CENTER);
        modePaint.setTextSize(16f);
        canvas.drawText("STANDARD 4H", cx, cy + 25f, modePaint);

        // Coolant Temp
        Paint tempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempPaint.setColor(Color.parseColor("#90CAF9"));
        tempPaint.setTextAlign(Paint.Align.CENTER);
        tempPaint.setTextSize(14f);
        canvas.drawText("COOLANT: " + ((int) coolantTemp) + " °C", cx, cy + 65f, tempPaint);
    }

    private void drawHolePunchMask(Canvas canvas, int w, int h) {
        // Upper center window for factory turn signals, high-beam, and alert icons
        float maskW = w * 0.36f;
        float maskH = h * 0.16f;
        float left = (w - maskW) / 2f;
        float top = 0f;
        RectF aperture = new RectF(left, top, left + maskW, top + maskH);
        canvas.drawRoundRect(aperture, 18f, 18f, holePunchPaint);

        // Bottom status line cutout (factory total mileage / odometer)
        float bottomH = h * 0.10f;
        RectF bottomAperture = new RectF(w * 0.35f, h - bottomH, w * 0.65f, h);
        canvas.drawRect(bottomAperture, holePunchPaint);
    }
}
