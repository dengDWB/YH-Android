package com.intfocus.yh_android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.intfocus.yh_android.screen_lock.ConfirmPassCodeActivity;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.K;
import com.intfocus.yh_android.util.LogUtil;
import com.intfocus.yh_android.util.URLs;
import com.pgyersdk.crash.PgyCrashManager;
import com.pgyersdk.update.PgyUpdateManager;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengNotificationClickHandler;
import com.umeng.message.entity.UMessage;
import com.umeng.socialize.PlatformConfig;

import org.OpenUDID.OpenUDID_manager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by lijunjie on 16/1/15.
 */
public class YHApplication extends Application {
    private Context mContext;
    private RefWatcher refWatcher;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;
        String sharedPath = FileUtil.sharedPath(mContext), basePath = FileUtil.basePath(mContext);

        /*
         * 微信平台验证
         */
        PlatformConfig.setWeixin(K.kWXAppId, K.kWXAppSecret);

        /*
         *  蒲公英平台，收集闪退日志
         */
        PgyCrashManager.register(this);

        /*
         *  初始化 OpenUDID, 设备唯一化
         */
        OpenUDID_manager.sync(getApplicationContext());

        /*
         *  基本目录结构
         */
        makeSureFolderExist(K.kSharedDirName);
        makeSureFolderExist(K.kCachedDirName);

        /**
         *  新安装、或升级后，把代码包中的静态资源重新拷贝覆盖一下
         *  避免再从服务器下载更新，浪费用户流量
         */
        copyAssetFiles(basePath, sharedPath);

        /*
         *  校正静态资源
         *
         *  sharedPath/filename.zip md5 值 <=> user.plist 中 filename_md5
         *  不一致时，则删除原解压后文件夹，重新解压 zip
         */
        FileUtil.checkAssets(mContext, URLs.kAssets, false);
        FileUtil.checkAssets(mContext, URLs.kLoding, false);
        FileUtil.checkAssets(mContext, URLs.kFonts, true);
        FileUtil.checkAssets(mContext, URLs.kImages, true);
        FileUtil.checkAssets(mContext, URLs.kStylesheets, true);
        FileUtil.checkAssets(mContext, URLs.kJavaScripts, true);
        FileUtil.checkAssets(mContext, URLs.kBarCodeScan, false);
//        FileUtil.checkAssets(mContext, URLs.kAdvertisement, false);

        /*
         *  手机待机再激活时发送开屏广播
         */
        registerReceiver(broadcastScreenOnAndOff, new IntentFilter(Intent.ACTION_SCREEN_ON));

