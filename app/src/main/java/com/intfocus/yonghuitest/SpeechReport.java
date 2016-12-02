package com.intfocus.yonghuitest;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.HttpUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * Created by liuruilin on 2016/11/15.
 */

public class SpeechReport {
    private static Context context;
    private static MediaPlayer mediaPlayer;
    private static SpeechSynthesizer mTts;                            // 语音合成对象
    public static JSONArray speechArray;
    public static int speechNum;

    public static void startSpeechSynthesizer(Context mContext,String info) {
        context = mContext;
        mTts = initSpeechSynthesizer(mContext);
        initTtsParms();
        mTts.synthesizeToUri(info,"./sdcard/iflytek.wav",mSynListener);
    }

    public static void startSpeechPlayer(Context mContext,JSONArray array) {
        try {
            context = mContext;
            speechArray = array;
            LogUtil.d("speechInfo",array.toString());
            mTts = initSpeechSynthesizer(mContext);
            initTtsParms();
            mTts.startSpeaking(array.getString(speechNum),mPlayListener);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    private static SpeechSynthesizer initSpeechSynthesizer(Context mContext) {
        return SpeechSynthesizer.createSynthesizer(mContext, null);
    }

    public static MediaPlayer getMediaPlayer() {
        if (mediaPlayer == null) {
            return new MediaPlayer();
        }
        else {
            return mediaPlayer;
        }
    }

    public static SpeechSynthesizer getmTts(Context mContext) {
        if (mTts == null) {
            return initSpeechSynthesizer(mContext);
        }
        else {
            return mTts;
        }
    }

    /*
     * 语音合成参数
     */
    private static void initTtsParms() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);

        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);   //设置云端
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaofeng");                   //设置发音人
        mTts.setParameter(SpeechConstant.SPEED, "50");                              //设置语速
        mTts.setParameter(SpeechConstant.PITCH, "50");                              //设置合成音调
        mTts.setParameter(SpeechConstant.VOLUME, "80");                             //设置音量，范围0~100
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
    }

    /*
     * 语音合成回调 - 本地合成
     */
    private static SynthesizerListener mSynListener = new SynthesizerListener() {
        //会话结束回调接口，没有错误时，error为null
        public void onCompleted(SpeechError error) {
            Uri uri = Uri.fromFile(new File("./sdcard/iflytek.wav"));
            mediaPlayer = MediaPlayer.create(context,uri);
            mediaPlayer.start();
            Log.i("voicePlay",mediaPlayer.getDuration() + " is time");
        }

        //缓冲进度回调
        //percent为缓冲进度0~100，beginPos为缓冲音频在文本中开始位置，endPos表示缓冲音频在文本中结束位置，info为附加信息。
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {}
        //开始播放
        public void onSpeakBegin() {}
        //暂停播放
        public void onSpeakPaused() {}
        //播放进度回调
        //percent为播放进度0~100,beginPos为播放音频在文本中开始位置，endPos表示播放音频在文本中结束位置.
        public void onSpeakProgress(int percent, int beginPos, int endPos) {}
        //恢复播放回调接口
        public void onSpeakResumed() {}
        //会话事件回调接口
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {}
    };

    /*
     * 语音合成回调 - 云端合成
     */
    public static SynthesizerListener mPlayListener = new SynthesizerListener() {
        //会话结束回调接口，没有错误时，error为null
        public void onCompleted(SpeechError error) {
            if (speechNum <= speechArray.length()) {
                speechNum++;
                startSpeechPlayer(context,speechArray);
            }
            else {
                mTts.destroy();
            }
        }

        //缓冲进度回调
        //percent为缓冲进度0~100，beginPos为缓冲音频在文本中开始位置，endPos表示缓冲音频在文本中结束位置，info为附加信息。
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {}
        //开始播放
        public void onSpeakBegin() {}
        //暂停播放
        public void onSpeakPaused() {}
        //播放进度回调
        //percent为播放进度0~100,beginPos为播放音频在文本中开始位置，endPos表示播放音频在文本中结束位置.
        public void onSpeakProgress(int percent, int beginPos, int endPos) {}
        //恢复播放回调接口
        public void onSpeakResumed() {}
        //会话事件回调接口
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {}
    };

    public static String infoProcess(final Context mContext,final String urlString,final String type) {
        String mAssetsPath = FileUtil.dirPath(mContext, K.kHTMLDirName);
        String speechCachePath = FileUtil.dirPath(mContext, K.kHTMLDirName,"PlayData.plist");
        final StringBuilder speechAudio = new StringBuilder();
        speechAudio.append("语音合成错误");
        try {
            Map<String,String> responseHeader = ApiHelper.checkResponseHeader(urlString,mAssetsPath);
            Map<String,String> response = HttpUtil.httpGet(urlString,responseHeader);
            if (response.get("code").equals("200")){
                speechAudio.delete(0,speechAudio.length()); // 若获取到播报数据,则清空 speechAudio 内信息
                JSONObject speechJson = new JSONObject(response.get("body"));
                switch (type) {
                    case "kpi":
                        FileUtil.writeFile(speechCachePath,speechJson.toString());
                        JSONArray speechList = speechJson.getJSONArray("data");
                        speechAudio.append("[");
                        speechAudio.append("\"播报内容如下:\",");
                        for (int i = 0;i < speechList.length();i++) {
                            if (i != 0) {
                                speechAudio.append(",");
                            }
                            JSONObject speechInfo = (JSONObject) speechList.get(i);
                            speechAudio.append("\"" + speechInfo.get("title"));
                            JSONArray audioList = speechInfo.getJSONArray("audio");
                            for (int j = 0;j < audioList.length();j++) {
                                speechAudio.append(audioList.getString(j));
                            }
                            speechAudio.append("\"");
                        }
                        speechAudio.append(",\"以上是全部内容，谢谢收听\"");
                        speechAudio.append("]");
                        break;

                    case "report":
                        speechAudio.append("报表名称:" + speechJson.get("title"));
                        JSONArray audioList = speechJson.getJSONArray("audio");
                        for (int j = 0;j < audioList.length();j++) {
                            speechAudio.append(audioList.getString(j));
                        }
                        speechAudio.append("以上是全部内容，谢谢收听");
                        break;

                    default:
                        break;
                }
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        return speechAudio.toString();
    }

}
