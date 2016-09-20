package com.intfocus.yh_android;

import android.content.Context;
import android.util.DisplayMetrics;

import com.intfocus.yh_android.util.URLs;
import com.readystatesoftware.viewbadger.BadgeView;

/**
 * Created by Liurl on 2016/9/20.
 */
public class RedPointView {
	private static int displayDpi; //屏幕密度

	  /*
	   * 设置应用内通知小红点参数
	   */
	public static void showRedPoint(Context context,String type, BadgeView badgeView) {
		//获取当前设备屏幕密度
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		displayDpi = dm.densityDpi;

		//根据不同屏幕显示密度设置小红点大小
		if (displayDpi < 320) {
			badgeView.setWidth(9);
			badgeView.setHeight(9);
		}
		else if (displayDpi >= 320 && displayDpi < 480) {
			badgeView.setWidth(19);
			badgeView.setHeight(19);
		}
		else if (displayDpi >= 480) {
			badgeView.setWidth(25);
			badgeView.setHeight(25);
		}

		//badgeView.setText(badgerCount);  //暂不需要计数
		badgeView.setBadgePosition(BadgeView.POSITION_TOP_RIGHT);
		switch (type) {
			case URLs.kSetting:
				badgeView.setBadgeMargin(20, 15);
				break;
			case "tab":
				badgeView.setBadgeMargin(45, 0);
				break;
			case URLs.kSettingPgyer:
			case URLs.kSettingPassword:
			case URLs.kSettingThursdaySay:
				badgeView.setBadgePosition(BadgeView.POSITION_TOP_LEFT);
				break;
			case "user":
				badgeView.setBadgeMargin(0, 5);
				break;
			default:
				badgeView.setBadgeMargin(45, 0);
				break;
		}
		badgeView.show();
	}
}
