package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class BodyFatDetailActivity extends AppCompatActivity {
    // UI组件
    private TextView tvDate;
    private TextView tvWeight;
    private TextView tvBmi;
    private TextView tvBodyAge;
    private TextView tvBodyFatRate;
    private TextView tvBodyFatMass;
    private TextView tvMuscleRate;
    private TextView tvMuscleMass;
    private TextView tvWaterRate;
    private TextView tvProteinRate;
    private TextView tvBoneMass;
    private TextView tvVisceralFat;
    private TextView tvBmr;
    private TextView tvIdealWeight;
    private ImageButton btnBack;

    // Intent extras keys
    public static final String EXTRA_DATE = "date";
    public static final String EXTRA_WEIGHT = "weight";
    public static final String EXTRA_BMI = "bmi";
    public static final String EXTRA_BODY_FAT_RATE = "body_fat_rate";
    public static final String EXTRA_BODY_FAT_MASS = "body_fat_mass";
    public static final String EXTRA_WATER_RATE = "water_rate";
    public static final String EXTRA_PROTEIN_RATE = "protein_rate";
    public static final String EXTRA_MUSCLE_RATE = "muscle_rate";
    public static final String EXTRA_MUSCLE_MASS = "muscle_mass";
    public static final String EXTRA_BONE_MASS = "bone_mass";
    public static final String EXTRA_VISCERAL_FAT = "visceral_fat";
    public static final String EXTRA_BMR = "bmr";
    public static final String EXTRA_BODY_AGE = "body_age";
    public static final String EXTRA_IDEAL_WEIGHT = "ideal_weight";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_body_fat_detail);

        initViews();
        loadData();

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvDate = findViewById(R.id.tv_detail_date);
        tvWeight = findViewById(R.id.tv_detail_weight);
        tvBmi = findViewById(R.id.tv_detail_bmi);
        tvBodyAge = findViewById(R.id.tv_detail_body_age);
        tvBodyFatRate = findViewById(R.id.tv_detail_body_fat_rate);
        tvBodyFatMass = findViewById(R.id.tv_detail_body_fat_mass);
        tvMuscleRate = findViewById(R.id.tv_detail_muscle_rate);
        tvMuscleMass = findViewById(R.id.tv_detail_muscle_mass);
        tvWaterRate = findViewById(R.id.tv_detail_water_rate);
        tvProteinRate = findViewById(R.id.tv_detail_protein_rate);
        tvBoneMass = findViewById(R.id.tv_detail_bone_mass);
        tvVisceralFat = findViewById(R.id.tv_detail_visceral_fat);
        tvBmr = findViewById(R.id.tv_detail_bmr);
        tvIdealWeight = findViewById(R.id.tv_detail_ideal_weight);
        btnBack = findViewById(R.id.btn_back);
    }

    private void loadData() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }

        // 获取并显示所有数据
        String date = extras.getString(EXTRA_DATE, "");
        float weight = extras.getFloat(EXTRA_WEIGHT, 0);
        double bmi = extras.getDouble(EXTRA_BMI, 0);
        double bodyFatRate = extras.getDouble(EXTRA_BODY_FAT_RATE, 0);
        double bodyFatMass = extras.getDouble(EXTRA_BODY_FAT_MASS, 0);
        double waterRate = extras.getDouble(EXTRA_WATER_RATE, 0);
        double proteinRate = extras.getDouble(EXTRA_PROTEIN_RATE, 0);
        double muscleRate = extras.getDouble(EXTRA_MUSCLE_RATE, 0);
        double muscleMass = extras.getDouble(EXTRA_MUSCLE_MASS, 0);
        double boneMass = extras.getDouble(EXTRA_BONE_MASS, 0);
        int visceralFat = extras.getInt(EXTRA_VISCERAL_FAT, 0);
        int bmr = extras.getInt(EXTRA_BMR, 0);
        int bodyAge = extras.getInt(EXTRA_BODY_AGE, 0);
        double idealWeight = extras.getDouble(EXTRA_IDEAL_WEIGHT, 0);

        // 显示数据
        tvDate.setText(date);
        tvWeight.setText(String.format("%.1f", weight));
        tvBmi.setText(String.format("%.1f", bmi));
        tvBodyAge.setText(String.valueOf(bodyAge));
        tvBodyFatRate.setText(String.format("%.1f%%", bodyFatRate));
        tvBodyFatMass.setText(String.format("%.1f kg", bodyFatMass));
        tvMuscleRate.setText(String.format("%.1f%%", muscleRate));
        tvMuscleMass.setText(String.format("%.1f kg", muscleMass));
        tvWaterRate.setText(String.format("%.1f%%", waterRate));
        tvProteinRate.setText(String.format("%.1f%%", proteinRate));
        tvBoneMass.setText(String.format("%.1f kg", boneMass));
        tvVisceralFat.setText(String.valueOf(visceralFat));
        tvBmr.setText(String.format("%d kcal", bmr));
        tvIdealWeight.setText(String.format("%.1f kg", idealWeight));
    }
}