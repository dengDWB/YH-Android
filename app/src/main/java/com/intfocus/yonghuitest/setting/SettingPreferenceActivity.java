package com.intfocus.yonghuitest.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.intfocus.yonghuitest.BaseActivity;
import com.intfocus.yonghuitest.R;
import com.intfocus.yonghuitest.screen_lock.InitPassCodeActivity;

/**
 * Created by liuruilin on 2017/3/28.
 * 用户选项配置页面
 */

public class SettingPreferenceActivity extends BaseActivity {
    private Switch mScreenLockSwitch, mScreenShotSwitch, mReportCopySwitch, mLandscapeBannerSwitch;
    private SharedPreferences mSharedPreferences;
    private Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_preference);

        mContext = this;

        mScreenLockSwitch = (Switch) findViewById(R.id.switch_screenLock);
        mScreenShotSwitch = (Switch) findViewById(R.id.switch_screenshot);
        mReportCopySwitch = (Switch) findViewById(R.id.switch_report_copy);
        mLandscapeBannerSwitch = (Switch) findViewById(R.id.switch_landscape_banner);

        mScreenLockSwitch.setOnCheckedChangeListener(mSwitchScreenLockListener);
        mScreenShotSwitch.setOnCheckedChangeListener(mSwitchScreenShotListener);
        mReportCopySwitch.setOnCheckedChangeListener(mSwitchReportCopyListener);
        mLandscapeBannerSwitch.setOnCheckedChangeListener(mSwitchBannerListener);

        initSwitchPreference();
    }

    /*
     * Switch 状态初始化
     */
    private void initSwitchPreference() {
        mSharedPreferences = getSharedPreferences("SettingPreference", MODE_PRIVATE);
        mScreenLockSwitch.setChecked(mSharedPreferences.getBoolean("ScreenLock", false));
        mScreenShotSwitch.setChecked(mSharedPreferences.getBoolean("ScreenShort", true));
        mReportCopySwitch.setChecked(mSharedPreferences.getBoolean("ReportCopy", false));
        mLandscapeBannerSwitch.setChecked(mSharedPreferences.getBoolean("Landscape", false));
    }

    /*
     *  Switch ScreenLock 开关
     */
    private final CompoundButton.OnCheckedChangeListener mSwitchScreenLockListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            SharedPreferences.Editor mEditor = mSharedPreferences.edit();
            mEditor.putBoolean("ScreenLock", isChecked);
            mEditor.commit();

            if (isChecked) {
                startActivity(InitPassCodeActivity.createIntent(mContext));
                // 开启锁屏设置
            }
        }
    };

    /*
     *  Switch LandScape Banner 开关
     */
    private final CompoundButton.OnCheckedChangeListener mSwitchBannerListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            SharedPreferences.Editor mEditor = mSharedPreferences.edit();
            mEditor.putBoolean("Landscape", isChecked);
            mEditor.commit();
        }
    };

    /*
     *  Switch ScreenShot 开关
     */
    private final CompoundButton.OnCheckedChangeListener mSwitchScreenShotListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            SharedPreferences.Editor mEditor = mSharedPreferences.edit();
            mEditor.putBoolean("ScreenShot", isChecked);
            mEditor.commit();
        }
    };

    /*
     *  Switch Report Copy 开关
     */
    private final CompoundButton.OnCheckedChangeListener mSwitchReportCopyListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            SharedPreferences.Editor mEditor = mSharedPreferences.edit();
            mEditor.putBoolean("ReportCopy", isChecked);
            mEditor.commit();
        }
    };
}
