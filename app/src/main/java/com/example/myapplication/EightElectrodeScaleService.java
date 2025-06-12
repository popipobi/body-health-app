package com.example.myapplication;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.pingwang.bluetoothlib.AILinkBleManager;
import com.pingwang.bluetoothlib.AILinkSDK;
import com.pingwang.bluetoothlib.bean.BleValueBean;
import com.pingwang.bluetoothlib.bean.SupportUnitBean;
import com.pingwang.bluetoothlib.config.BleConfig;
import com.pingwang.bluetoothlib.device.BleDevice;
import com.pingwang.bluetoothlib.listener.OnCallbackBle;

import java.util.List;

import cn.net.aicare.modulelibrary.module.EightBodyfatscale.EightBodyFatBleDeviceData;

public class EightElectrodeScaleService extends Service {
    private static final String TAG = "EightElectrodeService";

    // 常量
    public static final String ACTION_DEVICE_CONNECTED = "com.example.myapplication.ACTION_DEVICE_CONNECTED";
    public static final String ACTION_DEVICE_DISCONNECTED = "com.example.myapplication.ACTION_DEVICE_DISCONNECTED";
    public static final String ACTION_DATA_AVAILABLE = "com.example.myapplication.ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA = "com.example.myapplication.EXTRA_DATA";
    public static final String EXTRA_DEVICE_ADDRESS = "com.example.myapplication.EXTRA_DEVICE_ADDRESS";

    // AILink SDK相关
    private AILinkBleManager aiLinkBleManager;
    private BleDevice bleDevice;
    private EightBodyFatBleDeviceData eightBodyFatBleDeviceData;

    // 服务绑定
    private final IBinder mBinder = new LocalBinder();

