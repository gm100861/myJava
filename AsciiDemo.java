package com.example.datagen.lmdb;

import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;

/**
 * @author admin
 */
public class AsciiDemo {

    public static void main(String[] args) {
        String s = "1";
        char letter = (char) Integer.valueOf(s).intValue();
        System.out.println("letter = " + letter);
        String resp = "123,34,77,101,115,115,97,103,101,34,58,34,79,75,34,44,34,82,101,113,117,101,115,116,73,100,34,58,34,54,70,55,54,53,65,67,54,45,52,49,57,50,45,53,70,52,48,45,57,52,55,49,45,53,48,49,48,51,68,48,55,48,51,51,70,34,44,34,67,111,100,101,34,58,34,79,75,34,44,34,66,105,122,73,100,34,58,34,50,50,56,49,48,50,54,55,53,49,53,56,53,55,56,49,49,53,94,48,34,125";
        print(resp);
        String str = "{\"Message\":\"OK\",\"RequestId\":\"6F765AC6-4192-5F40-9471-50103D07033F\",\"Code\":\"OK\",\"BizId\":\"228102675158578115^0\"}";
        printBytes(str);
    }

    public static void print(String strs) {
        String[] strings = strs.split(",");
        for (String string : strings) {
            System.out.print((char) Integer.valueOf(string).intValue());
        }
    }

    public static void printBytes(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        System.out.println();
        System.out.println("join = " + StrUtil.join(",", bytes));
    }
}
