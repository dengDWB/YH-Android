package com.intfocus.yonghuitest.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.intfocus.yonghuitest.BaseActivity;
import com.intfocus.yonghuitest.DeveloperActivity;
import com.intfocus.yonghuitest.LoginActivity;
import com.intfocus.yonghuitest.R;
import com.intfocus.yonghuitest.util.HttpUtil;
import com.intfocus.yonghuitest.util.K;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by liuruilin on 2017/3/28.
 */

public class SettingActivity extends BaseActivity {
    private ArrayAdapter<String> mListAdapter;
    private TextView tvUserID, tvRoleID, tvGroupID;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mContext = this;
        initSettingListItem();
        initUserInfo();
    }

    /*
     * 用户信息初始化
     */
    private void initUserInfo() {
        tvUserID = (TextView) findViewById(R.id.user_id);
        tvRoleID = (TextView) findViewById(R.id.role_id);
        tvGroupID = (TextView) findViewById(R.id.group_id);

        try {
            tvUserID.setText(user.getString("user_name"));
            tvRoleID.setText(user.getString("role_name"));
            tvGroupID.setText(user.getString("group_name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
     * 个人信息页菜单项初始化
     */
    private void initSettingListItem() {
        ArrayList<String> listItem = new ArrayList<>();
        String[] itemName = {"基本信息", "应用信息", "选项配置", "消息推送", "更新日志", "开发者选项"};

        for (int i = 0; i < itemName.length; i++) {
            listItem.add(itemName[i]);
        }

        mListAdapter = new ArrayAdapter(this, R.layout.list_item_setting, R.id.item_setting, listItem);

        ListView listView = (ListView) findViewById(R.id.list_setting);
        listView.setAdapter(mListAdapter);
        listView.setOnItemClickListener(mListItemListener);
    }

    /*
     * 个人信息菜单项点击事件
     */
    private ListView.OnItemClickListener mListItemListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            TextView mItemText = (TextView) arg1.findViewById(R.id.item_setting);
            switch (mItemText.getText().toString()) {
                case "基本信息" :
                    Intent userInfoIntent = new Intent(mContext, SettingListActivity.class);
                    userInfoIntent.putExtra("type", "基本信息");
                    startActivity(userInfoIntent);
                    break;

                case "应用信息" :
                    Intent appInfoIntent = new Intent(mContext, SettingListActivity.class);
                    appInfoIntent.putExtra("type", "应用信息");
                    startActivity(appInfoIntent);
                    break;

                case "消息推送" :
                    Intent pushIntent = new Intent(mContext, SettingListActivity.class);
                    pushIntent.putExtra("type", "消息推送");
                    startActivity(pushIntent);
                    break;

                case "更新日志" :
                    Intent thursdaySayIntent = new Intent(SettingActivity.this,ThursdaySayActivity.class);
                    thursdaySayIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(thursdaySayIntent);
                    break;

                case "选项配置" :
                    Intent settingPreferenceIntent = new Intent(mContext, SettingPreferenceActivity.class);
                    startActivity(settingPreferenceIntent);
                    break;
                case "开发者选项" :
                    Intent developerActivityIntent = new Intent(SettingActivity.this, DeveloperActivity.class);
                    startActivity(developerActivityIntent);
                    break;
            }
        }
    };

    /*
     * 退出登录
     */
    public void loginOut(View v) {
        // 判断有无网络
        if (!isNetworkConnected(this)){
            toast("无网络，不退出");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String postUrl = String.format(K.kDeleteDeviceIdAPIPath, K.kBaseUrl, user.getString("user_device_id"));
                    final Map<String,String> response = HttpUtil.httpPost(postUrl, new HashMap());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.get("code").equals("200")){
                                modifiedUserConfig(false);
                                Intent intent = new Intent();
                                intent.setClass(SettingActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }else {
                                toast(response.get("body"));
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