    // 连接状态
    private boolean isConnected = false;
    private String connectedDeviceAddress = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "服务创建");
        initAiLinkSDK();
    }

    private void initAiLinkSDK() {
        try {
            // 初始化AILinkSDK
            AILinkSDK.getInstance().init(getApplicationContext());
            aiLinkBleManager = AILinkBleManager.getInstance();
            aiLinkBleManager.init(getApplicationContext(), new AILinkBleManager.onInitListener() {
                @Override
                public void onInitSuccess() {
                    Log.d(TAG, "AILink SDK初始化成功");
                    // 设置蓝牙状态回调
                    aiLinkBleManager.setOnCallbackBle(bleCallback);
                }

                @Override
                public void onInitFailure() {
                    Log.e(TAG, "AILink SDK初始化失败");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "初始化AILink SDK时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 蓝牙状态回调
    private final OnCallbackBle bleCallback = new OnCallbackBle() {
        @Override
        public void onStartScan() {
            Log.d(TAG, "开始扫描");
        }

        @Override
        public void onScanning(BleValueBean data) {
            Log.d(TAG, "扫描到设备: " + data.getName() + " MAC: " + data.getMac());
        }

        @Override
        public void onScanTimeOut() {
            Log.d(TAG, "扫描超时");
        }

        @Override
        public void onConnecting(String mac) {
            Log.d(TAG, "正在连接: " + mac);
        }

        @Override
        public void onDisConnected(String mac, int code) {
            Log.d(TAG, "连接断开: " + mac + " 错误码: " + code);
            isConnected = false;
            connectedDeviceAddress = null;

            // 广播断开连接事件
            Intent intent = new Intent(ACTION_DEVICE_DISCONNECTED);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, mac);
            sendBroadcast(intent);
        }

        @Override
        public void onConnectionSuccess(String mac) {
            Log.d(TAG, "连接成功: " + mac);
        }

        @Override
        public void onServicesDiscovered(String mac) {
            Log.d(TAG, "发现服务: " + mac);
            isConnected = true;
            connectedDeviceAddress = mac;

            // 获取BleDevice对象
            bleDevice = aiLinkBleManager.getBleDevice(mac);
            if (bleDevice != null) {
                // 初始化八电极体脂秤数据解析对象
                eightBodyFatBleDeviceData = new EightBodyFatBleDeviceData(bleDevice);
                // 设置数据回调
                setupEightElectrodeCallbacks();

                // 广播连接成功事件
                Intent intent = new Intent(ACTION_DEVICE_CONNECTED);
                intent.putExtra(EXTRA_DEVICE_ADDRESS, mac);
                sendBroadcast(intent);
            } else {
                Log.e(TAG, "获取BleDevice失败");
            }
        }

        @Override
        public void bleOpen() {
            Log.d(TAG, "蓝牙已开启");
        }

        @Override
        public void bleClose() {
            Log.d(TAG, "蓝牙已关闭");
        }
    };

    // 设置八电极体脂秤的数据回调
    private void setupEightElectrodeCallbacks() {
        if (eightBodyFatBleDeviceData != null) {
            eightBodyFatBleDeviceData.setEightBodyFatCallback(new EightBodyFatBleDeviceData.EightBodyFatCallback() {
                @Override
                public void onState(int type, int typeState, int result) {
                    Log.d(TAG, "测量状态: 类型=" + type + " 状态=" + typeState + " 结果=" + result);

                    // 广播状态数据
                    Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                    intent.putExtra("DATA_TYPE", "STATE");
                    intent.putExtra("TYPE", type);
                    intent.putExtra("TYPE_STATE", typeState);
                    intent.putExtra("RESULT", result);
                    sendBroadcast(intent);
                }

                @Override
                public void onWeight(int state, float weight, int unit, int decimal) {
                    Log.d(TAG, "体重数据: 状态=" + state + " 体重=" + weight + " 单位=" + unit + " 小数位=" + decimal);

                    // 广播体重数据
                    Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                    intent.putExtra("DATA_TYPE", "WEIGHT");
                    intent.putExtra("STATE", state);
                    intent.putExtra("WEIGHT", weight);
                    intent.putExtra("UNIT", unit);
                    intent.putExtra("DECIMAL", decimal);
                    sendBroadcast(intent);
                }

                @Override
                public void onImpedance(int adc, int part, int arithmetic) {
                    Log.d(TAG, "阻抗数据: 阻抗值=" + adc + " 部位=" + part + " 算法=" + arithmetic);

                    // 广播阻抗数据
                    Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                    intent.putExtra("DATA_TYPE", "IMPEDANCE");
                    intent.putExtra("ADC", adc);
                    intent.putExtra("PART", part);
                    intent.putExtra("ARITHMETIC", arithmetic);
                    sendBroadcast(intent);
                }

                @Override
                public void onHeartRate(int heartRate) {
                    Log.d(TAG, "心率: " + heartRate);

                    // 广播心率数据
                    Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                    intent.putExtra("DATA_TYPE", "HEART_RATE");
                    intent.putExtra("HEART_RATE", heartRate);
                    sendBroadcast(intent);
                }

                @Override
                public void onTemp(int sign, float temp, int unit, int decimal) {
                    Log.d(TAG, "温度数据: 符号=" + sign + " 温度=" + temp + " 单位=" + unit + " 小数位=" + decimal);

                    // 广播温度数据
                    Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                    intent.putExtra("DATA_TYPE", "TEMPERATURE");
                    intent.putExtra("SIGN", sign);
                    intent.putExtra("TEMPERATURE", temp);
                    intent.putExtra("UNIT", unit);
                    intent.putExtra("DECIMAL", decimal);
                    sendBroadcast(intent);
                }

                @Override
                public void onVersion(String version) {
                    Log.d(TAG, "版本: " + version);

                    // 广播版本数据
                    Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                    intent.putExtra("DATA_TYPE", "VERSION");
                    intent.putExtra("VERSION", version);
                    sendBroadcast(intent);
                }

                @Override
                public void onSupportUnit(List<SupportUnitBean> list) {
                    Log.d(TAG, "支持的单位列表: " + (list != null ? list.size() : 0));

                    // 广播支持的单位数据
                    Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                    intent.putExtra("DATA_TYPE", "SUPPORT_UNIT");
                    // 注意: 这里不能直接传递List对象，应该转换为数组或序列化对象
                    sendBroadcast(intent);
                }

                @Override
                public void showData(String data) {
                    Log.d(TAG, "原始数据: " + data);
                }
            });
        }
    }

    // 扫描设备
    public void startScan(long timeoutMs) {
        if (aiLinkBleManager != null) {
            // 使用AILink的UUID过滤扫描
            aiLinkBleManager.startScan(timeoutMs, BleConfig.UUID_SERVER_AILINK);
        }
    }

    // 停止扫描
    public void stopScan() {
        if (aiLinkBleManager != null) {
            aiLinkBleManager.stopScan();
        }
    }

    // 连接设备
    public void connect(String address) {
        if (aiLinkBleManager != null) {
            // 先停止扫描
            aiLinkBleManager.stopScan();
            // 连接设备
            aiLinkBleManager.connectDevice(address);
        }
    }

    // 断开连接
    public void disconnect() {
        if (bleDevice != null) {
            bleDevice.disconnect();
        }
    }

    // 检查是否已连接
    public boolean isConnected() {
        return isConnected;
    }

    // 获取连接的设备地址
    public String getConnectedDeviceAddress() {
        return connectedDeviceAddress;
    }

    // 设置体重单位
    public void setWeightUnit(int unit) {
        if (eightBodyFatBleDeviceData != null) {
            eightBodyFatBleDeviceData.setWeightUnit(unit);
        }
    }

    // 设置温度单位
    public void setTempUnit(int unit) {
        if (eightBodyFatBleDeviceData != null) {
            eightBodyFatBleDeviceData.setTempUnit(unit);
        }
    }

    // 获取设备支持的单位列表
    public void getUnitList() {
        if (eightBodyFatBleDeviceData != null) {
            eightBodyFatBleDeviceData.getUnitList();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        // 断开所有连接
        if (aiLinkBleManager != null) {
            aiLinkBleManager.disconnectAll();
        }
        super.onDestroy();
    }

    // 服务Binder
    public class LocalBinder extends Binder {
        public EightElectrodeScaleService getService() {
            return EightElectrodeScaleService.this;
        }
    }
}