package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
    private TextView tvSex, tvAge, tvHeight;
    private Button btnChangePassword, btnClearData, btnLogout, btnEditProfile;
    private UserDAO userDAO;
    private SharedPreferences preferences;
    private int userId;
    private UserDAO.UserInfo userInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        userDAO = new UserDAO(this);
        preferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        userId = preferences.getInt("user_id", -1);

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
        tvSex = findViewById(R.id.tv_sex);
        tvAge = findViewById(R.id.tv_age);
        tvHeight = findViewById(R.id.tv_height);

        btnChangePassword = findViewById(R.id.btn_change_password);
        btnClearData = findViewById(R.id.btn_clear_data);
        btnLogout = findViewById(R.id.btn_logout);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
    }

    private void loadUserInfo() {
        // 从SharedPreferences获取当前登录用户信息
        String username = preferences.getString("username", "");
        String registerDate = preferences.getString("register_date", "");

        // 显示用户信息
        tvUsername.setText("用户名： "+username);
        tvUserId.setText("用户ID： "+userId);
        tvRegisterDate.setText("注册日期： "+registerDate);

        // 从数据库获取用户详细信息
        userInfo = userDAO.getUserInfo(userId);
        if (userInfo != null) {
            tvSex.setText(userInfo.getSex() == 1 ? "男" : "女");
            tvAge.setText(String.valueOf(userInfo.getAge()));
            tvHeight.setText(userInfo.getHeight() + " cm");
        }
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

        btnEditProfile.setOnClickListener(v -> {
            showEditProfileDialog();
        });
    }

    private void showEditProfileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        RadioGroup radioGroupSex = dialogView.findViewById(R.id.dialog_radio_sex);
        RadioButton radioMale = dialogView.findViewById(R.id.dialog_radio_male);
        RadioButton radioFemale = dialogView.findViewById(R.id.dialog_radio_female);
        EditText etAge = dialogView.findViewById(R.id.dialog_et_age);
        EditText etHeight = dialogView.findViewById(R.id.dialog_et_height);

        // 设置当前值
        if (userInfo != null) {
            if (userInfo.getSex() == 1) {
                radioMale.setChecked(true);
            } else {
                radioFemale.setChecked(true);
            }
            etAge.setText(String.valueOf(userInfo.getAge()));
            etHeight.setText(String.valueOf(userInfo.getHeight()));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("编辑个人信息")
                .setView(dialogView)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                // 获取用户输入
                int sex = radioMale.isChecked() ? 1 : 0;
                String ageStr = etAge.getText().toString().trim();
                String heightStr = etHeight.getText().toString().trim();

                // 验证输入
                if (ageStr.isEmpty() || heightStr.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                    return;
                }

                int age, height;
                try {
                    age = Integer.parseInt(ageStr);
                    height = Integer.parseInt(heightStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(ProfileActivity.this, "年龄和身高必须为数字", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 验证年龄和身高范围
                if (age < 10 || age > 120) {
                    Toast.makeText(ProfileActivity.this, "年龄必须在10-120岁之间", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (height < 100 || height > 250) {
                    Toast.makeText(ProfileActivity.this, "身高必须在100-250厘米之间", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 更新用户信息
                boolean success = userDAO.updateUserInfo(userId, sex, age, height);
                if (success) {
                    Toast.makeText(ProfileActivity.this, "个人信息更新成功", Toast.LENGTH_SHORT).show();
                    // 刷新显示
                    loadUserInfo();
                    dialog.dismiss();
                } else {
                    Toast.makeText(ProfileActivity.this, "更新失败，请重试", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
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