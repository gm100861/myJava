package com.pinshang.qingyun.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.pinshang.qingyun.purchase.dto.PurchaseOrderDetailODto;
import com.pinshang.qingyun.purchase.dto.PurchaseOrderItemDetailODto;
import com.pinshang.qingyun.util.ListUtils;
import com.pinshang.qingyun.util.PdfUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by honway on 2017/4/20 13:43.
 * 采购单生成PDF
 */
@Component
public class PurchaseOrderPdfService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PurchaseOrderPdfService.class);

    private static final float TABLE_WIDTH_A4 = 99f;

    private static final int FONT_SIZE = 8;

    /**
     * 多少条,刚好算一页. 由于还要算上页脚和分页信息
     * 所以如果刚好一页,值要比多页的第一页显示的内容少.
     * 刚好一页的话,分页信息和页脚信息都在第一页,所以显示的要少一些
     */
    private static final Integer FIRST_PAGE_THRESHOLD = 32;

    /**
     * 如果超过一页,第一页显示多少行,按照当前模板 36行是最好的
     */
    private static final Integer FIRST_PAGE_ROW = 36;

    /**
     * 其它各页显示多少行,按照当前模板 46行是最好的
     */
    private static final Integer OTHER_PAGE_ROW = 46;
    /**
     * 生成PDF
     * @param orderDetailODto 数据DTO
     * @param outputStream  响应stream
     */
    public void createPdf(PurchaseOrderDetailODto orderDetailODto, OutputStream outputStream) {
        if (orderDetailODto == null) {
            LOGGER.error("采购单信息为空,不能生成pdf.");
            return;
        }
        Date data = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String printTime = format.format(data);
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font font = PdfUtils.chineseFont(FONT_SIZE);
            PdfPTable tableTitle = proceedTableTitle(orderDetailODto, font);
            List<PurchaseOrderItemDetailODto> purchaseOrderItemList = orderDetailODto.getCommodityList();
            List<List<PurchaseOrderItemDetailODto>> lists;
            if (purchaseOrderItemList.size() <= FIRST_PAGE_THRESHOLD) {
                //小于或等于刚好一页的值,说明可以一页打印完,其实就不存在分页了
                lists = ListUtils.splitList(purchaseOrderItemList, FIRST_PAGE_THRESHOLD);
            } else {
                //一页不能装下,就必须要分页了. 截取list的时候,第一页的数据截取的少,后面各页截取相同
                lists = ListUtils.splitList(purchaseOrderItemList, FIRST_PAGE_ROW, OTHER_PAGE_ROW);
            }
            int page = 0;
            int totalPage = lists.size();
            BigDecimal totalAmount = BigDecimal.ZERO;
            //只第一页显示表头
            document.add(tableTitle);
            Font fontBold = PdfUtils.chineseFontBold(FONT_SIZE);
            for (List<PurchaseOrderItemDetailODto> vos : lists) {
                PdfPTable table = new PdfPTable(new float[]{4,20,7,10,20,5,8,5,10});
                table.setWidthPercentage(99);
                PdfPCell cellTitle = new PdfPCell();
                cellTitle.setBorder(Rectangle.NO_BORDER);
                cellTitle.setCellEvent(new PurchaseOrderCustomCell());
                cellTitle.setPhrase(new Phrase("#", fontBold));
                table.addCell(cellTitle);
                cellTitle.setPhrase(new Phrase("商品名称", fontBold));
                table.addCell(cellTitle);
                cellTitle.setPhrase(new Phrase("商品编码", fontBold));
                table.addCell(cellTitle);
                cellTitle.setPhrase(new Phrase("一级分类", fontBold));
                table.addCell(cellTitle);
                cellTitle.setPhrase(new Phrase("规格", fontBold));
                table.addCell(cellTitle);
                cellTitle.setPhrase(new Phrase("数量", fontBold));
                table.addCell(cellTitle);
                cellTitle.setPhrase(new Phrase("计量单位", fontBold));
                table.addCell(cellTitle);
                cellTitle.setPhrase(new Phrase("采购价", fontBold));
                table.addCell(cellTitle);
                cellTitle.setPhrase(new Phrase("金额", fontBold));
                table.addCell(cellTitle);
                for (int i = 0; i < vos.size(); i++) {
                    PurchaseOrderItemDetailODto dto = vos.get(i);
                    PdfPCell cell = new PdfPCell();
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setFixedHeight(16);
                    cell.setVerticalAlignment(PdfPCell.ALIGN_BOTTOM);
                    cell.setCellEvent(new PurchaseOrderCustomCell());
                    cell.setPhrase(new Phrase(i + 1 + "", font));
                    table.addCell(cell);
                    cell.setPhrase(new Phrase(dto.getCommodityName(), font));
                    table.addCell(cell);
                    cell.setPhrase(new Phrase(dto.getCommodityCode(), font));
                    table.addCell(cell);
                    cell.setPhrase(new Phrase(dto.getCategoryName(), font));
                    table.addCell(cell);
                    cell.setPhrase(new Phrase(dto.getCommoditySpec(), font));
                    table.addCell(cell);
                    cell.setPhrase(new Phrase(dto.getQuantity().toString(), font));
                    table.addCell(cell);
                    cell.setPhrase(new Phrase(dto.getNumberUnit(), font));
                    table.addCell(cell);
                    cell.setPhrase(new Phrase(dto.getPrice().toString(), font));
                    table.addCell(cell);
                    totalAmount = totalAmount.add(dto.getAmount());
                    cell.setPhrase(new Phrase(dto.getAmount().toString(), font));
                    table.addCell(cell);
                }
                page++;
                if (page >= totalPage || totalPage == 1) {
                    //最后一页显示总金额
                    PdfPCell placeholderCell = new PdfPCell(new Phrase(" "));
                    placeholderCell.setColspan(5);
                    placeholderCell.setBorder(Rectangle.NO_BORDER);
                    table.addCell(placeholderCell);
                    PdfPCell totalAmountLabelCell = new PdfPCell(new Phrase("总额:", font));
                    totalAmountLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalAmountLabelCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
                    totalAmountLabelCell.setBorder(Rectangle.NO_BORDER);
                    totalAmountLabelCell.setBorderWidthBottom(1);
                    totalAmountLabelCell.setBorderColorBottom(BaseColor.BLUE);
                    PdfPCell totalAmountCell = new PdfPCell(new Phrase(totalAmount.toString(), font));
                    table.addCell(totalAmountLabelCell);
                    totalAmountCell.setColspan(3);
                    totalAmountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalAmountCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
                    totalAmountCell.setBorderWidthBottom(1);
                    totalAmountCell.setBorder(Rectangle.NO_BORDER);
                    totalAmountCell.setBorderWidthBottom(1);
                    totalAmountCell.setBorderColorBottom(BaseColor.BLUE);
                    table.addCell(totalAmountCell);

                    //备注
                    PdfPCell remarkLabel = new PdfPCell(new Phrase("备注: ", font));
                    remarkLabel.setColspan(9);
                    remarkLabel.setBorder(Rectangle.NO_BORDER);
                    table.addCell(remarkLabel);

                    PdfPCell remarkFixContent = new PdfPCell(new Phrase("1.请确保以产品的最低价供应，如与报价单不符，请提前通知", font));
                    remarkFixContent.setBorder(Rectangle.NO_BORDER);
                    remarkFixContent.setColspan(9);
                    table.addCell(remarkFixContent);

                    PdfPCell remark = new PdfPCell(new Phrase("2."+ orderDetailODto.getRemark(), font));
                    remark.setBorder(Rectangle.NO_BORDER);
                    remark.setColspan(9);
                    table.addCell(remark);
                }

                PdfPCell tableBottom = new PdfPCell(new Phrase("第 " + page + " 页, 共 " + totalPage + "页.                打印时间: " + printTime, font));
                tableBottom.setColspan(9);
                tableBottom.setBorder(0);
                tableBottom.setPaddingTop(3);
                table.addCell(tableBottom);
                document.add(table);
                if (page <= totalPage) {
                    document.newPage();
                }
            }
            document.close();
            outputStream.flush();
            outputStream.close();
        } catch (DocumentException | IOException e) {
            LOGGER.error("采购单生成PDF时异常:{}", e.getMessage());
        }
    }

    /**
     * 处理采购单表头
     * @param purchaseOrderResponseVo 采购单响应VO
     * @param font 中文字体
     * @return 返回表头
     */
    private PdfPTable proceedTableTitle(PurchaseOrderDetailODto purchaseOrderResponseVo, Font font) throws IOException, DocumentException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        PdfPCell emptyCell = new PdfPCell(new Phrase(" "));
        emptyCell.setBorder(Rectangle.NO_BORDER);
        PdfPTable table = new PdfPTable(new float[]{4,2,4});
        table.setWidthPercentage(TABLE_WIDTH_A4);
        PdfPCell cell ;
        cell = new PdfPCell(new Phrase("采购订单", PdfUtils.chineseFontBold(19)));
        cell.setColspan(3);
        cell.setBorder(0);
        cell.setFixedHeight(34);
        cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setCellEvent(new PurchaseOrderCustomCell());
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("采购方:上海品上生活电子商务有限公司", font));
        cell.setBorder(0);
        cell.setBorderColorBottom(BaseColor.BLUE);
        table.addCell(cell);

        table.addCell(emptyCell);

        cell = new PdfPCell(new Phrase("采购单编号: " + purchaseOrderResponseVo.getPurchaseCode(), font));
        cell.setBorder(0);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("采购员:" + purchaseOrderResponseVo.getCreateName(), font));
        cell.setBorder(0);
        cell.setCellEvent(new PurchaseOrderCustomCell());
        table.addCell(cell);


        table.addCell(emptyCell);

        cell = new PdfPCell(new Phrase("下单日期:" + dateFormat.format(purchaseOrderResponseVo.getOrderTime()), font));
        cell.setBorder(0);
        table.addCell(cell);

        table.addCell(emptyCell);
        table.addCell(emptyCell);

        cell = new PdfPCell(new Phrase("最晚收货日期:" + dateFormat.format(purchaseOrderResponseVo.getLatestReceiveTime()), font));
        cell.setBorder(0);
        cell.setCellEvent(new PurchaseOrderCustomCell());
        table.addCell(cell);

        table.addCell(emptyCell);
        table.addCell(emptyCell);
        table.addCell(emptyCell);

        cell = new PdfPCell(new Phrase("供应商:" + purchaseOrderResponseVo.getSupplierName(), font));
        cell.setBorder(0);
        cell.setCellEvent(new PurchaseOrderCustomCell());
        table.addCell(cell);

        table.addCell(emptyCell);

        cell = new PdfPCell(new Phrase("收货仓库:" + purchaseOrderResponseVo.getWarehouseName(), font));
        cell.setBorder(0);
        table.addCell(cell);


        table.addCell(emptyCell);
        table.addCell(emptyCell);

        cell = new PdfPCell(new Phrase("仓库地址:" + "上海市浦东新区宣桥镇宣春路201号", font));
        cell.setBorder(0);
        table.addCell(cell);

        table.addCell(emptyCell);
        table.addCell(emptyCell);

        cell = new PdfPCell(new Phrase("仓库电话:" + "021-33998899", font));
        cell.setBorder(0);
        cell.setCellEvent(new PurchaseOrderCustomCell());
        table.addCell(cell);

        table.addCell(emptyCell);
        table.addCell(emptyCell);
        table.addCell(emptyCell);
        return table;
    }
}
