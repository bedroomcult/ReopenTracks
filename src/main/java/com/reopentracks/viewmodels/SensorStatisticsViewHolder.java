package com.reopentracks.viewmodels;

import android.util.Pair;
import android.view.LayoutInflater;

import java.time.Instant;

import com.reopentracks.R;
import com.reopentracks.data.models.HeartRateZones;
import com.reopentracks.databinding.StatsSensorItemBinding;
import com.reopentracks.sensors.sensorData.SensorDataSet;
import com.reopentracks.services.RecordingData;
import com.reopentracks.settings.PreferencesUtils;
import com.reopentracks.settings.UnitSystem;
import com.reopentracks.ui.customRecordingLayout.DataField;
import com.reopentracks.util.StringUtils;

public abstract class SensorStatisticsViewHolder extends StatisticViewHolder<StatsSensorItemBinding> {

    @Override
    protected StatsSensorItemBinding createViewBinding(LayoutInflater inflater) {
        return StatsSensorItemBinding.inflate(inflater);
    }

    @Override
    public void configureUI(DataField dataField) {
        getBinding().statsValue.setTextAppearance(dataField.isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryValue : R.style.TextAppearance_OpenTracks_SecondaryValue);
        getBinding().statsDescriptionMain.setTextAppearance(dataField.isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryHeader : R.style.TextAppearance_OpenTracks_SecondaryHeader);
    }

    public static class SensorHeartRate extends SensorStatisticsViewHolder {

        @Override
        public void onChanged(UnitSystem unitSystem, RecordingData data) {
            SensorDataSet sensorDataSet = data.sensorDataSet();
            String sensorName = getContext().getString(R.string.value_unknown);

            Pair<String, String> valueAndUnit;
            if (sensorDataSet != null && sensorDataSet.getHeartRate() != null) {
                valueAndUnit = StringUtils.getHeartRateParts(getContext(), sensorDataSet.getHeartRate().first);
                sensorName = sensorDataSet.getHeartRate().second;
            } else {
                valueAndUnit = StringUtils.getHeartRateParts(getContext(), null);
            }

            //TODO Loads preference every time
            HeartRateZones zones = PreferencesUtils.getHeartRateZones();
            int textColor;
            if (sensorDataSet != null && sensorDataSet.getHeartRate() != null) {
                textColor = zones.getTextColorForZone(getContext(), sensorDataSet.getHeartRate().first);
            } else {
                textColor = zones.getTextColorForZone(getContext(), null);
            }

            getBinding().statsValue.setText(valueAndUnit.first);
            getBinding().statsUnit.setText(valueAndUnit.second);
            getBinding().statsDescriptionMain.setText(R.string.stats_sensors_heart_rate);

            getBinding().statsDescriptionSecondary.setText(sensorName);

            getBinding().statsValue.setTextColor(textColor);
        }
    }

    public static class SensorCadence extends SensorStatisticsViewHolder {

        @Override
        public void onChanged(UnitSystem unitSystem, RecordingData data) {
            SensorDataSet sensorDataSet = data.sensorDataSet();
            String sensorName = getContext().getString(R.string.value_unknown);

            Pair<String, String> valueAndUnit;
            if (sensorDataSet != null && sensorDataSet.getCadence() != null) {
                valueAndUnit = StringUtils.getCadenceParts(getContext(), sensorDataSet.getCadence().first);
                sensorName = sensorDataSet.getCadence().second;
            } else {
                valueAndUnit = StringUtils.getCadenceParts(getContext(), null);
            }

            getBinding().statsValue.setText(valueAndUnit.first);
            getBinding().statsUnit.setText(valueAndUnit.second);
            getBinding().statsDescriptionMain.setText(R.string.stats_sensors_cadence);

            getBinding().statsDescriptionSecondary.setText(sensorName);
        }
    }

    public static class SensorPower extends SensorStatisticsViewHolder {

        @Override
        public void onChanged(UnitSystem unitSystem, RecordingData data) {
            SensorDataSet sensorDataSet = data.sensorDataSet();
            String sensorName = getContext().getString(R.string.value_unknown);

            Pair<String, String> valueAndUnit;
            if (sensorDataSet != null && sensorDataSet.getCyclingPower() != null) {
                valueAndUnit = StringUtils.getPowerParts(getContext(), sensorDataSet.getCyclingPower().getAggregatedValue(Instant.now())); //TODO Use MonotonicClock
                sensorName = sensorDataSet.getCyclingPower().getSensorNameOrAddress();
            } else {
                valueAndUnit = StringUtils.getCadenceParts(getContext(), null);
            }

            getBinding().statsValue.setText(valueAndUnit.first);
            getBinding().statsUnit.setText(valueAndUnit.second);
            getBinding().statsDescriptionMain.setText(R.string.stats_sensors_power);

            getBinding().statsDescriptionSecondary.setText(sensorName);
        }
    }
}
