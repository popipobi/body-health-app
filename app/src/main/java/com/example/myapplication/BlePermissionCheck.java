package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BlePermissionCheck {
    private static final int REQUEST_CODE_PERMISSIONS = 2025;

    private static final String[] BLE_PERMISSIONS = new String[] {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static boolean hasPerMissions(Activity activity) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S) {
            for (String permission:BLE_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(activity, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean shouldShowRationale(Activity activity) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S) {
            for (String permission:BLE_PERMISSIONS) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void showRationale(Activity activity) {
        Toast.makeText(activity, "需要打开蓝牙和位置权限才能正常使用BLE功能", Toast.LENGTH_LONG).show();
    }

    public static void requestPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    public static boolean handlePermisssionsResult(int requestCode,
                                                   @NonNull String[] permissions,
                                                   @NonNull int[] grantResults) {
        if (requestCode==REQUEST_CODE_PERMISSIONS) {
            for (int result:grantResults) {
                if (result!=PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
