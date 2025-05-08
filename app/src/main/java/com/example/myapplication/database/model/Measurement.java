package com.example.myapplication.database.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Measurement {
    private int id;
    private int userId;
    private int systolic;
    private int diastolic;
    private int pulse;
    private String date;

    public Measurement(int id, int userId, int systolic, int diastolic, int pulse, String date) {
        this.id = id;
        this.userId = userId;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.pulse = pulse;
        this.date = date;
    }

    public int getId() { return  id; }
    public int getUserId() { return userId; }
    public int getSystolic() { return systolic; }
    public int getDiastolic() { return  diastolic; }
    public int getPulse() { return pulse; }
    public String getDate() { return date; }

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
