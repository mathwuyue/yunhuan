package com.yuyue.admin.web.controller;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;

public class HttpUtil {
    /**
     * 发送post请求
     * @param url  路径
     * @param jsonObject  参数(json类型)
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public static String post(String url, JSONObject jsonObject) throws ParseException, IOException {
        String body = "";

        //创建httpclient对象
        CloseableHttpClient client = HttpClients.createDefault();
        //创建post方式请求对象
        HttpPost httpPost = new HttpPost(url);

        //装填参数
        StringEntity s = new StringEntity(jsonObject.toString(), "utf-8");
        s.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
                "application/json"));
        //设置参数到请求对象中
        httpPost.setEntity(s);
//        System.out.println("请求地址："+url);
//        System.out.println("请求参数："+nvps.toString());

        //设置header信息
        //指定报文头【Content-type】、【User-Agent】
//        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

        //执行请求操作，并拿到结果（同步阻塞）
        CloseableHttpResponse response = client.execute(httpPost);
        //获取结果实体
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            //按指定编码转换结果实体为String类型
            body = EntityUtils.toString(entity, "UTF-8");
        }
        EntityUtils.consume(entity);
        //释放链接
        response.close();
        return body;
    }

    /**
     * 发送delete请求
     *
     * @param url
     * @param token
     * @param jsonStr
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static String delete(String url, String token, String jsonStr) {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000).setConnectionRequestTimeout(35000).setSocketTimeout(60000).build();
        httpDelete.setConfig(requestConfig);
        httpDelete.setHeader("Content-type", "application/json");
        httpDelete.setHeader("DataEncoding", "UTF-8");
        httpDelete.setHeader("token", token);

        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpDelete);
            HttpEntity entity = httpResponse.getEntity();
            String result = EntityUtils.toString(entity);
            return result;
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
//        将post请求参数写到map中（map中可以存map）
        HashMap<String, String> params = new HashMap<>();
        params.put("date", "20200630");
        params.put("typeId", "dsafa");
        HashMap<String, Object> map = new HashMap<>();
        map.put("key", "42423=q8");
        map.put("id", "42342352");
        map.put("pageNum", "0");
        map.put("pageSize", "1000");
        map.put("params", params);
//        将map转为json对象
        JSONObject json = new JSONObject(map);
        String  url = "http://dc-o.web.leiniao.com/operation/open/request";
        System.out.println(post(url, json));

    }
}

