package com.reopentracks.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;

import com.reopentracks.data.models.Distance;
import com.reopentracks.data.models.Position;
import com.reopentracks.data.models.TrackPoint;

public class TrackPointTest {

    @Test
    public void distanceToPrevious() {
        TrackPoint tp1 = new TrackPoint(TrackPoint.Type.TRACKPOINT,
                new Position(
                        Instant.ofEpochMilli(0),
                        0d,
                        0.0001,
                        null,
                        null,
                        null,
                        null,
                        null));

        TrackPoint tp2 = new TrackPoint(TrackPoint.Type.TRACKPOINT,
                new Position(
                        Instant.ofEpochMilli(1),
                        0d,
                        0.0002,
                        null,
                        null,
                        null,
                        null,
                        null));

        // without sensor distance
        assertEquals(11.13, tp2.distanceToPrevious(tp1).toM(), 0.01);

        // tp1 has sensor distance
        tp1.setSensorDistance(Distance.of(5));
        tp1.setSensorDistance(null);
        assertEquals(11.13, tp2.distanceToPrevious(tp1).toM(), 0.01);

        // tp2 has sensor distance
        tp1.setSensorDistance(null);
        tp2.setSensorDistance(Distance.of(5));
        assertEquals(5, tp2.distanceToPrevious(tp1).toM(), 0.01);

        // tp1 and tp2 have sensor distance
        tp1.setSensorDistance(Distance.of(10));
        tp2.setSensorDistance(Distance.of(5));
        assertEquals(5, tp2.distanceToPrevious(tp1).toM(), 0.01);
    }
}