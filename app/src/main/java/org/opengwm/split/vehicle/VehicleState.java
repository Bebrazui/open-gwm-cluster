package org.opengwm.split.vehicle;

public class VehicleState {
    public static final float NO_TEMP = -999f;
    public static final float NO_FLOAT = -1f;
    public static final int NO_INT = -1;

    public final int speedKmh;
    public final String gear;
    public final int rpm;
    public final boolean isClimateOn;
    public final float targetTempC;
    public final String diagnosticInfo;

    // Mini-widget telemetry fields
    public final float fuelLiters;
    public final int fuelPercent;
    public final int rangeKm;
    public final float batteryVoltage;
    public final int odometerKm;
    public final float outsideTempC;
    public final float tirePressureBar;

    public VehicleState(int speedKmh, String gear, int rpm, boolean isClimateOn, float targetTempC) {
        this(speedKmh, gear, rpm, isClimateOn, targetTempC, "OK", NO_FLOAT, NO_INT, NO_INT, NO_FLOAT, NO_INT, NO_TEMP, NO_FLOAT);
    }

    public VehicleState(int speedKmh, String gear, int rpm, boolean isClimateOn, float targetTempC, String diagnosticInfo) {
        this(speedKmh, gear, rpm, isClimateOn, targetTempC, diagnosticInfo, NO_FLOAT, NO_INT, NO_INT, NO_FLOAT, NO_INT, NO_TEMP, NO_FLOAT);
    }

    public VehicleState(int speedKmh, String gear, int rpm, boolean isClimateOn, float targetTempC, String diagnosticInfo,
                        float fuelLiters, int fuelPercent, int rangeKm, float batteryVoltage, int odometerKm,
                        float outsideTempC, float tirePressureBar) {
        this.speedKmh = speedKmh;
        this.gear = (gear != null && !gear.isEmpty()) ? gear : "--";
        this.rpm = rpm;
        this.isClimateOn = isClimateOn;
        this.targetTempC = targetTempC;
        this.diagnosticInfo = (diagnosticInfo != null) ? diagnosticInfo : "OK";
        this.fuelLiters = fuelLiters;
        this.fuelPercent = fuelPercent;
        this.rangeKm = rangeKm;
        this.batteryVoltage = batteryVoltage;
        this.odometerKm = odometerKm;
        this.outsideTempC = outsideTempC;
        this.tirePressureBar = tirePressureBar;
    }

    public String formatWidget(int widgetType) {
        switch (widgetType) {
            case 1: // Fuel
                if (fuelLiters < 0 && fuelPercent < 0) return "--";
                if (fuelPercent >= 0 && fuelLiters >= 0) return Math.round(fuelLiters) + "L (" + fuelPercent + "%)";
                if (fuelPercent >= 0) return fuelPercent + "%";
                return Math.round(fuelLiters) + "L";
            case 2: // Range
                return (rangeKm >= 0) ? rangeKm + " KM" : "-- KM";
            case 3: // Battery
                return (batteryVoltage > 0) ? String.format(java.util.Locale.US, "%.1f V", batteryVoltage) : "-- V";
            case 4: // Odometer
                return (odometerKm >= 0) ? odometerKm + " KM" : "-- KM";
            case 5: // Coolant
                return (targetTempC > -50f) ? Math.round(targetTempC) + " °C" : "-- °C";
            case 6: // Outside Temp
                return (outsideTempC > -50f) ? ((outsideTempC >= 0 ? "+" : "") + Math.round(outsideTempC) + " °C") : "-- °C";
            case 7: // Tire Pressure
                return (tirePressureBar > 0) ? String.format(java.util.Locale.US, "%.1f BAR", tirePressureBar) : "-- BAR";
            default:
                return "--";
        }
    }
}


