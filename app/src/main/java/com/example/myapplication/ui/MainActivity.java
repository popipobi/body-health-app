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
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.BlePermissionCheck;
import com.example.myapplication.BleService;
import com.example.myapplication.BloodPressureChart;
import com.example.myapplication.BottomNavigationHelper;
import com.example.myapplication.ByteUtils;
import com.example.myapplication.EightElectrodeScaleService;
import com.example.myapplication.adapter.DeviceListAdapter;
import com.example.myapplication.R;
import com.example.myapplication.database.dao.BodyFatMeasurementDAO;
import com.example.myapplication.database.dao.MeasurementDAO;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "aoaoao";

    // 页面元素相关变量
    private LinearLayout healthDataContainer;
    private LinearLayout deviceSearchContainer;
    private Button disconnectButton;
    private Button searchButton;
    private Button filterButton;
    private Button btnSaveData;
    private MeasurementDAO measurementDAO;
    private BodyFatMeasurementDAO bodyFatMeasurementDAO;
    private int currentSystolic = 0;
    private int currentDiastolic = 0;
    private int currentPulse = 0;
    private TextView current_xueya;
    private TextView tv_systolic;
    private TextView tv_diastolic;
    private TextView tv_pulse;
    private LineChart lineChart;
    private BloodPressureChart chartHelper;

    private LinearLayout bodyFatDataContainer;
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
    private Button btnDisconnect;

    private Button modeToggleButton;
    private boolean isAutoMode = true; // 默认自动模式
    private boolean isAutoConnecting = false; // 是否正在自动连接

    private RecyclerView rvDeviceList;
    // 蓝牙相关变量
    private BluetoothManager myBluetoothManager;
    private BluetoothAdapter myBluetoothAdapter;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private List<BluetoothDevice> myBluetoothDeviceList;
    private List<String> myRssiList;
    private DeviceListAdapter myDeviceListAdapter;
    private BleService myBleService;
    private BleReceiver myBleReceiver;

    // 八电极体脂秤服务相关
    private EightElectrodeScaleService mEightElectrodeScaleService;
    private boolean mBoundToEightElectrodeService = false;
    private EightElectrodeReceiver mEightElectrodeReceiver;

    private boolean filterNoName = false;

    private enum DeviceType {
        NONE,
        BLOOD_PRESSURE,
        EIGHT_ELECTRODE_SCALE
    }
    private DeviceType currentDeviceType = DeviceType.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 检查是否已登录，未登录会跳转到登录页面
        SharedPreferences preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("is_logged_in", false);
        if (!isLoggedIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        current_xueya = findViewById(R.id.current_value_xueya);
        tv_systolic = findViewById(R.id.tv_systolic);
        tv_diastolic = findViewById(R.id.tv_diastolic);
        tv_pulse = findViewById(R.id.tv_pulse);
        searchButton = findViewById(R.id.search);
        modeToggleButton = findViewById(R.id.mode_toggle_button);
        rvDeviceList = (RecyclerView) findViewById(R.id.rv_device_list);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        BottomNavigationHelper.setupBottomNavigation(this, bottomNavigationView, R.id.navigation_measure);

        healthDataContainer = findViewById(R.id.health_data_container);
        deviceSearchContainer = findViewById(R.id.device_search_container);
        disconnectButton = findViewById(R.id.disconnect_button);

        btnSaveData = findViewById(R.id.btn_save_data);
        measurementDAO = new MeasurementDAO(this);

        bodyFatMeasurementDAO = new BodyFatMeasurementDAO(this);

        // 初始化体脂数据容器
        bodyFatDataContainer = findViewById(R.id.body_fat_data_container);

        // 初始化体脂数据视图组件
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
        btnDisconnect = findViewById(R.id.btn_disconnect);

        // 设置按钮点击事件
        btnSaveBodyFatData.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);

        searchButton.setOnClickListener(this);
        modeToggleButton.setOnClickListener(this);
        disconnectButton.setOnClickListener(this);
        btnSaveData.setOnClickListener(this);

        showDeviceSearchUI();

        // 初始化 TextView 和图表
        lineChart = findViewById(R.id.chart_bp);
        chartHelper = new BloodPressureChart(this, lineChart);

        // 注册ActivResultLauncher
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

        // 初始化蓝牙
        if (!BlePermissionCheck.hasPerMissions(this)) {
            if (BlePermissionCheck.shouldShowRationale(this)) {
                BlePermissionCheck.showRationale(this);
            }
            BlePermissionCheck.requestPermissions(this);
            return;
        } else {
            initBle();
            initData();
            registerBleReceiver();
            bindEightElectrodeService(); // 绑定八电极体脂秤服务
        }
    }

    // 绑定八电极体脂秤服务
    private void bindEightElectrodeService() {
        try {
            Intent intent = new Intent(this, EightElectrodeScaleService.class);
            boolean bound = bindService(intent, mEightElectrodeServiceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "绑定八电极体脂秤服务结果: " + (bound ? "成功" : "失败"));
            startService(intent);

            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(EightElectrodeScaleService.ACTION_DEVICE_CONNECTED);
            filter.addAction(EightElectrodeScaleService.ACTION_DEVICE_DISCONNECTED);
            filter.addAction(EightElectrodeScaleService.ACTION_DATA_AVAILABLE);
            mEightElectrodeReceiver = new EightElectrodeReceiver();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(mEightElectrodeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            }

            Log.d(TAG, "八电极体脂秤服务绑定和广播接收器注册完成");
        } catch (Exception e) {
            Log.e(TAG, "绑定八电极体脂秤服务时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 八电极体脂秤服务连接
    private ServiceConnection mEightElectrodeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            EightElectrodeScaleService.LocalBinder binder = (EightElectrodeScaleService.LocalBinder) service;
            mEightElectrodeScaleService = binder.getService();
            mBoundToEightElectrodeService = true;
            Log.d(TAG, "八电极体脂秤服务已绑定");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mEightElectrodeScaleService = null;
            mBoundToEightElectrodeService = false;
            Log.d(TAG, "八电极体脂秤服务已断开");
        }
    };

    // 八电极体脂秤数据接收器
    private class EightElectrodeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) return;

            switch (action) {
                // 在EightElectrodeReceiver中，添加更多日志记录
                case EightElectrodeScaleService.ACTION_DEVICE_CONNECTED:
                    isAutoConnecting = false; // 重置状态
                    String connectedAddress = intent.getStringExtra(EightElectrodeScaleService.EXTRA_DEVICE_ADDRESS);
                    Log.d(TAG, "收到八电极体脂秤连接广播: " + connectedAddress);
                    currentDeviceType = DeviceType.EIGHT_ELECTRODE_SCALE;
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "八电极体脂秤已连接", Toast.LENGTH_SHORT).show();
                        showHealthDataUI();
                    });
                    break;

                case EightElectrodeScaleService.ACTION_DEVICE_DISCONNECTED:
                    isAutoConnecting = false; // 重置状态
                    String disconnectedAddress = intent.getStringExtra(EightElectrodeScaleService.EXTRA_DEVICE_ADDRESS);
                    Log.d(TAG, "八电极体脂秤已断开: " + disconnectedAddress);
                    if (currentDeviceType == DeviceType.EIGHT_ELECTRODE_SCALE) {
                        currentDeviceType = DeviceType.NONE;
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "八电极体脂秤已断开", Toast.LENGTH_SHORT).show();
                    });
                    break;

                case EightElectrodeScaleService.ACTION_DATA_AVAILABLE:
                    String dataType = intent.getStringExtra("DATA_TYPE");
                    if ("WEIGHT".equals(dataType)) {
                        float weight = intent.getFloatExtra("WEIGHT", 0);
                        int unit = intent.getIntExtra("UNIT", -1);
                        int state = intent.getIntExtra("STATE", -1);
                        Log.d(TAG, "八电极体脂秤体重数据: " + weight + " 状态: " + state + " 单位: " + unit);

                        // 如果是稳定体重(state=2)，更新UI
                        if (state == 2) {
                            runOnUiThread(() -> {
                                current_xueya.setText(String.format("%.1f", weight));
                                chartHelper.updateChartData(weight);
                            });
                        }
                    } else if ("IMPEDANCE".equals(dataType)) {
                        int adc = intent.getIntExtra("ADC", 0);
                        int part = intent.getIntExtra("PART", 0);
                        int arithmetic = intent.getIntExtra("ARITHMETIC", 0);
                        Log.d(TAG, "八电极体脂秤阻抗数据: 阻抗=" + adc + " 部位=" + part + " 算法=" + arithmetic);
                    } else if ("HEART_RATE".equals(dataType)) {
                        int heartRate = intent.getIntExtra("HEART_RATE", 0);
                        Log.d(TAG, "八电极体脂秤心率数据: " + heartRate);

                        runOnUiThread(() -> {
                            tv_pulse.setText(heartRate + " bpm");
                        });
                    } else if ("STATE".equals(dataType)) {
                        int type = intent.getIntExtra("TYPE", -1);
                        int typeState = intent.getIntExtra("TYPE_STATE", -1);
                        int result = intent.getIntExtra("RESULT", -1);
                        Log.d(TAG, "八电极体脂秤状态: 类型=" + type + " 状态=" + typeState + " 结果=" + result);
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
                            // 确保显示体脂数据UI
                            showBodyFatDataUI();

                            // 更新UI组件显示体脂数据
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

                            // 启用保存按钮
                            btnSaveBodyFatData.setEnabled(true);
                        });
                    }
                    break;
            }
        }
    }

    // 接受用户授予/拒绝权限后结果
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (BlePermissionCheck.handlePermisssionsResult(requestCode, permissions, grantResults)) {
            // 权限通过
            initBle();
            initData();
            registerBleReceiver();
            bindEightElectrodeService(); // 绑定八电极体脂秤服务
        } else {
            Toast.makeText(this, "未授予权限，BLE功能用不了", Toast.LENGTH_LONG).show();
        }
    }

    private void initBle() {// 初始化蓝牙
        myBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (myBluetoothManager==null) {
            Toast.makeText(this, "蓝牙用不了", Toast.LENGTH_LONG).show();
            return;
        }
        myBluetoothAdapter = myBluetoothManager.getAdapter();
        if (myBluetoothAdapter==null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show();
            return;
        }
        if (!myBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(intent);
            return;
        }
        scanBleDevice();
        Toast.makeText(this, "蓝牙已启动", Toast.LENGTH_SHORT).show();
    }

    private void initData() {// 初始化数据
        Log.i(TAG, "初始化数据");
        // 蓝牙设备列表
        myBluetoothDeviceList = new ArrayList<>();
        // 蓝牙设备RSSI列表
        myRssiList = new ArrayList<>();
        myDeviceListAdapter = new DeviceListAdapter(myBluetoothDeviceList, myRssiList, this);
        rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        rvDeviceList.setAdapter(myDeviceListAdapter);

        // 连接蓝牙设备 - 取消注释并修改
        myDeviceListAdapter.setOnItemClickListener(new DeviceListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                BluetoothDevice device = myBluetoothDeviceList.get(position);
                String deviceName = null;

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                deviceName = device.getName();
                String deviceAddress = device.getAddress();

                // 停止扫描
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothLeScanner.stopScan(myScanCallback);

                // 根据设备名称判断是连接血压计还是八电极体脂秤
                if (deviceName != null && deviceName.equals("BM100B")) {
                    // 连接血压计
                    Toast.makeText(MainActivity.this, "开始连接血压计: " + deviceName, Toast.LENGTH_SHORT).show();
                    myBleService.connect(myBluetoothAdapter, deviceAddress);
                    currentDeviceType = DeviceType.BLOOD_PRESSURE;
                } else if (deviceName != null && (deviceName.contains("AiLink") || deviceName.contains("Scale") || deviceName.contains("Body"))) {
                    // 连接八电极体脂秤
                    Toast.makeText(MainActivity.this, "开始连接八电极体脂秤: " + deviceName, Toast.LENGTH_SHORT).show();
                    if (mEightElectrodeScaleService != null) {
                        Log.d(TAG, "正在连接八电极体脂秤: " + deviceAddress);
                        mEightElectrodeScaleService.connect(deviceAddress);
                        currentDeviceType = DeviceType.EIGHT_ELECTRODE_SCALE;
                    } else {
                        Log.e(TAG, "八电极体脂秤服务未初始化");
                        Toast.makeText(MainActivity.this, "八电极体脂秤服务未初始化", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 默认尝试连接血压计
                    Toast.makeText(MainActivity.this, "开始连接未知设备: " + (deviceName != null ? deviceName : deviceAddress), Toast.LENGTH_SHORT).show();
                    myBleService.connect(myBluetoothAdapter, deviceAddress);
                    currentDeviceType = DeviceType.BLOOD_PRESSURE;
                }
            }
        });
    }

    private void scanBleDevice() {// 搜索蓝牙设备
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                !=PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Ble权限未授予", Toast.LENGTH_SHORT).show();
            return;
        }

        bluetoothLeScanner = myBluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner==null) {
            Toast.makeText(this, "无法获取BLE扫描器", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isScanning) return;
        isScanning = true;
        bluetoothLeScanner.startScan(myScanCallback);

        // 同时也用AILink SDK扫描八电极体脂秤
        if (mEightElectrodeScaleService != null) {
            mEightElectrodeScaleService.startScan(10000); // 10秒超时
            Log.d(TAG, "启动AILink SDK扫描");
        } else {
            Log.d(TAG, "八电极体脂秤服务未初始化，无法扫描");
        }

        new Handler().postDelayed(new Runnable() {// 搜索10s，找不着拉倒
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN)
                        !=PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothLeScanner.stopScan(myScanCallback);
                isScanning = false;

                // 同时停止AILink SDK扫描
                if (mEightElectrodeScaleService != null) {
                    mEightElectrodeScaleService.stopScan();
                    Log.d(TAG, "停止AILink SDK扫描");
                }
            }
        }, 10000);
    }

    private final ScanCallback myScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN)
                    !=PackageManager.PERMISSION_GRANTED) {
                return;
            }
            String deviceName = device.getName();
            int rssi = result.getRssi();

            // 默认过滤空设备名
            if (deviceName == null || deviceName.isEmpty()) return;

            // 自动模式：检测到目标设备立即连接
            if (isAutoMode && isTargetDevice(deviceName)) {
                Log.d(TAG, "自动模式检测到目标设备: " + deviceName);
                autoConnectToDevice(device, deviceName);
                return; // 自动连接时不添加到列表
            }

            // 手动模式：添加到设备列表
            if (!myBluetoothDeviceList.contains(device)) {
                myBluetoothDeviceList.add(device);
                myRssiList.add(String.valueOf(rssi));
                myDeviceListAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(MainActivity.this, "扫描失败"+errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private void registerBleReceiver() {// 注册蓝牙数据接收器
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
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            registerReceiver(myBleReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    // 服务
    private ServiceConnection myServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder rawBinder) {
            myBleService = ((BleService.LocalBinder) rawBinder).getService();
        }

        public void onServiceDisconnected(ComponentName classname) {
            myBleService = null;
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
                    isAutoConnecting = false; // 重置状态
                    currentDeviceType = DeviceType.BLOOD_PRESSURE;
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "血压计已连接", Toast.LENGTH_SHORT).show();
                        showHealthDataUI(); // 跳转到血压测量页面
                    });
                    break;
                case BleService.ACTION_GATT_DISCONNECTED:
                    isAutoConnecting = false; // 重置状态
                    if (currentDeviceType == DeviceType.BLOOD_PRESSURE) {
                        currentDeviceType = DeviceType.NONE; // 重置设备类型
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "血压计已断开", Toast.LENGTH_SHORT).show();
                        // 可以选择是否自动回到搜索页面
                        // showDeviceSearchUI();
                    });
                    myBleService.release();
                    break;
                case BleService.ACTION_GATT_SERVICES_DISCOVERD:
                    Toast.makeText(MainActivity.this, "发现服务", Toast.LENGTH_SHORT).show();
                    myBleService.setBleNotification();
                    break;
                case BleService.ACTION_CONNECTING_FAIL:
                    isAutoConnecting = false; // 重置状态
                    currentDeviceType = DeviceType.NONE; // 重置设备类型
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "血压计连接失败", Toast.LENGTH_SHORT).show();
                        // 连接失败后可以选择回到搜索页面
                        showDeviceSearchUI();
                    });
                    myBleService.disconnect();
                    break;
                case BleService.ACTION_DATA_AVAILABLE:
                    // 处理数据
                    byte[] data = intent.getByteArrayExtra(BleService.EXTRA_DATA);
