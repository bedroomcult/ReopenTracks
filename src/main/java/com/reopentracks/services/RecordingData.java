package com.reopentracks.services;

import androidx.annotation.NonNull;

import com.reopentracks.data.models.Track;
import com.reopentracks.data.models.TrackPoint;
import com.reopentracks.sensors.sensorData.SensorDataSet;
import com.reopentracks.stats.TrackStatistics;

/**
 * {@link Track} and {@link TrackPoint} must be immutable (i.e., their content does not change).
 */
public record RecordingData(Track track, TrackPoint latestTrackPoint, SensorDataSet sensorDataSet) {

    public String getTrackCategory() {
        if (track == null) {
            return "";
        }
        return track.getActivityTypeLocalized();
    }

    @NonNull
    public TrackStatistics getTrackStatistics() {
        if (track == null) {
            return new TrackStatistics();
        }

        return track.getTrackStatistics();
    }
}
