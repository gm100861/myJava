package org.linuxsogood.spider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class SexyFaceksSpider{
	
	private static final String base_dir = "d:/pics/";
	private static final String base_url = "http://sexy.faceks.com/";
	
	public static void main(String[] args) throws IOException {
		Document document = Jsoup.connect(base_url).get();
		Elements elements = document.getElementsByClass("m-post-img");
		Map<String, String> map  = getAlbumNameAndUrl(elements.toString());
		for(Entry<String, String> entry: map.entrySet()){
			Elements ele = Jsoup.connect(entry.getValue()).get().getElementsByClass("img");
			saveSubpageImages(ele,entry.getKey());
		}
	}


	/**
	 *
	 * @description 获取子页面的所有图片并保存
	 * @param elements2 
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@SuppressWarnings("deprecation")
	private static void saveSubpageImages(Elements elements2,String dirName)
			throws IOException, ClientProtocolException {
		for(int j = 0 ; j < elements2.size() ; j++){
			String url = elements2.get(j).attr("bigimgsrc");
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
			HttpResponse response = client.execute(get);
			
			int code = response.getStatusLine().getStatusCode();
			if(code == 200 || code == 304){
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				File dir = new File(base_dir+dirName);
				if(!dir.exists()){
					dir.mkdir();
				}
				FileUtils.copyInputStreamToFile(content, new File(base_dir+dirName+"/"+getFilename(url)));
			}
		}
	}
	
	/**
	 *
	 * @description 根据网站首页的Document获取每一个专辑的名字和专辑的详情地址
	 * @param html
	 * @return
	 */
	public static Map<String,String> getAlbumNameAndUrl(String html){
		Map<String,String> album = new HashMap<String, String>();
		Document document = Jsoup.parse(html);
		
		Elements elements = document.getElementsByClass("m-post-img");
		for (int i = 0; i < elements.size(); i++) {
			Document parse = Jsoup.parse(elements.get(i).toString());
			
			String value = parse.getElementsByClass("img").attr("href");
			String key = parse.getElementsByTag("p").get(0).text();
			album.put(key, value);
		}
		return album;
	}
	
	/**
	 * @description 从URL中提取中文件的名字
	 * @param url
	 * @return
	 */
	public static String getFilename(String url){
		String regEx = "img/(\\w+.jpg)";
		Pattern pat = Pattern.compile(regEx);
		Matcher mat = pat.matcher(url);
		while(mat.find()){
			return mat.group(1);
		}
		return System.currentTimeMillis()+"filenameisnotfound.jpg";
	}
}
