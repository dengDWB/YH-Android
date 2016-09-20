package com.intfocus.yh_android;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.readystatesoftware.viewbadger.BadgeView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Liurl on 2016/9/20.
 */
public class MenuAdapter extends SimpleAdapter {
	private Context mContext;
	private TextView itemName;
	private ArrayList<HashMap<String, Object>> listItem;

	public MenuAdapter(Context context, List<? extends Map<String, ?>> data,
					   int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
		this.listItem =(ArrayList<HashMap<String,Object>>) data;
		this.mContext = context;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);

		itemName = (TextView) v.findViewById(R.id.text_menu_item);
		if (listItem.get(position).get("ItemText").toString().equals("个人信息")){
			Log.i("menudrop",itemName.getText().toString() + position +  "    添加小红点");
			BadgeView bvUser = new BadgeView(mContext,itemName);
			RedPointView.showRedPoint(mContext,"user",bvUser);
		}

		return v;
	}
}
