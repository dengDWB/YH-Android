package com.intfocus.yonghuitest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.intfocus.yonghuitest.dashboard.DashboardActivity;
import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.HttpUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.URLs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lijunjie on 16/8/25.
 */
public class LocalNotificationService extends Service {
  private JSONObject notificationJSON;
  private JSONObject userJSON;
  private JSONObject pgyerJSON;
  private String notificationPath, pgyerVersionPath, userConfigPath;
  private String kpiUrl, analyseUrl, appUrl, messageUrl, thursdaySayUrl;
  private Context mContext;
  private Intent sendIntent;
  private String mAssetsPath;
  private String mRelativeAssetsPath;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mContext = this;

    mAssetsPath = FileUtil.dirPath(mContext, K.kHTMLDirName);
    mRelativeAssetsPath = "../../Shared/assets";
    notificationPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kLocalNotificationConfigFileName);
    userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kUserConfigFileName);
    pgyerVersionPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kPgyerVersionConfigFileName);

    //注册广播发送
    sendIntent = new Intent();
//    sendIntent.setAction(DashboardActivity.ACTION_UPDATENOTIFITION);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    userJSON = FileUtil.readConfigFile(userConfigPath);
    pgyerJSON = FileUtil.readConfigFile(pgyerVersionPath);
    notificationJSON = FileUtil.readConfigFile(notificationPath);
    try {
      String currentUIVersion = URLs.currentUIVersion(mContext);
      kpiUrl = String.format(K.kKPIMobilePath, K.kBaseUrl, currentUIVersion, userJSON.getString(URLs.kGroupId), userJSON.getString(URLs.kRoleId));
      analyseUrl = String.format(K.kAnalyseMobilePath, K.kBaseUrl, currentUIVersion, userJSON.getString(URLs.kRoleId));
      appUrl = String.format(K.kAppMobilePath, K.kBaseUrl, currentUIVersion, userJSON.getString(URLs.kRoleId));
      messageUrl = String.format(K.kMessageMobilePath, K.kBaseUrl, currentUIVersion, userJSON.getString(URLs.kRoleId), userJSON.getString(URLs.kGroupId), userJSON.getString("user_id"));
      thursdaySayUrl = String.format(K.kThursdaySayMobilePath, K.kBaseUrl, currentUIVersion);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    notifitionTask();
    return super.onStartCommand(intent, flags, startId);
  }

  /*
   * 通知定时刷新任务,间隔 30 分钟发送一次广播
   */
  private void notifitionTask() {
    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
      @Override
      public void run() {
        processDataCount();//先计算通知的数量
        sendBroadcast(sendIntent);
      }
    };
    timer.schedule(timerTask, 10 * 1000, K.kTimerInterval * 60 * 1000);
  }

  /*
   * 计算将要传递给 DashboardActivity 的通知数值
   */
  private void processDataCount() {
    try {
      int kpiCount = getDataCount(URLs.kTabKpi, kpiUrl);
      int analyseCount = getDataCount(URLs.kTabAnalyse, analyseUrl);
      int appCount = getDataCount(URLs.kTabApp, appUrl);
      int messageCount = getDataCount(URLs.kTabMessage, messageUrl);
      int thursdaySayCount = getDataCount(URLs.kSettingThursdaySay, thursdaySayUrl);

      /*
       * 遍历获取 Tab 栏上需要显示的通知数量 ("tab_*" 的值)
       */
      String[] typeString = {URLs.kTabKpi, URLs.kTabAnalyse, URLs.kTabApp, URLs.kTabMessage, URLs.kSettingThursdaySay};
      int[] typeCount = {kpiCount, analyseCount, appCount, messageCount, thursdaySayCount};
      for (int i = 0; i < typeString.length; i++) {
        notificationJSON.put(typeString[i], Math.abs(typeCount[i] - notificationJSON.getInt(typeString[i] + "_last")));
        notificationJSON.put(typeString[i] + "_last", typeCount[i]);
      }

      int updataCount;
      if ((new File(pgyerVersionPath)).exists()) {
        pgyerJSON = FileUtil.readConfigFile(pgyerVersionPath);
        JSONObject responseData = pgyerJSON.getJSONObject(URLs.kData);
        String pgyerCode = responseData.getString("versionCode");
        PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        String versionCode = String.valueOf(packageInfo.versionCode);
        updataCount = pgyerCode.equals(versionCode) ?  -1 : 1;
      }
      else {
        updataCount = -1;
      }

      int passwordCount = userJSON.getString(URLs.kPassword).equals(URLs.MD5(K.kInitPassword)) ? 1 : -1;
      notificationJSON.put(URLs.kSettingPassword, passwordCount);
      notificationJSON.put(URLs.kSettingPgyer, updataCount);

      int settingCount = (notificationJSON.getInt(URLs.kSettingPassword) > 0 || notificationJSON.getInt(URLs.kSettingPgyer) > 0 || notificationJSON.getInt(URLs.kSettingThursdaySay) > 0) ? 1 : 0;
      notificationJSON.put(URLs.kSetting, settingCount);

      FileUtil.writeFile(notificationPath, notificationJSON.toString());
    } catch (JSONException | IOException | PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }

  /*
   * 正则获取当前 DataCount，未获取到值则返回原数值
   */
  private int getDataCount(String keyName, String urlString)
          throws JSONException, IOException,PackageManager.NameNotFoundException {
      /*
       * 1. 定时器链接添加标志 platform=android&auto_timer=30&user_device_id=#{user_device_id}
       * 2. 读取本地缓存头文件
       */
    Map<String, String> headers = ApiHelper.checkResponseHeader(urlString, mAssetsPath);
    PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
    String extraParams = String.format("os=android&version=a%s&inteval=%d&udi=%d",packageInfo.versionName, K.kTimerInterval, userJSON.getInt(K.kUserDeviceId));
    String urlSplit = (urlString.contains("?") ? "&" : "?");
    String urlStringWithExtraParams = String.format("%s%s%s", urlString, urlSplit, extraParams);
    Map<String, String> response = HttpUtil.httpGet(urlStringWithExtraParams, headers);

    String keyLastName = keyName + "_last";
    if(!notificationJSON.has(keyName)) { notificationJSON.put(keyName, -1); }
    if(!notificationJSON.has(keyLastName)) { notificationJSON.put(keyLastName, -1); }

    int lastCount = notificationJSON.getInt(keyLastName);

    if (response.get(URLs.kCode).equals("200")) {
      /*
       * 1. 缓存头文件信息
       * 2. 服务器响应信息写入本地
       */
      String htmlName = HttpUtil.UrlToFileName(urlString);
      String htmlPath = String.format("%s/%s", mAssetsPath, htmlName);
      String urlKey = urlString.contains("?") ? TextUtils.split(urlString, "?")[0] : urlString;
      ApiHelper.storeResponseHeader(urlKey, mAssetsPath, response);
      String htmlContent = response.get(URLs.kBody);
      htmlContent = htmlContent.replace("/javascripts/", String.format("%s/javascripts/", mRelativeAssetsPath));
      htmlContent = htmlContent.replace("/stylesheets/", String.format("%s/stylesheets/", mRelativeAssetsPath));
      htmlContent = htmlContent.replace("/images/", String.format("%s/images/", mRelativeAssetsPath));
      FileUtil.writeFile(htmlPath, htmlContent);

      String strRegex = "\\bMobileBridge.setDashboardDataCount.+";
      String countRegex = "\\d+";
      Pattern patternString = Pattern.compile(strRegex);
      Pattern patternCount = Pattern.compile(countRegex);
      Matcher matcherString = patternString.matcher(htmlContent);
      matcherString.find();
      String str = matcherString.group();
      Matcher matcherCount = patternCount.matcher(str);
      if (matcherCount.find()) {
        int dataCount = Integer.parseInt(matcherCount.group());
        /*
         * 如果tab_*_last 的值为 -1,表示第一次加载
         */
        if (lastCount == -1) {
          notificationJSON.put(keyLastName, dataCount);
          notificationJSON.put(keyName, 1);
          FileUtil.writeFile(notificationPath, notificationJSON.toString());
        }
        return dataCount;
      } else {
        Log.i("notification", "未匹配到数值");
        return lastCount;
      }
    }
    else {
      Log.i("notification", "网络请求失败");
      return lastCount;
    }
  }
}
