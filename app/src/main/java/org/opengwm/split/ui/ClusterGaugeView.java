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
import android.graphics.Typeface;
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
    private String gear = "--";
    private float coolantTemp = -999f;
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
    private final RectF widgetCenterRect = new RectF();
    private final Paint widgetBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint widgetBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint widgetTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Interactive Layout Customization (Offsets as fraction of width/height and scales)
    public static class ElementLayout {
        public float dx = 0.0f;
        public float dy = 0.0f;
        public float scale = 1.0f;
        public float rotation = 0.0f;

        public ElementLayout() {}

        public ElementLayout(float dx, float dy, float scale, float rotation) {
            this.dx = dx;
            this.dy = dy;
            this.scale = scale;
            this.rotation = rotation;
        }

        public ElementLayout(float dx, float dy, float scale) {
            this(dx, dy, scale, 0.0f);
        }
    }

    private final ElementLayout[] elementLayouts = new ElementLayout[ClusterConfig.ELEM_COUNT];
    private boolean isEditMode = false;
    private int selectedElement = ClusterConfig.ELEM_LEFT_GAUGE;

    private float touchStartX = 0f;
    private float touchStartY = 0f;
    private float initialElemDx = 0f;
    private float initialElemDy = 0f;
    private boolean isDraggingElement = false;
    private android.view.ScaleGestureDetector scaleDetector;
    private float lastTwoFingerAngle = 0f;
    private boolean isTwoFingerGesture = false;

    public interface OnLayoutChangeListener {
        void onElementSelected(int elementId);
        void onElementLayoutChanged(int elementId, float dx, float dy, float scale, float rotation);
    }
    private OnLayoutChangeListener onLayoutChangeListener;

    // Effective transformed geometry computed per frame
    private float effLeftCx = 0f;
    private float effLeftCy = 0f;
    private float effLeftRadius = 0f;
    private float effRightCx = 0f;
    private float effRightCy = 0f;
    private float effRightRadius = 0f;
    private float effCenterCx = 0f;
    private float effCenterCy = 0f;
    private float effCenterScale = 1.0f;

    private final Paint editorSelectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint editorHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint editorPillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint editorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Matrix shaderMatrix = new android.graphics.Matrix();

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
    private final Paint miniFuelLedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Dynamic Media Player Detection & Metadata
    private boolean isMusicActive = false;
    private String trackTitle = "";
    private String trackArtist = "";
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
    private LinearGradient miniChromeBezelShader;

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

        for (int i = 0; i < ClusterConfig.ELEM_COUNT; i++) {
            elementLayouts[i] = new ElementLayout();
        }
        loadLayoutConfig();

        scaleDetector = new android.view.ScaleGestureDetector(getContext(), new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(android.view.ScaleGestureDetector detector) {
                if (!isEditMode) return false;
                float factor = detector.getScaleFactor();
                float newScale = Math.max(0.3f, Math.min(3.0f, elementLayouts[selectedElement].scale * factor));
                elementLayouts[selectedElement].scale = Math.round(newScale * 100f) / 100f;
                if (onLayoutChangeListener != null) {
                    onLayoutChangeListener.onElementLayoutChanged(
                            selectedElement,
                            elementLayouts[selectedElement].dx,
                            elementLayouts[selectedElement].dy,
                            elementLayouts[selectedElement].scale,
                            elementLayouts[selectedElement].rotation
                    );
                }
                postInvalidate();
                return true;
            }
        });

        setupPaints();
        initAudioMonitoring();
    }

    public void setOnLayoutChangeListener(OnLayoutChangeListener listener) {
        this.onLayoutChangeListener = listener;
    }

    public void setEditMode(boolean editMode, int elementToSelect) {
        this.isEditMode = editMode;
        if (elementToSelect >= 0 && elementToSelect < ClusterConfig.ELEM_COUNT) {
            this.selectedElement = elementToSelect;
        }
        postInvalidate();
    }

    public void setSelectedElement(int elementId) {
        if (elementId >= 0 && elementId < ClusterConfig.ELEM_COUNT) {
            this.selectedElement = elementId;
            postInvalidate();
        }
    }

    public int getSelectedElement() {
        return selectedElement;
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void loadLayoutConfig() {
        for (int i = 0; i < ClusterConfig.ELEM_COUNT; i++) {
            if (elementLayouts[i] == null) {
                elementLayouts[i] = new ElementLayout();
            }
            elementLayouts[i].dx = ClusterConfig.getElementDx(getContext(), i);
            elementLayouts[i].dy = ClusterConfig.getElementDy(getContext(), i);
            elementLayouts[i].scale = ClusterConfig.getElementScale(getContext(), i);
            elementLayouts[i].rotation = ClusterConfig.getElementRotation(getContext(), i);
        }
        postInvalidate();
    }

    public void setElementLayout(int elementId, float dx, float dy, float scale, float rot) {
        if (elementId >= 0 && elementId < ClusterConfig.ELEM_COUNT) {
            if (elementLayouts[elementId] == null) {
                elementLayouts[elementId] = new ElementLayout();
            }
            elementLayouts[elementId].dx = dx;
            elementLayouts[elementId].dy = dy;
            elementLayouts[elementId].scale = scale;
            elementLayouts[elementId].rotation = rot;
            postInvalidate();
        }
    }

    public void setElementLayout(int elementId, float dx, float dy, float scale) {
        float rot = (elementLayouts[elementId] != null) ? elementLayouts[elementId].rotation : 0f;
        setElementLayout(elementId, dx, dy, scale, rot);
    }

    public ElementLayout getElementLayout(int elementId) {
        if (elementId >= 0 && elementId < ClusterConfig.ELEM_COUNT && elementLayouts[elementId] != null) {
            return elementLayouts[elementId];
        }
        return new ElementLayout();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (!isEditMode) {
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

        if (scaleDetector != null) {
            scaleDetector.onTouchEvent(event);
        }

        // Disallow parent ScrollView from stealing touch events while in editor
        int actionMasked = event.getActionMasked();
        if (actionMasked == android.view.MotionEvent.ACTION_DOWN ||
            actionMasked == android.view.MotionEvent.ACTION_POINTER_DOWN ||
            actionMasked == android.view.MotionEvent.ACTION_MOVE) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }

        // Multi-touch gestures (Pinch-to-zoom + 2-finger rotation)
        int pointerCount = event.getPointerCount();
        if (pointerCount >= 2) {
            isDraggingElement = false;
            float p0x = event.getX(0);
            float p0y = event.getY(0);
            float p1x = event.getX(1);
            float p1y = event.getY(1);
            float currentAngle = (float) Math.toDegrees(Math.atan2(p1y - p0y, p1x - p0x));

            if (!isTwoFingerGesture || actionMasked == android.view.MotionEvent.ACTION_POINTER_DOWN) {
                lastTwoFingerAngle = currentAngle;
                isTwoFingerGesture = true;
            } else if (actionMasked == android.view.MotionEvent.ACTION_MOVE) {
                float deltaAngle = currentAngle - lastTwoFingerAngle;
                if (deltaAngle < -180f) deltaAngle += 360f;
                if (deltaAngle > 180f) deltaAngle -= 360f;

                if (Math.abs(deltaAngle) > 0.4f) {
                    float newRot = elementLayouts[selectedElement].rotation + deltaAngle;
                    while (newRot > 180f) newRot -= 360f;
                    while (newRot < -180f) newRot += 360f;
                    elementLayouts[selectedElement].rotation = Math.round(newRot * 10f) / 10f;
                    lastTwoFingerAngle = currentAngle;
                    if (onLayoutChangeListener != null) {
                        onLayoutChangeListener.onElementLayoutChanged(
                                selectedElement,
                                elementLayouts[selectedElement].dx,
                                elementLayouts[selectedElement].dy,
                                elementLayouts[selectedElement].scale,
                                elementLayouts[selectedElement].rotation
                        );
                    }
                    postInvalidate();
                }
            }
            return true;
        } else {
            isTwoFingerGesture = false;
        }

        float x = event.getX();
        float y = event.getY();

        switch (actionMasked) {
            case android.view.MotionEvent.ACTION_DOWN:
                int hit = -1;
                int[] hitOrder = {
                        ClusterConfig.ELEM_MEDIA_PLAYER,
                        ClusterConfig.ELEM_TOP_HEADER,
                        ClusterConfig.ELEM_BOTTOM_FOOTER,
                        ClusterConfig.ELEM_WIDGET_LEFT,
                        ClusterConfig.ELEM_WIDGET_RIGHT,
                        ClusterConfig.ELEM_WIDGET_CENTER,
                        ClusterConfig.ELEM_CENTER_BLOCK,
                        ClusterConfig.ELEM_LEFT_GAUGE,
                        ClusterConfig.ELEM_RIGHT_GAUGE
                };
                for (int elemId : hitOrder) {
                    RectF bounds = getElementBounds(elemId, viewWidth, viewHeight);
                    RectF target = new RectF(bounds);
                    target.inset(-28f, -28f);
                    if (target.contains(x, y)) {
                        hit = elemId;
                        break;
                    }
                }

                if (hit != -1) {
                    selectedElement = hit;
                    if (onLayoutChangeListener != null) {
                        onLayoutChangeListener.onElementSelected(selectedElement);
                    }
                }

                touchStartX = x;
                touchStartY = y;
                if (elementLayouts[selectedElement] == null) {
                    elementLayouts[selectedElement] = new ElementLayout();
                }
                initialElemDx = elementLayouts[selectedElement].dx;
                initialElemDy = elementLayouts[selectedElement].dy;
                isDraggingElement = true;
                postInvalidate();
                return true;

            case android.view.MotionEvent.ACTION_MOVE:
                if (isDraggingElement) {
                    float deltaX = (x - touchStartX) / (float) Math.max(1, viewWidth);
                    float deltaY = (y - touchStartY) / (float) Math.max(1, viewHeight);

                    float newDx = Math.max(-0.48f, Math.min(0.48f, initialElemDx + deltaX));
                    float newDy = Math.max(-0.48f, Math.min(0.48f, initialElemDy + deltaY));

                    elementLayouts[selectedElement].dx = newDx;
                    elementLayouts[selectedElement].dy = newDy;

                    if (onLayoutChangeListener != null) {
                        onLayoutChangeListener.onElementLayoutChanged(
                                selectedElement,
                                newDx,
                                newDy,
                                elementLayouts[selectedElement].scale,
                                elementLayouts[selectedElement].rotation
                        );
                    }
                    postInvalidate();
                    return true;
                }
                break;

            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                isDraggingElement = false;
                isTwoFingerGesture = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                postInvalidate();
                return true;
        }
        return super.onTouchEvent(event);
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
            if (now - lastMusicCheckTime > 300) {
                lastMusicCheckTime = now;
                try {
                    isMusicActive = audioManager.isMusicActive();
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

        editorSelectionPaint.setStyle(Paint.Style.STROKE);
        editorSelectionPaint.setStrokeWidth(3f);
        editorSelectionPaint.setColor(Color.parseColor("#00E5FF"));
        editorSelectionPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{14f, 8f}, 0));

        editorHandlePaint.setStyle(Paint.Style.FILL);
        editorHandlePaint.setColor(Color.parseColor("#00E5FF"));

        editorPillPaint.setStyle(Paint.Style.FILL);
        editorPillPaint.setColor(Color.parseColor("#E60A0E14"));

        editorTextPaint.setStyle(Paint.Style.FILL);
        editorTextPaint.setColor(Color.parseColor("#00E5FF"));
        editorTextPaint.setTextAlign(Paint.Align.CENTER);
        editorTextPaint.setFakeBoldText(true);
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

        // MINI Cooper Photorealistic Chrome Bezel
        miniChromeBezelShader = new LinearGradient(
                leftCenterX - gaugeRadius, centerY - gaugeRadius,
                leftCenterX + gaugeRadius, centerY + gaugeRadius,
                new int[]{
                        Color.parseColor("#FFFFFF"),
                        Color.parseColor("#D4DFEC"),
                        Color.parseColor("#7B8B9E"),
                        Color.parseColor("#262D36"),
                        Color.parseColor("#4F5D6D"),
                        Color.parseColor("#F0F5FA"),
                        Color.parseColor("#9AA8B8")
                },
                new float[]{0.0f, 0.15f, 0.38f, 0.62f, 0.78f, 0.92f, 1.0f},
                Shader.TileMode.CLAMP
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = viewWidth;
        int h = viewHeight;
        if (w <= 0 || h <= 0) return;

        if (elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE] == null) {
            loadLayoutConfig();
        }

        if (activeTheme == ClusterConfig.THEME_MINI_COOPER) {
            float miniLeftBaseCx = w * 0.38f;
            float miniRightBaseCx = w * 0.58f;
            effLeftCx = miniLeftBaseCx + w * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].dx;
            effLeftCy = centerY + h * 0.03f + h * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].dy;
            effLeftRadius = (gaugeRadius * 0.95f) * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].scale;

            effRightCx = miniRightBaseCx + w * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].dx;
            effRightCy = centerY - h * 0.01f + h * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].dy;
            effRightRadius = (gaugeRadius * 1.08f) * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].scale;
        } else {
            effLeftCx = leftCenterX + w * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].dx;
            effLeftCy = centerY + h * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].dy;
            effLeftRadius = gaugeRadius * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].scale;

            effRightCx = rightCenterX + w * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].dx;
            effRightCy = centerY + h * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].dy;
            effRightRadius = gaugeRadius * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].scale;
        }

        effCenterCx = w * 0.5f + w * elementLayouts[ClusterConfig.ELEM_CENTER_BLOCK].dx;
        effCenterCy = centerY + h * elementLayouts[ClusterConfig.ELEM_CENTER_BLOCK].dy;
        effCenterScale = elementLayouts[ClusterConfig.ELEM_CENTER_BLOCK].scale;

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
                case ClusterConfig.THEME_MINI_COOPER:
                    drawMiniCooperTheme(canvas, w, h);
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

        // Draw Configurable Telemetry Mini-Widgets (only on themes that need extra telemetry)
        drawMiniWidgets(canvas);

        if (isEditMode) {
            drawEditorOverlay(canvas, w, h);
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
        applyHolePunchIfNeeded(canvas, w, h);

        // Dials
        draw3DTubeDial(canvas, effLeftCx, effLeftCy, effLeftRadius, sportBezelShader, Color.parseColor("#FF1744"));
        draw3DTubeDial(canvas, effRightCx, effRightCy, effRightRadius, sportBezelShader, Color.parseColor("#FF1744"));

        // Speedometer Left
        drawSpeedometerScale(canvas, effLeftCx, effLeftCy, effLeftRadius, 240, Color.WHITE, Color.parseColor("#FF1744"), true);

        // Tachometer Right
        drawTachometerScale(canvas, effRightCx, effRightCy, effRightRadius, 8000, 5500, Color.WHITE, Color.parseColor("#FF1744"), true);

        // Needles
        drawSportNeedle(canvas, effLeftCx, effLeftCy, effLeftRadius * 0.82f, getSpeedAngle(displaySpeed, 240) + elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].rotation, Color.parseColor("#FF1744"));
        drawSportNeedle(canvas, effRightCx, effRightCy, effRightRadius * 0.82f, getRpmAngle(displayRpm, 8000) + elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].rotation, Color.parseColor("#FF1744"));

        // Center Box: Carbon card with F1 shift light bar
        drawSportCenterCard(canvas, effCenterCx, effCenterCy, gaugeRadius * effCenterScale);
    }

    private void draw3DTubeDial(Canvas canvas, float cx, float cy, float radius, Shader bezelShader, int accentColor) {
        // 1. Recessed dial plate with inner shadow
        if (dialPlateShader != null) {
            shaderMatrix.reset();
            shaderMatrix.postTranslate(cx - leftCenterX, cy - centerY);
            if (gaugeRadius > 0) {
                float s = radius / gaugeRadius;
                shaderMatrix.postScale(s, s, cx, cy);
            }
            dialPlateShader.setLocalMatrix(shaderMatrix);
            dialPlatePaint.setShader(dialPlateShader);
            canvas.drawCircle(cx, cy, radius, dialPlatePaint);
            dialPlatePaint.setShader(null);
        }

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
        if (bezelShader != null) {
            shaderMatrix.reset();
            shaderMatrix.postTranslate(cx - leftCenterX, cy - centerY);
            if (gaugeRadius > 0) {
                float s = radius / gaugeRadius;
                shaderMatrix.postScale(s, s, cx, cy);
            }
            bezelShader.setLocalMatrix(shaderMatrix);
            tubeBezelPaint.setShader(bezelShader);
            tubeBezelPaint.setStrokeWidth(radius * 0.07f);
            canvas.drawCircle(cx, cy, radius, tubeBezelPaint);
            tubeBezelPaint.setShader(null);
        }

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
        applyHolePunchIfNeeded(canvas, w, h);

        // 3D Brushed Gold Bezel
        draw3DTubeDial(canvas, effLeftCx, effLeftCy, effLeftRadius, luxuryBezelShader, Color.parseColor("#C5A059"));
        draw3DTubeDial(canvas, effRightCx, effRightCy, effRightRadius, luxuryBezelShader, Color.parseColor("#C5A059"));

        // Guilloche concentric fine texture
        dialTexturePaint.setColor(Color.parseColor("#22C5A059"));
        dialTexturePaint.setStrokeWidth(1f);
        for (float r = effLeftRadius * 0.35f; r < effLeftRadius * 0.85f; r += 12f) {
            canvas.drawCircle(effLeftCx, effLeftCy, r, dialTexturePaint);
        }
        for (float r = effRightRadius * 0.35f; r < effRightRadius * 0.85f; r += 12f) {
            canvas.drawCircle(effRightCx, effRightCy, r, dialTexturePaint);
        }

        // Sub-dial inside Speedometer (Mini Chrono)
        drawChronoSubDial(canvas, effLeftCx, effLeftCy - effLeftRadius * 0.28f, effLeftRadius * 0.28f, "OIL");

        // Sub-dial inside Tachometer (Mini Voltage/Temp)
        drawChronoSubDial(canvas, effRightCx, effRightCy - effRightRadius * 0.28f, effRightRadius * 0.28f, "BAT");

        // Scales
        drawSpeedometerScale(canvas, effLeftCx, effLeftCy, effLeftRadius, 240, Color.parseColor("#F5E6C8"), Color.parseColor("#C5A059"), false);
        drawTachometerScale(canvas, effRightCx, effRightCy, effRightRadius, 8000, 6000, Color.parseColor("#F5E6C8"), Color.parseColor("#C5A059"), false);

        // Classic Breguet Needles (Gold sword with circular counterweight)
        drawLuxuryNeedle(canvas, effLeftCx, effLeftCy, effLeftRadius * 0.80f, getSpeedAngle(displaySpeed, 240) + elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].rotation);
        drawLuxuryNeedle(canvas, effRightCx, effRightCy, effRightRadius * 0.80f, getRpmAngle(displayRpm, 8000) + elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].rotation);

        // Center Luxury Crest & Gear
        drawLuxuryCenterCard(canvas, effCenterCx, effCenterCy, gaugeRadius * effCenterScale);
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
        applyHolePunchIfNeeded(canvas, w, h);

        // Cyberpunk perspective horizon floor in background
        drawCyberFloorGrid(canvas, w, h);

        // Hexagonal / Angular Chamfered Dials
        drawHexGauge(canvas, effLeftCx, effLeftCy, effLeftRadius, Color.parseColor("#00E5FF"));
        drawHexGauge(canvas, effRightCx, effRightCy, effRightRadius, Color.parseColor("#FF007F"));

        // Segmented LED Bar Scales
        drawCyberScale(canvas, effLeftCx, effLeftCy, effLeftRadius, displaySpeed, 240, Color.parseColor("#00E5FF"), "KM/H");
        drawCyberScale(canvas, effRightCx, effRightCy, effRightRadius, displayRpm, 8000, Color.parseColor("#FF007F"), "RPM");

        // Center Holographic HUD Card
        drawCyberCenterCard(canvas, effCenterCx, effCenterCy, gaugeRadius * effCenterScale);
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
        applyHolePunchIfNeeded(canvas, w, h);

        // Bolted Industrial Steel Bezels
        draw3DTubeDial(canvas, effLeftCx, effLeftCy, effLeftRadius, tacticalBezelShader, Color.parseColor("#FF8F00"));
        draw3DTubeDial(canvas, effRightCx, effRightCy, effRightRadius, tacticalBezelShader, Color.parseColor("#FF8F00"));

        // 12 Industrial Allen Bolts around each bezel
        drawBezelBolts(canvas, effLeftCx, effLeftCy, effLeftRadius * 1.05f);
        drawBezelBolts(canvas, effRightCx, effRightCy, effRightRadius * 1.05f);

        // Military Stencil Scales
        drawSpeedometerScale(canvas, effLeftCx, effLeftCy, effLeftRadius, 240, Color.parseColor("#FFB300"), Color.parseColor("#FF6D00"), true);
        drawTachometerScale(canvas, effRightCx, effRightCy, effRightRadius, 8000, 5000, Color.parseColor("#FFB300"), Color.parseColor("#FF3D00"), true);

        // Aviation Orange Needles
        drawSportNeedle(canvas, effLeftCx, effLeftCy, effLeftRadius * 0.82f, getSpeedAngle(displaySpeed, 240) + elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].rotation, Color.parseColor("#FF6D00"));
        drawSportNeedle(canvas, effRightCx, effRightCy, effRightRadius * 0.82f, getRpmAngle(displayRpm, 8000) + elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].rotation, Color.parseColor("#FF3D00"));

        // Center Console: 3D Off-Road Inclinometer / Artificial Horizon
        drawTacticalInclinometerCard(canvas, effCenterCx, effCenterCy, gaugeRadius * effCenterScale);
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
        applyHolePunchIfNeeded(canvas, w, h);

        // Floating arc ribbons (no heavy outer rings, pure luminous arcs)
        drawMinimalFloatingArc(canvas, effLeftCx, effLeftCy, effLeftRadius, displaySpeed, 240, Color.parseColor("#4D88FF"), "km/h");
        drawMinimalFloatingArc(canvas, effRightCx, effRightCy, effRightRadius, displayRpm, 8000, Color.parseColor("#E0E0E0"), "rpm");

        // Clean Center Typography (only when HolePunch is disabled)
        if (!isHolePunchEnabled) {
            gearPaint.setColor(Color.WHITE);
            gearPaint.setTextSize(64f * effCenterScale);
            canvas.drawText(gear, effCenterCx, effCenterCy + 4f, gearPaint);

            modePaint.setColor(Color.parseColor("#80FFFFFF"));
            modePaint.setTextSize(spToPx(13) * effCenterScale);
            canvas.drawText(driveMode, effCenterCx, effCenterCy + 38f * effCenterScale, modePaint);

            subTextPaint.setColor(Color.parseColor("#40FFFFFF"));
            subTextPaint.setTextSize(spToPx(11) * effCenterScale);
            canvas.drawText(Math.round(coolantTemp) + " °C", effCenterCx, effCenterCy + 62f * effCenterScale, subTextPaint);
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

    private void applyHolePunchIfNeeded(Canvas canvas, int w, int h) {
        if (isHolePunchEnabled && activeTheme != ClusterConfig.THEME_TAYCAN_ARC && activeTheme != ClusterConfig.THEME_MINI_COOPER) {
            drawHolePunchMask(canvas, w, h);
        }
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
        canvas.save();
        canvas.translate(effCenterCx - w * 0.5f, effCenterCy - centerY);
        canvas.scale(effCenterScale, effCenterScale, effCenterCx, effCenterCy);
        drawCenterNavPlaceholder(canvas, w, h);
        canvas.restore();

        // 4. Speedometer Pod Left
        drawTaycanSpeedoArc(canvas, effLeftCx, effLeftCy, effLeftRadius * 0.94f, w, h);

        // 5. Tachometer / Power Pod Right
        drawTaycanTachoArc(canvas, effRightCx, effRightCy, effRightRadius * 0.94f, w, h);

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
        ElementLayout layout = elementLayouts[ClusterConfig.ELEM_TOP_HEADER];
        canvas.save();
        canvas.translate(w * layout.dx, h * layout.dy);
        canvas.scale(layout.scale, layout.scale, w * 0.5f, h * 0.068f);
        canvas.rotate(layout.rotation, w * 0.5f, h * 0.068f);

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

        canvas.restore();
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
        if (!checkIsMusicPlaying() && !isEditMode) {
            return;
        }

        ElementLayout layout = elementLayouts[ClusterConfig.ELEM_MEDIA_PLAYER];
        float baseCardW = Math.min(360f, w * 0.28f);
        float baseCardH = 54f;
        float baseCardX = w * 0.5f;
        float baseCardY = h * 0.82f;

        float curCardX = baseCardX + w * layout.dx;
        float curCardY = baseCardY + h * layout.dy;
        float curCardW = baseCardW * layout.scale;
        float curCardH = baseCardH * layout.scale;
        mediaCardRect.set(curCardX - curCardW / 2f, curCardY - curCardH / 2f, curCardX + curCardW / 2f, curCardY + curCardH / 2f);

        canvas.save();
        canvas.translate(w * layout.dx, h * layout.dy);
        canvas.scale(layout.scale, layout.scale, baseCardX, baseCardY);
        canvas.rotate(layout.rotation, baseCardX, baseCardY);

        RectF localCardRect = new RectF(baseCardX - baseCardW / 2f, baseCardY - baseCardH / 2f, baseCardX + baseCardW / 2f, baseCardY + baseCardH / 2f);

        statusPillBgPaint.setColor(Color.parseColor("#E60D1520"));
        canvas.drawRoundRect(localCardRect, 14f, 14f, statusPillBgPaint);
        statusPillBorderPaint.setColor(Color.parseColor("#00E5FF"));
        statusPillBorderPaint.setStrokeWidth(1.2f);
        canvas.drawRoundRect(localCardRect, 14f, 14f, statusPillBorderPaint);

        // Album Art thumbnail (dark cyan container)
        RectF artRect = new RectF(localCardRect.left + 8f, baseCardY - 19f, localCardRect.left + 46f, baseCardY + 19f);
        statusPillBgPaint.setColor(Color.parseColor("#152D42"));
        canvas.drawRoundRect(artRect, 8f, 8f, statusPillBgPaint);

        subTextPaint.setColor(Color.parseColor("#00E5FF"));
        subTextPaint.setTextSize(spToPx(13));
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("♬", artRect.centerX(), artRect.centerY() + 5f, subTextPaint);

        // Track title & artist
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(11.5f));
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        String title = (trackTitle != null && !trackTitle.isEmpty()) ? trackTitle : "Аудиопоток";
        String artist = (trackArtist != null && !trackArtist.isEmpty()) ? trackArtist : "Воспроизведение";
        canvas.drawText(title, localCardRect.left + 54f, baseCardY - 4f, subTextPaint);

        subTextPaint.setColor(Color.parseColor("#78909C"));
        subTextPaint.setTextSize(spToPx(9.5f));
        canvas.drawText(artist, localCardRect.left + 54f, baseCardY + 14f, subTextPaint);
        subTextPaint.setTextAlign(Paint.Align.CENTER);

        // Play/Pause & Next icons
        subTextPaint.setColor(Color.parseColor("#00E5FF"));
        subTextPaint.setTextSize(spToPx(13));
        canvas.drawText("❚❚", localCardRect.right - 44f, baseCardY + 5f, subTextPaint);
        canvas.drawText("⏭", localCardRect.right - 18f, baseCardY + 5f, subTextPaint);

        canvas.restore();
    }

    // =========================================================================
    // THEME: MINI COOPER F56 (Overlapping Asymmetric Chrome Twin Pod, LCD, Neon)
    // =========================================================================
    private void drawMiniCooperTheme(Canvas canvas, int w, int h) {
        canvas.drawColor(Color.parseColor("#08090D"));
        applyHolePunchIfNeeded(canvas, w, h);

        Typeface origNumberTf = textNumberPaint.getTypeface();
        Typeface origSubTf = subTextPaint.getTypeface();
        Typeface origValueTf = textValuePaint.getTypeface();
        Typeface origGearTf = gearPaint.getTypeface();
        Typeface miniBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);

        try {
            // 1. Ultrawide Cockpit Wings (Side Telemetry & Infotainment)
            drawMiniSideCards(canvas, w, h);

            // 2. LEFT POD: Tachometer (0 - 8000 RPM, OFF, READY) - Physically Underneath
            canvas.save();
            canvas.rotate(elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].rotation, effLeftCx, effLeftCy);

            // Volumetric Chrome Bezel
            if (miniChromeBezelShader != null) {
                shaderMatrix.reset();
                shaderMatrix.postTranslate(effLeftCx - leftCenterX, effLeftCy - centerY);
                if (gaugeRadius > 0) {
                    float s = effLeftRadius / gaugeRadius;
                    shaderMatrix.postScale(s, s, effLeftCx, effLeftCy);
                }
                miniChromeBezelShader.setLocalMatrix(shaderMatrix);
                tubeBezelPaint.setShader(miniChromeBezelShader);
                tubeBezelPaint.setStrokeWidth(effLeftRadius * 0.08f);
                canvas.drawCircle(effLeftCx, effLeftCy, effLeftRadius, tubeBezelPaint);
                tubeBezelPaint.setShader(null);
            }

            // Inner dark stepped chamfer
            tubeInnerShadowPaint.setColor(Color.parseColor("#151820"));
            tubeInnerShadowPaint.setStrokeWidth(effLeftRadius * 0.05f);
            canvas.drawCircle(effLeftCx, effLeftCy, effLeftRadius * 0.94f, tubeInnerShadowPaint);

            // Dial Plate Face (Dark matte charcoal)
            dialPlatePaint.setColor(Color.parseColor("#1E2128"));
            canvas.drawCircle(effLeftCx, effLeftCy, effLeftRadius * 0.91f, dialPlatePaint);

            // Redline Arc (6500 to 8000 RPM, 290° to 335°)
            arcRect.set(effLeftCx - effLeftRadius * 0.85f, effLeftCy - effLeftRadius * 0.85f,
                    effLeftCx + effLeftRadius * 0.85f, effLeftCy + effLeftRadius * 0.85f);
            redlineArcPaint.setColor(Color.parseColor("#FF3D00"));
            redlineArcPaint.setStrokeWidth(effLeftRadius * 0.055f);
            canvas.drawArc(arcRect, 290f, 45f, false, redlineArcPaint);

            // Tachometer Ticks & Numerals
            float tachoNumSize = effLeftRadius * 0.165f;
            textNumberPaint.setTypeface(miniBold);
            textNumberPaint.setTextSize(tachoNumSize);

            for (int r = 0; r <= 8000; r += 250) {
                float angle = 95f + ((float) r / 8000f) * 240f;
                double rad = Math.toRadians(angle);
                boolean isMajor = (r % 1000 == 0);
                boolean isMid = (r % 500 == 0);
                boolean isRedline = (r >= 6500);

                float innerR = isMajor ? effLeftRadius * 0.81f : (isMid ? effLeftRadius * 0.84f : effLeftRadius * 0.865f);
                float outerR = effLeftRadius * 0.90f;

                float x1 = (float) (effLeftCx + innerR * Math.cos(rad));
                float y1 = (float) (effLeftCy + innerR * Math.sin(rad));
                float x2 = (float) (effLeftCx + outerR * Math.cos(rad));
                float y2 = (float) (effLeftCy + outerR * Math.sin(rad));

                tickMajorPaint.setColor(isRedline ? Color.parseColor("#FF3D00") : (isMajor ? Color.WHITE : Color.parseColor("#8E9DAE")));
                tickMajorPaint.setStrokeWidth(isMajor ? 4.0f : (isMid ? 2.4f : 1.5f));
                canvas.drawLine(x1, y1, x2, y2, tickMajorPaint);

                if (isMajor && r > 0) {
                    float textR = effLeftRadius * 0.69f;
                    float tx = (float) (effLeftCx + textR * Math.cos(rad));
                    float ty = (float) (effLeftCy + textR * Math.sin(rad) + tachoNumSize * 0.35f);
                    textNumberPaint.setColor(isRedline ? Color.parseColor("#FF3D00") : Color.WHITE);
                    textNumberPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(String.valueOf(r / 1000), tx, ty, textNumberPaint);
                }
            }

            // Labels: OFF, READY, rpm x 1000
            double offRad = Math.toRadians(62.0);
            float offX = (float) (effLeftCx + effLeftRadius * 0.72f * Math.cos(offRad));
            float offY = (float) (effLeftCy + effLeftRadius * 0.72f * Math.sin(offRad) + 5f);
            subTextPaint.setTypeface(miniBold);
            subTextPaint.setColor(Color.WHITE);
            subTextPaint.setTextSize(effLeftRadius * 0.095f);
            subTextPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("OFF", offX, offY, subTextPaint);

            // Orange tick at OFF
            float offTickIn = effLeftRadius * 0.80f;
            float offTickOut = effLeftRadius * 0.90f;
            tickMajorPaint.setColor(Color.parseColor("#FF6200"));
            tickMajorPaint.setStrokeWidth(4.0f);
            canvas.drawLine((float) (effLeftCx + offTickIn * Math.cos(offRad)), (float) (effLeftCy + offTickIn * Math.sin(offRad)),
                    (float) (effLeftCx + offTickOut * Math.cos(offRad)), (float) (effLeftCy + offTickOut * Math.sin(offRad)), tickMajorPaint);

            double readyRad = Math.toRadians(78.0);
            float readyX = (float) (effLeftCx + effLeftRadius * 0.65f * Math.cos(readyRad));
            float readyY = (float) (effLeftCy + effLeftRadius * 0.65f * Math.sin(readyRad) + 4f);
            subTextPaint.setTypeface(miniBold);
            subTextPaint.setTextSize(effLeftRadius * 0.11f);
            canvas.drawText("READY", readyX, readyY, subTextPaint);

            subTextPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            subTextPaint.setTextSize(effLeftRadius * 0.075f);
            subTextPaint.setColor(Color.parseColor("#8E9DAE"));
            canvas.drawText("rpm x 1000", readyX, readyY + effLeftRadius * 0.10f, subTextPaint);

            // Tachometer Needle
            float rpmAngleDeg = (displayRpm <= 50f) ? 62f : (95f + (Math.min(displayRpm, 8000f) / 8000f) * 240f);
            drawMiniNeedle(canvas, effLeftCx, effLeftCy, effLeftRadius * 0.78f, rpmAngleDeg);
            canvas.restore();

            // 3. Drop Shadow cast from Speedometer onto Tachometer
            tubeInnerShadowPaint.setColor(Color.parseColor("#CC000000"));
            tubeInnerShadowPaint.setStrokeWidth(effRightRadius * 0.14f);
            canvas.drawCircle(effRightCx, effRightCy, effRightRadius * 1.05f, tubeInnerShadowPaint);

            // 4. RIGHT POD: Speedometer (0 - 260 km/h) - Physically in Front
            canvas.save();
            canvas.rotate(elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].rotation, effRightCx, effRightCy);

            // Volumetric Chrome Bezel
            if (miniChromeBezelShader != null) {
                shaderMatrix.reset();
                shaderMatrix.postTranslate(effRightCx - leftCenterX, effRightCy - centerY);
                if (gaugeRadius > 0) {
                    float s = effRightRadius / gaugeRadius;
                    shaderMatrix.postScale(s, s, effRightCx, effRightCy);
                }
                miniChromeBezelShader.setLocalMatrix(shaderMatrix);
                tubeBezelPaint.setShader(miniChromeBezelShader);
                tubeBezelPaint.setStrokeWidth(effRightRadius * 0.09f);
                canvas.drawCircle(effRightCx, effRightCy, effRightRadius, tubeBezelPaint);
                tubeBezelPaint.setShader(null);
            }

            // Inner dark chamfer
            tubeInnerShadowPaint.setColor(Color.parseColor("#151820"));
            tubeInnerShadowPaint.setStrokeWidth(effRightRadius * 0.05f);
            canvas.drawCircle(effRightCx, effRightCy, effRightRadius * 0.94f, tubeInnerShadowPaint);

            // Dial Plate Face
            dialPlatePaint.setColor(Color.parseColor("#1C1F26"));
            canvas.drawCircle(effRightCx, effRightCy, effRightRadius * 0.91f, dialPlatePaint);

            // Speedometer Ticks & Numerals
            float speedNumSize = effRightRadius * 0.145f;
            textNumberPaint.setTypeface(miniBold);
            textNumberPaint.setTextSize(speedNumSize);

            for (int s = 0; s <= 260; s += 5) {
                float angle = 176f + ((float) s / 260f) * 188f;
                double rad = Math.toRadians(angle);
                boolean isMajor = (s % 20 == 0);
                boolean isMid = (s % 10 == 0);
                boolean isSpecialLimit = (s == 30 || s == 50);

                float innerR = (isMajor || isSpecialLimit) ? effRightRadius * 0.81f : (isMid ? effRightRadius * 0.84f : effRightRadius * 0.865f);
                float outerR = effRightRadius * 0.90f;

                float x1 = (float) (effRightCx + innerR * Math.cos(rad));
                float y1 = (float) (effRightCy + innerR * Math.sin(rad));
                float x2 = (float) (effRightCx + outerR * Math.cos(rad));
                float y2 = (float) (effRightCy + outerR * Math.sin(rad));

                int tickColor;
                if (isSpecialLimit) {
                    tickColor = Color.parseColor("#FF1744");
                } else if (isMajor) {
                    tickColor = Color.WHITE;
                } else if (isMid) {
                    tickColor = Color.parseColor("#CCD6E0");
                } else {
                    tickColor = Color.parseColor("#788796");
                }

                tickMajorPaint.setColor(tickColor);
                tickMajorPaint.setStrokeWidth(isSpecialLimit ? 4.0f : (isMajor ? 4.0f : (isMid ? 2.4f : 1.5f)));
                canvas.drawLine(x1, y1, x2, y2, tickMajorPaint);

                if (isMajor) {
                    float textR = effRightRadius * 0.70f;
                    float tx = (float) (effRightCx + textR * Math.cos(rad));
                    float ty = (float) (effRightCy + textR * Math.sin(rad) + speedNumSize * 0.35f);
                    textNumberPaint.setColor(Color.WHITE);
                    textNumberPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(String.valueOf(s), tx, ty, textNumberPaint);
                }
            }

            // Vertical "km/h" label next to 0/20 ticks
            float kmhX = effRightCx - effRightRadius * 0.88f;
            float kmhY = effRightCy - effRightRadius * 0.08f;
            subTextPaint.setTypeface(miniBold);
            subTextPaint.setColor(Color.WHITE);
            subTextPaint.setTextSize(effRightRadius * 0.072f);
            subTextPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("k", kmhX, kmhY, subTextPaint);
            canvas.drawText("m", kmhX, kmhY + effRightRadius * 0.085f, subTextPaint);
            canvas.drawText("/", kmhX, kmhY + effRightRadius * 0.170f, subTextPaint);
            canvas.drawText("h", kmhX, kmhY + effRightRadius * 0.255f, subTextPaint);

            // Upper Telltales (ADAS car, turn signals)
            drawMiniUpperTelltales(canvas, effRightCx, effRightCy, effRightRadius);

            // Trip Reset Pin Stalk (0.0) at 325°
            double stalkRad = Math.toRadians(325.0);
            float stalkX = (float) (effRightCx + effRightRadius * 0.74f * Math.cos(stalkRad));
            float stalkY = (float) (effRightCy + effRightRadius * 0.74f * Math.sin(stalkRad));
            canvas.drawCircle(stalkX + 3f, stalkY + 3f, 7f, needleShadowPaint);
            boltPaint.setColor(Color.parseColor("#181A20"));
            canvas.drawCircle(stalkX, stalkY, 7f, boltPaint);
            boltPaint.setColor(Color.parseColor("#383E4B"));
            canvas.drawCircle(stalkX - 1.5f, stalkY - 1.5f, 3f, boltPaint);
            subTextPaint.setTypeface(miniBold);
            subTextPaint.setColor(Color.WHITE);
            subTextPaint.setTextSize(spToPx(9.5f));
            subTextPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("0.0", stalkX, stalkY + 19f, subTextPaint);

            // Integrated Lower Multifunction LCD Screen
            drawMiniLcdScreen(canvas, effRightCx, effRightCy, effRightRadius);

            // Speedometer Needle
            float speedAngleDeg = 176f + (Math.min(Math.max(displaySpeed, 0f), 260f) / 260f) * 188f;
            drawMiniNeedle(canvas, effRightCx, effRightCy, effRightRadius * 0.78f, speedAngleDeg);
            canvas.restore();

            // 5. Outer Right Flank Curved Fuel Gauge with LED strip
            drawMiniFuelGauge(canvas, effRightCx, effRightCy, effRightRadius);
        } finally {
            textNumberPaint.setTypeface(origNumberTf);
            subTextPaint.setTypeface(origSubTf);
            textValuePaint.setTypeface(origValueTf);
            gearPaint.setTypeface(origGearTf);
        }
    }

    private void drawMiniNeedle(Canvas canvas, float cx, float cy, float length, float angleDeg) {
        canvas.save();
        canvas.rotate(angleDeg, cx, cy);

        // Soft Drop Shadow
        needleShadowPath.reset();
        needleShadowPath.moveTo(cx - 16f, cy + 3f);
        needleShadowPath.lineTo(cx + length, cy + 6f);
        needleShadowPath.lineTo(cx - 16f, cy + 9f);
        needleShadowPath.close();
        canvas.drawPath(needleShadowPath, needleShadowPaint);

        // Fluorescent Neon Orange Blade
        needlePath.reset();
        needlePath.moveTo(cx - 18f, cy - 3.2f);
        needlePath.lineTo(cx + length, cy);
        needlePath.lineTo(cx - 18f, cy + 3.2f);
        needlePath.close();

        needlePaint.setColor(Color.parseColor("#FF6200"));
        canvas.drawPath(needlePath, needlePaint);

        // Slender luminous core line
        needleGlowPaint.setColor(Color.parseColor("#FFA040"));
        needleGlowPaint.setStrokeWidth(1.6f);
        canvas.drawLine(cx + 10f, cy, cx + length * 0.88f, cy, needleGlowPaint);

        // 3D Dark Charcoal Cap
        needleCapPaint.setColor(Color.parseColor("#1C1E24"));
        canvas.drawCircle(cx, cy, 22f, needleCapPaint);
        needleCapPaint.setColor(Color.parseColor("#2B2E37"));
        canvas.drawCircle(cx, cy, 19f, needleCapPaint);

        // Neon Orange Accent Stripe on Cap along needle direction
        needlePinPaint.setColor(Color.parseColor("#FF6200"));
        needlePinPaint.setStrokeWidth(4.5f);
        canvas.drawLine(cx, cy, cx + 18f, cy, needlePinPaint);

        canvas.restore();
    }

    private void drawMiniLcdScreen(Canvas canvas, float cx, float cy, float radius) {
        float lcdW = radius * 1.08f;
        float lcdH = radius * 0.48f;
        float lcdTop = cy + radius * 0.22f;
        float lcdBottom = lcdTop + lcdH;
        RectF lcdRect = new RectF(cx - lcdW * 0.5f, lcdTop, cx + lcdW * 0.5f, lcdBottom);

        Typeface miniBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);

        // Deep glossy black background & glass border
        centerBoxBgPaint.setColor(Color.parseColor("#07090F"));
        canvas.drawRoundRect(lcdRect, 10f, 10f, centerBoxBgPaint);
        centerBoxBorderPaint.setColor(Color.parseColor("#1B232E"));
        centerBoxBorderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(lcdRect, 10f, 10f, centerBoxBorderPaint);

        // 1. Top row: Gear display
        gearPaint.setTypeface(miniBold);
        gearPaint.setColor(Color.parseColor("#FFA000"));
        gearPaint.setTextSize(radius * 0.13f);
        gearPaint.setTextAlign(Paint.Align.CENTER);
        String curGear = (latestState != null && latestState.gear != null && !latestState.gear.isEmpty()) ? latestState.gear : gear;
        canvas.drawText("[ " + curGear + " ]", cx - lcdW * 0.28f, lcdTop + lcdH * 0.36f, gearPaint);

        // 2. Center/Right: Digital speed readout
        textValuePaint.setTypeface(miniBold);
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(radius * 0.24f);
        textValuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.valueOf(Math.round(displaySpeed)), cx + lcdW * 0.20f, lcdTop + lcdH * 0.44f, textValuePaint);

        textUnitPaint.setTypeface(miniBold);
        textUnitPaint.setColor(Color.parseColor("#8E9DAE"));
        textUnitPaint.setTextSize(radius * 0.08f);
        textUnitPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("KM/H", cx + lcdW * 0.23f, lcdTop + lcdH * 0.44f, textUnitPaint);

        // 3. Bottom row: Outside temp, Digital clock, Range (strictly real or --)
        subTextPaint.setTypeface(miniBold);
        subTextPaint.setTextSize(radius * 0.095f);
        subTextPaint.setColor(Color.parseColor("#9AA8B8"));
        subTextPaint.setTextAlign(Paint.Align.CENTER);

        // Temperature (Left)
        String tempStr = (latestState != null && latestState.outsideTempC > -50f)
                ? ((latestState.outsideTempC >= 0 ? "+" : "") + Math.round(latestState.outsideTempC) + "°C")
                : "--°C";
        canvas.drawText(tempStr, cx - lcdW * 0.28f, lcdBottom - 11f, subTextPaint);

        // Clock (Center)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US);
        String timeStr = sdf.format(new java.util.Date());
        canvas.drawText(timeStr, cx, lcdBottom - 11f, subTextPaint);

        // Range (Right)
        String rangeStr = (latestState != null && latestState.rangeKm >= 0) ? (latestState.rangeKm + " km") : "-- km";
        canvas.drawText(rangeStr, cx + lcdW * 0.28f, lcdBottom - 11f, subTextPaint);
    }

    private void drawMiniFuelGauge(Canvas canvas, float cx, float cy, float radius) {
        float rInner = radius * 1.02f;
        float rOuter = radius * 1.15f;
        float rMid = (rInner + rOuter) * 0.5f;
        float strokeW = rOuter - rInner;

        // Housing background arc on right flank
        arcRect.set(cx - rMid, cy - rMid, cx + rMid, cy + rMid);
        cockpitBezelPaint.setColor(Color.parseColor("#121419"));
        cockpitBezelPaint.setStyle(Paint.Style.STROKE);
        cockpitBezelPaint.setStrokeWidth(strokeW + 6f);
        canvas.drawArc(arcRect, -30f, 60f, false, cockpitBezelPaint);

        // Fuel percentage from vehicle state
        int fuelPct = (latestState != null && latestState.fuelPercent >= 0) ? latestState.fuelPercent : -1;
        int activeBars = (fuelPct >= 0) ? Math.min(8, Math.max(0, Math.round((fuelPct / 100f) * 8f))) : -1;

        // 8 curved LED segments
        miniFuelLedPaint.setStyle(Paint.Style.STROKE);
        miniFuelLedPaint.setStrokeWidth(strokeW * 0.72f);

        for (int i = 0; i < 8; i++) {
            float segAngle = 24f - i * 6.8f;
            boolean isActive = (activeBars > 0 && i < activeBars);

            if (isActive) {
                int col = (activeBars <= 2) ? Color.parseColor("#FF2626") : Color.parseColor("#FFA000");
                miniFuelLedPaint.setColor(col);
            } else {
                miniFuelLedPaint.setColor(Color.parseColor("#221810"));
            }
            canvas.drawArc(arcRect, segAngle - 2.5f, 5.0f, false, miniFuelLedPaint);
        }

        // Fuel pump icon stamped at bottom of wing
        double pumpRad = Math.toRadians(33.0);
        float pumpX = (float) (cx + (rMid + 4f) * Math.cos(pumpRad));
        float pumpY = (float) (cy + (rMid + 4f) * Math.sin(pumpRad));
        subTextPaint.setColor(Color.parseColor("#8E9DAE"));
        subTextPaint.setTextSize(spToPx(11f));
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("⛽▶", pumpX, pumpY, subTextPaint);
    }

    private void drawMiniUpperTelltales(Canvas canvas, float cx, float cy, float radius) {
        // ADAS lane car silhouette
        subTextPaint.setColor(Color.parseColor("#38424E"));
        subTextPaint.setTextSize(spToPx(18f));
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("🚗", cx, cy - radius * 0.44f, subTextPaint);

        // Turn signal indicators
        subTextPaint.setTextSize(spToPx(15f));
        subTextPaint.setColor(Color.parseColor("#203828"));
        canvas.drawText("◀", cx - radius * 0.32f, cy - radius * 0.28f, subTextPaint);
        canvas.drawText("▶", cx + radius * 0.32f, cy - radius * 0.28f, subTextPaint);

        // High beam & Engine runes
        subTextPaint.setTextSize(spToPx(11f));
        subTextPaint.setColor(Color.parseColor("#26303C"));
        canvas.drawText("D", cx - radius * 0.16f, cy - radius * 0.28f, subTextPaint);
        canvas.drawText("⚡", cx + radius * 0.16f, cy - radius * 0.28f, subTextPaint);
    }

    private void drawMiniSideCards(Canvas canvas, int w, int h) {
        if (isHolePunchEnabled) return;

        // LEFT WING: MINI Driving Mode & Vehicle Status (x ~ 0.04w .. 0.22w)
        float leftCardW = w * 0.18f;
        float leftCardH = h * 0.55f;
        float leftCardX = w * 0.12f;
        float cardY = h * 0.50f;
        RectF leftCardRect = new RectF(leftCardX - leftCardW * 0.5f, cardY - leftCardH * 0.5f,
                leftCardX + leftCardW * 0.5f, cardY + leftCardH * 0.5f);

        centerBoxBgPaint.setColor(Color.parseColor("#0C0E14"));
        canvas.drawRoundRect(leftCardRect, 14f, 14f, centerBoxBgPaint);
        centerBoxBorderPaint.setColor(Color.parseColor("#1B232E"));
        centerBoxBorderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(leftCardRect, 14f, 14f, centerBoxBorderPaint);

        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#00E5FF"));
        subTextPaint.setTextSize(spToPx(10.5f));
        canvas.drawText("MINI DRIVING MODES", leftCardX, leftCardRect.top + 26f, subTextPaint);

        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(13.5f));
        subTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("SPORT", leftCardX, leftCardRect.top + 52f, subTextPaint);
        subTextPaint.setTypeface(Typeface.DEFAULT);

        subTextPaint.setColor(Color.parseColor("#8E9DAE"));
        subTextPaint.setTextSize(spToPx(9.5f));
        canvas.drawText("MAXIMUM GO-KART FEEL", leftCardX, leftCardRect.top + 70f, subTextPaint);

        // Divider
        canvas.drawLine(leftCardRect.left + 18f, leftCardRect.top + 88f, leftCardRect.right - 18f, leftCardRect.top + 88f, centerBoxBorderPaint);

        // Drive Mode info
        subTextPaint.setColor(Color.parseColor("#CCD8E6"));
        subTextPaint.setTextSize(spToPx(11f));
        canvas.drawText("TRANSMISSION: " + driveMode, leftCardX, leftCardRect.top + 115f, subTextPaint);

        String voltStr = (latestState != null && latestState.batteryVoltage > 0)
                ? String.format(java.util.Locale.US, "BATTERY: %.1f V", latestState.batteryVoltage)
                : "BATTERY: -- V";
        canvas.drawText(voltStr, leftCardX, leftCardRect.top + 140f, subTextPaint);

        // RIGHT WING: Infotainment / Trip card (x ~ 0.78w .. 0.96w)
        float rightCardW = w * 0.18f;
        float rightCardH = h * 0.55f;
        float rightCardX = w * 0.88f;
        RectF rightCardRect = new RectF(rightCardX - rightCardW * 0.5f, cardY - rightCardH * 0.5f,
                rightCardX + rightCardW * 0.5f, cardY + rightCardH * 0.5f);

        canvas.drawRoundRect(rightCardRect, 14f, 14f, centerBoxBgPaint);
        canvas.drawRoundRect(rightCardRect, 14f, 14f, centerBoxBorderPaint);

        subTextPaint.setColor(Color.parseColor("#FFA000"));
        subTextPaint.setTextSize(spToPx(10.5f));
        canvas.drawText("TRIP COMPUTER", rightCardX, rightCardRect.top + 26f, subTextPaint);

        String odoStr = (latestState != null && latestState.odometerKm >= 0) ? (latestState.odometerKm + " KM") : "-- KM";
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(14f));
        subTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(odoStr, rightCardX, rightCardRect.top + 54f, subTextPaint);
        subTextPaint.setTypeface(Typeface.DEFAULT);

        subTextPaint.setColor(Color.parseColor("#8E9DAE"));
        subTextPaint.setTextSize(spToPx(9.5f));
        canvas.drawText("TOTAL ODOMETER", rightCardX, rightCardRect.top + 70f, subTextPaint);

        // Divider
        canvas.drawLine(rightCardRect.left + 18f, rightCardRect.top + 88f, rightCardRect.right - 18f, rightCardRect.top + 88f, centerBoxBorderPaint);

        String tpmsStr = (latestState != null && latestState.tirePressureBar > 0)
                ? String.format(java.util.Locale.US, "TYRES: %.1f BAR", latestState.tirePressureBar)
                : "TYRES: -- BAR";
        subTextPaint.setColor(Color.parseColor("#CCD8E6"));
        subTextPaint.setTextSize(spToPx(11f));
        canvas.drawText(tpmsStr, rightCardX, rightCardRect.top + 115f, subTextPaint);

        String fuelLitStr = (latestState != null && latestState.fuelLiters >= 0)
                ? String.format(java.util.Locale.US, "FUEL: %.0f L", latestState.fuelLiters)
                : "FUEL: -- L";
        canvas.drawText(fuelLitStr, rightCardX, rightCardRect.top + 140f, subTextPaint);
    }

    private final RectF splitDstRect = new RectF();
    private final Paint splitBmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    private float getNeedleAngleOffset(int splitId, boolean isTacho) {
        if (splitId == 24 || splitId == 25) {
            return isTacho ? -90f : 90f;
        }
        if (splitId == 32) return -180f;
        if (splitId == 33 || splitId == 34 || splitId == 35) return -90f;
        return 0f;
    }

    private void drawSplitBitmapTheme(Canvas canvas, int w, int h, int splitId) {
        canvas.drawColor(Color.parseColor("#04060A"));

        // Modern angular polygon themes (Haval / BMW digital cluster: e.g. Theme 17, 18, 24, 25, 30, 33, 40, etc.)
        boolean isAngular = (splitId == 17 || splitId == 18 || splitId == 24 || splitId == 25 ||
                             splitId == 30 || splitId == 33 || splitId == 34 || splitId == 35 ||
                             splitId == 40 || splitId == 41);

        // 1. Proportional Widescreen Dial Geometry (Exact OEM 1920x720 proportions)
        float dialHeight = h * 0.86f;
        float dialWidth = dialHeight;
        float dialCy = h * 0.50f;

        // Push dials flush outwards like in factory GWM instrument panel
        float baseLeftCx = dialWidth * 0.50f + w * 0.02f;
        float baseRightCx = w - (dialWidth * 0.50f + w * 0.02f);

        float leftCx = baseLeftCx + w * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].dx;
        float rightCx = baseRightCx + w * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].dx;
        float leftDialCy = dialCy + h * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].dy;
        float rightDialCy = dialCy + h * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].dy;
        float leftDialW = dialWidth * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].scale;
        float leftDialH = dialHeight * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].scale;
        float rightDialW = dialWidth * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].scale;
        float rightDialH = dialHeight * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].scale;
        float leftRot = elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].rotation;
        float rightRot = elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].rotation;

        // 2. Center Cutout Mask (if HolePunch enabled) or 3D Vehicle Visualizer
        if (isHolePunchEnabled) {
            drawHolePunchMask(canvas, w, h);
        } else {
            canvas.save();
            canvas.translate(effCenterCx - w * 0.5f, effCenterCy - dialCy);
            canvas.scale(effCenterScale, effCenterScale, effCenterCx, effCenterCy);
            drawSplitCenterVisualizer(canvas, w, h, dialHeight);
            canvas.restore();
        }

        // 3. Top Status Bar: 18:41 📡 (P) 0°C + Telltales
        drawSplitTopStatusBar(canvas, w, h);

        // 4. Left Dial: Speedometer Bitmap (drawn over cutout)
        Bitmap spdBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "spd");
        if (spdBmp != null) {
            canvas.save();
            canvas.rotate(leftRot, leftCx, leftDialCy);
            splitDstRect.set(leftCx - leftDialW / 2f, leftDialCy - leftDialH / 2f, leftCx + leftDialW / 2f, leftDialCy + leftDialH / 2f);
            canvas.drawBitmap(spdBmp, null, splitDstRect, splitBmpPaint);
            canvas.restore();
        }

        // 5. Right Dial: Tachometer Bitmap (drawn over cutout)
        Bitmap rpmBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "rpm");
        if (rpmBmp != null) {
            canvas.save();
            canvas.rotate(rightRot, rightCx, rightDialCy);
            splitDstRect.set(rightCx - rightDialW / 2f, rightDialCy - rightDialH / 2f, rightCx + rightDialW / 2f, rightDialCy + rightDialH / 2f);
            canvas.drawBitmap(rpmBmp, null, splitDstRect, splitBmpPaint);
            canvas.restore();
        }

        // 6. Sub-Dials: Fuel (left) & Coolant Temp (right) with theme_X_needle_small (only on angular themes with corner sub-dials)
        if (isAngular) {
            Bitmap needleFuelBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "needle_small_fuel");
            if (needleFuelBmp == null) needleFuelBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "needle_small");
            if (needleFuelBmp != null) {
                float subDialSize = leftDialW * 0.35f;
                float subHalf = subDialSize / 2f;

                // Fuel Sub-dial (Upper Right of Left Dial)
                float fuelCx = leftCx + leftDialW * 0.23f;
                float fuelCy = leftDialCy - leftDialH * 0.22f;
                int fuelPct = (latestState != null && latestState.fuelPercent > 0) ? latestState.fuelPercent : 55;
                float fuelAngle = -45f + (fuelPct / 100f) * 90f;

                canvas.save();
                canvas.rotate(fuelAngle + leftRot, fuelCx, fuelCy);
                splitDstRect.set(fuelCx - subHalf, fuelCy - subHalf, fuelCx + subHalf, fuelCy + subHalf);
                canvas.drawBitmap(needleFuelBmp, null, splitDstRect, splitBmpPaint);
                canvas.restore();

                // Fuel Text: 333km / 36L
                subTextPaint.setTextAlign(Paint.Align.CENTER);
                subTextPaint.setColor(Color.WHITE);
                subTextPaint.setTextSize(spToPx(10.5f));
                int rangeKm = (latestState != null && latestState.rangeKm > 0) ? latestState.rangeKm : 333;
                canvas.drawText(rangeKm + "km", fuelCx, fuelCy - 12f, subTextPaint);
                subTextPaint.setColor(Color.parseColor("#FFD54F"));
                canvas.drawText(Math.round(fuelPct * 0.65f) + "L", fuelCx, fuelCy + 2f, subTextPaint);
            }

            Bitmap needleTempBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "needle_small_temp");
            if (needleTempBmp == null) needleTempBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "needle_small");
            if (needleTempBmp != null) {
                float subDialSize = rightDialW * 0.35f;
                float subHalf = subDialSize / 2f;

                // Coolant Temp Sub-dial (Upper Left of Right Dial)
                float tempCx = rightCx - rightDialW * 0.23f;
                float tempCy = rightDialCy - rightDialH * 0.22f;
                float tempVal = coolantTemp > 0 ? coolantTemp : 96f;
                float tempAngle = 45f - ((tempVal - 40f) / 90f) * 90f;

                canvas.save();
                canvas.rotate(tempAngle + rightRot, tempCx, tempCy);
                splitDstRect.set(tempCx - subHalf, tempCy - subHalf, tempCx + subHalf, tempCy + subHalf);
                canvas.drawBitmap(needleTempBmp, null, splitDstRect, splitBmpPaint);
                canvas.restore();

                // Temp Text: 96°C
                subTextPaint.setColor(Color.WHITE);
                canvas.drawText(Math.round(tempVal) + "°C", tempCx, tempCy - 8f, subTextPaint);
            }
        }

        // 7. Main Needles
        Bitmap needleSpdBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "needle_spd");
        if (needleSpdBmp == null) {
            needleSpdBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "needle");
        }
        if (needleSpdBmp != null) {
            float halfSpd = leftDialW / 2f;
            float spdAngle;
            if (isAngular) {
                spdAngle = -135f + (Math.min(Math.max(displaySpeed, 0f), 220f) / 220f) * 180f;
            } else {
                spdAngle = -135f + (Math.min(Math.max(displaySpeed, 0f), 240f) / 240f) * 270f;
            }
            canvas.save();
            canvas.rotate(spdAngle + getNeedleAngleOffset(splitId, false) + leftRot, leftCx, leftDialCy);
            splitDstRect.set(leftCx - halfSpd, leftDialCy - halfSpd, leftCx + halfSpd, leftDialCy + halfSpd);
            canvas.drawBitmap(needleSpdBmp, null, splitDstRect, splitBmpPaint);
            canvas.restore();
        }

        Bitmap needleRpmBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "needle_rpm");
        if (needleRpmBmp == null) {
            needleRpmBmp = ThemeAssetLoader.loadSplitThemeBitmap(getContext(), splitId, "needle");
        }
        if (needleRpmBmp != null) {
            float halfRpm = rightDialW / 2f;
            float rpmAngle;
            if (isAngular) {
                rpmAngle = -45f - (Math.min(Math.max(displayRpm, 0f), 8000f) / 8000f) * 180f;
            } else {
                rpmAngle = -135f + (Math.min(Math.max(displayRpm, 0f), 8000f) / 270f) * 270f;
            }
            canvas.save();
            canvas.rotate(rpmAngle + getNeedleAngleOffset(splitId, true) + rightRot, rightCx, rightDialCy);
            splitDstRect.set(rightCx - halfRpm, rightDialCy - halfRpm, rightCx + halfRpm, rightDialCy + halfRpm);
            canvas.drawBitmap(needleRpmBmp, null, splitDstRect, splitBmpPaint);
            canvas.restore();
        }

        // 8. Typography Inside Left Dial (Image 2 OEM: Large Speed + km/h + ODO Trip)
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(leftDialH * 0.20f);
        textValuePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(Math.round(displaySpeed)), leftCx, leftDialCy + leftDialH * 0.05f, textValuePaint);

        if (isAngular) {
            subTextPaint.setTextAlign(Paint.Align.LEFT);
            subTextPaint.setColor(Color.parseColor("#90A4AE"));
            subTextPaint.setTextSize(spToPx(11f));
            canvas.drawText("km/h", leftCx - leftDialW * 0.24f, leftDialCy - leftDialH * 0.03f, subTextPaint);
        }

        // Odometer card (ODO: 6702 / Day: 354.0 / Trip: 338) centered under digital speed
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(leftDialH * 0.046f);
        float odoY = leftDialCy + leftDialH * 0.17f;
        canvas.drawText("ODO: 6702", leftCx, odoY, subTextPaint);
        canvas.drawText("Day: 354.0", leftCx, odoY + leftDialH * 0.055f, subTextPaint);
        canvas.drawText("Trip: 338", leftCx, odoY + leftDialH * 0.11f, subTextPaint);

        // 9. Typography Inside Right Dial (Image 2 OEM: Large RPM + x1000 + TPMS 4 Wheels + NORMAL)
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(rightDialH * 0.18f);
        textValuePaint.setTextAlign(Paint.Align.CENTER);
        String rpmFormatted = String.format(java.util.Locale.US, "%.1f", displayRpm / 1000f);
        canvas.drawText(rpmFormatted, rightCx, rightDialCy + rightDialH * 0.04f, textValuePaint);

        if (isAngular) {
            subTextPaint.setTextAlign(Paint.Align.LEFT);
            subTextPaint.setColor(Color.parseColor("#90A4AE"));
            subTextPaint.setTextSize(spToPx(10.5f));
            canvas.drawText("1/min x1000", rightCx + rightDialW * 0.16f, rightDialCy - rightDialH * 0.03f, subTextPaint);
        }

        // 4-Wheel TPMS under RPM (2.4 2.4 / 2.3 2.3) with icon_car.png
        float tpmsX = rightCx;
        float tpmsY = rightDialCy + rightDialH * 0.15f;
        Bitmap tpmsIcon = ThemeAssetLoader.loadSplitAsset(getContext(), "icon_car.png");
        if (tpmsIcon != null) {
            RectF iconRect = new RectF(tpmsX - 9f, tpmsY, tpmsX + 9f, tpmsY + 36f);
            canvas.drawBitmap(tpmsIcon, null, iconRect, splitBmpPaint);
        }
        subTextPaint.setTextSize(rightDialH * 0.044f);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("2.4", tpmsX - 14f, tpmsY + 12f, subTextPaint);
        canvas.drawText("2.3", tpmsX - 14f, tpmsY + 32f, subTextPaint);
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("2.4", tpmsX + 14f, tpmsY + 12f, subTextPaint);
        canvas.drawText("2.3", tpmsX + 14f, tpmsY + 32f, subTextPaint);

        // Drive Mode badge under TPMS: NORMAL
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#B0BEC5"));
        subTextPaint.setTextSize(rightDialH * 0.042f);
        canvas.drawText(driveMode != null ? driveMode : "NORMAL", rightCx, tpmsY + 54f, subTextPaint);

        // 10. Screen Corners / Footer: 11.1L / 100km & 13.9V
        ElementLayout footLayout = elementLayouts[ClusterConfig.ELEM_BOTTOM_FOOTER];
        canvas.save();
        canvas.translate(w * footLayout.dx, h * footLayout.dy);
        canvas.scale(footLayout.scale, footLayout.scale, w * 0.5f, h * 0.94f);
        canvas.rotate(footLayout.rotation, w * 0.5f, h * 0.94f);

        // Bottom-left: 11.1L / 100km
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(dialHeight * 0.044f);
        subTextPaint.setFakeBoldText(true);
        canvas.drawText("11.1L", w * 0.03f, h * 0.91f, subTextPaint);
        subTextPaint.setColor(Color.parseColor("#90A4AE"));
        subTextPaint.setTextSize(dialHeight * 0.036f);
        subTextPaint.setFakeBoldText(false);
        canvas.drawText("100km", w * 0.03f, h * 0.96f, subTextPaint);

        // Bottom-right: 13.9V
        subTextPaint.setTextAlign(Paint.Align.RIGHT);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(dialHeight * 0.044f);
        subTextPaint.setFakeBoldText(true);
        canvas.drawText("13.9V", w * 0.97f, h * 0.93f, subTextPaint);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(12.5f));
        subTextPaint.setFakeBoldText(true);
        canvas.restore();

        // 11. Dynamic Music Player card (only drawn if music is actively playing)
        drawFloatingMusicCard(canvas, w, h);
    }

    private void drawSplitTopStatusBar(Canvas canvas, int w, int h) {
        ElementLayout layout = elementLayouts[ClusterConfig.ELEM_TOP_HEADER];
        canvas.save();
        canvas.translate(w * layout.dx, h * layout.dy);
        canvas.scale(layout.scale, layout.scale, w * 0.5f, h * 0.065f);
        canvas.rotate(layout.rotation, w * 0.5f, h * 0.065f);

        float barY = h * 0.065f;

        // Headlights icon (green) on left: cx = w * 0.22f
        subTextPaint.setColor(Color.parseColor("#00E676"));
        subTextPaint.setTextSize(spToPx(14));
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("🔆", w * 0.22f, barY + 4f, subTextPaint);

        // Handbrake (P) icon (red) on right: cx = w * 0.78f
        subTextPaint.setColor(Color.parseColor("#FF1744"));
        canvas.drawText("ⓟ", w * 0.78f, barY + 4f, subTextPaint);

        // Time: 18:41
        String timeStr = clockFormat.format(new java.util.Date());
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(13.5f));
        subTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(timeStr, w * 0.44f, barY + 4f, subTextPaint);

        // Signal rune
        subTextPaint.setColor(Color.parseColor("#80DEEA"));
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setTextSize(spToPx(11.5f));
        canvas.drawText("📡", w * 0.47f, barY + 4f, subTextPaint);

        // Gear (P / D / R): Center
        int gearColor = "P".equals(gear) ? Color.WHITE : ("R".equals(gear) ? Color.parseColor("#FF1744") : Color.parseColor("#00E5FF"));
        subTextPaint.setColor(gearColor);
        subTextPaint.setTextSize(spToPx(16f));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(gear, w * 0.50f, barY + 5f, subTextPaint);
        subTextPaint.setFakeBoldText(false);

        // Outside Temp: 0°C or +18°C
        float tempVal = (latestState != null) ? latestState.outsideTempC : 18f;
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(spToPx(13.5f));
        canvas.drawText(Math.round(tempVal) + "°C", w * 0.54f, barY + 4f, subTextPaint);

        canvas.restore();
    }

    private void drawSplitCenterVisualizer(Canvas canvas, int w, int h, float dialHeight) {
        float topY = h * 0.38f;
        float bottomY = h * 0.88f;
        float centerX = w * 0.5f;

        // Perspective road lane lines
        statusPillBorderPaint.setColor(Color.parseColor("#15202E"));
        statusPillBorderPaint.setStrokeWidth(2f);
        canvas.drawLine(centerX - 24f, topY, centerX - 140f, bottomY, statusPillBorderPaint);
        canvas.drawLine(centerX + 24f, topY, centerX + 140f, bottomY, statusPillBorderPaint);

        // 3D Vehicle model (Tank 500 / Haval)
        Bitmap carBmp = ThemeAssetLoader.loadSplitAsset(getContext(), "car_body.png");
        if (carBmp != null) {
            float carW = dialHeight * 0.36f;
            float carH = carW * (carBmp.getHeight() / (float) carBmp.getWidth());
            float carX = centerX;
            float carY = h * 0.74f;
            RectF carRect = new RectF(carX - carW / 2f, carY - carH / 2f, carX + carW / 2f, carY + carH / 2f);
            canvas.drawBitmap(carBmp, null, carRect, splitBmpPaint);
        }
    }

    private long lastTapTime = 0;



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

        // 1. Digital Clock & Outside Temp (Top-Left exterior)
        String timeStr = clockFormat.format(new java.util.Date());
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        subTextPaint.setColor(Color.parseColor("#ECEFF1"));
        subTextPaint.setTextSize(spToPx(16));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(timeStr, w * 0.055f, h * 0.12f, subTextPaint);

        float outsideTemp = (latestState != null) ? latestState.outsideTempC : 18f;
        subTextPaint.setColor(Color.parseColor("#80DEEA"));
        subTextPaint.setTextSize(spToPx(12));
        subTextPaint.setFakeBoldText(false);
        canvas.drawText((outsideTemp >= 0 ? "+" : "") + Math.round(outsideTemp) + "°C", w * 0.055f, h * 0.16f, subTextPaint);

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
                float textR = arcR + 24f;
                float tx = (float) (cx + textR * Math.cos(rad));
                float ty = (float) (cy + textR * Math.sin(rad) + 4f);
                textNumberPaint.setColor(s <= displaySpeed ? Color.WHITE : Color.parseColor("#607D8B"));
                textNumberPaint.setTextSize(radius * 0.075f);
                canvas.drawText(String.valueOf(s), tx, ty, textNumberPaint);
            }
        }

        // kW unit at bottom tick
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#607D8B"));
        subTextPaint.setTextSize(radius * 0.065f);
        canvas.drawText("kW", cx - arcR + 12f, cy + arcR * 0.72f, subTextPaint);

        // 6. Inside Dial Content
        // Top: Odometer / Total
        subTextPaint.setColor(Color.parseColor("#78909C"));
        subTextPaint.setTextSize(radius * 0.075f);
        canvas.drawText("Odometer", cx, cy - radius * 0.48f, subTextPaint);

        int odoVal = (latestState != null && latestState.odometerKm > 0) ? latestState.odometerKm : 14350;
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(radius * 0.088f);
        subTextPaint.setFakeBoldText(true);
        canvas.drawText("Total: " + String.format(java.util.Locale.US, "%,d KM", odoVal), cx, cy - radius * 0.34f, subTextPaint);

        // Center: Digital Speed + KM/H
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(radius * 0.36f);
        canvas.drawText(String.valueOf(Math.round(displaySpeed)), cx, cy + radius * 0.04f, textValuePaint);

        textUnitPaint.setColor(Color.parseColor("#00E5FF"));
        textUnitPaint.setTextSize(radius * 0.082f);
        canvas.drawText("KM/H", cx, cy + radius * 0.16f, textUnitPaint);

        // Below Center: Trip: 89 KM
        subTextPaint.setColor(Color.parseColor("#80DEEA"));
        subTextPaint.setTextSize(radius * 0.082f);
        canvas.drawText("Trip: 89 KM", cx, cy + radius * 0.34f, subTextPaint);

        // Bottom Inside: Instant Power Use + Value Pill [ 0.0 kW ]
        subTextPaint.setColor(Color.parseColor("#78909C"));
        subTextPaint.setTextSize(radius * 0.065f);
        canvas.drawText("Instant Power Use", cx, cy + radius * 0.50f, subTextPaint);

        float pillW = radius * 0.42f;
        float pillH = radius * 0.13f;
        float pillCy = cy + radius * 0.66f;
        pillRect.set(cx - pillW / 2, pillCy - pillH / 2, cx + pillW / 2, pillCy + pillH / 2);
        statusPillBgPaint.setColor(Color.parseColor("#0A1826"));
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, statusPillBgPaint);
        statusPillBorderPaint.setColor(Color.parseColor("#1B3346"));
        statusPillBorderPaint.setStrokeWidth(1.2f);
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, statusPillBorderPaint);

        float powerKw = (displaySpeed * displayRpm) / 25000f;
        subTextPaint.setColor(Color.parseColor("#80DEEA"));
        subTextPaint.setTextSize(radius * 0.075f);
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(String.format(java.util.Locale.US, "%.1f kW", powerKw), cx, pillCy + 4f, subTextPaint);

        // 7. Footer Under Left Dial
        ElementLayout footLayout = elementLayouts[ClusterConfig.ELEM_BOTTOM_FOOTER];
        canvas.save();
        canvas.translate(w * footLayout.dx, h * footLayout.dy);
        canvas.scale(footLayout.scale, footLayout.scale, cx, h - 22f);
        canvas.rotate(footLayout.rotation, cx, h - 22f);
        subTextPaint.setTextSize(h * 0.042f);
        subTextPaint.setColor(Color.parseColor("#78909C"));
        String rangeStr = ((latestState != null && latestState.rangeKm > 0) ? latestState.rangeKm : 350) + " KM";
        canvas.drawText("Range Left: ", cx - 30f, h - 22f, subTextPaint);
        subTextPaint.setColor(Color.parseColor("#00E5FF"));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(rangeStr, cx + 46f, h - 22f, subTextPaint);
        canvas.restore();
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
                float textR = arcR + 24f;
                float tx = (float) (cx + textR * Math.cos(rad));
                float ty = (float) (cy + textR * Math.sin(rad) + 4f);
                textNumberPaint.setColor(isRedline ? Color.parseColor("#FF5252") : (r <= displayRpm ? Color.WHITE : Color.parseColor("#607D8B")));
                textNumberPaint.setTextSize(radius * 0.075f);
                canvas.drawText(String.valueOf(r / 1000), tx, ty, textNumberPaint);
            }
        }

        // kW unit at bottom right tick
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#607D8B"));
        subTextPaint.setTextSize(radius * 0.065f);
        canvas.drawText("kW", cx + arcR - 12f, cy + arcR * 0.72f, subTextPaint);

        // 7. Inside Dial Content
        // Gear (P in Amber, D in Cyan, R in Red)
        int gearColor = "P".equals(gear) ? Color.parseColor("#FFB300") : ("R".equals(gear) ? Color.parseColor("#FF1744") : Color.parseColor("#00E5FF"));
        gearPaint.setColor(gearColor);
        gearPaint.setTextSize(radius * 0.36f);
        canvas.drawText(gear, cx, cy - radius * 0.10f, gearPaint);

        // Ready label
        subTextPaint.setColor(Color.parseColor("#CFD8DC"));
        subTextPaint.setTextSize(radius * 0.082f);
        subTextPaint.setFakeBoldText(false);
        canvas.drawText("Ready", cx, cy + radius * 0.05f, subTextPaint);

        // Battery SOC Capsule Card [ 85% 🔋 ]
        float batCardW = radius * 0.44f;
        float batCardH = radius * 0.13f;
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
        subTextPaint.setTextSize(radius * 0.075f);
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(socVal + "%", batteryCardRect.left + 8f, batCardY + 4f, subTextPaint);

        // Battery icon glyph with fill bar
        float bIconLeft = batteryCardRect.right - radius * 0.12f;
        RectF bIcon = new RectF(bIconLeft, batCardY - 6f, bIconLeft + radius * 0.08f, batCardY + 6f);
        statusPillBorderPaint.setColor(Color.parseColor("#00E5FF"));
        statusPillBorderPaint.setStrokeWidth(1.2f);
        canvas.drawRoundRect(bIcon, 2f, 2f, statusPillBorderPaint);
        canvas.drawRect(bIcon.right, batCardY - 3f, bIcon.right + 2f, batCardY + 3f, statusPillBorderPaint);

        statusPillBgPaint.setColor(Color.parseColor("#00E676"));
        canvas.drawRect(bIcon.left + 2f, bIcon.top + 2f, bIcon.left + 2f + (bIcon.width() - 4f) * (socVal / 100f), bIcon.bottom - 2f, statusPillBgPaint);

        // Regen Braking & Pill [ 540 KM ]
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setColor(Color.parseColor("#78909C"));
        subTextPaint.setTextSize(radius * 0.065f);
        canvas.drawText("Regen Braking", cx, cy + radius * 0.50f, subTextPaint);

        float rPillW = radius * 0.42f;
        float rPillH = radius * 0.13f;
        float rPillCy = cy + radius * 0.66f;
        pillRect.set(cx - rPillW / 2, rPillCy - rPillH / 2, cx + rPillW / 2, rPillCy + rPillH / 2);
        statusPillBgPaint.setColor(Color.parseColor("#0A1826"));
        canvas.drawRoundRect(pillRect, rPillH / 2f, rPillH / 2f, statusPillBgPaint);
        statusPillBorderPaint.setColor(Color.parseColor("#1B3346"));
        statusPillBorderPaint.setStrokeWidth(1.2f);
        canvas.drawRoundRect(pillRect, rPillH / 2f, rPillH / 2f, statusPillBorderPaint);

        subTextPaint.setColor(Color.parseColor("#80DEEA"));
        subTextPaint.setTextSize(radius * 0.075f);
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(((latestState != null && latestState.rangeKm > 0) ? latestState.rangeKm : 540) + " KM", cx, rPillCy + 4f, subTextPaint);

        // 8. Footer Under Right Dial
        ElementLayout footLayoutR = elementLayouts[ClusterConfig.ELEM_BOTTOM_FOOTER];
        canvas.save();
        canvas.translate(w * footLayoutR.dx, h * footLayoutR.dy);
        canvas.scale(footLayoutR.scale, footLayoutR.scale, cx, h - 22f);
        canvas.rotate(footLayoutR.rotation, cx, h - 22f);
        subTextPaint.setTextSize(h * 0.042f);
        subTextPaint.setColor(Color.parseColor("#78909C"));
        canvas.drawText("Next Charge: ", cx - 30f, h - 22f, subTextPaint);
        subTextPaint.setColor(Color.parseColor("#00E5FF"));
        subTextPaint.setFakeBoldText(true);
        canvas.drawText("50 KM", cx + 46f, h - 22f, subTextPaint);
        canvas.restore();
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
            applyHolePunchIfNeeded(canvas, w, h);
            if (userSpeedoBmp != null) {
                float halfW = effLeftRadius;
                RectF dst = new RectF(effLeftCx - halfW, effLeftCy - halfW, effLeftCx + halfW, effLeftCy + halfW);
                canvas.save();
                canvas.rotate(elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].rotation, effLeftCx, effLeftCy);
                canvas.drawBitmap(userSpeedoBmp, null, dst, null);
                canvas.restore();
            }
            if (userTachoBmp != null) {
                float halfW = effRightRadius;
                RectF dst = new RectF(effRightCx - halfW, effRightCy - halfW, effRightCx + halfW, effRightCy + halfW);
                canvas.save();
                canvas.rotate(elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].rotation, effRightCx, effRightCy);
                canvas.drawBitmap(userTachoBmp, null, dst, null);
                canvas.restore();
            }
            if (userNeedleBmp != null) {
                float spdAngle = getSpeedAngle(displaySpeed, 240);
                canvas.save();
                canvas.rotate(spdAngle + elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].rotation, effLeftCx, effLeftCy);
                canvas.drawBitmap(userNeedleBmp, effLeftCx - userNeedleBmp.getWidth() / 2f, effLeftCy - userNeedleBmp.getHeight() / 2f, null);
                canvas.restore();

                float rpmAngle = getRpmAngle(displayRpm, 8000);
                canvas.save();
                canvas.rotate(rpmAngle + elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].rotation, effRightCx, effRightCy);
                canvas.drawBitmap(userNeedleBmp, effRightCx - userNeedleBmp.getWidth() / 2f, effRightCy - userNeedleBmp.getHeight() / 2f, null);
                canvas.restore();
            } else {
                drawSportNeedle(canvas, effLeftCx, effLeftCy, effLeftRadius * 0.82f, getSpeedAngle(displaySpeed, 240) + elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].rotation, Color.RED);
                drawSportNeedle(canvas, effRightCx, effRightCy, effRightRadius * 0.82f, getRpmAngle(displayRpm, 8000) + elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].rotation, Color.RED);
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
        if (activeTheme == ClusterConfig.THEME_TAYCAN_ARC || 
            activeTheme == ClusterConfig.THEME_MINI_COOPER ||
            activeTheme == ClusterConfig.THEME_USER_THEME || 
            activeTheme >= ClusterConfig.THEME_SPLIT_OFFSET) {
            // All telemetry cards & pills are natively integrated into the Taycan/MINI layout & Split themes
            return;
        }
        if (latestState == null) {
            latestState = new VehicleState((int) displaySpeed, gear, (int) displayRpm, true, coolantTemp);
        }

        // 1. Left Mini-Widget (Under Left Dial / Speedometer)
        if (widgetLeft != ClusterConfig.WIDGET_NONE) {
            String text = latestState.formatWidget(widgetLeft);
            if (!text.isEmpty()) {
                float scale = elementLayouts[ClusterConfig.ELEM_WIDGET_LEFT].scale;
                widgetTextPaint.setTextSize(spToPx(11) * scale);
                float textW = widgetTextPaint.measureText(text);
                float pillW = Math.max(130f * scale, textW + 32f * scale);
                float pillH = 28f * scale;
                float cx = effLeftCx + viewWidth * elementLayouts[ClusterConfig.ELEM_WIDGET_LEFT].dx;
                float cy = effLeftCy + effLeftRadius * 0.74f + viewHeight * elementLayouts[ClusterConfig.ELEM_WIDGET_LEFT].dy;
                widgetLeftRect.set(cx - pillW / 2f, cy - pillH / 2f, cx + pillW / 2f, cy + pillH / 2f);
                canvas.drawRoundRect(widgetLeftRect, pillH / 2f, pillH / 2f, widgetBgPaint);
                canvas.drawRoundRect(widgetLeftRect, pillH / 2f, pillH / 2f, widgetBorderPaint);
                canvas.drawText(text, cx, cy + 4f * scale, widgetTextPaint);
            }
        }

        // 2. Right Mini-Widget (Under Right Dial / Tachometer)
        if (widgetRight != ClusterConfig.WIDGET_NONE) {
            String text = latestState.formatWidget(widgetRight);
            if (!text.isEmpty()) {
                float scale = elementLayouts[ClusterConfig.ELEM_WIDGET_RIGHT].scale;
                widgetTextPaint.setTextSize(spToPx(11) * scale);
                float textW = widgetTextPaint.measureText(text);
                float pillW = Math.max(130f * scale, textW + 32f * scale);
                float pillH = 28f * scale;
                float cx = effRightCx + viewWidth * elementLayouts[ClusterConfig.ELEM_WIDGET_RIGHT].dx;
                float cy = effRightCy + effRightRadius * 0.74f + viewHeight * elementLayouts[ClusterConfig.ELEM_WIDGET_RIGHT].dy;
                widgetRightRect.set(cx - pillW / 2f, cy - pillH / 2f, cx + pillW / 2f, cy + pillH / 2f);
                canvas.drawRoundRect(widgetRightRect, pillH / 2f, pillH / 2f, widgetBgPaint);
                canvas.drawRoundRect(widgetRightRect, pillH / 2f, pillH / 2f, widgetBorderPaint);
                canvas.drawText(text, cx, cy + 4f * scale, widgetTextPaint);
            }
        }

        // 3. Center Mini-Widget (Under Center Box / Display)
        if (widgetCenter != ClusterConfig.WIDGET_NONE) {
            String text = latestState.formatWidget(widgetCenter);
            if (!text.isEmpty()) {
                float scale = elementLayouts[ClusterConfig.ELEM_WIDGET_CENTER].scale;
                widgetTextPaint.setTextSize(spToPx(11) * scale);
                float textW = widgetTextPaint.measureText(text);
                float pillW = Math.max(130f * scale, textW + 32f * scale);
                float pillH = 28f * scale;
                float cx = effCenterCx + viewWidth * elementLayouts[ClusterConfig.ELEM_WIDGET_CENTER].dx;
                float cy = viewHeight * 0.82f + viewHeight * elementLayouts[ClusterConfig.ELEM_WIDGET_CENTER].dy;
                widgetCenterRect.set(cx - pillW / 2f, cy - pillH / 2f, cx + pillW / 2f, cy + pillH / 2f);
                canvas.drawRoundRect(widgetCenterRect, pillH / 2f, pillH / 2f, widgetBgPaint);
                canvas.drawRoundRect(widgetCenterRect, pillH / 2f, pillH / 2f, widgetBorderPaint);
                canvas.drawText(text, cx, cy + 4f * scale, widgetTextPaint);
            }
        }
    }

    public RectF getElementBounds(int elementId, int w, int h) {
        RectF r = new RectF();
        float curGaugeRadius = Math.min(w * 0.142f, h * 0.375f);
        float baseCy = h * 0.50f;
        float baseLeftCx = w * 0.16f;
        float baseRightCx = w * 0.84f;

        if (activeTheme == ClusterConfig.THEME_MINI_COOPER) {
            baseLeftCx = w * 0.38f;
            baseRightCx = w * 0.58f;
        } else if (activeTheme >= ClusterConfig.THEME_SPLIT_OFFSET) {
            float dialHeight = h * 0.86f;
            baseLeftCx = dialHeight * 0.50f + w * 0.02f;
            baseRightCx = w - (dialHeight * 0.50f + w * 0.02f);
            curGaugeRadius = dialHeight * 0.5f;
        }

        switch (elementId) {
            case ClusterConfig.ELEM_LEFT_GAUGE: {
                float rad = (activeTheme == ClusterConfig.THEME_MINI_COOPER ? curGaugeRadius * 0.95f : curGaugeRadius) * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].scale;
                float cx = baseLeftCx + w * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].dx;
                float cy = (activeTheme == ClusterConfig.THEME_MINI_COOPER ? baseCy + h * 0.03f : baseCy) + h * elementLayouts[ClusterConfig.ELEM_LEFT_GAUGE].dy;
                r.set(cx - rad, cy - rad, cx + rad, cy + rad);
                break;
            }
            case ClusterConfig.ELEM_RIGHT_GAUGE: {
                float rad = (activeTheme == ClusterConfig.THEME_MINI_COOPER ? curGaugeRadius * 1.08f : curGaugeRadius) * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].scale;
                float cx = baseRightCx + w * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].dx;
                float cy = (activeTheme == ClusterConfig.THEME_MINI_COOPER ? baseCy - h * 0.01f : baseCy) + h * elementLayouts[ClusterConfig.ELEM_RIGHT_GAUGE].dy;
                r.set(cx - rad, cy - rad, cx + rad, cy + rad);
                break;
            }
            case ClusterConfig.ELEM_CENTER_BLOCK: {
                float scale = elementLayouts[ClusterConfig.ELEM_CENTER_BLOCK].scale;
                float cx = w * 0.5f + w * elementLayouts[ClusterConfig.ELEM_CENTER_BLOCK].dx;
                float cy = baseCy + h * elementLayouts[ClusterConfig.ELEM_CENTER_BLOCK].dy;
                float halfW = (w * 0.14f) * scale;
                float halfH = (curGaugeRadius * 0.75f) * scale;
                r.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
                break;
            }
            case ClusterConfig.ELEM_WIDGET_LEFT: {
                float scale = elementLayouts[ClusterConfig.ELEM_WIDGET_LEFT].scale;
                float cx = effLeftCx + w * elementLayouts[ClusterConfig.ELEM_WIDGET_LEFT].dx;
                float cy = effLeftCy + effLeftRadius * 0.74f + h * elementLayouts[ClusterConfig.ELEM_WIDGET_LEFT].dy;
                float halfW = Math.max(65f, w * 0.06f) * scale;
                float halfH = 16f * scale;
                r.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
                break;
            }
            case ClusterConfig.ELEM_WIDGET_RIGHT: {
                float scale = elementLayouts[ClusterConfig.ELEM_WIDGET_RIGHT].scale;
                float cx = effRightCx + w * elementLayouts[ClusterConfig.ELEM_WIDGET_RIGHT].dx;
                float cy = effRightCy + effRightRadius * 0.74f + h * elementLayouts[ClusterConfig.ELEM_WIDGET_RIGHT].dy;
                float halfW = Math.max(65f, w * 0.06f) * scale;
                float halfH = 16f * scale;
                r.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
                break;
            }
            case ClusterConfig.ELEM_WIDGET_CENTER: {
                float scale = elementLayouts[ClusterConfig.ELEM_WIDGET_CENTER].scale;
                float cx = effCenterCx + w * elementLayouts[ClusterConfig.ELEM_WIDGET_CENTER].dx;
                float cy = h * 0.82f + h * elementLayouts[ClusterConfig.ELEM_WIDGET_CENTER].dy;
                float halfW = Math.max(70f, w * 0.07f) * scale;
                float halfH = 16f * scale;
                r.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
                break;
            }
            case ClusterConfig.ELEM_MEDIA_PLAYER: {
                ElementLayout el = elementLayouts[ClusterConfig.ELEM_MEDIA_PLAYER];
                float halfW = (Math.min(360f, w * 0.28f) * 0.5f) * el.scale;
                float halfH = 27f * el.scale;
                float cx = w * 0.5f + w * el.dx;
                float cy = h * 0.82f + h * el.dy;
                r.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
                break;
            }
            case ClusterConfig.ELEM_TOP_HEADER: {
                ElementLayout el = elementLayouts[ClusterConfig.ELEM_TOP_HEADER];
                float halfW = (w * 0.42f) * el.scale;
                float halfH = (h * 0.065f) * el.scale;
                float cx = w * 0.5f + w * el.dx;
                float cy = h * 0.075f + h * el.dy;
                r.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
                break;
            }
            case ClusterConfig.ELEM_BOTTOM_FOOTER: {
                ElementLayout el = elementLayouts[ClusterConfig.ELEM_BOTTOM_FOOTER];
                float halfW = (w * 0.44f) * el.scale;
                float halfH = (h * 0.055f) * el.scale;
                float cx = w * 0.5f + w * el.dx;
                float cy = h * 0.94f + h * el.dy;
                r.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
                break;
            }
        }
        return r;
    }

    private void drawEditorOverlay(Canvas canvas, int w, int h) {
        // 1. Draw subtle dashed borders around all inactive elements
        editorSelectionPaint.setColor(Color.parseColor("#4400E5FF"));
        editorSelectionPaint.setStrokeWidth(1.5f);
        for (int elemId = 0; elemId < ClusterConfig.ELEM_COUNT; elemId++) {
            if (elemId == selectedElement) continue;
            RectF bounds = getElementBounds(elemId, w, h);
            canvas.save();
            canvas.rotate(elementLayouts[elemId].rotation, bounds.centerX(), bounds.centerY());
            canvas.drawRoundRect(bounds, 12f, 12f, editorSelectionPaint);
            canvas.restore();
        }

        // 2. Draw prominent glowing neon border around selected element
        RectF selBounds = getElementBounds(selectedElement, w, h);
        float cx = selBounds.centerX();
        float cy = selBounds.centerY();
        ElementLayout layout = elementLayouts[selectedElement];

        canvas.save();
        canvas.rotate(layout.rotation, cx, cy);

        editorSelectionPaint.setColor(Color.parseColor("#00E5FF"));
        editorSelectionPaint.setStrokeWidth(3f);
        canvas.drawRoundRect(selBounds, 12f, 12f, editorSelectionPaint);

        // 3. Draw 4 corner resize handles
        float handleRadius = 7f;
        editorHandlePaint.setColor(Color.parseColor("#00E5FF"));
        canvas.drawCircle(selBounds.left, selBounds.top, handleRadius, editorHandlePaint);
        canvas.drawCircle(selBounds.right, selBounds.top, handleRadius, editorHandlePaint);
        canvas.drawCircle(selBounds.left, selBounds.bottom, handleRadius, editorHandlePaint);
        canvas.drawCircle(selBounds.right, selBounds.bottom, handleRadius, editorHandlePaint);

        editorHandlePaint.setColor(Color.WHITE);
        canvas.drawCircle(selBounds.left, selBounds.top, handleRadius * 0.5f, editorHandlePaint);
        canvas.drawCircle(selBounds.right, selBounds.top, handleRadius * 0.5f, editorHandlePaint);
        canvas.drawCircle(selBounds.left, selBounds.bottom, handleRadius * 0.5f, editorHandlePaint);
        canvas.drawCircle(selBounds.right, selBounds.bottom, handleRadius * 0.5f, editorHandlePaint);

        // 4. Draw Center Crosshair
        canvas.drawLine(cx - 14f, cy, cx + 14f, cy, editorSelectionPaint);
        canvas.drawLine(cx, cy - 14f, cx, cy + 14f, editorSelectionPaint);

        canvas.restore();

        // 5. Draw Information Pill
        int dxPx = Math.round(layout.dx * w);
        int dyPx = Math.round(layout.dy * h);
        int scalePct = Math.round(layout.scale * 100f);
        int rotDeg = Math.round(layout.rotation);
        String label = ClusterConfig.ELEMENT_NAMES[selectedElement] + ": X=" + (dxPx >= 0 ? "+" : "") + dxPx + "px  Y=" + (dyPx >= 0 ? "+" : "") + dyPx + "px  (" + scalePct + "%  " + (rotDeg >= 0 ? "+" : "") + rotDeg + "°)";

        editorTextPaint.setTextSize(spToPx(11));
        float textW = editorTextPaint.measureText(label);
        float pillW = textW + 28f;
        float pillH = 26f;
        float pillY = selBounds.top - pillH - 6f;
        if (pillY < 10f) {
            pillY = selBounds.bottom + 6f;
        }

        RectF pillR = new RectF(cx - pillW / 2f, pillY, cx + pillW / 2f, pillY + pillH);
        canvas.drawRoundRect(pillR, pillH / 2f, pillH / 2f, editorPillPaint);

        editorSelectionPaint.setStrokeWidth(1.2f);
        canvas.drawRoundRect(pillR, pillH / 2f, pillH / 2f, editorSelectionPaint);

        canvas.drawText(label, cx, pillY + pillH * 0.68f, editorTextPaint);
    }
}
