package com.example.myapplication.ui;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.BlePermissionCheck;
import com.example.myapplication.BleService;
import com.example.myapplication.BloodPressureChart;
import com.example.myapplication.ByteUtils;
import com.example.myapplication.R;
import com.example.myapplication.database.dao.MeasurementDAO;
import com.github.mikephil.charting.charts.LineChart;

public class BloodPressureMeasureActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "BloodPressureMeasure";

    // UI组件
    private ImageButton btnBack;
    private TextView tvDeviceName;
    private TextView tvConnectionStatus;
    private TextView currentXueya;
    private TextView tvSystolic;
    private TextView tvDiastolic;
    private TextView tvPulse;
    private Button btnSaveData;
    private LineChart lineChart;
    private BloodPressureChart chartHelper;

    // 蓝牙相关
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private BleService bleService;
    private BleReceiver bleReceiver;
    private boolean isScanning = false;
    private boolean isConnected = false;

    // 数据相关
    private String deviceName;
    private MeasurementDAO measurementDAO;
    private int currentSystolic = 0;
    private int currentDiastolic = 0;
    private int currentPulse = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_blood_pressure_measure);

        getIntentData();
        initViews();
        setupClickListeners();
        initDatabase();

        // 检查权限并初始化蓝牙
        if (!BlePermissionCheck.hasPerMissions(this)) {
            if (BlePermissionCheck.shouldShowRationale(this)) {
                BlePermissionCheck.showRationale(this);
            }
            BlePermissionCheck.requestPermissions(this);
        } else {
            initBluetooth();
        }
    }

    private void getIntentData() {
        deviceName = getIntent().getStringExtra("device_name");
        if (deviceName == null) {
            deviceName = "血压计";
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        currentXueya = findViewById(R.id.current_value_xueya);
        tvSystolic = findViewById(R.id.tv_systolic);
        tvDiastolic = findViewById(R.id.tv_diastolic);
        tvPulse = findViewById(R.id.tv_pulse);
        btnSaveData = findViewById(R.id.btn_save_data);
        lineChart = findViewById(R.id.chart_bp);

        // 设置设备名称
        tvDeviceName.setText(deviceName);
        updateConnectionStatus(false);

        // 初始化图表
        chartHelper = new BloodPressureChart(this, lineChart);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(this);
        btnSaveData.setOnClickListener(this);
    }

    private void initDatabase() {
        measurementDAO = new MeasurementDAO(this);
    }

    private void initBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show();
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show();
            return;
        }

        // 注册ActivityResultLauncher
        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        startBluetoothService();
                        startAutoScan();
                    } else {
                        Toast.makeText(this, "蓝牙未启用", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(intent);
        } else {
            startBluetoothService();
            startAutoScan();
        }
    }

    private void startBluetoothService() {
        // 绑定服务
        Intent intent = new Intent(this, BleService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);

        // 注册蓝牙信息广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BleService.ACTION_GATT_CONNECTED);
        filter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERD);
        filter.addAction(BleService.ACTION_DATA_AVAILABLE);
        filter.addAction(BleService.ACTION_CONNECTING_FAIL);
        bleReceiver = new BleReceiver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    private void startAutoScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            return;
        }

        if (isScanning) return;
        isScanning = true;

        Toast.makeText(this, "正在搜索" + deviceName + "...", Toast.LENGTH_SHORT).show();
        updateConnectionStatus(false, "正在搜索...");

        bluetoothLeScanner.startScan(scanCallback);

        // 10秒后停止扫描
        new Handler().postDelayed(() -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothLeScanner.stopScan(scanCallback);
            isScanning = false;

            if (!isConnected) {
                updateConnectionStatus(false, "未找到设备");
                Toast.makeText(this, "未找到" + deviceName + "，请确保设备已开启", Toast.LENGTH_LONG).show();
            }
        }, 10000);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (ActivityCompat.checkSelfPermission(BloodPressureMeasureActivity.this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            String deviceName = device.getName();
            if (deviceName != null && deviceName.equals("BM100B")) {
                // 找到目标设备，停止扫描并连接
                bluetoothLeScanner.stopScan(scanCallback);
                isScanning = false;

                updateConnectionStatus(false, "正在连接...");
                Toast.makeText(BloodPressureMeasureActivity.this, "找到" + deviceName + "，正在连接...", Toast.LENGTH_SHORT).show();

                if (bleService != null) {
                    bleService.connect(bluetoothAdapter, device.getAddress());
                }
            }
        }
    };

    // 服务连接
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder rawBinder) {
            bleService = ((BleService.LocalBinder) rawBinder).getService();
        }

        public void onServiceDisconnected(ComponentName classname) {
            bleService = null;
        }
    };

    // 蓝牙数据接收器
    private class BleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) return;

            switch (action) {
                case BleService.ACTION_GATT_CONNECTED:
                    isConnected = true;
                    runOnUiThread(() -> {
                        updateConnectionStatus(true);
                        Toast.makeText(BloodPressureMeasureActivity.this, deviceName + "已连接", Toast.LENGTH_SHORT).show();
                    });
                    break;

                case BleService.ACTION_GATT_DISCONNECTED:
                    isConnected = false;
                    runOnUiThread(() -> {
                        updateConnectionStatus(false, "连接已断开");
                        Toast.makeText(BloodPressureMeasureActivity.this, deviceName + "已断开", Toast.LENGTH_SHORT).show();
                    });
                    if (bleService != null) {
                        bleService.release();
                    }
                    break;

                case BleService.ACTION_GATT_SERVICES_DISCOVERD:
                    Toast.makeText(BloodPressureMeasureActivity.this, "发现服务", Toast.LENGTH_SHORT).show();
                    if (bleService != null) {
                        bleService.setBleNotification();
                    }
                    break;

                case BleService.ACTION_CONNECTING_FAIL:
                    isConnected = false;
                    runOnUiThread(() -> {
                        updateConnectionStatus(false, "连接失败");
                        Toast.makeText(BloodPressureMeasureActivity.this, deviceName + "连接失败", Toast.LENGTH_SHORT).show();
                    });
                    if (bleService != null) {
                        bleService.disconnect();
                    }
                    break;

                case BleService.ACTION_DATA_AVAILABLE:
                    // 处理数据
                    byte[] data = intent.getByteArrayExtra(BleService.EXTRA_DATA);
                    if (data != null && data.length > 0) {
                        String ans = ByteUtils.formatByteArray(data);
                        int i = compute(ans);

                        runOnUiThread(() -> {
                            currentXueya.setText(String.valueOf(i));
                            chartHelper.updateChartData(i);
                        });

                        // 处理完整的血压数据
                        if (data.length >= 17) {
                            ByteUtils.HealthData bloodPressureData = ByteUtils.parseHealthData(data);

                            currentSystolic = bloodPressureData.getSystolic();
                            currentDiastolic = bloodPressureData.getDiastolic();
                            currentPulse = bloodPressureData.getPulse();

                            runOnUiThread(() -> {
                                tvSystolic.setText(currentSystolic + " mmHg");
                                tvDiastolic.setText(currentDiastolic + " mmHg");
                                tvPulse.setText(currentPulse + " bpm");

                                // 当收缩压有值时，启用保存按钮并变为绿色
                                if (currentSystolic > 0) {
                                    enableSaveButton();
                                }
                            });
                        }
                    }
                    break;
            }
        }
    }

    private static Integer compute(String input) {
        String regex = "\\((\\d+)\\)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(input);
        Integer ans = null;
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            if (value != 0) ans = value;
        }
        return ans;
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

    private void enableSaveButton() {
        btnSaveData.setEnabled(true);
        btnSaveData.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // 绿色
    }

    private void disableSaveButton() {
        btnSaveData.setEnabled(false);
        btnSaveData.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBDBDBD)); // 灰色
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_back) {
            // 返回时自动断开连接
            disconnectDevice();
            finish();
        } else if (v.getId() == R.id.btn_save_data) {
            saveMeasurementData();
        }
    }

    private void saveMeasurementData() {
        if (currentSystolic <= 0 || currentDiastolic <= 0 || currentPulse <= 0) {
            Toast.makeText(this, "无有效数据可保存", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        int userId = preferences.getInt("user_id", -1);

        long result = measurementDAO.saveMeasurement(userId, currentSystolic, currentDiastolic, currentPulse);

        if (result > 0) {
            Toast.makeText(this, "测量数据保存成功", Toast.LENGTH_SHORT).show();
            disableSaveButton(); // 保存后禁用按钮
        } else {
            Toast.makeText(this, "测量数据保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void disconnectDevice() {
        if (bleService != null) {
            bleService.disconnect();
        }
        updateConnectionStatus(false, "已断开连接");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (BlePermissionCheck.handlePermisssionsResult(requestCode, permissions, grantResults)) {
            initBluetooth();
        } else {
            Toast.makeText(this, "未授予权限，无法使用蓝牙功能", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleReceiver != null) {
            unregisterReceiver(bleReceiver);
            bleReceiver = null;
        }
        if (bleService != null) {
            unbindService(serviceConnection);
            bleService = null;
        }
    }
}