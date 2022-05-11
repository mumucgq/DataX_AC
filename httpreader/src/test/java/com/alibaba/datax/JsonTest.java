package com.alibaba.datax;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.ac.datax.plugin.reader.httpreader.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * @author: 敖炽
 * @description：
 * @date: 2022-04-11 11:18
 **/
public class JsonTest {

    @Test
    public void testJson() {
        String queryParm = "{\n" +
                "\"access_token\":\"xxxxxxxxxxxxxxxxxxxx\",\n" +
                "“name”:\"张三\"\n" +
                "}";
        Configuration conf = Configuration.from(queryParm);
        String key = "param.page";
        int value = 2;
        conf.set(key,value);

        String s =  conf.toString();
        System.out.println(s);
        JSONObject jsonObject = JSON.parseObject(queryParm);
        Object object = jsonObject.put(key, value);
        System.out.println(object);
    }

    @Test
    public void testJSONPAth() {
        String result = "{\"code\":0,\"data\":[{\"bizId\":\"1438766551301083139\",\"fileName\":\"hello.jar\",\"fileUniqueId\":\"744f385c-9038-3670-9cb3-599616168f9c\",\"id\":\"1440566207203143685\",\"name\":\"hellobbb\",\"parentFolderId\":\"1438768264351305755\",\"path\":\"/udf/sprak/jar\",\"size\":\"431043\",\"submitStatus\":0,\"tenantId\":\"1\",\"type\":2,\"workspaceId\":\"1438766472884375554\"}],\"msg\":\"成功\",\"success\":true}";
        Object root = JSON.parse(result);
        Object id = JSONPath.eval(root, "data[0].id");
        System.out.println(id);
    }


    @Test
    public void testGetDataFromJosnPath () {
        String response = "{\n" +
                "    \"code\": 0,\n" +
                "    \"success\": true,\n" +
                "    \"msg\": \"成功\",\n" +
                "    \"data\": {\n" +
                "        \"pageNum\": 1,\n" +
                "        \"pageSize\": 3,\n" +
                "        \"totalSize\": 15,\n" +
                "        \"totalPage\": 5,\n" +
                "        \"data\": [\n" +
                "            {\n" +
                "                \"id\": \"1440661693716959336\",\n" +
                "                \"refId\": \"1458040646236893238\",\n" +
                "                \"name\": \"dataflow_25_test\",\n" +
                "                \"publishType\": 4,\n" +
                "                \"publishObjectType\": \"OFFLINE_SYNC\",\n" +
                "                \"changeType\": 0,\n" +
                "                \"releasedId\": 0,\n" +
                "                \"version\": \"1\",\n" +
                "                \"commitUser\": \"未知\",\n" +
                "                \"commitTime\": \"0\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"1440661693716959335\",\n" +
                "                \"refId\": \"1458040646236893210\",\n" +
                "                \"name\": \"spark-sql-test\",\n" +
                "                \"publishType\": 4,\n" +
                "                \"publishObjectType\": \"SPARK_SQL\",\n" +
                "                \"changeType\": 1,\n" +
                "                \"releasedId\": 0,\n" +
                "                \"version\": \"3\",\n" +
                "                \"commitUser\": \"未知\",\n" +
                "                \"commitTime\": \"0\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": \"1440661693716959334\",\n" +
                "                \"refId\": \"1458040646236893209\",\n" +
                "                \"name\": \"c-3\",\n" +
                "                \"publishType\": 4,\n" +
                "                \"publishObjectType\": \"SPARK_SQL\",\n" +
                "                \"changeType\": 0,\n" +
                "                \"releasedId\": 0,\n" +
                "                \"version\": \"1\",\n" +
                "                \"commitUser\": \"未知\",\n" +
                "                \"commitTime\": \"0\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        String jsonPath = "data.data";
        String content = HttpClientUtil.getContentByJsonPath(response, jsonPath);
        System.out.println(content);
        JSONArray jsonArray = null;
        String dataMode = "multiData";
        if (StringUtils.equalsIgnoreCase("multiData", dataMode)) {
            jsonArray = JSON.parseArray(content);
        } else if (StringUtils.equalsIgnoreCase("oneData", dataMode)) {
            jsonArray = new JSONArray();
            jsonArray.add( JSONObject.parseObject(content));
        }
        System.out.println("jsonArray:" + jsonArray);
    }


    @Test
    public void testGetOneDataFromJsonPath() {
        String response = "{\n" +
                "    \"code\": 0,\n" +
                "    \"success\": true,\n" +
                "    \"msg\": \"成功\",\n" +
                "    \"data\": {\n" +
                "        \"id\": \"1440566207203143688\",\n" +
                "        \"bizId\": \"1438766551301083139\",\n" +
                "        \"parentFolderId\": \"1438768264351305755\",\n" +
                "        \"name\": \"testHello2.jar\",\n" +
                "        \"path\": \"/test/hello\",\n" +
                "        \"type\": 2,\n" +
                "        \"size\": \"2796\",\n" +
                "        \"tenantId\": \"1\",\n" +
                "        \"workspaceId\": \"1438766472884375554\",\n" +
                "        \"fileName\": \"java-test-1.0-SNAPSHOT.jar\",\n" +
                "        \"fileUniqueId\": \"3db98965-31ee-36e3-af7d-9136f9fabc38\",\n" +
                "        \"submitStatus\": 0\n" +
                "    }\n" +
                "}";
        String jsonPath = "data";
        String content = HttpClientUtil.getContentByJsonPath(response, jsonPath);
        String dataMode = "oneData";
        JSONArray jsonArray = null;
        if (StringUtils.equalsIgnoreCase("multiData", dataMode)) {
            jsonArray = JSON.parseArray(content);
        } else if (StringUtils.equalsIgnoreCase("oneData", dataMode)) {
            jsonArray = new JSONArray();
            jsonArray.add( JSONObject.parseObject(content));
        }
        System.out.println("jsonArray:" + jsonArray);
    }
}
