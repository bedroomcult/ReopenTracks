package com.reopentracks.chart;

import androidx.annotation.NonNull;

import com.reopentracks.data.models.Distance;
import com.reopentracks.data.models.Speed;
import com.reopentracks.data.models.TrackPoint;
import com.reopentracks.settings.UnitSystem;
import com.reopentracks.stats.TrackStatistics;

public record ChartPoint(
        //X-axis
        double timeOrDistance,

        //Y-axis
        Double altitude,
        Double speed,
        Double pace,
        Double heartRate,
        Double cadence,
        Double power
) {


    public static ChartPoint create(@NonNull TrackStatistics trackStatistics, @NonNull TrackPoint trackPoint, Speed smoothedSpeed, boolean chartByDistance, UnitSystem unitSystem) {
        return new ChartPoint(
                chartByDistance
                        ? trackStatistics.getTotalDistance().toKM_Miles(unitSystem)
                        : trackStatistics.getTotalTime().toMillis(),
                trackPoint.hasAltitude()
                        ? Distance.of(trackPoint.getAltitude().toM()).toM_FT(unitSystem)
                        : null,
                smoothedSpeed != null
                        ? smoothedSpeed.to(unitSystem)
                        : null,
                smoothedSpeed != null
                        ? smoothedSpeed.toPace(unitSystem).toSeconds() / 60d
                        : null,
                trackPoint.hasHeartRate()
                        ? (double) trackPoint.getHeartRate().getBPM()
                        : null,
                trackPoint.hasCadence()
                        ? (double) trackPoint.getCadence().getRPM()
                        : null,
                trackPoint.hasPower()
                        ? (double) trackPoint.getPower().getW()
                        : null
        );
    }
}
