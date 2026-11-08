package org.opengwm.split.vehicle;

public interface VehicleDataProvider {
    interface Callback {
        void onVehicleStateChanged(VehicleState state);
    }

    void startListening(Callback callback);
    void stopListening();
}
