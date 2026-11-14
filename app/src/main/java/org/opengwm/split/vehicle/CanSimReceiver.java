package org.opengwm.split.vehicle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.opengwm.split.util.AppLogger;

/**
 * ADB Broadcast Receiver for CAN Simulation commands ("Способ А").
 * Allows real-time remote injection of CAN packets, stress floods, and faults.
 *
 * Examples:
 *   am broadcast -a org.opengwm.split.action.CAN_SIM --es cmd drive
 *   am broadcast -a org.opengwm.split.action.CAN_SIM --es cmd fault
 *   am broadcast -a org.opengwm.split.action.CAN_SIM --es cmd stress
 *   am broadcast -a org.opengwm.split.action.CAN_SIM --es cmd stop
 *   am broadcast -a org.opengwm.split.action.CAN_SIM --ef speed 140 --ef rpm 4200 --es gear D7
 */
public class CanSimReceiver extends BroadcastReceiver {
    private static final String TAG = "CanSimReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!"org.opengwm.split.action.CAN_SIM".equals(action)) return;

        CanBusSimulator sim = CanBusSimulator.getInstance();
        String cmd = intent.getStringExtra("cmd");
        if (cmd == null) {
            cmd = intent.getStringExtra("command");
        }

        if (cmd != null) {
            cmd = cmd.trim().toLowerCase();
            switch (cmd) {
                case "drive":
                case "start":
                    sim.startDriveMode();
                    AppLogger.i(TAG, "Команда Broadcast: запуск симуляции поездки 50 Гц");
                    return;
                case "fault":
                case "anomaly":
                    sim.injectFault();
                    AppLogger.i(TAG, "Команда Broadcast: запуск теста сбоев");
                    return;
                case "stress":
                case "flood":
                    sim.startStressMode();
                    AppLogger.i(TAG, "Команда Broadcast: запуск стресс-теста 200 Гц");
                    return;
                case "stop":
                    sim.stop();
                    AppLogger.i(TAG, "Команда Broadcast: остановка симуляции");
                    return;
                default:
                    break;
            }
        }

        // Manual injection if speed/rpm/gear extras are supplied
        if (intent.hasExtra("speed") || intent.hasExtra("rpm") || intent.hasExtra("gear")) {
            float speed = intent.getFloatExtra("speed", 0f);
            float rpm = intent.getFloatExtra("rpm", 800f);
            String gear = intent.getStringExtra("gear");
            if (gear == null) gear = "D";
            float coolant = intent.getFloatExtra("coolant", 90f);
            float fuel = intent.getFloatExtra("fuel", 75f);
            float batt = intent.getFloatExtra("batt", 14.1f);

            sim.injectManual(speed, rpm, gear, coolant, fuel, batt);
            AppLogger.i(TAG, "Команда Broadcast: ручной ввод speed=" + speed + ", rpm=" + rpm + ", gear=" + gear);
        }
    }
}
