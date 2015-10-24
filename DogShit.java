package com.plusnet.search.engine.task;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.plusnet.common.core.engine.lock.Lock;
import com.plusnet.common.core.engine.lock.LockException;
import com.plusnet.common.core.engine.lock.LockService;
import com.plusnet.common.core.engine.lock.enums.LockType;
import com.plusnet.common.core.engine.schedule.enums.DaemonTaskType;
import com.plusnet.common.core.engine.schedule.task.AbstractDaemonTask;
import com.plusnet.search.client.ElasticSearchHandler;
import com.plusnet.search.content.domain.ContentSourceTO;
import com.plusnet.search.core.dal.daointerface.ArrangementDAO;
import com.plusnet.search.core.dal.daointerface.BehaviorDAO;
import com.plusnet.search.core.dal.daointerface.HotelDAO;
import com.plusnet.search.core.dal.daointerface.RegionDAO;
import com.plusnet.search.core.dal.daointerface.TourDAO;
import com.plusnet.search.core.dal.dataobject.ArrangementDO;
import com.plusnet.search.core.dal.dataobject.BehaviorDO;
import com.plusnet.search.core.dal.dataobject.HotelDO;
import com.plusnet.search.core.dal.dataobject.TourDO;
import com.plusnet.search.manage.ContentSourceManage;
import com.plusnet.search.template.HtmlHelper;
import com.plusnet.search.util.FileUtil;
import com.plusnet.search.util.JsonUtil;

@Component("dogShitIndexTask")
public class DogShitTask extends AbstractDaemonTask{

    private static DaemonTaskType TRAVEL_INDEX_TASK = new DaemonTaskType("travelIndex", "旅游索引任务");
    
    private static final Logger log = LoggerFactory.getLogger(TravelTask.class);
    
    private static final String TRAVEL_CACHE_PREFIX = "travelOffset";
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Autowired
    private BehaviorDAO behaviorDAO;
    
    @Autowired
    private HotelDAO hotelDao;
    
    @Autowired
    private ArrangementDAO arrangementDao;
    
    @Autowired
    private RegionDAO regionDao;
    
    @Autowired
    private TourDAO tourDao;
    
    @Autowired
    private ContentSourceManage contentSourceManage;
    
    @Autowired
    private LockService lockService;
    
    private Lock lock;
    
    @Resource(name = "searchHandler")
    private ElasticSearchHandler es;
    
    @Value("${templateFilePath}")
    private String templateFilePath;
    
    @Value("${savePath}")
    private String savePath;
    
    @Override
    public DaemonTaskType getTaskType() {
        return TRAVEL_INDEX_TASK;
    }
    
    

    public TravelTask() {
        super();
        batchSize = 500;
        lock = new Lock();
        lock.setLockKey("TRAVEL_INDEX_TASK");
        lock.setLockName("travelIndexLock");
        lock.setLockSecond(10);
        lock.setLockType(LockType.EXCLUSION);
    }



