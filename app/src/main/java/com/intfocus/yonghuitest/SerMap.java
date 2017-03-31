package com.intfocus.yonghuitest;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by dengwenbin on 17/3/30.
 */

public class SerMap implements Serializable {

    public Map<String, String> map;
    public  SerMap(){

    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }
}
