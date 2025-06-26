package com.example.myapplication.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.EightElectrodeScaleService;
import com.example.myapplication.R;
import com.example.myapplication.database.dao.BodyFatMeasurementDAO;

public class BodyFatMeasureActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "BodyFatMeasure";

    // UI组件
    private ImageButton btnBack;
    private TextView tvDeviceName;
    private TextView tvConnectionStatus;
    private TextView tvWeightValue;
    private TextView tvBmiValue;
    private TextView tvBodyAgeValue;
    private TextView tvBodyFatRate;
    private TextView tvBodyFatMass;
    private TextView tvMuscleRate;
    private TextView tvMuscleMass;
    private TextView tvWaterRate;
    private TextView tvProteinRate;
    private TextView tvBoneMass;
    private TextView tvVisceralFat;
    private TextView tvBmr;
    private TextView tvIdealWeight;
    private Button btnSaveBodyFatData;

    // 八电极体脂秤服务相关
    private EightElectrodeScaleService eightElectrodeScaleService;
    private boolean boundToEightElectrodeService = false;
    private EightElectrodeReceiver eightElectrodeReceiver;

    // 数据相关
    private String deviceName;
    private BodyFatMeasurementDAO bodyFatMeasurementDAO;
    private boolean isConnected = false;
    private Handler searchTimeoutHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_body_fat_measure);

        getIntentData();
        initViews();
        setupClickListeners();
        initDatabase();
        bindEightElectrodeService();

        // 初始化搜索超时处理器
        searchTimeoutHandler = new Handler();
    }

    private void getIntentData() {
        deviceName = getIntent().getStringExtra("device_name");
        if (deviceName == null) {
            deviceName = "八电极体脂秤";
        }
    }

    private void enableSaveButton() {
        btnSaveBodyFatData.setEnabled(true);
        btnSaveBodyFatData.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // 绿色
    }

    private void disableSaveButton() {
        btnSaveBodyFatData.setEnabled(false);
        btnSaveBodyFatData.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBDBDBD)); // 灰色
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        tvWeightValue = findViewById(R.id.tv_weight_value);
        tvBmiValue = findViewById(R.id.tv_bmi_value);
        tvBodyAgeValue = findViewById(R.id.tv_body_age_value);
        tvBodyFatRate = findViewById(R.id.tv_body_fat_rate);
        tvBodyFatMass = findViewById(R.id.tv_body_fat_mass);
        tvMuscleRate = findViewById(R.id.tv_muscle_rate);
        tvMuscleMass = findViewById(R.id.tv_muscle_mass);
        tvWaterRate = findViewById(R.id.tv_water_rate);
        tvProteinRate = findViewById(R.id.tv_protein_rate);
        tvBoneMass = findViewById(R.id.tv_bone_mass);
        tvVisceralFat = findViewById(R.id.tv_visceral_fat);
        tvBmr = findViewById(R.id.tv_bmr);
        tvIdealWeight = findViewById(R.id.tv_ideal_weight);
        btnSaveBodyFatData = findViewById(R.id.btn_save_body_fat_data);

        // 设置设备名称
        tvDeviceName.setText(deviceName);
        updateConnectionStatus(false);

        // 初始化显示数据
        resetDisplayValues();
        disableSaveButton(); // 初始状态为禁用
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(this);
        btnSaveBodyFatData.setOnClickListener(this);
    }

    private void initDatabase() {
        bodyFatMeasurementDAO = new BodyFatMeasurementDAO(this);
    }

    private void bindEightElectrodeService() {
        try {
            Intent intent = new Intent(this, EightElectrodeScaleService.class);
            boolean bound = bindService(intent, eightElectrodeServiceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "绑定八电极体脂秤服务结果: " + (bound ? "成功" : "失败"));
            startService(intent);

            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(EightElectrodeScaleService.ACTION_DEVICE_CONNECTED);
            filter.addAction(EightElectrodeScaleService.ACTION_DEVICE_DISCONNECTED);
            filter.addAction(EightElectrodeScaleService.ACTION_DATA_AVAILABLE);
            eightElectrodeReceiver = new EightElectrodeReceiver();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(eightElectrodeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            }

            Log.d(TAG, "八电极体脂秤服务绑定和广播接收器注册完成");
        } catch (Exception e) {
            Log.e(TAG, "绑定八电极体脂秤服务时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 八电极体脂秤服务连接
    private ServiceConnection eightElectrodeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            EightElectrodeScaleService.LocalBinder binder = (EightElectrodeScaleService.LocalBinder) service;
            eightElectrodeScaleService = binder.getService();
            boundToEightElectrodeService = true;
            Log.d(TAG, "八电极体脂秤服务已绑定");

            // 开始自动搜索和连接
            startAutoScanAndConnect();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            eightElectrodeScaleService = null;
            boundToEightElectrodeService = false;
            Log.d(TAG, "八电极体脂秤服务已断开");
        }
    };

    private void startAutoScanAndConnect() {
        if (eightElectrodeScaleService != null) {
            Toast.makeText(this, "正在搜索" + deviceName + "...", Toast.LENGTH_SHORT).show();
            updateConnectionStatus(false, "正在搜索...");

            // 使用AILink SDK开始扫描八电极体脂秤
            eightElectrodeScaleService.startScan(15000); // 15秒超时
            Log.d(TAG, "开始使用AILink SDK扫描八电极体脂秤");

            // 设置额外的超时检查（20秒）
            searchTimeoutHandler.postDelayed(() -> {
                if (!isConnected) {
                    Log.w(TAG, "搜索超时，未找到八电极体脂秤");
                    runOnUiThread(() -> {
                        updateConnectionStatus(false, "未找到设备");
                        Toast.makeText(this, "未找到" + deviceName + "，请确保设备已开启", Toast.LENGTH_LONG).show();
                    });
                    // 停止扫描
                    if (eightElectrodeScaleService != null) {
                        eightElectrodeScaleService.stopScan();
                    }
                }
            }, 20000);

        } else {
            Log.e(TAG, "EightElectrodeScaleService未绑定，无法开始扫描");
            updateConnectionStatus(false, "服务未就绪");
        }
    }

    // 八电极体脂秤数据接收器
    private class EightElectrodeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) return;

            switch (action) {
                case EightElectrodeScaleService.ACTION_DEVICE_CONNECTED:
                    String connectedAddress = intent.getStringExtra(EightElectrodeScaleService.EXTRA_DEVICE_ADDRESS);
                    Log.d(TAG, "收到八电极体脂秤连接广播: " + connectedAddress);
                    isConnected = true;

                    // 取消搜索超时
                    if (searchTimeoutHandler != null) {
                        searchTimeoutHandler.removeCallbacksAndMessages(null);
                    }

                    runOnUiThread(() -> {
                        updateConnectionStatus(true);
                        Toast.makeText(BodyFatMeasureActivity.this, deviceName + "已连接", Toast.LENGTH_SHORT).show();
                    });
                    break;

                case EightElectrodeScaleService.ACTION_DEVICE_DISCONNECTED:
                    String disconnectedAddress = intent.getStringExtra(EightElectrodeScaleService.EXTRA_DEVICE_ADDRESS);
                    Log.d(TAG, "八电极体脂秤已断开: " + disconnectedAddress);
                    isConnected = false;
                    runOnUiThread(() -> {
                        updateConnectionStatus(false, "连接已断开");
                        Toast.makeText(BodyFatMeasureActivity.this, deviceName + "已断开", Toast.LENGTH_SHORT).show();
                    });
                    break;

                case EightElectrodeScaleService.ACTION_DATA_AVAILABLE:
                    String dataType = intent.getStringExtra("DATA_TYPE");
                    if ("WEIGHT".equals(dataType)) {
                        float weight = intent.getFloatExtra("WEIGHT", 0);
                        int state = intent.getIntExtra("STATE", -1);
                        Log.d(TAG, "八电极体脂秤体重数据: " + weight + " 状态: " + state);

                        if (state == 2) { // 稳定体重
                            runOnUiThread(() -> {
                                tvWeightValue.setText(String.format("%.1f", weight));
                            });
                        }
                    } else if ("HEART_RATE".equals(dataType)) {
                        int heartRate = intent.getIntExtra("HEART_RATE", 0);
                        Log.d(TAG, "八电极体脂秤心率数据: " + heartRate);
                    } else if ("BODY_FAT_RESULT".equals(dataType)) {
                        // 处理体脂计算结果
                        double bmi = intent.getDoubleExtra("BMI", 0);
                        double bodyFatRate = intent.getDoubleExtra("BODY_FAT_RATE", 0);
                        double bodyFatMass = intent.getDoubleExtra("BODY_FAT_MASS", 0);
                        double waterRate = intent.getDoubleExtra("WATER_RATE", 0);
                        double proteinRate = intent.getDoubleExtra("PROTEIN_RATE", 0);
                        double muscleRate = intent.getDoubleExtra("MUSCLE_RATE", 0);
                        double muscleMass = intent.getDoubleExtra("MUSCLE_MASS", 0);
                        double boneMass = intent.getDoubleExtra("BONE_MASS", 0);
                        int visceralFat = intent.getIntExtra("VISCERAL_FAT", 0);
                        int bmr = intent.getIntExtra("BMR", 0);
                        int bodyAge = intent.getIntExtra("BODY_AGE", 0);
                        double idealWeight = intent.getDoubleExtra("IDEAL_WEIGHT", 0);
                        float weight = intent.getFloatExtra("WEIGHT", 0);

                        // 更新UI
                        runOnUiThread(() -> {
                            updateBodyFatData(weight, bmi, bodyFatRate, bodyFatMass, waterRate,
                                    proteinRate, muscleRate, muscleMass, boneMass,
                                    visceralFat, bmr, bodyAge, idealWeight);
                        });
                    }
                    break;
            }
        }
    }

    private void updateBodyFatData(float weight, double bmi, double bodyFatRate, double bodyFatMass,
                                   double waterRate, double proteinRate, double muscleRate,
                                   double muscleMass, double boneMass, int visceralFat,
                                   int bmr, int bodyAge, double idealWeight) {
        tvWeightValue.setText(String.format("%.1f", weight));
        tvBmiValue.setText(String.format("%.1f", bmi));
        tvBodyAgeValue.setText(String.valueOf(bodyAge));
        tvBodyFatRate.setText(String.format("%.1f%%", bodyFatRate));
        tvBodyFatMass.setText(String.format("%.1f kg", bodyFatMass));
        tvMuscleRate.setText(String.format("%.1f%%", muscleRate));
        tvMuscleMass.setText(String.format("%.1f kg", muscleMass));
        tvWaterRate.setText(String.format("%.1f%%", waterRate));
        tvProteinRate.setText(String.format("%.1f%%", proteinRate));
        tvBoneMass.setText(String.format("%.1f kg", boneMass));
        tvVisceralFat.setText(String.valueOf(visceralFat));
        tvBmr.setText(String.format("%d kcal", bmr));
        tvIdealWeight.setText(String.format("%.1f kg", idealWeight));

        // 当体脂率有值时，启用保存按钮并变为绿色
        if (bodyFatRate > 0) {
            enableSaveButton();
        }
    }

    private void resetDisplayValues() {
        tvWeightValue.setText("0.0");
        tvBmiValue.setText("0.0");
        tvBodyAgeValue.setText("0");
        tvBodyFatRate.setText("0.0%");
        tvBodyFatMass.setText("0.0 kg");
        tvMuscleRate.setText("0.0%");
        tvMuscleMass.setText("0.0 kg");
        tvWaterRate.setText("0.0%");
        tvProteinRate.setText("0.0%");
        tvBoneMass.setText("0.0 kg");
        tvVisceralFat.setText("0");
        tvBmr.setText("0 kcal");
        tvIdealWeight.setText("0.0 kg");

        // 禁用保存按钮
        btnSaveBodyFatData.setEnabled(false);
    }

    private void updateConnectionStatus(boolean connected) {
        updateConnectionStatus(connected, null);
    }

    private void updateConnectionStatus(boolean connected, String customStatus) {
        if (customStatus != null) {
            tvConnectionStatus.setText(customStatus);
            tvConnectionStatus.setTextColor(0xFF757575);
        } else if (connected) {
            tvConnectionStatus.setText("已连接");
            tvConnectionStatus.setTextColor(0xFF4CAF50);
        } else {
            tvConnectionStatus.setText("未连接");
            tvConnectionStatus.setTextColor(0xFF757575);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_back) {
            // 返回时自动断开连接
            disconnectDevice();
            finish();
        } else if (v.getId() == R.id.btn_save_body_fat_data) {
            saveBodyFatData();
        }
    }

    private void saveBodyFatData() {
        try {
            // 获取用户ID
            SharedPreferences preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
            int userId = preferences.getInt("user_id", -1);

            if (userId == -1) {
                Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
                return;
            }

            // 从UI获取当前体脂数据
            float weight = Float.parseFloat(tvWeightValue.getText().toString());
            double bmi = Double.parseDouble(tvBmiValue.getText().toString());
            double bodyFatRate = Double.parseDouble(tvBodyFatRate.getText().toString().replace("%", ""));
            double bodyFatMass = Double.parseDouble(tvBodyFatMass.getText().toString().replace(" kg", ""));
            double waterRate = Double.parseDouble(tvWaterRate.getText().toString().replace("%", ""));
            double proteinRate = Double.parseDouble(tvProteinRate.getText().toString().replace("%", ""));
            double muscleRate = Double.parseDouble(tvMuscleRate.getText().toString().replace("%", ""));
            double muscleMass = Double.parseDouble(tvMuscleMass.getText().toString().replace(" kg", ""));
            double boneMass = Double.parseDouble(tvBoneMass.getText().toString().replace(" kg", ""));
            int visceralFat = Integer.parseInt(tvVisceralFat.getText().toString());
            int bmr = Integer.parseInt(tvBmr.getText().toString().replace(" kcal", ""));
            int bodyAge = Integer.parseInt(tvBodyAgeValue.getText().toString());
            double idealWeight = Double.parseDouble(tvIdealWeight.getText().toString().replace(" kg", ""));

            // 保存数据到数据库
            long result = bodyFatMeasurementDAO.saveBodyFatMeasurement(
                    userId, weight, bmi, bodyFatRate, bodyFatMass, waterRate, proteinRate,
                    muscleRate, muscleMass, boneMass, visceralFat, bmr, bodyAge, idealWeight);

            if (result > 0) {
                Toast.makeText(this, "体脂数据保存成功", Toast.LENGTH_SHORT).show();
                disableSaveButton(); // 保存后禁用按钮
            } else {
                Toast.makeText(this, "体脂数据保存失败", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "解析体脂数据时出错: " + e.getMessage());
            Toast.makeText(this, "数据格式错误，无法保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "保存体脂数据时出错: " + e.getMessage());
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void disconnectDevice() {
        if (eightElectrodeScaleService != null) {
            eightElectrodeScaleService.disconnect();
        }
        updateConnectionStatus(false, "已断开连接");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 清理搜索超时处理器
        if (searchTimeoutHandler != null) {
            searchTimeoutHandler.removeCallbacksAndMessages(null);
        }

        // 解绑八电极体脂秤服务
        if (boundToEightElectrodeService) {
            if (eightElectrodeReceiver != null) {
                unregisterReceiver(eightElectrodeReceiver);
                eightElectrodeReceiver = null;
            }
            unbindService(eightElectrodeServiceConnection);
            eightElectrodeScaleService = null;
            boundToEightElectrodeService = false;
        }
    }
}