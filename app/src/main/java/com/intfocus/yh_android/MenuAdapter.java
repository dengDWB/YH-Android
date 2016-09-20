package com.intfocus.yh_android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * Created by Liurl on 2016/9/20.
 */
public class MenuAdapter extends SimpleAdapter {
	private LayoutInflater listContainer;

	public MenuAdapter(Context context, List<? extends Map<String, ?>> data,
						   int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		final int mPosition = position;
		ListItemView listItemView = null;
		if (convertView == null) {
			convertView = listContainer.inflate(R.layout.menu_list_items, null);//加载布局
			listItemView = new ListItemView();
			/*初始化控件容器集合*/
			listItemView.txtName=(TextView) convertView
					.findViewById(R.id.name);
			// 设置控件集到convertView
			convertView.setTag(listItemView);

		}else{
			listItemView=(ListItemView)convertView.getTag();//利用缓存的View
		}
		if ( )
		return v;
	}
}
