package org.linuxsogood.reference.chp1.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by honway on 2017/1/12.
 */
public class TablePDFDemo {
    private static String path;
    static {
        try {
            File file = ResourceUtils.getFile("classpath:application.yml");
            path = file.getParent();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws FileNotFoundException {
        ArrayList<Somain> list = new ArrayList<>();
        Somain somain = new Somain("00001","66601","品尚生活1",new Date().toString(),"7778889","88899","29.11","1");
        Somain somain1 = new Somain("00001","66601","品尚生活2",new Date().toString(),"7778889","44422","29.11","0");
        Somain somain2 = new Somain("00001","66601","品尚生活3",new Date().toString(),"7778889","1123123","29.11","3");
        Somain somain3 = new Somain("00001","66601","品尚生活4",new Date().toString(),"7778889","1223","29.11","1");
        Somain somain4 = new Somain("00001","66601","品尚生活5",new Date().toString(),"7778889","22231","29.11","3");
        Somain somain5 = new Somain("00001","66601","品尚生活6",new Date().toString(),"7778889","22231","29.11","3");
        Somain somain6 = new Somain("00001","66601","品尚生活7",new Date().toString(),"7778889","22231","29.11","3");
        Somain somain7 = new Somain("00001","66601","品尚生活8",new Date().toString(),"7778889","22231","29.11","3");
        Somain somain8 = new Somain("00001","66601","品尚生活9",new Date().toString(),"7778889","232323","29.11","3");
        Somain somain9 = new Somain("00001","66601","品尚生活9",new Date().toString(),"7778889","232323","29.11","3");
        Somain somain11 = new Somain("00001","66601","品尚生活9",new Date().toString(),"7778889","232323","29.11","3");
        Somain somain12 = new Somain("00001","66601","品尚生活9",new Date().toString(),"7778889","232323","29.11","3");
        list.add(somain);
        list.add(somain1);
        list.add(somain2);
        list.add(somain3);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain4);
        list.add(somain5);
        list.add(somain6);
        list.add(somain7);
        list.add(somain8);
        list.add(somain);
        list.add(somain9);
        list.add(somain11);
        list.add(somain12);
        method(list);
    }

    public static void method(List<Somain> list) throws FileNotFoundException {

        FileOutputStream out = new FileOutputStream(path+"/test.pdf");
        Rectangle rect = new Rectangle(PageSize.A4);
        Document document = new Document(rect);
        BaseFont bf = null;
        Font fontChinese = null;
        Font fontChar=new Font();
        fontChar.setSize(7);
        fontChar.setColor(BaseColor.DARK_GRAY);
        try {
            bf = BaseFont.createFont(path+"/simhei.ttf",BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            fontChinese = new Font(bf, 10, Font.NORMAL);// 中文字体
            fontChinese.setSize(8);//字体大小
            PdfWriter pdfWriter=PdfWriter.getInstance(document, out);// 文档输出流。
            document.open();
            PdfPTable table = new PdfPTable(new float[] {28,19, 15,27, 12,15, 14, 16});// 8列的表格以及单元格的宽度。
            table.setPaddingTop(2);// 顶部空白区高度
            table.setTotalWidth(360);//表格整体宽度
            //PdfPCell cell = new PdfPCell(new Phrase("Details of past one month on sale."));

            //cell.setColspan(8);//占据八列
            //cell.setRowspan(2);
            //table.addCell(cell);
            table.addCell(new Paragraph("销售单号", fontChinese));
            table.addCell(new Paragraph("客户编号", fontChinese));
            table.addCell(new Paragraph("客户名称", fontChinese));
            table.addCell(new Paragraph("销售日期", fontChinese));
            table.addCell(new Paragraph("经手人", fontChinese));
            table.addCell(new Paragraph("总金额", fontChinese));
            table.addCell(new Paragraph("预付款", fontChinese));
            table.addCell(new Paragraph("购买方式", fontChinese));
            for(Somain s :list){//将集合内的对象循环写入到表格
                table.addCell(new Paragraph(s.getSoId(),fontChar));
                table.addCell(new Paragraph(s.getCustomerCode(),fontChar));
                table.addCell(new Paragraph(s.getName(),fontChar));
                table.addCell(new Paragraph(s.getCreateTime(),fontChar));
                table.addCell(new Paragraph(s.getAccount(),fontChar));
                table.addCell(new Paragraph(String.valueOf(s.getSoTotal()),fontChar));
                table.addCell(new Paragraph(String.valueOf(s.getPrePayFee()),fontChar));
                if(Integer.parseInt(s.getPayType())==1){
                    table.addCell(new Paragraph("预付款发货", fontChinese));
                }else if(Integer.parseInt(s.getPayType())==0){
                    table.addCell(new Paragraph("货到付款", fontChinese));
                }else{
                    table.addCell(new Paragraph("款到发货", fontChinese));
                }
            }
            String content =  "             线路编码:A001                线路名称:上海杨浦              线路组:美食连锁            发货仓库：一厂冷库";
            String content2 = "             订单日期：2016-09-21         送货员：朱国良                 打印批次：1-专卖";
            String empty = "                                                                                                        ";
            Paragraph element = new Paragraph(content, fontChinese);
            element.setPaddingTop(20);
            document.add(element);
            //document.add(new Paragraph(empty));
            Paragraph element1 = new Paragraph(content2, fontChinese);
            element1.setPaddingTop(20);
            document.add(element1);
            document.add(new Paragraph(empty));
            document.add(table);
            document.close();
            pdfWriter.flush();
            System.out.println("document itext pdf write finished...100%");
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
