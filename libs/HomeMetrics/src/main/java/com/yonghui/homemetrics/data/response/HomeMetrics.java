package com.yonghui.homemetrics.data.response;

import java.util.List;

/**
 * Created by CANC on 2017/4/6.
 */

public class HomeMetrics {

    /**
     * period : 201605
     * xaxis_order : 201605
     * head : 销售额
     */

    private String period;
    private String xaxis_order;
    private String head;
    public List<Product> products;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getXaxis_order() {
        return xaxis_order;
    }

    public void setXaxis_order(String xaxis_order) {
        this.xaxis_order = xaxis_order;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }
}
