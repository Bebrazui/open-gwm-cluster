package org.opengwm.split.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Automatically starts the InstrumentClusterService when the car's head unit powers up.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "OpenGwmBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        Log.i(TAG, "Received boot/power broadcast: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
                "com.gwm.intent.action.ACC_ON".equals(action)) {

            Intent serviceIntent = new Intent(context, InstrumentClusterService.class);
            serviceIntent.setAction(InstrumentClusterService.ACTION_START);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
