package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.database.model.BodyFatMeasurement;

import java.util.List;

public class BodyFatHistoryAdapter extends RecyclerView.Adapter<BodyFatHistoryAdapter.ViewHolder> {
    private List<BodyFatMeasurement> measurements;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(BodyFatMeasurement measurement);
    }

    public BodyFatHistoryAdapter(List<BodyFatMeasurement> measurements) {
        this.measurements = measurements;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_body_fat_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BodyFatMeasurement measurement = measurements.get(position);

        holder.tvDate.setText(measurement.getFormattedDate());
        holder.tvWeight.setText(String.format("%.1f kg", measurement.getWeight()));
        holder.tvBodyFatRate.setText(String.format("%.1f%%", measurement.getBodyFatRate()));
        holder.tvMuscleRate.setText(String.format("%.1f%%", measurement.getMuscleRate()));

        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onItemClick(measurement));
        }
    }

    @Override
    public int getItemCount() {
        return measurements != null ? measurements.size() : 0;
    }

    public void updateData(List<BodyFatMeasurement> newMeasurements) {
        this.measurements = newMeasurements;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvWeight;
        TextView tvBodyFatRate;
        TextView tvMuscleRate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_body_fat_date);
            tvWeight = itemView.findViewById(R.id.tv_weight_value_history);
            tvBodyFatRate = itemView.findViewById(R.id.tv_body_fat_rate_history);
            tvMuscleRate = itemView.findViewById(R.id.tv_muscle_rate_history);
        }
    }
}