//
                    if (data!=null && data.length>0) {
                        String ans = ByteUtils.formatByteArray(data);
                        int i = compute(ans);
//                        Log.d("看看BLE数据啥玩意", "Received data: " + i);
//
                        runOnUiThread(()-> {
                            current_xueya.setText(String.valueOf(i));// 更新TextView
                            chartHelper.updateChartData(i);// 更新图表
                        });

                        // 最后满足特定格式才会出现大长串串
                        if (data.length>=17) {
                            ByteUtils.HealthData bloodPressureData = ByteUtils.parseHealthData(data);

                            // 存储当前读取的值
                            currentSystolic = bloodPressureData.getSystolic();
                            currentDiastolic = bloodPressureData.getDiastolic();
                            currentPulse = bloodPressureData.getPulse();

                            runOnUiThread(()-> {
                                tv_systolic.setText(bloodPressureData.getSystolic() + " mmHg");
                                tv_diastolic.setText(bloodPressureData.getDiastolic() + " mmHg");
                                tv_pulse.setText(bloodPressureData.getPulse() + " bpm");

                                btnSaveData.setEnabled(currentSystolic > 0 && currentDiastolic > 0 && currentPulse > 0);
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
            if (value!=0) ans = value;
        }
        return ans;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myBleReceiver!=null) {
            unregisterReceiver(myBleReceiver);
            myBleReceiver = null;
        }
        unbindService(myServiceConnection);
        myBleService = null;

        // 解绑八电极体脂秤服务
        if (mBoundToEightElectrodeService) {
            if (mEightElectrodeReceiver != null) {
                unregisterReceiver(mEightElectrodeReceiver);
                mEightElectrodeReceiver = null;
            }
            unbindService(mEightElectrodeServiceConnection);
            mEightElectrodeScaleService = null;
            mBoundToEightElectrodeService = false;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.search) {
            // 搜索蓝牙设备
            scanBleDevice();
            // 初始化数据
            initData();
            //注册蓝牙数据接收器
            registerBleReceiver();
        } else if (v.getId() == R.id.mode_toggle_button) {
            toggleMode();
        } else if (v.getId() == R.id.disconnect_button) {
            disconnectCurrentDevice();
        } else if (v.getId() == R.id.btn_save_data) {
            saveMeasurementData();
        } else if (v.getId() == R.id.btn_save_body_fat_data) {
            saveBodyFatData();
        } else if (v.getId() == R.id.btn_disconnect) {
            // 体脂数据界面的断开连接
            disconnectCurrentDevice();
        }
    }

    /**
     * 断开当前连接的设备
     */
    private void disconnectCurrentDevice() {
        Log.d(TAG, "断开当前设备连接，设备类型: " + currentDeviceType);

        // 根据当前连接的设备类型断开连接
        if (currentDeviceType == DeviceType.BLOOD_PRESSURE && myBleService != null) {
            myBleService.disconnect();
            Toast.makeText(this, "断开血压计连接", Toast.LENGTH_SHORT).show();
        } else if (currentDeviceType == DeviceType.EIGHT_ELECTRODE_SCALE && mEightElectrodeScaleService != null) {
            mEightElectrodeScaleService.disconnect();
            Toast.makeText(this, "断开八电极体脂秤连接", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "没有已连接的设备", Toast.LENGTH_SHORT).show();
        }

        // 重置当前设备类型
        currentDeviceType = DeviceType.NONE;

        // 回到蓝牙搜索界面
        showDeviceSearchUI();

        // 重置显示数据
        resetDisplayValues();

        // 重置体脂数据显示
        resetBodyFatDisplayValues();

        // 重新开始搜索（可选，你可以决定是否自动搜索）
        // scanBleDevice();
    }

    /**
     * 重置体脂数据显示
     */
    private void resetBodyFatDisplayValues() {
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

    private void toggleMode() {
        isAutoMode = !isAutoMode;
        updateModeButton();
    }

    private void updateModeButton() {
        if (isAutoMode) {
            modeToggleButton.setText("自动模式");
            modeToggleButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_orange_dark)));
        } else {
            modeToggleButton.setText("手动模式");
            modeToggleButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
        }
    }

    private void saveBodyFatData() {
        try {
            // 获取用户ID和其他信息
            SharedPreferences preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
            int userId = preferences.getInt("user_id", -1);
            int userHeight = preferences.getInt("user_height", 170); // 获取用户身高

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
                btnSaveBodyFatData.setEnabled(false);  // 禁用保存按钮，防止重复保存
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

    private void resetDisplayValues() {
        current_xueya.setText("0");
        tv_systolic.setText("0 mmHg");
        tv_diastolic.setText("0 mmHg");
        tv_pulse.setText("0 bpm");
        if (chartHelper != null) {
            chartHelper.resetChartData();
        }
    }

    private void saveMeasurementData() {
        if (currentSystolic<=0 || currentDiastolic<=0 || currentPulse<=0) {
            Toast.makeText(this, "无有效数据可保存", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        int userId = preferences.getInt("user_id", -1);

        // 保存数据
        long result = measurementDAO.saveMeasurement(userId, currentSystolic, currentDiastolic, currentPulse);

        if (result > 0) {
            Toast.makeText(this, "测量数据保存成功", Toast.LENGTH_SHORT).show();
            btnSaveData.setEnabled(false);
        } else {
            Toast.makeText(this, "测量数据保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 显示体脂数据UI
    public void showBodyFatDataUI() {
        deviceSearchContainer.setVisibility(View.GONE);
        healthDataContainer.setVisibility(View.GONE);
        bodyFatDataContainer.setVisibility(View.VISIBLE);
    }

    public void showHealthDataUI() {
        // 根据设备类型显示不同UI
        if (currentDeviceType == DeviceType.BLOOD_PRESSURE) {
            deviceSearchContainer.setVisibility(View.GONE);
            healthDataContainer.setVisibility(View.VISIBLE);
            bodyFatDataContainer.setVisibility(View.GONE);
        } else if (currentDeviceType == DeviceType.EIGHT_ELECTRODE_SCALE) {
            showBodyFatDataUI();
        }
    }

    public void showDeviceSearchUI() {
        deviceSearchContainer.setVisibility(View.VISIBLE);
        healthDataContainer.setVisibility(View.GONE);
        bodyFatDataContainer.setVisibility(View.GONE);

        // 重置数据和按钮状态
        resetDisplayValues();
    }

    /**
     * 判断是否为目标设备（血压计或八电极体脂秤）
     */
    private boolean isTargetDevice(String deviceName) {
        if (deviceName == null) return false;

        // 血压计
        if ("BM100B".equals(deviceName)) {
            return true;
        }

        // 八电极体脂秤
        if (deviceName.contains("AiLink")) {
            return true;
        }

        return false;
    }

    /**
     * 获取设备类型
     */
    private DeviceType getDeviceType(String deviceName) {
        if (deviceName == null) return DeviceType.NONE;

        if ("BM100B".equals(deviceName)) {
            return DeviceType.BLOOD_PRESSURE;
        }

        if (deviceName.contains("AiLink")) {
            return DeviceType.EIGHT_ELECTRODE_SCALE;
        }

        return DeviceType.NONE;
    }

    /**
     * 自动连接到检测到的目标设备
     */
    private void autoConnectToDevice(BluetoothDevice device, String deviceName) {
        // 如果已经在连接中，忽略
        if (isAutoConnecting) {
            Log.d(TAG, "已经在自动连接中，忽略设备: " + deviceName);
            return;
        }

        isAutoConnecting = true; // 设置连接状态

        // 停止扫描
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothLeScanner.stopScan(myScanCallback);
        isScanning = false;

        // 同时停止AILink SDK扫描
        if (mEightElectrodeScaleService != null) {
            mEightElectrodeScaleService.stopScan();
        }

        String deviceAddress = device.getAddress();
        DeviceType detectedDeviceType = getDeviceType(deviceName);

        Log.d(TAG, "开始自动连接设备: " + deviceName + " (" + deviceAddress + ")");

        // 显示连接提示
        runOnUiThread(() -> {
            Toast.makeText(this, "检测到" + getDeviceTypeName(detectedDeviceType) +
                    "，正在自动连接...", Toast.LENGTH_SHORT).show();
        });

        // 根据设备类型进行连接
        if (detectedDeviceType == DeviceType.BLOOD_PRESSURE) {
            // 连接血压计
            myBleService.connect(myBluetoothAdapter, deviceAddress);
            currentDeviceType = DeviceType.BLOOD_PRESSURE;
        } else if (detectedDeviceType == DeviceType.EIGHT_ELECTRODE_SCALE) {
            // 连接八电极体脂秤
            if (mEightElectrodeScaleService != null) {
                mEightElectrodeScaleService.connect(deviceAddress);
                currentDeviceType = DeviceType.EIGHT_ELECTRODE_SCALE;
            } else {
                Log.e(TAG, "八电极体脂秤服务未初始化");
                runOnUiThread(() -> {
                    Toast.makeText(this, "八电极体脂秤服务未初始化", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    /**
     * 获取设备类型的中文名称
     */
    private String getDeviceTypeName(DeviceType deviceType) {
        switch (deviceType) {
            case BLOOD_PRESSURE:
                return "血压计";
            case EIGHT_ELECTRODE_SCALE:
                return "八电极体脂秤";
            default:
                return "未知设备";
        }
    }
}