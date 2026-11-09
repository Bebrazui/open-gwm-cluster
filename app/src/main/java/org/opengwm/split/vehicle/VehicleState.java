package org.opengwm.split.vehicle;

public class VehicleState {
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
        this(speedKmh, gear, rpm, isClimateOn, targetTempC, "OK", 62.5f, 75, 540, 14.1f, 18450, 18f, 2.3f);
    }

    public VehicleState(int speedKmh, String gear, int rpm, boolean isClimateOn, float targetTempC, String diagnosticInfo) {
        this(speedKmh, gear, rpm, isClimateOn, targetTempC, diagnosticInfo, 62.5f, 75, 540, 14.1f, 18450, 18f, 2.3f);
    }

    public VehicleState(int speedKmh, String gear, int rpm, boolean isClimateOn, float targetTempC, String diagnosticInfo,
                        float fuelLiters, int fuelPercent, int rangeKm, float batteryVoltage, int odometerKm,
                        float outsideTempC, float tirePressureBar) {
        this.speedKmh = speedKmh;
        this.gear = gear;
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
                return Math.round(fuelLiters) + "L (" + fuelPercent + "%)";
            case 2: // Range
                return rangeKm + " KM";
            case 3: // Battery
                return String.format(java.util.Locale.US, "%.1f V", batteryVoltage);
            case 4: // Odometer
                return odometerKm + " KM";
            case 5: // Coolant
                return Math.round(targetTempC) + " °C";
            case 6: // Outside Temp
                return (outsideTempC >= 0 ? "+" : "") + Math.round(outsideTempC) + " °C";
            case 7: // Tire Pressure
                return String.format(java.util.Locale.US, "%.1f BAR", tirePressureBar);
            default:
                return "";
        }
    }
}


