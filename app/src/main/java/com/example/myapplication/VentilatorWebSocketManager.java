package com.example.myapplication;

import android.util.Log;
import okhttp3.*;
import okio.ByteString;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class VentilatorWebSocketManager {
    private static final String TAG = "huhu";

    // 呼吸机MQTT连接参数
    // 方案1: 使用IP + WSS + 标准WebSocket端口
    private static final String WEBSOCKET_URL_1 = "wss://119.23.204.237:8083/WebSocket";

    // 方案2: 使用IP + WSS + HTTPS端口
    private static final String WEBSOCKET_URL_2 = "wss://119.23.204.237:443/WebSocket";

    // 方案3: 使用域名 + WSS（如果DNS能解析）
    private static final String WEBSOCKET_URL_3 = "wss://down.conmo.net:8083/WebSocket";

    // 方案4: 原始文档地址（如果DNS问题解决）
    private static final String WEBSOCKET_URL_4 = "wss://down.conmo.net:1883/WebSocket";
    private static final String WEBSOCKET_URL = WEBSOCKET_URL_1;

    private static final String USERNAME = "km888#1";
    private static final String PASSWORD = "km888#8";

    // MQTT消息类型
    private static final byte MQTT_CONNECT = 1;
    private static final byte MQTT_CONNACK = 2;
    private static final byte MQTT_PUBLISH = 3;
    private static final byte MQTT_PUBACK = 4;
    private static final byte MQTT_SUBSCRIBE = 8;
    private static final byte MQTT_SUBACK = 9;
    private static final byte MQTT_PINGREQ = 12;
    private static final byte MQTT_PINGRESP = 13;
    private static final byte MQTT_DISCONNECT = 14;

    private OkHttpClient okHttpClient;
    private WebSocket webSocket;
    private String clientId; // 从配网获得的客户端ID
    private AtomicInteger messageId = new AtomicInteger(1);
    private boolean isConnected = false;

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
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .pingInterval(25, TimeUnit.SECONDS) // 心跳间隔
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
    /**
     * 连接到呼吸机MQTT服务器 - 增加重试机制
     */
    public void connect(String clientId) {
        this.clientId = clientId;
        connectWithRetry(WEBSOCKET_URL, 0);
    }

    private void connectWithRetry(String url, int attemptCount) {
        Log.d(TAG, "尝试连接 (第" + (attemptCount + 1) + "次): " + url);
        Log.d(TAG, "客户端ID: " + clientId);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Sec-WebSocket-Protocol", "mqtt")
                .build();

        webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "✅ WebSocket连接成功: " + url);
                // 发送MQTT连接消息
                sendMqttConnect();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "收到文本消息: " + text);
                if (connectionListener != null) {
                    connectionListener.onError("收到意外的文本消息: " + text);
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d(TAG, "收到二进制消息，长度: " + bytes.size());
                handleMqttBinaryMessage(bytes);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket正在关闭: " + reason);
                webSocket.close(1000, null);
                isConnected = false;
                if (connectionListener != null) {
                    connectionListener.onDisconnected();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "❌ WebSocket连接失败: " + t.getMessage());
                isConnected = false;

                // 尝试其他URL
                tryNextUrl(attemptCount);
            }
        });
    }

    private void tryNextUrl(int attemptCount) {
        String[] urls = {
                WEBSOCKET_URL_1, // wss://119.23.204.237:8083/WebSocket
                WEBSOCKET_URL_2, // wss://119.23.204.237:443/WebSocket
                WEBSOCKET_URL_3, // wss://down.conmo.net:8083/WebSocket
                WEBSOCKET_URL_4  // wss://down.conmo.net:1883/WebSocket
        };

        int nextAttempt = attemptCount + 1;
        if (nextAttempt < urls.length) {
            Log.d(TAG, "🔄 尝试下一个URL...");
            // 延迟2秒后尝试下一个URL
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                connectWithRetry(urls[nextAttempt], nextAttempt);
            }, 2000);
        } else {
            Log.e(TAG, "❌ 所有连接方式都失败了");
            if (connectionListener != null) {
                connectionListener.onError("无法连接到呼吸机服务器，已尝试所有连接方式");
            }
        }
    }

    /**
     * 发送MQTT连接消息
     */
    private void sendMqttConnect() {
        Log.d(TAG, "发送MQTT连接请求...");

        try {
            // 构建MQTT CONNECT消息
            ByteBuffer buffer = ByteBuffer.allocate(256);

            // Fixed header
            buffer.put((byte) (MQTT_CONNECT << 4)); // Message Type

            // Variable header - Protocol Name
            String protocolName = "MQTT";
            buffer.put((byte) 0x00);
            buffer.put((byte) protocolName.length());
            buffer.put(protocolName.getBytes(StandardCharsets.UTF_8));

            // Protocol Level
            buffer.put((byte) 0x04); // MQTT 3.1.1

            // Connect Flags
            byte connectFlags = 0x02; // Clean Session
            if (USERNAME != null && !USERNAME.isEmpty()) {
                connectFlags |= 0x80; // Username Flag
            }
            if (PASSWORD != null && !PASSWORD.isEmpty()) {
                connectFlags |= 0x40; // Password Flag
            }
            buffer.put(connectFlags);

            // Keep Alive
            buffer.put((byte) 0x00);
            buffer.put((byte) 0x3C); // 60 seconds

            // Payload - Client Identifier
            buffer.put((byte) 0x00);
            buffer.put((byte) clientId.length());
            buffer.put(clientId.getBytes(StandardCharsets.UTF_8));

            // Username
            if (USERNAME != null && !USERNAME.isEmpty()) {
                buffer.put((byte) 0x00);
                buffer.put((byte) USERNAME.length());
                buffer.put(USERNAME.getBytes(StandardCharsets.UTF_8));
            }

            // Password
            if (PASSWORD != null && !PASSWORD.isEmpty()) {
                buffer.put((byte) 0x00);
                buffer.put((byte) PASSWORD.length());
                buffer.put(PASSWORD.getBytes(StandardCharsets.UTF_8));
            }

            // 计算并更新剩余长度
            int payloadLength = buffer.position() - 2; // 减去固定头的2字节
            buffer.put(1, (byte) payloadLength);

            // 发送消息
            byte[] message = new byte[buffer.position()];
            buffer.rewind();
            buffer.get(message);

            webSocket.send(ByteString.of(message));

        } catch (Exception e) {
            Log.e(TAG, "发送MQTT连接消息失败: " + e.getMessage());
            if (connectionListener != null) {
                connectionListener.onError("发送连接消息失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理收到的MQTT二进制消息
     */
    private void handleMqttBinaryMessage(ByteString bytes) {
        if (bytes.size() < 2) {
            Log.w(TAG, "收到的MQTT消息太短");
            return;
        }

        byte[] data = bytes.toByteArray();
        byte messageType = (byte) ((data[0] >> 4) & 0x0F);

        Log.d(TAG, "收到MQTT消息类型: " + messageType);

        switch (messageType) {
            case MQTT_CONNACK:
                handleConnAck(data);
                break;
            case MQTT_PUBLISH:
                handlePublish(data);
                break;
            case MQTT_SUBACK:
                handleSubAck(data);
                break;
            case MQTT_PINGRESP:
                Log.d(TAG, "收到心跳响应");
                break;
            default:
                Log.w(TAG, "未处理的MQTT消息类型: " + messageType);
                break;
        }
    }

    /**
     * 处理连接确认消息
     */
    private void handleConnAck(byte[] data) {
        if (data.length >= 4) {
            byte returnCode = data[3];
            if (returnCode == 0) {
                Log.d(TAG, "MQTT连接成功");
                isConnected = true;
                if (connectionListener != null) {
                    connectionListener.onConnected();
                }
                // 连接成功后订阅呼吸机数据
                subscribeToTopics();
            } else {
                Log.e(TAG, "MQTT连接失败，返回码: " + returnCode);
                if (connectionListener != null) {
                    connectionListener.onError("MQTT连接失败，返回码: " + returnCode);
                }
            }
        }
    }

    /**
     * 处理发布消息
     */
    private void handlePublish(byte[] data) {
        try {
            // 解析PUBLISH消息
            int index = 1;

            // 跳过剩余长度字段
            while ((data[index] & 0x80) != 0) {
                index++;
            }
            index++;

            // 读取主题长度
            int topicLength = ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
            index += 2;

            // 读取主题
            String topic = new String(data, index, topicLength, StandardCharsets.UTF_8);
            index += topicLength;

            // 读取消息ID（如果QoS > 0）
            // 这里假设QoS = 0，跳过消息ID

            // 读取载荷
            int payloadLength = data.length - index;
            String payload = new String(data, index, payloadLength, StandardCharsets.UTF_8);

            Log.d(TAG, "收到发布消息 - 主题: " + topic + ", 载荷: " + payload);

            if (connectionListener != null) {
                connectionListener.onDataReceived(topic, payload);
            }

        } catch (Exception e) {
            Log.e(TAG, "解析发布消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理订阅确认消息
     */
    private void handleSubAck(byte[] data) {
        Log.d(TAG, "收到订阅确认");
        // 可以在这里检查订阅是否成功
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
        try {
            ByteBuffer buffer = ByteBuffer.allocate(256);

            // Fixed header
            buffer.put((byte) (MQTT_SUBSCRIBE << 4 | 0x02)); // QoS 1

            // Message ID
            int msgId = messageId.getAndIncrement();

            // Variable header - Message ID
            buffer.put((byte) ((msgId >> 8) & 0xFF));
            buffer.put((byte) (msgId & 0xFF));

            // Payload - Topic Filter
            buffer.put((byte) ((topic.length() >> 8) & 0xFF));
            buffer.put((byte) (topic.length() & 0xFF));
            buffer.put(topic.getBytes(StandardCharsets.UTF_8));
            buffer.put((byte) 0x00); // QoS 0

            // 计算并更新剩余长度
            int remainingLength = buffer.position() - 2;
            buffer.put(1, (byte) remainingLength);

            // 发送消息
            byte[] message = new byte[buffer.position()];
            buffer.rewind();
            buffer.get(message);

            webSocket.send(ByteString.of(message));

        } catch (Exception e) {
            Log.e(TAG, "订阅topic失败: " + e.getMessage());
        }
    }

    /**
     * 发布消息到呼吸机
     */
    public void publishVentilatorParameters(String parameters) {
        if (!isConnected || webSocket == null || clientId == null) {
            Log.e(TAG, "WebSocket未连接或clientId为空");
            return;
        }

        // 生成app的clientId
        String appClientId = clientId.replace("longfenkeji_", "app_");
        String topic = "/longfenkeji/app/" + appClientId + "/user/VentilatorParameters";

        publishMessage(topic, parameters);
        Log.d(TAG, "发送呼吸机参数: " + parameters + " 到topic: " + topic);
    }

    /**
     * 发布MQTT消息
     */
    private void publishMessage(String topic, String payload) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(256);

            // Fixed header
            buffer.put((byte) (MQTT_PUBLISH << 4)); // QoS 0

            // Variable header - Topic Name
            buffer.put((byte) ((topic.length() >> 8) & 0xFF));
            buffer.put((byte) (topic.length() & 0xFF));
            buffer.put(topic.getBytes(StandardCharsets.UTF_8));

            // Payload
            buffer.put(payload.getBytes(StandardCharsets.UTF_8));

            // 计算并更新剩余长度
            int remainingLength = buffer.position() - 2;
            buffer.put(1, (byte) remainingLength);

            // 发送消息
            byte[] message = new byte[buffer.position()];
            buffer.rewind();
            buffer.get(message);

            webSocket.send(ByteString.of(message));

        } catch (Exception e) {
            Log.e(TAG, "发布消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送心跳
     */
    public void sendPingRequest() {
        if (webSocket != null && isConnected) {
            byte[] pingRequest = {(byte) (MQTT_PINGREQ << 4), 0x00};
            webSocket.send(ByteString.of(pingRequest));
            Log.d(TAG, "发送心跳请求");
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (webSocket != null) {
            // 发送MQTT断开消息
            byte[] disconnectMessage = {(byte) (MQTT_DISCONNECT << 4), 0x00};
            webSocket.send(ByteString.of(disconnectMessage));

            webSocket.close(1000, "主动断开");
            webSocket = null;
        }
        isConnected = false;
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return isConnected && webSocket != null;
    }
}