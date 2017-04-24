package com.intfocus.yonghuitest.setting;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.intfocus.yonghuitest.BaseActivity;
import com.intfocus.yonghuitest.R;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.K;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ShowListMsgActivity extends BaseActivity {
    private ListView pushListView;
    private TextView bannerTitle;
    private ArrayList<HashMap<String, Object>> listItem;
    private SimpleAdapter mSimpleAdapter;
    private String response;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_list_msg);

        pushListView = (ListView) findViewById(R.id.pushListView);
        bannerTitle = (TextView) findViewById(R.id.bannerTitle);
        listItem = new ArrayList<>();
        Intent intent = getIntent();
        bannerTitle.setText(intent.getStringExtra("title"));
        if (intent.hasExtra("type")){
            type = intent.getStringExtra("type");
            initListInfo(type);
        }

        if (intent.hasExtra("response")){
            response = intent.getStringExtra("response");
            try {
                JSONArray array = new JSONArray(response);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject json = array.getJSONObject(i);
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("ItemTile", json.getString("name"));
                    if (json.getString("os").startsWith("iPhone")){
                        map.put("ItemState", "iPhone" + "(" + json.getString("os_version") + ")");
                    }else {
                        map.put("ItemState", "Android" + "(" + json.getString("os_version") + ")");
                    }
                    listItem.add(map);
                }
                mSimpleAdapter = new SimpleAdapter(this, listItem, R.layout.list_info_setting, new String[]{"ItemTile", "ItemState"}, new int[]{R.id.item_setting_key, R.id.item_setting_info});
                pushListView.setAdapter(mSimpleAdapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void initListInfo(String type) {
        switch (type){
            case "subjectCache":
                try {
                    SharedPreferences sp = getSharedPreferences("subjectCache", MODE_PRIVATE);
                    String cache = sp.getString("cache","");
                    JSONObject json = new JSONObject(cache);
                    Iterator<String> it = json.keys();
                    while (it.hasNext()){
                        HashMap<String, Object> map = new HashMap<>();
                        String key = it.next();
                        map.put("ItemTile", json.getString(key));
                        map.put("ItemState", "");
                        listItem.add(map);
                    }
                    mSimpleAdapter = new SimpleAdapter(this, listItem, R.layout.list_info_setting, new String[]{"ItemTile", "ItemState"}, new int[]{R.id.item_setting_key, R.id.item_setting_info});
                    pushListView.setAdapter(mSimpleAdapter);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case "cacheHeaders":
                try {
                    String cacheHeaderPath = FileUtil.dirPath(mAppContext, "HTML", K.kCachedHeaderConfigFileName);
                    if (new File(cacheHeaderPath).exists()){
                        JSONObject json = FileUtil.readConfigFile(cacheHeaderPath);
                        Iterator<String> it = json.keys();
                        while (it.hasNext()){
                            HashMap<String, Object> map = new HashMap<>();
                            String key = it.next();
                            map.put("ItemTile", key+" :");
                            map.put("ItemState", "");
                            listItem.add(map);
                            map = new HashMap<>();
                            map.put("ItemTile", "");
                            map.put("ItemState", ((JSONObject)json.get(key)).toString());
                            listItem.add(map);
                        }
                        mSimpleAdapter = new SimpleAdapter(this, listItem, R.layout.list_info_setting, new String[]{"ItemTile", "ItemState"}, new int[]{R.id.item_setting_key, R.id.item_setting_info});
                        pushListView.setAdapter(mSimpleAdapter);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case "config":
                String userConfigPath = String.format("%s/%s", FileUtil.basePath(mAppContext), K.kUserConfigFileName);
                JSONObject user = FileUtil.readConfigFile(userConfigPath);
                Iterator<String> it = user.keys();
                while (it.hasNext()){
                    try {
                        HashMap<String, Object> map = new HashMap<>();
                        String key = it.next();
                        if (!key.equals("assets")){
                            map.put("ItemTile", key+" :");
                            map.put("ItemState", "");
                            listItem.add(map);
                            map = new HashMap<>();
                            map.put("ItemTile", "");
                            map.put("ItemState", user.getString(key));
                            listItem.add(map);
                        }
                        mSimpleAdapter = new SimpleAdapter(this, listItem, R.layout.list_info_setting, new String[]{"ItemTile", "ItemState"}, new int[]{R.id.item_setting_key, R.id.item_setting_info});
                        pushListView.setAdapter(mSimpleAdapter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case "pushMessage":
                try {
                    SharedPreferences sp = getSharedPreferences("allPushMessage", MODE_PRIVATE);
                    JSONObject json = new JSONObject(sp.getString("message", "false"));
                    for (int i = 0; i < json.length(); i++) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("ItemTile", json.getString("" + i));
                        map.put("ItemState", json.getString("" + i));
                        listItem.add(map);
                    }
                    mSimpleAdapter = new SimpleAdapter(this, listItem, R.layout.layout_push_list, new String[]{"ItemTile", "ItemState"}, new int[]{R.id.item_setting_key, R.id.item_setting_info});
                    pushListView.setAdapter(mSimpleAdapter);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public void dismissActivity(View v) {
        ShowListMsgActivity.this.onBackPressed();
    }
}
