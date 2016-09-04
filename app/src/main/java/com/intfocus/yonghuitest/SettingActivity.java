package com.intfocus.yonghuitest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.intfocus.yonghuitest.screen_lock.InitPassCodeActivity;
import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.URLs;
import com.readystatesoftware.viewbadger.BadgeView;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengRegistrar;
import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

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
    private TextView mPygerLink;
    private TextView mChangePWD;
    private TextView mCheckUpgrade;
    private TextView mWarnPWD;
    private BadgeView bvCheckUpgrade;
    private BadgeView bvChangePWD;
    private IconImageView mIconImageView;
    private PopupWindow popupWindow;
    private String iconPath;

    /* 请求识别码 */
    private static final int CODE_GALLERY_REQUEST = 0xa0;
    private static final int CODE_CAMERA_REQUEST = 0xa1;
    private static final int CODE_RESULT_REQUEST = 0xa2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mMyApp.setCurrentActivity(this);

        mUserID = (TextView) findViewById(R.id.user_id);
        mRoleID = (TextView) findViewById(R.id.role_id);
        mGroupID = (TextView) findViewById(R.id.group_id);
        mChangePWD = (TextView) findViewById(R.id.change_pwd);
        mWarnPWD = (TextView) findViewById(R.id.warn_pwd);
        mCheckUpgrade = (TextView) findViewById(R.id.check_upgrade);
        mPygerLink = (TextView) findViewById(R.id.pgyer_link);
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
        mIconImageView =(IconImageView) findViewById(R.id.img_icon);

        screenLockInfo = "取消锁屏成功";
        mLockSwitch.setChecked(FileUtil.checkIsLocked(mContext));
        mCheckAssets.setOnClickListener(mCheckAssetsListener);
        mUISwitch = (Switch) findViewById(R.id.ui_switch);
        mUISwitch.setChecked(URLs.currentUIVersion(mContext).equals("v1"));

        bvCheckUpgrade = new BadgeView(this, mCheckUpgrade);
        bvChangePWD = new BadgeView(this, mChangePWD);

        mChangeLock.setOnClickListener(mChangeLockListener);
        mChangePWD.setOnClickListener(mChangePWDListener);
        mLogout.setOnClickListener(mLogoutListener);
        mCheckUpgrade.setOnClickListener(mCheckUpgradeListener);
        mLockSwitch.setOnCheckedChangeListener(mSwitchLockListener);
        mUISwitch.setOnCheckedChangeListener(mSwitchUIListener);
        mPygerLink.setOnClickListener(mPgyerLinkListener);
        mIconImageView.setOnClickListener(mIconImageViewListener);

        initIconMenu();
        initializeUI();
        setSettingViewControlBadges();
    }
    @Override
    public void onResume() {
        super.onResume();

        mMyApp.setCurrentActivity(this);
        mLockSwitch.setChecked(FileUtil.checkIsLocked(mContext));
    }

    /*
     * 初始化界面内容
     */
    private void initializeUI() {
        try {
            mUserID.setText(user.getString("user_name"));
            mRoleID.setText(user.getString("role_name"));
            mGroupID.setText(user.getString("group_name"));
            mPushState.setText(PushAgent.getInstance(mContext).isEnabled() ? "开启" : "关闭");

            mAppName.setText(getApplicationName(SettingActivity.this));

            mDeviceID.setText(TextUtils.split(android.os.Build.MODEL, " - ")[0]);
            mApiDomain.setText(URLs.kBaseUrl.replace("http://", "").replace("https://", ""));

            iconPath = FileUtil.dirPath(mContext,URLs.CONFIG_DIRNAME,"icon.jpg");
            if (new File(iconPath).exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(iconPath);
                mIconImageView.setImageBitmap(bitmap);
            }
            else {
                mIconImageView.setImageResource(R.drawable.login_bg_logo);
            }

            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionInfo = String.format("%s(%d)", packageInfo.versionName, packageInfo.versionCode);
            mAppVersion.setText(versionInfo);
            mAppIdentifier.setText(packageInfo.packageName);

            String pgyerVersionPath = String.format("%s/%s", FileUtil.basePath(mContext), URLs.PGYER_VERSION_FILENAME),
                   betaLink = "", pgyerInfo = "";
            if((new File(pgyerVersionPath)).exists()) {
                JSONObject pgyerJSON = FileUtil.readConfigFile(pgyerVersionPath);
                JSONObject responseData = pgyerJSON.getJSONObject("data");
                pgyerInfo = String.format("%s(%s)", responseData.getString("versionName"), responseData.getString("versionCode"));
                betaLink = pgyerInfo.equals(versionInfo) ? "" : pgyerInfo;
            }
            mPygerLink.setText(betaLink.isEmpty() ? "已是最新版本" : String.format("有发布测试版本%s", pgyerInfo));
            mPygerLink.setTextColor(Color.parseColor(betaLink.isEmpty() ? "#808080" : "#0000ff"));
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }

    private final View.OnClickListener mIconImageViewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            popupWindow.showAtLocation(mIconImageView, Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
        }
    };

    public void initIconMenu() {
        final View iconMenuView = LayoutInflater.from(this).inflate(R.layout.activity_icon_dialog, null);

        Button btnTakePhoto =(Button) iconMenuView.findViewById(R.id.btn_icon_takephoto);
        Button btnGetPhoto  =(Button) iconMenuView.findViewById(R.id.btn_icon_getphoto);
        Button btnCancel =(Button) iconMenuView.findViewById(R.id.btn_icon_cancel);

        popupWindow = new PopupWindow(this);
        popupWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setContentView(iconMenuView);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(true);

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCameraCapture();
            }
        });

        btnGetPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getGallery();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
    }

    /*
     * 获取相册图片
     */
    private void getGallery() {
        Intent intentFromGallery = new Intent();
        intentFromGallery.setType("image/*");
        intentFromGallery.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intentFromGallery,CODE_GALLERY_REQUEST);
    }

    /*
     * 启动拍照并获取图片
     */
    private void getCameraCapture() {
        Intent intentFromCapture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        /*
         * 需要调用裁剪图片功能，无法读取内部存储，故使用 SD 卡先存储图片
         */
        if (hasSdcard()) {
            intentFromCapture.putExtra(MediaStore.EXTRA_OUTPUT, Uri.
                    fromFile(new File(Environment.getExternalStorageDirectory(),"icon.jpg")));
        }

        startActivityForResult(intentFromCapture,CODE_CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,Intent intent) {
        // 用户没有选择图片，返回
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplication(), "取消", Toast.LENGTH_LONG).show();
            return;
        }

        switch (requestCode) {
            case CODE_GALLERY_REQUEST:
                cropPhoto(intent.getData());
                break;
            case CODE_CAMERA_REQUEST:
                    File tempFile = new File(Environment.getExternalStorageDirectory(),"icon.jpg");
                    cropPhoto(Uri.fromFile(tempFile));
                break;
            default:
                if (intent != null) {
                    setImageToHeadView(intent);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    /*
     * 调用系统的裁剪
     */
    public void cropPhoto(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, CODE_RESULT_REQUEST);
    }

    /*
     * 提取保存裁剪之后的图片数据，并设置头像部分的View
     */
    private void setImageToHeadView(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Bitmap userIcon = extras.getParcelable("data");
            popupWindow.dismiss();
            FileUtil.saveImage(iconPath,userIcon);
            mIconImageView.setImageBitmap(userIcon);
        }
    }

    /*
     * 检查设备是否存在SDCard的工具方法
     */
    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * 返回
     */
    public void dismissActivity(View v) {
        SettingActivity.this.onBackPressed();
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
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent();
            intent.setClass(SettingActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put("action", "退出登录");
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }

            finish();
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
            } catch (Exception e) {
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
                        /*
                         * 用户报表数据js文件存放在公共区域
                         */
                        String headerPath = String.format("%s/%s", FileUtil.sharedPath(mContext), URLs.CACHED_HEADER_FILENAME);
                        new File(headerPath).delete();
                        headerPath = String.format("%s/%s", FileUtil.dirPath(mContext, URLs.HTML_DIRNAME), URLs.CACHED_HEADER_FILENAME);
                        new File(headerPath).delete();

                        /*
                         * Umeng Device Token
                         */
                        String device_token = UmengRegistrar.getRegistrationId(mContext);
                        String pushConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), URLs.PUSH_CONFIG_FILENAME);
                        JSONObject pushJSON = FileUtil.readConfigFile(pushConfigPath);
                        if(!pushJSON.has("push_device_token") || pushJSON.getString("push_device_token").length() != 44 ||
                            device_token.length() != 44 || !pushJSON.getString("push_device_token").equals(device_token)) {
                            pushJSON.put("push_valid", false);
                            pushJSON.put("push_device_token", device_token);
                            FileUtil.writeFile(pushConfigPath, pushJSON.toString());
                        }

                        ApiHelper.authentication(SettingActivity.this, user.getString("user_num"), user.getString("password"));

                        // pushJSON = FileUtil.readConfigFile(pushConfigPath);
                        // mPushState.setText(pushJSON.has("push_valid") && pushJSON.getBoolean("push_valid") ? "开启" : "关闭");

                        /*
                         * 检测服务器静态资源是否更新，并下载
                         */
                        runOnUiThread(new Runnable() {
                            @Override public void run() {
                                checkAssetsUpdated(false);

                                FileUtil.checkAssets(mContext, "assets", false);
                                FileUtil.checkAssets(mContext, "loading", false);
                                FileUtil.checkAssets(mContext, "fonts",true);
                                FileUtil.checkAssets(mContext, "images", true);
                                FileUtil.checkAssets(mContext, "javascripts", true);
                                FileUtil.checkAssets(mContext, "stylesheets", true);

                                toast("校正完成");
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    };

    /*
     *  Switch 锁屏开关
     */
    private final CompoundButton.OnCheckedChangeListener mSwitchLockListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.i("onCheckedChanged", isChecked ? "ON" : "OFF");
            if(isChecked) {
                startActivity(InitPassCodeActivity.createIntent(mContext));
            } else {
                try {
                    String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), URLs.USER_CONFIG_FILENAME);
                    if((new File(userConfigPath)).exists()) {
                        JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);
                        userJSON.put("use_gesture_password", false);
                        userJSON.put("gesture_password", "");

                        FileUtil.writeFile(userConfigPath, userJSON.toString());
                        String settingsConfigPath = FileUtil.dirPath(mContext, URLs.CONFIG_DIRNAME, URLs.SETTINGS_CONFIG_FILENAME);
                        FileUtil.writeFile(settingsConfigPath, userJSON.toString());
                    }

                    toast(screenLockInfo);
                } catch (Exception e) {
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
                betaJSON.put("old_ui", isChecked);
                FileUtil.writeFile(betaConfigPath, betaJSON.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    /*
     * 检测版本更新
     * {"code":0,"message":"","data":{"lastBuild":"10","downloadURL":"","versionCode":"15","versionName":"0.1.5","appUrl":"http:\/\/www.pgyer.com\/yh-a","build":"10","releaseNote":"\u66f4\u65b0\u5230\u7248\u672c: 0.1.5(build10)"}}
     */
    final View.OnClickListener mPgyerLinkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent browserIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(URLs.kPgyerUrl));
            startActivity(browserIntent);
        }
    };

    /**
     * 设置界面，需要显示通知样式的控件，检测是否需要通知
     */
    private void setSettingViewControlBadges() {
        String notificationPath = FileUtil.dirPath(mContext, "Cached", URLs.LOCAL_NOTIFICATION_FILENAME);
        if(!(new File(notificationPath)).exists()) {
            return;
        }

        try {
            JSONObject notificationJSON = FileUtil.readConfigFile(notificationPath);
            // 每次进入设置页面都判断密码是否修改以及是否需要更新
            int passwordCount = user.getString("password").equals(URLs.MD5(URLs.kInitPassword)) ? 1 : -1;
            notificationJSON.put("setting_password", passwordCount);

            if (passwordCount == 1) {
                mWarnPWD.setTextColor(Color.parseColor("#808080"));
                mWarnPWD.setTextSize(16);
                mWarnPWD.setText("请修改初始密码");
                mChangePWD.setText("   修改登录密码");
                setBadgeView("setting_password", bvChangePWD);
            } else {
                mWarnPWD.setVisibility(View.GONE);
                mChangePWD.setText("修改登录密码");
                bvChangePWD.setVisibility(View.GONE);
            }

            if (notificationJSON.getInt("setting_pgyer") == 1) {
                mCheckUpgrade.setText("   检测更新");
                setBadgeView("setting_pgyer", bvCheckUpgrade);
            }

            FileUtil.writeFile(notificationPath, notificationJSON.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
