package com.reopentracks.publicapi;

import com.reopentracks.services.TrackRecordingService;

public class CreateMarker extends AbstractAPIActivity {

    @Override
    protected void execute(TrackRecordingService service) {
        service.createMarker();
    }

    @Override
    protected boolean isPostExecuteStopService() {
        return false;
    }
}
