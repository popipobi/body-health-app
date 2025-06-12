package com.example.myapplication.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.database.model.BodyFatMeasurement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BodyFatMeasurementDAO {
    private static final String TAG = "BodyFatMeasurementDAO";
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    public BodyFatMeasurementDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    // 保存体脂测量数据
    public long saveBodyFatMeasurement(int userId, float weight, double bmi, double bodyFatRate,
                                       double bodyFatMass, double waterRate, double proteinRate,
                                       double muscleRate, double muscleMass, double boneMass,
                                       int visceralFat, int bmr, int bodyAge, double idealWeight) {
        open();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_WEIGHT, weight);
        values.put(DatabaseHelper.COLUMN_BMI, bmi);
        values.put(DatabaseHelper.COLUMN_BODY_FAT_RATE, bodyFatRate);
        values.put(DatabaseHelper.COLUMN_BODY_FAT_MASS, bodyFatMass);
        values.put(DatabaseHelper.COLUMN_WATER_RATE, waterRate);
        values.put(DatabaseHelper.COLUMN_PROTEIN_RATE, proteinRate);
        values.put(DatabaseHelper.COLUMN_MUSCLE_RATE, muscleRate);
        values.put(DatabaseHelper.COLUMN_MUSCLE_MASS, muscleMass);
        values.put(DatabaseHelper.COLUMN_BONE_MASS, boneMass);
        values.put(DatabaseHelper.COLUMN_VISCERAL_FAT, visceralFat);
        values.put(DatabaseHelper.COLUMN_BMR, bmr);
        values.put(DatabaseHelper.COLUMN_BODY_AGE, bodyAge);
        values.put(DatabaseHelper.COLUMN_IDEAL_WEIGHT, idealWeight);
        values.put(DatabaseHelper.COLUMN_MEASUREMENT_DATE_BF, getCurrentDateTime());

        long id = database.insert(DatabaseHelper.TABLE_BODY_FAT_MEASUREMENTS, null, values);
        close();

        return id;
    }

    // 获取用户所有体脂测量记录
    public List<BodyFatMeasurement> getUserBodyFatMeasurements(int userId) {
        open();

        List<BodyFatMeasurement> measurements = new ArrayList<>();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_BODY_FAT_MEASUREMENTS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?" +
                " ORDER BY " + DatabaseHelper.COLUMN_MEASUREMENT_DATE_BF + " DESC";
        Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BODY_FAT_ID));
                float weight = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT));
                double bmi = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BMI));
                double bodyFatRate = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BODY_FAT_RATE));
                double bodyFatMass = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BODY_FAT_MASS));
                double waterRate = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WATER_RATE));
                double proteinRate = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROTEIN_RATE));
                double muscleRate = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MUSCLE_RATE));
                double muscleMass = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MUSCLE_MASS));
                double boneMass = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BONE_MASS));
                int visceralFat = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_VISCERAL_FAT));
                int bmr = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BMR));
                int bodyAge = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BODY_AGE));
                double idealWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IDEAL_WEIGHT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MEASUREMENT_DATE_BF));

                BodyFatMeasurement measurement = new BodyFatMeasurement(id, userId, weight, bmi, bodyFatRate,
                        bodyFatMass, waterRate, proteinRate, muscleRate, muscleMass, boneMass,
                        visceralFat, bmr, bodyAge, idealWeight, date);
                measurements.add(measurement);
            } while (cursor.moveToNext());

            cursor.close();
        }
        close();
        return measurements;
    }

    // 删除用户所有体脂测量记录
    public int deleteUserBodyFatMeasurements(int userId) {
        open();

        String whereClause = DatabaseHelper.COLUMN_USER_ID + " = ?";
        String[] whereArgs = {String.valueOf(userId)};

        int rowsDeleted = database.delete(DatabaseHelper.TABLE_BODY_FAT_MEASUREMENTS, whereClause, whereArgs);
        close();

        return rowsDeleted;
    }

    public boolean deleteBodyFatMeasurement(int measurementId) {
        open();

        String whereClause = DatabaseHelper.COLUMN_BODY_FAT_ID + " = ?";
        String[] whereArgs = {String.valueOf(measurementId)};

        int rowsDeleted = database.delete(DatabaseHelper.TABLE_BODY_FAT_MEASUREMENTS, whereClause, whereArgs);
        close();

        return rowsDeleted > 0;
    }

    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}