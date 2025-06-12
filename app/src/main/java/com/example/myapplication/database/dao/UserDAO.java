package com.example.myapplication.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.myapplication.database.DatabaseHelper;

import java.security.MessageDigest;

public class UserDAO {
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    public UserDAO(Context context) {
//        dbHelper = new DatabaseHelper(context); 记得解开我，我就一行代码
        // for test
        try {
            dbHelper = new DatabaseHelper(context);
            Log.d("userDAO", "UserDAO初始化成功");
        } catch (Exception e) {
            Log.e("userDAO", "UserDAO初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void open() throws SQLException {
//        database = dbHelper.getWritableDatabase(); 记得打开我就一行代码
        // for test
        try {
            database = dbHelper.getWritableDatabase();
            Log.d("userDAO", "数据库打开成功");
        } catch (Exception e) {
            Log.e("userDAO", "打开数据库时出错: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("数据库打开失败: " + e.getMessage());
        }
    }

    public void close() {
        dbHelper.close();
    }

    // register
    public boolean registerUser(String username, String password, int sex, int age, int height) {
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_PASSWORD, hashPassword(password));
        values.put(DatabaseHelper.COLUMN_SEX, sex);
        values.put(DatabaseHelper.COLUMN_AGE, age);
        values.put(DatabaseHelper.COLUMN_HEIGHT, height);

        try {
            long userId = database.insert(DatabaseHelper.TABLE_USERS, null, values);
            close();
            return userId > 0;
        } catch (Exception e) {
            close();
            return false;
        }
    }

    // 更新用户基本信息
    public boolean updateUserInfo(int userId, int sex, int age, int height) {
        open();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_SEX, sex);
        values.put(DatabaseHelper.COLUMN_AGE, age);
        values.put(DatabaseHelper.COLUMN_HEIGHT, height);

        String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(userId)};

        int rowsAffected = database.update(DatabaseHelper.TABLE_USERS, values, whereClause, whereArgs);
        close();

        return rowsAffected > 0;
    }

    // 获取用户基本信息
    public UserInfo getUserInfo(int userId) {
        open();

        UserInfo userInfo = null;
        String[] columns = {
                DatabaseHelper.COLUMN_USERNAME,
                DatabaseHelper.COLUMN_SEX,
                DatabaseHelper.COLUMN_AGE,
                DatabaseHelper.COLUMN_HEIGHT
        };
        String selection = DatabaseHelper.COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};

        Cursor cursor = database.query(
                DatabaseHelper.TABLE_USERS,
                columns,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME));
            int sex = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SEX));
            int age = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AGE));
            int height = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HEIGHT));

            userInfo = new UserInfo(username, sex, age, height);
            cursor.close();
        }

        close();
        return userInfo;
    }

    // 用户基本信息类
    public static class UserInfo {
        private String username;
        private int sex;
        private int age;
        private int height;

        public UserInfo(String username, int sex, int age, int height) {
            this.username = username;
            this.sex = sex;
            this.age = age;
            this.height = height;
        }

        public String getUsername() { return username; }
        public int getSex() { return sex; }
        public int getAge() { return age; }
        public int getHeight() { return height; }
    }

    public boolean checkLogin(String username, String password) {
        open();

        String[] columns = {DatabaseHelper.COLUMN_ID};
        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?" + " AND " + DatabaseHelper.COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {username, hashPassword(password)};

        Cursor cursor = database.query(DatabaseHelper.TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        close();

        return count>0;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public boolean updatePassword(String username, String newPassword) {
        open();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PASSWORD, hashPassword(newPassword));

        String whereClause = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] whereArgs = {username};

        int rowAffected = database.update(DatabaseHelper.TABLE_USERS, values, whereClause, whereArgs);
        close();

        return rowAffected > 0;
    }

    public boolean deleteUser(int userId) {
        open();

        // 删除用户测量记录
        String measurementWhereClause = DatabaseHelper.COLUMN_USER_ID + " = ?";
        String[] whereArgs = {String.valueOf(userId)};
        database.delete(DatabaseHelper.TABLE_MEASUREMENTS, measurementWhereClause, whereArgs);

        // 删除用户记录
        String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
        int rowsAffected = database.delete(DatabaseHelper.TABLE_USERS, whereClause, whereArgs);

        close();

        return  rowsAffected > 0;
    }

    private boolean tableExists(String tableName) {
        Cursor cursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public int getUserId(String username) {
        try {
            open();

            String[] columns = {DatabaseHelper.COLUMN_ID};
            String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
            String[] selectionArgs = {username};

            Cursor cursor = database.query(DatabaseHelper.TABLE_USERS, columns, selection,selectionArgs, null, null, null);

            int userId = -1;
            if (cursor!=null && cursor.moveToFirst()) {
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                cursor.close();
            }
            close();
            return userId;
        } catch (Exception e) {
            Log.e("userDAO", "获取用户ID时出错：" + e.getMessage());
            close();
            return -1;
        }
    }
}