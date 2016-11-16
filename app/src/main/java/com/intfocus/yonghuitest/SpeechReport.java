package com.intfocus.yonghuitest;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

import java.io.File;

/**
 * Created by liuruilin on 2016/11/15.
 */

public class SpeechReport {
    private static Context context;
    private static MediaPlayer mediaPlayer;
    private static SpeechSynthesizer mTts;                            // 语音合成对象

    public static void startSpeechSynthesizer(Context mContext,String info) {
        context = mContext;
        mTts = initSpeechSynthesizer(mContext);
        initTtsParms();
        mTts.synthesizeToUri(info,"./sdcard/iflytek.wav",mSynListener);
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
     * 语音合成回调
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
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
        }

        //开始播放
        public void onSpeakBegin() {
        }

        //暂停播放
        public void onSpeakPaused() {
        }

        //播放进度回调
        //percent为播放进度0~100,beginPos为播放音频在文本中开始位置，endPos表示播放音频在文本中结束位置.
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
        }

        //恢复播放回调接口
        public void onSpeakResumed() {
        }

        //会话事件回调接口
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
        }
    };

}
