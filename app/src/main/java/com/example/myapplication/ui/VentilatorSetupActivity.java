package com.example.myapplication.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.BlePermissionCheck;
import com.example.myapplication.R;

import java.util.UUID;

/**
 * 呼吸机蓝牙配网页面
 * 作用：通过蓝牙给呼吸机配置WiFi网络信息
 */
public class VentilatorSetupActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "VentilatorSetup";

    // 呼吸机蓝牙服务UUIDs（来自文档）
    private static final String SERVICE_UUID = "0000FFFF-0000-1000-8000-00805F9B34FB";
    private static final String CHARACTERISTIC_WRITE_UUID = "0000FF01-0000-1000-8000-00805F9B34FB";
    private static final String CHARACTERISTIC_READ_UUID = "0000FF02-0000-1000-8000-00805F9B34FB";

    // UI组件
    private ImageButton btnBack;
    private EditText etWifiSsid;           // WiFi名称输入框
    private EditText etWifiPassword;       // WiFi密码输入框
    private Button btnStartSetup;          // 开始配网按钮
    private CardView cardStatus;           // 状态显示卡片
    private ProgressBar progressSetup;     // 进度条
    private TextView tvStatus;             // 状态文本
    private TextView tvStatusDetail;       // 状态详情

    // 蓝牙相关
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic writeCharacteristic;  // 用于写入数据
    private BluetoothGattCharacteristic readCharacteristic;   // 用于读取数据

    // 状态管理
    private boolean isScanning = false;
    private boolean isConnected = false;
    private String generatedClientId;      // 生成的客户端ID
    private Handler timeoutHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ventilator_setup);

        initViews();
        setupClickListeners();
        initBluetooth();
        generateClientId();
    }

    /**
     * 初始化界面组件
     */
    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etWifiSsid = findViewById(R.id.et_wifi_ssid);
        etWifiPassword = findViewById(R.id.et_wifi_password);
        btnStartSetup = findViewById(R.id.btn_start_setup);
        cardStatus = findViewById(R.id.card_status);
        progressSetup = findViewById(R.id.progress_setup);
        tvStatus = findViewById(R.id.tv_status);
        tvStatusDetail = findViewById(R.id.tv_status_detail);
    }

    /**
     * 设置按钮点击监听
     */
    private void setupClickListeners() {
        btnBack.setOnClickListener(this);
        btnStartSetup.setOnClickListener(this);
    }

    /**
     * 初始化蓝牙功能
     */
    private void initBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            showError("设备不支持蓝牙");
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            showError("设备不支持蓝牙");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showError("请先开启蓝牙");
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    /**
     * 生成客户端ID
     * 格式：longfenkeji_随机11位字符
     */
    private void generateClientId() {
        // 生成11位随机字符串
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            randomString.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        generatedClientId = "longfenkeji_" + randomString.toString();
        Log.d(TAG, "生成的客户端ID: " + generatedClientId);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_back) {
            finish();
        } else if (v.getId() == R.id.btn_start_setup) {
            startVentilatorSetup();
        }
    }

    /**
     * 开始呼吸机配网流程
     */
    private void startVentilatorSetup() {
        // 验证输入
        String ssid = etWifiSsid.getText().toString().trim();
        String password = etWifiPassword.getText().toString().trim();

        if (TextUtils.isEmpty(ssid)) {
            Toast.makeText(this, "请输入WiFi名称", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入WiFi密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查蓝牙权限
        if (!BlePermissionCheck.hasPerMissions(this)) {
            Toast.makeText(this, "需要蓝牙权限才能配网", Toast.LENGTH_SHORT).show();
            BlePermissionCheck.requestPermissions(this);
            return;
        }

        // 开始配网流程
        showStatus("正在搜索呼吸机...", "请确保呼吸机已进入配网模式");
        btnStartSetup.setEnabled(false);
        startBluetoothScan();
    }

    /**
     * 开始蓝牙扫描
     */
    private void startBluetoothScan() {
        if (isScanning) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            showError("缺少蓝牙扫描权限");
            return;
        }

        isScanning = true;
        bluetoothLeScanner.startScan(scanCallback);

        // 设置10秒扫描超时
        timeoutHandler.postDelayed(() -> {
            if (isScanning) {
                stopBluetoothScan();
                showError("未找到呼吸机设备\n请确保设备已进入配网模式");
            }
        }, 10000);
    }

    /**
     * 停止蓝牙扫描
     */
    private void stopBluetoothScan() {
        if (!isScanning) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
        isScanning = false;
        timeoutHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 蓝牙扫描回调
     */
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (ActivityCompat.checkSelfPermission(VentilatorSetupActivity.this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            String deviceName = device.getName();
            Log.d(TAG, "发现设备: " + deviceName);

            // 查找ESP32设备（呼吸机蓝牙名称）
            if (deviceName != null && deviceName.equals("ESP32")) {
                Log.d(TAG, "找到呼吸机: " + device.getAddress());
                stopBluetoothScan();
                connectToVentilator(device);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "蓝牙扫描失败: " + errorCode);
            showError("蓝牙扫描失败");
        }
    };

    /**
     * 连接到呼吸机
     */
    private void connectToVentilator(BluetoothDevice device) {
        showStatus("正在连接呼吸机...", "建立蓝牙连接");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            showError("缺少蓝牙连接权限");
            return;
        }

        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    /**
     * 蓝牙GATT回调
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "蓝牙连接成功");
                isConnected = true;
                runOnUiThread(() -> showStatus("连接成功", "正在发现服务..."));

                if (ActivityCompat.checkSelfPermission(VentilatorSetupActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "蓝牙连接断开");
                isConnected = false;
                runOnUiThread(() -> showError("连接断开"));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "发现服务成功");
                findCharacteristics(gatt);
            } else {
                Log.e(TAG, "发现服务失败");
                runOnUiThread(() -> showError("发现服务失败"));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                handleReadData(data);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "写入特征成功");
                runOnUiThread(() -> showStatus("发送成功", "等待配网结果..."));
                // 写入成功后读取响应
                readConfigResult();
            } else {
                Log.e(TAG, "写入特征失败");
                runOnUiThread(() -> showError("发送配网数据失败"));
            }
        }
    };

    /**
     * 查找读写特征
     */
    private void findCharacteristics(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
        if (service == null) {
            runOnUiThread(() -> showError("未找到呼吸机服务"));
            return;
        }

        writeCharacteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_WRITE_UUID));
        readCharacteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_READ_UUID));

        if (writeCharacteristic == null || readCharacteristic == null) {
            runOnUiThread(() -> showError("未找到必要的特征"));
            return;
        }

        runOnUiThread(() -> {
            showStatus("准备发送配网数据...", "正在构建配网信息");
            sendWifiConfig();
        });
    }

    /**
     * 发送WiFi配置数据
     */
    private void sendWifiConfig() {
        String ssid = etWifiSsid.getText().toString().trim();
        String password = etWifiPassword.getText().toString().trim();

        // 按照文档格式：ssid#password#clientID
        String configData = ssid + "#" + password + "#" + generatedClientId;
        Log.d(TAG, "发送配网数据: " + configData);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            showError("缺少蓝牙权限");
            return;
        }

        writeCharacteristic.setValue(configData.getBytes());
        boolean success = bluetoothGatt.writeCharacteristic(writeCharacteristic);

        if (!success) {
            showError("发送配网数据失败");
        }
    }

    /**
     * 读取配网结果
     */
    private void readConfigResult() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // 延迟2秒后读取结果
        timeoutHandler.postDelayed(() -> {
            if (bluetoothGatt != null && readCharacteristic != null) {
                bluetoothGatt.readCharacteristic(readCharacteristic);
            }
        }, 2000);
    }

    /**
     * 处理读取到的数据
     */
    private void handleReadData(byte[] data) {
        if (data == null || data.length < 3) return;

        // 按照文档：成功返回A6 00 01，失败返回A6 00 00
        if (data[0] == (byte) 0xA6 && data[1] == 0x00) {
            if (data[2] == 0x01) {
                // 配网成功
                runOnUiThread(() -> showConfigSuccess());
            } else {
                // 配网失败
                runOnUiThread(() -> showError("WiFi连接失败\n请检查网络名称和密码"));
            }
        }
    }

    /**
     * 显示配网成功
     */
    private void showConfigSuccess() {
        hideStatus();
        Toast.makeText(this, "呼吸机配网成功！", Toast.LENGTH_LONG).show();

        // 返回结果给AddDeviceActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("device_added", true);
        resultIntent.putExtra("device_type", "ventilator");
        resultIntent.putExtra("device_name", "呼吸机");
        resultIntent.putExtra("client_id", generatedClientId);
        setResult(RESULT_OK, resultIntent);

        finish();
    }

    /**
     * 显示状态信息
     */
    private void showStatus(String status, String detail) {
        cardStatus.setVisibility(View.VISIBLE);
        progressSetup.setVisibility(View.VISIBLE);
        tvStatus.setText(status);
        tvStatusDetail.setText(detail);
    }

    /**
     * 隐藏状态显示
     */
    private void hideStatus() {
        cardStatus.setVisibility(View.GONE);
        btnStartSetup.setEnabled(true);
    }

    /**
     * 显示错误信息
     */
    private void showError(String error) {
        hideStatus();
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        stopBluetoothScan();
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                bluetoothGatt.close();
            }
            bluetoothGatt = null;
        }
        timeoutHandler.removeCallbacksAndMessages(null);
    }
}