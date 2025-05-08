package com.example.myapplication;

import java.util.Locale;

public class ByteUtils {
    // 将byte数组转换为16进制字符串
    public static String byteArrayToHexString(byte[] array) {
        if (array==null) return "";
        StringBuilder buffer = new StringBuilder();
        for (byte b:array) buffer.append(byteToHex(b));
        return buffer.toString();
    }
    // 将单byte转换为16进制字符
    public static String byteToHex(byte b) {
        String hex = Integer.toHexString(b&0xFF);
        return hex.length()==1?"0"+hex:hex.toUpperCase(Locale.getDefault());
    }

    // 同时有16进制和无符号10进制
    public static String formatByteArray(byte[] array) {
        if (array==null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b:array) {
            int unsigned = b&0xFF;
            sb.append(String.format("%02X(%d)", unsigned, unsigned));
//            sb.append(String.format("%d", unsigned));

        }
        return sb.toString().trim();
    }

    // 解析最后蹦出一串血压数据
    public static HealthData parseHealthData(byte[] data) {
        if (data==null || data.length<17) {
            throw new IllegalArgumentException("数据长度不足");
        }

        int systolic = data[1] & 0xFF;// 高压
        int diastolic = data[3] & 0xFF;// 低压
        int pulse = data[14] & 0xFF;// 脉搏

        return new HealthData(systolic, diastolic, pulse);
    }

    // 最后蹦出一串血压数据
    public static class HealthData {
        private final int systolic;
        private final int diastolic;
        private final int pulse;

        public HealthData(int systolic, int diastolic, int pulse) {
            this.systolic = systolic;
            this.diastolic = diastolic;
            this.pulse = pulse;
        }

        public int getSystolic() {
            return systolic;
        }

        public int getDiastolic() {
            return diastolic;
        }

        public int getPulse() {
            return pulse;
        }
    }
}
