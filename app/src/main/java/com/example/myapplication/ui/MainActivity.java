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
import com.example.myapplication.adapter.DeviceListAdapter;
import com.example.myapplication.R;
import com.example.myapplication.database.dao.MeasurementDAO;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    // 页面元素相关变量
    private LinearLayout healthDataContainer;
    private LinearLayout deviceSearchContainer;
    private Button disconnectButton;
    private Button searchButton;
    private Button filterButton;
    private Button btnSaveData;
    private MeasurementDAO measurementDAO;
    private int currentSystolic = 0;
    private int currentDiastolic = 0;
    private int currentPulse = 0;
    private TextView current_xueya;
    private TextView tv_systolic;
    private TextView tv_diastolic;
    private TextView tv_pulse;
    private LineChart lineChart;
    private BloodPressureChart chartHelper;

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

    private boolean filterNoName = false;

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
        filterButton = findViewById(R.id.filter_button);
        rvDeviceList = (RecyclerView) findViewById(R.id.rv_device_list);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        BottomNavigationHelper.setupBottomNavigation(this, bottomNavigationView, R.id.navigation_measure);

        healthDataContainer = findViewById(R.id.health_data_container);
        deviceSearchContainer = findViewById(R.id.device_search_container);
        disconnectButton = findViewById(R.id.disconnect_button);

        btnSaveData = findViewById(R.id.btn_save_data);
        measurementDAO = new MeasurementDAO(this);

        searchButton.setOnClickListener(this);
        filterButton.setOnClickListener(this);
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
        Log.i("main", "初屎化！");
        // 蓝牙设备列表
        myBluetoothDeviceList = new ArrayList<>();
        // 蓝牙设备RSSI列表
        myRssiList = new ArrayList<>();
        myDeviceListAdapter = new DeviceListAdapter(myBluetoothDeviceList, myRssiList, this);
        rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        rvDeviceList.setAdapter(myDeviceListAdapter);

        // 连接蓝牙设备
        myDeviceListAdapter.setOnItemClickListener(new DeviceListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(MainActivity.this, "开始连接", Toast.LENGTH_SHORT).show();
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN)
                        !=PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothLeScanner.stopScan(myScanCallback);
                myBleService.connect(myBluetoothAdapter, myBluetoothDeviceList.get(position).getAddress());
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

        new Handler().postDelayed(new Runnable() {// 搜索10s，找不着拉倒
            @Override
            public void run() {
//              myBluetoothAdapter.stopLeScan(myLeScanCallback);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN)
                        !=PackageManager.PERMISSION_GRANTED) {
                    return;
                }// 多余检查两遍，要不然startScan和stopScan会红，虽然之前已经检查了权限，但在 scanBleDevice() 方法中直接调用了需要权限的方法就是会红
                bluetoothLeScanner.stopScan(myScanCallback);
                isScanning = false;
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

            if (filterNoName && (deviceName==null || deviceName.isEmpty())) return;

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
                    Toast.makeText(MainActivity.this, "蓝牙已连接", Toast.LENGTH_SHORT).show();
                    break;
                case BleService.ACTION_GATT_DISCONNECTED:
                    Toast.makeText(MainActivity.this, "蓝牙已断开", Toast.LENGTH_SHORT).show();
                    myBleService.release();
                    break;
                case BleService.ACTION_GATT_SERVICES_DISCOVERD:
                    Toast.makeText(MainActivity.this, "发现服务", Toast.LENGTH_SHORT).show();
                    myBleService.setBleNotification();
                    break;
                case BleService.ACTION_CONNECTING_FAIL:
                    Toast.makeText(MainActivity.this, "蓝牙连接失败", Toast.LENGTH_SHORT).show();
                    myBleService.disconnect();
                    break;
                case BleService.ACTION_DATA_AVAILABLE:
//                    // 处理数据
//                    byte[] data = intent.getByteArrayExtra(BleService.EXTRA_DATA);
//
//                    if (data!=null && data.length>0) {
//                        String ans = ByteUtils.formatByteArray(data);
//                        int i = compute(ans);
//                        Log.d("看看BLE数据啥玩意", "Received data: " + i);
//
//                        runOnUiThread(()-> {
//                            current_xueya.setText(String.valueOf(i));// 更新TextView
//                            chartHelper.updateChartData(i);// 更新图表
//                        });
//
//                        // 最后满足特定格式才会出现大长串串
//                        if (data.length>=17) {
//                            ByteUtils.HealthData bloodPressureData = ByteUtils.parseHealthData(data);
//
//                            // 存储当前读取的值
//                            currentSystolic = bloodPressureData.getSystolic();
//                            currentDiastolic = bloodPressureData.getDiastolic();
//                            currentPulse = bloodPressureData.getPulse();
//
//                            runOnUiThread(()-> {
//                                tv_systolic.setText(bloodPressureData.getSystolic() + " mmHg");
//                                tv_diastolic.setText(bloodPressureData.getDiastolic() + " mmHg");
//                                tv_pulse.setText(bloodPressureData.getPulse() + " bpm");
//
//                                btnSaveData.setEnabled(currentSystolic > 0 && currentDiastolic > 0 && currentPulse > 0);
//                            });
//                        }
//                    }
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
        } else if (v.getId() == R.id.filter_button) {
            filterNoName = !filterNoName;

            if (filterNoName) {
                filterButton.setText("显示全部");
                filterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_orange_dark)));
            } else {
                filterButton.setText("过滤空设备名");
                filterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
            }
            if (isScanning || !myBluetoothDeviceList.isEmpty()) {
                myBluetoothDeviceList.clear();
                myRssiList.clear();
                myDeviceListAdapter.notifyDataSetChanged();

                scanBleDevice();
            }
        } else if (v.getId() == R.id.disconnect_button) {
            if (myBleService != null) {
                myBleService.disconnect();
                Toast.makeText(this, "断开蓝牙连接", Toast.LENGTH_SHORT).show();
            }

            showDeviceSearchUI();

            current_xueya.setText("0");
            tv_systolic.setText("0 mmHg");
            tv_diastolic.setText("0 mmHg");
            tv_pulse.setText("0 bpm");
            if (chartHelper != null) {
                chartHelper.resetChartData();
            }

            scanBleDevice();
        } else if (v.getId() == R.id.btn_save_data) {
            saveMeasurementData();
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

    public void showHealthDataUI() {
        deviceSearchContainer.setVisibility(View.GONE);
        healthDataContainer.setVisibility(View.VISIBLE);
    }

    public void showDeviceSearchUI() {
        deviceSearchContainer.setVisibility(View.VISIBLE);
        healthDataContainer.setVisibility(View.GONE);

        // 重置数据和按钮状态
        currentSystolic = 0;
        currentDiastolic = 0;
        currentPulse = 0;
        if (btnSaveData != null) {
            btnSaveData.setEnabled(false);
        }
    }
}