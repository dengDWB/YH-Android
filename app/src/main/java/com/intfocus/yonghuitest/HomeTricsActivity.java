package com.intfocus.yonghuitest;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intfocus.yonghuitest.adapter.MetricsAdapter;
import com.intfocus.yonghuitest.adapter.ProductListAdapter;
import com.intfocus.yonghuitest.util.HttpUtil;
import com.yonghui.homemetrics.data.response.HomeData;
import com.yonghui.homemetrics.data.response.HomeMetrics;
import com.yonghui.homemetrics.data.response.Item;
import com.yonghui.homemetrics.data.response.Product;
import com.yonghui.homemetrics.utils.ReorganizeTheDataUtils;
import com.yonghui.homemetrics.utils.Utils;

import org.json.JSONException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by CANC on 2017/4/6.
 */

public class HomeTricsActivity extends BaseActivity implements ProductListAdapter.ProductListListener
        , MetricsAdapter.MetricsListener, OnChartValueSelectedListener {
    @BindView(R.id.product_recycler_view)
    RecyclerView productRecyclerView;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.anim_loading)
    RelativeLayout mAnimLoading;
    @BindView(R.id.metrics_recycler_view)
    RecyclerView metricsRecyclerView;
    @BindView(R.id.iv_back)
    ImageView ivBack;
    @BindView(R.id.rl_title)
    RelativeLayout rlTitle;
    @BindView(R.id.tv_data_title)
    TextView tvDataTitle;
    @BindView(R.id.ll_bottom)
    LinearLayout llBottom;
    @BindView(R.id.tv_date_time)
    TextView tvDateTime;
    @BindView(R.id.iv_warning)
    ImageView ivWarning;
    @BindView(R.id.ll_data_title)
    RelativeLayout llDataTitle;
    @BindView(R.id.tv_sale_sort)
    TextView tvSaleSort;
    @BindView(R.id.tv_name_sort)
    TextView tvNameSort;
    @BindView(R.id.iv_name_sort)
    ImageView ivNameSort;
    @BindView(R.id.iv_sale_sort)
    ImageView ivSaleSort;
    @BindView(R.id.combined_chart)
    CombinedChart combinedChart;
    @BindView(R.id.rl_chart)
    LinearLayout rlChart;
    @BindView(R.id.tv_main_data)
    TextView tvMainData;
    @BindView(R.id.tv_main_data_name)
    TextView tvMainDataName;
    @BindView(R.id.tv_sub_data)
    TextView tvSubData;
    @BindView(R.id.tv_rate_of_change)
    TextView tvRateOfChange;
    @BindView(R.id.iv_rate_of_change)
    ImageView ivRateOfChange;

    private Gson gson;
    private List<HomeMetrics> datas;
    //供排序后使用
    private List<HomeMetrics> homeMetricses;
    private ProductListAdapter adapter;
    private MetricsAdapter metricsAdapter;

    private HomeData homeData;
    //当前展示的区间数据
    private HomeMetrics homeMetrics;
    private List<Product> products;
    //区间数据中的选中数据，用于上面指标展示
    private Product product;
    //是否升序，默认降序排序
    private boolean isAsc;
    //选中区域的数据
    private int dateSelected;
    //选中的单条数据
    private int productSelected;
    //上次选中的单条数据
    private int lastProductSelected;
    //记录当前
    private int page;
    //记录选中的指标
    private int itemSelected;
    //当前数据得最大值
    private double maxValue;
    //报表使用的数据
    private List<Item> items;
    //报表X轴值
    private List<String> xAxisList;
    private String[] xAxisValue;
    private boolean isShowChartData;
    private boolean isDoubleClick;
    protected Typeface mTfRegular;
    protected Typeface mTfLight;

    private PopupWindow popupWindow;
    private Context mContext;
    private String urlString;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hometrics);
        mContext = this;
        ButterKnife.bind(this);

        Intent intent = getIntent();
        urlString = intent.getStringExtra("urlString");

        new LoadReportData().execute();
    }

    /*
     * 下载数据
     */
    class LoadReportData extends AsyncTask<String,Void,Map<String,String>> {
        @Override
        protected Map<String,String> doInBackground(String... params) {
            Map<String,String> response = HttpUtil.httpGet(urlString,new HashMap<String, String>());
            Log.i("reportData", response.toString());
                return response;
        }

        @Override
        protected void onPostExecute(Map<String,String> response){
            if (response.get("code").equals("200") || response.get("code").equals("304")) {
                initView();
                initData(response.get("body"));
                setData(false, true);
                mAnimLoading.setVisibility(View.GONE);
            }
            super.onPostExecute(response);
        }
    }

    private void initView() {
        mTfRegular = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
        mTfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");

        metricsAdapter = new MetricsAdapter(mContext, null, this);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        metricsRecyclerView.setLayoutManager(mLayoutManager);
        metricsRecyclerView.setAdapter(metricsAdapter);

        adapter = new ProductListAdapter(mContext, null, this);
        LinearLayoutManager mLayoutManager1 = new LinearLayoutManager(this);
        mLayoutManager1.setOrientation(LinearLayoutManager.VERTICAL);
        productRecyclerView.setLayoutManager(mLayoutManager1);
        productRecyclerView.setAdapter(adapter);
        rlChart.setVisibility(View.GONE);
    }

    private void initData(String mReportData) {
        isShowChartData = false;
        isDoubleClick = false;
        items = new ArrayList<>();
        xAxisList = new ArrayList<>();
        products = new ArrayList<>();
        homeMetricses = new ArrayList<>();
        gson = new Gson();
        //初始化,获取所有数据

        JsonObject returnData = new JsonParser().parse(mReportData).getAsJsonObject();
        homeData = gson.fromJson(returnData, HomeData.class);
        datas = homeData.data;
        homeMetricses.addAll(datas);
        //默认取第1个区域的第1条数据的第1个指标用来展示
        dateSelected = 0;
        productSelected = 0;
        lastProductSelected = 0;
        itemSelected = 0;
        //默认降序显示
        isAsc = false;
        //因为Y轴数据默认向右便宜1个单位，所以X轴数据默认加1
        xAxisList.add("0");
        for (HomeMetrics homeMetrics : datas) {
            xAxisList.add(homeMetrics.getPeriod());
        }
        xAxisValue = xAxisList.toArray(new String[xAxisList.size()]);
    }

    @OnClick({R.id.iv_warning, R.id.tv_name_sort, R.id.tv_sale_sort, R.id.iv_back})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_warning:
                if (isShowChartData) {
                    metricsRecyclerView.setVisibility(View.VISIBLE);
                    rlChart.setVisibility(View.GONE);
                    ivWarning.setImageResource(R.drawable.btn_inf);
                    isShowChartData = false;
                } else {
                    showPopWindows();
                }
                break;
            case R.id.tv_name_sort:
                Toast.makeText(mContext, "暂不支持名字排序", Toast.LENGTH_SHORT).show();
                break;
            case R.id.tv_sale_sort:
                isAsc = !isAsc;
                products = ReorganizeTheDataUtils.sortData(products, itemSelected, isAsc);
                adapter.setDatas(products, itemSelected, maxValue);
                ivSaleSort.setRotation(isAsc ? 0 : 180);
                ivNameSort.setRotation(isAsc ? 0 : 180);
                break;
            case R.id.iv_back:
                onBackPressed();
                break;
        }
    }

    @Override
    public void productSelected(int position) {
        lastProductSelected = productSelected;
        productSelected = position;
//        itemSelected = 0;
        setData(isAsc, true);
    }

    @Override
    public void itemSelected(int page, int position, boolean isDoubleClick) {
        this.isDoubleClick = isDoubleClick;
        //设置排序栏数据
        itemSelected = page * 4 + position;
        setData(false, true);
        if (isDoubleClick) {
            showCombinedChart();
        }
    }

    /**
     * @param isNeedSort    是否排序
     * @param showAnimation 是否展示动画
     */
    private void setData(final boolean isNeedSort, final boolean showAnimation) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
                items.clear();
                homeMetrics = datas.get(dateSelected);
                if (isNeedSort) {
                    products = ReorganizeTheDataUtils.sortData(homeMetrics.products, itemSelected, isAsc);
                } else {
                    products = homeMetrics.products;
                }
                //排序数据，获取当前数据最大值
                List<Product> sortProducts = new ArrayList<>();
                sortProducts.addAll(products);
                maxValue = ReorganizeTheDataUtils.sortData(sortProducts, itemSelected, false).get(0).items.get(itemSelected).main_data.getData();
                //给报表使用的数据,已经排序，则提取得报表数据也要排序后得字段
                for (HomeMetrics homeMetrics : homeMetricses) {
                    Product product = ReorganizeTheDataUtils.sortData(homeMetrics.products, itemSelected, isAsc).get(productSelected);
                    Item item = product.items.get(itemSelected);
                    items.add(item);
                }

