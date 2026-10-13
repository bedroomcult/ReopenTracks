package com.reopentracks.sensors.sensorData;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.List;

import com.reopentracks.sensors.SensorManager;
import com.reopentracks.sensors.ServiceMeasurementUUID;

public interface SensorHandlerInterface {

    List<ServiceMeasurementUUID> getServices();

    Aggregator<?, ?> createEmptySensorData(String address, String name);

    void handlePayload(SensorManager.SensorDataChangedObserver observer, ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic);
}
