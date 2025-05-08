package com.example.myapplication;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import java.util.UUID;

// 蓝牙连接服务
public class BleService extends Service {
    private final String TAG = BleService.class.getSimpleName();
    private BluetoothGatt myBlueToothGatt;
    private BluetoothAdapter myBluetoothAdapter;

    private int myConnectionState = 0;// 蓝牙连接状态
    private final int STATE_DISCONNECTED = 0;// 蓝牙连接已断开
    private final int STATE_CONNECTING = 1;// 蓝牙连接中
    private final int STATE_CONNECTED = 2;// 蓝牙已连接

    public final static String ACTION_GATT_CONNECTED = "com.example.ble.ACTION_GATT_CONNECTED";// 蓝牙已连接
    public final static String ACTION_GATT_DISCONNECTED = "com.example.ble.ACTION_GATT_DISCONNECTED";// 蓝牙已断开
    public final static String ACTION_GATT_SERVICES_DISCOVERD = "com.example.ble.ACTION_GATT_SERVICES_DISCOVERD";// 发现GATT服务
    public final static String ACTION_DATA_AVAILABLE = "com.example.ble.ACTION_DATA_AVAILABLE";// 收到蓝牙数据
    public final static String ACTION_CONNECTING_FAIL = "com.example.ble.ACTION_CONNECTING_FAIL";// 连接失败
    public final static String EXTRA_DATA = "com.example.ble.EXTRA_DATA";// 蓝牙数据
//    public final static String EXTRA_DATA_mys = "com.example.ble.EXTRA_DATA_mys";// 不知道是哪门子的蓝牙数据


    // 血压服务
    public static final UUID SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    // 血压测量特征值(Notify,数据通过这个来发送)
    public final UUID CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb");
    // 描述标识

    public static final UUID SERVICE_UUID_AILINK_1 = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID_AILINK_1 = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb");

    public static final UUID SERVICE_UUID_AILINK_2 = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID_AILINK_2 = UUID.fromString("0000fee2-0000-1000-8000-00805f9b34fb");

