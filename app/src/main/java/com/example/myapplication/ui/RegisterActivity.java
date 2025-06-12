package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.database.dao.UserDAO;

public class RegisterActivity extends AppCompatActivity {
    private UserDAO userDAO;
    private EditText etUsername, etPassword, etConfirmPassword, etAge, etHeight;
    private RadioGroup radioGroupSex;
    private RadioButton radioMale, radioFemale;
    private Button btnRegister;
    private TextView tvLoginLink;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        userDAO = new UserDAO(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAge = findViewById(R.id.etAge);
        etHeight = findViewById(R.id.etHeight);
        radioGroupSex = findViewById(R.id.radioGroupSex);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                // 获取性别、年龄、身高
                int sex = radioMale.isChecked() ? 1 : 0; // 1=男, 0=女
                String ageStr = etAge.getText().toString().trim();
                String heightStr = etHeight.getText().toString().trim();

                // 验证输入
                if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
                        || ageStr.isEmpty() || heightStr.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 密码长度至少6位
                if (password.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "密码长度至少为6位", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }

                int age;
                int height;
                try {
                    age = Integer.parseInt(ageStr);
                    height = Integer.parseInt(heightStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(RegisterActivity.this, "年龄和身高必须为数字", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 验证年龄和身高范围
                if (age < 10 || age > 120) {
                    Toast.makeText(RegisterActivity.this, "年龄必须在10-120岁之间", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (height < 100 || height > 250) {
                    Toast.makeText(RegisterActivity.this, "身高必须在100-250厘米之间", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 注册用户
                boolean success = userDAO.registerUser(username, password, sex, age, height);
                if (success) {
                    Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                    // 跳转到登录页面
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "注册失败，用户名可能已存在", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // 跳转到登录界面
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}