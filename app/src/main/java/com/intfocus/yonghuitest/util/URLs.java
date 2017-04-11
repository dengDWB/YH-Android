package com.intfocus.yonghuitest.util;

import android.content.Context;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * api链接，宏
 *
 * @author jay
 * @version 1.0
 * @created 2016-01-06
 */
public class URLs implements Serializable {


    public final static String kTabKpi                   = "tab_kpi";
    public final static String kTabAnalyse               = "tab_analyse";
    public final static String kTabApp                   = "tab_app";
    public final static String kTabMessage               = "tab_message";

    public final static String kJSInterfaceName          = "AndroidJSBridge";

    public final static String kAssets                   = "assets";
    public final static String kLoading                  = "loading";
    public final static String kFonts                    = "fonts";
    public final static String kImages                   = "images";
    public final static String kStylesheets              = "stylesheets";
    public final static String kJavaScripts              = "javascripts";
    public final static String kBarCodeScan              = "BarCodeScan";
    public final static String kAdvertisement            = "advertisement";

    public final static String kAction                   = "action";
    public final static String kPassword                 = "password";
    public final static String kSettingThursdaySay       = "setting_thursday_say";
    public final static String kRoleId                   = "role_id";
    public final static String kGroupId                  = "group_id";
    public final static String kSetting                  = "setting";
    public final static String kSettingPgyer             = "setting_pgyer";
    public final static String kSettingPassword          = "setting_password";
    public final static String kData                     = "data";
    public final static String kCodeInfo                 = "code_info";
    public final static String kCodeType                 = "code_type";
    public final static String kBannerName               = "bannerName";
    public final static String kObjectId                 = "objectID";
    public final static String kObjectType               = "objectType";
    public final static String kObjTitle                 = "obj_title";
    public final static String kObjType                  = "obj_type";
    public final static String kLink                     = "link";
    public final static String kUserNum                  = "user_num";
    public final static String kUserName                 = "user_name";
    public final static String kRoleName                 = "role_name";
    public final static String kGroupName                = "group_name";
    public final static String kStore                    = "store";
    public final static String kStoreIds                 = "store_ids";
    public final static String kName                     = "name";
    public final static String kUseGesturePassword       = "use_gesture_password";
    public final static String kGesturePassword          = "gesture_password";
    public final static String kIsLogin                  = "is_login";

    public final static String kCode                     = "code";
    public final static String kBody                     = "body";
    public final static String kETag                     = "ETag";
    public final static String kLastModified             = "Last_Modified";

    public final static boolean kIsQRCode                = false;

    public static String storage_base(Context context) {
        //    String path = "";
        //    if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        //        path = String.format("%s/com.intfocus.yonghuitest", Environment.getExternalStorageDirectory().getAbsolutePath());
        //    } else {
        //        path =String.format("%s/com.intfocus.yonghuitest", context.getApplicationContext().getFilesDir());
        //    }
        return context.getFilesDir().getPath();
    }

    public static String timestamp() {
        return (new SimpleDateFormat("yyyyMMddKKmmss")).format(new Date());
    }

    /*
     * UI 版本
     */
    public static String currentUIVersion(Context mContext) {
        //try {
        //    String betaConfigPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kBetaConfigFileName);
        //    JSONObject betaJSON = new JSONObject();
        //    if (new File(betaConfigPath).exists()) {
        //        betaJSON = FileUtil.readConfigFile(betaConfigPath);
        //    }
        //
        //    return betaJSON.has("old_ui") && betaJSON.getBoolean("old_ui") ? "v1" : "v2";
        //} catch (JSONException e) {
        //    e.printStackTrace();
        //}
        return "v2";
    }
    /**
     * 对URL进行格式处理
     *
     * @param path 路径
     * @return "http://" + URLEncoder.encode(path)
     */
    private static String formatURL(String path) throws UnsupportedEncodingException {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return "http://" + URLEncoder.encode(path,"UTF-8");
    }

    /**
     * MD5加密-32位
     *
     * @param inStr 需要MD5加密的内容
     * @return hexValue.toString()
     */
    public static String MD5(String inStr) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }
        char[] charArray = inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }

        byte[] md5Bytes = md5.digest(byteArray);

        StringBuilder hexValue = new StringBuilder();

        for (byte bytes : md5Bytes) {
            int val = ((int) bytes) & 0xff;
            if (val < 16)
                hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }

        return hexValue.toString();
    }
}
