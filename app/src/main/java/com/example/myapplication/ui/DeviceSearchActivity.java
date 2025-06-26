package com.example.myapplication.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.BlePermissionCheck;
import com.example.myapplication.R;

public class DeviceSearchActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "DeviceSearchActivity";

    // Intent传递的参数
    public static final String EXTRA_DEVICE_TYPE = "device_type";
    public static final String DEVICE_TYPE_BLOOD_PRESSURE = "blood_pressure";
    public static final String DEVICE_TYPE_BODY_FAT_SCALE = "body_fat_scale";

    // UI组件
    private ImageButton btnBack;
    private TextView tvDeviceName;
    private TextView tvSuccessDeviceName;
    private ProgressBar progressSearch;
    private Button btnCancelSearch;
    private Button btnFinish;
    private LinearLayout searchingContainer;
    private LinearLayout successContainer;

    // 蓝牙相关
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;

    // 数据
    private String deviceType;
    private String deviceName;
    private String targetDeviceName; // 要搜索的具体设备名称
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_search);

        getIntentData();
        initViews();
        setupClickListeners();

        // 检查权限并开始搜索
        if (!BlePermissionCheck.hasPerMissions(this)) {
            Toast.makeText(this, "需要蓝牙权限才能搜索设备", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initBluetooth();
    }

    private void getIntentData() {
        deviceType = getIntent().getStringExtra(EXTRA_DEVICE_TYPE);
        if (DEVICE_TYPE_BLOOD_PRESSURE.equals(deviceType)) {
            deviceName = "血压计";
            targetDeviceName = "BM100B"; // 血压计的实际设备名
        } else if (DEVICE_TYPE_BODY_FAT_SCALE.equals(deviceType)) {
            deviceName = "八电极体脂秤";
            targetDeviceName = "AiLink"; // 体脂秤的设备名包含这个关键字
        } else {
            deviceName = "未知设备";
            targetDeviceName = "";
        }

        Log.d(TAG, "搜索设备类型: " + deviceType + ", 设备名: " + deviceName + ", 目标关键字: " + targetDeviceName);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvSuccessDeviceName = findViewById(R.id.tv_success_device_name);
        progressSearch = findViewById(R.id.progress_search);
        btnCancelSearch = findViewById(R.id.btn_cancel_search);
        btnFinish = findViewById(R.id.btn_finish);
        searchingContainer = findViewById(R.id.searching_container);
        successContainer = findViewById(R.id.success_container);

        // 设置设备名称
        tvDeviceName.setText(deviceName);
        tvSuccessDeviceName.setText(deviceName);

        handler = new Handler();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(this);
        btnCancelSearch.setOnClickListener(this);
        btnFinish.setOnClickListener(this);
    }

    private void initBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "请先开启蓝牙", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        startRealBluetoothSearch();
    }

    private void startRealBluetoothSearch() {
        // 显示搜索界面
        searchingContainer.setVisibility(View.VISIBLE);
        successContainer.setVisibility(View.GONE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "缺少蓝牙扫描权限", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Toast.makeText(this, "无法获取蓝牙扫描器", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isScanning) return;
        isScanning = true;

        Log.d(TAG, "开始搜索设备: " + targetDeviceName);
        bluetoothLeScanner.startScan(scanCallback);

        // 20秒后超时
        handler.postDelayed(searchTimeoutRunnable, 20000);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (ActivityCompat.checkSelfPermission(DeviceSearchActivity.this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            String foundDeviceName = device.getName();
            Log.d(TAG, "发现设备: " + foundDeviceName);

            // 检查是否是目标设备
            if (isTargetDevice(foundDeviceName)) {
                Log.d(TAG, "找到目标设备: " + foundDeviceName);

                // 停止扫描
                stopBluetoothSearch();

                // 显示成功
                runOnUiThread(() -> {
                    showSearchSuccess();
                });
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "扫描失败，错误码: " + errorCode);
            runOnUiThread(() -> {
                Toast.makeText(DeviceSearchActivity.this, "蓝牙扫描失败", Toast.LENGTH_SHORT).show();
            });
        }
    };

    private boolean isTargetDevice(String foundDeviceName) {
        if (foundDeviceName == null || targetDeviceName.isEmpty()) {
            return false;
        }

        if (DEVICE_TYPE_BLOOD_PRESSURE.equals(deviceType)) {
            // 血压计精确匹配
            boolean isMatch = targetDeviceName.equals(foundDeviceName);
            Log.d(TAG, "血压计匹配检查: " + foundDeviceName + " == " + targetDeviceName + " ? " + isMatch);
            return isMatch;
        } else if (DEVICE_TYPE_BODY_FAT_SCALE.equals(deviceType)) {
            // 体脂秤包含关键字匹配
            boolean isMatch = foundDeviceName.contains(targetDeviceName);
            Log.d(TAG, "体脂秤匹配检查: " + foundDeviceName + " contains " + targetDeviceName + " ? " + isMatch);
            return isMatch;
        }

        return false;
    }

    private final Runnable searchTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "搜索超时，未找到目标设备: " + targetDeviceName);
            stopBluetoothSearch();
            runOnUiThread(() -> {
                Toast.makeText(DeviceSearchActivity.this,
                        "未找到" + deviceName + "，请确保设备已开启并处于可连接状态",
                        Toast.LENGTH_LONG).show();
                // 搜索失败，直接返回，不传递成功结果
                finish();
            });
        }
    };

    private void stopBluetoothSearch() {
        if (isScanning && bluetoothLeScanner != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothLeScanner.stopScan(scanCallback);
            isScanning = false;
        }

        if (handler != null) {
            handler.removeCallbacks(searchTimeoutRunnable);
        }
    }

    private void showSearchSuccess() {
        // 显示成功界面
        searchingContainer.setVisibility(View.GONE);
        successContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_back) {
            // 返回
            stopBluetoothSearch();
            finish();
        } else if (v.getId() == R.id.btn_cancel_search) {
            // 取消搜索
            stopBluetoothSearch();
            finish();
        } else if (v.getId() == R.id.btn_finish) {
            // 完成，返回主页面
            try {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("device_added", true);
                resultIntent.putExtra("device_type", deviceType);
                resultIntent.putExtra("device_name", deviceName);
                setResult(RESULT_OK, resultIntent);
                finish();
            } catch (Exception e) {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBluetoothSearch();
    }
}