package com.intfocus.yonghuitest;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.iflytek.cloud.SpeechSynthesizer;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.K;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_selector);


        mPlayButton = (CircleImageView) findViewById(R.id.btn_play);
        mPlayButton.setImageResource(R.drawable.btn_stop);
        mTts = SpeechReport.getmTts(mAppContext);

        Intent intent = getIntent();
        speechAudio = intent.getStringExtra("speechAudio");
        try {
            speechArray = new JSONArray(speechAudio);
            speechArray.put(0,"本次报表针对" + user.getString("role_name") + user.getString("group_name"));
            SpeechReport.speechNum = 0;
            SpeechReport.startSpeechPlayer(mAppContext,speechArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        initListView();
    }

    private void initListView(){
        String speechCachePath = FileUtil.dirPath(mAppContext, K.kHTMLDirName,"PlayData.plist");
        mSpeechList.add("播报列表初始化失败");
        try {
            if (!new File(speechCachePath).exists()) {
                mSpeechList.clear(); //
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
        SpeechListActivity.ListArrayAdapter mArrayAdapter = new SpeechListActivity.ListArrayAdapter(this, R.layout.speech_list_item, mSpeechList);
        mListView.setAdapter(mArrayAdapter);
        mListView.setTextFilterEnabled(true);
    }

    public void onClick(View v) {
        if (mTts.isSpeaking()){
            mTts.stopSpeaking();
            mPlayButton.setImageResource(R.drawable.btn_play);
        }
        else {
            SpeechReport.speechNum = 0;
            SpeechReport.startSpeechPlayer(mAppContext,speechArray);
            mPlayButton.setImageResource(R.drawable.btn_stop);
        }
    }

    public class ListArrayAdapter extends ArrayAdapter<String> {
        private int resourceId;

        public ListArrayAdapter(Context context, int textViewResourceId, List<String> items) {
            super(context, textViewResourceId, items);
            this.resourceId = textViewResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String item = getItem(position).trim();
            LinearLayout listItem = new LinearLayout(getContext());
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
            vi.inflate(resourceId, listItem, true);
            TextView viewItem = (TextView) listItem.findViewById(R.id.speechSelectorItem);
            viewItem.setText(item);
            viewItem.setBackgroundColor(Color.WHITE);

            return listItem;
        }
    }

    /*
     * 返回
     */
    public void dismissActivity(View v) {
        SpeechListActivity.this.onBackPressed();
        finish();
    }
}
