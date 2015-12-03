import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.plusnet.search.img.service.ImageProxyService;
import com.plusnet.search.img.vo.DownloadResponseVO;
import com.plusnet.search.img.vo.ImageRequestVO;
import com.plusnet.search.img.vo.ImageResponseCode;

/**
 * 代理图片访问,解决防盗链问题
 * @author gm100861
 *
 */
@Controller
public class ImageProxyAction {


    private Logger logger = LoggerFactory.getLogger(DownloadAction.class);
    
    private static final Integer URL_TIMEOUT = 5*1000;
    
    /** 图片有效期 (3年) */
    private long expiry = 1000 * 60 * 60 * 24 * 30 * 36;
    
    @Autowired
    private ImageProxyService imageProxyService;
    
    @RequestMapping(value = { "/getPicsAddress" })
    @ResponseBody
    public DownloadResponseVO view(@RequestBody ImageRequestVO imgRequest, HttpServletRequest request,HttpServletResponse response)
            {
        DownloadResponseVO vo = new DownloadResponseVO();
        try {
            Map<String, String> urls = imgRequest.getUrls();
            Map<String,String> result = imageProxyService.generationProxyImageUrls(urls);
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
    
    /**
     * 代理图片请求
     * @param request
     * @param response
     */
    @RequestMapping(value = "/proxyImg" )
    public void proxyImg(HttpServletRequest request,HttpServletResponse response){
        long lastModifiedTimestamp = System.currentTimeMillis();
        InputStream inputStream = null;
        ServletOutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            String url = request.getParameter("redirectURI");
            if(StringUtils.isNotBlank(url)){
                
                URL u = new URL(url);  
                // 打开连接  
                URLConnection con = u.openConnection();  
                //设置请求超时为5s  
                con.setConnectTimeout(URL_TIMEOUT);  
                // 输入流  
                inputStream = con.getInputStream();
                byte tmp[] = new byte[256];
                int i = 0;
                while ((i = inputStream.read(tmp)) != -1) {
                    outputStream.write(tmp, 0, i);
                }
                
                response.setDateHeader("Last-Modified", lastModifiedTimestamp);
                response.setHeader("Cache-Control", "max-age="+ expiry/1000l); // HTTP 1.1
                response.setDateHeader("Expires", lastModifiedTimestamp +  expiry);
                response.setHeader("Content-Type", "image/png");
                outputStream.flush();
            }
            
        } catch (IOException e) {
            if(logger.isErrorEnabled()){
                logger.error("代理图片服务器代理图片请求时执行异常:{}-{}",e,e.getMessage());
            }
        } finally {
            //关闭流
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    inputStream = null;
                }
            }
            if(outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    outputStream = null;
                }
            }
        }
    }
}
