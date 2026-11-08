package org.opengwm.split.vehicle;

import android.os.Handler;
import android.os.Looper;

public class MockVehicleProvider implements VehicleDataProvider {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Callback callback;
    private boolean running = false;
    private int speed = 0;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            speed = (speed + 2) % 130;
            String gear;
            if (speed == 0) gear = "P";
            else if (speed < 20) gear = "D1";
            else if (speed < 40) gear = "D2";
            else if (speed < 60) gear = "D3";
            else gear = "D4";

            if (callback != null) {
                callback.onVehicleStateChanged(new VehicleState(speed, gear, 800 + speed * 25, true, 21.5f));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void startListening(Callback callback) {
        this.callback = callback;
        this.running = true;
        handler.post(ticker);
    }

    @Override
    public void stopListening() {
        this.running = false;
        handler.removeCallbacks(ticker);
        this.callback = null;
    }
}
