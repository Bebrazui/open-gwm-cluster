package org.opengwm.split.media;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.KeyEvent;

import org.opengwm.split.util.AppLogger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Universal Media & Telemetry Tracker for Open GWM Cluster.
 * Connects directly to Android MediaSessionManager, NotificationListenerService,
 * and system broadcast intents to extract real-time:
 *  - Track Title, Artist, Album
 *  - High-resolution Album Cover Art
 *  - Real-time Playback Position (ms) and Track Duration (ms)
 *  - Playback State (Playing / Paused / Stopped)
 *  - Transport controls (Play/Pause, Skip Next, Skip Prev, Seek)
 */
public class MediaTracker {
    private static final String TAG = "MediaTracker";
    private static volatile MediaTracker sInstance = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<OnMediaStateListener> listeners = new CopyOnWriteArrayList<>();

    // Playback State
    private volatile boolean isPlaying = false;
    private volatile String trackTitle = "Рок-н-Ролла нет";
    private volatile String trackArtist = "Миша Бурый";
    private volatile String trackAlbum = "GWM Tank 500 Cockpit";
    private volatile long positionMs = 74000;
    private volatile long durationMs = 215000;
    private volatile long lastPositionUpdateRealtime = SystemClock.elapsedRealtime();
    private volatile float playbackSpeed = 1.0f;

    // Artwork & Colors
    private volatile Bitmap albumArt = null;
    private volatile Bitmap circularAlbumArt = null;
    private volatile int dominantColor = 0xFF00E5FF; // Default Cyan
    private volatile int secondaryColor = 0xFF152D42;

    // Android Media System Connections
    private Context appContext = null;
    private MediaSessionManager mediaSessionManager = null;
    private MediaController activeController = null;
    private NotificationListenerService notificationListener = null;
    private BroadcastReceiver broadcastReceiver = null;

    public interface OnMediaStateListener {
        void onMediaStateChanged(MediaTracker tracker);
    }

    public static synchronized MediaTracker getInstance() {
        if (sInstance == null) {
            sInstance = new MediaTracker();
        }
        return sInstance;
    }

    private MediaTracker() {
        // Initialize default sample artwork
        createDefaultAlbumArt();
    }

    public synchronized void init(Context context) {
        if (context == null) return;
        this.appContext = context.getApplicationContext();

        try {
            mediaSessionManager = (MediaSessionManager) appContext.getSystemService(Context.MEDIA_SESSION_SERVICE);
        } catch (Throwable t) {
            AppLogger.w(TAG, "MediaSessionManager unavailable: " + t.getMessage());
        }

        registerBroadcastReceivers();
        checkActiveSessions();
    }

    public synchronized void onListenerConnected(NotificationListenerService listener) {
        this.notificationListener = listener;
        if (appContext == null && listener != null) {
            this.appContext = listener.getApplicationContext();
        }
        AppLogger.i(TAG, "NotificationListenerService connected. Querying active MediaSessions...");
        checkActiveSessions();
    }

    public synchronized void onListenerDisconnected() {
        this.notificationListener = null;
        AppLogger.i(TAG, "NotificationListenerService disconnected");
    }

    /**
     * Checks if notification access is granted by the user in Android Settings.
     */
    public static boolean isNotificationAccessGranted(Context context) {
        if (context == null) return false;
        try {
            String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            return flat != null && flat.contains(context.getPackageName());
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Queries active sessions via MediaSessionManager when NotificationListener is bound.
     */
    public synchronized void checkActiveSessions() {
        if (mediaSessionManager == null || appContext == null) return;
        try {
            ComponentName compName = notificationListener != null 
                    ? new ComponentName(appContext, MediaNotificationListener.class) 
                    : null;

            List<MediaController> controllers = null;
            if (compName != null) {
                controllers = mediaSessionManager.getActiveSessions(compName);
            }

            if (controllers != null && !controllers.isEmpty()) {
                // Find first playing session, or first available session
                MediaController chosen = null;
                for (MediaController mc : controllers) {
                    PlaybackState ps = mc.getPlaybackState();
                    if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) {
                        chosen = mc;
                        break;
                    }
                }
                if (chosen == null) {
                    chosen = controllers.get(0);
                }
                bindController(chosen);
            }
        } catch (Throwable t) {
            AppLogger.d(TAG, "checkActiveSessions: " + t.getMessage());
        }
    }

    private synchronized void bindController(final MediaController controller) {
        if (controller == null || controller == this.activeController) return;

        if (this.activeController != null) {
            try {
                this.activeController.unregisterCallback(controllerCallback);
            } catch (Throwable ignored) {}
        }

        this.activeController = controller;
        AppLogger.i(TAG, "Привязан MediaController: " + controller.getPackageName());

        try {
            controller.registerCallback(controllerCallback, mainHandler);
            updateFromMetadata(controller.getMetadata());
            updateFromPlaybackState(controller.getPlaybackState());
        } catch (Throwable t) {
            AppLogger.w(TAG, "Ошибка регистрации коллбэка контроллера: " + t.getMessage());
        }
    }

    private final MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            updateFromMetadata(metadata);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            updateFromPlaybackState(state);
        }