    private final UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // 服务相关
    private final IBinder myBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return myBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        release();
        return super.onUnbind(intent);
    }

    // 蓝牙操作回调（连接了才回调）
    private final BluetoothGattCallback myGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "监听连接状态改变");
            if (newState== BluetoothProfile.STATE_CONNECTED) {
                // 蓝牙已连接
                myConnectionState = STATE_CONNECTED;
                sendBleBroadCast(ACTION_GATT_CONNECTED);
                if (ActivityCompat.checkSelfPermission(BleService.this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                myBlueToothGatt.discoverServices();// 搜索GATT服务
            } else if (newState==BluetoothProfile.STATE_DISCONNECTED) {
                // 蓝牙已断开
                myConnectionState = STATE_DISCONNECTED;
                sendBleBroadCast(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {// 发现GATT服务
            // 测试看所有服务特征
//            for (BluetoothGattService service : gatt.getServices()) {
//                Log.d(TAG, "发现服务 UUID: " + service.getUuid());
//                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
//                    Log.d(TAG, "特征 UUID: " + characteristic.getUuid());
//                }
//            }

            if (status==BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "myBluetoothGatt="+myBlueToothGatt);
                sendBleBroadCast(ACTION_GATT_SERVICES_DISCOVERD);
            }
            // test
            else Log.e(TAG, "服务发现失败，状态码：" + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            sendBleBroadCast(ACTION_DATA_AVAILABLE, characteristic);// 收到数据
            if (SERVICE_UUID.equals(characteristic.getService().getUuid()) &&
                    CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT.equals(characteristic.getUuid())) {
                sendBleBroadCast(ACTION_DATA_AVAILABLE, characteristic);
            }

            // AiLink_B657 第一个
            if (SERVICE_UUID_AILINK_1.equals(characteristic.getService().getUuid()) &&
                    CHARACTERISTIC_UUID_AILINK_1.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                Log.i(TAG, "接收到秤第一个Notify数据：" + ByteUtils.formatByteArray(data));
                sendBleBroadCast(ACTION_DATA_AVAILABLE, characteristic);
            }

            // AiLink_B657 第二个
            if (SERVICE_UUID_AILINK_2.equals(characteristic.getService().getUuid()) &&
                    CHARACTERISTIC_UUID_AILINK_2.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                Log.i(TAG, "接收到秤第二个Notify数据：" + new String(data));
                sendBleBroadCast(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,  BluetoothGattCharacteristic characteristic, int status) {
            if (status==BluetoothGatt.GATT_SUCCESS) {
                sendBleBroadCast(ACTION_DATA_AVAILABLE, characteristic);
            } else {
                Log.i(TAG, "接收数据失败："+status);
            }
        }
    };

    private void sendBleBroadCast(String action) {// 发送通知 广播Action
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void sendBleBroadCast(String action, BluetoothGattCharacteristic characteristic) {// 发送通知 广播Action， characteristic数据
        Log.i(TAG,"来活了");
        // 记得恢复我
        Intent intent = new Intent(action);
        if (CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT.equals(characteristic.getUuid())) {
            // 处理血压测量特征数据
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        } else {
            Log.i(TAG, "整错了，不是我要的特征");
        }
        sendBroadcast(intent);
    }

    public boolean connect(BluetoothAdapter bluetoothAdapter, String address) {// 蓝牙连接 address 设备mac地址
        if (bluetoothAdapter==null || TextUtils.isEmpty(address)) return false;

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device==null) return false;
        if (ActivityCompat.checkSelfPermission(BleService.this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        myBlueToothGatt = device.connectGatt(this, false, myGattCallback);
        myConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {// 蓝牙断开连接
        if (myBlueToothGatt==null) return;
        if (ActivityCompat.checkSelfPermission(BleService.this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        myBlueToothGatt.disconnect();
    }

    public void release() {
        if (myBlueToothGatt==null) return;
        if (ActivityCompat.checkSelfPermission(BleService.this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        myBlueToothGatt.close();
        myBlueToothGatt = null;
    }

    public void setBleNotification() {// 蓝牙设备在数据改变时，通知app
        if (myBlueToothGatt==null) {
            if (ActivityCompat.checkSelfPermission(BleService.this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            sendBleBroadCast(ACTION_CONNECTING_FAIL);
            return;
        }

        // 处理血压测量服务通知 indicate
        BluetoothGattService gattService = myBlueToothGatt.getService(SERVICE_UUID);
        if (gattService != null) {
            BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(CHARACTERISTIC_BLOOD_PRESSURE_MEASUREMENT);
            if (gattCharacteristic != null) {
                BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(DESCRIPTOR_UUID);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                if (myBlueToothGatt.writeDescriptor(descriptor)) {
                    myBlueToothGatt.setCharacteristicNotification(gattCharacteristic, true);
                }
            }
        }

        // 处理秤的服务1 notify
        BluetoothGattService gattService1 = myBlueToothGatt.getService(SERVICE_UUID_AILINK_1);
        if (gattService1 != null) {
            BluetoothGattCharacteristic gattCharacteristic1 = gattService1.getCharacteristic(CHARACTERISTIC_UUID_AILINK_1);
            if (gattCharacteristic1 != null) {
                myBlueToothGatt.setCharacteristicNotification(gattCharacteristic1, true);
            }
        }

        // 处理秤的服务2 notify
        BluetoothGattService gattService2 = myBlueToothGatt.getService(SERVICE_UUID_AILINK_2);
        if (gattService2 != null) {
            BluetoothGattCharacteristic gattCharacteristic2 = gattService2.getCharacteristic(CHARACTERISTIC_UUID_AILINK_2);
            if (gattCharacteristic2 != null) {
                myBlueToothGatt.setCharacteristicNotification(gattCharacteristic2, true);
            }
        }
    }
}