package com.intfocus.yh_android;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.URLs;
import com.readystatesoftware.viewbadger.BadgeView;

import org.json.JSONException;
import org.json.JSONObject;

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
	private String noticePath;
	private JSONObject notificationJSON;
	private BadgeView bvUser;


	public MenuAdapter(Context context, List<? extends Map<String, ?>> data,
					   int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
		this.listItem = (ArrayList<HashMap<String, Object>>) data;
		this.mContext = context;
		noticePath = FileUtil.dirPath(mContext, URLs.CACHED_DIRNAME, URLs.LOCAL_NOTIFICATION_FILENAME);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);

		if (convertView != null) {
			return v;
		}

		itemName = (TextView) v.findViewById(R.id.text_menu_item);

		if (itemName.getText().equals("个人信息")) {
			notificationJSON = FileUtil.readConfigFile(noticePath);
			try {
				bvUser = new BadgeView(mContext, itemName);
				bvUser.setVisibility(View.GONE);

				if (notificationJSON.getInt(URLs.kSetting) > 0) {
					RedPointView.showRedPoint(mContext, "user", bvUser);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return v;
	}
}
