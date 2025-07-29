package com.hangyue.auth.controller;

import com.hangyue.auth.config_manage.DynamicConfigProperties;
import com.hangyue.auth.config_manage.NacosConfigPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理控制器，提供查看和更新配置的API
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private DynamicConfigProperties dynamicConfig;

    @Autowired
    private NacosConfigPushService nacosConfigPushService;

    /**
     * 获取指定配置项的值
     * @param key 配置键
     * @return 配置值
     */
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable String key) {
        String value = dynamicConfig.getProperty(key);
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("value", value);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前所有Nacos配置
     * @return 配置内容
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllConfig() {
        String config = nacosConfigPushService.getCurrentConfig();
        Map<String, Object> response = new HashMap<>();
        response.put("config", config);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新Nacos配置
     * @param content 新的配置内容
     * @return 更新结果
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody String content) {
        boolean success = nacosConfigPushService.updateConfig(content);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        if (success) {
            response.put("message", "配置更新成功，已自动刷新");
        } else {
            response.put("message", "配置更新失败");
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 手动同步Nacos配置到本地文件
     * @return 同步结果
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncConfig() {
        boolean success = nacosConfigPushService.syncNacosConfigToLocal();
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        if (success) {
            response.put("message", "配置已同步到本地文件");
        } else {
            response.put("message", "配置同步失败");
        }
        return ResponseEntity.ok(response);
    }
}
