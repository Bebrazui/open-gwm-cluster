package org.opengwm.split.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class CarAccessibilityService extends AccessibilityService {

    public static volatile CarAccessibilityService instance = null;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Monitoring window state changes
    }

    @Override
    public void onInterrupt() {
        instance = null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    public static boolean performBack() {
        CarAccessibilityService svc = instance;
        return svc != null && svc.performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public static boolean performHome() {
        CarAccessibilityService svc = instance;
        return svc != null && svc.performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public static boolean toggleSplitScreen() {
        CarAccessibilityService svc = instance;
        return svc != null && svc.performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
    }
}
