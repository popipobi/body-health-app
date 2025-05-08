package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.database.model.Measurement;

import java.util.List;

public class MeasurementHistoryAdapter extends RecyclerView.Adapter<MeasurementHistoryAdapter.ViewHolder> {
    private List<Measurement> measurements;

    public MeasurementHistoryAdapter(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_measurement_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Measurement measurement = measurements.get(position);

        holder.tvDate.setText(measurement.getFormattedDate());
        holder.tvSystolic.setText(String.valueOf(measurement.getSystolic()));
        holder.tvDiastolic.setText(String.valueOf(measurement.getDiastolic()));
        holder.tvPulse.setText(String.valueOf(measurement.getPulse()));
    }

    @Override
    public int getItemCount() {
        return measurements.size();
    }

    public void updateData(List<Measurement> newMeasurements) {
        this.measurements = newMeasurements;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvSystolic;
        TextView tvDiastolic;
        TextView tvPulse;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_measurement_date);
            tvSystolic = itemView.findViewById(R.id.tv_systolic_value);
            tvDiastolic = itemView.findViewById(R.id.tv_diastolic_value);
            tvPulse = itemView.findViewById(R.id.tv_pulse_value);
        }
    }
}
