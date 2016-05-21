package com.intfocus.yh_android;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.intfocus.yh_android.screen_lock.InitPassCodeActivity;
import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.URLs;
import com.umeng.message.PushAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class SettingActivity extends BaseActivity {
    private TextView mUserID;
    private TextView mRoleID;
    private TextView mGroupID;
    private TextView mAppName;
    private TextView mAppVersion;
    private TextView mDeviceID;
    private TextView mAppIdentifier;
    private TextView mPushState;
    private TextView mApiDomain;
    private Switch mLockSwitch;
    private Switch mUISwitch;
    private String screenLockInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        findViewById(R.id.back).setOnClickListener(mOnBackListener);
        findViewById(R.id.back_text).setOnClickListener(mOnBackListener);

        mUserID = (TextView) findViewById(R.id.user_id);
        mRoleID = (TextView) findViewById(R.id.role_id);
        mGroupID = (TextView) findViewById(R.id.group_id);
        TextView mChangePWD = (TextView) findViewById(R.id.change_pwd);
        TextView mCheckUpgrade = (TextView) findViewById(R.id.check_upgrade);
        mAppName = (TextView) findViewById(R.id.app_name);
        mAppVersion = (TextView) findViewById(R.id.app_version);
        mDeviceID = (TextView) findViewById(R.id.device_id);
        mApiDomain = (TextView) findViewById(R.id.api_domain);
        mAppIdentifier = (TextView) findViewById(R.id.app_identifier);
        mPushState = (TextView) findViewById(R.id.push_state);
        TextView mChangeLock = (TextView) findViewById(R.id.change_lock);
        TextView mCheckAssets = (TextView) findViewById(R.id.check_assets);
        Button mLogout = (Button) findViewById(R.id.logout);
        mLockSwitch = (Switch) findViewById(R.id.lock_switch);
        screenLockInfo = "取消锁屏成功";
        mLockSwitch.setChecked(FileUtil.checkIsLocked(mContext));
        mCheckAssets.setOnClickListener(mCheckAssetsListener);
        mUISwitch = (Switch) findViewById(R.id.ui_switch);
        mUISwitch.setChecked(currentUIVersion() == "v2");

        mChangeLock.setOnClickListener(mChangeLockListener);
        mChangePWD.setOnClickListener(mChangePWDListener);
        mLogout.setOnClickListener(mLogoutListener);
        mCheckUpgrade.setOnClickListener(mCheckUpgradeListener);
        mLockSwitch.setOnCheckedChangeListener(mSwitchLockListener);
        mUISwitch.setOnCheckedChangeListener(mSwitchUIListener);

        initializeUI();
    }
    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        // Get the Camera instance as the activity achieves full user focus

        mLockSwitch.setChecked(FileUtil.checkIsLocked(mContext));
    }

    private void initializeUI() {
        /*
         * 初始化界面内容
         */
        try {
            mUserID.setText(user.getString("user_name"));
            mRoleID.setText(user.getString("role_name"));
            mGroupID.setText(user.getString("group_name"));
            // mGroupID.setText(UmengRegistrar.getRegistrationId(mContext));
            mPushState.setText(PushAgent.getInstance(mContext).isEnabled() ? "开启" : "关闭");

            mAppName.setText(getApplicationName(SettingActivity.this));

            mDeviceID.setText(TextUtils.split(android.os.Build.MODEL, " - ")[0]);
            mApiDomain.setText(URLs.HOST.replace("http://", "").replace("https://", ""));

            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mAppVersion.setText(packageInfo.versionName);
            mAppIdentifier.setText(packageInfo.packageName);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }

    /*
     * 返回
     */
    private final View.OnClickListener mOnBackListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SettingActivity.this.onBackPressed();
        }
    };

    /*
     * 退出登录
     */
    private final View.OnClickListener mLogoutListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                JSONObject configJSON = new JSONObject();
                configJSON.put("is_login", false);

                modifiedUserConfig(configJSON);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent();
            intent.setClass(SettingActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//它可以关掉所要到的界面中间的activity
            startActivity(intent);


            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put("action", "退出登录");
                new Thread(mRunnableForLogger).start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /*
    * 修改密码
    */
    private final View.OnClickListener mChangePWDListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(mContext, ResetPasswordActivity.class);
            mContext.startActivity(intent);

            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put("action", "点击/设置页面/修改密码");
                new Thread(mRunnableForLogger).start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /*
     * 修改锁屏密码
     */
    private final View.OnClickListener mChangeLockListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(SettingActivity.this, "TODO: 修改锁屏密码", Toast.LENGTH_SHORT).show();
        }
    };

    /*
     * 校正静态文件
     */
    private final View.OnClickListener mCheckAssetsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ApiHelper.authentication(SettingActivity.this, user.getString("user_num"), user.getString("password"));
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            /*
             * 检测服务器静态资源是否更新，并下载
             */
            checkAssetsUpdated(false);
            /*
             * 用户报表数据js文件存放在公共区域
             */
            String headerPath = String.format("%s/%s", FileUtil.sharedPath(mContext), URLs.CACHED_HEADER_FILENAME);
            new File(headerPath).delete();
            headerPath = String.format("%s/%s", FileUtil.dirPath(mContext, URLs.HTML_DIRNAME), URLs.CACHED_HEADER_FILENAME);
            new File(headerPath).delete();

            FileUtil.checkAssets(mContext, "assets", false);
            FileUtil.checkAssets(mContext, "loading", false);
            FileUtil.checkAssets(mContext, "fonts",true);
            FileUtil.checkAssets(mContext, "images", true);
            FileUtil.checkAssets(mContext, "javascripts", true);
            FileUtil.checkAssets(mContext, "stylesheets", true);

            Toast.makeText(mContext, "校正完成", Toast.LENGTH_SHORT).show();
        }
    };

    /*
     *  Switch 锁屏开关
     */
    private final CompoundButton.OnCheckedChangeListener mSwitchLockListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            // TODO Auto-generated method stub
            if(isChecked) {
                startActivity(InitPassCodeActivity.createIntent(mContext));
            }
            else {
                try {
                    String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), URLs.USER_CONFIG_FILENAME);
                    if((new File(userConfigPath)).exists()) {
                        JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);
                        userJSON.put("use_gesture_password", false);
                        if(!userJSON.has("gesture_password")) { userJSON.put("gesture_password", ""); }

                        FileUtil.writeFile(userConfigPath, userJSON.toString());
                        String settingsConfigPath = FileUtil.dirPath(mContext, URLs.CONFIG_DIRNAME, URLs.SETTINGS_CONFIG_FILENAME);
                        FileUtil.writeFile(settingsConfigPath, userJSON.toString());
                    }

                    toast(screenLockInfo);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put("action", String.format("点击/设置页面/%s锁屏", isChecked ? "开启" : "禁用"));
                new Thread(mRunnableForLogger).start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /*
     * 切换UI
     */
    private final CompoundButton.OnCheckedChangeListener mSwitchUIListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            // TODO Auto-generated method stub
            try {
                String betaConfigPath = FileUtil.dirPath(mContext, URLs.CONFIG_DIRNAME, URLs.BETA_CONFIG_FILENAME);
                JSONObject betaJSON = new JSONObject();
                if(new File(betaConfigPath).exists()) {
                    betaJSON = FileUtil.readConfigFile(betaConfigPath);
                }
                betaJSON.put("new_ui", isChecked);
                FileUtil.writeFile(betaConfigPath, betaJSON.toString());
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