    @Override
    protected List<BehaviorDO> loadTask(long start, int batchSize) {
        //加载任务 从数据库中不断获取记录
        final int b = batchSize;
        while(true){
            try {
                log.info("TravelIndexTask开始加载任务数据");
                List<BehaviorDO> behaviors = lockService.excLock(lock, new Callable<List<BehaviorDO>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public List<BehaviorDO> call() throws Exception {
                        int offset = 0;
                        try {
                            Object object = redisTemplate.boundValueOps(TRAVEL_CACHE_PREFIX).get();
                            if(object != null){
                                offset = Integer.parseInt(object.toString());
                            }
                            List<BehaviorDO> result = behaviorDAO.pageQuery(b,offset);
                            if(result == null || result.size() == 0){
                                return null;
                            }
                            redisTemplate.boundValueOps(TRAVEL_CACHE_PREFIX).set(offset+result.size());
                            return result;
                        } catch(Exception e) {
                            logger.error("加载任务出错，偏移量:{},批量值:{}",offset,b,e);
                            throw e;
                        }
                    }
                });
                return behaviors;
            } catch (LockException e) {
                if(log.isErrorEnabled()){
                    log.error("MusicIndexTask在加载数据的时候执行异常:{},异常原因:{}",e,e.getMessage());
                }
            }
        }
    }

    @Override
    protected boolean executeTask(Object domain) {
        try {
            log.debug("旅游调度任务开始执行,首先数据入库");
            //创建要保存数据的对象
            BehaviorDO behavior = (BehaviorDO) domain;
            TourDO tour = new TourDO(); //旅游行程
            ArrangementDO arrangement = new ArrangementDO();    //行程安排
            HotelDO hotel = new HotelDO(); //酒店
            
            try {
                //数据入库
                Integer tourId = insertTour(behavior, tour);
                Integer hotelId = insertHotel(behavior, hotel);
                insertArrangement(behavior, arrangement, tourId, hotelId);
               
            } catch (Exception e) {
                log.error("旅游调度任务executeTask方法在执行的时候异常:{}-{}",e,e.getMessage());
                e.printStackTrace();
                return false;
            }
            log.debug("旅游调度任务开始执行,数据入库完成");
            //生成html
            // 模板文件
            String filePath=generTravelFilename(savePath);
            log.debug("生成HTML文件:{}",filePath);
            String modFile = templateFilePath;
            String htmlContent = new HtmlHelper().generateHtmlByTemplate(modFile+"/article.html", behavior.getTitle(), behavior.getNavigatModule() == null ? "":behavior.getNavigatModule().split(";")[0] ,
                    behavior.getContent());
            File file = new File(savePath);
            FileUtil.createFile(savePath+filePath, htmlContent);
            log.debug("生成HTML文件完成:{}",filePath);
            log.debug("将数据转换成JSON格式,准备创建索引");
            String jsonData = JsonUtil.behaviorToJson(behavior,tour.getSourceId(),tour.getid(),filePath);
            List<String> jsonList = new ArrayList<String>();
            jsonList.add(jsonData);
            es.addIndexData("article", jsonList);
            log.debug("索引创建完成");
            return true;
        } catch (Exception e) {
            if(log.isErrorEnabled()){
                log.error("TravelIndexTask任务执行异常:{},{}",e,e.getMessage());
            }
            e.printStackTrace();
        }
        return false;
    }

    /**
     *  生成随机目录加文件名,如果目录不存在则创建
     * @return
     */
    private String generTravelFilename(String savePath) {
        String str = UUID.randomUUID().toString().replace("-", "");
        try {
            String firstPath = str.substring(0, 2);
            String lastPath = str.substring(str.length()-2);
            File file = new File(savePath+"/travel/"+firstPath+"/"+lastPath);
            if(!file.exists()){
                file.mkdirs();
            }
            return "/travel/"+firstPath+"/"+lastPath+"/"+str+".html";
        } catch (Exception e) {
            log.error("旅游调度任务在执行generTravelFilename方法时异常:{}-{}",e,e.getMessage());
            return savePath+"/error-"+str+".html";
        }
    }



    /**
     * 行程相关信息入库
     * @param behavior
     * @param arrangement
     * @param tourId
     * @param hotelId
     */
    private void insertArrangement(BehaviorDO behavior, ArrangementDO arrangement,
            Integer tourId, Integer hotelId) {
        arrangement.setAccommondation(hotelId);  //hotile入库处理后的id
        arrangement.setAttractionPic(subSightsUrl(behavior.getSightsPhotosUrl()));
        arrangement.setFromDay(parseNumberDay(behavior.getNumberDays()));
        arrangement.setHotelPic(behavior.getAccommodationPootosUrl());
        arrangement.setPlace(behavior.getDestination());
        arrangement.setTradeDate(parseDate(behavior.getCurrDate()));
        arrangement.setTravelNo(tourId); //行程表中的ID
        arrangementDao.insert(arrangement);
    }

    /**
     * 截取URL地址,防止出现过长的情况,只保留前1个
     * @param sightsPhotosUrl
     * @return
     */
    private String subSightsUrl(String sightsPhotosUrl) {
        if(StringUtils.isBlank(sightsPhotosUrl)){
            return null;
        }
        if(sightsPhotosUrl.length() < 500){
            return sightsPhotosUrl;
        }else {
            String[] split = sightsPhotosUrl.split(",");
            StringBuffer sb = new StringBuffer();
            if(split == null || split.length < 2){
                return sightsPhotosUrl.substring(0,500);
            }else {
                return split[0];
            }
        }
    }



    /**
     * 酒店相关信息入库
     * @param behavior
     * @param hotel
     * @return 返回入库后的酒店的id
     */
    private Integer insertHotel(BehaviorDO behavior, HotelDO hotel) {
        String hotelName = parseHotelInfo(behavior.getAccommodationName()) == null ? null :parseHotelInfo(behavior.getAccommodationName())[0];
        String hotelAddress = parseHotelInfo(behavior.getAccommodationName()) == null ? null :parseHotelInfo(behavior.getAccommodationName())[1];
        HotelDO dbResult = hotelDao.queryByNameAndAddress(hotelName,hotelAddress);
        if(dbResult == null){
            hotel.setName(hotelName);
            hotel.setAddress(hotelAddress);
            hotel.setPicture(behavior.getAccommodationPootosUrl());
            return hotelDao.insert(hotel);
        }
        return dbResult.getId();
    }



    /**
     * 入库保存tour,并返回插入之后的id
     * @param behavior
     * @param tour
     * @return
     */
    private Integer insertTour(BehaviorDO behavior, TourDO tour) {
        tour.setCover(behavior.getThemeImageUrl());
        tour.setCrawledTime(behavior.getDataTime());
        tour.setCrawledUrl(behavior.getSourceUrl());
        tour.setOutTime(covertStringToDate(behavior.getDepartureDate()));
        tour.setReleaser(behavior.getActivePromoter());
        tour.setSourceId(querySourceId(behavior));
        tour.setStartAddress(behavior.getStartAddress());
        tour.setTag(behavior.getTags());
        tour.setTitle(behavior.getTitle());
        tour.setTravelDays(parseTravelDays(behavior.getTripDays()));
        return tourDao.insert(tour);
    }

    /**
     * 将字符串日期转换为日期格式
     * @param currDate
     * @return
     */
    private Date parseDate(String currDate) {
        try {
            if(StringUtils.isBlank(currDate)){
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date parse = sdf.parse(currDate);
            return parse;
        } catch (Exception e) {
            log.error("旅游调度任务在执行parseDate方法时异常,要转换的日期为:{},异常信息:{}-{}",currDate,e,e.getMessage());
        }
        return null;
    }



    /**
     *  将字符串天转换为int天
     * @param numberDays 格式为 day30
     * @return 30
     */
    private Integer parseNumberDay(String numberDays) {
        try {
            if(StringUtils.isBlank(numberDays)){
                return -1;
            }
            String replace = numberDays.trim().replace("day", "");
            return Integer.parseInt(replace);
        } catch (Exception e) {
            log.error("旅游任务调试在执行parseNumberDay方法时异常:{}-{}-{}",e,e.getMessage(),numberDays);
        }
        return -2;
    }



    /**
     * 从给定的数据中拿到hotelName
     * @param accommodationName 格式为: 弗莱士酒店，Hotel Fresh，雅典
     * @return 返回弗莱士酒店
     */
    private String[] parseHotelInfo(String accommodationName) {
        try {
            if(StringUtils.isBlank(accommodationName)){
                return null;
            }
            String[] result = new String[2];
            String[] split = accommodationName.split("，");
            if(split == null){
                String[] split2 = accommodationName.split(",");
                if(split2 == null){
                    return null;
                }else if(split2.length == 1 || split2.length == 2){
                    result[0] = split2[0];
                    return result;
                }else if(split2.length == 3){
                    result[0] = split2[0];
                    result[1] = split2[2];
                    return result;
                }
            }else if(split.length == 1 || split.length == 2){
                result[0] = split[0];
                return result;
            }else if(split.length == 3){
                result[0] = split[0];
                result[1] = split[2];
                return result;
            }
        } catch (Exception e) {
            log.error("旅行任务调度在将转换酒店名字和地点时执行异常,要转换的信息为:{}",accommodationName);
            log.error("旅行任务调度在将转换酒店名字和地点时执行异常,异常信息:{}-{}",e,e.getMessage());
        }
        return null;
    }



    @Override
    protected void sendToMonitor(Object domain) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void monitorTotalCount(int totalCount) {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * 将日期转为整型
     * @param days 数据格式 :（共15天）    
     * @return 返回15
     */
    private Integer parseTravelDays(String days){
        if(StringUtils.isBlank(days)){
            return -1;
        }
        String replace = days.replaceAll("（|共|天|\\s|）", "");
        try {
            int day = Integer.parseInt(replace);
            return day;
        } catch (Exception e) {
            log.error("旅游调度任务在日期格式转换的时候执行异常,要转换的字符串为:{}",days);
            log.error("旅游调度任务在日期格式转换的时候执行异常,异常信息:{}-{}",e,e.getMessage());
            return -2;
        }
    }
    
    /**
     * 查询数据来源,首先根据model查询,如果查询不到再根据model_url查询
     * @param behavior
     * @return
     */
    private Integer querySourceId(BehaviorDO behavior){
        if(behavior == null){
            return -1;
        }
        ContentSourceTO contentSourceTO = null;
        try {
            String stationName = null;
            String typeCode = null;
            String editionName = null;
            String detailName = null;
            String url = null;
            String navigatModule = behavior.getNavigatModule();
            if(navigatModule == null | "".equals(navigatModule)){
                return -1;
            }
            String[] split = navigatModule.split(";");
            if(split.length == 0){
                return -1;
            } else if(split.length == 1){
                stationName = split[0];
            }else if(split.length == 2){
                stationName = split[0];
                typeCode = split[1];
            }else if(split.length == 3){
                stationName = split[0];
                typeCode = split[1];
                editionName = split[2];
            }else if(split.length == 4){
                stationName = split[0];
                typeCode = split[1];
                editionName = split[2];
                detailName = split[3];
            }else {
                return -1;
            }
            
            contentSourceTO = contentSourceManage.getSourceByStation(stationName, typeCode,editionName, detailName);
            if (contentSourceTO == null) {
                log.error("根据指定模块找不到数据来源,stationName:{},typeCode:{},editionName:{},detailName:{},尝试根据url查找",stationName,typeCode,editionName,detailName);
                contentSourceTO = contentSourceManage.loadByUrl(behavior.getNavigatModuleUrl());
                if(contentSourceTO == null){
                    log.error("根据URL查找不到数据来源:{}",behavior.getNavigatModuleUrl());
                    return -1;
                }
                return contentSourceTO.getId();
            }
            return contentSourceTO.getId();
        } catch (Exception e) {
            log.error("旅游调度任务在根据相关信息查找数据来源的时候执行异常:{}-{}",e,e.getMessage());
            return -2;
        }
    }
    
    /**
     *  将string类型的数据转为date格式
     * @param date  格式是: 出发日期：2015-07-26 
     * @return 返回 2015-07-26 
     */
    private Date covertStringToDate(String date){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if(StringUtils.isBlank(date)){
                return null;
            }
            String[] split = date.trim().split(":");
            if(split == null || split.length != 2){
                String[] split2 = date.split("：");
                if(split2 == null || split2.length != 2){
                    return null;
                }
                date = split2[1];
            }else {
                date = split[1];
            }
            Date d = sdf.parse(date);
            return d;
        } catch (Exception e) {
            try {
                log.error("yyyy-MM-dd格式转换{}日期失败,尝试使用yyyy-MM-dd HH:mm:ss格式转换",date);
                SimpleDateFormat esdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String[] split = date.trim().split(":");
                if(split == null || split.length != 2){
                    String[] split2 = date.split("：");
                    if(split2 == null || split2.length != 2){
                        return null;
                    }
                    date = split2[1];
                }else {
                    date = split[1];
                }
                Date d = esdf.parse(date);
                return d;
            } catch (ParseException e1) {
                log.error("yyyy-MM-dd HH:mm:ss格式转换{}日期失败",date);
                return null;
            }
        }
    }

}
