package com.intfocus.yonghuitest;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.iflytek.cloud.SpeechSynthesizer;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.URLs;
import com.intfocus.yonghuitest.view.CircleImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuruilin on 2016/11/30.
 */

public class SpeechListActivity extends BaseActivity{
    private ListView mListView;
    private ArrayList<String> mSpeechList = new ArrayList<>();
    private SpeechSynthesizer mTts;
    private CircleImageView mPlayButton;
    private String speechAudio;
    private JSONArray speechArray;
    private SpeechListAdapter.ListArrayAdapter mArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_selector);

        mPlayButton = (CircleImageView) findViewById(R.id.btn_play);
        mPlayButton.setImageResource(R.drawable.btn_stop);
        mTts = SpeechReport.getmTts(mAppContext);
        initSpeechInfo();
        initListView();

        if (!mTts.isSpeaking()) {
            SpeechReport.speechNum = 0;
            SpeechReport.startSpeechPlayer(mAppContext,speechArray);
        }
    }

    private void initSpeechInfo() {
        try {
            Intent intent = getIntent();
            speechAudio = intent.getStringExtra("speechAudio");
            speechArray = new JSONArray(speechAudio);
            speechArray.put(0,"本次报表针对" + user.getString("role_name") + user.getString("group_name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initListView(){
        String speechCachePath = FileUtil.dirPath(mAppContext, K.kHTMLDirName,"PlayData.plist");
        mSpeechList.add("播报列表初始化失败");
        try {
            if (new File(speechCachePath).exists()) {
                mSpeechList.clear();
                mSpeechList.add("本次报表针对" + user.getString("role_name") + user.getString("group_name"));
                JSONObject speechJson = FileUtil.readConfigFile(speechCachePath);
                JSONArray speechArray = speechJson.getJSONArray("data");
                for (int i = 0, len = speechArray.length(); i < len; i++) {
                    JSONObject speechInfo = (JSONObject) speechArray.get(i);
                    mSpeechList.add(speechInfo.getString("title"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mListView = (ListView) findViewById(R.id.list_speech);
        mListView.setOnItemClickListener(mItemClickListener);
        mArrayAdapter = SpeechListAdapter.SpeechListAdapter(this, R.layout.speech_list_item, mSpeechList);
        mListView.setAdapter(mArrayAdapter);
        mListView.setTextFilterEnabled(true);
    }

    public void onClick(View v) {
        SpeechReport.speechNum = 0;
        mArrayAdapter.notifyDataSetChanged();
        if (mTts.isSpeaking()){

            mTts.stopSpeaking();
            mPlayButton.setImageResource(R.drawable.btn_play);
        }
        else {
            SpeechReport.startSpeechPlayer(mAppContext,speechArray);
            mPlayButton.setImageResource(R.drawable.btn_stop);
        }
    }

    /*
     * listview 点击事件
     */
    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                SpeechReport.speechNum = position;
                SpeechReport.startSpeechPlayer(mAppContext,speechArray);
                mPlayButton.setImageResource(R.drawable.btn_stop);
                mArrayAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /*
     * 返回
     */
    public void dismissActivity(View v) {
        SpeechListActivity.this.onBackPressed();
        finish();
    }
}
