package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.BottomNavigationHelper;
import com.example.myapplication.R;
import com.example.myapplication.database.dao.UserDAO;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvUsername, tvUserId, tvRegisterDate;
    private Button btnChangePassword, btnClearData, btnLogout;
    private UserDAO userDAO;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        userDAO = new UserDAO(this);
        preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        BottomNavigationHelper.setupBottomNavigation(this, bottomNavigationView, R.id.navigation_profile);

        initViews();// 初始化视图
        loadUserInfo();// 加载用户信息
        setupButtonListeners(); // 设置按钮监听器
    }

    private void initViews() {
        tvUsername = findViewById(R.id.tv_username);
        tvUserId = findViewById(R.id.tv_user_id);
        tvRegisterDate = findViewById(R.id.tv_register_date);

        btnChangePassword = findViewById(R.id.btn_change_password);
        btnClearData = findViewById(R.id.btn_clear_data);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void loadUserInfo() {
        // 从SharedPreferences获取当前登录用户信息
        String username = preferences.getString("username", "");
        int userId = preferences.getInt("user_id", -1);
        String registerDate = preferences.getString("register_date", "");

        // 显示用户信息
        tvUsername.setText("用户名： "+username);
        tvUserId.setText("用户ID： "+userId);
        tvRegisterDate.setText("注册日期： "+registerDate);
    }

    private void setupButtonListeners() {
        btnChangePassword.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        btnClearData.setOnClickListener(v -> {
            showClearDataConfirmationDialog();
        });

        btnLogout.setOnClickListener(v -> {
            logout();
        });
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText etOldPassword = dialogView.findViewById(R.id.et_old_password);
        EditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修改密码")
                .setView(dialogView)
                .setPositiveButton("确认", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String oldPassword = etOldPassword.getText().toString();
                String newPassword = etNewPassword.getText().toString();
                String confirmPassword = etConfirmPassword.getText().toString();

                if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(ProfileActivity.this, "新密码与确认密码不匹配", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 验证旧密码并更新密码
                if (verifyAndUpdatePassword(oldPassword, newPassword)) {
                    dialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "密码修改成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfileActivity.this, "原密码不正确", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    private boolean verifyAndUpdatePassword(String oldPassword, String newPassword) {
        String username = preferences.getString("username", "");

        if (userDAO.checkLogin(username, oldPassword)) {
            return userDAO.updatePassword(username, newPassword);// 更新密码
        }

        return false;
    }

    private void showClearDataConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("清除数据")
                .setMessage("确定要清除所有用户数据吗？此操作将删除您的账户信息和所有测量记录，且无法恢复。")
                .setPositiveButton("确认", (dialog, which) -> {
                    clearAllUserData();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void clearAllUserData() {
        int userId = preferences.getInt("user_id", -1);

        if (userId != -1) {
            boolean success = userDAO.deleteUser(userId);

            if (success) {
                preferences.edit().clear().apply();
                Toast.makeText(this, "所有用户数据已清除", Toast.LENGTH_SHORT).show();

                // 跳转登录
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "清除数据失败，请重试", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 退出登录
    private void logout() {
        // 清除登录状态
        preferences.edit()
                .putBoolean("is_logged_in", false)
                .apply();

        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();

        // 跳转到登录页面并清除Activity堆栈
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // 确保当前Activity被销毁
    }
}