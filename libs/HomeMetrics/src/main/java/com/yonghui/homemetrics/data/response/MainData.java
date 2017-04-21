package com.yonghui.homemetrics.data.response;

import java.math.BigDecimal;

/**
 * Created by CANC on 2017/4/6.
 */

public class MainData {


    /**
     * data : 0.6069000095129014
     * format : float
     */

    private double data;
    private String format;

    public double getData() {
//        BigDecimal data2 = new BigDecimal(data);
//        data2 = data2.setScale(2, BigDecimal.ROUND_HALF_UP);
//        return data2.doubleValue();
        return (double)Math.round(data*100)/100;
//        return data;
    }

    public void setData(double data) {
        this.data = data;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
