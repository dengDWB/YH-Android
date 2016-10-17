package com.intfocus.yh_android;

import android.app.Activity;

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

    public static void removeActivity(Activity activity) {
        activityStack.remove(activity);
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
