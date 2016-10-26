package com.intfocus.yonghuitest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.URLs;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Created by lijunjie on 16/7/20.
 */
public class ReportSelectorAcitity extends BaseActivity  {
  private ListView mListView;
  private String templateID;
  private String reportID;
  private int groupID;

  @Override
  @SuppressLint("SetJavaScriptEnabled")
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_report_selector);

    Intent intent = getIntent();
    groupID = intent.getIntExtra(URLs.kGroupId, 0);
    String bannerName = intent.getStringExtra(URLs.kBannerName);
    reportID = intent.getStringExtra("reportID");
    templateID = intent.getStringExtra("templateID");

    TextView mTitle = (TextView) findViewById(R.id.bannerTitle);
    TextView mSelectedItem = (TextView) findViewById(R.id.report_item_select);
    final LinearLayout mListHead = (LinearLayout)findViewById(R.id.report_list_head);
    mTitle.setText(bannerName);

    /**
     *  - 如果用户已设置筛选项，则 banner 显示该信息
     *  - 未设置时，默认显示第一个
     */
    ArrayList<String> searchItems = FileUtil.reportSearchItems(mAppContext, String.format("%d", groupID), templateID, reportID);
    String selectedItem = FileUtil.reportSelectedItem(mAppContext, String.format("%d", groupID), templateID, reportID);
    if((selectedItem == null || selectedItem.length() == 0 ) && searchItems.size() > 0) {
      selectedItem = searchItems.get(0).toString().trim();
    }
    mSelectedItem.setText(selectedItem);

    /**
     *  筛选项列表按字母排序，以便于用户查找
     */
    Collections.sort(searchItems, new Comparator<String>() {
      @Override public int compare(String one, String two) {
        return Collator.getInstance(Locale.CHINESE).compare(one, two);
      }
    });

    /*
     * 搜索框初始化
     */
    SearchView mSearchView = (SearchView) findViewById(R.id.reportSearchView);
    int searchEditId = mSearchView.getContext().getResources().getIdentifier("android:id/search_src_text",null,null);
    TextView mSearchEdit = (TextView)findViewById(searchEditId);
    mSearchEdit.setTextSize(14);
    mSearchEdit.setPadding(0,30,0,0);

    /*
     * ListView列表初始化
     */
    mListView = (ListView)findViewById(R.id.listSearchItems);
    ListArrayAdapter mArrayAdapter = new ListArrayAdapter(this, R.layout.list_item_report_selector, searchItems);
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
    mListView.setOnItemClickListener(mListItemListener);

    List<ImageView> colorViews = new ArrayList<>();
    colorViews.add((ImageView) findViewById(R.id.colorView0));
    colorViews.add((ImageView) findViewById(R.id.colorView1));
    colorViews.add((ImageView) findViewById(R.id.colorView2));
    colorViews.add((ImageView) findViewById(R.id.colorView3));
    colorViews.add((ImageView) findViewById(R.id.colorView4));
    initColorView(colorViews);
  }

  @Override
  protected void onResume() {
    mMyApp.setCurrentActivity(this);
    super.onResume();
  }

  protected void onDestroy() {
    mWebView = null;
    user = null;
    super.onDestroy();
  }

  private ListView.OnItemClickListener mListItemListener = new ListView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
      try {
        TextView mSelector = (TextView) arg1.findViewById(R.id.reportSelectorItem);
        String selectedItemPath = String.format("%s.selected_item", FileUtil.reportJavaScriptDataPath(mAppContext, String.format("%d", groupID), templateID, reportID));
        FileUtil.writeFile(selectedItemPath, mSelector.getText().toString());

        dismissActivity(null);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  };

  public class ListArrayAdapter extends ArrayAdapter<String> {
    private int resourceId;
    public ListArrayAdapter(Context context, int textViewResourceId, List<String> items) {
      super(context, textViewResourceId, items);
      this.resourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
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
      ReportSelectorAcitity.this.onBackPressed();
      finish();
  }
}
