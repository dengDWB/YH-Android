package com.intfocus.yonghuitest.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.intfocus.yonghuitest.R;
import com.yonghui.homemetrics.data.response.HomeMetrics;
import com.yonghui.homemetrics.data.response.Item;
import com.yonghui.homemetrics.data.response.Product;

import java.util.List;

/**
 * Created by CANC on 2017/4/6.
 */

public class MetricsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private MetricsListener listener;
    private Context context;
    private HomeMetrics homeMetrics;
    private final LayoutInflater inflater;
    private static long DOUBLE_CLICK_TIME = 200;
    private static long mLastTime;
    private static long mCurTime;
    private int dataSize = 1;

    public MetricsAdapter(Context context, HomeMetrics homeMetrics, MetricsListener listener) {
        this.context = context;
        this.homeMetrics = homeMetrics;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void setDatas(HomeMetrics homeMetrics) {
        this.homeMetrics = homeMetrics;
        this.dataSize = homeMetrics.products.size();
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View contentView = inflater.inflate(R.layout.item_metrics, parent, false);
        return new ProductListHolder(contentView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        ProductListHolder viewHolder = (ProductListHolder) holder;
        final List<Product> datas = homeMetrics.products;
        final Product product = datas.get(position);
        final List<Item> items = product.items;
        if (items.size() == 6) {
            viewHolder.ll6.setVisibility(View.VISIBLE);
            Item item = items.get(5);
            viewHolder.ll6.setBackgroundResource(item.isSelected ?
                    R.drawable.background_square_green_boder_white :
                    R.drawable.background_square_black_boder_white);
            viewHolder.ll6.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mLastTime = mCurTime;
                    mCurTime = System.currentTimeMillis();
                    if (mCurTime - mLastTime < DOUBLE_CLICK_TIME) {
                        listener.itemSelected(position, 5, true);
                    } else {
                        listener.itemSelected(position, 5, false);
                    }
                }
            });
            viewHolder.tvHead6.setText(item.getName());
            viewHolder.ivArrow6.setImageResource(getArrowImg(item));
            viewHolder.tvMainData6.setText(item.main_data.getData() + "");
            viewHolder.tvSubData6.setText(item.sub_data.getData() + "");
        } else {
            viewHolder.ll6.setVisibility(View.GONE);
        }
        if (items.size() >= 5) {
            viewHolder.ll5.setVisibility(View.VISIBLE);
            Item item = items.get(4);
            viewHolder.ll5.setBackgroundResource(item.isSelected ?
                    R.drawable.background_square_green_boder_white :
                    R.drawable.background_square_black_boder_white);
            viewHolder.ll5.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mLastTime = mCurTime;
                    mCurTime = System.currentTimeMillis();
                    if (mCurTime - mLastTime < DOUBLE_CLICK_TIME) {
                        listener.itemSelected(position, 4, true);
                    } else {
                        listener.itemSelected(position, 4, false);
                    }
                }
            });
            viewHolder.tvHead5.setText(item.getName());
            viewHolder.ivArrow5.setImageResource(getArrowImg(item));
            viewHolder.tvMainData5.setText(item.main_data.getData() + "");
            viewHolder.tvSubData5.setText(item.sub_data.getData() + "");
        } else {
            if (dataSize > 1) {
                viewHolder.ll6.setVisibility(View.GONE);
                viewHolder.ll5.setVisibility(View.GONE);
            } else {
                viewHolder.ll6.setVisibility(View.GONE);
                viewHolder.ll5.setVisibility(View.GONE);
            }
        }
        if (items.size() >= 4) {
            viewHolder.ll4.setVisibility(View.VISIBLE);
            Item item = items.get(3);
            viewHolder.ll4.setBackgroundResource(item.isSelected ?
                    R.drawable.background_square_green_boder_white :
                    R.drawable.background_square_black_boder_white);
            viewHolder.ll4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mLastTime = mCurTime;
                    mCurTime = System.currentTimeMillis();
                    if (mCurTime - mLastTime < DOUBLE_CLICK_TIME) {
                        listener.itemSelected(position, 3, true);
                    } else {
                        listener.itemSelected(position, 3, false);
                    }
                }
            });
            viewHolder.tvHead4.setText(item.getName());
            viewHolder.ivArrow4.setImageResource(getArrowImg(item));
            viewHolder.tvMainData4.setText(item.main_data.getData() + "");
            viewHolder.tvSubData4.setText(item.sub_data.getData() + "");
        } else {
            if (dataSize > 1) {
                viewHolder.ll6.setVisibility(View.INVISIBLE);
                viewHolder.ll5.setVisibility(View.INVISIBLE);
            } else {
                viewHolder.ll6.setVisibility(View.GONE);
                viewHolder.ll5.setVisibility(View.GONE);
            }
            viewHolder.ll4.setVisibility(View.INVISIBLE);
        }
        if (items.size() >= 3) {
            viewHolder.ll3.setVisibility(View.VISIBLE);
            Item item = items.get(2);
            viewHolder.ll3.setBackgroundResource(item.isSelected ?
                    R.drawable.background_square_green_boder_white :
                    R.drawable.background_square_black_boder_white);
            viewHolder.ll3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mLastTime = mCurTime;
                    mCurTime = System.currentTimeMillis();
                    if (mCurTime - mLastTime < DOUBLE_CLICK_TIME) {
                        listener.itemSelected(position, 2, true);
                    } else {
                        listener.itemSelected(position, 2, false);
                    }
                }
            });
            viewHolder.tvHead3.setText(item.getName());
            viewHolder.ivArrow3.setImageResource(getArrowImg(item));
            viewHolder.tvMainData3.setText(item.main_data.getData() + "");
            viewHolder.tvSubData3.setText(item.sub_data.getData() + "");
        } else {
            if (dataSize > 1) {
                viewHolder.ll6.setVisibility(View.INVISIBLE);
                viewHolder.ll5.setVisibility(View.INVISIBLE);
                viewHolder.ll4.setVisibility(View.INVISIBLE);
                viewHolder.ll3.setVisibility(View.INVISIBLE);
            } else {
                viewHolder.ll6.setVisibility(View.GONE);
                viewHolder.ll5.setVisibility(View.GONE);
                viewHolder.ll4.setVisibility(View.GONE);
                viewHolder.ll3.setVisibility(View.GONE);
            }
        }
        if (items.size() >= 2) {
            viewHolder.ll2.setVisibility(View.VISIBLE);
            Item item = items.get(1);
            viewHolder.ll2.setBackgroundResource(item.isSelected ?
                    R.drawable.background_square_green_boder_white :
                    R.drawable.background_square_black_boder_white);
            viewHolder.ll2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mLastTime = mCurTime;
                    mCurTime = System.currentTimeMillis();
                    if (mCurTime - mLastTime < DOUBLE_CLICK_TIME) {
                        listener.itemSelected(position, 1, true);
                    } else {
                        listener.itemSelected(position, 1, false);
                    }
                }
            });
            viewHolder.tvHead2.setText(item.getName());
            viewHolder.ivArrow1.setImageResource(getArrowImg(item));
            viewHolder.tvMainData2.setText(item.main_data.getData() + "");
            viewHolder.tvSubData2.setText(item.sub_data.getData() + "");
        } else {
            if (dataSize > 1) {
                viewHolder.ll6.setVisibility(View.INVISIBLE);
                viewHolder.ll5.setVisibility(View.INVISIBLE);
                viewHolder.ll4.setVisibility(View.INVISIBLE);
                viewHolder.ll3.setVisibility(View.INVISIBLE);
            } else {
                viewHolder.ll6.setVisibility(View.GONE);
                viewHolder.ll5.setVisibility(View.GONE);
                viewHolder.ll4.setVisibility(View.GONE);
                viewHolder.ll3.setVisibility(View.GONE);
            }
            viewHolder.ll2.setVisibility(View.INVISIBLE);
        }
        Item item = items.get(0);
        viewHolder.ll1.setBackgroundResource(item.isSelected ?
                R.drawable.background_square_green_boder_white :
                R.drawable.background_square_black_boder_white);
        viewHolder.ll1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLastTime = mCurTime;
                mCurTime = System.currentTimeMillis();
                if (mCurTime - mLastTime < DOUBLE_CLICK_TIME) {
                    listener.itemSelected(position, 0, true);
                } else {
                    listener.itemSelected(position, 0, false);
                }
            }
        });
        viewHolder.tvHead1.setText(item.getName());
        viewHolder.ivArrow1.setImageResource(getArrowImg(item));
        viewHolder.tvMainData1.setText(item.main_data.getData() + "");
        viewHolder.tvSubData1.setText(item.sub_data.getData() + "");
    }

    public int getArrowImg(Item item) {
        if ("up".equalsIgnoreCase(item.state.getArrow())) {
            Log.i("arrow", item.state.getArrow() + "  " + item.state.getColor());
            switch (item.state.getColor()) {
                case "#F2E1AC" :
                    return R.drawable.up_yellowarrow;

                case "#F2836B" :
                    return R.drawable.up_redarrow;

                case "#63A69F" :
                    return R.drawable.up_greenarrow;

                default:
                    return R.drawable.up_redarrow;
            }
        } else {
            Log.i("arrow", item.state.getArrow() + "  " + item.state.getColor());
            switch (item.state.getColor()) {
                case "#F2E1AC" :
                    return R.drawable.down_yellowarrow;

                case "#F2836B" :
                    return R.drawable.down_redarrow;

                case "#63A69F" :
                    return R.drawable.down_greenarrow;

                default:
                    return R.drawable.up_redarrow;
            }
        }
    }

    @Override
    public int getItemCount() {
        return (homeMetrics == null || homeMetrics.products == null) ? 0 : homeMetrics.products.size();
    }

    public class ProductListHolder extends RecyclerView.ViewHolder {
        LinearLayout ll1;
        TextView tvHead1;
        TextView tvMainData1;
        TextView tvSubData1;
        ImageView ivArrow1;

        LinearLayout ll2;
        TextView tvHead2;
        TextView tvMainData2;
        TextView tvSubData2;
        ImageView ivArrow2;

        LinearLayout ll3;
        TextView tvHead3;
        TextView tvMainData3;
        TextView tvSubData3;
        ImageView ivArrow3;

        LinearLayout ll4;
        TextView tvHead4;
        TextView tvMainData4;
        TextView tvSubData4;
        ImageView ivArrow4;

        LinearLayout ll5;
        TextView tvHead5;
        TextView tvMainData5;
        TextView tvSubData5;
        ImageView ivArrow5;

        LinearLayout ll6;
        TextView tvHead6;
        TextView tvMainData6;
        TextView tvSubData6;
        ImageView ivArrow6;

        public ProductListHolder(View itemView) {
            super(itemView);
            ll1 = (LinearLayout) itemView.findViewById(R.id.ll_1);
            tvHead1 = (TextView) itemView.findViewById(R.id.tv_head_1);
            tvMainData1 = (TextView) itemView.findViewById(R.id.tv_main_data_1);
            tvSubData1 = (TextView) itemView.findViewById(R.id.tv_sub_data_1);
            ivArrow1 = (ImageView) itemView.findViewById(R.id.iv_arrow1);

            ll2 = (LinearLayout) itemView.findViewById(R.id.ll_2);
            tvHead2 = (TextView) itemView.findViewById(R.id.tv_head_2);
            tvMainData2 = (TextView) itemView.findViewById(R.id.tv_main_data_2);
            tvSubData2 = (TextView) itemView.findViewById(R.id.tv_sub_data_2);
            ivArrow2 = (ImageView) itemView.findViewById(R.id.iv_arrow2);

            ll3 = (LinearLayout) itemView.findViewById(R.id.ll_3);
            tvHead3 = (TextView) itemView.findViewById(R.id.tv_head_3);
            tvMainData3 = (TextView) itemView.findViewById(R.id.tv_main_data_3);
            tvSubData3 = (TextView) itemView.findViewById(R.id.tv_sub_data_3);
            ivArrow3 = (ImageView) itemView.findViewById(R.id.iv_arrow3);

            ll4 = (LinearLayout) itemView.findViewById(R.id.ll_4);
            tvHead4 = (TextView) itemView.findViewById(R.id.tv_head_4);
            tvMainData4 = (TextView) itemView.findViewById(R.id.tv_main_data_4);
            tvSubData4 = (TextView) itemView.findViewById(R.id.tv_sub_data_4);
            ivArrow4 = (ImageView) itemView.findViewById(R.id.iv_arrow4);

            ll5 = (LinearLayout) itemView.findViewById(R.id.ll_5);
            tvHead5 = (TextView) itemView.findViewById(R.id.tv_head_5);
            tvMainData5 = (TextView) itemView.findViewById(R.id.tv_main_data_5);
            tvSubData5 = (TextView) itemView.findViewById(R.id.tv_sub_data_5);
            ivArrow5 = (ImageView) itemView.findViewById(R.id.iv_arrow5);

            ll6 = (LinearLayout) itemView.findViewById(R.id.ll_6);
            tvHead6 = (TextView) itemView.findViewById(R.id.tv_head_6);
            tvMainData6 = (TextView) itemView.findViewById(R.id.tv_main_data_6);
            tvSubData6 = (TextView) itemView.findViewById(R.id.tv_sub_data_6);
            ivArrow6 = (ImageView) itemView.findViewById(R.id.iv_arrow6);
        }
    }

    public interface MetricsListener {
        void itemSelected(int page, int position, boolean isDoubleClick);
    }


}
