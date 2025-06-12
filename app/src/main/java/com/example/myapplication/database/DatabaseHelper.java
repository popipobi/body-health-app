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

    public static final String TABLE_BODY_FAT_MEASUREMENTS = "body_fat_measurements";
    public static final String COLUMN_BODY_FAT_ID = "body_fat_id";
    public static final String COLUMN_WEIGHT = "weight";
    public static final String COLUMN_BMI = "bmi";
    public static final String COLUMN_BODY_FAT_RATE = "body_fat_rate";
    public static final String COLUMN_BODY_FAT_MASS = "body_fat_mass";
    public static final String COLUMN_WATER_RATE = "water_rate";
    public static final String COLUMN_PROTEIN_RATE = "protein_rate";
    public static final String COLUMN_MUSCLE_RATE = "muscle_rate";
    public static final String COLUMN_MUSCLE_MASS = "muscle_mass";
    public static final String COLUMN_BONE_MASS = "bone_mass";
    public static final String COLUMN_VISCERAL_FAT = "visceral_fat";
    public static final String COLUMN_BMR = "bmr";
    public static final String COLUMN_BODY_AGE = "body_age";
    public static final String COLUMN_IDEAL_WEIGHT = "ideal_weight";
    public static final String COLUMN_MEASUREMENT_DATE_BF = "measurement_date";

    public static final String COLUMN_SEX = "sex";
    public static final String COLUMN_AGE = "age";
    public static final String COLUMN_HEIGHT = "height";

    private static final String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USERNAME + " TEXT UNIQUE,"
            + COLUMN_PASSWORD + " TEXT,"
            + COLUMN_SEX + " INTEGER DEFAULT 1," // 默认为男性(1)，女性为0
            + COLUMN_AGE + " INTEGER DEFAULT 30,"
            + COLUMN_HEIGHT + " INTEGER DEFAULT 170"
            + ")";

    private static final String CREATE_MEASUREMENTS_TABLE = "CREATE TABLE " + TABLE_MEASUREMENTS + "("
            + COLUMN_MEASUREMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_ID + " INTEGER,"
            + COLUMN_SYSTOLIC + " INTEGER,"
            + COLUMN_DIASTOLIC + " INTEGER,"
            + COLUMN_PULSE + " INTEGER,"
            + COLUMN_MEASUREMENT_DATE + " DATETIME,"
            + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")"
            + ")";

    // 创建体脂测量表的SQL语句
    private static final String CREATE_BODY_FAT_MEASUREMENTS_TABLE = "CREATE TABLE " + TABLE_BODY_FAT_MEASUREMENTS + "("
            + COLUMN_BODY_FAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_ID + " INTEGER,"
            + COLUMN_WEIGHT + " REAL,"
            + COLUMN_BMI + " REAL,"
            + COLUMN_BODY_FAT_RATE + " REAL,"
            + COLUMN_BODY_FAT_MASS + " REAL,"
            + COLUMN_WATER_RATE + " REAL,"
            + COLUMN_PROTEIN_RATE + " REAL,"
            + COLUMN_MUSCLE_RATE + " REAL,"
            + COLUMN_MUSCLE_MASS + " REAL,"
            + COLUMN_BONE_MASS + " REAL,"
            + COLUMN_VISCERAL_FAT + " INTEGER,"
            + COLUMN_BMR + " INTEGER,"
            + COLUMN_BODY_AGE + " INTEGER,"
            + COLUMN_IDEAL_WEIGHT + " REAL,"
            + COLUMN_MEASUREMENT_DATE_BF + " DATETIME,"
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
            db.execSQL(CREATE_BODY_FAT_MEASUREMENTS_TABLE);
            Log.d("DatabaseHelper", "数据库表创建成功");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "创建表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BODY_FAT_MEASUREMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEASUREMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}
