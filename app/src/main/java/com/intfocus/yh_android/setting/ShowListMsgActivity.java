package com.intfocus.yh_android.setting;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.intfocus.yh_android.BaseActivity;
import com.intfocus.yh_android.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class ShowListMsgActivity extends BaseActivity {
    private ListView pushListView;
    private TextView bannerTitle;
    private ArrayList<HashMap<String, Object>> listItem;
    private SimpleAdapter mSimpleAdapter;
    private String response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_list_msg);

        pushListView = (ListView) findViewById(R.id.pushListView);
        bannerTitle = (TextView) findViewById(R.id.bannerTitle);
        listItem = new ArrayList<>();
        Intent intent = getIntent();
        bannerTitle.setText(intent.getStringExtra("title"));
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
                    mSimpleAdapter = new SimpleAdapter(this, listItem, R.layout.layout_push_list, new String[]{"ItemTile", "ItemState"}, new int[]{R.id.titleItem, R.id.stateItem});
                    pushListView.setAdapter(mSimpleAdapter);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else if (intent.hasExtra("pushMessage")){
            try {
                SharedPreferences sp = getSharedPreferences("allPushMessage", MODE_PRIVATE);
                JSONObject json = new JSONObject(sp.getString("message","false"));
                for (int i = 0; i < json.length(); i++){
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("ItemTile", json.getString(""+i));
                    map.put("ItemState", json.getString(""+i));
                    listItem.add(map);
                    mSimpleAdapter = new SimpleAdapter(this, listItem, R.layout.layout_push_list, new String[]{"ItemTile", "ItemState"}, new int[]{R.id.titleItem, R.id.stateItem});
                    pushListView.setAdapter(mSimpleAdapter);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void dismissActivity(View v) {
        ShowListMsgActivity.this.onBackPressed();
    }
}
