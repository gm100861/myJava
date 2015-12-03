
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.plusnet.search.img.service.DownloadService;

@Service
public class DownloadServiceImpl implements DownloadService{

    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    //连接超时时间
    private static final Integer CONNECTION_TIMEOUT = 2000;
    
    @Value("${downloadPath}")
    private String downloadPath;
    
    @Value("${errorPicsAddress}")
    private String errorPicsAddress;
    
    @Value("${baseDomain}")
    private String baseDomain;
    
    @Value("${imgServers}")
    private String imgServers;
    
    private List<String> serverLists = new ArrayList<String>();
    
    @Override
    public Map<String, String> download(Map<String, String> urls) {
        Map<String, String> result = new HashMap<String, String>();
        for (Entry entry : urls.entrySet()) {
            String url = entry.getValue().toString();
            String file = generRandomFile(downloadPath,url);
            boolean saveResult = saveImages(url,file);
            if(saveResult &&  file != null){
                result.put(entry.getKey().toString(), getServers()+file);
            }else {
                result.put(entry.getKey().toString(), errorPicsAddress);
            }
        }
        return result;
    }
    
    @SuppressWarnings({ "deprecation", "resource" })
    private boolean saveImages(String url,String filename) {
        try {
            if(StringUtils.isBlank(url)){
                return false;
            }
            url = replaceUrl(url);
            if(url == null){
                return false;
            }
            HttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get);
            
            int code = response.getStatusLine().getStatusCode();
            if(code == 200 || code == 304){
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                FileUtils.copyInputStreamToFile(content, new File(downloadPath+filename));
                return true;
            }
        } catch (ClientProtocolException e) {
            if(log.isErrorEnabled()){
                log.error("下载图片客户端池异常:{}-{}",e,e.getMessage());
            }
        } catch (IllegalStateException e) {
            if(log.isErrorEnabled()){
            log.error("下载图片时程序异常:{}-{}",e,e.getMessage());
            }
        } catch (IOException e) {
            if(log.isErrorEnabled()){
                log.error("下载图片时程序异常:{}-{}",e,e.getMessage());
            }
        }
        return false;
    }
    
    /**
     * @description 从URL中提取中文件的后缀格式
     * @param url
     * @return
     */
    public String getFilExt(String url){
        String regEx = "wx_fmt=(\\w+)?&?";
        Pattern pat = Pattern.compile(regEx);
        Matcher mat = pat.matcher(url);
        while(mat.find()){
            return mat.group(1);
        }
        return null;
    }
    
    /**
     * 
     * @param url
     * @return
     */
    private String replaceUrl(String url){
        String regEx = "(http.+wx_fmt=\\w+)";
        Pattern pat = Pattern.compile(regEx);
        Matcher mat = pat.matcher(url);
        while(mat.find()){
            return mat.group(1);
        }
        return null;
    }
    
    /**
     *  生成随机目录加文件名,如果目录不存在则创建
     * @return
     */
    private String generRandomFile(String savePath,String url) {
        String str = UUID.randomUUID().toString().replace("-", "");
        try {
            String date = formatDate(new Date(), "yyyyMMdd");
            String firstPath = str.substring(0, 2);
            String lastPath = str.substring(str.length()-2);
            File file = new File(savePath+"/"+date+"/"+firstPath+"/"+lastPath);
            if(!file.exists()){
                file.mkdirs();
            }
            String filExt = getFilExt(url);
            if(filExt == null){
                return null;
            }
            String result = "/"+date+"/"+firstPath+"/"+lastPath+"/"+str+"."+filExt;
            return result;
        } catch (Exception e) {
            log.error("{}在执行generRandomDir方法时异常:{}-{}",getClass(),e,e.getMessage());
            return null;
        }
    }
    
    private String formatDate(Date date,String format){
        DateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }
    
    /**
     * 随机返回域名
     * @return
     */
    private String getServers(){
        if(serverLists.size() == 0){
            String[] subdomains = imgServers.split(",");
            for (int i = 0; i < subdomains.length; i++) {
                serverLists.add("http://".concat(subdomains[i]).concat(".").concat(baseDomain));
            }
        }
        Random random = new Random();
        int randomInt = Math.abs(random.nextInt())%serverLists.size();
        return serverLists.get(randomInt);
    }
}
