package org.opengwm.split.vehicle;

public class VehicleState {
    public final int speedKmh;
    public final String gear;
    public final int rpm;
    public final boolean isClimateOn;
    public final float targetTempC;

    public VehicleState(int speedKmh, String gear, int rpm, boolean isClimateOn, float targetTempC) {
        this.speedKmh = speedKmh;
        this.gear = gear;
        this.rpm = rpm;
        this.isClimateOn = isClimateOn;
        this.targetTempC = targetTempC;
    }
}
