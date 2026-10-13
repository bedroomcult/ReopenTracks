package com.reopentracks.sensors.sensorData;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.reopentracks.data.models.AtmosphericPressure;
import com.reopentracks.data.models.Cadence;
import com.reopentracks.data.models.HeartRate;
import com.reopentracks.data.models.Position;
import com.reopentracks.data.models.Speed;
import com.reopentracks.data.models.TrackPoint;
import com.reopentracks.sensors.BluetoothHandlerCyclingCadence;
import com.reopentracks.sensors.BluetoothHandlerCyclingDistanceSpeed;
import com.reopentracks.sensors.BluetoothHandlerManagerCyclingPower;
import com.reopentracks.sensors.BluetoothHandlerRunningSpeedAndCadence;
import com.reopentracks.services.handlers.TrackPointCreator;
import com.reopentracks.settings.PreferencesUtils;

public class SensorDataSet {

    private static final String TAG = SensorDataSet.class.getSimpleName();

    @VisibleForTesting
    public AggregatorHeartRate heartRate;

    @VisibleForTesting
    public AggregatorCyclingCadence cyclingCadence;

    @VisibleForTesting
    public AggregatorCyclingDistanceSpeed cyclingDistanceSpeed;

    @VisibleForTesting
    public AggregatorCyclingPower cyclingPower;

    @VisibleForTesting
    public AggregatorRunning runningDistanceSpeedCadence;

    @VisibleForTesting
    public AggregatorBarometer barometer;

    public AggregatorGPS gps;

    private final TrackPointCreator trackPointCreator;

    public SensorDataSet(TrackPointCreator trackPointCreator) {
        this.trackPointCreator = trackPointCreator;
    }

    @Deprecated //TODO This is not a copy constructor anymore, but it should be - aggregators are no value objects; best guess: can be removed
    public SensorDataSet(SensorDataSet toCopy) {
        this.trackPointCreator = toCopy.trackPointCreator;
        this.heartRate = toCopy.heartRate;
        this.cyclingCadence = toCopy.cyclingCadence;
        this.cyclingDistanceSpeed = toCopy.cyclingDistanceSpeed;
        this.cyclingPower = toCopy.cyclingPower;
        this.runningDistanceSpeedCadence = toCopy.runningDistanceSpeedCadence;
        this.barometer = toCopy.barometer;
        this.gps = toCopy.gps;
    }

    public Pair<HeartRate, String> getHeartRate() {
        if (heartRate != null) {
            return heartRate.getAggregatedValueWithSensorName(trackPointCreator.createNow());
        }

        return null;
    }

    public Pair<Cadence, String> getCadence() {
        if (cyclingCadence != null) {
            return cyclingCadence.getAggregatedValueWithSensorName(trackPointCreator.createNow());
        }

        if (runningDistanceSpeedCadence != null && runningDistanceSpeedCadence.hasReceivedData() && runningDistanceSpeedCadence.aggregatedValue.cadence() != null) {
            return new Pair<>(runningDistanceSpeedCadence.aggregatedValue.cadence(), runningDistanceSpeedCadence.getSensorNameOrAddress());
        }

        return null;
    }

    public Pair<Speed, String> getSpeed() {
        if (cyclingDistanceSpeed != null && cyclingDistanceSpeed.hasReceivedData() && cyclingDistanceSpeed.getAggregatedValue(trackPointCreator.createNow()).speed() != null) {
            return new Pair<>(cyclingDistanceSpeed.getAggregatedValue(trackPointCreator.createNow()).speed(), cyclingDistanceSpeed.getSensorNameOrAddress());
        }

        if (runningDistanceSpeedCadence != null && runningDistanceSpeedCadence.hasReceivedData() && runningDistanceSpeedCadence.getAggregatedValue(trackPointCreator.createNow()).speed() != null) {
            return new Pair<>(runningDistanceSpeedCadence.aggregatedValue.speed(), runningDistanceSpeedCadence.getSensorNameOrAddress());
        }

        return null;
    }

    public AggregatorCyclingPower getCyclingPower() {
        return cyclingPower;
    }

    public void add(@NonNull Aggregator<?, ?> data) {
        set(data, data);
    }

