package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.BottomNavigationHelper;
import com.example.myapplication.R;
import com.example.myapplication.adapter.BodyFatHistoryAdapter;
import com.example.myapplication.adapter.MeasurementHistoryAdapter;
import com.example.myapplication.database.dao.BodyFatMeasurementDAO;
import com.example.myapplication.database.dao.MeasurementDAO;
import com.example.myapplication.database.model.BodyFatMeasurement;
import com.example.myapplication.database.model.Measurement;
import com.example.myapplication.util.SwipeToDeleteCallback;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView rvMeasurementHistory;
    private RecyclerView rvBodyFatHistory;
    private TextView tvEmptyStateBP;
    private TextView tvEmptyStateBF;
    private LinearLayout bloodPressureHistoryContainer;
    private LinearLayout bodyFatHistoryContainer;
    private TabLayout tabLayout;

    private MeasurementDAO measurementDAO;
    private BodyFatMeasurementDAO bodyFatMeasurementDAO;
    private MeasurementHistoryAdapter bpAdapter;
    private BodyFatHistoryAdapter bfAdapter;

    private List<Measurement> bpMeasurements;
    private List<BodyFatMeasurement> bfMeasurements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        // 初始化视图
        rvMeasurementHistory = findViewById(R.id.rv_measurement_history);
        rvBodyFatHistory = findViewById(R.id.rv_body_fat_history);
        tvEmptyStateBP = findViewById(R.id.tv_empty_state_bp);
        tvEmptyStateBF = findViewById(R.id.tv_empty_state_bf);
        bloodPressureHistoryContainer = findViewById(R.id.blood_pressure_history_container);
        bodyFatHistoryContainer = findViewById(R.id.body_fat_history_container);
        tabLayout = findViewById(R.id.tab_layout);

        // 底部设置导航
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        BottomNavigationHelper.setupBottomNavigation(this, bottomNavigationView, R.id.navigation_history);

        // 初始化DAO
        measurementDAO = new MeasurementDAO(this);
        bodyFatMeasurementDAO = new BodyFatMeasurementDAO(this);

        // 设置RecyclerView
        rvMeasurementHistory.setLayoutManager(new LinearLayoutManager(this));
        bpAdapter = new MeasurementHistoryAdapter(null);
        rvMeasurementHistory.setAdapter(bpAdapter);

        rvBodyFatHistory.setLayoutManager(new LinearLayoutManager(this));
        bfAdapter = new BodyFatHistoryAdapter(null);
        rvBodyFatHistory.setAdapter(bfAdapter);

        // 为血压历史添加滑动删除功能
        ItemTouchHelper bpItemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(this, position -> {
            if (bpMeasurements != null && position < bpMeasurements.size()) {
                showDeleteConfirmationDialog(
                        bpMeasurements.get(position).getId(),
                        position,
                        true
                );
            }
        }));
        bpItemTouchHelper.attachToRecyclerView(rvMeasurementHistory);

        // 为体脂历史添加滑动删除功能
        ItemTouchHelper bfItemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(this, position -> {
            if (bfMeasurements != null && position < bfMeasurements.size()) {
                showDeleteConfirmationDialog(
                        bfMeasurements.get(position).getId(),
                        position,
                        false
                );
            }
        }));
        bfItemTouchHelper.attachToRecyclerView(rvBodyFatHistory);

        // 设置点击体脂历史项的监听器（可以添加详情查看功能）
        bfAdapter.setOnItemClickListener(measurement -> {
            Intent intent = new Intent(this, BodyFatDetailActivity.class);
            intent.putExtra(BodyFatDetailActivity.EXTRA_DATE, measurement.getFormattedDate());
            intent.putExtra(BodyFatDetailActivity.EXTRA_WEIGHT, measurement.getWeight());
            intent.putExtra(BodyFatDetailActivity.EXTRA_BMI, measurement.getBmi());
            intent.putExtra(BodyFatDetailActivity.EXTRA_BODY_FAT_RATE, measurement.getBodyFatRate());
            intent.putExtra(BodyFatDetailActivity.EXTRA_BODY_FAT_MASS, measurement.getBodyFatMass());
            intent.putExtra(BodyFatDetailActivity.EXTRA_WATER_RATE, measurement.getWaterRate());
            intent.putExtra(BodyFatDetailActivity.EXTRA_PROTEIN_RATE, measurement.getProteinRate());
            intent.putExtra(BodyFatDetailActivity.EXTRA_MUSCLE_RATE, measurement.getMuscleRate());
            intent.putExtra(BodyFatDetailActivity.EXTRA_MUSCLE_MASS, measurement.getMuscleMass());
            intent.putExtra(BodyFatDetailActivity.EXTRA_BONE_MASS, measurement.getBoneMass());
            intent.putExtra(BodyFatDetailActivity.EXTRA_VISCERAL_FAT, measurement.getVisceralFat());
            intent.putExtra(BodyFatDetailActivity.EXTRA_BMR, measurement.getBmr());
            intent.putExtra(BodyFatDetailActivity.EXTRA_BODY_AGE, measurement.getBodyAge());
            intent.putExtra(BodyFatDetailActivity.EXTRA_IDEAL_WEIGHT, measurement.getIdealWeight());
            startActivity(intent);
        });

        // 设置Tab选择监听器
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    // 显示血压历史
                    bloodPressureHistoryContainer.setVisibility(View.VISIBLE);
                    bodyFatHistoryContainer.setVisibility(View.GONE);
                    loadBloodPressureHistory();
                } else if (position == 1) {
                    // 显示体脂历史
                    bloodPressureHistoryContainer.setVisibility(View.GONE);
                    bodyFatHistoryContainer.setVisibility(View.VISIBLE);
                    loadBodyFatHistory();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // 不需要处理
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // 可以在这里添加刷新逻辑
                int position = tab.getPosition();
                if (position == 0) {
                    loadBloodPressureHistory();
                } else if (position == 1) {
                    loadBodyFatHistory();
                }
            }
        });

        // 加载血压数据
        loadBloodPressureHistory();
    }

    // 显示删除确认对话框
    private void showDeleteConfirmationDialog(int itemId, int position, boolean isBloodPressure) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("删除记录");
        builder.setMessage("确定要删除这条记录吗？");
        builder.setPositiveButton("删除", (dialog, which) -> {
            if (isBloodPressure) {
                deleteBloodPressureRecord(itemId, position);
            } else {
                deleteBodyFatRecord(itemId, position);
            }
        });
        builder.setNegativeButton("取消", (dialog, which) -> {
            // 取消删除，恢复列表项
            if (isBloodPressure) {
                bpAdapter.notifyItemChanged(position);
            } else {
                bfAdapter.notifyItemChanged(position);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 删除血压记录
    private void deleteBloodPressureRecord(int itemId, int position) {
        boolean success = measurementDAO.deleteMeasurement(itemId);
        if (success) {
            bpMeasurements.remove(position);
            bpAdapter.updateData(bpMeasurements);
            Toast.makeText(this, "记录已删除", Toast.LENGTH_SHORT).show();

            // 如果列表为空，显示空状态
            if (bpMeasurements.isEmpty()) {
                rvMeasurementHistory.setVisibility(View.GONE);
                tvEmptyStateBP.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            bpAdapter.notifyItemChanged(position);
        }
    }

    // 删除体脂记录
    private void deleteBodyFatRecord(int itemId, int position) {
        boolean success = bodyFatMeasurementDAO.deleteBodyFatMeasurement(itemId);
        if (success) {
            bfMeasurements.remove(position);
            bfAdapter.updateData(bfMeasurements);
            Toast.makeText(this, "记录已删除", Toast.LENGTH_SHORT).show();

            // 如果列表为空，显示空状态
            if (bfMeasurements.isEmpty()) {
                rvBodyFatHistory.setVisibility(View.GONE);
                tvEmptyStateBF.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            bfAdapter.notifyItemChanged(position);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次页面恢复就刷新当前选中的数据
        int selectedTabPosition = tabLayout.getSelectedTabPosition();
        if (selectedTabPosition == 0) {
            loadBloodPressureHistory();
        } else if (selectedTabPosition == 1) {
            loadBodyFatHistory();
        }
    }

    private void loadBloodPressureHistory() {
        SharedPreferences preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        int userId = preferences.getInt("user_id", -1);

        if (userId != -1) {
            try {
                bpMeasurements = measurementDAO.getUserMeasurements(userId);

                if (bpMeasurements.isEmpty()) {
                    rvMeasurementHistory.setVisibility(View.GONE);
                    tvEmptyStateBP.setVisibility(View.VISIBLE);
                } else {
                    rvMeasurementHistory.setVisibility(View.VISIBLE);
                    tvEmptyStateBP.setVisibility(View.GONE);
                    bpAdapter.updateData(bpMeasurements);
                }
            } catch (Exception e) {
                rvMeasurementHistory.setVisibility(View.GONE);
                tvEmptyStateBP.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadBodyFatHistory() {
        SharedPreferences preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        int userId = preferences.getInt("user_id", -1);

        if (userId != -1) {
            try {
                bfMeasurements = bodyFatMeasurementDAO.getUserBodyFatMeasurements(userId);

                if (bfMeasurements.isEmpty()) {
                    rvBodyFatHistory.setVisibility(View.GONE);
                    tvEmptyStateBF.setVisibility(View.VISIBLE);
                } else {
                    rvBodyFatHistory.setVisibility(View.VISIBLE);
                    tvEmptyStateBF.setVisibility(View.GONE);
                    bfAdapter.updateData(bfMeasurements);
                }
            } catch (Exception e) {
                rvBodyFatHistory.setVisibility(View.GONE);
                tvEmptyStateBF.setVisibility(View.VISIBLE);
            }
        }
    }
}