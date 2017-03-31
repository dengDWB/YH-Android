package com.intfocus.yonghuitest;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.umeng.message.PushAgent;

import java.util.List;
import java.util.Map;

/**
 * Created by dengwenbin on 17/3/29.
 */

public class PushListAdapter extends SimpleAdapter {

    private PushAgent mPushAgent;
    private Context context;
    private boolean isVisible;

    public PushListAdapter(Context context, List<? extends Map<String, ?>> data,
                       int resource, String[] from, int[] to, boolean isVisible) {
        super(context, data, resource, from, to);
        this.context = context;
        this.isVisible = isVisible;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);

        TextView itemName = (TextView) v.findViewById(R.id.titleItem);
        TextView stateAdapter = (TextView) v.findViewById(R.id.stateItem);
        if (isVisible){
            if (itemName.getText().equals("消息推送")) {
                if (convertView != null) {
                    return v;
                }
                try {
                    mPushAgent = PushAgent.getInstance(context);
                    String deviceToken  = mPushAgent.getRegistrationId();
                    if (deviceToken.length() == 44) {
                        stateAdapter.setText("开启");
                    }
                }catch (NullPointerException e){
                    stateAdapter.setText("关闭");
                }
            }else {
                stateAdapter.setVisibility(View.GONE);
            }
        }

        return v;
    }

}
