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
    private BluetoothManager myBluetoothManager;
    private BluetoothAdapter myBluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private BleService myBleService;
    private BleReceiver myBleReceiver;
    private boolean isScanning = false;

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
            return;
        } else {
            initBle();
            registerBleReceiver();
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

    private void initBle() {// 初始化蓝牙 - 完全按照旧版本MainActivity的逻辑
        myBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (myBluetoothManager == null) {
            Toast.makeText(this, "蓝牙用不了", Toast.LENGTH_LONG).show();
            return;
        }
        myBluetoothAdapter = myBluetoothManager.getAdapter();
        if (myBluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show();
            return;
        }

        // 注册ActivityResultLauncher
        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        scanBleDevice();
                    } else {
                        Toast.makeText(this, "蓝牙未启用，没法扫描", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (!myBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(intent);
            return;
        }
        scanBleDevice();
        Toast.makeText(this, "蓝牙已启动", Toast.LENGTH_SHORT).show();
    }

    // 添加Handler变量
    private Handler scanTimeoutHandler = new Handler();
    private Runnable scanTimeoutRunnable;

    private void scanBleDevice() {// 搜索蓝牙设备
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Ble权限未授予", Toast.LENGTH_SHORT).show();
            return;
        }

        bluetoothLeScanner = myBluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Toast.makeText(this, "无法获取BLE扫描器", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isScanning) return;
        isScanning = true;

        Toast.makeText(this, "正在搜索" + deviceName + "...", Toast.LENGTH_SHORT).show();
        updateConnectionStatus(false, "正在搜索...");

        bluetoothLeScanner.startScan(myScanCallback);

        // 创建超时任务
        scanTimeoutRunnable = new Runnable() {// 搜索10s，找不着拉倒
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(BloodPressureMeasureActivity.this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothLeScanner.stopScan(myScanCallback);
                isScanning = false;
                updateConnectionStatus(false, "未找到设备");
                Toast.makeText(BloodPressureMeasureActivity.this, "未找到" + deviceName + "，请确保设备已开启", Toast.LENGTH_LONG).show();
            }
        };
        scanTimeoutHandler.postDelayed(scanTimeoutRunnable, 10000);
    }

    private final ScanCallback myScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (ActivityCompat.checkSelfPermission(BloodPressureMeasureActivity.this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            String deviceName = device.getName();

            // 只连接血压计
            if (deviceName != null && deviceName.equals("BM100B")) {
                // 🔥 找到目标设备，立即取消超时Handler
                if (scanTimeoutHandler != null && scanTimeoutRunnable != null) {
                    scanTimeoutHandler.removeCallbacks(scanTimeoutRunnable);
                }

                // 停止扫描
                bluetoothLeScanner.stopScan(myScanCallback);
                isScanning = false;

                updateConnectionStatus(false, "正在连接...");
                Toast.makeText(BloodPressureMeasureActivity.this, "找到" + deviceName + "，正在连接...", Toast.LENGTH_SHORT).show();

                // 连接血压计
                myBleService.connect(myBluetoothAdapter, device.getAddress());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(BloodPressureMeasureActivity.this, "扫描失败" + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private void registerBleReceiver() {// 注册蓝牙数据接收器 - 完全按照旧版本MainActivity的逻辑
        // 绑定服务
        Intent intent = new Intent(this, BleService.class);
        bindService(intent, myServiceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);

        // 注册蓝牙信息广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BleService.ACTION_GATT_CONNECTED);
        filter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERD);
        filter.addAction(BleService.ACTION_DATA_AVAILABLE);
        filter.addAction(BleService.ACTION_CONNECTING_FAIL);
        myBleReceiver = new BleReceiver();
        // >= API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(myBleReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    // 服务 - 完全按照旧版本MainActivity的逻辑
    private ServiceConnection myServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder rawBinder) {
            myBleService = ((BleService.LocalBinder) rawBinder).getService();
        }

        public void onServiceDisconnected(ComponentName classname) {
            myBleService = null;
        }
    };

    // 蓝牙数据接收器 - 完全按照旧版本MainActivity的逻辑
    private class BleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) return;

            switch (action) {
                case BleService.ACTION_GATT_CONNECTED:
                    runOnUiThread(() -> {
                        updateConnectionStatus(true);
//                        Toast.makeText(BloodPressureMeasureActivity.this, "血压计已连接", Toast.LENGTH_SHORT).show();
                    });
                    break;
                case BleService.ACTION_GATT_DISCONNECTED:
                    runOnUiThread(() -> {
                        updateConnectionStatus(false, "连接已断开");
//                        Toast.makeText(BloodPressureMeasureActivity.this, "血压计已断开", Toast.LENGTH_SHORT).show();
                    });
                    myBleService.release();
                    break;
                case BleService.ACTION_GATT_SERVICES_DISCOVERD:
//                    Toast.makeText(BloodPressureMeasureActivity.this, "发现服务", Toast.LENGTH_SHORT).show();
                    myBleService.setBleNotification();
                    break;
                case BleService.ACTION_CONNECTING_FAIL:
                    runOnUiThread(() -> {
                        updateConnectionStatus(false, "连接失败");
//                        Toast.makeText(BloodPressureMeasureActivity.this, "血压计连接失败", Toast.LENGTH_SHORT).show();
                    });
                    myBleService.disconnect();
                    break;
                case BleService.ACTION_DATA_AVAILABLE:
                    // 处理数据 - 完全按照旧版本MainActivity的逻辑
                    byte[] data = intent.getByteArrayExtra(BleService.EXTRA_DATA);
                    if (data != null && data.length > 0) {
                        String ans = ByteUtils.formatByteArray(data);
                        int i = compute(ans);

                        runOnUiThread(() -> {
                            currentXueya.setText(String.valueOf(i));// 更新TextView
                            chartHelper.updateChartData(i);// 更新图表
                        });

                        // 最后满足特定格式才会出现大长串串
                        if (data.length >= 17) {
                            ByteUtils.HealthData bloodPressureData = ByteUtils.parseHealthData(data);

                            // 存储当前读取的值
                            currentSystolic = bloodPressureData.getSystolic();
                            currentDiastolic = bloodPressureData.getDiastolic();
                            currentPulse = bloodPressureData.getPulse();

                            runOnUiThread(() -> {
                                tvSystolic.setText(bloodPressureData.getSystolic() + " mmHg");
                                tvDiastolic.setText(bloodPressureData.getDiastolic() + " mmHg");
                                tvPulse.setText(bloodPressureData.getPulse() + " bpm");

                                btnSaveData.setEnabled(currentSystolic > 0 && currentDiastolic > 0 && currentPulse > 0);
                                if (currentSystolic > 0) {
                                    enableSaveButton();
                                }
                            });
                        }
                    }
                    break;
                default:
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
        if (myBleService != null) {
            myBleService.disconnect();
        }
        updateConnectionStatus(false, "已断开连接");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (BlePermissionCheck.handlePermisssionsResult(requestCode, permissions, grantResults)) {
            initBle();
            registerBleReceiver();
        } else {
            Toast.makeText(this, "未授予权限，无法使用蓝牙功能", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 🔥 清理扫描超时Handler
        if (scanTimeoutHandler != null && scanTimeoutRunnable != null) {
            scanTimeoutHandler.removeCallbacks(scanTimeoutRunnable);
        }

        if (myBleReceiver != null) {
            unregisterReceiver(myBleReceiver);
            myBleReceiver = null;
        }
        unbindService(myServiceConnection);
        myBleService = null;
    }
}