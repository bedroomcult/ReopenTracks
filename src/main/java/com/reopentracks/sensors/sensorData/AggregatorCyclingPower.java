package com.reopentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import com.reopentracks.data.models.Power;
import com.reopentracks.sensors.BluetoothHandlerManagerCyclingPower;

public class AggregatorCyclingPower extends Aggregator<BluetoothHandlerManagerCyclingPower.Data, Power> {

    public AggregatorCyclingPower(String name, String address) {
        super(name, address);
    }

    @Override
    public void computeValue(Raw<BluetoothHandlerManagerCyclingPower.Data> current) {
        this.aggregatedValue = current.value().power();
    }

    @Override
    protected void resetImmediate() {
        aggregatedValue = getNoneValue();
    }

    @Override
    public void resetAggregated() {
    }

    @NonNull
    @Override
    protected Power getNoneValue() {
        return Power.of(0f);
    }
}
