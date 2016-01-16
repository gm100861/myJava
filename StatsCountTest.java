package com.plusnet.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by honway on 2016/1/16.
 * Blog: linuxsogood.org
 */
public class StatsCountTest implements PageProcessor{
    String UA = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36";
    private Site site = Site.me().setUserAgent(UA).setTimeOut(2000).setCycleRetryTimes(2);

    public static void main(String[] args) {
        String url = "http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2013/index.html";
        Spider.create(new StatsCountTest()).addUrl(url).thread(10).addPipeline(new ConsolePipeline()).run();
    }

    @Override
    public void process(Page page) {
        Html html = page.getHtml();
        List<String> provincetable = html.css(".provincetable").links().all();
        //省市
        if(provincetable != null && provincetable.size() != 0){
            //TODO 处理省市
            List<String> allA = html.css(".provincetr>td>a").all();
            for (String a : allA) {
                String text = Jsoup.parse(a).getElementsByTag("a").text();
                String url = Jsoup.parse(a).getElementsByTag("a").attr("href").replaceAll(".+/(\\d{1,}).html","$1");
                page.putField("name",text);
                page.putField("code",url);
                //System.out.println("名称:"+text+",代码:"+url);
            }
            List<String> all = html.links().regex("^http://www.stats.gov.cn.+").all();
            page.addTargetRequests(all);
        }
        //处理城市
        List<String> citytable = html.css(".citytable").links().all();
        if(citytable != null && citytable.size() !=0 ){
            List<String> allTr = html.css(".citytr").all();
            for (String tr : allTr) {
                String cityCode = "";
                String cityName = "";
                Elements a = Jsoup.parse(tr).getElementsByTag("a");
                for (Element element : a) {
                    String text = element.text();
                    if(Pattern.compile("\\d+").matcher(text).find()){
                        cityCode = text;
                    }else {
                        cityName = text;
                    }
                }
                page.putField("name",cityName);
                page.putField("code",cityCode);
                //System.out.println("城市名:"+cityName+",编码:"+cityCode);
            }
            List<String> all = html.links().regex("^http://www.stats.gov.cn.+").all();
            page.addTargetRequests(all);
        }

        //处理城镇
        List<String> countytable = html.css(".countytable").links().all();
        if(countytable != null && countytable.size() !=0 ){
            List<String> allTr = html.css(".countytr").all();
            for (String tr : allTr) {
                String cityCode = "";
                String cityName = "";
                Elements a = Jsoup.parse(tr).getElementsByTag("a");
                for (Element element : a) {
                    String text = element.text();
                    if(Pattern.compile("\\d+").matcher(text).find()){
                        cityCode = text;
                    }else {
                        cityName = text;
                    }
                }
                page.putField("name",cityName);
                page.putField("code",cityCode);
                //System.out.println("城市名:"+cityName+",编码:"+cityCode);
            }
            List<String> all = html.links().regex("^http://www.stats.gov.cn.+").all();
            page.addTargetRequests(all);
        }

        //处理乡村
        List<String> towntable = html.css(".towntable").links().all();
        if(countytable != null && towntable.size() !=0 ){
            List<String> allTr = html.css(".towntr").all();
            for (String tr : allTr) {
                String cityCode = "";
                String cityName = "";
                Elements a = Jsoup.parse(tr).getElementsByTag("a");
                for (Element element : a) {
                    String text = element.text();
                    if(Pattern.compile("\\d+").matcher(text).find()){
                        cityCode = text;
                    }else {
                        cityName = text;
                    }
                }
                page.putField("name",cityName);
                page.putField("code",cityCode);
                //System.out.println("城市名:"+cityName+",编码:"+cityCode);
            }
            List<String> all = html.links().regex("^http://www.stats.gov.cn.+").all();
            page.addTargetRequests(all);
        }

        //处理乡村村
        List<String> villagetable = html.css(".villagetable").links().all();
        if(countytable != null && villagetable.size() !=0 ) {
            List<String> allTr = html.css(".villagetr").all();
            for (String tr : allTr) {
                String cityCode = "";
                String cityName = "";
                String cityType = "";

                //WARN 这里用jsoup处理只有td标签的html时,jsoup会把标签给去除掉,只留td里的内容.
                String val = Jsoup.parse(tr).body().text();
                String[] split = val.split(" ");
                if (split.length != 3) {
                    continue;
                }
                for (int i = 0; i < split.length; i++) {
                    if (Pattern.compile("\\d+").matcher(split[i]).find()) {
                        if (Pattern.compile("^\\d{3}$").matcher(split[i]).find()) {
                            cityType = split[i];
                        } else {
                            cityCode = split[i];
                        }
                    } else {
                        cityName = split[i];
                    }
                }
                page.putField("name",cityName);
                page.putField("code",cityCode);
                page.putField("type",cityType);
                //System.out.println("城市名:" + cityName + ",分类编码:" + cityType + ",编码:" + cityCode);
            }
            List<String> all = html.links().regex("^http://www.stats.gov.cn.+").all();
            page.addTargetRequests(all);
        }
    }

    @Override
    public Site getSite() {
        return this.site;
    }
}
