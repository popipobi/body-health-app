package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.myapplication.ui.VentilatorSetupActivity;

import com.example.myapplication.R;

public class AddDeviceActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "AddDeviceActivity";

    // UI组件
    private ImageButton btnBack;
    private CardView cardBloodPressure;
    private CardView cardBodyFatScale;
    private CardView cardVentilator;

    // ActivityResultLauncher
    private ActivityResultLauncher<Intent> deviceSearchLauncher;
    private ActivityResultLauncher<Intent> ventilatorSetupLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_device);

        initViews();
        setupClickListeners();
        setupActivityResultLauncher();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        cardBloodPressure = findViewById(R.id.card_blood_pressure);
        cardBodyFatScale = findViewById(R.id.card_body_fat_scale);
        cardVentilator = findViewById(R.id.card_ventilator);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(this);
        cardBloodPressure.setOnClickListener(this);
        cardBodyFatScale.setOnClickListener(this);
        cardVentilator.setOnClickListener(this);
    }

    private void setupActivityResultLauncher() {
        deviceSearchLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // 处理血压计和体脂秤的搜索结果
                        Intent data = result.getData();
                        boolean deviceAdded = data.getBooleanExtra("device_added", false);
                        if (deviceAdded) {
                            String deviceType = data.getStringExtra("device_type");
                            String deviceName = data.getStringExtra("device_name");
                            setResult(RESULT_OK, data);
                            finish();
                        }
                    }
                });

        // 呼吸机配网启动器
        ventilatorSetupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // 处理呼吸机配网结果
                        Intent data = result.getData();
                        boolean deviceAdded = data.getBooleanExtra("device_added", false);
                        if (deviceAdded) {
                            // 呼吸机配网成功，返回结果给MainActivity
                            setResult(RESULT_OK, data);
                            finish();
                        }
                    }
                });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_back) {
            // 返回主页面
            finish();
        } else if (v.getId() == R.id.card_blood_pressure) {
            // 点击血压计卡片 - 跳转到搜索页面
            Intent intent = new Intent(this, DeviceSearchActivity.class);
            intent.putExtra(DeviceSearchActivity.EXTRA_DEVICE_TYPE, DeviceSearchActivity.DEVICE_TYPE_BLOOD_PRESSURE);
            deviceSearchLauncher.launch(intent);
        } else if (v.getId() == R.id.card_body_fat_scale) {
            // 点击八电极体脂秤卡片 - 跳转到搜索页面
            Intent intent = new Intent(this, DeviceSearchActivity.class);
            intent.putExtra(DeviceSearchActivity.EXTRA_DEVICE_TYPE, DeviceSearchActivity.DEVICE_TYPE_BODY_FAT_SCALE);
            deviceSearchLauncher.launch(intent);
        } else if (v.getId() == R.id.card_ventilator) {
            // 呼吸机
            Intent intent = new Intent(this, VentilatorSetupActivity.class);
            ventilatorSetupLauncher.launch(intent);
        }
    }
}