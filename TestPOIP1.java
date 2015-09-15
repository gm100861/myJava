package org.linuxsogood.miscTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.poi.hssf.util.Region;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TestPOIP1 {
	public static void main(String[] args) throws Exception {
		
		Random random = new Random();
		
		String pathname = "E:\\files\\P1-需求行为对应平台.xlsx";
		//String pathname = "E:\\files\\P1-需求行为对应平台.xlsx";
		File file = new File(pathname);
		//FileWriter writer = new FileWriter(new File("e:/sqls_content_source.txt"));
		InputStream in = new FileInputStream(file);
		//得到整个excel对象
		XSSFWorkbook excel = new XSSFWorkbook(in);
		//获取整个excel有多少个sheet
		int sheets = excel.getNumberOfSheets();
		//便利第一个sheet
		Map<String,String> colMap = new HashMap<String, String>();
		for(int i = 0 ; i < sheets ; i++ ){
			XSSFSheet sheet = excel.getSheetAt(i);
			if(sheet == null){
				continue;
			}
			//便利每一行
			for( int rowNum = 8 ; rowNum <= sheet.getLastRowNum() ; rowNum++ ){
				//System.out.println("------开始遍历第"+rowNum+"行-----");
				System.out.println();
				XSSFRow row = sheet.getRow(rowNum);
				if(row == null){
					continue;
				}
				short firstCellNum = row.getFirstCellNum();
				short lastCellNum = row.getLastCellNum();
				System.out.print("insert into t_content_source(station_name,type_code,edition_name,detail_name,entry_url) values( ");
				for( int col = firstCellNum ; col < lastCellNum ; col++ ){
					XSSFCell cell = row.getCell(col);
					if(cell == null || col == 0 || col == 1){
						continue;
					}
					String content = cell.toString();
					if(!content.equals("") && !content.equals("null")){
						colMap.put(col+"", content);
						if(col != lastCellNum-1){
							System.out.print("\""+content+"\",");
						}else {
							System.out.print("\""+content+"?random="+random.nextInt(1000)+"\");");
						}
					}else if(content.equals("null")){
						if( col != lastCellNum-1 ){
							System.out.print("NULL,");
						}else {
							System.out.print("NULL)");
						}
					}else {
						//System.out.println(cell.toString());
						String string = colMap.get(col+"");
						if(string == null ||string == ""){
							if(col != lastCellNum-1 ){
								System.out.print("\" \",");
							}else {
								System.out.print("\"\");");
							}
						}else {
							if(col != lastCellNum-1 ){
								System.out.print("\""+string+"\",");
							}else {
								System.out.print("\""+string+"\");");
							}
						}
					}
				}
				//System.out.println("------便利第"+rowNum+"行结束------");
			}
		}
	}
}
