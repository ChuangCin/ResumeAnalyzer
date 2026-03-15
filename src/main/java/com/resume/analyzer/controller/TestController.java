package com.resume.analyzer.controller;

import com.resume.analyzer.common.Result;
import com.resume.analyzer.service.CacheService;
import com.resume.analyzer.service.MinioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    private final MinioService minioService;
    private final CacheService cacheService;

    public TestController(MinioService minioService, CacheService cacheService) {
        this.minioService = minioService;
        this.cacheService = cacheService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Backend is working!";
    }

    /** 检查 MinIO 是否正常（连接与 bucket 是否可用） */
    @GetMapping("/minio/health")
    public Result<String> minioHealth() {
        try {
            minioService.checkHealth();
            return Result.success("MinIO 正常，bucket 已就绪");
        } catch (Exception e) {
            return Result.error("MinIO 异常: " + e.getMessage());
        }
    }

    /** 检查 Redis 是否在用：能连上并读写则说明已使用 Redis 缓存（支持 /redis/health 与 /api/redis/health） */
    @GetMapping({"/redis/health", "/api/redis/health"})
    public Result<Map<String, Object>> redisHealth() {
        Map<String, Object> info = new HashMap<>();
        try {
            cacheService.set("health:redis", "ok", 10);
            String v = cacheService.get("health:redis", String.class);
            boolean ok = "ok".equals(v);
            info.put("usingRedis", ok);
            info.put("message", ok ? "Redis 正常，缓存已启用" : "Redis 连接异常，读写校验失败");
            return ok ? Result.success(info) : Result.error(info.get("message").toString());
        } catch (Exception e) {
            info.put("usingRedis", false);
            info.put("message", "Redis 异常: " + e.getMessage());
            return Result.error(info.get("message").toString());
        }
    }
}