package com.reopentracks.publicapi;

import com.reopentracks.data.models.Track;
import com.reopentracks.services.RecordingData;
import com.reopentracks.services.TrackRecordingService;
import com.reopentracks.util.ExportUtils;

public class StopRecording extends AbstractAPIActivity {

    protected void execute(TrackRecordingService service) {
        RecordingData recordingData = service.getRecordingDataObservable().getValue();
        Track.Id trackId = null;
        if (recordingData != null && recordingData.track() != null) {
            trackId = recordingData.track().getId();
        }

        service.endCurrentTrack();

        if (trackId != null) {
            ExportUtils.postWorkoutExport(this, trackId);
        }
    }

    @Override
    protected boolean isPostExecuteStopService() {
        return true;
    }
}