//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
                        initChart(showAnimation);
                        combinedChart.invalidate();
                        if (product == null || lastProductSelected != productSelected) {
                            product = products.get(productSelected);
                            //设置下方选中
                            for (int i = 0; i < products.size(); i++) {
                                if (i == productSelected) {
                                    products.get(i).isSelected = true;
                                } else {
                                    products.get(i).isSelected = false;
                                }
                            }
                        }
                        tvTitle.setText(homeMetrics.getHead());
                        tvNameSort.setText(homeMetrics.getHead());
                        tvDateTime.setText(homeMetrics.getPeriod());
                        tvDataTitle.setText(product.getName());
                        //设置指标选中状态
                        for (int i = 0; i < product.items.size(); i++) {
                            if (i == itemSelected) {
                                product.items.get(i).isSelected = true;
                            } else {
                                product.items.get(i).isSelected = false;
                            }
                        }
                        //重组指标数据
                        homeMetrics = ReorganizeTheDataUtils.groupByNumber(product, 4, true);
                        metricsAdapter.setDatas(homeMetrics);
                        //设置排序栏显示文字
                        tvSaleSort.setText(product.items.get(itemSelected).getName());
                        adapter.setDatas(products, itemSelected, maxValue);
                    }
//                });
//            }
//        }).start();
//    }

    //显示报表
    void showCombinedChart() {
        isShowChartData = true;
        ivWarning.setImageResource(R.drawable.pop_close);
        metricsRecyclerView.setVisibility(View.GONE);
        combinedChart.animateY(2000);
        rlChart.setVisibility(View.VISIBLE);
    }

    /**
     * 初始化报表
     *
     * @param showAnimation 是否显示动画
     */
    void initChart(boolean showAnimation) {

        int screenWidth = Utils.getScreenWidth(mContext);
        combinedChart.getLayoutParams().height = screenWidth * 300 / 640;

        //启用缩放和拖动
        combinedChart.setDragEnabled(true);//拖动
        combinedChart.setScaleEnabled(false);//缩放
        combinedChart.setOnChartValueSelectedListener(this);
        combinedChart.getDescription().setEnabled(false);
        combinedChart.setBackgroundColor(ContextCompat.getColor(mContext, R.color.co10));
        combinedChart.setDrawGridBackground(false);
        combinedChart.setDrawBarShadow(false);
        combinedChart.setHighlightFullBarEnabled(false);

        Legend l = combinedChart.getLegend();
        l.setForm(Legend.LegendForm.NONE);//底部样式
        l.setTextColor(ContextCompat.getColor(mContext, R.color.transparent));
        l.setWordWrapEnabled(false);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);

        YAxis rightAxis = combinedChart.getAxisRight();
        rightAxis.setEnabled(false);

        YAxis leftAxis = combinedChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setGranularityEnabled(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        leftAxis.setTextColor(ContextCompat.getColor(mContext, R.color.co4));

        updateTitle(items.get(itemSelected));
        XAxis xAxis = combinedChart.getXAxis();
        xAxis.setTypeface(mTfLight);
        xAxis.setEnabled(true);//设置轴启用或禁用 如果禁用以下的设置全部不生效
        xAxis.setDrawAxisLine(true);//是否绘制轴线
        xAxis.setDrawGridLines(false);//设置x轴上每个点对应的线
        xAxis.setDrawLabels(true);//绘制标签  指x轴上的对应数值
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);//设置x轴的显示位置
        xAxis.setTextSize(8f); //设置X轴文字大小
        xAxis.setGranularityEnabled(true);//是否允许X轴上值重复出现
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.co4));//设置X轴文字颜色
//        //设置竖线的显示样式为虚线
//        //lineLength控制虚线段的长度
//        //spaceLength控制线之间的空间
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setAxisMinimum(0f);//设置x轴的最小值
        xAxis.setAxisMaximum(mDatas.length);//设置最大值
        xAxis.setAvoidFirstLastClipping(true);//图表将避免第一个和最后一个标签条目被减掉在图表或屏幕的边缘
        xAxis.setLabelRotationAngle(0f);//设置x轴标签字体的旋转角度
