package com.reopentracks.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.reopentracks.R;
import com.reopentracks.TrackListActivity;
import com.reopentracks.data.models.Distance;
import com.reopentracks.data.models.DistanceFormatter;
import com.reopentracks.data.models.SpeedFormatter;
import com.reopentracks.data.models.TrackPoint;
import com.reopentracks.settings.PreferencesUtils;
import com.reopentracks.settings.UnitSystem;
import com.reopentracks.stats.TrackStatistics;
import com.reopentracks.util.IntentUtils;

/**
 * Manages the content of the notification shown by {@link TrackRecordingService}.
 */
class TrackRecordingServiceNotificationManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    static final int NOTIFICATION_ID = 123;

    private static final String CHANNEL_ID = TrackRecordingServiceNotificationManager.class.getSimpleName();

    private final NotificationCompat.Builder notificationBuilder;

    private final NotificationManager notificationManager;

    private boolean previousLocationWasAccurate = true;

    private UnitSystem unitSystem = null;

    TrackRecordingServiceNotificationManager(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                notificationChannel.setAllowBubbles(true);
            }

            notificationManager.createNotificationChannel(notificationChannel);
        }

        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(context.getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_logo_color_24dp);
    }

    void stop() {
        cancelNotification();
    }

    @VisibleForTesting
    TrackRecordingServiceNotificationManager(NotificationManager notificationManager, NotificationCompat.Builder notificationBuilder) {
        this.notificationManager = notificationManager;
        this.notificationBuilder = notificationBuilder;
    }

    void updateContent(String content) {
        notificationBuilder.setSubText(content);
        updateNotification();
    }

    void updateTrackPoint(Context context, TrackStatistics trackStatistics, TrackPoint trackPoint, Distance recordingGpsAccuracy) {
        String formattedAccuracy = context.getString(R.string.value_none);

        DistanceFormatter formatter = DistanceFormatter.Builder()
                .setUnit(unitSystem)
                .build(context);
        // The decision if a trackpoint is not accurate enough should happen in the sensor subsystem.
        if (trackPoint.hasHorizontalAccuracy()) {
            formattedAccuracy = formatter.formatDistance(trackPoint.getHorizontalAccuracy());

            boolean currentLocationWasAccurate = trackPoint.getHorizontalAccuracy().lessThan(recordingGpsAccuracy);
            boolean shouldAlert = !currentLocationWasAccurate && previousLocationWasAccurate;
            notificationBuilder.setOnlyAlertOnce(!shouldAlert);
            previousLocationWasAccurate = currentLocationWasAccurate;
        }

        notificationBuilder.setContentTitle(context.getString(R.string.track_distance_notification, formatter.formatDistance(trackStatistics.getTotalDistance())));
        String formattedSpeed = SpeedFormatter.Builder().setUnit(unitSystem).setReportSpeedOrPace(true).build(context).formatSpeed(trackPoint.getSpeed());
        notificationBuilder.setContentText(context.getString(R.string.track_speed_notification, formattedSpeed));
        notificationBuilder.setSubText(context.getString(R.string.track_recording_notification_accuracy, formattedAccuracy));
        updateNotification();

        notificationBuilder.setOnlyAlertOnce(true);
    }

    Notification setGPSonlyStarted(Context context) {
        Intent intent = IntentUtils.newIntent(context, TrackListActivity.class);

        int pendingIntentFlags = 0;
        pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addParentStack(TrackListActivity.class)
                .addNextIntent(intent)
                .getPendingIntent(0, pendingIntentFlags);

        updateContent(context.getString(R.string.gps_starting));

        notificationBuilder.setContentIntent(pendingIntent);
        updateNotification();

        return getNotification();
    }

    void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    void setUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem;
    }

    private void updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, getNotification());
    }

    private Notification getNotification() {
        return notificationBuilder.build();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferencesUtils.isKey(R.string.stats_units_key, key)) {
            setUnitSystem(PreferencesUtils.getUnitSystem());
        }
    }
}
