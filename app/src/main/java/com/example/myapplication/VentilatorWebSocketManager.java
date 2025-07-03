package com.example.myapplication;

import android.util.Log;
import okhttp3.*;
import okio.ByteString;

import java.util.concurrent.TimeUnit;

public class VentilatorWebSocketManager {
    private static final String TAG = "VentilatorWebSocket";

    // 呼吸机MQTT连接参数
    private static final String WEBSOCKET_URL = "wss://down.conmo.net:1883/WebSocket";
    private static final String USERNAME = "km888#1";
    private static final String PASSWORD = "km888#8";

    private OkHttpClient okHttpClient;
    private WebSocket webSocket;
    private String clientId; // 将从配网获得

    // 连接状态监听器
    public interface ConnectionListener {
        void onConnected();
        void onDisconnected();
        void onError(String error);
        void onDataReceived(String topic, String data);
    }

    private ConnectionListener connectionListener;

    public VentilatorWebSocketManager() {
        // 创建OkHttp客户端
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 设置连接监听器
     */
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    /**
     * 连接到呼吸机MQTT服务器
     * @param clientId 从蓝牙配网获得的客户端ID
     */
    public void connect(String clientId) {
        this.clientId = clientId;

        Log.d(TAG, "开始连接WebSocket MQTT服务器...");
        Log.d(TAG, "客户端ID: " + clientId);

        Request request = new Request.Builder()
                .url(WEBSOCKET_URL)
                .build();

        webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket连接成功");
                // 发送MQTT连接消息
                sendMqttConnect();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "收到文本消息: " + text);
                handleMqttMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d(TAG, "收到二进制消息: " + bytes.hex());
                // 处理二进制MQTT消息
                handleMqttBinaryMessage(bytes);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket正在关闭: " + reason);
                webSocket.close(1000, null);
                if (connectionListener != null) {
                    connectionListener.onDisconnected();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket连接失败: " + t.getMessage());
                if (connectionListener != null) {
                    connectionListener.onError("连接失败: " + t.getMessage());
                }
            }
        });
    }

    /**
     * 发送MQTT连接消息
     */
    private void sendMqttConnect() {
        Log.d(TAG, "发送MQTT连接请求...");

        // 简化的MQTT连接消息构建
        // 实际项目中这里需要按MQTT协议构建二进制消息
        // 暂时用JSON格式模拟，后续完善
        String connectMessage = "{\"type\":\"connect\",\"clientId\":\"" + clientId +
                "\",\"username\":\"" + USERNAME +
                "\",\"password\":\"" + PASSWORD + "\"}";

        webSocket.send(connectMessage);

        // 连接成功后订阅呼吸机数据
        subscribeToTopics();
    }

    /**
     * 订阅呼吸机相关topics
     */
    private void subscribeToTopics() {
        if (clientId == null) return;

        // 从clientID生成设备标识
        // 例如: longfenkeji_mhuxqvxmlu7 -> esp_mhuxqvxmlu7
        String deviceId = clientId.replace("longfenkeji_", "esp_");

        // 订阅呼吸机数据topics
        String[] topics = {
                "/longfenkeji/ventilator/" + deviceId + "/user/VentilatorForm",
                "/longfenkeji/ventilator/" + deviceId + "/user/VentilatorFlowPressure",
                "/longfenkeji/ventilator/" + deviceId + "/user/Oximeter"
        };

        for (String topic : topics) {
            subscribeToTopic(topic);
            Log.d(TAG, "订阅topic: " + topic);
        }
    }

    /**
     * 订阅单个topic
     */
    private void subscribeToTopic(String topic) {
        // 简化的订阅消息
        String subscribeMessage = "{\"type\":\"subscribe\",\"topic\":\"" + topic + "\"}";
        webSocket.send(subscribeMessage);
    }

    /**
     * 处理收到的MQTT消息
     */
    private void handleMqttMessage(String message) {
        Log.d(TAG, "处理MQTT消息: " + message);

        // 这里解析实际的MQTT消息
        // 暂时简单处理，后续完善
        if (connectionListener != null) {
            connectionListener.onDataReceived("test", message);
        }
    }

    /**
     * 处理二进制MQTT消息
     */
    private void handleMqttBinaryMessage(ByteString bytes) {
        // 解析二进制MQTT协议消息
        // 这里需要实现MQTT协议解析
        Log.d(TAG, "处理二进制MQTT消息，长度: " + bytes.size());
    }

    /**
     * 发布消息到呼吸机
     */
    public void publishVentilatorParameters(String parameters) {
        if (webSocket == null || clientId == null) {
            Log.e(TAG, "WebSocket未连接或clientId为空");
            return;
        }

        // 生成app的clientId
        String appClientId = clientId.replace("longfenkeji_", "app_");
        String topic = "/longfenkeji/app/" + appClientId + "/user/VentilatorParameters";

        // 发布消息
        String publishMessage = "{\"type\":\"publish\",\"topic\":\"" + topic +
                "\",\"payload\":\"" + parameters + "\"}";
        webSocket.send(publishMessage);

        Log.d(TAG, "发送呼吸机参数: " + parameters + " 到topic: " + topic);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "主动断开");
            webSocket = null;
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return webSocket != null;
    }
}