package org.opengwm.split.media;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.opengwm.split.util.AppLogger;

/**
 * Android NotificationListenerService to intercept active MediaSessions,
 * track changes, album art, and title/artist updates across all music apps
 * on the GWM Tank 500 head unit.
 */
public class MediaNotificationListener extends NotificationListenerService {
    private static final String TAG = "MediaNotifListener";

    @Override
    public void onCreate() {
        super.onCreate();
        AppLogger.i(TAG, "MediaNotificationListener onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        AppLogger.i(TAG, "MediaNotificationListener onBind");
        return super.onBind(intent);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        AppLogger.s(TAG, "MediaNotificationListener успешно подключен к системе");
        MediaTracker.getInstance().onListenerConnected(this);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        AppLogger.w(TAG, "MediaNotificationListener отключен");
        MediaTracker.getInstance().onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        MediaTracker.getInstance().onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        MediaTracker.getInstance().onNotificationRemoved(sbn);
    }
}
