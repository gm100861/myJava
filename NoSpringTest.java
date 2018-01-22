package com.plusnet.search.core.dal.ibatis;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class NoSpringTest {

    public static void main(String[] args) {
        //System.out.println(converterToSpell("虎(音si)亭镇"));
        String abc = "abc ";
        System.out.println(abc);
        abc = abc.trim();
        System.out.println(abc);
    }

    public static String converterToSpell(String chines) {
        StringBuffer sb = new StringBuffer();
        char[] nameChar = chines.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        for (int i = 0; i < nameChar.length; i++) {
            if (nameChar[i] > 128) {
                try {
                    sb.append(PinyinHelper.toHanyuPinyinStringArray(
                            nameChar[i], defaultFormat)[0]);
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            } else {
                sb.append(nameChar[i]);
            }
        }
        return sb.toString();
    }
    public static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("^[+|-]?[0-9][.]?[0-9]*");
        return pattern.matcher(str).matches();
    }
}
