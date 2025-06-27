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
                    // 清空之前的内容
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
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(cardView.getContext(), "创建设备卡片时出错", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}