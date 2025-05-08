package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;

import java.util.ArrayList;

public class BloodPressureChart {
    private LineChart lineChart;
    private ArrayList<Entry> bloodPressureData;
    private LineDataSet lineDataSet;

    public BloodPressureChart(Context context, LineChart chart) {
        this.lineChart = chart;
        this.bloodPressureData = new ArrayList<>();

        // 初始化数据集
        lineDataSet = new LineDataSet(bloodPressureData, "");
        lineDataSet.setColor(Color.RED);
        lineDataSet.setValueTextColor(Color.BLACK);
        lineDataSet.setValueTextSize(0f);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setCircleRadius(3f);

        setupChart();
    }

    // 设置图表
    private void setupChart() {
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(5, false);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(200f);

        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    // 更新图表数据
    public void updateChartData(float systolic) {
        int xIndex = bloodPressureData.size();  // X轴点数递增
        bloodPressureData.add(new Entry(xIndex, systolic));

        lineDataSet.notifyDataSetChanged();
        lineChart.getData().notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();

        lineChart.setVisibleXRangeMaximum(10f);
        lineChart.moveViewToX(xIndex - 10);
    }

    // 重置图表数据
    public void resetChartData() {
        LineData data = lineChart.getData();
        if (data != null) {
            for (IDataSet set:data.getDataSets()) set.clear();
        }
        data.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }
}
