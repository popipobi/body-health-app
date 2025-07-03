package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.BlePermissionCheck;
import com.example.myapplication.BottomNavigationHelper;
import com.example.myapplication.R;
import com.example.myapplication.VentilatorWebSocketManager;
import com.example.myapplication.util.SwipeToDeleteCallback;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    // UI组件
    private ImageButton btnAddDevice;
    private LinearLayout emptyContent;
    private LinearLayout deviceListContainer;

    // 数据
    private List<DeviceInfo> addedDevices;
    private ActivityResultLauncher<Intent> addDeviceLauncher;

    private VentilatorWebSocketManager ventilatorManager;

    // 添加呼吸机配网结果处理
    private ActivityResultLauncher<Intent> ventilatorSetupLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 检查是否已登录
        SharedPreferences preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("is_logged_in", false);
        if (!isLoggedIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        checkAndRequestPermissions();

        initData();
        initViews();
        setupClickListeners();
        setupBottomNavigation();
        setupActivityResultLauncher();
        updateUI();

        // 初始化呼吸机管理器
        initVentilatorManager();
        // 检查是否有已配网的呼吸机
        checkAndConnectVentilator();
    }

    private void initVentilatorManager() {
        ventilatorManager = new VentilatorWebSocketManager();
        ventilatorManager.setConnectionListener(new VentilatorWebSocketManager.ConnectionListener() {
            @Override
            public void onConnected() {
                Log.d("huhumain", "✅ 呼吸机MQTT连接成功！");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "呼吸机连接成功", Toast.LENGTH_SHORT).show();
                    // 更新呼吸机设备状态为已连接
                    updateVentilatorConnectionStatus(true);
                });
            }

            @Override
            public void onDisconnected() {
                Log.d("huhumain", "❌ 呼吸机MQTT连接断开");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "呼吸机连接断开", Toast.LENGTH_SHORT).show();
                    updateVentilatorConnectionStatus(false);
                });
            }

            @Override
            public void onError(String error) {
                Log.e("huhumain", "❌ 呼吸机连接错误: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "呼吸机连接错误: " + error, Toast.LENGTH_LONG).show();
                    updateVentilatorConnectionStatus(false);
                });
            }

            @Override
            public void onDataReceived(String topic, String data) {
                Log.d("huhumain", "📊 收到呼吸机数据:");
                Log.d("huhumain", "   主题: " + topic);
                Log.d("huhumain", "   数据: " + data);

                runOnUiThread(() -> {
                    // 解析不同类型的数据
                    if (topic.contains("VentilatorForm")) {
                        parseVentilatorForm(data);
                    } else if (topic.contains("VentilatorFlowPressure")) {
                        parseFlowPressure(data);
                    } else if (topic.contains("Oximeter")) {
                        parseOximeter(data);
                    }
                });
            }
        });
    }

    private void parseVentilatorForm(String data) {
        // 解析治疗数据: 0#2000#3000#500#0#10#4
        String[] parts = data.split("#");
        if (parts.length >= 7) {
            String mode = getModeName(Integer.parseInt(parts[0]));
            String inhaleTime = parts[1] + "ms";
            String exhaleTime = parts[2] + "ms";
            String tidalVolume = parts[3] + "ml";
            String leakage = parts[4] + "ml";
            String inhalePressure = parts[5];
            String exhalePressure = parts[6];

            Log.d("huhumain", "🫁 治疗数据解析:");
            Log.d("huhumain", "   模式: " + mode);
            Log.d("huhumain", "   吸气时间: " + inhaleTime);
            Log.d("huhumain", "   呼气时间: " + exhaleTime);
            Log.d("huhumain", "   潮气量: " + tidalVolume);
            Log.d("huhumain", "   漏气量: " + leakage);
            Log.d("huhumain", "   吸气压力: " + inhalePressure);
            Log.d("huhumain", "   呼气压力: " + exhalePressure);

            // 显示Toast提示收到数据
            Toast.makeText(this, "收到治疗数据: " + mode + "模式", Toast.LENGTH_SHORT).show();
        }
    }

    private void parseFlowPressure(String data) {
        // 解析流量压力: 50#22#20230901VT300
        String[] parts = data.split("#");
        if (parts.length >= 3) {
            String flow = parts[0] + " L/Min";
            String pressure = parts[1] + " cmH2O";
            String deviceModel = parts[2];

            Log.d("huhumain", "💨 流量压力数据:");
            Log.d("huhumain", "   流量: " + flow);
            Log.d("huhumain", "   压力: " + pressure);
            Log.d("huhumain", "   设备型号: " + deviceModel);

            Toast.makeText(this, "收到流量压力数据: " + flow, Toast.LENGTH_SHORT).show();
        }
    }

    private void parseOximeter(String data) {
        // 解析血氧数据: 98#96
        String[] parts = data.split("#");
        if (parts.length >= 2) {
            String spo2 = parts[0] + "%";
            String heartRate = parts[1] + " bpm";

            Log.d("huhumain", "❤️ 血氧数据:");
            Log.d("huhumain", "   血氧: " + spo2);
            Log.d("huhumain", "   心率: " + heartRate);

            Toast.makeText(this, "收到血氧数据: " + spo2 + ", 心率: " + heartRate, Toast.LENGTH_SHORT).show();
        }
    }

    private String getModeName(int mode) {
        switch (mode) {
            case 0: return "CPAP";
            case 1: return "S";
            case 2: return "T";
            case 3: return "ST";
            case 4: return "S+V";
            case 5: return "T+V";
            case 6: return "ST+V";
            default: return "未知模式(" + mode + ")";
        }
    }

    // 添加测试发送参数的方法 - 可选，用于测试
    private void testSendParameters() {
        if (ventilatorManager.isConnected()) {
            // 发送CPAP模式参数: 设定压力10, 初始压力4
            String parameters = "0#20#8"; // 0=CPAP模式, 20=设定压力10*2, 8=初始压力4*2
            ventilatorManager.publishVentilatorParameters(parameters);
            Log.d("huhumain", "📤 发送CPAP参数: " + parameters);
            Toast.makeText(this, "已发送CPAP参数到呼吸机", Toast.LENGTH_SHORT).show();
        } else {
            Log.w("huhumain", "⚠️ 呼吸机未连接，无法发送参数");
            Toast.makeText(this, "呼吸机未连接，无法发送参数", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ventilatorManager != null) {
            ventilatorManager.disconnect();
        }
    }

    private void checkAndConnectVentilator() {
        // 检查是否有已配网的呼吸机
        String savedClientId = getVentilatorClientId();
        if (savedClientId != null && !savedClientId.isEmpty()) {
            Log.d("huhumain", "🔄 发现已配网的呼吸机，开始连接...");
            Log.d("huhumain", "客户端ID: " + savedClientId);

            // 先测试网络连接
            testNetworkConnectivity();

            // 延迟3秒后尝试连接，给网络测试一些时间
            new android.os.Handler().postDelayed(() -> {
                ventilatorManager.connect(savedClientId);
            }, 3000);

        } else {
            Log.d("huhumain", "ℹ️ 未发现已配网的呼吸机");
        }
    }

    private String getVentilatorClientId() {
        // 从SharedPreferences获取保存的呼吸机ClientID
        SharedPreferences prefs = getSharedPreferences("ventilator_config", MODE_PRIVATE);
        return prefs.getString("client_id", null);
    }

    private void saveVentilatorClientId(String clientId) {
        // 保存呼吸机ClientID
        SharedPreferences prefs = getSharedPreferences("ventilator_config", MODE_PRIVATE);
        prefs.edit().putString("client_id", clientId).apply();
        Log.d("huhumain", "💾 保存呼吸机ClientID: " + clientId);
    }

    private boolean isVentilatorConfigured() {
        String clientId = getVentilatorClientId();
        return clientId != null && !clientId.isEmpty();
    }

    private void updateVentilatorConnectionStatus(boolean connected) {
        // 更新已添加设备列表中呼吸机的连接状态
        for (DeviceInfo device : addedDevices) {
            if ("ventilator".equals(device.type)) {
                device.isConnected = connected;
                break;
            }
        }
        // 刷新UI
        updateUI();
        // 保存状态
        saveDevicesToPreferences();
    }

    private void checkAndRequestPermissions() {
        if (!BlePermissionCheck.hasPerMissions(this)) {
            Log.d(TAG, "缺少蓝牙权限，开始申请");

            if (BlePermissionCheck.shouldShowRationale(this)) {
                // 显示权限说明
                BlePermissionCheck.showRationale(this);
            }

            // 申请权限
            BlePermissionCheck.requestPermissions(this);
        } else {
            Log.d(TAG, "已有所需的蓝牙权限");
        }
    }

    // 添加权限申请结果处理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (BlePermissionCheck.handlePermisssionsResult(requestCode, permissions, grantResults)) {
            Log.d(TAG, "蓝牙权限申请成功");
            Toast.makeText(this, "权限申请成功，现在可以使用蓝牙功能", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "蓝牙权限申请失败");
            Toast.makeText(this, "需要蓝牙权限才能使用设备连接功能", Toast.LENGTH_LONG).show();
        }
    }

    private void initData() {
        addedDevices = new ArrayList<>();
        loadDevicesFromPreferences(); // 从本地存储加载已添加的设备
    }

    private void initViews() {
        btnAddDevice = findViewById(R.id.btn_add_device);
        emptyContent = findViewById(R.id.empty_content);
        deviceListContainer = findViewById(R.id.device_list_container);
    }

    private void setupClickListeners() {
        btnAddDevice.setOnClickListener(this);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        BottomNavigationHelper.setupBottomNavigation(this, bottomNavigationView, R.id.navigation_measure);
    }

    private void setupActivityResultLauncher() {
        addDeviceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        boolean deviceAdded = data.getBooleanExtra("device_added", false);

                        if (deviceAdded) {
                            String deviceType = data.getStringExtra("device_type");
                            String deviceName = data.getStringExtra("device_name");

                            // 添加设备到列表
                            addDevice(deviceType, deviceName);

                            // 保存到本地存储
                            saveDevicesToPreferences();

                            // 更新UI
                            updateUI();
                        }
                    }
                });

        // 添加呼吸机配网启动器
        ventilatorSetupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String clientId = data.getStringExtra("client_id");

                        if (clientId != null && !clientId.isEmpty()) {
                            // 保存ClientID
                            saveVentilatorClientId(clientId);

                            // 如果还没有添加呼吸机设备，则添加
                            boolean hasVentilator = false;
                            for (DeviceInfo device : addedDevices) {
                                if ("ventilator".equals(device.type)) {
                                    hasVentilator = true;
                                    break;
                                }
                            }

                            if (!hasVentilator) {
                                addDevice("ventilator", "呼吸机");
                                saveDevicesToPreferences();
                            }

                            // 开始连接MQTT
                            Log.d("MainActivity", "🔄 配网成功，开始连接MQTT...");
                            ventilatorManager.connect(clientId);

                            updateUI();
                        }
                    }
                });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_add_device) {
            // 跳转到添加设备页面
            Intent intent = new Intent(this, AddDeviceActivity.class);
            addDeviceLauncher.launch(intent);
        }
    }

    private void addDevice(String deviceType, String deviceName) {
        // 避免重复添加同类型设备
        for (DeviceInfo existing : addedDevices) {
            if (existing.type.equals(deviceType)) {
                Toast.makeText(this, "该类型设备已存在", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        DeviceInfo deviceInfo = new DeviceInfo(deviceType, deviceName, false); // 默认未连接
        addedDevices.add(deviceInfo);

        Log.d(TAG, "添加设备: " + deviceName + " (" + deviceType + ")");
    }

    private void updateUI() {
        if (addedDevices.isEmpty()) {
            showEmptyContent();
        } else {
            showDeviceList();
        }
    }

    private void showEmptyContent() {
        emptyContent.setVisibility(View.VISIBLE);
        deviceListContainer.setVisibility(View.GONE);
    }

    private void showDeviceList() {
        emptyContent.setVisibility(View.GONE);
        deviceListContainer.setVisibility(View.VISIBLE);

        // 清空现有的设备卡片
        deviceListContainer.removeAllViews();

        // 创建RecyclerView来支持滑动删除
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 创建适配器
        DeviceCardAdapter adapter = new DeviceCardAdapter(addedDevices, this);
        recyclerView.setAdapter(adapter);

        // 添加滑动删除功能
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(this, position -> {
            if (position >= 0 && position < addedDevices.size()) {
                showDeleteConfirmationDialog(position);
            }
        }));
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // 将RecyclerView添加到容器
        deviceListContainer.addView(recyclerView);
    }

    // 设备信息类
    private static class DeviceInfo {
        String type;
        String name;
        boolean isConnected;

        // 默认构造函数
        public DeviceInfo() {}

        // 带参数构造函数
        public DeviceInfo(String type, String name, boolean isConnected) {
            this.type = type;
            this.name = name;
            this.isConnected = isConnected;
        }
    }

    // 保存设备信息到SharedPreferences
    private void saveDevicesToPreferences() {
        SharedPreferences preferences = getSharedPreferences("added_devices", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // 保存设备数量
        editor.putInt("device_count", addedDevices.size());

        // 保存每个设备的信息
        for (int i = 0; i < addedDevices.size(); i++) {
            DeviceInfo device = addedDevices.get(i);
            editor.putString("device_" + i + "_type", device.type);
            editor.putString("device_" + i + "_name", device.name);
            editor.putBoolean("device_" + i + "_connected", device.isConnected);
        }

        editor.apply();
        Log.d(TAG, "已保存 " + addedDevices.size() + " 个设备到本地存储");
    }

    // 从SharedPreferences加载设备信息
    private void loadDevicesFromPreferences() {
        SharedPreferences preferences = getSharedPreferences("added_devices", MODE_PRIVATE);

        int deviceCount = preferences.getInt("device_count", 0);
        Log.d(TAG, "从本地存储加载 " + deviceCount + " 个设备");

        for (int i = 0; i < deviceCount; i++) {
            String type = preferences.getString("device_" + i + "_type", "");
            String name = preferences.getString("device_" + i + "_name", "");
            boolean isConnected = preferences.getBoolean("device_" + i + "_connected", false);

            if (!type.isEmpty() && !name.isEmpty()) {
                DeviceInfo deviceInfo = new DeviceInfo(type, name, isConnected);
                addedDevices.add(deviceInfo);
                Log.d(TAG, "加载设备: " + name + " (" + type + ")");
            }
        }
    }

    // 从列表和本地存储中删除设备
    public void removeDevice(int position) {
        if (position >= 0 && position < addedDevices.size()) {
            DeviceInfo removedDevice = addedDevices.get(position);
            addedDevices.remove(position);

            // 更新本地存储
            saveDevicesToPreferences();

            // 更新UI
            updateUI();

            Toast.makeText(this, removedDevice.name + " 已移除", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "移除设备: " + removedDevice.name);
        }
    }

    // 显示删除确认对话框
    private void showDeleteConfirmationDialog(int position) {
        if (position >= 0 && position < addedDevices.size()) {
            DeviceInfo device = addedDevices.get(position);

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("删除设备");
            builder.setMessage("确定要删除 \"" + device.name + "\" 吗？");
            builder.setPositiveButton("删除", (dialog, which) -> {
                removeDevice(position);
            });
            builder.setNegativeButton("取消", (dialog, which) -> {
                // 取消删除，刷新列表以恢复滑动状态
                updateUI();
            });

            androidx.appcompat.app.AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    // 设备卡片适配器
    private static class DeviceCardAdapter extends RecyclerView.Adapter<DeviceCardAdapter.ViewHolder> {
        private List<DeviceInfo> devices;
        private MainActivity mainActivity;

        public DeviceCardAdapter(List<DeviceInfo> devices, MainActivity mainActivity) {
            this.devices = devices;
            this.mainActivity = mainActivity;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 创建设备卡片视图
            CardView cardView = new CardView(parent.getContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 32);
            cardView.setLayoutParams(cardParams);
            cardView.setRadius(16);
            cardView.setCardElevation(6);
            cardView.setClickable(true);
            cardView.setFocusable(true);

            return new ViewHolder(cardView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DeviceInfo deviceInfo = devices.get(position);
            holder.setupCard(deviceInfo, mainActivity);
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private CardView cardView;

            ViewHolder(CardView cardView) {
                super(cardView);
                this.cardView = cardView;
            }

            void setupCard(DeviceInfo deviceInfo, MainActivity mainActivity) {
                try {
                    cardView.removeAllViews();

                    // 创建内容布局
                    LinearLayout contentLayout = new LinearLayout(cardView.getContext());
                    contentLayout.setOrientation(LinearLayout.HORIZONTAL);
                    contentLayout.setPadding(48, 48, 48, 48);

                    // 设备图标
                    ImageView deviceIcon = new ImageView(cardView.getContext());
                    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(120, 120);
                    iconParams.setMarginEnd(48);
                    deviceIcon.setLayoutParams(iconParams);
                    deviceIcon.setImageResource(android.R.drawable.stat_sys_data_bluetooth);
                    deviceIcon.setColorFilter(0xFF1976D2);

                    // 设备信息布局
                    LinearLayout infoLayout = new LinearLayout(cardView.getContext());
                    infoLayout.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
                    );
                    infoLayout.setLayoutParams(infoParams);

                    // 设备名称
                    TextView deviceName = new TextView(cardView.getContext());
                    deviceName.setText(deviceInfo.name);
                    deviceName.setTextSize(20);
                    deviceName.setTextColor(0xFF333333);
                    deviceName.setTypeface(deviceName.getTypeface(), android.graphics.Typeface.BOLD);

                    // 连接状态
                    TextView connectionStatus = new TextView(cardView.getContext());
                    connectionStatus.setText(deviceInfo.isConnected ? "已连接" : "未连接");
                    connectionStatus.setTextSize(14);
                    connectionStatus.setTextColor(deviceInfo.isConnected ? 0xFF4CAF50 : 0xFF757575);
                    LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    statusParams.setMargins(0, 12, 0, 0);
                    connectionStatus.setLayoutParams(statusParams);

                    // 组装布局
                    infoLayout.addView(deviceName);
                    infoLayout.addView(connectionStatus);
                    contentLayout.addView(deviceIcon);
                    contentLayout.addView(infoLayout);
                    cardView.addView(contentLayout);

                    // 设置点击事件
                    cardView.setOnClickListener(v -> {
                        if (DeviceSearchActivity.DEVICE_TYPE_BLOOD_PRESSURE.equals(deviceInfo.type)) {
                            Intent intent = new Intent(mainActivity, BloodPressureMeasureActivity.class);
                            intent.putExtra("device_name", deviceInfo.name);
                            mainActivity.startActivity(intent);
                        } else if (DeviceSearchActivity.DEVICE_TYPE_BODY_FAT_SCALE.equals(deviceInfo.type)) {
                            Intent intent = new Intent(mainActivity, BodyFatMeasureActivity.class);
                            intent.putExtra("device_name", deviceInfo.name);
                            mainActivity.startActivity(intent);
                        } else if ("ventilator".equals(deviceInfo.type)) {
                            // 呼吸机-根据配网状态决定操作
                            if (mainActivity.isVentilatorConfigured()) {
                                // 已配网 - 进入测量页面（待创建）
                                Toast.makeText(mainActivity, "呼吸机测量页面开发中...", Toast.LENGTH_SHORT).show();
                                // TODO: 后续创建呼吸机测量页面
                                // Intent intent = new Intent(mainActivity, VentilatorMeasureActivity.class);
                                // mainActivity.startActivity(intent);
                            } else {
                                // 未配网 - 进入配网页面
                                Intent intent = new Intent(mainActivity, VentilatorSetupActivity.class);
                                mainActivity.ventilatorSetupLauncher.launch(intent);
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(cardView.getContext(), "创建设备卡片时出错", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // 测试
    private void testNetworkConnectivity() {
        new Thread(() -> {
            try {
                Log.d("huhumain", "🌐 开始测试网络连接...");

                // 测试1: DNS解析
                try {
                    java.net.InetAddress address = java.net.InetAddress.getByName("down.conmo.net");
                    Log.d("huhumain", "✅ DNS解析成功: " + address.getHostAddress());

                    runOnUiThread(() -> {
                        Toast.makeText(this, "DNS解析成功: " + address.getHostAddress(), Toast.LENGTH_LONG).show();
                    });

                    // 测试2: 尝试连接
                    testSocketConnection(address.getHostAddress());

                } catch (java.net.UnknownHostException e) {
                    Log.e("huhumain", "❌ DNS解析失败: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(this, "DNS解析失败，请检查网络", Toast.LENGTH_LONG).show();
                    });

                    // 尝试使用公共DNS测试
                    testWithPublicDNS();
                }

            } catch (Exception e) {
                Log.e("huhumain", "❌ 网络测试失败: " + e.getMessage());
            }
        }).start();
    }

    private void testSocketConnection(String host) {
        try {
            Log.d("huhumain", "🔌 测试Socket连接到: " + host + ":1883");

            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(host, 1883), 5000);
            socket.close();

            Log.d("huhumain", "✅ Socket连接成功");
            runOnUiThread(() -> {
                Toast.makeText(this, "服务器连接测试成功", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            Log.e("huhumain", "❌ Socket连接失败: " + e.getMessage());
            runOnUiThread(() -> {
                Toast.makeText(this, "服务器连接失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    private void testWithPublicDNS() {
        try {
            Log.d("huhumain", "🌐 尝试使用8.8.8.8 DNS...");

            // 这里可以尝试使用不同的DNS服务器
            // 但在Android中比较复杂，暂时跳过

            runOnUiThread(() -> {
                Toast.makeText(this, "DNS问题，请尝试切换网络", Toast.LENGTH_LONG).show();
            });

        } catch (Exception e) {
            Log.e("huhumain", "❌ 公共DNS测试失败: " + e.getMessage());
        }
    }

    // 添加一个按钮来触发网络测试（可选）
    private void addNetworkTestButton() {
        // 在某个地方添加测试按钮，比如长按呼吸机卡片时
        // 这里只是示例代码
    }
}