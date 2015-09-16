package org.linuxsogood.miscTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TestPOIP1Title {
	public static void main(String[] args) throws Exception {
		/**
		 * 用来处理二级分类对应的excel,形如以下,性别和年龄段为合并的单元格,分别对应下面的子类
		 * |    性别        |    年龄段        |
		 * | 男    | 女       |青年 |中年 |老年|
		 */
		String pathname = "E:\\files\\title.xlsx";
		File file = new File(pathname);
		InputStream in = new FileInputStream(file);
		//得到整个excel对象
		XSSFWorkbook excel = new XSSFWorkbook(in);
		//获取整个excel有多少个sheet
		int sheets = excel.getNumberOfSheets();
		//便利每一个sheet
		for(int i = 0 ; i < sheets ; i++ ){
			XSSFSheet sheet = excel.getSheetAt(i);
			//如果是一个空的sheet则跳过本地循环
			if(sheet == null){
				continue;
			}
			//获取这个表格,总共有多少个合并单元格
			int mergedRegions = sheet.getNumMergedRegions();
			//获取第一行
			XSSFRow row2 = sheet.getRow(0);
			/**
			 * 创建一个用于保存分类的map,key为合并单元格的起始位置,value为:合并单元格的结束位置-合并单元格内的内容
			 * 这样后面拿到列值的时候,根据列的位置从map中拿到合并单元格的结束位置和内容,如果列的位置等于map的key,表示这个是
			 * 一个合并单元格的分类下面的第一个格子,保存一下分类和这个全并单元格的结束位置,后面再取的时候,判断是不是小于这个
			 * 合并单元格的结束位置,如果小于或等于表示还是当前分类,如果不小于等于当前合并单元格的结束位置,表示可能是下一个分类
			 * 或者是分类未知
			 */
			Map<Integer,String> category = new HashMap<Integer, String>();
			//遍历当前sheet,拿到所有的合并单元格的起始和结束位置,以及合并单元格里的内容
			for(int j = 0 ; j < mergedRegions; j++ ){
				CellRangeAddress rangeAddress = sheet.getMergedRegion(j);
				int firstRow = rangeAddress.getFirstColumn(); //合并单元格的起始位置
				int lastRow = rangeAddress.getLastColumn();  //合并单元格的结束位置
				category.put(firstRow, lastRow+"-"+row2.getCell(firstRow).toString());
			}
			//便利第二行,即子类所在的行
			for( int rowNum = 1 ; rowNum <= sheet.getLastRowNum() ; rowNum++ ){
				XSSFRow row = sheet.getRow(rowNum);
				if(row == null){
					continue;
				}
				short lastCellNum = row.getLastCellNum();	//获取该行的最后一列单元格的位置
				String cate = "";	//定义一个临时变量用于保存分类
				Integer maxIndex = 0;	//定义一个变量用于保存一个合并单元格的最大位置
				for( int col = row.getFirstCellNum() ; col < lastCellNum ; col++ ){
					XSSFCell cell = row.getCell(col);
					if(cell == null ){
						continue;
					}
					if("".equals(cell.toString())){
						continue;
					}
					int columnIndex = cell.getColumnIndex();
					String string = category.get(columnIndex);
					if(string != null && !string.equals("")){
						String[] split = string.split("-");
						cate = split[1];
						maxIndex = Integer.parseInt(split[0]);
						System.out.println(cate+"<-->"+cell.toString());
					}else {
						//如果当前便利的列编号小于等于合并单元格的结束,说明分类还是上面的分类名称
						if(columnIndex<=maxIndex){
							System.out.println(cate+"<-->"+cell.toString());
						}else {
							System.out.println("分类未知"+"<-->"+cell.toString());
						}
					}
				}
			}
		}
	}
}
