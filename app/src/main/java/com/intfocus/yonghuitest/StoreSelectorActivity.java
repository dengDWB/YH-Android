package com.intfocus.yonghuitest;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.LogUtil;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lijunjie on 16/8/15.
 */
public class StoreSelectorActivity extends BaseActivity {

  private ListView mListView;
  private String cachedPath;
  private ArrayList<JSONObject> dataList = new ArrayList<>();
  private ArrayList<String> storeNameList = new ArrayList<>();
  private JSONObject cachedJSON, currentStore;

  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Override
  @SuppressLint("SetJavaScriptEnabled")
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_store_selector);

    try {
      cachedPath = FileUtil.dirPath(mContext, "Cached", "barcode.json");
      cachedJSON = FileUtil.readConfigFile(cachedPath);
      currentStore = cachedJSON.getJSONObject("store");

      if (user.has("store_ids") && user.getJSONArray("store_ids").length() > 0) {
        JSONArray stores = user.getJSONArray("store_ids");
        for(int i = 0, len = stores.length(); i < len; i ++) {
          dataList.add(stores.getJSONObject(i));
          storeNameList.add(stores.getJSONObject(i).getString("name"));
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    /**
     *  筛选项列表按字母排序，以便于用户查找
     */
    Collections.sort(dataList, new Comparator<JSONObject>() {
      @Override public int compare(JSONObject one, JSONObject two) {
        String one_name = "", two_name = "";
        try {
          one_name = one.getString("name");
          two_name = two.getString("name");
        } catch (JSONException e) {
          e.printStackTrace();
        }
        return Collator.getInstance(Locale.CHINESE).compare(one_name, two_name);
      }
    });

    mListView = (ListView) findViewById(R.id.listStores);
    ListArrayAdapter mArrayAdapter = new ListArrayAdapter(this, R.layout.list_item_report_selector, storeNameList);
    mListView.setAdapter(mArrayAdapter);

    /**
     *  用户点击项写入本地缓存文件
     */
    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        try {
          cachedJSON.put("store", dataList.get(arg2));
          FileUtil.writeFile(cachedPath, cachedJSON.toString());

          dismissActivity(null);
        }catch (Exception e){
          e.printStackTrace();
        }
        dismissActivity(null);
      }
    });
  }

  protected void onResume() {
    mMyApp.setCurrentActivity(this);
    super.onResume();
  }

  protected void onDestroy() {
    mContext = null;
    mWebView = null;
    user = null;
    super.onDestroy();
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
      TextView viewItem = (TextView) listItem.findViewById(R.id.reportSelectorItem);
      viewItem.setText(item);

      /**
       *  上次选中项显示选中状态
       */
      String currentStoreName = "";
      try {
        currentStoreName = currentStore.getString("name");
      } catch (JSONException e) {
        e.printStackTrace();
      }

      boolean isSelected = (item != null && item.compareTo(currentStoreName) == 0);
      LogUtil.d("getView", String.format("%s %s %s", item, currentStoreName, isSelected ? "==" : "!="));
      viewItem.setBackgroundColor(isSelected ? Color.GREEN : Color.WHITE);

      return listItem;
    }
  }

  /*
   * 返回
   */
  public void dismissActivity(View v) {
    StoreSelectorActivity.this.onBackPressed();
    finish();
  }
}
