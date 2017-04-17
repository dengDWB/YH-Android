package com.yonghui.homemetrics.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by CANC on 2017/4/6.
 */

public class Utils {

    /**
     * 从asset路径下读取对应文件转String输出
     *
     * @param mContext
     * @return
     */
    public static String getJson(Context mContext) {
        String newString = "";
        try {
            InputStreamReader isr = new InputStreamReader(mContext.getAssets().open("report_data.json"), "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            br.close();
            isr.close();
//            JSONObject testjson = new JSONObject(builder.toString());//builder读取了JSON中的数据。
            newString = builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newString;
    }


    /**
     * 获得屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }
}
