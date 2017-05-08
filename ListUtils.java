package com.pinshang.qingyun.util;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {
    /**
     * 以指定大小切割list,把切割的小list放到一个大list中
     * @param list 要切割的list
     * @param pageSize 多少条记录切成一个小list
     * @param <T> list泛型对象
     * @return 返回一个大list,里面保存了切割的小list
     */
    public static <T> List<List<T>> splitList(List<T> list, int pageSize) {
        List<List<T>> result = new ArrayList<>();
        ArrayList<T> al = new ArrayList<>();
        for(T x : list){
            al.add(x);
            if (pageSize == al.size()){
                result.add(al);
                al = new ArrayList<>();
            }
        }
        if (0 != al.size()) {
            result.add(al);
        }
        return result;
    }

    /**
     * 切割给定list(非等分切割), 第一次切割使用first的值,后面都使用last
     * @param list 要切割的list
     * @param first 第一段要切割的长度
     * @param last 后面要切割的长度
     * @param <T> 对象信息
     * @return 返回切割后的结果.
     */
    public static <T> List<List<T>> splitList(List<T> list, int first, int last) {
        List<List<T>> result = new ArrayList<>();
        ArrayList<T> al = new ArrayList<>();
        boolean firstFlag = true;
        for (T t : list) {
            al.add(t);
            if (firstFlag) {
                if (first == al.size()) {
                    result.add(al);
                    firstFlag = false;
                    al = new ArrayList<>();
                }
            } else {
                if (last == al.size()) {
                    result.add(al);
                    al = new ArrayList<>();
                }
            }
        }
        if (0 != al.size())
            result.add(al);
        return result;
    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");
        list.add("f");
        list.add("g");
        list.add("h");
        List<List<String>> lists = splitList(list, 2, 3);
        for (List<String> strings : lists) {
            System.out.println(strings.size());
        }
    }
}
