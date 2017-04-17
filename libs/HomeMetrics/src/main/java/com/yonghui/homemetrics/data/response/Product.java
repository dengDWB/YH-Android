package com.yonghui.homemetrics.data.response;

import java.util.List;

/**
 * Created by CANC on 2017/4/6.
 */

public class Product {

    /**
     * name : 加工部
     */

    private String name;

    public List<Item> items;

    public boolean isSelected;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
