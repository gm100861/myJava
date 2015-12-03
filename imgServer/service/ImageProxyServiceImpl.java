
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.plusnet.search.img.service.ImageProxyService;
@Service
public class ImageProxyServiceImpl implements ImageProxyService{

    @Value("${baseDomain}")
    private String baseDomain;
    
    @Value("${imgServers}")
    private String imgServers;
    
    @Value("${projectName}")
    private String projectName;
    
    private List<String> serverLists = new ArrayList<String>();
    
    @Override
    public Map<String, String> generationProxyImageUrls(Map<String, String> urls) {
        Map<String, String> resultMap = new HashMap<String, String>();
        for(Entry entry : urls.entrySet()){
            String url = entry.getValue().toString();
            String servers = getServers();
            resultMap.put(entry.getKey().toString(), servers.concat("/img-web/proxyImg.do?redirectURI=").concat(url));
        }
        return resultMap;
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
