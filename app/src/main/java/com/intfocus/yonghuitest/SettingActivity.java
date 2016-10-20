package com.intfocus.yonghuitest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.PrivateURLs;
import com.intfocus.yonghuitest.util.URLs;
import com.intfocus.yonghuitest.view.CircleImageView;
import com.intfocus.yonghuitest.view.RedPointView;
import com.pgyersdk.update.PgyUpdateManager;
import com.readystatesoftware.viewbadger.BadgeView;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengRegistrar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingActivity extends BaseActivity {
    public final static String kGravatar = "gravatar";
    public final static String kGravatarId = "gravatar_id";
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
    private Switch mLongCatSwitch;
    private String screenLockInfo;
    private TextView mPygerLink;
    private TextView mChangePWD;
    private TextView mCheckUpgrade;
    private TextView mWarnPWD;
    private BadgeView bvCheckUpgrade;
    private BadgeView bvChangePWD;
    private BadgeView bvCheckThursdaySay;
    private CircleImageView mIconImageView;
    private PopupWindow popupWindow;
    private String gravatarJsonPath, gravatarImgPath, gravatarFileName;
    private TextView mCheckThursdaySay;
    private TextView developerTv;

    /* 请求识别码 */
    private static final int CODE_GALLERY_REQUEST = 0xa0;
    private static final int CODE_CAMERA_REQUEST = 0xa1;
    private static final int CODE_RESULT_REQUEST = 0xa2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

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
        mIconImageView =(CircleImageView) findViewById(R.id.img_icon);
        mCheckThursdaySay = (TextView) findViewById(R.id.check_thursday_say);
        developerTv = (TextView) findViewById(R.id.developerTv);

        screenLockInfo = "取消锁屏成功";
        mLockSwitch.setChecked(FileUtil.checkIsLocked(mContext));
        mCheckAssets.setOnClickListener(mCheckAssetsListener);

        try {
            String betaConfigPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kBetaConfigFileName);
            JSONObject betaJSON = FileUtil.readConfigFile(betaConfigPath);
            mLongCatSwitch = (Switch) findViewById(R.id.longcat_switch);
            mLongCatSwitch.setChecked(betaJSON.has("image_within_screen") && betaJSON.getBoolean("image_within_screen"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        bvCheckUpgrade = new BadgeView(this, mCheckUpgrade);
        bvChangePWD = new BadgeView(this, mChangePWD);
        bvCheckThursdaySay = new BadgeView(this, mCheckThursdaySay);

        mChangeLock.setOnClickListener(mChangeLockListener);
        mChangePWD.setOnClickListener(mChangePWDListener);
        mLogout.setOnClickListener(mLogoutListener);
        mCheckUpgrade.setOnClickListener(mCheckUpgradeListener);
        mLockSwitch.setOnCheckedChangeListener(mSwitchLockListener);
        mLongCatSwitch.setOnCheckedChangeListener(mSwitchLongCatListener);
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

    @Override
    protected void onDestroy() {
        PgyUpdateManager.unregister(); // 解除注册蒲公英版本更新检查
        super.onDestroy();
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
            String deviceInfo = String.format("%s(Android %s)",TextUtils.split(android.os.Build.MODEL, " - ")[0],Build.VERSION.RELEASE);
            mDeviceID.setText(deviceInfo);
            mApiDomain.setText(K.kBaseUrl.replace("http://", "").replace("https://", ""));

            gravatarJsonPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kGravatarConfigFileName);
            if (new File(gravatarJsonPath).exists()) {
                JSONObject gravatarJSON = FileUtil.readConfigFile(gravatarJsonPath);
                gravatarFileName = gravatarJSON.getString(URLs.kName);
                gravatarImgPath = FileUtil.dirPath(mContext, K.kConfigDirName, gravatarFileName);
                String currentGravatarUrl = user.getString(kGravatar);
                String currentGravatarFileName = currentGravatarUrl.substring(currentGravatarUrl.lastIndexOf("/")+1, currentGravatarUrl.length());
                // 以用户验证响应的 gravatar 值为准，不一致则下载
                if (!(gravatarFileName.equals(currentGravatarFileName))) {
                    gravatarImgPath = FileUtil.dirPath(mContext, K.kConfigDirName, currentGravatarFileName);
                    gravatarFileName = currentGravatarFileName;
                    httpGetBitmap(currentGravatarUrl, true);
                    return;
                }

                Bitmap bitmap = BitmapFactory.decodeFile(gravatarImgPath);
                mIconImageView.setImageBitmap(bitmap);
            }
            else {
                if (user.has(kGravatar) && (user.getString(kGravatar).indexOf("http") != -1)) {
                    String gravatarUrl = user.getString(kGravatar);
                    gravatarFileName = gravatarUrl.substring(gravatarUrl.lastIndexOf("/")+1, gravatarUrl.length());
                    gravatarImgPath = FileUtil.dirPath(mContext, K.kConfigDirName, gravatarFileName);
                    httpGetBitmap(gravatarUrl, false);
                }

                mIconImageView.setImageResource(R.drawable.login_logo);
            }

            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionInfo = String.format("%s(%d)", packageInfo.versionName, packageInfo.versionCode);
            int currentVersionCode = packageInfo.versionCode;
            mAppVersion.setText(versionInfo);
            mAppIdentifier.setText(packageInfo.packageName);

            String pgyerVersionPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kPgyerVersionConfigFileName),
                   betaLink = "", pgyerInfo = "";
            if((new File(pgyerVersionPath)).exists()) {
                JSONObject pgyerJSON = FileUtil.readConfigFile(pgyerVersionPath);
                JSONObject responseData = pgyerJSON.getJSONObject(URLs.kData);
                pgyerInfo = String.format("%s(%s)", responseData.getString("versionName"), responseData.getString("versionCode"));
                int newVersionCode = responseData.getInt("versionCode");
                if (newVersionCode > currentVersionCode) {
                    betaLink = pgyerInfo;
                }
            }
            mPygerLink.setText(betaLink.isEmpty() ? "已是最新版本" : String.format("有发布版本%s", pgyerInfo));
            mPygerLink.setTextColor(Color.parseColor(betaLink.isEmpty() ? "#808080" : "#0000ff"));
        } catch (NameNotFoundException | JSONException e) {
            e.printStackTrace();
        }
    }

    public void writeJson(String path, String name, boolean upload_state, String gravatar_url, String gravatar_id, boolean isDelete){
        try {
            String previousImgPath = "";
            JSONObject jsonObject;
            if (new File(path).exists()) {
                jsonObject = new JSONObject(FileUtil.readFile(path));
                previousImgPath = FileUtil.dirPath(mContext, K.kConfigDirName, jsonObject.getString(URLs.kName));
                if (isDelete) {
                    new File(previousImgPath).delete();
                }
            }
            else {
                jsonObject = new JSONObject();
            }

            jsonObject.put(URLs.kName, name);
            jsonObject.put("upload_state", upload_state);
            if(gravatar_url.length() > 0 && gravatar_url.indexOf("http") > -1) {
                jsonObject.put("gravatar_url", gravatar_url);

                /*
                 * 上传头像成功后，重置user.json#gravatar, 以名再回到设置界面被还原
                 */
                String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kUserConfigFileName);
                user.put("gravatar", gravatar_url);
                FileUtil.writeFile(userConfigPath, user.toString());
            }
            if (!gravatar_id.equals("")) {
                jsonObject.put(kGravatarId, gravatar_id);
            }
            FileUtil.writeFile(path, jsonObject.toString());
            Log.i("upload", FileUtil.readFile(path));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void httpGetBitmap(String urlString, final boolean isDelete) {

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(urlString).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (e == null) { return; }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mIconImageView.setImageResource(R.drawable.login_logo);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    try{
                        InputStream is = response.body().byteStream();
                        final Bitmap bm = BitmapFactory.decodeStream(is);
                        FileUtil.saveImage(gravatarImgPath, bm);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mIconImageView.setImageBitmap(bm);
                            }
                        });
                        writeJson(gravatarJsonPath, gravatarFileName, true, "", "", isDelete);
                    } catch (Exception e){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mIconImageView.setImageResource(R.drawable.login_logo);
                            }
                        });
                    }
                }
            }
        });
    }

    private static String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        Log.i("getactivity",context.getString(stringId));
        return context.getString(stringId);
    }

    final View.OnClickListener mCheckUpgradeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            checkPgyerVersionUpgrade(SettingActivity.this,true);

            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put(URLs.kAction, "点击/设置页面/检测更新");
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private final View.OnClickListener mIconImageViewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int cameraPermission = ContextCompat.checkSelfPermission(mContext,Manifest.permission.CAMERA);
            if(cameraPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(SettingActivity.this,new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},CODE_CAMERA_REQUEST);
                return;
            }else{
                popupWindow.showAtLocation(mIconImageView, Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
            }
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
	 * 权限获取反馈
	 */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case CODE_CAMERA_REQUEST:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    popupWindow.showAtLocation(mIconImageView, Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
                } else {
                    Toast.makeText(SettingActivity.this, "权限获取失败，请重试", Toast.LENGTH_SHORT)
                            .show();
                    try {
                        logParams = new JSONObject();
                        logParams.put("action", "头像/拍照");
                        logParams.put("obj_title", "功能: \"头像上传，拍照\",报错: \"相机权限获取失败\"");
                        new Thread(mRunnableForLogger).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /*
     * 获取相册图片
     */
    private void getGallery() {
        Intent intentFromGallery = new Intent(Intent.ACTION_PICK,null);
        intentFromGallery.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
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
        else {
            try {
                logParams = new JSONObject();
                logParams.put("action", "头像/拍照");
                logParams.put("obj_title", "功能: \"头像上传，拍照\",报错: \"not find SdCard\"");
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.KITKAT) {
            String url=FileUtil.getBitmapUrlPath(this, uri);
            intent.setDataAndType(Uri.fromFile(new File(url)), "image/*");
        }else{
            intent.setDataAndType(uri, "image/*");
        }
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        intent.putExtra("return-data",true);
        startActivityForResult(intent, CODE_RESULT_REQUEST);
    }

    /*
     * 提取保存裁剪之后的图片数据，并设置头像部分的View
     */
    private void setImageToHeadView(Intent intent) {
        try {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Bitmap userIcon = extras.getParcelable(URLs.kData);
                gravatarImgPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kAppCode + "_" + user.getString(URLs.kUserNum) + "_" + getDate() + ".jpg");
                gravatarFileName = gravatarImgPath.substring(gravatarImgPath.lastIndexOf("/")+1, gravatarImgPath.length());
                mIconImageView.setImageBitmap(userIcon);
                popupWindow.dismiss();
                FileUtil.saveImage(gravatarImgPath, userIcon);
                writeJson(gravatarJsonPath, gravatarFileName, false, "", "", true);
                uploadImg();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void uploadImg() {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();

            File file = new File(gravatarImgPath);
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpg"), file);
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            builder.addFormDataPart(kGravatar, file.getName(), fileBody);

            MultipartBody requestBody = builder.build();

            Request request = new Request.Builder()
                        .url(String.format(K.kUploadGravatarAPIPath, PrivateURLs.kBaseUrl, user.getString("user_device_id"), user.getString("user_id")))
                        .post(requestBody)
                        .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    uploadImg();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast("上传失败");
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 201) {
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            writeJson(gravatarJsonPath, gravatarFileName, true, json.getString("gravatar_url"), json.getString(kGravatarId), false);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    toast("上传成功");
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        uploadImg();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getDate(){
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        return format.format(date);
    }

    /*
     * 检查设备是否存在SDCard的工具方法
     */
    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    /*
     * 返回
     */
    public void dismissActivity(View v) {
        SettingActivity.this.onBackPressed();
    }

    public void launchThursdaySayActivity(View v) {
        try {
            String noticePath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kLocalNotificationConfigFileName);
            JSONObject notificationJson = new JSONObject(FileUtil.readFile(noticePath));
            notificationJson.put(URLs.kSettingThursdaySay, 0);
            FileUtil.writeFile(noticePath, notificationJson.toString());

            Intent blogLinkIntent = new Intent(SettingActivity.this,ThursdaySayActivity.class);
            blogLinkIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(blogLinkIntent);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void launchDeveloperActivity(View v) {
        Intent developerIntent = new Intent(SettingActivity.this, DeveloperActivity.class);
        developerIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(developerIntent);
    }

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
                logParams.put(URLs.kAction, "退出登录");
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
                logParams.put(URLs.kAction, "点击/设置页面/修改密码");
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
                        String headerPath = String.format("%s/%s", FileUtil.sharedPath(mContext), K.kCachedHeaderConfigFileName);
                        new File(headerPath).delete();
                        headerPath = String.format("%s/%s", FileUtil.dirPath(mContext, K.kHTMLDirName), K.kCachedHeaderConfigFileName);

                        new File(headerPath).delete();

                        /*
                         * Umeng Device Token
                         */
                        String device_token = UmengRegistrar.getRegistrationId(mContext);
                        String pushConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kPushConfigFileName);
                        JSONObject pushJSON = FileUtil.readConfigFile(pushConfigPath);
                        if(!pushJSON.has(URLs.kPushDeviceToken) || pushJSON.getString(URLs.kPushDeviceToken).length() != 44 ||
                            device_token.length() != 44 || !pushJSON.getString(URLs.kPushDeviceToken).equals(device_token)) {
                            pushJSON.put("push_valid", false);
                            pushJSON.put(URLs.kPushDeviceToken, device_token);
                            FileUtil.writeFile(pushConfigPath, pushJSON.toString());
                        }

                        ApiHelper.authentication(SettingActivity.this, user.getString(URLs.kUserNum), user.getString(URLs.kPassword));

                        // pushJSON = FileUtil.readConfigFile(pushConfigPath);
                        // mPushState.setText(pushJSON.has("push_valid") && pushJSON.getBoolean("push_valid") ? "开启" : "关闭");

                        /*
                         * 检测服务器静态资源是否更新，并下载
                         */
                        runOnUiThread(new Runnable() {
                            @Override public void run() {
                                checkAssetsUpdated(false);

                                FileUtil.checkAssets(mContext, URLs.kAssets, false);
                                FileUtil.checkAssets(mContext, URLs.kLoding, false);
                                FileUtil.checkAssets(mContext, URLs.kFonts, true);
                                FileUtil.checkAssets(mContext, URLs.kImages, true);
                                FileUtil.checkAssets(mContext, URLs.kStylesheets, true);
                                FileUtil.checkAssets(mContext, URLs.kJavaScripts, true);
                                FileUtil.checkAssets(mContext, URLs.kBarCodeScan, false);
                                // FileUtil.checkAssets(mContext, URLs.kAdvertisement, false);

                                toast("校正完成");
                            }
                        });
                    } catch (JSONException | IOException e) {
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
                    String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kUserConfigFileName);
                    if((new File(userConfigPath)).exists()) {
                        JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);
                        userJSON.put(URLs.kUseGesturePassword, false);
                        userJSON.put(URLs.kGesturePassword, "");

                        FileUtil.writeFile(userConfigPath, userJSON.toString());
                        String settingsConfigPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kSettingConfigFileName);
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
                logParams.put(URLs.kAction, String.format("点击/设置页面/%s锁屏", isChecked ? "开启" : "禁用"));
                new Thread(mRunnableForLogger).start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /*
     * 切换截屏
     */
    private final CompoundButton.OnCheckedChangeListener mSwitchLongCatListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            try {
                String betaConfigPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kBetaConfigFileName);
                JSONObject betaJSON = FileUtil.readConfigFile(betaConfigPath);

                betaJSON.put("image_within_screen", isChecked);
                FileUtil.writeFile(betaConfigPath, betaJSON.toString());
            } catch (JSONException | IOException e) {
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
            Intent browserIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(K.kPgyerUrl));
            startActivity(browserIntent);
        }
    };

    /**
     * 设置界面，需要显示通知样式的控件，检测是否需要通知
     */
    private void setSettingViewControlBadges() {
        String notificationPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kLocalNotificationConfigFileName);
        if(!(new File(notificationPath)).exists()) {
            return;
        }

        try {
            JSONObject notificationJSON = FileUtil.readConfigFile(notificationPath);
            // 每次进入设置页面都判断密码是否修改以及是否需要更新
            int passwordCount = user.getString(URLs.kPassword).equals(URLs.MD5(K.kInitPassword)) ? 1 : -1;
            notificationJSON.put(URLs.kSettingPassword, passwordCount);

            if (passwordCount > 0) {
                mWarnPWD.setTextColor(Color.parseColor("#808080"));
                mWarnPWD.setTextSize(16);
                mWarnPWD.setText("请修改初始密码");
                mChangePWD.setText("   修改登录密码");
                RedPointView.showRedPoint(mContext, URLs.kSettingPassword, bvChangePWD);
            }
            else {
                mWarnPWD.setVisibility(View.GONE);
                mChangePWD.setText("修改登录密码");
                bvChangePWD.setVisibility(View.GONE);
            }

            if (notificationJSON.getInt(URLs.kSettingPgyer) > 0) {
                mCheckUpgrade.setText("   检测更新");
                RedPointView.showRedPoint(mContext, URLs.kSettingPgyer, bvCheckUpgrade);
            }

            if (notificationJSON.getInt(URLs.kSettingThursdaySay) > 0){
                mCheckThursdaySay.setText("   小四说");
                RedPointView.showRedPoint(mContext, URLs.kSettingThursdaySay, bvCheckThursdaySay);
            }

            FileUtil.writeFile(notificationPath, notificationJSON.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
