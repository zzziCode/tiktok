package com.zzzi.common.constant;

/**
 * @author zzzi
 * @date 2024/3/26 13:13
 * 防止缓存穿透的默认值
 * 一旦从缓存中获取到的是这个值就直接返回
 */
public class RedisDefaultValue {

    public static final String REDIS_DEFAULT_VALUE = "redis_default_value";
}
