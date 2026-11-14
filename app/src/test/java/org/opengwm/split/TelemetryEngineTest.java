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
}
