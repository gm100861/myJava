package com.plusnet.search.img.action;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.plusnet.search.img.service.DownloadService;
import com.plusnet.search.img.vo.DownloadResponseVO;
import com.plusnet.search.img.vo.ImageRequestVO;
import com.plusnet.search.img.vo.ImageResponseCode;

/**
 * 下载图片到本地,自己提供图片访问服务,解决防盗链问题.
 * @author pc117
 *
 */
@Controller
public class DownloadAction  {
    private Logger logger = LoggerFactory.getLogger(DownloadAction.class);

    @Autowired
    private DownloadService downloadService;
    
    @RequestMapping(value = { "/downloadWXPics" })
    @ResponseBody
    public DownloadResponseVO view(@RequestBody ImageRequestVO imgRequest, HttpServletRequest request,HttpServletResponse response)
            {
        DownloadResponseVO vo = new DownloadResponseVO();
        try {
            Map<String, String> urls = imgRequest.getUrls();
            Map<String,String> result = downloadService.download(urls);
            vo.setUrls(result);
            vo.setCode(ImageResponseCode.OK);
            vo.setMsg("download ok");
            response.addHeader("Access-Control-Allow-Origin", "*");
            return vo;
        } catch (Exception e) {
            if(logger.isErrorEnabled()){
                logger.error("执行异常,异常信息:{}-{}",e,e.getMessage());
            }
        }
        vo.setMsg("may be exception");
        vo.setCode(ImageResponseCode.EXCEPTION);
        return vo;
    }
}
