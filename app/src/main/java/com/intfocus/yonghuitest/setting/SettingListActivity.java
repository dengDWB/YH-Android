package com.intfocus.yonghuitest.setting;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.intfocus.yonghuitest.BaseActivity;
import com.intfocus.yonghuitest.R;
import com.intfocus.yonghuitest.util.K;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by liuruilin on 2017/3/29.
 */
public class SettingListActivity extends BaseActivity {
    private ArrayList<HashMap<String, Object>> listItem;
    private SimpleAdapter mSimpleAdapter;
    private TextView mBannerTitle;
    private String mListType;
    private long mLastExitTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_list);
        mBannerTitle = (TextView)findViewById(R.id.bannerTitle);
        mListType = getIntent().getStringExtra("type");
        mBannerTitle.setText(mListType);
        initListInfo(mListType);
    }

    private void initListInfo(String type) {
        if (type.equals("个人资料")) {
            String[] itemName = {};
        }
        else if (type.equals("应用信息")) {
            PackageInfo packageInfo = null;
            try {
                packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String appName = getString(getApplicationInfo().labelRes);
            String deviceInfo = String.format("%s(Android %s)", TextUtils.split(android.os.Build.MODEL, " - ")[0], Build.VERSION.RELEASE);
            String apiDomain = K.kBaseUrl.replace("http://", "").replace("https://", "");
            String versionInfo = String.format("%s(%d)", packageInfo.versionName, packageInfo.versionCode);
            String appPackageInfo = packageInfo.packageName;
            String[] itemName = {"应用名称", "版本号", "设备型号", "数据接口", "应用标识"};
            String[] itemContent = {appName, versionInfo, deviceInfo, apiDomain, appPackageInfo};
            initListView(itemName, itemContent);
        }
    }

    private void initListView(String[] itemName, String[] itemContent) {
        listItem = new ArrayList<>();
        for (int i = 0; i < itemName.length; i++) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("ItemName", itemName[i]);
            map.put("ItemContent", itemContent[i]);
            listItem.add(map);
        }

        mSimpleAdapter = new SimpleAdapter(this, listItem, R.layout.list_info_setting, new String[]{"ItemName", "ItemContent"}, new int[]{R.id.item_setting_key, R.id.item_setting_info});

        ListView listView = (ListView) findViewById(R.id.list_listSetting);
        listView.setAdapter(mSimpleAdapter);
        listView.setOnItemClickListener(mListItemListener);
    }

    /*
     * 个人信息菜单项点击事件
     */
    private ListView.OnItemClickListener mListItemListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            TextView mItemText = (TextView) arg1.findViewById(R.id.item_setting_key);
            switch (mItemText.getText().toString()) {
                case "应用标识" :
                    if (System.currentTimeMillis() - mLastExitTime < 2000) {
                        toast("打开 开发者选项");
                    } else {
                        mLastExitTime = System.currentTimeMillis();
                        toast("再点击一下将打开 开发者选项");
                    }
                    break;

                case "版本号" :
                    checkPgyerVersionUpgrade(SettingListActivity.this, true);
                    break;


            }
        }
    };
}
