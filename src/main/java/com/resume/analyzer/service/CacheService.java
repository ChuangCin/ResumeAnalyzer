package com.resume.analyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 统一缓存读写，key 规范：job:list、job:id:{id}、analysis:{resumeId}、match:{resumeId}:{jobId}
 */
@Service
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cache.default-ttl:3600}")
    private long defaultTtlSeconds;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object v = redisTemplate.opsForValue().get(key);
        if (v == null) return null;
        if (type.isInstance(v)) return (T) v;
        return null;
    }

    public void set(String key, Object value) {
        set(key, value, defaultTtlSeconds);
    }

    public void set(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void delete(Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /** 按前缀删除（需扫描 key，用于 match:resumeId:* 等） */
    public void deleteByPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /** 未读消息数 key：user:message:unread:{userId} */
    public static String unreadCountKey(Long userId) {
        return "user:message:unread:" + (userId != null ? userId : "");
    }

    /** 用户信息缓存 key：user:info:{userId}，TTL 10 分钟 */
    public static String userInfoKey(Long userId) {
        return "user:info:" + (userId != null ? userId : "");
    }

    private static final long USER_INFO_TTL_SECONDS = 600;

    @SuppressWarnings("unchecked")
    public <T> T getUserInfo(Long userId, Class<T> type) {
        if (userId == null) return null;
        return get(userInfoKey(userId), type);
    }

    public void setUserInfo(Long userId, Object user) {
        if (userId != null && user != null) {
            set(userInfoKey(userId), user, USER_INFO_TTL_SECONDS);
        }
    }

    public void evictUserInfo(Long userId) {
        if (userId != null) delete(userInfoKey(userId));
    }

    public Long getUnreadCount(Long userId) {
        if (userId == null) return 0L;
        Object v = redisTemplate.opsForValue().get(unreadCountKey(userId));
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return null;
    }

    public void setUnreadCount(Long userId, long count) {
        if (userId == null) return;
        redisTemplate.opsForValue().set(unreadCountKey(userId), count, defaultTtlSeconds, TimeUnit.SECONDS);
    }

    /** 用户收到新消息时调用，未读 +1 */
    public void incrementUnreadCount(Long userId) {
        if (userId == null) return;
        Long cur = getUnreadCount(userId);
        setUnreadCount(userId, (cur != null ? cur : 0L) + 1L);
    }

    /** 用户查看消息后清零未读 */
    public void clearUnreadCount(Long userId) {
        if (userId == null) return;
        redisTemplate.delete(unreadCountKey(userId));
    }}
