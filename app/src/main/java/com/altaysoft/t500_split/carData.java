package com.altaysoft.t500_split;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import org.opengwm.split.util.AppLogger;
import org.opengwm.split.vehicle.VehicleState;

/**
 * Split telemetry data store with exact MutableLiveData fields matching classes.dex.
 */
public class carData {
    private static final String TAG = "carData";
    private static volatile carData sInstance;

    public interface TelemetryListener {
        void onTelemetryUpdate(VehicleState state);
    }

    private final java.util.concurrent.CopyOnWriteArrayList<TelemetryListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addListener(TelemetryListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(TelemetryListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void setListener(TelemetryListener listener) {
        listeners.clear();
        if (listener != null) {
            listeners.add(listener);
        }
    }

    // High-level decoded vehicle properties for UI
    public float speed = 0f;
    public float rpm = 0f;
    public int currentGear = 1; // P
    public String customGearStr = null;
    public float coolantTemp = 90f;
    public float oilTemp = 90f;
    public float oilPress = 300f;
    public float remainFuelPerc = 50f;
    public float remainOdometer = 300f;
    public float batteryVoltage = 13.8f;
    public float batteryPowerLevel = 100f;
    public float odoTotal = 15000f;
    public float outsideTemp = 20f;
    public float insideTemp = 22f;
    public float wheel_fl_p = 2.4f;
    public float wheel_fr_p = 2.4f;
    public float wheel_rl_p = 2.4f;
    public float wheel_rr_p = 2.4f;
    public int driveMode = 0;
    public float steerAngle = 0f;

    // Direct Handler field
    public Handler a = Looper.getMainLooper() != null ? new Handler(Looper.getMainLooper()) : null;
    public int i = 0;

    // LiveData fields matching classes.dex layout:
    public final MutableLiveData<Object> A = new MutableLiveData<>();
    public final MutableLiveData<Object> A0 = new MutableLiveData<>();
    public final MutableLiveData<Object> B = new MutableLiveData<>();
    public final MutableLiveData<Object> B0 = new MutableLiveData<>();
    public final MutableLiveData<Object> C = new MutableLiveData<>();
    public final MutableLiveData<Object> C0 = new MutableLiveData<>();
    public final MutableLiveData<Object> D = new MutableLiveData<>();
    public final MutableLiveData<Object> D0 = new MutableLiveData<>();
    public final MutableLiveData<Object> E = new MutableLiveData<>();
    public final MutableLiveData<Object> E0 = new MutableLiveData<>();
    public final MutableLiveData<Object> F = new MutableLiveData<>();
    public final MutableLiveData<Object> F0 = new MutableLiveData<>();
    public final MutableLiveData<Object> G = new MutableLiveData<>();
    public final MutableLiveData<Object> G0 = new MutableLiveData<>();
    public final MutableLiveData<Object> H = new MutableLiveData<>();
    public final MutableLiveData<Object> H0 = new MutableLiveData<>();
    public final MutableLiveData<Object> I = new MutableLiveData<>();
    public final MutableLiveData<Object> I0 = new MutableLiveData<>();
    public final MutableLiveData<Object> J = new MutableLiveData<>();
    public final MutableLiveData<Object> J0 = new MutableLiveData<>();
    public final MutableLiveData<Object> K = new MutableLiveData<>();
    public final MutableLiveData<Object> K0 = new MutableLiveData<>();
    public final MutableLiveData<Object> L = new MutableLiveData<>();
    public final MutableLiveData<Object> L0 = new MutableLiveData<>();
    public final MutableLiveData<Object> M = new MutableLiveData<>();
    public final MutableLiveData<Object> M0 = new MutableLiveData<>();
    public final MutableLiveData<Object> N = new MutableLiveData<>();
    public final MutableLiveData<Object> N0 = new MutableLiveData<>();
    public final MutableLiveData<Object> O = new MutableLiveData<>();
    public final MutableLiveData<Object> O0 = new MutableLiveData<>();
    public final MutableLiveData<Object> P = new MutableLiveData<>();
    public final MutableLiveData<Object> P0 = new MutableLiveData<>();
    public final MutableLiveData<Object> Q = new MutableLiveData<>();
    public final MutableLiveData<Object> Q0 = new MutableLiveData<>();
    public final MutableLiveData<Object> R = new MutableLiveData<>();
    public final MutableLiveData<Object> R0 = new MutableLiveData<>();
    public final MutableLiveData<Object> S = new MutableLiveData<>();
    public final MutableLiveData<Object> S0 = new MutableLiveData<>();
    public final MutableLiveData<Object> T = new MutableLiveData<>();
    public final MutableLiveData<Object> T0 = new MutableLiveData<>();
    public final MutableLiveData<Object> U = new MutableLiveData<>();
    public final MutableLiveData<Object> U0 = new MutableLiveData<>();
    public final MutableLiveData<Object> V = new MutableLiveData<>();
    public final MutableLiveData<Object> V0 = new MutableLiveData<>();
    public final MutableLiveData<Object> W = new MutableLiveData<>();
    public final MutableLiveData<Object> W0 = new MutableLiveData<>();
    public final MutableLiveData<Object> X = new MutableLiveData<>();
    public final MutableLiveData<Object> X0 = new MutableLiveData<>();
    public final MutableLiveData<Object> Y = new MutableLiveData<>();
    public final MutableLiveData<Object> Y0 = new MutableLiveData<>();
    public final MutableLiveData<Object> Z = new MutableLiveData<>();
    public final MutableLiveData<Object> Z0 = new MutableLiveData<>();
    public final MutableLiveData<Object> a0 = new MutableLiveData<>();
    public final MutableLiveData<Object> a1 = new MutableLiveData<>();
    public final MutableLiveData<Object> b = new MutableLiveData<>();
    public final MutableLiveData<Object> b0 = new MutableLiveData<>();
    public final MutableLiveData<Object> b1 = new MutableLiveData<>();
    public final MutableLiveData<Object> c = new MutableLiveData<>();
    public final MutableLiveData<Object> c0 = new MutableLiveData<>();
    public final MutableLiveData<Object> c1 = new MutableLiveData<>();
    public final MutableLiveData<Object> d = new MutableLiveData<>();
    public final MutableLiveData<Object> d0 = new MutableLiveData<>();
    public final MutableLiveData<Object> d1 = new MutableLiveData<>();
    public final MutableLiveData<Object> e = new MutableLiveData<>();
    public final MutableLiveData<Object> e0 = new MutableLiveData<>();
    public final MutableLiveData<Object> e1 = new MutableLiveData<>();
    public final MutableLiveData<Object> f = new MutableLiveData<>();
    public final MutableLiveData<Object> f0 = new MutableLiveData<>();
    public final MutableLiveData<Object> f1 = new MutableLiveData<>();
    public final MutableLiveData<Object> g = new MutableLiveData<>();
    public final MutableLiveData<Object> g0 = new MutableLiveData<>();
    public final MutableLiveData<Object> g1 = new MutableLiveData<>();
    public final MutableLiveData<Object> h = new MutableLiveData<>();
    public final MutableLiveData<Object> h0 = new MutableLiveData<>();
    public final MutableLiveData<Object> i0 = new MutableLiveData<>();
    public final MutableLiveData<Object> j = new MutableLiveData<>();
    public final MutableLiveData<Object> j0 = new MutableLiveData<>();
    public final MutableLiveData<Object> k = new MutableLiveData<>();
    public final MutableLiveData<Object> k0 = new MutableLiveData<>();
    public final MutableLiveData<Object> l = new MutableLiveData<>();
    public final MutableLiveData<Object> l0 = new MutableLiveData<>();
    public final MutableLiveData<Object> m = new MutableLiveData<>();
    public final MutableLiveData<Object> m0 = new MutableLiveData<>();
    public final MutableLiveData<Object> n = new MutableLiveData<>();
    public final MutableLiveData<Object> n0 = new MutableLiveData<>();
    public final MutableLiveData<Object> o = new MutableLiveData<>();
    public final MutableLiveData<Object> o0 = new MutableLiveData<>();
    public final MutableLiveData<Object> p = new MutableLiveData<>();
    public final MutableLiveData<Object> p0 = new MutableLiveData<>();
    public final MutableLiveData<Object> q = new MutableLiveData<>();
    public final MutableLiveData<Object> q0 = new MutableLiveData<>();
    public final MutableLiveData<Object> r = new MutableLiveData<>();
    public final MutableLiveData<Object> s = new MutableLiveData<>();
    public final MutableLiveData<Object> s0 = new MutableLiveData<>();
    public final MutableLiveData<Object> t = new MutableLiveData<>();
    public final MutableLiveData<Object> t0 = new MutableLiveData<>();
    public final MutableLiveData<Object> u = new MutableLiveData<>();
    public final MutableLiveData<Object> u0 = new MutableLiveData<>();
    public final MutableLiveData<Object> v = new MutableLiveData<>();
    public final MutableLiveData<Object> v0 = new MutableLiveData<>();
    public final MutableLiveData<Object> w = new MutableLiveData<>();
    public final MutableLiveData<Object> w0 = new MutableLiveData<>();
    public final MutableLiveData<Object> x = new MutableLiveData<>();
    public final MutableLiveData<Object> x0 = new MutableLiveData<>();
    public final MutableLiveData<Object> y = new MutableLiveData<>();
    public final MutableLiveData<Object> y0 = new MutableLiveData<>();
    public final MutableLiveData<Object> z = new MutableLiveData<>();
    public final MutableLiveData<Object> z0 = new MutableLiveData<>();

    public static synchronized carData getInstance() {
        if (sInstance == null) {
            sInstance = new carData();
        }
        return sInstance;
    }

    public carData() {
        sInstance = this;
    }

    private long lastLogTime = 0;

    public void dispatchTelemetry() {
        if (!listeners.isEmpty()) {
            String gearStr = (this.customGearStr != null && !this.customGearStr.isEmpty()) ?
                    this.customGearStr : formatGear(this.currentGear);
            VehicleState st = new VehicleState(
                    Math.round(this.speed),
                    gearStr,
                    Math.round(this.rpm),
                    true,
                    this.coolantTemp,
                    "FDBus-Live",
                    -1f,
                    Math.round(this.remainFuelPerc),
                    Math.round(this.remainOdometer),
                    this.batteryVoltage > 0 ? this.batteryVoltage : (this.batteryPowerLevel > 0 ? this.batteryPowerLevel : 13.8f),
                    Math.round(this.odoTotal),
                    this.outsideTemp,
                    this.wheel_fl_p > 0 ? this.wheel_fl_p : -1f
            );
            for (TelemetryListener l : listeners) {
                try {
                    l.onTelemetryUpdate(st);
                } catch (Throwable ignored) {}
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastLogTime > 500) {
            lastLogTime = now;
            String gearDisplay = (this.customGearStr != null) ? this.customGearStr : formatGear(this.currentGear);
            AppLogger.i(TAG, "CAN Telemetry: speed=" + speed + " km/h, rpm=" + rpm + ", gear=" + gearDisplay + ", coolant=" + coolantTemp + "C, batt=" + batteryVoltage + "V");
        }
    }

    public void setCustomGear(String gear) {
        this.customGearStr = gear;
        dispatchTelemetry();
    }

    public static String formatGear(int g) {
        switch (g) {
            case 1: return "P";
            case 2: return "R";
            case 3: return "N";
            case 4: return "D";
            case 5: return "M1";
            case 6: return "M2";
            case 7: return "M3";
            case 8: return "M4";
            case 9: return "M5";
            case 10: return "M6";
            case 11: return "M7";
            case 12: return "M8";
            case 13: return "M9";
            case 21: return "D1";
            case 22: return "D2";
            case 23: return "D3";
            case 24: return "D4";
            case 25: return "D5";
            case 26: return "D6";
            case 27: return "D7";
            case 28: return "D8";
            case 29: return "D9";
            default: return "D";
        }
    }

    public void setAlt(float f2) {
        try {
this.O.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setAtTemp(int i) {
        try {
this.V.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setAvgFuelCons(float f2) {
        try {
this.l0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setAvgFuelConsJour(float f2) {
        try {
this.m0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setAvgSpeedAccum(float f2) {
        try {
this.t0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setAvgSpeedJour(float f2) {
        try {
this.s0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setBaro(float f2) {
        try {
this.P.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setBatteryPowerLevel(float f2) {
        try {
this.e0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.batteryPowerLevel = f2;
        dispatchTelemetry();
    }

    public void setBatteryVoltage(float f2) {
        try {
this.b0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.batteryVoltage = f2;
        dispatchTelemetry();
    }

    public void setBeltStatus(int i) {
        try {
this.k.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setBrake_status(boolean z2) {
        try {
this.m.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setCoolantT(float f2) {
        try {
this.F.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.coolantTemp = f2;
        dispatchTelemetry();
    }

    public void setCruiseDistance(int i) {
        try {
this.C0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setCruiseMode(int i) {
        try {
this.A0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setCruiseSpeed(int i) {
        try {
this.B0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setCurrentGear(int i) {
        try {
this.Y.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        this.currentGear = i;
        dispatchTelemetry();
    }

    public void setDayNightState(int i) {
        try {
this.J0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDebugTraceData1(String str) {
        try {
this.B.postValue(str);
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDebugTraceData2(String str) {
        try {
this.C.postValue(str);
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDebugTraceData3(String str) {
        try {
this.D.postValue(str);
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDebugTraceData4(String str) {
        try {
this.E.postValue(str);
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDebugTraceEnabled(boolean z2) {
        try {
this.A.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDiesel(boolean z2) {
        try {
this.z.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDoorStatus(int i) {
        try {
this.j.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDrHeading(float f2) {
        try {
this.f1.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDrPitch(float f2) {
        try {
this.d1.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDrRoll(float f2) {
        try {
this.e1.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDrStatus(int i) {
        try {
this.g1.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDriveMode(int i) {
        try {
this.D0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        this.driveMode = i;
        dispatchTelemetry();
    }

    public void setDriveTimeAccum(int i) {
        try {
this.v0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setDriveTimeJour(int i) {
        try {
this.u0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setGearStatus(int i) {
        try {
this.W.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvCurrent(float f2) {
        try {
this.d0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvVoltage(float f2) {
        try {
this.c0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacAc(int i) {
        try {
this.L0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacAuto(int i) {
        try {
this.M0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacBlow(int i) {
        try {
this.Q0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacDef(int i) {
        try {
this.R0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacFan(int i) {
        try {
this.P0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacHeatFl(int i) {
        try {
this.V0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacHeatFr(int i) {
        try {
this.W0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacHeatRl(int i) {
        try {
this.X0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacHeatRr(int i) {
        try {
this.Y0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacInlet(int i) {
        try {
this.N0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacPwr(int i) {
        try {
this.K0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacRear(int i) {
        try {
this.T0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacTemp(float f2) {
        try {
this.O0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacVentFl(int i) {
        try {
this.Z0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacVentFr(int i) {
        try {
this.a1.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacVentRl(int i) {
        try {
this.b1.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacVentRr(int i) {
        try {
this.c1.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacWheel(int i) {
        try {
this.U0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setHvacWin(int i) {
        try {
this.S0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setIPMode(int i) {
        try {
this.a0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setIPThemeID(int i) {
        try {
this.Z.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setInet(boolean z2) {
        try {
this.r.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setInsideTemp(float f2) {
        try {
this.S.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.insideTemp = f2;
        dispatchTelemetry();
    }

    public void setInstFuelCons(float f2) {
        try {
this.p0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setIpMessage(int i) {
        try {
MutableLiveData<Object> yVar = this.f;
        if (i == 0) {
            Integer num = (Integer) yVar.getValue();
            if (num == null || num.intValue() == 0) {
                return;
            }
            yVar.postValue(0);
            return;
        }
        Integer num2 = (Integer) yVar.getValue();
        if (num2 != null && num2.intValue() == i) {
            yVar.postValue(0);
        }
        yVar.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setIpMessageParam(int i) {
        try {
this.i = i;
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setIpNotice(int i) {
        try {
MutableLiveData<Object> yVar = this.g;
        if (i == 0) {
            Integer num = (Integer) yVar.getValue();
            if (num == null || num.intValue() == 0) {
                return;
            }
            yVar.postValue(0);
            return;
        }
        Integer num2 = (Integer) yVar.getValue();
        if (num2 != null && num2.intValue() == i) {
            yVar.postValue(0);
        }
        yVar.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setIpWellCar(int i) {
        try {
MutableLiveData<Object> yVar = this.h;
        Integer num = (Integer) yVar.getValue();
        if ((num != null ? num.intValue() : 0) == i) {
            return;
        }
        yVar.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setIp_navi(boolean z2) {
        try {
this.v.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setIp_navi_full(boolean z2) {
        try {
this.w.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setK0(String str) {
        try {
this.d.postValue(str);
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setKIv(String str) {
        try {
this.c.postValue(str);
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setKeyEvent(int i) {
        try {
this.X.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setLcError(boolean z2) {
        try {
this.l.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setMotoHours(int i) {
        try {
this.w0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setOdoAccumulated(float f2) {
        try {
this.z0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setOdoJourney(float f2) {
        try {
this.x0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setOdoTotal(float f2) {
        try {
this.y0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.odoTotal = f2;
        dispatchTelemetry();
    }

    public void setOilPress(float f2) {
        try {
this.n0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.oilPress = f2;
        dispatchTelemetry();
    }

    public void setOilTemp(float f2) {
        try {
this.o0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.oilTemp = f2;
        dispatchTelemetry();
    }

    public void setOutsideTemp(float f2) {
        try {
this.R.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.outsideTemp = f2;
        dispatchTelemetry();
    }

    public void setRemainFuelPerc(float f2) {
        try {
this.f0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.remainFuelPerc = f2;
        dispatchTelemetry();
    }

    public void setRemainFuelPerc1(float f2) {
        try {
this.h0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setRemainFuelPerc2(float f2) {
        try {
this.j0.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setRemainOdometer(float f2) {
        try {
this.g0.postValue(Float.valueOf(G(f2)));
        } catch (Throwable ignored) {}
        this.remainOdometer = f2;
        dispatchTelemetry();
    }

    public void setRemainOdometer1(float f2) {
        try {
this.i0.postValue(Float.valueOf(G(f2)));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setRemainOdometer2(float f2) {
        try {
this.k0.postValue(Float.valueOf(G(f2)));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setRpm(float f2) {
        try {
this.U.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.rpm = f2;
        dispatchTelemetry();
    }

    public void setShowBackButton(boolean z2) {
        try {
this.o.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setShowNaviButton(boolean z2) {
        try {
this.p.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setShow_gauge(boolean z2) {
        try {
this.x.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setSpeed(float f2) {
        try {
this.T.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.speed = f2;
        dispatchTelemetry();
    }

    public void setSpeed_right(boolean z2) {
        try {
this.y.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setStartTime(int i) {
        try {
this.b.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setSteerAngle(float f2) {
        try {
this.Q.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.steerAngle = f2;
        dispatchTelemetry();
    }

    public void setTraffic_leak(boolean z2) {
        try {
this.n.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setVolMedia(int i) {
        try {
this.E0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setVolNavi(int i) {
        try {
this.F0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setVolPhone(int i) {
        try {
this.G0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setVolRing(int i) {
        try {
this.I0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setVolVoice(int i) {
        try {
this.H0.postValue(Integer.valueOf(i));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setWheel_fl_p(float f2) {
        try {
this.G.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.wheel_fl_p = f2;
        dispatchTelemetry();
    }

    public void setWheel_fl_t(float f2) {
        try {
this.H.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setWheel_fr_p(float f2) {
        try {
this.I.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.wheel_fr_p = f2;
        dispatchTelemetry();
    }

    public void setWheel_fr_t(float f2) {
        try {
this.J.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setWheel_rl_p(float f2) {
        try {
this.K.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.wheel_rl_p = f2;
        dispatchTelemetry();
    }

    public void setWheel_rl_t(float f2) {
        try {
this.L.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setWheel_rr_p(float f2) {
        try {
this.M.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        this.wheel_rr_p = f2;
        dispatchTelemetry();
    }

    public void setWheel_rr_t(float f2) {
        try {
this.N.postValue(Float.valueOf(f2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    public void setWifi(boolean z2) {
        try {
this.s.postValue(Boolean.valueOf(z2));
        } catch (Throwable ignored) {}
        
        dispatchTelemetry();
    }

    // Getters and helper methods expected by Split native libraries
    public float G(float f2) { return f2; }
    public boolean getDebugTraceEnabled() { return false; }
    public boolean getDiesel() { return false; }
    public String getK0() { return ""; }
    public String getKIv() { return ""; }
    public int getStartTime() { return 0; }
    public boolean getWifi() { return true; }
    public float a() { return speed; }
    public float b() { return rpm; }
    public float c() { return coolantTemp; }
    public float d() { return outsideTemp; }
    public int e() { return currentGear; }
    public int f() { return Math.round(remainFuelPerc); }
    public int g() { return Math.round(remainOdometer); }
    public float h() { return batteryVoltage; }
}
