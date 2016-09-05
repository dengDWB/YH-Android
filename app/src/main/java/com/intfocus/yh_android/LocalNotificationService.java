package com.intfocus.yh_android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.HttpUtil;
import com.intfocus.yh_android.util.URLs;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lijunjie on 16/8/25.
 */
public class LocalNotificationService extends Service {
  private JSONObject notifition;
  private JSONObject user;
  private JSONObject pgyerJSON;
  private Timer timer;
  private TimerTask timerTask;
  private PackageInfo packageInfo;
  private String notifitionPath, pgyerVersionPath, userConfigPath;
  private String kpiUrl, analyseUrl, appUrl, messageUrl;
  private String pgyerCode, versionCode;
  private int kpiCount, analyseCount, appCount, messageCount, updataCount, passwordCount;
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

    notifitionPath = FileUtil.dirPath(mContext, "Cached", URLs.LOCAL_NOTIFICATION_FILENAME);
    userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), URLs.USER_CONFIG_FILENAME);
    pgyerVersionPath = String.format("%s/%s", FileUtil.basePath(mContext), URLs.PGYER_VERSION_FILENAME);

    //注册广播发送
    sendIntent = new Intent();
    sendIntent.setAction(DashboardActivity.ACTION_UPDATENOTIFITION);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    user = FileUtil.readConfigFile(userConfigPath);
    pgyerJSON = FileUtil.readConfigFile(pgyerVersionPath);
    notifition = FileUtil.readConfigFile(notifitionPath);
    try {
      String currentUIVersion = URLs.currentUIVersion(mContext);
      kpiUrl = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("group_id"), user.getString("role_id"));
      analyseUrl = String.format(URLs.ANALYSE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));
      appUrl = String.format(URLs.APPLICATION_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));
      messageUrl = String.format(URLs.MESSAGE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"), user.getString("group_id"), user.getString("user_id"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    notifitionTask();
    return super.onStartCommand(intent, flags, startId);
  }

  /*
   * 通知定时刷新任务,间隔 15 分钟发送一次广播
   */
  private void notifitionTask() {
    timer = new Timer();
    timerTask = new TimerTask() {
      @Override
      public void run() {
        processDataCount();//先计算通知的数量
        Log.i("Timer", URLs.timestamp());
        sendBroadcast(sendIntent);
      }
    };
    timer.schedule(timerTask, 0, 15 * 60 * 1000);//间隔 15 分钟
  }

  /*
   * 计算将要传递给 DashboardActivity 的通知数值
   */
  private void processDataCount() {
    try {
      kpiCount = getDataCount("kpi", kpiUrl);
      analyseCount = getDataCount("analyse", analyseUrl);
      appCount = getDataCount("app", appUrl);
      messageCount = getDataCount("message", messageUrl);

			/*
			 * 遍历获取 Tab 栏上需要显示的通知数量 ("tab_*" 的值)
			 */
      String[] typeString = {"kpi", "analyse", "app", "message"};
      int[] typeCount = {kpiCount, analyseCount, appCount, messageCount};
      for (int i = 0; i < typeString.length; i++) {
        if (notifition.getInt("tab_" + typeString[i] + "_last") > 0 && notifition.getInt("tab_" + typeString[i] + "_last") != typeCount[i]) {
          notifition.put("tab_" + typeString[i], Math.abs(typeCount[i] - notifition.getInt("tab_" + typeString[i] + "_last")));
          notifition.put("tab_" + typeString[i] + "_last", typeCount[i]);
        }
      }

      if ((new File(pgyerVersionPath)).exists()) {
        pgyerJSON = FileUtil.readConfigFile(pgyerVersionPath);
        JSONObject responseData = pgyerJSON.getJSONObject("data");
        pgyerCode = responseData.getString("versionCode");
        packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        versionCode = String.valueOf(packageInfo.versionCode);
        updataCount = pgyerCode.equals(versionCode) ?  -1 : 1;
      }
      else {
        updataCount = -1;
      }

      passwordCount = user.getString("password").equals(URLs.MD5("123456")) ? 1 : -1;
      notifition.put("setting_password", passwordCount);
      notifition.put("setting_pgyer", updataCount);

      FileUtil.writeFile(notifitionPath, notifition.toString());
    } catch (JSONException | IOException | PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }

  /*
   * 正则获取当前 DataCount，未获取到值则返回原数值
   */
  private int getDataCount(String tabTpye, String urlString) throws JSONException, IOException {
    Map<String, String> response = HttpUtil.httpGet(urlString, new HashMap<String, String>());
    int lastCount = notifition.getInt("tab_" + tabTpye + "_last");
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
        int dataCount = Integer.parseInt(matcherCount.group());
				/*
				 * 如果tab_*_last 的值为 -1,表示第一次加载
				 */
        if (lastCount == -1) {
          notifition.put("tab_" + tabTpye + "_last", dataCount);
          notifition.put("tab_" + tabTpye, 0);
          FileUtil.writeFile(notifitionPath, notifition.toString());
        }
        return dataCount;
      } else {
        Log.i("notifition", "未匹配到数值");
        return lastCount;
      }
    } else if (response.get("code").equals("304")) {
      Log.i("notifition", "当前无通知");
      return lastCount;
    } else {
      Log.i("notifition", "网络请求失败");
      return lastCount;
    }
  }
}
