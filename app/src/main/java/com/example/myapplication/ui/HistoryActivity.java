package com.example.myapplication.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.BottomNavigationHelper;
import com.example.myapplication.R;
import com.example.myapplication.adapter.MeasurementHistoryAdapter;
import com.example.myapplication.database.dao.MeasurementDAO;
import com.example.myapplication.database.model.Measurement;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView rvMeasurementHistory;
    private TextView tvEmptyState;
    private MeasurementDAO measurementDAO;
    private MeasurementHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        // 初始化视图
        rvMeasurementHistory = findViewById(R.id.rv_measurement_history);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        // 底部设置导航
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        BottomNavigationHelper.setupBottomNavigation(this, bottomNavigationView, R.id.navigation_history);

        // 初始化DAO
        measurementDAO = new MeasurementDAO(this);

        // 社会RecyclerView
        rvMeasurementHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MeasurementHistoryAdapter(null);
        rvMeasurementHistory.setAdapter(adapter);

        // 加载数据
        loadMeasurementHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次页面恢复就刷新数据
        loadMeasurementHistory();
    }

    private void loadMeasurementHistory() {
        SharedPreferences preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        int userId = preferences.getInt("user_id", -1);

        if (userId != -1) {
            List<Measurement> measurements = measurementDAO.getUserMeasurements(userId);

            if (measurements.isEmpty()) {
                rvMeasurementHistory.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                rvMeasurementHistory.setVisibility(View.VISIBLE);
                tvEmptyState.setVisibility(View.GONE);
                adapter.updateData(measurements);
            }
        }
    }
}