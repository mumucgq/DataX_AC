/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ac.datax.plugin.reader.httpreader;

import com.alibaba.datax.common.element.BoolColumn;
import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.DateColumn;
import com.alibaba.datax.common.element.DoubleColumn;
import com.alibaba.datax.common.element.LongColumn;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.unstructuredstorage.reader.Key;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpReader
        extends Reader
{
    public static class Job
            extends Reader.Job {
        private static Logger LOG = LoggerFactory.getLogger(Reader.Job.class);

        private Configuration readerOriginConfig = null;

        @Override
        public void init() {
            // 获取httpreader插件配置
            this.readerOriginConfig = this.getPluginJobConf();
            // 校验参数
            validateParameter();
        }

        @Override
        public void destroy() {

        }

        @Override
        public List<Configuration> split(int adviceNumber) {
            List<Configuration> result = new ArrayList<>();
            result.add(this.readerOriginConfig);
            return result;
        }

        public void validateParameter() {
            // 获取请求方式
            String method = readerOriginConfig.getString(HttpKey.METHOD);
            Configuration connection = readerOriginConfig.getConfiguration(HttpKey.CONNECTION);
//            String resultType = readerOriginConfig.getString(HttpKey.RESULT_TYPE);
            String requestTimes = readerOriginConfig.getString(HttpKey.REQUEST_TIMES);

            if (StringUtils.isBlank(method)) {
                throw DataXException.asDataXException(
                        HttpReaderErrorCode.REQUIRED_VALUE,
                        "请求方式 [" + HttpKey.METHOD + "] 为必填项.");
            }
            if (!MethodEnum.isExistByName(method)) {
                throw DataXException.asDataXException(
                        HttpReaderErrorCode.ILLEGAL_VALUE,
                        "不支持的请求方式: [" + method + "] "
                );
            }

//            // 检查返回结果的数据格式
//            if (StringUtils.isBlank(resultType)) {
//                throw DataXException.asDataXException(
//                        HttpReaderErrorCode.REQUIRED_VALUE,
//                        "The parameter [" + HttpKey.RESULT_TYPE + "] is not set.");
//            } else {
//                if (!StringUtils.equalsIgnoreCase("json", resultType)) {
//                    throw DataXException.asDataXException(
//                            HttpReaderErrorCode.ILLEGAL_VALUE, "不支持的返回结果格式:" + resultType
//                    );
//                }
//            }
            // 检查请求次数
            if (StringUtils.isBlank(requestTimes)) {
                throw DataXException.asDataXException(
                        HttpReaderErrorCode.REQUIRED_VALUE,
                        "请求次数 [" + HttpKey.REQUEST_TIMES + "] 为必填项.");
            } else {
                if (!RequestTimesEnum.isExistByName(requestTimes)) {
                    throw DataXException.asDataXException(
                            HttpReaderErrorCode.ILLEGAL_VALUE, "请求次数的值应为:" + RequestTimesEnum.getAllName()
                    );
                }
                if (StringUtils.equalsIgnoreCase(RequestTimesEnum.multiple.name(), requestTimes)) {
                    if (StringUtils.isBlank(readerOriginConfig.getString(HttpKey.LOOP_PARAM))) {
                        throw DataXException.asDataXException(
                                HttpReaderErrorCode.REQUIRED_VALUE,
                                "请求次数为多次请求时, " + "需要指定循环的参数, [" + HttpKey.LOOP_PARAM + "] 为必填项.");
                    }
                    if (readerOriginConfig.getLong(HttpKey.START_INDEX) == null) {
                        throw DataXException.asDataXException(
                                HttpReaderErrorCode.REQUIRED_VALUE,
                                "请求次数为多次请求时, " + "需要指定循环请求的起点, [" + HttpKey.START_INDEX + "] 为必填项.");
                    }
                    if (readerOriginConfig.getLong(HttpKey.END_INDEX) == null) {
                        throw DataXException.asDataXException(
                                HttpReaderErrorCode.REQUIRED_VALUE,
                                "请求次数为多次请求时, " + "需要指定循环请求的终点, [" + HttpKey.END_INDEX + "] 为必填项.");
                    }
                    if (readerOriginConfig.getLong(HttpKey.STEP) == null) {
                        throw DataXException.asDataXException(
                                HttpReaderErrorCode.REQUIRED_VALUE,
                                "请求次数为多次请求时, " + "需要指定循环请求的步长, [" + HttpKey.STEP + "] 为必填项.");
                    }
                    if (readerOriginConfig.getLong(HttpKey.START_INDEX) > readerOriginConfig.getLong(HttpKey.END_INDEX)) {
                        throw DataXException.asDataXException(
                                HttpReaderErrorCode.ILLEGAL_VALUE, "stardIndex的值应小于等于endIndex的值");
                    }
                    if (readerOriginConfig.getLong(HttpKey.STEP) <= 0) {
                        throw DataXException.asDataXException(
                                HttpReaderErrorCode.ILLEGAL_VALUE, "step值大于0");
                    }
                }
            }

            List<Configuration> columns = this.readerOriginConfig.getListConfiguration(Key.COLUMN);
            if (CollectionUtils.isEmpty(columns)) {
                throw DataXException.asDataXException(HttpReaderErrorCode.REQUIRED_VALUE, "您需要指定 columns");
            } else {
                for (int i = 0; i < columns.size(); i++) {
                    Configuration eachColumnConf = columns.get(i);
                    eachColumnConf.getNecessaryValue(HttpKey.NAME, HttpReaderErrorCode.COLUMN_REQUIRED_VALUE);
                    eachColumnConf.getNecessaryValue(Key.TYPE, HttpReaderErrorCode.COLUMN_REQUIRED_VALUE);
                }

                if (connection == null) {
                    throw DataXException.asDataXException(
                            HttpReaderErrorCode.REQUIRED_VALUE,
                            "连接信息 [" + HttpKey.CONNECTION + "] 为必填项.");
                } else {
                    if (StringUtils.isBlank(connection.getString(HttpKey.URL))) {
                        throw DataXException.asDataXException(
                                HttpReaderErrorCode.REQUIRED_VALUE,
                                "请求的URL [" + HttpKey.CONNECTION + "." + HttpKey.URL + "] 为必填项.");
                    }
                    Configuration authentic = connection.getConfiguration(HttpKey.AUTHENTIC);
                    if (authentic == null) {
                        throw DataXException.asDataXException(
                                HttpReaderErrorCode.REQUIRED_VALUE,
                                "认证信息 [" + HttpKey.CONNECTION + "." + HttpKey.AUTHENTIC + "] 为必填项.");
                    } else {
                        String authType = authentic.getString(HttpKey.AUTH_TYPE);
                        if (StringUtils.isBlank(authType)) {
                            throw DataXException.asDataXException(
                                    HttpReaderErrorCode.REQUIRED_VALUE,
                                    "认证的方式 [" + HttpKey.CONNECTION + "." + HttpKey.AUTHENTIC + "." + HttpKey.AUTH_TYPE +
                                            "] 为必填项.");
                        }

                        if (!AuthTypeEnum.isExistByName(authType)) {
                            throw DataXException.asDataXException(
                                    HttpReaderErrorCode.ILLEGAL_VALUE, "认证方式暂不支持:" + authType
                            );
                        }
                        if (StringUtils.equalsIgnoreCase(AuthTypeEnum.basic.name(), authType)) {
                            if (StringUtils.isBlank(authentic.getString(HttpKey.AUTH_USERNAME)) ||
                                    StringUtils.isBlank(authentic.getString(HttpKey.AUTH_PASSWORD))) {
                                throw DataXException.asDataXException(
                                        HttpReaderErrorCode.REQUIRED_VALUE,
                                        "认证方式为:" + AuthTypeEnum.basic.name() + "时,  ["
                                                + String.join(",", HttpKey.AUTH_USERNAME, HttpKey.AUTH_PASSWORD) + "] 为必填项."
                                );
                            }
                        } else if (StringUtils.equalsIgnoreCase(AuthTypeEnum.token.name(), authType)) {
                            if (StringUtils.isBlank(authentic.getString(HttpKey.AUTH_TOKEN))) {
                                throw DataXException.asDataXException(
                                        HttpReaderErrorCode.REQUIRED_VALUE,
                                        "认证方式为:" + AuthTypeEnum.basic.name() + "时,  ["
                                                + HttpKey.AUTH_TOKEN + "] 为必填项."
                                );
                            }

                        }
                    }
                }

                if (StringUtils.isBlank(readerOriginConfig.getString(HttpKey.DATA_MODE))) {
                    throw DataXException.asDataXException(
                            HttpReaderErrorCode.REQUIRED_VALUE,
                            "请求返回的数据格式 [" + HttpKey.DATA_MODE + "] 为必填项.");
                }
                // data_mode
                if (!DataModeEnum.isExistByName(readerOriginConfig.getString(HttpKey.DATA_MODE))) {
                    throw DataXException.asDataXException(
                            HttpReaderErrorCode.ILLEGAL_VALUE, "请求返回的结果JSON数据的格式值应为:[" + DataModeEnum.getAllName() + "]"
                    );
                }

                // dirty_DATA
                String dirtyData = readerOriginConfig.getString(HttpKey.DIRTY_DATA);
                if (StringUtils.isBlank(dirtyData)) {
                    throw DataXException.asDataXException(
                            HttpReaderErrorCode.REQUIRED_VALUE,
                            "脏数据处理方式 [" + HttpKey.DIRTY_DATA + "] 为必填项.");
                } else {
                    if (!StringUtils.equalsIgnoreCase("dirty", dirtyData) && !StringUtils.equalsIgnoreCase("null", dirtyData)) {
                        throw DataXException.asDataXException(
                                HttpReaderErrorCode.ILLEGAL_VALUE, "请正确填写脏数据处理方式."
                        );
                    }
                }

                String encoding = this.readerOriginConfig.getString(HttpKey.ENCODING, "UTF-8");
                try {
                    Charsets.toCharset(encoding);
                } catch (UnsupportedCharsetException uce) {
                    throw DataXException.asDataXException(
                            HttpReaderErrorCode.ILLEGAL_VALUE,
                            String.format("不支持的编码格式 : [%s]", encoding), uce);
                }
            }
        }
    }

    public static class Task
            extends Reader.Task {
        private static Logger LOG = LoggerFactory.getLogger(Reader.Task.class);

        private Configuration readerSliceConfig = null;
        private CloseableHttpClient httpclient;
        private final HttpClientContext context = HttpClientContext.create();
        private Configuration connection = null;
        private Configuration authentic = null;
        private String requestTimes;
        private String dirtyData;
        private String dataMode;
        private AuthTypeEnum authType;
        private String method;

        @Override
        public void init() {
            // 获取httpreader插件的配置参数
            this.readerSliceConfig = this.getPluginJobConf();
            // 获取连接配置
            this.connection = readerSliceConfig.getConfiguration(HttpKey.CONNECTION);
            this.requestTimes = this.readerSliceConfig.getString(HttpKey.REQUEST_TIMES).toLowerCase();
            this.method = readerSliceConfig.getString(HttpKey.METHOD).toLowerCase();
            this.dataMode = readerSliceConfig.getString(HttpKey.DATA_MODE).toLowerCase();
            this.dirtyData = readerSliceConfig.getString(HttpKey.DIRTY_DATA).toLowerCase();

        }


        @Override
        public void startRead(RecordSender recordSender) {
            // 判断是否是单次请求
            if (StringUtils.equalsIgnoreCase(RequestTimesEnum.single.name(), requestTimes)) {
                String parameters = readerSliceConfig.getString(HttpKey.PARAMETERS);
                executeRequestAndTransport(parameters, recordSender);
            } else {
                // 请求循环参数
                String loopParam = readerSliceConfig.getString(HttpKey.LOOP_PARAM);
                // 控制循环
                Integer startIndex = readerSliceConfig.getInt(HttpKey.START_INDEX);
                Integer endIndex = readerSliceConfig.getInt(HttpKey.END_INDEX);
                Integer step = readerSliceConfig.getInt(HttpKey.STEP);
                for (; startIndex <= endIndex; startIndex += step) {
                    String parameters = readerSliceConfig.getString(HttpKey.PARAMETERS);
                    parameters = resetParameters(parameters, loopParam, startIndex);
                    executeRequestAndTransport(parameters, recordSender);
                }
            }
        }

        public void executeRequestAndTransport(String parameters,
                                               RecordSender recordSender) {
            String headers = readerSliceConfig.getString(HttpKey.HEADERS);
            String url = connection.getString(HttpKey.URL);
            String jsonPath = readerSliceConfig.getString(HttpKey.JSON_PATH);
            String encoding = readerSliceConfig.getString(HttpKey.ENCODING, "UTF-8");

            Configuration authentic = this.connection.getConfiguration(HttpKey.AUTHENTIC);
            AuthTypeEnum authType = AuthTypeEnum.getByName(authentic.getString(HttpKey.AUTH_TYPE));
            Map<String, String> authenticParam = new HashMap<>();
            authenticParam.put(HttpConstant.AUTH_USERNAME, authentic.getString(HttpConstant.AUTH_USERNAME));
            authenticParam.put(HttpConstant.AUTH_PASSWORD, authentic.getString(HttpConstant.AUTH_PASSWORD));
            authenticParam.put(HttpConstant.AUTH_APP_KEY, authentic.getString(HttpConstant.AUTH_APP_KEY));
            authenticParam.put(HttpConstant.AUTH_APP_SECRET, authentic.getString(HttpConstant.AUTH_APP_SECRET));
            authenticParam.put(HttpConstant.AUTH_TOKEN, authentic.getString(HttpConstant.AUTH_TOKEN));
            String responseContent = null;
            try {
                responseContent = HttpClientUtil.executeRequest(url, method, parameters, headers, jsonPath, authType, authenticParam, encoding);
            } catch (URISyntaxException | IOException e) {
                throw DataXException.asDataXException(HttpReaderErrorCode.EXECUTE_REQUEST_FAIL, e);
            }

            List<Configuration> columnConfList = readerSliceConfig.getListConfiguration(HttpKey.COLUMN);
            // 处理结果
            transportOneRecord(responseContent, recordSender, columnConfList, getTaskPluginCollector());
        }


        @Override
        public void destroy() {
        }

        public String resetParameters(String parameters, String resetParam, Integer value) {
            if (StringUtils.isNotBlank(parameters)) {
                if (StringUtils.equals(MethodEnum.get.name(), method)) {
                    Map<String, String> paramMap = HttpClientUtil.getMapFromQueryParam(parameters);
                    if(MapUtils.isNotEmpty(paramMap)) {
                        paramMap.put(resetParam, value.toString());
                        List<String> newQuery = new ArrayList<>();
                        for (Map.Entry<String, String> queryMap : paramMap.entrySet()) {
                            newQuery.add(queryMap.getKey() + "=" + queryMap.getValue());
                        }
                        parameters = String.join("&", newQuery);
                    }
                } else if (StringUtils.equals(MethodEnum.post.name(), method)) {
                    JSONObject parametersRoot = JSON.parseObject(parameters);
                    JSONPath.set(parametersRoot, resetParam, value);
                    parameters = parametersRoot.toJSONString();
                }
            }
            return parameters;
        }


        // 转换成一条记录
        public void transportOneRecord(String responseContent, RecordSender recordSender,
                                       List<Configuration> columns, TaskPluginCollector taskPluginCollector) {

            JSONArray jsonArray = null;

            if (StringUtils.isBlank(responseContent)) {
                return;
            }
            if (StringUtils.equalsIgnoreCase(DataModeEnum.multiData.name(), dataMode)) {
                    jsonArray = JSON.parseArray(responseContent);
            } else if (StringUtils.equalsIgnoreCase(DataModeEnum.oneData.name(), dataMode)) {
                jsonArray = new JSONArray();
                jsonArray.add(JSONObject.parseObject(responseContent));
            }

            if (jsonArray == null || jsonArray.isEmpty()) {
                // empty result
                return;
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Record record = recordSender.createRecord();
                Column columnGenerated;
                try {
                    for (Configuration column : columns) {
                        String columnName = column.getString(HttpKey.NAME);
                        String columnType = column.getString(HttpKey.TYPE).toUpperCase();

                        Object columnValue = JSONPath.eval(jsonObject, columnName);
                        if (columnValue == null) {
                            if (StringUtils.equalsIgnoreCase("dirty", dirtyData)) {
                                throw new IllegalArgumentException(String.format(
                                        "从指定的column json路径[%s]中找不到数据", columnName));
                            } else if (StringUtils.equalsIgnoreCase("null", dirtyData)) {
                                record.addColumn(new StringColumn());
                            }
                        } else {
                            HttpDataType type = HttpDataType.valueOf(columnType);
                            try {
                                switch (type) {
                                    case INT:
                                    case LONG:
                                        columnGenerated = new LongColumn(Long.valueOf(columnValue.toString()));
                                        break;
                                    case BOOLEAN:
                                        columnGenerated = new BoolColumn(Boolean.valueOf(columnValue.toString()));
                                        break;
                                    case DOUBLE:
                                    case FLOAT:
                                        columnGenerated = new DoubleColumn(Double.valueOf(columnValue.toString()));
                                        break;
                                    case STRING:
                                        columnGenerated = new StringColumn(columnValue.toString());
                                        break;
                                    case DATE:
                                        String dateStr = columnValue.toString();
                                        String dataFormat = column.getString(HttpKey.DATE_FORMAT);
                                        if (StringUtils.isNotBlank(dataFormat)) {
                                            // 用户自己配置的格式转换
                                            SimpleDateFormat format = new SimpleDateFormat(dataFormat);
                                            columnGenerated = new DateColumn(format.parse(dateStr));
                                        }
                                        else {
                                            // 框架尝试转换
                                            columnGenerated = new DateColumn(new StringColumn(columnValue.toString()).asDate());
                                        }
                                        break;
                                    default:
                                        throw DataXException
                                                .asDataXException(
                                                        HttpReaderErrorCode.ILLEGAL_VALUE,
                                                        String.format(
                                                                "您的配置文件中的列配置信息有误. 因为DataX 不支持数据库写入这种字段类型. 字段名:[%s], 字段类型:[%s]. 请修改表中该字段的类型或者不同步该字段.",
                                                                columnName, columnType));
                                }
                            } catch (Exception e) {
                                throw new IllegalArgumentException(String.format(
                                        "类型转换错误, 无法将[%s] 转换为[%s], %s", columnValue, type, e));
                            }
                            record.addColumn(columnGenerated);
                        }
                    } // end for
                    recordSender.sendToWriter(record);
                } catch (IllegalArgumentException | IndexOutOfBoundsException iae) {
                    taskPluginCollector.collectDirtyRecord(record, iae.getMessage());
                } catch (Exception e) {
                    if (e instanceof DataXException) {
                        throw (DataXException) e;
                    }
                    // 每一种转换失败都是脏数据处理,包括数字格式 & 日期格式
                    taskPluginCollector.collectDirtyRecord(record, e.getMessage());
                }
            }
        }


        private SSLConnectionSocketFactory ignoreSSLErrors()
        {
            try {
                // use the TrustSelfSignedStrategy to allow Self Signed Certificates
                SSLContext sslContext = SSLContextBuilder
                        .create()
                        .loadTrustMaterial(new TrustSelfSignedStrategy())
                        .build();

                // we can optionally disable hostname verification.
                // if you don't want to further weaken the security, you don't have to include this.
                HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

                // create an SSL Socket Factory to use the SSLContext with the trust self-signed certificate strategy
                // and allow all hosts verifier.
                return new SSLConnectionSocketFactory(sslContext, allowAllHosts);
            }
            catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    static class MyConnectionSocketFactory
            implements
            ConnectionSocketFactory
    {

        @Override
        public Socket createSocket(final HttpContext context)
        {
            Proxy proxy = null;
            URI uri = (URI) context.getAttribute("proxy");
            if (uri == null) {
                return null;
            }

            InetSocketAddress socksAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
            String proxyType = uri.getScheme();
            if (proxyType.startsWith("socks")) {
                proxy = new Proxy(Proxy.Type.SOCKS, socksAddress);
            }
            else if (proxyType.startsWith("http")) {
                proxy = new Proxy(Proxy.Type.HTTP, socksAddress);
            }
            if (proxy == null) {
                return null;
            }
            else {
                return new Socket(proxy);
            }
        }

        @Override
        public Socket connectSocket(final int connectTimeout,
                final Socket socket, final HttpHost host,
                final InetSocketAddress remoteAddress,
                final InetSocketAddress localAddress,
                final HttpContext context)
                throws IOException
        {
            Socket sock;
            if (socket != null) {
                sock = socket;
            }
            else {
                sock = createSocket(context);
            }
            if (localAddress != null) {
                sock.bind(localAddress);
            }
            try {
                sock.connect(remoteAddress, connectTimeout);
            }
            catch (SocketTimeoutException ex) {
                throw new ConnectTimeoutException(ex, host,
                        remoteAddress.getAddress());
            }
            return sock;
        }


    }


    enum RequestTimesEnum {
        single, multiple;

        public static boolean isExistByName(String name) {
            for (RequestTimesEnum value : values()) {
                if (StringUtils.equalsIgnoreCase(value.name(), name)) {
                    return true;
                }
            }
            return false;
        }

        public static String getAllName() {
            return Arrays.stream(values()).map(RequestTimesEnum::name).collect(Collectors.joining(","));
        }

    }

    enum MethodEnum {
        get, post;
        public static boolean isExistByName(String name) {
            for (MethodEnum value : values()) {
                if (StringUtils.equalsIgnoreCase(value.name(), name)) {
                    return true;
                }
            }
            return false;
        }
        public static String getAllName() {
           return Arrays.stream(values()).map(MethodEnum::name).collect(Collectors.joining(","));
        }

    }

    enum DataModeEnum {
        oneData, multiData;
        public static boolean isExistByName(String name) {
            for (DataModeEnum value : values()) {
                if (StringUtils.equalsIgnoreCase(value.name(), name)) {
                    return true;
                }
            }
            return false;
        }
        public static String getAllName() {
            return Arrays.stream(values()).map(DataModeEnum::name).collect(Collectors.joining(","));
        }

    }

//    enum Type {
//        STRING, LONG, BOOL, DOUBLE, FLOAT, DATE, BYTES;
//
//        private static boolean isTypeIllegal(String typeString) {
//            try {
//                Type.valueOf(typeString.toUpperCase());
//            } catch (Exception e) {
//                return false;
//            }
//
//            return true;
//        }
//    }

}
