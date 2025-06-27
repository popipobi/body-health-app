package com.example.myapplication.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

    // 配对相关
    private BluetoothDevice targetDevice;
    private boolean isPairing = false;
    private PairingReceiver pairingReceiver;

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

        // 注册配对状态接收器
        registerPairingReceiver();

        startRealBluetoothSearch();
    }

    private void registerPairingReceiver() {
        pairingReceiver = new PairingReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(pairingReceiver, filter);
        Log.d(TAG, "已注册配对状态接收器");
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

                // 保存目标设备
                targetDevice = device;

                // 根据设备类型处理
                if (DEVICE_TYPE_BLOOD_PRESSURE.equals(deviceType)) {
                    // 血压计需要配对
                    startBloodPressurePairing();
                } else {
                    // 体脂秤不需要配对，直接成功
                    runOnUiThread(() -> {
                        showSearchSuccess();
                    });
                }
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

    private void startBloodPressurePairing() {
        if (targetDevice == null) {
            Log.e(TAG, "目标设备为空，无法配对");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "缺少蓝牙连接权限", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否已经配对
        if (targetDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            runOnUiThread(() -> {
                showSearchSuccess();
            });
            return;
        }

        // 开始配对
        isPairing = true;
        Log.d(TAG, "开始配对血压计: " + targetDevice.getAddress());

        boolean pairResult = targetDevice.createBond();
        Log.d(TAG, "配对请求结果: " + pairResult);

        if (!pairResult) {
            Log.e(TAG, "配对请求失败");
            runOnUiThread(() -> {
                Toast.makeText(this, "配对请求失败", Toast.LENGTH_SHORT).show();
                // 配对失败，直接返回
                finish();
            });
        }

        // 设置配对超时（30秒）
        handler.postDelayed(() -> {
            if (isPairing) {
                Log.w(TAG, "配对超时");
                isPairing = false;
                runOnUiThread(() -> {
                    Toast.makeText(this, "配对超时，请重试", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }, 30000);
    }

    // 配对状态接收器
    private class PairingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "收到配对广播: " + action);

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.equals(targetDevice)) {
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);

                    Log.d(TAG, "配对状态变化: " + previousBondState + " -> " + bondState);

                    switch (bondState) {
                        case BluetoothDevice.BOND_BONDING:
                            Log.d(TAG, "正在配对...");
                            runOnUiThread(() -> {
                                Toast.makeText(DeviceSearchActivity.this, "正在配对...", Toast.LENGTH_SHORT).show();
                            });
                            break;

                        case BluetoothDevice.BOND_BONDED:
                            Log.d(TAG, "配对成功！");
                            isPairing = false;
                            handler.removeCallbacksAndMessages(null); // 清除超时回调
                            runOnUiThread(() -> {
                                Toast.makeText(DeviceSearchActivity.this, "配对成功！", Toast.LENGTH_SHORT).show();
                                showSearchSuccess();
                            });
                            break;

                        case BluetoothDevice.BOND_NONE:
                            Log.d(TAG, "配对失败或取消");
                            if (previousBondState == BluetoothDevice.BOND_BONDING) {
                                // 从配对中变为未配对，说明配对失败
                                isPairing = false;
                                handler.removeCallbacksAndMessages(null);
                                runOnUiThread(() -> {
                                    Toast.makeText(DeviceSearchActivity.this, "配对失败，请重试", Toast.LENGTH_LONG).show();
                                    finish();
                                });
                            }
                            break;
                    }
                }
            }
        }
    }

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

        // 注销配对状态接收器
        if (pairingReceiver != null) {
            try {
                unregisterReceiver(pairingReceiver);
                pairingReceiver = null;
                Log.d(TAG, "已注销配对状态接收器");
            } catch (Exception e) {
                Log.e(TAG, "注销配对状态接收器时出错: " + e.getMessage());
            }
        }

        // 清理所有handler回调
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}