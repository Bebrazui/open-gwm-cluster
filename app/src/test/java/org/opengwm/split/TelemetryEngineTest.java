package org.opengwm.split;

import com.altaysoft.t500_split.carData;
import com.altaysoft.t500_split.cfgData;
import org.junit.Before;
import org.junit.Test;
import org.opengwm.split.vehicle.VehicleState;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class TelemetryEngineTest {

    @Before
    public void setUp() {
        carData.getInstance().setListener(null);
    }

    @Test
    public void testCfgDataTank500Layout() {
        cfgData cfg = cfgData.getInstance();
        assertNotNull("cfgData instance should not be null", cfg);
        assertEquals("Vehicle profile must be Tank 500 (ID=5)", 5, cfg.L0);
        assertTrue("CAN stream flag j1 must be active", cfg.j1);
        assertEquals("Package identity must match com.altaysoft.t500_split", "com.altaysoft.t500_split", cfg.n);
    }

    @Test
    public void testCarDataLiveDataFields() {
        carData data = carData.getInstance();
        assertNotNull("Speed LiveData (T) must be non-null", data.T);
        assertNotNull("RPM LiveData (U) must be non-null", data.U);
        assertNotNull("Gear LiveData (Y) must be non-null", data.Y);
        assertNotNull("Coolant LiveData (F) must be non-null", data.F);
    }

    @Test
    public void testCarDataSettersAndDispatch() {
        carData data = carData.getInstance();
        final AtomicReference<VehicleState> captured = new AtomicReference<>();

        data.setListener(new carData.TelemetryListener() {
            @Override
            public void onTelemetryUpdate(VehicleState state) {
                captured.set(state);
            }
        });

        data.setSpeed(75.4f);
        data.setRpm(1850f);
        data.setCurrentGear(4); // D
        data.setCoolantT(88f);
        data.setBatteryVoltage(14.2f);
        data.setRemainFuelPerc(65f);
        data.setWheel_fl_p(2.5f);

        VehicleState st = captured.get();
        assertNotNull("Telemetry update must be dispatched", st);
        assertEquals("Speed should be rounded to 75", 75, st.speedKmh);
        assertEquals("RPM should be rounded to 1850", 1850, st.rpm);
        assertEquals("Gear 4 should format to 'D'", "D", st.gear);
        assertEquals("Coolant temperature should match", 88f, st.targetTempC, 0.01f);
        assertEquals("Battery voltage should match", 14.2f, st.batteryVoltage, 0.01f);
        assertEquals("Fuel percentage should match", 65, st.fuelPercent);
        assertEquals("TPMS pressure should match", 2.5f, st.tirePressureBar, 0.01f);
    }

    @Test
    public void testGearFormatting() {
        assertEquals("P", carData.formatGear(1));
        assertEquals("R", carData.formatGear(2));
        assertEquals("N", carData.formatGear(3));
        assertEquals("D", carData.formatGear(4));
        assertEquals("M1", carData.formatGear(5));
        assertEquals("M9", carData.formatGear(13));
        assertEquals("D", carData.formatGear(99)); // fallback
    }

    @Test
    public void testTank500LogcatSignalsViaGwmPushBinder() {
        carData data = carData.getInstance();
        final AtomicReference<VehicleState> captured = new AtomicReference<>();

        data.setListener(new carData.TelemetryListener() {
            @Override
            public void onTelemetryUpdate(VehicleState state) {
                captured.set(state);
            }
        });

        com.altaysoft.t500_split.GwmPushBinder binder = com.altaysoft.t500_split.GwmPushBinder.getInstance();

        // 1. Idle engine speed from logcat: 806 RPM
        binder.handlePush("car.basic.engine_speed", "806");
        assertNotNull(captured.get());
        assertEquals(806, captured.get().rpm);

        // 2. Battery voltage: 14.86524 V
        binder.handlePush("car.basic.battery_voltage", "14.86524");
        assertEquals(14.86524f, captured.get().batteryVoltage, 0.001f);

        // 3. Vehicle speed: 45.2 km/h
        binder.handlePush("car.basic.vehicle_speed", "45.2");
        assertEquals(45, captured.get().speedKmh);

        // 4. Gear transitions from real user dump (4 -> 2 -> 0 -> 3):
        binder.handlePush("car.basic.gear_status", "4");
        assertEquals("P", captured.get().gear);

        binder.handlePush("car.basic.gear_status", "2");
        assertEquals("R", captured.get().gear);

        binder.handlePush("car.basic.gear_status", "0");
        assertEquals("N", captured.get().gear);

        binder.handlePush("car.basic.gear_status", "3");
        assertEquals("D", captured.get().gear);

        // 5. Named gear (e.g. D4)
        binder.handlePush("car.basic.gear_status", "D4");
        assertEquals("D4", captured.get().gear);

        // 6. Pitch and Roll matrices: [[1,1]] and [[0,1]]
        binder.handlePush("car.basic.slope.status", "[[1,1]]");
        assertEquals(1.0f, ((Number) data.d1.getValue()).floatValue(), 0.01f);

        binder.handlePush("car.basic.title.status", "[[0,1]]");
        assertEquals(0.0f, ((Number) data.e1.getValue()).floatValue(), 0.01f);

        binder.handlePush("car.basic.compass.status", "[3]");
        assertEquals(3.0f, ((Number) data.f1.getValue()).floatValue(), 0.01f);
    }

    @Test
    public void testMediaTrackerTimeFormattingAndDemo() {
        assertEquals("00:00", org.opengwm.split.media.MediaTracker.formatTime(0));
        assertEquals("01:05", org.opengwm.split.media.MediaTracker.formatTime(65000));
        assertEquals("03:35", org.opengwm.split.media.MediaTracker.formatTime(215000));

        org.opengwm.split.media.MediaTracker tracker = org.opengwm.split.media.MediaTracker.getInstance();
        assertNotNull("MediaTracker must be non-null", tracker);

        tracker.loadDemoTrack();
        assertEquals("Рок-н-Ролла нет", tracker.getTrackTitle());
        assertEquals("Миша Бурый", tracker.getTrackArtist());
        assertTrue("Demo track must be playing", tracker.isPlaying());
        assertEquals(215000, tracker.getDurationMs());
        assertTrue("Progress must be between 0 and 1", tracker.getProgress() > 0f && tracker.getProgress() <= 1f);
    }
}
