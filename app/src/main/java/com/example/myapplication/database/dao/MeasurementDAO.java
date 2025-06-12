package com.example.myapplication.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.database.DatabaseHelper;
import com.example.myapplication.database.model.Measurement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MeasurementDAO {
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    public MeasurementDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    // 保存所有测量记录
    public long saveMeasurement(int userId, int systolic, int diastolic, int pulse) {
        open();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_SYSTOLIC, systolic);
        values.put(DatabaseHelper.COLUMN_DIASTOLIC, diastolic);
        values.put(DatabaseHelper.COLUMN_PULSE, pulse);
        values.put(DatabaseHelper.COLUMN_MEASUREMENT_DATE, getCurrentDateTime());

        long id = database.insert(DatabaseHelper.TABLE_MEASUREMENTS, null, values);
        close();

        return id;
    }

    // 获取用户所有测量记录
    public List<Measurement> getUserMeasurements(int userId) {
        open();

        List<Measurement> measurements = new ArrayList<>();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_MEASUREMENTS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?" +
                " ORDER BY " + DatabaseHelper.COLUMN_MEASUREMENT_DATE + " DESC";
        Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MEASUREMENT_ID);
            int systolicIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SYSTOLIC);
            int diastolicIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DIASTOLIC);
            int pulseIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PULSE);
            int dateIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MEASUREMENT_DATE);

            do {
                int id = cursor.getInt(idIndex);
                int systolic = cursor.getInt(systolicIndex);
                int diastolic = cursor.getInt(diastolicIndex);
                int pulse = cursor.getInt(pulseIndex);
                String date = cursor.getString(dateIndex);

                Measurement measurement = new Measurement(id, userId, systolic, diastolic, pulse, date);
                measurements.add(measurement);
            } while (cursor.moveToNext());

            cursor.close();
        }
        close();
        return measurements;
    }

    // 删除用户所有测量记录
    public int deleteUserMeasurements(int userId) {
        open();

        String whereClause = DatabaseHelper.COLUMN_USER_ID + " = ?";
        String[] whereArgs = {String.valueOf(userId)};

        int rowsDeleted = database.delete(DatabaseHelper.TABLE_MEASUREMENTS, whereClause, whereArgs);
        close();

        return rowsDeleted;
    }

    public boolean deleteMeasurement(int measurementId) {
        open();

        String whereClause = DatabaseHelper.COLUMN_MEASUREMENT_ID + " = ?";
        String[] whereArgs = {String.valueOf(measurementId)};

        int rowsDeleted = database.delete(DatabaseHelper.TABLE_MEASUREMENTS, whereClause, whereArgs);
        close();

        return rowsDeleted > 0;
    }

    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}
