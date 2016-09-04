package com.intfocus.yonghuitest.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.OpenUDID.OpenUDID_manager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ApiHelper {

    /*
     * 用户登录验证
     * params: {device: {name, platform, os, os_version, uuid}}
     */
    public static String authentication(Context context, String username, String password) {
        String responseState = "success", urlString = String.format(URLs.API_USER_PATH, URLs.kBaseUrl, "android", username, password);
        try {
            JSONObject device = new JSONObject();
            device.put("name", android.os.Build.MODEL);
            device.put("platform", "android");
            device.put("os", android.os.Build.MODEL);
            device.put("os_version", Build.VERSION.RELEASE);
            device.put("uuid", OpenUDID_manager.getOpenUDID());

            JSONObject params = new JSONObject();
            params.put("device", device);
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            params.put("app_version", String.format("a%s", packageInfo.versionName));
            Log.i("DeviceParams", params.toString());

            Map<String, String> response = HttpUtil.httpPost(urlString, params);
            String userConfigPath = String.format("%s/%s", FileUtil.basePath(context), URLs.USER_CONFIG_FILENAME);
            JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);
            userJSON.put("password", password);
            userJSON.put("is_login", response.get("code").equals("200"));

            if (response.get("code").equals("400")) {
                return "请检查网络环境";
            } else if (response.get("code").equals("401")) {
                return new JSONObject(response.get("body")).getString("info");
            } else if (response.get("code").equals("408")) {
                return "连接超时";
            } else if (!response.get("code").equals("200")) {
                return response.get("body");
            }
            // FileUtil.dirPath 需要优先写入登录用户信息
            JSONObject responseJSON = new JSONObject(response.get("body"));
            userJSON = ApiHelper.merge(userJSON, responseJSON);
            FileUtil.writeFile(userConfigPath, userJSON.toString());

            String settingsConfigPath = FileUtil.dirPath(context, URLs.CONFIG_DIRNAME, URLs.SETTINGS_CONFIG_FILENAME);
            if ((new File(settingsConfigPath)).exists()) {
                JSONObject settingJSON = FileUtil.readConfigFile(settingsConfigPath);
                userJSON.put("use_gesture_password", settingJSON.has("use_gesture_password") ? settingJSON.getBoolean("use_gesture_password") : false);
                userJSON.put("gesture_password", settingJSON.has("gesture_password") ? settingJSON.getString("gesture_password") : "");
            } else {
                userJSON.put("use_gesture_password", false);
                userJSON.put("gesture_password", "");
            }

            JSONObject assetsJSON = userJSON.getJSONObject("assets");
            userJSON.put("fonts_md5", assetsJSON.getString("fonts_md5"));
            userJSON.put("images_md5", assetsJSON.getString("images_md5"));
            userJSON.put("stylesheets_md5", assetsJSON.getString("stylesheets_md5"));
            userJSON.put("javascripts_md5", assetsJSON.getString("javascripts_md5"));

            FileUtil.writeFile(userConfigPath, userJSON.toString());

            Log.i("CurrentUser", userJSON.toString());
            if (response.get("code").equals("200")) {
                // 第三方消息推送，设备标识
                ApiHelper.pushDeviceToken(context, userJSON.getString("device_uuid"));

                FileUtil.writeFile(settingsConfigPath, userJSON.toString());
            } else {
                responseState = responseJSON.getString("info");
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseState = e.getMessage();
        }
        return responseState;
    }

    /*
     *  获取报表网页数据
     */
    public static void reportData(Context context, String groupID, String templateID, String reportID) {
        String urlString = String.format(URLs.API_DATA_PATH, URLs.kBaseUrl, groupID, templateID, reportID);
        String javascriptPath = FileUtil.reportJavaScriptDataPath(context, groupID, templateID, reportID);

        String assetsPath = FileUtil.sharedPath(context);
        Map<String, String> headers = ApiHelper.checkResponseHeader(urlString, assetsPath);
        Map<String, String> response = HttpUtil.httpGet(urlString, headers);

        if (response.get("code").equals("200")) {
            try {
                ApiHelper.storeResponseHeader(urlString, assetsPath, response);

                FileUtil.writeFile(javascriptPath, response.get("body"));

                String searchItemsPath = String.format("%s.search_items", javascriptPath);
                File searchItemsFile = new File(searchItemsPath);
                if(searchItemsFile.exists()) {
                    searchItemsFile.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.i("Code", response.get("code"));
        }
    }

    /*
     * 发表评论
     */
    public static void writeComment(int userID, int objectType, int objectID, Map params) throws UnsupportedEncodingException {
        String urlString = String.format(URLs.API_COMMENT_PATH, URLs.kBaseUrl, userID, objectID,
            objectType);

        Map<String, String> response = HttpUtil.httpPost(urlString, params);
        Log.i("WriteComment", response.get("code"));
        Log.i("WriteComment", response.get("body"));
    }

    public static Map<String, String> httpGetWithHeader(String urlString, String assetsPath, String relativeAssetsPath) {
        Map<String, String> retMap = new HashMap<>();

        String urlKey = urlString.contains("?") ? TextUtils.split(urlString, "?")[0] : urlString;

        try {
            Map<String, String> headers = ApiHelper.checkResponseHeader(urlString, assetsPath);

            Map<String, String> response = HttpUtil.httpGet(urlKey, headers);
            String statusCode = response.get("code");
            retMap.put("code", statusCode);

            String htmlName = HttpUtil.UrlToFileName(urlString);
            String htmlPath = String.format("%s/%s", assetsPath, htmlName);
            retMap.put("path", htmlPath);

            if (statusCode.equals("200")) {
                ApiHelper.storeResponseHeader(urlKey, assetsPath, response);

                String htmlContent = response.get("body");
                htmlContent = htmlContent.replace("/javascripts/", String.format("%s/javascripts/", relativeAssetsPath));
                htmlContent = htmlContent.replace("/stylesheets/", String.format("%s/stylesheets/", relativeAssetsPath));
                htmlContent = htmlContent.replace("/images/", String.format("%s/images/", relativeAssetsPath));
                FileUtil.writeFile(htmlPath, htmlContent);
            } else {
                retMap.put("code", statusCode);
            }
        } catch (Exception e) {
            retMap.put("code", "500");
            e.printStackTrace();
        }

        return retMap;
    }

    public static Map<String, String> resetPassword(String userID, String newPassword) {
        Map<String, String> retMap = new HashMap<>();

        try {
            String urlString = String.format(URLs.API_RESET_PASSWORD_PATH, URLs.kBaseUrl, userID);

            Map<String, String> params = new HashMap<>();
            params.put("password", newPassword);
            retMap = HttpUtil.httpPost(urlString, params);
        } catch (Exception e) {
            e.printStackTrace();
            retMap.put("code", "500");
            retMap.put("body", e.getLocalizedMessage());
        }
        return retMap;
    }

    /*
     * 缓存文件中，清除指定链接的内容
     *
     * @param 链接
     * @param 缓存头文件相对文件夹
     */
    public static void clearResponseHeader(String urlKey, String assetsPath) {
        String headersFilePath = String.format("%s/%s", assetsPath, URLs.CACHED_HEADER_FILENAME);
        if (!(new File(headersFilePath)).exists()) {
            return;
        }

        JSONObject headersJSON = FileUtil.readConfigFile(headersFilePath);
        if (headersJSON.has(urlKey)) {
            try {
                headersJSON.remove(urlKey);
                Log.i("clearResponseHeader", String.format("%s[%s]", headersFilePath, urlKey));

                FileUtil.writeFile(headersFilePath, headersJSON.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 从缓存头文件中，获取指定链接的ETag/Last-Modified
     *
     * @param urlKey 链接
     * @param assetsPath 缓存头文件相对文件夹
     */
    private static Map<String, String> checkResponseHeader(String urlKey, String assetsPath) {
        Map<String, String> headers = new HashMap<>();

        try {
            JSONObject headersJSON = new JSONObject();

            String headersFilePath = String.format("%s/%s", assetsPath, URLs.CACHED_HEADER_FILENAME);
            if ((new File(headersFilePath)).exists()) {
                headersJSON = FileUtil.readConfigFile(headersFilePath);
            }

            JSONObject headerJSON;
            if (headersJSON.has(urlKey)) {
                headerJSON = (JSONObject) headersJSON.get(urlKey);

                if (headerJSON.has("ETag")) {
                    headers.put("ETag", headerJSON.getString("ETag"));
                }
                if (headerJSON.has("Last-Modified")) {
                    headers.put("Last-Modified", headerJSON.getString("Last-Modified"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return headers;
    }

    /**
     * 把服务器响应的ETag/Last-Modified存入本地
     *
     * @param urlKey 链接
     * @param assetsPath 缓存头文件相对文件夹
     * @param response 服务器响应的ETag/Last-Modifiede
     */
    private static void storeResponseHeader(String urlKey, String assetsPath, Map<String, String> response) {
        try {
            JSONObject headersJSON = new JSONObject();

            String headersFilePath = String.format("%s/%s", assetsPath, URLs.CACHED_HEADER_FILENAME);
            if ((new File(headersFilePath)).exists()) {
                headersJSON = FileUtil.readConfigFile(headersFilePath);
            }

            JSONObject headerJSON = new JSONObject();

            if (response.containsKey("ETag")) {
                headerJSON.put("ETag", response.get("ETag"));
            }
            if (response.containsKey("Last-Modified")) {
                headerJSON.put("Last-Modified", response.get("Last-Modified"));
            }

            headersJSON.put(urlKey, headerJSON);
            FileUtil.writeFile(headersFilePath, headersJSON.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 合并两个JSONObject
     *
     * @param obj JSONObject
     * @param other JSONObject
     * @return 合并后的JSONObject
     */
    public static JSONObject merge(JSONObject obj, JSONObject other) {
        try {
            Iterator it = other.keys();
            while (it.hasNext()) {
                String key = (String) it.next();
                obj.put(key, other.get(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    /**
     * 下载文件
     *
     * @param context 上下文
     * @param urlString 下载链接
     * @param outputFile 写入本地文件路径
     */
    public static void downloadFile(Context context, String urlString, File outputFile) {
        try {
            URL url = new URL(urlString);
            String headerPath = String.format("%s/%s/%s", FileUtil.basePath(context), URLs.CACHED_DIRNAME, URLs.CACHED_HEADER_FILENAME);

            JSONObject headerJSON = new JSONObject();
            if ((new File(headerPath)).exists()) {
                headerJSON = FileUtil.readConfigFile(headerPath);
            }

            URLConnection conn = url.openConnection();
            String etag = conn.getHeaderField("ETag");

            boolean isDownloaded = outputFile.exists() && headerJSON.has(urlString) && etag != null && !etag.isEmpty() && headerJSON.getString(urlString).equals(etag);

            if (isDownloaded) {
                Log.i("downloadFile", "exist - " + outputFile.getAbsolutePath());
            } else {
                InputStream in = url.openStream();
                FileOutputStream fos = new FileOutputStream(outputFile);

                int length;
                byte[] buffer = new byte[1024];// buffer for portion of data from connection
                while ((length = in.read(buffer)) > -1) {
                    fos.write(buffer, 0, length);
                }
                fos.close();
                in.close();

                if (etag != null && !etag.isEmpty()) {
                    headerJSON.put(urlString, etag);
                    FileUtil.writeFile(headerPath, headerJSON.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传锁屏信息
     *
     * @param deviceID 设备标识
     * @param password 锁屏密码
     * @param state 是否启用锁屏
     */
    public static void screenLock(String deviceID, String password, boolean state) {
        String urlString = String.format(URLs.API_SCREEN_LOCK_PATH, URLs.kBaseUrl, deviceID);

        Map<String, String> params = new HashMap<>();
        params.put("screen_lock_state", "1");
        params.put("screen_lock_type", "4位数字");
        params.put("screen_lock", password);

        HttpUtil.httpPost(urlString, params);
    }

    /**
     * 上传用户行为
     *
     * @param context 上下文
     * @param param 用户行为
     */
    public static void actionLog(Context context, JSONObject param) {
        try {
            String userConfigPath = String.format("%s/%s", FileUtil.basePath(context), URLs.USER_CONFIG_FILENAME);
            JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);

            param.put("user_id", userJSON.getInt("user_id"));
            param.put("user_name", userJSON.getString("user_name"));
            param.put("user_device_id", userJSON.getInt("user_device_id"));

            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            param.put("app_version", String.format("a%s", packageInfo.versionName));

            JSONObject params = new JSONObject();
            params.put("action_log", param);

            JSONObject userParams = new JSONObject();
            userParams.put("user_name", userJSON.getString("user_name"));
            userParams.put("user_pass", userJSON.getString("password"));
            params.put("user", userParams);

            String urlString = String.format(URLs.API_ACTION_LOG_PATH, URLs.kBaseUrl);
            HttpUtil.httpPost(urlString, params);
        } catch (JSONException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     *  消息推送， 设备标识
     *
     *  @param deviceUUID  设备ID
     *
     *  @return 服务器是否更新成功
     */
    public static boolean pushDeviceToken(Context context, String deviceUUID) {
        try {
            String pushConfigPath = String.format("%s/%s", FileUtil.basePath(context), URLs.PUSH_CONFIG_FILENAME);
            JSONObject pushJSON = FileUtil.readConfigFile(pushConfigPath);

            if(pushJSON.has("push_valid") && pushJSON.getBoolean("push_valid") && pushJSON.has("push_device_token") && pushJSON
                .getString("push_device_token").length() == 44) return true;
            if(pushJSON.has("push_device_token") && pushJSON.getString("push_device_token").length() != 44) return false;

            if(pushJSON.has("push_device_token")) {
                String urlString = String.format(URLs.API_PUSH_DEVICE_TOKEN_PATH, URLs.kBaseUrl, deviceUUID, pushJSON.getString("push_device_token"));
                Map<String, String> response = HttpUtil.httpPost(urlString, new JSONObject());
                JSONObject responseJSON = new JSONObject(response.get("body"));

                pushJSON.put("push_valid",
                    responseJSON.has("valid") && responseJSON.getBoolean("valid"));
                FileUtil.writeFile(pushConfigPath, pushJSON.toString());

                return pushJSON.has("push_valid") && pushJSON.getBoolean("push_valid");
            } else {
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     *  二维码扫描
     *
     *  @param groupID    群组ID
     *  @param roleID     角色ID
     *  @param userNum    用户编号
     *  @param storeID    门店ID
     *  @param codeInfo   条形码信息
     *  @param codeType   条形码或二维码
     */
    public static void barCodeScan(Context mContext, String groupID, String roleID, String userNum, String storeID, String codeInfo, String codeType) {
        try {
            JSONObject params = new JSONObject();
            params.put("code_info", codeInfo);
            params.put("code_type", codeType);

            String urlString = String.format(URLs.API_BARCODE_SCAN_PATH, URLs.kBaseUrl, groupID, roleID, userNum, storeID, codeInfo, codeType);
            Map<String, String> response = HttpUtil.httpGet(urlString, new HashMap());
            // Map<String, String> response = HttpUtil.httpPost(urlString, params);

            String responseString = response.get("body");
            if (!response.get("code").equals("200") && !response.get("code").equals("201")) {
                responseString = "{\"chart\": \"[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]\", \"tabs\": [{ title: \"提示\", table: { length: 1, \"1\": [\"获取数据失败...\"]}}]}";
            }

            FileUtil.barCodeScanResult(mContext, responseString);
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }
}
