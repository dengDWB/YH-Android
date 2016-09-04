package com.intfocus.yonghuitest.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtil {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * ִ执行一个HTTP GET请求，返回请求响应的HTML
     *
     * @param urlString 请求的URL地址
     * @return 返回请求响应的HTML
     */
    //@throws UnsupportedEncodingException
    public static Map<String, String> httpGet(String urlString, Map<String, String> headers) {
        LogUtil.d("GET", urlString);
        Map<String, String> retMap = new HashMap<>();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .writeTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
        okhttp3.Request.Builder builder = new Request.Builder()
                .url(urlString)
                .addHeader("User-Agent", HttpUtil.webViewUserAgent());

        if (headers.containsKey("ETag")) {
            builder = builder.addHeader("IF-None-Match", headers.get("ETag"));
        }
        if (headers.containsKey("Last-Modified")) {
            builder = builder.addHeader("If-Modified-Since", headers.get("Last-Modified"));
        }
        Response response;
        Request request = builder.build();
        try {
            response = client.newCall(request).execute();
            Headers responseHeaders = response.headers();
            boolean isJSON = false;
            for (int i = 0, len = responseHeaders.size(); i < len; i++) {
                retMap.put(responseHeaders.name(i), responseHeaders.value(i));
                // Log.i("HEADER", String.format("Key : %s, Value: %s", responseHeaders.name(i), responseHeaders.value(i)));
                isJSON = responseHeaders.name(i).equalsIgnoreCase("Content-Type") && responseHeaders.value(i).contains("application/json");
            }
            retMap.put("code", String.format("%d", response.code()));
            retMap.put("body", response.body().string());
            LogUtil.d("BODY", retMap.get("body").toString());

            if(isJSON) {
                LogUtil.d("code", retMap.get("code"));
                LogUtil.d("responseBody", retMap.get("body"));
            }
        } catch (UnknownHostException e) {
            // 400: Unable to resolve host "yonghui.idata.mobi": No address associated with hostname
            if(e != null && e.getMessage() != null) {
                LogUtil.d("UnknownHostException2", e.getMessage());
            }
            retMap.put("code", "400");
            retMap.put("body", "{\"info\": \"请检查网络环境！\"}");
        } catch (Exception e) {
            // Default Response
            retMap.put("code", "400");
            retMap.put("body", "{\"info\": \"请检查网络环境！\"}");

            if(e != null && e.getMessage() != null) {
                String errorMessage = e.getMessage().toLowerCase();
                LogUtil.d("Exception", errorMessage);
                if (errorMessage.contains("unable to resolve host") || errorMessage.contains("failed to connect to")) {
                    retMap.put("code", "400");
                    retMap.put("body", "{\"info\": \"请检查网络环境！\"}");
                } else if (errorMessage.contains("unauthorized")) {
                    retMap.put("code", "401");
                    retMap.put("body", "{\"info\": \"用户名或密码错误\"}");
                }
            }
        }
        return retMap;
    }

    /**
     * ִ执行一个HTTP POST请求，返回请求响应的HTML
     */
    //@throws UnsupportedEncodingException
    //@throws JSONException
    public static Map<String, String> httpPost(String urlString, Map params){
        LogUtil.d("POST", urlString);
        Map<String, String> retMap = new HashMap<>();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .writeTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
        Request request;
        Response response;
        Request.Builder requestBuilder = new Request.Builder();
        if (params != null) {
            try {
                Iterator iter = params.entrySet().iterator();
                JSONObject holder = new JSONObject();

                while (iter.hasNext()) {
                    Map.Entry pairs = (Map.Entry) iter.next();
                    String key = (String) pairs.getKey();

                    if (pairs.getValue() instanceof Map) {
                        Map m = (Map) pairs.getValue();

                        JSONObject data = new JSONObject();
                        for (Object o : m.entrySet()) {
                            Map.Entry pairs2 = (Map.Entry) o;
                            data.put((String) pairs2.getKey(), pairs2.getValue());
                            holder.put(key, data);
                        }
                    } else {
                        holder.put(key, pairs.getValue());
                    }
                }
                requestBuilder.post(RequestBody.create(JSON, holder.toString()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            request = requestBuilder
                    .url(urlString)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-type", "application/json")
                    .addHeader("User-Agent", HttpUtil.webViewUserAgent())
                    .build();
            response = client.newCall(request).execute();

            Headers responseHeaders = response.headers();
            int headerSize = responseHeaders.size();
            for (int i = 0; i < headerSize; i++) {
                retMap.put(responseHeaders.name(i), responseHeaders.value(i));
                LogUtil.d("HEADER", String.format("Key : %s, Value: %s", responseHeaders.name(i),
                    responseHeaders.value(i)));
            }

            retMap.put("code", String.format("%d", response.code()));
            retMap.put("body", response.body().string());

            LogUtil.d("code", retMap.get("code"));
            LogUtil.d("responseBody", retMap.get("body"));
        } catch (UnknownHostException e) {
            if(e != null && e.getMessage() != null) {
                LogUtil.d("UnknownHostException", e.getMessage());
            }
            retMap.put("code", "400");
            retMap.put("body", "{\"info\": \"请检查网络环境！\"}");
        } catch (Exception e) {
            // Default Response
            retMap.put("code", "400");
            retMap.put("body", "{\"info\": \"请检查网络环境！\"}");

            if(e != null && e.getMessage() != null) {
                String errorMessage = e.getMessage().toLowerCase();
                LogUtil.d("Exception", errorMessage);
                if (errorMessage.contains("unable to resolve host") || errorMessage.contains("failed to connect to")) {
                    retMap.put("code", "400");
                    retMap.put("body", "{\"info\": \"请检查网络环境！\"}");
                } else if (errorMessage.contains("unauthorized")) {
                    retMap.put("code", "401");
                    retMap.put("body", "{\"info\": \"用户名或密码错误\"}");
                }
            }
        }
        return retMap;
    }


    /**
     * ִ执行一个HTTP POST请求，返回请求响应的HTML
     */
    public static Map<String, String> httpPost(String urlString, JSONObject params) {
        LogUtil.d("POST2", urlString);
        Map<String, String> retMap = new HashMap<>();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .writeTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
        Request request;
        Response response;
        Request.Builder requestBuilder = new Request.Builder();

        if (params != null) {
            requestBuilder.post(RequestBody.create(JSON, params.toString()));
            LogUtil.d("PARAM", params.toString());
        }
        try {
            request = requestBuilder
                    .url(urlString)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-type", "application/json")
                    .addHeader("User-Agent", HttpUtil.webViewUserAgent())
                    .build();
            response = client.newCall(request).execute();
            Headers responseHeaders = response.headers();
            for (int i = 0, headerSize = responseHeaders.size(); i < headerSize; i++) {
                retMap.put(responseHeaders.name(i), responseHeaders.value(i));
                LogUtil.d("HEADER", String.format("Key : %s, Value: %s", responseHeaders.name(i),
                    responseHeaders.value(i)));
            }
            retMap.put("code", String.format("%d", response.code()));
            retMap.put("body", response.body().string());

            LogUtil.d("code", retMap.get("code"));
            LogUtil.d("responseBody", retMap.get("body"));
        } catch (UnknownHostException e) {
            if(e != null && e.getMessage() != null) {
                LogUtil.d("UnknownHostException2", e.getMessage());
            }
            retMap.put("code", "400");
            retMap.put("body", "{\"info\": \"请检查网络环境！\"}");
        } catch (Exception e) {
            retMap.put("code", "400");
            retMap.put("body", "{\"info\": \"请检查网络环境！\"}");

            if(e != null && e.getMessage() != null) {
                String errorMessage = e.getMessage().toLowerCase();
                LogUtil.d("Exception2", errorMessage);
                if (errorMessage.contains("unable to resolve host") || errorMessage.contains("failed to connect to")) {
                    retMap.put("code", "400");
                    retMap.put("body", "{\"info\": \"请检查网络环境！\"}");
                } else if (errorMessage.contains("unauthorized")) {
                    retMap.put("code", "401");
                    retMap.put("body", "{\"info\": \"用户名或密码错误\"}");
                }
            }
        }
        return retMap;
    }

    public static String UrlToFileName(String urlString) {
        String path = "default";
        try {
            urlString = urlString.replace(URLs.kBaseUrl, "");
            URI uri = new URI(urlString);
            path = uri.getPath().replace("/", "_");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.format("%s.html", path);
    }

    private static String webViewUserAgent() {
        String userAgent = System.getProperty("http.agent");
        if (userAgent == null) {
            userAgent = "Mozilla/5.0 (Linux; U; Android 4.3; en-us; HTC One - 4.3 - API 18 - 1080x1920 Build/JLS36G) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 default-by-hand";
        }

        return userAgent;
    }

    private static void dealWithException(String errorMessage, Map<String, String> retMap) {
        LogUtil.d("DDEBUG", errorMessage);
        errorMessage = errorMessage.toLowerCase();
        if (errorMessage.contains("timed out") || errorMessage.contains("unable to resolve host") || errorMessage.contains("failed to connect to")) {
            retMap.put("code", "408");
            retMap.put("body", "{\"info\": \"连接超时,请检查网络环境\"}");
        }
    }

    /*
     * http://stackoverflow.com/questions/2802472/detect-network-connection-type-on-android
     */

    /**
     * Get the network info
     * @param context
     * @return
     */
    public static NetworkInfo getNetworkInfo(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }

    /**
     * Check if there is any connectivity
     * @param context
     * @return
     */
    public static boolean isConnected(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return (info != null && info.isConnected() && HttpUtil.isConnectionFast(info.getType(), tm.getNetworkType()));
    }

    /**
     * Check if there is any connectivity to a Wifi network
     * @param context
     * @param type
     * @return
     */
    public static boolean isConnectedWifi(Context context){
        NetworkInfo info = HttpUtil.getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
    }

    /**
     * Check if there is any connectivity to a mobile network
     * @param context
     * @param type
     * @return
     */
    public static boolean isConnectedMobile(Context context){
        NetworkInfo info = HttpUtil.getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * Check if there is fast connectivity
     * @param context
     * @return
     */
    public static boolean isConnectedFast(Context context){
        NetworkInfo info = HttpUtil.getNetworkInfo(context);
        return (info != null && info.isConnected() && HttpUtil.isConnectionFast(info.getType(),info.getSubtype()));
    }

    /**
     * Check if the connection is fast
     * @param type
     * @param subType
     * @return
     */
    public static boolean isConnectionFast(int type, int subType){
        if(type== ConnectivityManager.TYPE_WIFI){
            return true;
        }else if(type==ConnectivityManager.TYPE_MOBILE){
            switch(subType){
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return false; // ~ 14-64 kbps
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return true; // ~ 400-1000 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return true; // ~ 600-1400 kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return false; // ~ 100 kbps
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return true; // ~ 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return true; // ~ 700-1700 kbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return true; // ~ 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return true; // ~ 400-7000 kbps
            /*
             * Above API level 7, make sure to set android:targetSdkVersion
             * to appropriate level to use these
             */
                case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11
                    return true; // ~ 1-2 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
                    return true; // ~ 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
                    return true; // ~ 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
                    return false; // ~25 kbps
                case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
                    return true; // ~ 10+ Mbps
                // Unknown
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                default:
                    return false;
            }
        }
        else{
            return false;
        }
    }
}