package com.yonghui.homemetrics.utils;

import com.yonghui.homemetrics.data.response.HomeMetrics;
import com.yonghui.homemetrics.data.response.Item;
import com.yonghui.homemetrics.data.response.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by CANC on 2017/4/6.
 */

public class ReorganizeTheDataUtils {

    /**
     * 重新组织数据
     * 供上面用
     *
     * @param product
     * @param number
     * @param saveRemainder
     * @return
     */
    public static HomeMetrics groupByNumber(Product product,
                                            int number,
                                            boolean saveRemainder) {
        if (product == null || product.items.size() == 0) {
            return null;
        }
        HomeMetrics homeMetrics = new HomeMetrics();
        List<Product> products = new ArrayList<>();
        List<Item> themeItems = product.items;

        int group = themeItems.size() / number;
        if (group == 0) {
            List<Item> items = new ArrayList<>();
            Product product1 = new Product();
            for (int i = 0; i < themeItems.size(); i++) {
                items.add(themeItems.get(i));
            }
            product1.items = items;
            products.add(product1);
            homeMetrics.products = products;
            return homeMetrics;
        } else {
            for (int i = 0; i < group; i++) {
                Product product1 = new Product();
                List<Item> items = new ArrayList<>();
                for (int j = 0; j < number; j++) {
                    items.add(themeItems.get(i * number + j));
                }
                product1.items = items;
                products.add(product1);
            }
            //取余下的项
            int remainder = themeItems.size() % number;
            if (saveRemainder && remainder > 0) {
                Product product2 = new Product();
                List<Item> items = new ArrayList<>();
                for (int i = 0; i < remainder; i++) {
                    items.add(themeItems.get(group * number + i));
                }
                product2.items = items;
                products.add(product2);
            }
            homeMetrics.products = products;
            return homeMetrics;
        }
    }

    /**
     * 排序
     *
     * @param datas    待排序数据
     * @param position 选择的排序指标
     * @param isAsc    降序升序
     */
    public static List<Product> sortData(List<Product> datas, int position, boolean isAsc) {

        if (isAsc) {
            for (int i = 0; i < datas.size(); i++) {
                for (int j = 0; j < datas.size() - i - 1; j++) {
                    if (datas.get(j).items.get(position).main_data.getData()
                            > datas.get(j + 1).items.get(position).main_data.getData()) {
                        Product product = datas.get(j);
                        datas.set(j, datas.get(j + 1));
                        datas.set(j + 1, product);
                    }
                }
            }
        } else {
            for (int i = 0; i < datas.size(); i++) {
                for (int j = 0; j < datas.size() - i - 1; j++) {
                    if (datas.get(j).items.get(position).main_data.getData()
                            < datas.get(j + 1).items.get(position).main_data.getData()) {
                        Product product = datas.get(j);
                        datas.set(j, datas.get(j + 1));
                        datas.set(j + 1, product);
                    }
                }
            }
        }
        return datas;
    }
}
