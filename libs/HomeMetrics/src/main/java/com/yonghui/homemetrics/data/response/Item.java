package com.yonghui.homemetrics.data.response;

/**
 * Created by CANC on 2017/4/6.
 */

public class Item {


    /**
     * name : 综合毛利
     */

    private String name;

    public MainData main_data;

    public SubData sub_data;

    public State state;

    public boolean isSelected;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
