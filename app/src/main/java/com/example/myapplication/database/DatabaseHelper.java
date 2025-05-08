package com.example.myapplication.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "health_app.db";
    private static final int DATABASE_VERSION = 1;

    // user table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    // 测量记录表
    public static final String TABLE_MEASUREMENTS = "measurements";
    public static final String COLUMN_MEASUREMENT_ID = "measurement_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_SYSTOLIC = "systolic";
    public static final String COLUMN_DIASTOLIC = "diastolic";
    public static final String COLUMN_PULSE = "pulse";
    public static final String COLUMN_MEASUREMENT_DATE = "measurement_date";

    private static final String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USERNAME + " TEXT UNIQUE,"
            + COLUMN_PASSWORD + " TEXT" + ")";

    private static final String CREATE_MEASUREMENTS_TABLE = "CREATE TABLE " + TABLE_MEASUREMENTS + "("
            + COLUMN_MEASUREMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_ID + " INTEGER,"
            + COLUMN_SYSTOLIC + " INTEGER,"
            + COLUMN_DIASTOLIC + " INTEGER,"
            + COLUMN_PULSE + " INTEGER,"
            + COLUMN_MEASUREMENT_DATE + " DATETIME,"
            + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_USERS_TABLE);
            db.execSQL(CREATE_MEASUREMENTS_TABLE);
            Log.d("DatabaseHelper", "数据库表创建成功");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "创建表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEASUREMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}
