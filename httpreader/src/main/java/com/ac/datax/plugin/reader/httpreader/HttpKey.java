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


import com.alibaba.datax.plugin.unstructuredstorage.reader.Key;

public final class HttpKey extends Key
{
    // 获取返回json的那个key值
    public static final String JSON_PATH = "jsonPath";
    // 连接信息
    public static final String CONNECTION = "connection";
    // 配置连接代理
    public static final String PROXY = "proxy";
    // 代理地址
    public static final String HOST = "host";
    // 代理认证信息，格式为 username:password
    public static final String AUTH = "auth";
    // 请求的地址
//    public static final String URL = "url";
    public static final String URL = "url";
    // 接口认证帐号
    public static final String USERNAME = "username";
    // 接口认证密码
    public static final String PASSWORD = "password";
    // 接口认证token
    public static final String TOKEN = "token";
    // 接口请求参数
    public static final String PARAMETERS = "parameters";
    // 请求的定制头信息
    public static final String HEADERS = "headers";
    // 请求超时参数，单位为秒
    public static final String TIMEOUT_SEC = "timeout";
    // 请求方法，仅支持get，post两种模式
    public static final String METHOD = "method";
    // data encoding，default is UTF-8. string type
//    public static final String ENCODING = "encoding";
    // 脏数据处理
    public static final String DIRTY_DATA = "dirtyData";
    // 返回的结果类型
    public static final String RESULT_TYPE = "resultType";
    // JSON的数据格式
    public static final String DATA_MODE = "dataMode";

    // 认证配置
    public static final String AUTHENTIC = "authentic";
    // 认证类型，支持no,basic,token,params, request
    public static final String AUTH_TYPE = "authType";
    // 认证的用户名
    public static final String AUTH_USERNAME = "authUsername";
    // 认证的密码
    public static final String AUTH_PASSWORD = "authPassword";
    // 认证的token
    public static final String AUTH_TOKEN = "authToken";
    // 认证URL
    public static final String AUTH_URL = "authUrl";
    // 认证URL请求参数
    public static final String AUTH_REQUEST_PARAM = "authRequestParam";

    public static final String AUTH_APP_KEY = "appkey";

    public static final String AUTH_APP_SECRET = "appSecret";



    // must hvae for column
    public static final String COLUMN = "column";
    // 列名
    public static final String NAME = "name";
    // 列类型
    public static final String TYPE = "type";
    public static final String DATE_FORMAT = "dateFormat";


    // 请求次数 single、multiple
    public static final String REQUEST_TIMES = "requestTimes";
    // 指定循循环的参数
    public static final String LOOP_PARAM = "loopParam";
    // 循环请求的起点
    public static final String START_INDEX = "startIndex";
    // 循环请求的终点
    public static final String END_INDEX = "endIndex";
    // 循环请求的步长
    public static final String STEP = "step";







}
