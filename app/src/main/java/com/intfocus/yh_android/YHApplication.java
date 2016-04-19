package com.intfocus.yh_android;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.intfocus.yh_android.screen_lock.ConfirmPassCodeActivity;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.URLs;
import com.pgyersdk.crash.PgyCrashManager;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.OpenUDID.OpenUDID_manager;

import java.io.File;
import java.io.IOException;

/**
 * Created by lijunjie on 16/1/15.
 */
public class YHApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private String currentActivityName;
    private Context mContext;
    private RefWatcher refWatcher;

    /*
     *  手机待机再激活时发送解屏广播
     */
    private final BroadcastReceiver broadcastScreenOnAndOff = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {


            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            String runningActivity = activityManager.getRunningTasks(1).get(0).topActivity.getClassName();

            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON) && // 开屏状态
                    runningActivity != null && // 应用活动Activity数量大于零
                    !currentActivityName.contains("ConfirmPassCodeActivity") && // 当前活动的Activity非解锁界面
                    FileUtil.checkIsLocked(mContext)) { // 应用处于登录状态，并且开启了密码锁

                Intent i = new Intent(getApplicationContext(), ConfirmPassCodeActivity.class);
                i.putExtra("is_from_login", false);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        currentActivityName = "";
        mContext = YHApplication.this;
        String sharedPath = FileUtil.sharedPath(mContext), basePath = FileUtil.basePath(mContext);

        /*
         *  蒲公英平台，收集闪退日志
         */
        PgyCrashManager.register(this);

        /*
         *  初始化OpenUDID, 设备唯一化
         */
        OpenUDID_manager.sync(getApplicationContext());

        /*
         *  基本目录结构
         */
        makeSureFolderExist(URLs.CACHED_DIRNAME);
        makeSureFolderExist(URLs.SHARED_DIRNAME);

        /**
         *  新安装、或升级后，把代码包中的静态资源重新拷贝覆盖一下
         *  避免再从服务器下载更新，浪费用户流量
         */
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionConfigPath = String.format("%s/%s", basePath, URLs.CURRENT_VERSION__FILENAME);

            boolean isUpgrade = true;
            String localVersion = "new-installer";
            if ((new File(versionConfigPath)).exists()) {
                localVersion = FileUtil.readFile(versionConfigPath);
                if (localVersion.equals(packageInfo.versionName)) {
                    isUpgrade = false;
                }
            }

            if (isUpgrade) {
                Log.i("VersionUpgrade", String.format("%s => %s remove %s/%s", localVersion, packageInfo.versionName, basePath, URLs.CACHED_HEADER_FILENAME));

                String assetZipPath;
                File assetZipFile;
                String[] assetsName = {"assets.zip", "loading.zip", "fonts.zip", "images.zip", "stylesheets.zip", "javascripts.zip"};
                for (String string : assetsName) {
                    assetZipPath = String.format("%s/%s", sharedPath, string);
                    assetZipFile = new File(assetZipPath);
                    if (!assetZipFile.exists()) {
                        assetZipFile.delete();
                    }
                    FileUtil.copyAssetFile(mContext, string, assetZipPath);
                }

                FileUtil.writeFile(versionConfigPath, packageInfo.versionName);
            }
        } catch (PackageManager.NameNotFoundException | IOException e) {
            e.printStackTrace();
        }

        /*
         *  校正静态资源
         *
         *  sharedPath/filename.zip md5值 <=> user.plist中filename_md5
         *  不一致时，则删除原解压后文件夹，重新解压zip
         */
        FileUtil.checkAssets(mContext, "loading", false);
        FileUtil.checkAssets(mContext, "assets", false);
        FileUtil.checkAssets(mContext, "fonts", true);
        FileUtil.checkAssets(mContext, "images", true);
        FileUtil.checkAssets(mContext, "stylesheets", true);
        FileUtil.checkAssets(mContext, "javascripts", true);


        registerActivityLifecycleCallbacks(this);

        /*
         *  手机待机再激活时发送开屏广播
         */
        registerReceiver(broadcastScreenOnAndOff, new IntentFilter(Intent.ACTION_SCREEN_ON));
        // registerReceiver(broadcastScreenOnAndOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        /*
         *  监测内存泄漏
         */
        refWatcher = LeakCanary.install(this);
    }

    @Override
    public void onTerminate() {
        unregisterActivityLifecycleCallbacks(this);

        Log.i("YHApplication", "onTerminate");
        super.onTerminate();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.i("YHApplication", "onActivityCreated - " + activity.getClass());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.i("YHApplication", "onActivityStarted - " + activity.getClass());
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.i("YHApplication", "onActivityResumed - " + activity.getClass());
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.i("YHApplication", "onActivityPaused - " + activity.getClass());

        /*
         * 进入待机状态，会优先触发puased， 然后stopped.
         * 如果用户使用app时，进入待机状态前的最后一个ctivit
         * 1. 如果用户已使用锁屏功能，则进入验证密码界面
         * 2. 如果未使用锁屏功能，则进入登录状态
         */
        currentActivityName = activity.getClass().toString();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.i("YHApplication", "onActivityStopped - " + activity.getClass());
        /*
         * 进入待机状态，会优先触发puased， 然后stopped.
         * 如果用户使用app时，进入待机状态前的最后一个ctivit
         * 1. 如果用户已使用锁屏功能，则进入验证密码界面
         * 2. 如果未使用锁屏功能，则进入登录状态
         */
        currentActivityName = activity.getClass().toString();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.i("YHApplication", "onActivityDestroyed");
    }


    public static RefWatcher getRefWatcher(Context context) {
        YHApplication application = (YHApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    private void makeSureFolderExist(String folderName) {
        String cachedPath = String.format("%s/%s", FileUtil.basePath(mContext), folderName);
        FileUtil.makeSureFolderExist(cachedPath);
    }
}