        /*
         *  监测内存泄漏
         */
        refWatcher = LeakCanary.install(this);
        PushAgent mPushAgent = PushAgent.getInstance(mContext);
        // 开启推送并设置注册的回调处理
        mPushAgent.enable(new IUmengRegisterCallback() {
            @Override
            public void onRegistered(final String registrationId) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(mContext == null) {
                                LogUtil.d("PushAgent", "mContext is null");
                                return;
                            }
                            // onRegistered方法的参数registrationId即是device_token
                            String pushConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kPushConfigFileName );
                            JSONObject pushJSON = FileUtil.readConfigFile(pushConfigPath);
                            pushJSON.put("push_valid", false);
                            pushJSON.put("push_device_token", registrationId);
                            FileUtil.writeFile(pushConfigPath, pushJSON.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        mPushAgent.onAppStart();

        mPushAgent.setNotificationClickHandler(pushMessageHandler);
    }

    /*
     * 程序终止时会执行以下代码
     */
    @Override
    public void onTerminate() {
        PgyCrashManager.unregister(); // 解除注册蒲公英异常信息上传
        ActivityCollector.finishAll();
        super.onTerminate();
    }

    public static RefWatcher getRefWatcher(Context context) {
        YHApplication application = (YHApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    private void makeSureFolderExist(String folderName) {
        String cachedPath = String.format("%s/%s", FileUtil.basePath(mContext), folderName);
        FileUtil.makeSureFolderExist(cachedPath);
    }

    /**
     *  新安装、或升级后，把代码包中的静态资源重新拷贝覆盖一下
     *  避免再从服务器下载更新，浪费用户流量
     */
    private void copyAssetFiles(String basePath, String sharedPath) {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionConfigPath = String.format("%s/%s", basePath, K.kCurrentVersionFileName);

            boolean isUpgrade = true;
            String localVersion = "new-installer";
            if ((new File(versionConfigPath)).exists()) {
                localVersion = FileUtil.readFile(versionConfigPath);
                isUpgrade = !localVersion.equals(packageInfo.versionName);
            }
            if (!isUpgrade) return;
            Log.i("VersionUpgrade", String.format("%s => %s remove %s/%s", localVersion, packageInfo.versionName, basePath, K.kCachedHeaderConfigFileName));

            String assetZipPath;
            File assetZipFile;
            String[] assetsName = {URLs.kAssets,URLs.kLoding,URLs.kFonts,URLs.kImages,URLs.kStylesheets,URLs.kJavaScripts,URLs.kBarCodeScan,URLs.kAdvertisement};

            for (String string : assetsName) {
                assetZipPath = String.format("%s/%s.zip", sharedPath, string);
                assetZipFile = new File(assetZipPath);
                if (!assetZipFile.exists()) { assetZipFile.delete();}
                FileUtil.copyAssetFile(mContext, String.format("%s.zip",string), assetZipPath);
            }
            FileUtil.writeFile(versionConfigPath, packageInfo.versionName);
        }
        catch (PackageManager.NameNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    /*
     *  手机待机再激活时接收解屏广播,进入解锁密码页
     */
    private final BroadcastReceiver broadcastScreenOnAndOff = new BroadcastReceiver() {

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!intent.getAction().equals(Intent.ACTION_SCREEN_ON) || isBackground(mContext)) return;
            Log.i("BroadcastReceiver", "Screen On");
            String currentActivityName = ((YHApplication)context.getApplicationContext()).getCurrentActivity();
            if ((currentActivityName != null && !currentActivityName.trim().equals("ConfirmPassCodeActivity")) && // 当前活动的Activity非解锁界面
                    FileUtil.checkIsLocked(mContext)) { // 应用处于登录状态，并且开启了密码锁
                intent = new Intent(mContext, ConfirmPassCodeActivity.class);
                intent.putExtra("is_from_login", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                mContext.startActivity(intent);
            }
        }
    };

    private String mCurrentActivity = null;
    public String getCurrentActivity(){
        return mCurrentActivity;
    }

    public void setCurrentActivity(Context context) {
        if (context == null) {
            mCurrentActivity = null;
            return;
        }
        String mActivity = context.toString();
        String mActivityName = mActivity.substring(mActivity.lastIndexOf(".") + 1, mActivity.indexOf("@"));
        Log.i("activityName",mActivityName);
        mCurrentActivity = mActivityName;
    }

    /*
     * 判断应用当前是否处于后台
     * Android 4.4 以上版本 不适用 getRunningTasks() 方法
     */
    private boolean isBackground(Context context) {
        boolean isBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isBackground = false;
            }
        }

        return isBackground;
    }

    UmengNotificationClickHandler pushMessageHandler = new UmengNotificationClickHandler() {
        @Override
        public void dealWithCustomAction(Context context, UMessage uMessage) {
            super.dealWithCustomAction(context, uMessage);
            try {
                String pushMessagePath = String.format("%s/%s", FileUtil.basePath(mContext), K.kPushMessageFileName);
                JSONObject pushMessageJSON = new JSONObject(uMessage.custom);
                pushMessageJSON.put("state", false);
                FileUtil.writeFile(pushMessagePath, pushMessageJSON.toString());

                Intent intent;
                if ((mCurrentActivity == null)) {
                    intent = new Intent (mContext, LoginActivity.class);
                }
                else {
                    String activityName = mCurrentActivity.getClass().getSimpleName();

                    if (activityName.equals("LoginActivity") || activityName.equals("ConfirmPassCodeActivity")) {
                        return;
                    }
                    ActivityCollector.finishAll();
                    intent = new Intent (mContext,DashboardActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

}


