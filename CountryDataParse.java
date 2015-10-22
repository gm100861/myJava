package com.plusnet.search.core.dal.daointerface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.Test;

import com.plusnet.search.core.dal.dataobject.CountryDO;
import com.plusnet.search.core.dal.dataobject.RegionDO;
import com.plusnet.search.core.dal.ibatis.NoSpringTest;

public class CountryTest extends BaseDaoTestCase{

    @Resource
    private CountryDAO countryDao;
    
    @Resource
    private RegionDAO regionDao;
    
   @Test
   public void countryInsert() throws IOException{
        try {
            AbstractConfiguration.setDefaultListDelimiter('-');
            XMLConfiguration configuration = new XMLConfiguration("e:/files/LocList.xml");
            List<Object> list = configuration.getList("CountryRegion[@Name]");
            System.out.println(list.size());
            List<CountryDO> countrys = new ArrayList<CountryDO>();
            List<RegionDO> regions = new ArrayList<RegionDO>();
            for(int i = 0 ; i < list.size() ; i++){
                //国家
                CountryDO country = new CountryDO();
                RegionDO region = new RegionDO();
                country.setChineseName(list.get(i).toString());
                country.setCountryCode(configuration.getString("CountryRegion("+i+")[@Code]"));
                countrys.add(country);
                if(StringUtils.isBlank(configuration.getString("CountryRegion("+i+").State[@Name]"))){
                    List<Object> citys = configuration.getList("CountryRegion("+i+").State.City[@Name]");
                    for (int q = 0 ; q < citys.size(); q++) {
                        region.setCode(country.getCountryCode()+"-"+configuration.getString("CountryRegion("+i+").State("+q+").City[@Code]"));
                        region.setRegionNameC(citys.get(q).toString());
                        region.setUpperRegion(country.getCountryCode());
                    }
                }else {
                    List<Object> citys = configuration.getList("CountryRegion("+i+").State[@Name]");
                    for (int c = 0 ; c < citys.size() ; c++) {
                        RegionDO privoce = new RegionDO();
                        privoce.setCode(country.getCountryCode()+"-"+configuration.getString("CountryRegion("+i+").State("+c+")[@Code]"));
                        privoce.setCountryCode(country.getCountryCode());
                        privoce.setRegionNameC(configuration.getString("CountryRegion("+i+").State("+c+")[@Name]"));
                        privoce.setUpperRegion(country.getCountryCode());
                        regions.add(privoce);
                        List<Object> list2 = configuration.getList("CountryRegion("+i+").State("+c+").City[@Name]");
                        for(int l = 0 ; l < list2.size() ; l++){
                            RegionDO subProvice = new RegionDO();
                            subProvice.setCode(privoce.getCode()+"-"+configuration.getString("CountryRegion("+i+").State("+c+").City("+l+")[@Code]"));
                            subProvice.setCountryCode(country.getCountryCode());
                            subProvice.setRegionNameC(configuration.getString("CountryRegion("+i+").State("+c+").City("+l+")[@Name]"));
                            subProvice.setUpperRegion(privoce.getCode());
                            regions.add(subProvice);
                        }
                    }
                }
            }
            FileWriter writer = new FileWriter("e:/files/country.txt");
            for (int i = 0; i < countrys.size(); i++) {
                writer.write(countrys.get(i).toString()+"\r\n");
            }
            writer.flush();
            writer.close();
            FileWriter pwriter = new FileWriter("e:/files/provice.txt");
            for (int i = 0; i < regions.size(); i++) {
                pwriter.write(regions.get(i).toString()+"\r\n");
            }
            pwriter.flush();
            pwriter.close();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }
    
    public void chinaAreaCityTestInsert() throws IOException{
        List<RegionDO> regions = new ArrayList<RegionDO>();
        String pathname = "E:/files/全国行政/全国行政/2013最新全国街道乡镇级以上行政区划代码表.xls";
        File file = new File(pathname);
        //FileWriter writer = new FileWriter(new File("e:/sqls_content_source.txt"));
        InputStream in = new FileInputStream(file);
        //得到整个excel对象
        HSSFWorkbook excel = new HSSFWorkbook(in);
        //获取整个excel有多少个sheet
        int sheets = excel.getNumberOfSheets();
        //便利第一个sheet
        Map<String,String> colMap = new HashMap<String, String>();
        for(int i = 0 ; i < sheets ; i++ ){
            HSSFSheet sheet = excel.getSheetAt(i);
            if(sheet == null){
                continue;
            }
            //便利每一行
            int lastRowNum = sheet.getLastRowNum();
            for( int rowNum = 1 ; rowNum <= lastRowNum ; rowNum++ ){
                HSSFRow row = sheet.getRow(rowNum);
                if(row == null || row.getCell(0) == null){
                    continue;
                }
                RegionDO rdo = new RegionDO();
                rdo.setCode(row.getCell(0).toString());
                rdo.setLevel(row.getCell(3).toString());
                rdo.setRegionNameC(row.getCell(2).toString());
                rdo.setRegionNameE(NoSpringTest.converterToSpell(row.getCell(2).toString()));
                rdo.setUpperRegion(row.getCell(1).toString());
                regions.add(rdo);
            }
        }
        //入库
        for (RegionDO region : regions) {
            regionDao.insert(region);
        }
    }
}
