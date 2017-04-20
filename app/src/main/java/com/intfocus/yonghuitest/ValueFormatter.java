package com.intfocus.yonghuitest;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

/**
 * Created by CANC on 2017/4/10.
 */
public class ValueFormatter implements IAxisValueFormatter {

    protected String[] mValue = new String[]{
            "0", "W1", "W2", "W3", "W4",
            "W5", "W6", "W7", "W8",
            "W9", "W10", "W11", "W12",
            "W13", "W14", "W15", "W16",
            "W17", "W18", "W19", "W20",
            "W21", "W22", "W23", "W24"
    };

    public void setmValues(String[] mValue) {
        this.mValue = mValue;
    }

    private BarLineChartBase<?> chart;

    public ValueFormatter(BarLineChartBase<?> chart) {
        this.chart = chart;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        int days = (int) value;
        String valueName = mValue[days];
        return days == 0 ? "" : valueName;
    }
}
