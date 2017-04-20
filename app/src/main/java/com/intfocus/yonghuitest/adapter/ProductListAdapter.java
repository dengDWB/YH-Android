package com.intfocus.yonghuitest.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.intfocus.yonghuitest.R;
import com.yonghui.homemetrics.data.response.Item;
import com.yonghui.homemetrics.data.response.Product;
import com.yonghui.homemetrics.utils.Utils;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by CANC on 2017/4/6.
 */

public class ProductListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ProductListListener listener;
    private Context context;
    private List<Product> datas;
    private final LayoutInflater inflater;
    private int screenWidth;
    private double maxValue;
    private int itemSelected;

    public ProductListAdapter(Context context, List<Product> datas, ProductListListener listener) {
        this.context = context;
        this.datas = datas;
        this.inflater = LayoutInflater.from(context);
        this.screenWidth = Utils.getScreenWidth(context);
        this.listener = listener;
    }

    public void setDatas(List<Product> datas, int itemSelected, double maxValue) {
        this.datas = datas;
        this.maxValue = maxValue;
        this.itemSelected = itemSelected;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View contentView = inflater.inflate(R.layout.item_product_list, parent, false);
        return new ProductListHolder(contentView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        ProductListHolder viewHolder = (ProductListHolder) holder;
        Product product = datas.get(position);
        if (product.items.size() > itemSelected) {
            Item item = product.items.get(itemSelected);
            if (product.isSelected) {
                viewHolder.ivArrow.setImageResource(R.drawable.icon_herearrow);
                viewHolder.tvProductName.setTextColor(ContextCompat.getColor(context, R.color.co15));
                viewHolder.tvProductNumber.setTextColor(ContextCompat.getColor(context, R.color.co15));
            } else {
                viewHolder.ivArrow.setImageResource(R.drawable.icon_nowt);
                viewHolder.tvProductName.setTextColor(ContextCompat.getColor(context, R.color.co3));
                viewHolder.tvProductNumber.setTextColor(ContextCompat.getColor(context, R.color.co3));
            }
            viewHolder.llLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.productSelected(position);
                }
            });

            viewHolder.viewBar.setVisibility(View.VISIBLE);
            viewHolder.tvProductName.setText(product.getName());

            BigDecimal bigDecimal = new BigDecimal(item.main_data.getData());
            double mainData = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            viewHolder.tvProductNumber.setText(mainData + "");
            String color = item.state.getColor();
            viewHolder.viewBar.setBackgroundColor(Color.parseColor(color));

            LayoutParams layoutParams = (LayoutParams) viewHolder.viewBar.getLayoutParams();
            double a = (item.main_data.getData() / maxValue);
            layoutParams.width = (int) (screenWidth * 3 / 8 * a);
            viewHolder.viewBar.setLayoutParams(layoutParams);
        } else {
            viewHolder.tvProductName.setText("数据缺失");
            viewHolder.tvProductNumber.setText("数据缺失");
            viewHolder.viewBar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return datas == null ? 0 : datas.size();
    }

    public class ProductListHolder extends RecyclerView.ViewHolder {
        LinearLayout llLayout;
        ImageView ivArrow;
        TextView tvProductName;
        TextView tvProductNumber;
        View viewBar;

        public ProductListHolder(View itemView) {
            super(itemView);
            llLayout = (LinearLayout) itemView.findViewById(R.id.ll_layout);
            ivArrow = (ImageView) itemView.findViewById(R.id.iv_arrow);
            tvProductName = (TextView) itemView.findViewById(R.id.tv_product_name);
            tvProductNumber = (TextView) itemView.findViewById(R.id.tv_product_number);
            viewBar = (View) itemView.findViewById(R.id.view_bar);
        }
    }

    public interface ProductListListener {
        void productSelected(int position);
    }
}