        @Override
        public void onSessionDestroyed() {
            activeController = null;
            checkActiveSessions();
        }
    };

    /**
     * Extracts title, artist, album, art, and duration from standard MediaMetadata.
     */
    public synchronized void updateFromMetadata(MediaMetadata metadata) {
        if (metadata == null) return;
        try {
            String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            if (title == null || title.isEmpty()) {
                title = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
            }
            if (title != null && !title.isEmpty()) {
                this.trackTitle = title;
            }

            String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            if (artist == null || artist.isEmpty()) {
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
            }
            if (artist == null || artist.isEmpty()) {
                artist = metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR);
            }
            if (artist != null && !artist.isEmpty()) {
                this.trackArtist = artist;
            }

            String album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
            if (album != null && !album.isEmpty()) {
                this.trackAlbum = album;
            }

            long dur = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            if (dur > 0) {
                this.durationMs = dur;
            }

            // Extract Artwork
            Bitmap bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (bmp == null) {
                bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            }
            if (bmp != null) {
                setAlbumArt(bmp);
            }

            notifyListeners();
        } catch (Throwable t) {
            AppLogger.w(TAG, "updateFromMetadata error: " + t.getMessage());
        }
    }

    /**
     * Extracts playback state, current position, and speed from PlaybackState.
     */
    public synchronized void updateFromPlaybackState(PlaybackState state) {
        if (state == null) return;
        try {
            int st = state.getState();
            this.isPlaying = (st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_FAST_FORWARDING || st == PlaybackState.STATE_REWINDING);
            this.positionMs = state.getPosition();
            this.lastPositionUpdateRealtime = SystemClock.elapsedRealtime();
            float speed = state.getPlaybackSpeed();
            this.playbackSpeed = speed > 0 ? speed : 1.0f;

            notifyListeners();
        } catch (Throwable t) {
            AppLogger.w(TAG, "updateFromPlaybackState error: " + t.getMessage());
        }
    }

    /**
     * Intercepts media notifications from Yandex Music, Spotify, VK, Radio, etc.
     */
    public synchronized void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        Notification n = sbn.getNotification();
        if (n == null) return;

        // Check if media session token is inside extras
        if (n.extras != null) {
            MediaSession.Token token = n.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
            if (token != null && appContext != null) {
                try {
                    MediaController mc = new MediaController(appContext, token);
                    bindController(mc);
                    return;
                } catch (Throwable ignored) {}
            }

            // Fallback: extract title, artist, art from notification extras
            CharSequence titleCs = n.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textCs = n.extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence subCs = n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

            boolean updated = false;
            if (titleCs != null && titleCs.length() > 0) {
                this.trackTitle = titleCs.toString();
                updated = true;
            }
            if (textCs != null && textCs.length() > 0) {
                this.trackArtist = textCs.toString();
                updated = true;
            }
            if (subCs != null && subCs.length() > 0) {
                this.trackAlbum = subCs.toString();
                updated = true;
            }

            // Extract large icon / art
            Bitmap artBmp = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Icon icon = n.getLargeIcon();
                if (icon != null && appContext != null) {
                    try {
                        Drawable d = icon.loadDrawable(appContext);
                        if (d instanceof BitmapDrawable) {
                            artBmp = ((BitmapDrawable) d).getBitmap();
                        }
                    } catch (Throwable ignored) {}
                }
            }
            if (artBmp == null) {
                Object obj = n.extras.get(Notification.EXTRA_LARGE_ICON);
                if (obj instanceof Bitmap) {
                    artBmp = (Bitmap) obj;
                }
            }

            if (artBmp != null) {
                setAlbumArt(artBmp);
                updated = true;
            }

            if (updated) {
                this.isPlaying = true;
                notifyListeners();
            }
        }
    }

    public synchronized void onNotificationRemoved(StatusBarNotification sbn) {
        // If the active music player notification was dismissed
        if (sbn != null && activeController != null && sbn.getPackageName().equals(activeController.getPackageName())) {
            this.isPlaying = false;
            notifyListeners();
        }
    }

    private void registerBroadcastReceivers() {
        if (appContext == null || broadcastReceiver != null) return;

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String track = intent.getStringExtra("track");
                if (track == null) track = intent.getStringExtra("title");
                String artist = intent.getStringExtra("artist");
                String album = intent.getStringExtra("album");

                boolean updated = false;
                if (track != null && !track.isEmpty()) {
                    trackTitle = track;
                    updated = true;
                }
                if (artist != null && !artist.isEmpty()) {
                    trackArtist = artist;
                    updated = true;
                }
                if (album != null && !album.isEmpty()) {
                    trackAlbum = album;
                    updated = true;
                }

                if (intent.hasExtra("playing")) {
                    isPlaying = intent.getBooleanExtra("playing", false);
                    updated = true;
                } else if (appContext != null) {
                    try {
                        AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
                        if (am != null) {
                            isPlaying = am.isMusicActive();
                            updated = true;
                        }
                    } catch (Throwable ignored) {}
                }

                long dur = intent.getLongExtra("duration", -1);
                if (dur > 0) {
                    durationMs = dur;
                    updated = true;
                }
                long pos = intent.getLongExtra("position", -1);
                if (pos >= 0) {
                    positionMs = pos;
                    lastPositionUpdateRealtime = SystemClock.elapsedRealtime();
                    updated = true;
                }

                if (updated) {
                    notifyListeners();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.music.metachanged");
        filter.addAction("com.android.music.playstatechanged");
        filter.addAction("com.android.music.playbackcomplete");
        filter.addAction("com.spotify.music.metadatachanged");
        filter.addAction("com.spotify.music.playbackstatechanged");
        filter.addAction("yandex.auto.music.action.track_changed");
        filter.addAction("com.yandex.music.action.PLAY");
        filter.addAction("com.yandex.music.action.PAUSE");
        filter.addAction("com.maxmpz.audioplayer.TRACK_CHANGED");
        filter.addAction("com.vk.music.METADATA_CHANGED");
        filter.addAction("com.gwm.app.media.action.TRACK_INFO");

        try {
            appContext.registerReceiver(broadcastReceiver, filter);
        } catch (Throwable t) {
            AppLogger.w(TAG, "registerReceiver error: " + t.getMessage());
        }
    }

    public synchronized void setAlbumArt(Bitmap raw) {
        if (raw == null || raw.isRecycled()) return;
        try {
            // Scale to a maximum 240x240 for buttery smooth 60 FPS canvas rendering
            int targetSize = 240;
            Bitmap scaled = Bitmap.createScaledBitmap(raw, targetSize, targetSize, true);
            this.albumArt = scaled;

            // Generate circular cropped version for Vinyl disk center
            this.circularAlbumArt = createCircularBitmap(scaled);

            // Extract dominant vibrant color & dark secondary color
            this.dominantColor = extractDominantColor(scaled);
            this.secondaryColor = darkenColor(dominantColor, 0.35f);
        } catch (Throwable t) {
            AppLogger.w(TAG, "setAlbumArt error: " + t.getMessage());
        }
    }

    private Bitmap createCircularBitmap(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Rect rect = new Rect(0, 0, w, h);
        RectF rectF = new RectF(rect);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, rect, rect, paint);
        return output;
    }

    private void createDefaultAlbumArt() {
        int size = 240;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.parseColor("#0F1A24"));

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor("#00E5FF"));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4f);
        canvas.drawCircle(size / 2f, size / 2f, size * 0.42f, p);

        p.setStyle(Paint.Style.FILL);
        p.setTextSize(64f);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("♬", size / 2f, size / 2f + 22f, p);

        setAlbumArt(bmp);
    }

    /**
     * Loads high-fidelity demo track for immediate offline inspection and theme switching.
     */
    public synchronized void loadDemoTrack() {
        this.trackTitle = "Рок-н-Ролла нет";
        this.trackArtist = "Миша Бурый";
        this.trackAlbum = "Tank 500 Cockpit Drive";
        this.durationMs = 215000;
        this.positionMs = 86000;
        this.lastPositionUpdateRealtime = SystemClock.elapsedRealtime();
        this.isPlaying = true;

        // Create high-tech demo album cover
        int size = 240;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.parseColor("#180E24"));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#E040FB"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        canvas.drawRect(20f, 20f, size - 20f, size - 20f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(58f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("⚡", size / 2f, size / 2f + 20f, paint);

        setAlbumArt(bmp);
        this.dominantColor = Color.parseColor("#E040FB");
        this.secondaryColor = Color.parseColor("#250A36");

        notifyListeners();
    }

    // =========================================================================
    // Transport Controls
    // =========================================================================

    public void playPause() {
        if (activeController != null) {
            try {
                PlaybackState ps = activeController.getPlaybackState();
                if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) {
                    activeController.getTransportControls().pause();
                    this.isPlaying = false;
                } else {
                    activeController.getTransportControls().play();
                    this.isPlaying = true;
                }
                notifyListeners();
                return;
            } catch (Throwable t) {
                AppLogger.w(TAG, "playPause via controller failed: " + t.getMessage());
            }
        }
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        this.isPlaying = !this.isPlaying;
        notifyListeners();
    }

    public void skipNext() {
        if (activeController != null) {
            try {
                activeController.getTransportControls().skipToNext();
                return;
            } catch (Throwable ignored) {}
        }
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    public void skipPrev() {
        if (activeController != null) {
            try {
                activeController.getTransportControls().skipToPrevious();
                return;
            } catch (Throwable ignored) {}
        }
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    public void seekTo(long targetMs) {
        if (targetMs < 0) targetMs = 0;
        if (durationMs > 0 && targetMs > durationMs) targetMs = durationMs;
        this.positionMs = targetMs;
        this.lastPositionUpdateRealtime = SystemClock.elapsedRealtime();

        if (activeController != null) {
            try {
                activeController.getTransportControls().seekTo(targetMs);
            } catch (Throwable ignored) {}
        }
        notifyListeners();
    }

    private void dispatchMediaKeyEvent(int keyCode) {
        if (appContext == null) return;
        try {
            AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                long now = SystemClock.uptimeMillis();
                am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
                am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
            }
        } catch (Throwable t) {
            AppLogger.w(TAG, "dispatchMediaKeyEvent: " + t.getMessage());
        }
    }

    // =========================================================================
    // Getters & Math
    // =========================================================================

    public boolean isPlaying() {
        return isPlaying;
    }

    public String getTrackTitle() {
        return (trackTitle != null && !trackTitle.isEmpty()) ? trackTitle : "Аудиопоток";
    }

    public String getTrackArtist() {
        return (trackArtist != null && !trackArtist.isEmpty()) ? trackArtist : "Воспроизведение";
    }

    public String getTrackAlbum() {
        return trackAlbum;
    }

    public Bitmap getAlbumArt() {
        return albumArt;
    }

    public Bitmap getCircularAlbumArt() {
        return circularAlbumArt;
    }

    public int getDominantColor() {
        return dominantColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public long getDurationMs() {
        return durationMs > 0 ? durationMs : 0;
    }

    public long getCurrentPositionMs() {
        if (!isPlaying || durationMs <= 0) {
            return positionMs;
        }
        long elapsed = SystemClock.elapsedRealtime() - lastPositionUpdateRealtime;
        long current = positionMs + (long) (elapsed * playbackSpeed);
        if (current > durationMs) current = durationMs;
        if (current < 0) current = 0;
        return current;
    }

    public float getProgress() {
        if (durationMs <= 0) return 0f;
        float p = (float) getCurrentPositionMs() / (float) durationMs;
        if (p < 0f) p = 0f;
        if (p > 1f) p = 1f;
        return p;
    }

    public static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format(java.util.Locale.US, "%02d:%02d", min, sec);
    }

    public void addListener(OnMediaStateListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeListener(OnMediaStateListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    private void notifyListeners() {
        mainHandler.post(() -> {
            for (OnMediaStateListener l : listeners) {
                try {
                    l.onMediaStateChanged(this);
                } catch (Throwable ignored) {}
            }
        });
    }

    // =========================================================================
    // Color Helpers
    // =========================================================================

    public static int extractDominantColor(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return 0xFF00E5FF;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int step = Math.max(1, Math.min(w, h) / 12);
        long rSum = 0, gSum = 0, bSum = 0;
        int count = 0;

        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int pixel = bitmap.getPixel(x, y);
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                int brightness = (r + g + b) / 3;
                // Exclude near black and washed out white
                if (brightness > 35 && brightness < 225) {
                    rSum += r;
                    gSum += g;
                    bSum += b;
                    count++;
                }
            }
        }
        if (count == 0) return 0xFF00E5FF;
        int r = (int) (rSum / count);
        int g = (int) (gSum / count);
        int b = (int) (bSum / count);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.round(((color >> 16) & 0xFF) * factor);
        int g = Math.round(((color >> 8) & 0xFF) * factor);
        int b = Math.round((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
