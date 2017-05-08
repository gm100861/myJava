package com.pinshang.qingyun.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.pinshang.qingyun.util.BarcodeUtils;
import com.pinshang.qingyun.util.PdfUtils;
import com.pinshang.qingyun.vo.PlankCodeVo;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;

/**
 * Created by honway on 2017/5/5 11:04.
 * 板单号生成PDF服务类
 */
@Component
@ConfigurationProperties(prefix = "qingyun")
public class PlankPdfService {

    /**
     * PDF 文件保存位置.
     */
    private String pdfSavePath;

    /**
     * 打印纸张大小及比例
     * 宽度为100mm
     * 高度为100mm
     * 比例为2.83
     */
    private static final float SCALE = 2.83f;
    private static final float PAPER_WIDTH = 100 * SCALE;
    private static final float PAPER_HEIGHT = 100 * SCALE;

    public void createPdf(PlankCodeVo plankCodeVo) throws DocumentException, IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        FileOutputStream fileOutputStream = new FileOutputStream(new File(getPdfSavePath() + "bd-" + UUID.randomUUID().toString().substring(0, 8) + ".pdf"));

        Font font = PdfUtils.chineseFont(10);

        PdfPCell blankLine = new PdfPCell(new Phrase(" "));
        blankLine.setBorder(Rectangle.NO_BORDER);
        blankLine.setColspan(2);

        Document document = new Document(new RectangleReadOnly(PAPER_HEIGHT, PAPER_WIDTH),5,5,5,5);
        PdfWriter.getInstance(document, fileOutputStream);
        document.open();
        PdfPTable table = new PdfPTable(new float[]{6,4});
        table.setWidthPercentage(100);

        table.addCell(blankLine);

        PdfPCell contentCell = new PdfPCell(new Phrase("货位: " + plankCodeVo.getShelfName(), font));
        contentCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contentCell);

        contentCell = new PdfPCell(new Phrase("仓库: " + plankCodeVo.getWarehouseName().substring(0, 7), font));
        contentCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contentCell);

        table.addCell(blankLine);

        contentCell = new PdfPCell(new Phrase("商品名称: " + plankCodeVo.getCommodityName().substring(0, 10), font));
        contentCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contentCell);

        contentCell = new PdfPCell(new Phrase("到期日期: " + dateFormat.format(plankCodeVo.getExpirationDate()), font));
        contentCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contentCell);

        table.addCell(blankLine);

        contentCell = new PdfPCell(new Phrase("规格: " + plankCodeVo.getCommoditySpec().substring(0, 15), font));
        contentCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contentCell);

        contentCell = new PdfPCell(new Phrase("商品编码: " + plankCodeVo.getCommodityCode(), font));
        contentCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contentCell);

        table.addCell(blankLine);

        contentCell = new PdfPCell(new Phrase("收货数量: " + plankCodeVo.getReceiveNumber(), font));
        contentCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contentCell);

        contentCell = new PdfPCell(new Phrase("收货日期: " + dateFormat.format(plankCodeVo.getReceiveDate()), font));
        contentCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contentCell);

        table.addCell(blankLine);

        contentCell = new PdfPCell(new Phrase("收货备注: " + plankCodeVo.getReceiveRemark().substring(0, 21), font));
        contentCell.setColspan(2);
        contentCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contentCell);

        table.addCell(blankLine);

        ByteArrayOutputStream byteArrayOutputStream = BarcodeUtils.generatorBarcode(plankCodeVo.getPlankCode());
        Image image = Image.getInstance(byteArrayOutputStream.toByteArray());
        image.setAlignment(Element.ALIGN_CENTER);
        image.scalePercent(60);
        PdfPCell cell = new PdfPCell(image);
        cell.setColspan(2);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);


        Font fontBold = PdfUtils.chineseFontBold(16);
        PdfPCell codeCell = new PdfPCell(new Phrase("板单号: " + plankCodeVo.getPlankCode(), fontBold));
        codeCell.setColspan(2);
        codeCell.setBorder(Rectangle.NO_BORDER);
        codeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        codeCell.setVerticalAlignment(Element.ALIGN_CENTER);
        table.addCell(codeCell);

        document.add(table);
        document.close();
    }


    public String getPdfSavePath() {
        return pdfSavePath;
    }

    public void setPdfSavePath(String pdfSavePath) {
        this.pdfSavePath = pdfSavePath;
    }
}
