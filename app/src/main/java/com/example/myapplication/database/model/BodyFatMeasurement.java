package com.example.myapplication.database.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BodyFatMeasurement {
    private int id;
    private int userId;
    private float weight;
    private double bmi;
    private double bodyFatRate;
    private double bodyFatMass;
    private double waterRate;
    private double proteinRate;
    private double muscleRate;
    private double muscleMass;
    private double boneMass;
    private int visceralFat;
    private int bmr;
    private int bodyAge;
    private double idealWeight;
    private String date;

    public BodyFatMeasurement(int id, int userId, float weight, double bmi, double bodyFatRate,
                              double bodyFatMass, double waterRate, double proteinRate,
                              double muscleRate, double muscleMass, double boneMass,
                              int visceralFat, int bmr, int bodyAge, double idealWeight,
                              String date) {
        this.id = id;
        this.userId = userId;
        this.weight = weight;
        this.bmi = bmi;
        this.bodyFatRate = bodyFatRate;
        this.bodyFatMass = bodyFatMass;
        this.waterRate = waterRate;
        this.proteinRate = proteinRate;
        this.muscleRate = muscleRate;
        this.muscleMass = muscleMass;
        this.boneMass = boneMass;
        this.visceralFat = visceralFat;
        this.bmr = bmr;
        this.bodyAge = bodyAge;
        this.idealWeight = idealWeight;
        this.date = date;
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public float getWeight() { return weight; }
    public double getBmi() { return bmi; }
    public double getBodyFatRate() { return bodyFatRate; }
    public double getBodyFatMass() { return bodyFatMass; }
    public double getWaterRate() { return waterRate; }
    public double getProteinRate() { return proteinRate; }
    public double getMuscleRate() { return muscleRate; }
    public double getMuscleMass() { return muscleMass; }
    public double getBoneMass() { return boneMass; }
    public int getVisceralFat() { return visceralFat; }
    public int getBmr() { return bmr; }
    public int getBodyAge() { return bodyAge; }
    public double getIdealWeight() { return idealWeight; }
    public String getDate() { return date; }

    // 格式化日期显示
    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            return outputFormat.format(parsedDate);
        } catch (ParseException e) {
            return date;
        }
    }
}