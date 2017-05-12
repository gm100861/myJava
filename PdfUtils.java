package com.pinshang.qingyun.util;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * Created by honway on 2017/4/19.
 * pdf生成辅助类
 */
public class PdfUtils {

    /**
     * 获取中文字体
     * @param fontSize 指定中文字体大小
     * @return 返回中文字体
     * @throws IOException
     * @throws DocumentException
     */
    public static Font chineseFont(float fontSize) throws IOException, DocumentException {
        ClassPathResource pathResource = new ClassPathResource("simfang.ttf");
        String absolutePath = pathResource.getFilename();
        BaseFont bf = BaseFont.createFont(absolutePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        return new Font(bf, fontSize, Font.NORMAL);// 中文字体
    }
    public static Font chineseFontBold(float fontSize) throws IOException, DocumentException {
        ClassPathResource pathResource = new ClassPathResource("simfang.ttf");
        String absolutePath = pathResource.getFilename();
        BaseFont bf = BaseFont.createFont(absolutePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        return new Font(bf, fontSize, Font.BOLD);// 中文字体
    }
}
