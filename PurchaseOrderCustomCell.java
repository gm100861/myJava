package com.pinshang.qingyun.pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;

/**
 * Created by honway on 2017/4/20 15:34.
 * 拣货单自定义单元格样式
 */
public class PurchaseOrderCustomCell implements PdfPCellEvent {
    @Override
    public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
        PdfContentByte cb = canvases[PdfPTable.LINECANVAS];
        cb.saveState();
        cb.setLineWidth(0.5f);
        cb.setLineDash(new float[]{1.0f,1.0f}, 0);
        cb.moveTo(position.getLeft(), position.getBottom());
        cb.lineTo(position.getRight(), position.getBottom());
        cb.setColorStroke(BaseColor.BLUE);
        cb.stroke();
        cb.restoreState();
    }
}
