package com.intfocus.yonghuitest.util;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.intfocus.yonghuitest.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuruilin on 2017/3/30.
 */

public class WidgetUtil {
    public static Toast toast;

    public static void showToastShort(Context context, String info) {
        try {
            if (null == toast) {
                toast = Toast.makeText(context, info, Toast.LENGTH_SHORT);
            } else {
                toast.setText(info); //若当前已有 Toast 在显示,则直接修改当前 Toast 显示的内容
            }
            toast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showToastLong(Context context, String info) {
        try {
            if (null == toast) {
                toast = Toast.makeText(context, info, Toast.LENGTH_LONG);
            } else {
                toast.setText(info); //若当前已有 Toast 在显示,则直接修改当前 Toast 显示的内容
            }
            toast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * 用户编号
     */
    public static void initUserIDColorView(View view, int userID) {
        List<ImageView> colorViews = new ArrayList<>();
        colorViews.add((ImageView) view.findViewById(R.id.colorView0));
        colorViews.add((ImageView) view.findViewById(R.id.colorView1));
        colorViews.add((ImageView) view.findViewById(R.id.colorView2));
        colorViews.add((ImageView) view.findViewById(R.id.colorView3));
        colorViews.add((ImageView) view.findViewById(R.id.colorView4));
        initColorView(colorViews, userID);
    }

    public static void initColorView(List<ImageView> colorViews, int userID) {
        String[] colors = {"#00ffff", "#ffcd0a", "#fd9053", "#dd0929", "#016a43", "#9d203c", "#093db5", "#6a3906", "#192162", "#000000"};
        String userIDStr = String.format("%d", userID);
        int numDiff = colorViews.size() - userIDStr.length();
        numDiff = numDiff < 0 ? 0 : numDiff;
        for (int i = 0; i < colorViews.size(); i++) {
            int colorIndex = 0;
            if (i >= numDiff) {
                colorIndex = Character.getNumericValue(userIDStr.charAt(i - numDiff));
            }
            colorViews.get(i).setBackgroundColor(Color.parseColor(colors[colorIndex]));
        }
    }
}