    public void update(@NonNull Raw<?> data) {
        Object value = data.value();

        if (value instanceof HeartRate) {
            this.heartRate.add((Raw<HeartRate>) data);
            return;
        }

        if (value instanceof BluetoothHandlerCyclingCadence.CrankData) {
            this.cyclingCadence.add((Raw<BluetoothHandlerCyclingCadence.CrankData>) data);
            return;
        }
        if (value instanceof BluetoothHandlerCyclingDistanceSpeed.WheelData ) {
            this.cyclingDistanceSpeed.setWheelCircumference(PreferencesUtils.getWheelCircumference()); //TODO Fetch once and then listen for changes.
            this.cyclingDistanceSpeed.add((Raw<BluetoothHandlerCyclingDistanceSpeed.WheelData>) data);
            return;
        }
        if (value instanceof BluetoothHandlerRunningSpeedAndCadence.Data) {
            this.runningDistanceSpeedCadence.add((Raw<BluetoothHandlerRunningSpeedAndCadence.Data>) data);

            return;
        }
        if (value instanceof BluetoothHandlerManagerCyclingPower.Data) {
            this.cyclingPower.add((Raw<BluetoothHandlerManagerCyclingPower.Data>) data);
            return;
        }
        if (value instanceof AtmosphericPressure) {
            this.barometer.add((Raw<AtmosphericPressure>) data);
            return;
        }
        if (value instanceof Position) {
            this.gps.add((Raw<Position>) data);
            return;
        }

        throw new UnsupportedOperationException(data.getClass().getCanonicalName() + " " + data.value().getClass().getCanonicalName());
    }

    public void remove(@NonNull Aggregator<?, ?> type) {
        set(type, null);
    }

    public void clear() {
        Log.i(TAG, "Removing all aggregators");
        this.heartRate = null;
        this.cyclingCadence = null;
        this.cyclingDistanceSpeed = null;
        this.cyclingPower = null;
        this.runningDistanceSpeedCadence = null;
        this.barometer = null;
        this.gps = null;
    }

    public void fillTrackPoint(TrackPoint trackPoint) {
        if (gps != null && gps.hasReceivedData()) {
            trackPoint.setPosition(gps.getAggregatedValue(trackPointCreator.createNow()));
        }

        if (getHeartRate() != null) {
            trackPoint.setHeartRate(getHeartRate().first);
        }

        if (getCadence() != null) {
            trackPoint.setCadence(getCadence().first);
        }

        if (getSpeed() != null) {
            trackPoint.setSpeed(getSpeed().first);
        }

        if (cyclingDistanceSpeed != null && cyclingDistanceSpeed.hasReceivedData()) {
            trackPoint.setSensorDistance(cyclingDistanceSpeed.getAggregatedValue(trackPointCreator.createNow()).distanceOverall());
        }

        if (cyclingPower != null && cyclingPower.hasReceivedData()) {
            trackPoint.setPower(cyclingPower.getAggregatedValue(trackPointCreator.createNow()));
        }

        if (runningDistanceSpeedCadence != null && runningDistanceSpeedCadence.hasReceivedData()) {
            trackPoint.setSensorDistance(runningDistanceSpeedCadence.getAggregatedValue(trackPointCreator.createNow()).distance());
        }

        if (barometer != null && barometer.hasReceivedData()) {
            trackPoint.setAltitudeGain(barometer.getAggregatedValue(trackPointCreator.createNow()).gain_m());
            trackPoint.setAltitudeLoss(barometer.getAggregatedValue(trackPointCreator.createNow()).loss_m());
        }
    }

    public void reset() {
        Log.i(TAG, "Resetting data");

        if (heartRate != null) heartRate.resetAggregated();
        if (cyclingCadence != null) cyclingCadence.resetAggregated();
        if (cyclingDistanceSpeed != null) cyclingDistanceSpeed.resetAggregated();
        if (cyclingPower != null) cyclingPower.resetAggregated();
        if (runningDistanceSpeedCadence != null) runningDistanceSpeedCadence.resetAggregated();
        if (barometer != null) barometer.resetAggregated();
        if (gps != null) gps.resetAggregated();
    }

    private void set(@NonNull Aggregator<?, ?> type, @Nullable Aggregator<?, ?> sensorData) {
        Log.i(TAG, "Setting aggregator " + type.getClass().getCanonicalName() + " to " + sensorData);

        if (type instanceof AggregatorHeartRate) {
            heartRate = (AggregatorHeartRate) sensorData;
            return;
        }
        if (type instanceof AggregatorCyclingCadence) {
            cyclingCadence = (AggregatorCyclingCadence) sensorData;
            return;
        }
        if (type instanceof AggregatorCyclingDistanceSpeed) {
            cyclingDistanceSpeed = (AggregatorCyclingDistanceSpeed) sensorData;
            return;
        }
        if (type instanceof AggregatorCyclingPower) {
            cyclingPower = (AggregatorCyclingPower) sensorData;
            return;
        }
        if (type instanceof AggregatorRunning) {
            runningDistanceSpeedCadence = (AggregatorRunning) sensorData;
            return;
        }
        if (type instanceof AggregatorBarometer) {
            barometer = (AggregatorBarometer) sensorData;
            return;
        }
        if (type instanceof AggregatorGPS) {
            gps = (AggregatorGPS) sensorData;
            return;
        }

        throw new UnsupportedOperationException(type.getClass().getCanonicalName());
    }
}
