package com.hangyue.auth.config_manage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 动态配置属性类，使用@RefreshScope注解，当配置变更时会自动刷新
 */
@RefreshScope
@Component
public class DynamicConfigProperties {

    private static final Logger logger = LoggerFactory.getLogger(DynamicConfigProperties.class);

    @Autowired
    private Environment environment;

    /**
     * 获取配置值
     * @param key 配置键
     * @return 配置值，如果不存在则返回null
     */
    public String getProperty(String key) {
        String value = environment.getProperty(key);
        logger.debug("获取配置: {} = {}", key, value);
        return value;
    }

    /**
     * 获取配置值，如果不存在则返回默认值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在则返回默认值
     */
    public String getProperty(String key, String defaultValue) {
        String value = environment.getProperty(key, defaultValue);
        logger.debug("获取配置: {} = {} (默认值: {})", key, value, defaultValue);
        return value;
    }

    /**
     * 获取类型化的配置值
     * @param key 配置键
     * @param targetType 目标类型
     * @param <T> 类型参数
     * @return 配置值，如果不存在则返回null
     */
    public <T> T getProperty(String key, Class<T> targetType) {
        T value = environment.getProperty(key, targetType);
        logger.debug("获取配置: {} = {} (类型: {})", key, value, targetType.getSimpleName());
        return value;
    }

    /**
     * 获取类型化的配置值，如果不存在则返回默认值
     * @param key 配置键
     * @param targetType 目标类型
     * @param defaultValue 默认值
     * @param <T> 类型参数
     * @return 配置值，如果不存在则返回默认值
     */
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        T value = environment.getProperty(key, targetType, defaultValue);
        logger.debug("获取配置: {} = {} (类型: {}, 默认值: {})", key, value, targetType.getSimpleName(), defaultValue);
        return value;
    }
}
