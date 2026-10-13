package com.reopentracks.viewmodels;

import android.view.LayoutInflater;

import com.reopentracks.R;
import com.reopentracks.databinding.StatsClockItemBinding;
import com.reopentracks.services.RecordingData;
import com.reopentracks.settings.UnitSystem;
import com.reopentracks.ui.customRecordingLayout.DataField;

public class ClockViewHolder extends StatisticViewHolder<StatsClockItemBinding> {

    @Override
    protected StatsClockItemBinding createViewBinding(LayoutInflater inflater) {
        return StatsClockItemBinding.inflate(inflater);
    }

    @Override
    public void configureUI(DataField dataField) {
        //TODO Unify with GenericStatisticsViewHolder?
        getBinding().statsClock.setTextAppearance(dataField.isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryValue : R.style.TextAppearance_OpenTracks_SecondaryValue);
    }

    @Override
    public void onChanged(UnitSystem unitSystem, RecordingData data) {
    }
}
