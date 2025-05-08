package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.ui.HistoryActivity;
import com.example.myapplication.ui.LoginActivity;
import com.example.myapplication.ui.MainActivity;
import com.example.myapplication.ui.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomNavigationHelper {
    /**
     * 设置底部导航栏
     * @param activity 当前活动
     * @param navigationView 底部导航视图
     * @param currentItemId 当前选中的菜单项ID
     */
    public static void setupBottomNavigation(AppCompatActivity activity,
                                             BottomNavigationView navigationView,
                                             int currentItemId) {
        // 检查登录状态
        SharedPreferences preferences = activity.getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("is_logged_in", false);

        if (!isLoggedIn) {
            // 用户未登录，跳转到登录页面
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
            return;
        }

        // 设置当前页为选中状态
        navigationView.setSelectedItemId(currentItemId);

        navigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId()==currentItemId) {
                return true;
            }
            Intent intent = null;

            int itemId = item.getItemId();
            if (itemId==R.id.navigation_measure) {
                intent = new Intent(activity, MainActivity.class);
            } else if (itemId==R.id.navigation_history) {
                intent = new Intent(activity, HistoryActivity.class);
            } else if (itemId==R.id.navigation_profile) {
                intent = new Intent(activity, ProfileActivity.class);
            }

            if (intent!=null) {
                activity.startActivity(intent);
                activity.finish();
                return true;
            }

            return false;
        });
    }
}
