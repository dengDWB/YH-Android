package com.intfocus.yh_android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.HttpUtil;
import com.intfocus.yh_android.util.URLs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Liurl on 2016/8/16.
 */
public class NotifitionService extends Service {
	private JSONObject notifition;
	private JSONObject user;
	private Timer timer;
	private TimerTask timerTask;
	private String notifitionPath;
	private String kpiUrl,analyseUrl,appUrl,messageUrl;
	private int kpiCount,analyseCount,appCount,messageCount,settingCount;
	private Context mContext;
	private Intent sendIntent;


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mContext = this;
		String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), URLs.USER_CONFIG_FILENAME);
		user = FileUtil.readConfigFile(userConfigPath);
		String currentUIVersion = currentUIVersion();

		//注册广播发送
		sendIntent = new Intent();
		sendIntent.setAction(DashboardActivity.ACTION_UPDATENOTIFITION);

		try {
			kpiUrl = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("group_id"), user.getString("role_id"));
			analyseUrl = String.format(URLs.ANALYSE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));
			appUrl = String.format(URLs.APPLICATION_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));
			messageUrl = String.format(URLs.MESSAGE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"), user.getString("group_id"), user.getString("user_id"));
			Log.i("notifition","kpiurl:"+kpiUrl);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		notifitionPath = FileUtil.basePath(mContext) + "/" + "Cached" + "/" + "local_notifition.json";
		File file = new File(notifitionPath);
		if (!file.exists()) {
			initNotifition();
			notifitionTask();
		}
		else {
			notifitionTask();
		}
	}

	private void notifitionTask() {
		timer = new Timer();
		timerTask = new TimerTask() {
			@Override
			public void run() {
				getNotifition("tab_kpi",kpiUrl);
				getNotifition("tab_analyse",analyseUrl);
				getNotifition("tab_app",appUrl);
				getNotifition("tab_message",messageUrl);

				notifitionCount();

				sendIntent.putExtra("kpi",kpiCount);
				sendIntent.putExtra("analyse",analyseUrl);
				sendIntent.putExtra("app",appCount);
				sendIntent.putExtra("message",messageCount);
				sendIntent.putExtra("setting",settingCount);
				sendBroadcast(sendIntent);
			}
		};
		timer.schedule(timerTask,0,15 * 1000);
	}

	/*
	 * UI 版本
	 */
	final String currentUIVersion() {
		try {
			String betaConfigPath = FileUtil.dirPath(mContext, URLs.CONFIG_DIRNAME, URLs.BETA_CONFIG_FILENAME);
			JSONObject betaJSON = new JSONObject();
			if(new File(betaConfigPath).exists()) {
				betaJSON = FileUtil.readConfigFile(betaConfigPath);
			}
			return betaJSON.has("new_ui") && betaJSON.getBoolean("new_ui") ? "v2" : "v1";
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "v1";
	}

	/*
	 * 初始化通知文件
	 */
	private void initNotifition() {
		try {
			notifition = new JSONObject();
			notifition.put("app",-1);
			notifition.put("tab_kpi",-1);
			notifition.put("tab_kpi_last",-1);
			notifition.put("tab_analyse",-1);
			notifition.put("tab_analyse_last",-1);
			notifition.put("tab_app","-1");
			notifition.put("tab_app_last",-1);
			notifition.put("tab_message",-1);
			notifition.put("tab_message_last",-1);
			notifition.put("setting",-1);
			FileUtil.writeFile(notifitionPath,notifition.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * 差值计算
	 */
	private void notifitionCount() {
		try {
			notifition = new JSONObject(FileUtil.readFile(notifitionPath));

			kpiCount = Math.abs(notifition.getInt("tab_kpi") - notifition.getInt("tab_kpi_last"));
			analyseCount = Math.abs(notifition.getInt("tab_analyse") - notifition.getInt("tab_analyse_last"));
			appCount = Math.abs(notifition.getInt("tab_app") - notifition.getInt("tab_app_last"));
			messageCount = Math.abs(notifition.getInt("tab_message") - notifition.getInt("tab_message_last"));

			notifition.put("tab_kpi_last",notifition.getInt("tab_kpi"));
			notifition.put("tab_analyse_last",notifition.getInt("tab_analyse"));
			notifition.put("tab_app_last",notifition.getInt("tab_app"));
			notifition.put("tab_message_last",notifition.getInt("tab_message"));
			FileUtil.writeFile(notifitionPath,notifition.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * 正则获取当前通知数量
	 */
	private void getNotifition(String tabTpye,String urlString) {
		try {
			Map<String, String> response = HttpUtil.httpGet(urlString,
					new HashMap<String, String>());
			notifition = new JSONObject(FileUtil.readFile(notifitionPath));
			if (response.get("code").equals("200")) {
				String strRegex = "\\bMobileBridge.setDashboardDataCount.+";
				String countRegex = "\\d+";
				Pattern patternString = Pattern.compile(strRegex);
				Pattern patternCount = Pattern.compile(countRegex);
				Matcher matcherString = patternString.matcher(response.get("body"));
				matcherString.find();
				String str = matcherString.group();
				Matcher matcherCount = patternCount.matcher(str);
				if (matcherCount.find()) {
					int count = Integer.parseInt(matcherCount.group());
					Log.i("notifition",tabTpye + String.valueOf(count));
					notifition.put(tabTpye,count);
					FileUtil.writeFile(notifitionPath,notifition.toString());
					Log.i("notifition",notifition.toString());
				}
				else {
					Log.i("notifition","未匹配到值");
				}
			}
			else if (response.get("code").equals("304")) {
				Log.i("notifition","当前无通知");
			}
			else {
				Log.i("notifition","通知获取失败");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
