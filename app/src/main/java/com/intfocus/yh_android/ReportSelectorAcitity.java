package com.intfocus.yh_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
public class ReportSelectorAcitity extends BaseActivity {
	private ListView mListView;
	private String selectedItem;
	private ArrayList<String> searchItems = new ArrayList<String>();
	private String templateID, reportID, bannerName;
	private int groupID;

	@Override
	@SuppressLint("SetJavaScriptEnabled")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_report_selector);

		Intent intent = getIntent();
		groupID = intent.getIntExtra("groupID", 0);
		bannerName = intent.getStringExtra("bannerName");
		reportID = intent.getStringExtra("reportID");
		templateID = intent.getStringExtra("templateID");

		TextView mTitle = (TextView) findViewById(R.id.bannerTitle);
		mTitle.setText(bannerName);

		/**
		 *  - 如果用户已设置筛选项，则 banner 显示该信息
		 *  - 未设置时，默认显示第一个
		 */
		searchItems = FileUtil.reportSearchItems(mContext, String.format("%d", groupID), templateID, reportID);
		selectedItem = FileUtil.reportSelectedItem(mContext, String.format("%d", groupID), templateID, reportID);
		if ((selectedItem == null || selectedItem.length() == 0) && searchItems.size() > 0) {
			selectedItem = searchItems.get(0).toString().trim();
		}

		/**
		 *  筛选项列表按字母排序，以便于用户查找
		 */
		Collections.sort(searchItems, new Comparator<String>() {
			@Override
			public int compare(String one, String two) {
				return Collator.getInstance(Locale.CHINESE).compare(one, two);
			}
		});

		mListView = (ListView) findViewById(R.id.listSearchItems);
		ListArrayAdapter mArrayAdapter = new ListArrayAdapter(this, R.layout.list_item_report_selector, searchItems);
		mListView.setAdapter(mArrayAdapter);

		/**
		 *  用户点击项写入本地缓存文件
		 */
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				try {
					String selectedItemPath = String.format("%s.selected_item", FileUtil.reportJavaScriptDataPath(mContext, String.format("%d", groupID), templateID, reportID));
					FileUtil.writeFile(selectedItemPath, searchItems.get(arg2));

					dismissActivity(null);
				} catch (IOException e) {
					e.printStackTrace();
				}
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
			boolean isSelected = (item != null && selectedItem != null && item.compareTo(selectedItem) == 0);
			LogUtil.d("getView", String.format("%s %s %s", item, selectedItem, isSelected ? "==" : "!="));
			viewItem.setBackgroundColor(isSelected ? Color.GREEN : Color.WHITE);

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
