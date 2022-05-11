package com.ac.datax.plugin.reader.httpreader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: 敖炽
 * @description：
 * @date: 2022-04-19 21:17
 **/
public class HttpClientUtil {

    private static Logger LOG = LoggerFactory.getLogger(HttpClientUtil.class);

    public static String executeRequest(String url, String requestMethod, String parameters,
                                        String headers, String jsonPath, AuthTypeEnum authType ,
                                        Map<String, String> authenticParam) throws URISyntaxException, IOException {
       return executeRequest(url, requestMethod, parameters, headers, jsonPath, authType, authenticParam, "UTF-8");
    }

    public static String executeRequest(String url, String requestMethod, String parameters,
                                        String headers, String jsonPath, AuthTypeEnum authType,
                                        Map<String, String> authenticParam, String encoding)
            throws URISyntaxException, IOException {
        URI uri = URI.create(url);
        authenticParam = authenticParam == null ? new HashMap<>() : authenticParam;
        String content = executeRequestCommon(uri, requestMethod, parameters, headers, authType, authenticParam, encoding);
        return getContentByJsonPath(content, jsonPath);
    }

    public static String executeRequestCommon(URI uri, String requestMethod, String parameters,
                                              String headers, AuthTypeEnum authType, Map<String, String> authenticParam, String encoding)
            throws IOException, URISyntaxException {
        // 创建客户端
        CloseableHttpClient httpclient = createCloseableHttpClient(authType, uri, authenticParam);
        // 创建请求
        HttpUriRequest uriRequest = createUriRequest(uri, headers, authType, authenticParam, requestMethod, parameters);
        // 执行
        CloseableHttpResponse response = httpclient.execute(uriRequest);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            response.getEntity().getContent().close();
            throw new IOException(statusLine.getReasonPhrase());
        }
        HttpEntity entity = response.getEntity();
        Charset charset = Charsets.toCharset(encoding);
        return EntityUtils.toString(entity, charset);
    }

    public static HttpUriRequest createUriRequest(URI uri, String headers, AuthTypeEnum authType, Map<String, String> authenticParam,
                                                  String requestMethod, String parameters) throws URISyntaxException, UnsupportedEncodingException {
        HttpUriRequest request = null;
        Map<String, Object> headerMap = JSONObject.parseObject(headers, Map.class);

        if (StringUtils.equals(MethodEnum.get.name(), requestMethod)) {
            if (StringUtils.isNotBlank(parameters)) {
                String query = uri.getQuery();
                query = StringUtils.isBlank(query) ? parameters : query + "&" + parameters;
                String url = uri.toString();
                int questionIndex = url.lastIndexOf("?");
                questionIndex = questionIndex < 0 ? url.length() : questionIndex;
                String newUrl =  uri.toString().substring(0, questionIndex) + "?" + query;
                uri = new URI(newUrl);
            }
            request = new HttpGet(uri);

        } else {
            // 创建post请求
            HttpPost post = new HttpPost(uri);
            if (StringUtils.isNotBlank(parameters)) {
                StringEntity entity = new StringEntity(parameters);
                entity.setContentType(ContentType.APPLICATION_JSON.toString());
                post.setEntity(entity);
            }
            request = post;
        }
        // 设置请求头
        HashMap<String, Object> allHeaderMap = new HashMap<>();
        String authToken = authenticParam.get(HttpConstant.AUTH_TOKEN);
        Map<String, Object> tokenMap = JSONObject.parseObject(authToken, Map.class);
        if (AuthTypeEnum.token.equals(authType) && MapUtils.isNotEmpty(tokenMap)) {
            allHeaderMap.putAll(tokenMap);
        }
        if (MapUtils.isNotEmpty(headerMap)) {
            allHeaderMap.putAll(headerMap);
        }
        String username = authenticParam.get(HttpConstant.AUTH_USERNAME);
        String password = authenticParam.get(HttpConstant.AUTH_PASSWORD);
        if (AuthTypeEnum.basic.equals(authType)  && StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            String auth = username + ":" + password;
            String encodedAuth = new Base64().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + encodedAuth;
            allHeaderMap.put(HttpHeaders.AUTHORIZATION, authHeader);
        }
        // 设置请求头
        if (MapUtils.isNotEmpty(allHeaderMap)) {
            for (Map.Entry<String, Object> header : allHeaderMap.entrySet()) {
                request.setHeader(header.getKey(), header.getValue().toString());
            }
        }
        return request;
    }

    public static String getContentByJsonPath(String responseContent, String jsonPath) {
        JSONObject contentRoot = null;
        try {
            contentRoot = JSON.parseObject(responseContent);
        } catch (Exception e) {
            String msg = String.format("响应体解析为json格式失败, 失败详情:%s", e.getMessage());
            LOG.error(msg);
            throw new RuntimeException(msg);
        }
        Object content = null;
        if (StringUtils.isBlank(jsonPath)) {
            content = contentRoot;
        } else {
            try {
                // 判断该路径是否存在
                if (!JSONPath.contains(contentRoot, jsonPath)) {
                    String msg = String.format("返回值中没有包含指定的json路径:%s", jsonPath);
                    LOG.error(msg);
                    throw new RuntimeException(msg);
                }
                content = JSONPath.eval(contentRoot, jsonPath);
            } catch (Exception e) {
                String msg = String.format("响应体根据指定的数据存储json路径获取数据失败, 失败详情为:%s", e.getMessage());
                LOG.error(msg);
                throw new RuntimeException(msg);
            }
        }
        return content == null ? "" : content.toString();
    }


    /**
     * 创建httpClient客户端
     * @param authType
     * @param uri
     * @param authenticParam
     * @return
     */

    public static CloseableHttpClient createCloseableHttpClient(AuthTypeEnum authType, URI uri, Map<String, String> authenticParam) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        return httpClientBuilder.build();
    }

    public static Map<String, String> getMapFromQueryParam(String queryParam) {
        List<String> queryList = Arrays.asList(queryParam.split("&"));
        Map<String, String> paramMap = null;
        if (CollectionUtils.isNotEmpty(queryList)) {
            paramMap = queryList.stream().filter(param -> param.contains("="))
                    .map(param -> {
                String[] split = param.split("=");
                return Pair.of(split[0], split[1]);
            }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        }
        return paramMap;
    }


}
