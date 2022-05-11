package com.ac.datax.plugin.reader.httpreader;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum AuthTypeEnum {

    no("匿名"),
    basic("basic模式"),
    token("token模式");

    private final String desc;

    AuthTypeEnum(String desc) {
        this.desc = desc;
    }

    public static boolean isExistByName(String name) {
        for (AuthTypeEnum value : values()) {
            if (StringUtils.equalsIgnoreCase(value.name(), name)) {
                return true;
            }
        }
        return false;
    }

    public static String getAllName() {
        return Arrays.stream(values()).map(AuthTypeEnum::name).collect(Collectors.joining(","));
    }

    public static AuthTypeEnum getByName(String name) {
        for (AuthTypeEnum value : values()) {
            if (StringUtils.equalsIgnoreCase(value.name(), name)) {
                return value;
            }
        }
        return null;
    }
}
