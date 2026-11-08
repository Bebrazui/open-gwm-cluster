package org.opengwm.split.vehicle;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Real vehicle telemetry provider for GWM (Tank 300 / 500, Haval).
 * Scans and reads CAN bus packets from Linux kernel device `/dev/can0`
 * or falls back to Mock if hardware access is restricted.
 */
public class CanBusVehicleProvider implements VehicleDataProvider {

    private static final String TAG = "CanBusVehicleProvider";
    private static final String CAN_DEVICE_PATH = "/dev/can0";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Callback callback;
    private volatile boolean isRunning = false;
    private Thread readerThread;

    // Last known state
    private int currentSpeed = 0;
    private int currentRpm = 0;
    private String currentGear = "P";
    private float currentTemp = 90.0f;

    @Override
    public void startListening(Callback callback) {
        this.callback = callback;
        this.isRunning = true;

        readerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                readCanLoop();
            }
        }, "CAN-Bus-Reader");
        readerThread.start();
    }

    private void readCanLoop() {
        File canDevice = new File(CAN_DEVICE_PATH);

        if (!canDevice.exists() || !canDevice.canRead()) {
            Log.w(TAG, "CAN device " + CAN_DEVICE_PATH + " is not directly accessible (exists="
                    + canDevice.exists() + ", canRead=" + canDevice.canRead() + "). Fallback mode active.");
            runFallbackLoop();
            return;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(canDevice);
            byte[] buffer = new byte[16]; // Standard Linux CAN frame buffer (4 bytes ID, 1 byte DLC, 3 pad, 8 data)

            while (isRunning) {
                int read = fis.read(buffer);
                if (read >= 16) {
                    parseCanFrame(buffer);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading from " + CAN_DEVICE_PATH + ", switching to diagnostic fallback", e);
            runFallbackLoop();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Parses standard GWM High-Speed CAN matrix frames.
     */
    private void parseCanFrame(byte[] frame) {
        // CAN ID (little-endian 32-bit uint)
        int canId = (frame[0] & 0xFF) |
                ((frame[1] & 0xFF) << 8) |
                ((frame[2] & 0xFF) << 16) |
                ((frame[3] & 0xFF) << 24);

        int dlc = frame[4] & 0x0F;
        if (dlc < 8) return;

        int b0 = frame[8] & 0xFF;
        int b1 = frame[9] & 0xFF;
        int b2 = frame[10] & 0xFF;
        int b3 = frame[11] & 0xFF;

        boolean updated = false;

        // GWM Engine RPM (Standard frame 0x1F0 or 0x0C0: 16-bit RPM, factor 0.25)
        if (canId == 0x1F0 || canId == 0x0C0) {
            int rawRpm = (b0 << 8) | b1;
            currentRpm = (int) (rawRpm * 0.25f);
            updated = true;
        }
        // GWM Wheel / Vehicle Speed (Frame 0x260 or 0x210: factor 0.05625 km/h)
        else if (canId == 0x260 || canId == 0x210) {
            int rawSpeed = (b1 << 8) | b2;
            currentSpeed = (int) (rawSpeed * 0.05625f);
            updated = true;
        }
        // GWM Transmission Gear (Frame 0x1B0)
        else if (canId == 0x1B0) {
            int gearVal = b0 & 0x0F;
            switch (gearVal) {
                case 1: currentGear = "P"; break;
                case 2: currentGear = "R"; break;
                case 3: currentGear = "N"; break;
                case 4: currentGear = "D"; break;
                case 5: currentGear = "M1"; break;
                case 6: currentGear = "M2"; break;
                default: currentGear = "D"; break;
            }
            updated = true;
        }

        if (updated) {
            dispatchState();
        }
    }

    private void runFallbackLoop() {
        // Fallback generator when running in emulator or restricted userspace
        int simSpeed = 0;
        while (isRunning) {
            simSpeed = (simSpeed + 1) % 180;
            currentSpeed = simSpeed;
            currentRpm = 800 + simSpeed * 35;
            currentGear = (simSpeed == 0) ? "P" : "D";
            dispatchState();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void dispatchState() {
        final VehicleState state = new VehicleState(currentSpeed, currentGear, currentRpm, true, currentTemp);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onVehicleStateChanged(state);
                }
            }
        });
    }

    @Override
    public void stopListening() {
        isRunning = false;
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
        callback = null;
    }
}
