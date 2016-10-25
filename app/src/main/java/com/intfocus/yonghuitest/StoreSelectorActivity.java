package com.intfocus.yonghuitest;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.URLs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Created by lijunjie on 16/8/15.
 */
public class StoreSelectorActivity extends BaseActivity {
  private ListView mListView;
  private String cachedPath;
  private ArrayList<JSONObject> dataList = new ArrayList<>();
  private ArrayList<String> storeNameList = new ArrayList<>();
  private JSONObject cachedJSON;

  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Override
  @SuppressLint("SetJavaScriptEnabled")
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_store_selector);

    final LinearLayout mListHead = (LinearLayout)findViewById(R.id.store_list_head);
    TextView mSelectedItem = (TextView) findViewById(R.id.store_item_select);

    try {
      cachedPath = FileUtil.dirPath(mAppContext, K.kCachedDirName, K.kBarCodeResultFileName);
      cachedJSON = FileUtil.readConfigFile(cachedPath);
      JSONObject currentStore = cachedJSON.getJSONObject(URLs.kStore);

      mSelectedItem.setText(currentStore.getString("name")); // 已选项显示当前门店

      if (user.has(URLs.kStoreIds) && user.getJSONArray(URLs.kStoreIds).length() > 0) {
        JSONArray stores = user.getJSONArray(URLs.kStoreIds);
        for(int i = 0, len = stores.length(); i < len; i ++) {
          dataList.add(stores.getJSONObject(i));
          storeNameList.add(stores.getJSONObject(i).getString(URLs.kName));
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

    Collections.sort(storeNameList,Collator.getInstance(Locale.CHINESE));

    /*
     * 搜索框初始化
     */
    SearchView mSearchView = (SearchView) findViewById(R.id.storeSearchView);
    int searchEditId = mSearchView.getContext().getResources().getIdentifier("android:id/search_src_text",null,null);
    TextView mSearchEdit = (TextView)findViewById(searchEditId);
    mSearchEdit.setTextSize(14);
    mSearchEdit.setPadding(0,30,0,0);

    /*
     * ListView 初始化
     */
    mListView = (ListView) findViewById(R.id.listStores);
    ListArrayAdapter mArrayAdapter = new ListArrayAdapter(this, R.layout.list_item_report_selector, storeNameList);
    mListView.setAdapter(mArrayAdapter);
    mListView.setTextFilterEnabled(true);


        /*
     * 搜索框事件监听
     */
    mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      // 当点击搜索按钮时触发该方法
      @Override
      public boolean onQueryTextSubmit(String query) {
        return false;
      }

      // 当搜索内容改变时触发该方法
      @Override
      public boolean onQueryTextChange(String newText) {
        if (!TextUtils.isEmpty(newText)){
          mListHead.setVisibility(View.GONE);
          mListView.setFilterText(newText);
        }else{
          mListHead.setVisibility(View.VISIBLE);
          mListView.clearTextFilter();
        }
        return true;
      }
    });

    /**
     *  用户点击项写入本地缓存文件
     */
    mListView.setOnItemClickListener(mItemClickListener);
  }

  /*
   * listview 点击事件
   */
  private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      try {
        TextView mSelector = (TextView) view.findViewById(R.id.reportSelectorItem);
        String selectedItem = mSelector.getText().toString();
        for (int i = 0;i < dataList.size();i++) {
          if (dataList.get(i).getString("name").equals(selectedItem)) {
            cachedJSON.put(URLs.kStore, dataList.get(i));
            FileUtil.writeFile(cachedPath, cachedJSON.toString());
          }
        }

        dismissActivity(null);
      } catch (Exception e) {
        e.printStackTrace();
      }
      dismissActivity(null);
    }
  };

    protected void onResume() {
    mMyApp.setCurrentActivity(this);
    super.onResume();
  }

  protected void onDestroy() {
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
      viewItem.setBackgroundColor(Color.WHITE);

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