//        设置x轴显示标签数量  还有一个重载方法第二个参数为布尔值强制设置数量 如果启用会导致绘制点出现偏差
        xAxis.setLabelCount(10);
        xAxis.setGridLineWidth(10f);//设置竖线大小
//        xAxis.setGridColor(Color.RED);//设置竖线颜色
        xAxis.setAxisLineColor(Color.GRAY);//设置x轴线颜色
        xAxis.setAxisLineWidth(1f);//设置x轴线宽度

        CombinedData combinedData = new CombinedData();
        combinedData.setData(generateLineData());
        combinedData.setData(generateBarData());
        combinedData.setValueTypeface(mTfLight);
        xAxis.setAxisMaximum(combinedData.getXMax() + 0.25f);
        //X轴的数据格式
        ValueFormatter xAxisFormatter = new ValueFormatter(combinedChart);
        xAxisFormatter.setmValues(xAxisValue);
        xAxis.setValueFormatter(xAxisFormatter);//格式化x轴标签显示字符
        combinedChart.setData(combinedData);
        combinedChart.invalidate();
        if (showAnimation) {
            combinedChart.animateY(2000);
        }
    }

    /**
     * 曲线
     */
    private LineData generateLineData() {
        LineData lineData = new LineData();
        ArrayList<Entry> entries = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            entries.add(new Entry(index + 1f, (float) items.get(index).sub_data.getData()));
        }
        LineDataSet lineDataSet = new LineDataSet(entries, "对比数据");
        lineDataSet.setValues(entries);
        lineDataSet.setDrawValues(false);//是否在线上显示值
        lineDataSet.setColor(ContextCompat.getColor(mContext, R.color.co3));
        lineDataSet.setLineWidth(2.5f);
        lineDataSet.setCircleColor(ContextCompat.getColor(mContext, R.color.co3));
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);//设置线条类型
        //set.setDrawHorizontalHighlightIndicator(false);//隐藏选中线
        //set.setDrawVerticalHighlightIndicator(false);//隐藏选中线条
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setHighlightEnabled(false);
        lineData.setHighlightEnabled(false);
        lineData.addDataSet(lineDataSet);
        return lineData;
    }

    /**
     * 柱形图数据
     */
    private BarData generateBarData() {
        BarData barData = new BarData();
        ArrayList<BarEntry> entries1 = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            entries1.add(new BarEntry(index + 1f, (float) items.get(index).main_data.getData()));
        }
        BarDataSet barDataSet = new BarDataSet(entries1, "当前数据");
        barDataSet.setValues(entries1);
        barDataSet.setDrawValues(false);//是否在线上显示值
        barDataSet.setColor(Color.rgb(230, 230, 230));
        barDataSet.setHighLightColor(Color.parseColor(items.get(dateSelected).state.getColor()));
        barDataSet.setValueTextColor(Color.rgb(60, 220, 78));
        barDataSet.setValueTextSize(10f);
        barDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        float barWidth = 0.45f;
        barData.addDataSet(barDataSet);
        barData.setBarWidth(barWidth);
        return barData;
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.d("X:", e.getX() + "");
        Log.d("Y:", e.getX() + "");
        int dateSelected = ((int) e.getX() - 1);
        changeDate(dateSelected);
        updateTitle(items.get(dateSelected));
    }

    @Override
    public void onNothingSelected() {

    }

    /**
     * 更改最外层数据
     */
    private void changeDate(int position) {
        if (dateSelected < datas.size() - 1) {
            dateSelected++;
        } else {
            dateSelected = 0;
        }
        dateSelected = position;
        product = null;
        lastProductSelected = 0;
        setData(false, false);
    }

    /**
     * 获取格式化的图表颜色
     *
     * @param position
     * @return
     */
    private List<Integer> formateColors(int position) {
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i == itemSelected) {
                colors.add(Color.parseColor(items.get(position).state.getColor()));
            } else {
                colors.add(ContextCompat.getColor(mContext, R.color.co4));
            }
        }
        return colors;
    }

    /**
     * 更新表格顶部数据
     *
     * @param item
     */
    private void updateTitle(Item item) {
        String result;
        tvMainDataName.setText(item.getName());
        BigDecimal bigDecimal = new BigDecimal(item.main_data.getData());
        double mainData = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        tvMainData.setText(item.main_data.getData() + "");
        BigDecimal bigDecimal1 = new BigDecimal(item.sub_data.getData());
        double subData = bigDecimal1.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        tvSubData.setText(subData + "");
        BigDecimal bigDecimal2;
        if (bigDecimal1.compareTo(BigDecimal.ZERO) == 0) {
            bigDecimal2 = bigDecimal.subtract(bigDecimal1).multiply(new BigDecimal(100));
        } else {
            bigDecimal2 = bigDecimal.subtract(bigDecimal1).multiply(new BigDecimal(100)).divide(bigDecimal1, 2);
        }
        double f1 = bigDecimal2.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        result = f1 + "%";
        tvRateOfChange.setText(result);
        if ("up".equalsIgnoreCase(item.state.getArrow())) {
            Log.i("arrow", item.state.getArrow() + "  " + item.state.getColor());
            switch (item.state.getColor()) {
                case "#F2E1AC" :
                    ivRateOfChange.setImageResource(R.drawable.up_yellowarrow);
                    break;

                case "#F2836B" :
                    ivRateOfChange.setImageResource(R.drawable.up_redarrow);
                    break;

                case "#63A69F" :
                    ivRateOfChange.setImageResource(R.drawable.up_greenarrow);
                    break;

                default:
                    ivRateOfChange.setVisibility(View.GONE);
                    break;
            }
        } else {
            Log.i("arrow", item.state.getArrow() + "  " + item.state.getColor());
            switch (item.state.getColor()) {
                case "#F2E1AC" :
                    ivRateOfChange.setImageResource(R.drawable.down_yellowarrow);
                    break;

                case "#F2836B" :
                    ivRateOfChange.setImageResource(R.drawable.down_redarrow);
                    break;

                case "#63A69F" :
                    ivRateOfChange.setImageResource(R.drawable.down_greenarrow);
                    break;

                default:
                    ivRateOfChange.setVisibility(View.GONE);
                    break;
            }
        }
    }

    /**
     * 显示图表说明
     */
    private void showPopWindows() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.pop_warning, null);
        popupWindow = new PopupWindow(view, Utils.getScreenWidth(mContext), ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        view.findViewById(R.id.iv_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
    }

    /*
     * 返回
     */
    public void dismissActivity(View v) {
        HomeTricsActivity.this.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    protected float[] mDatas = new float[]{
            29, 33, 50, 59, 10, 5, 50, 18, 29, 19
    };
}
