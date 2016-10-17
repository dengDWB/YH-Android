package com.intfocus.yh_android;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by 40284 on 2016/8/28.
 * Activity 管理类
 */
public class ActivityCollector {
    public static Stack<Activity> activityStack;

    public static void addActivity(Activity activity) {
        if (activityStack == null) {
            activityStack = new Stack<>();
        }

        for (Activity act : activityStack) {
            /*
             * 如果 Activity 列表内已经存在当前 Activity,则不重复添加。
             */
            if (activity == act) {
                return;
            }
        }
        activityStack.add(activity);
    }

    /*
     * 结束所有 Activity
     */
    public static void finishAll() {
        for (Activity activity : activityStack) {
            if (!activity.isFinishing()) {
                activity.finish();
            }
        }
        activityStack.clear();
    }
}
