package com.intfocus.yonghuitest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.intfocus.yonghuitest.util.HttpUtil;
import com.intfocus.yonghuitest.util.K;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PushListActivity extends BaseActivity {

    private ListView pushListView;
    private ArrayList<HashMap<String, Object>> listItem;
    private PushListAdapter mSimpleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_list);

        pushListView = (ListView) findViewById(R.id.pushListView);
        listItem = new ArrayList<>();
        String[] itemName = {"消息推送", "关联的设备列表", "推送的消息列表"};
        for (int i = 0; i < itemName.length; i++) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("ItemTile", itemName[i]);
            map.put("ItemState", itemName[i]);
            listItem.add(map);
        }

        mSimpleAdapter = new PushListAdapter(this, listItem, R.layout.layout_push_list, new String[]{"ItemTile", "ItemState"}, new int[]{R.id.titleItem, R.id.stateItem}, true);
        pushListView.setAdapter(mSimpleAdapter);
        pushListView.setOnItemClickListener(itemClickListener);

    }

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d("device_token", listItem.get(position).get("ItemTile").toString());
            switch (listItem.get(position).get("ItemTile").toString()) {
                case "关联的设备列表":
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String deviceTokenUrl = String.format(K.kDeviceTokenAPIPath, K.kBaseUrl, user.getString("user_num"));
                                final Map<String, String> response = HttpUtil.httpGet(deviceTokenUrl, new HashMap<String, String>());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (response.containsKey("code") && response.get("code").equals("200")){
                                            Intent intent = new Intent(PushListActivity.this, ShowListMsgActivity.class);
                                            try {
                                                JSONObject jsonObject = new JSONObject(response.get("body"));
                                                intent.putExtra("response", jsonObject.getString("devices"));
                                                intent.putExtra("title", "关联的设备列表");
                                                startActivity(intent);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }else {
                                            toast("获取关联的设备列表失败");
                                        }
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    break;

                case "推送的消息列表":
                    try {
                        SharedPreferences sp = getSharedPreferences("allPushMessage", MODE_PRIVATE);
                        String allMesaage = sp.getString("message","false");
                        if (allMesaage.equals("false")){
                            toast("从未接收到推送消息");
                        }else {
                            Intent intent = new Intent(PushListActivity.this, ShowListMsgActivity.class);
                            intent.putExtra("pushMessage", true);
                            intent.putExtra("title", "推送的消息列表");
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    break;
            }
        }
    };

    public void dismissActivity(View v) {
        PushListActivity.this.onBackPressed();
    }
}
