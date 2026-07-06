package com.hnu.legal_cases.util;

import com.hnu.legal_cases.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
public class DistributedLock {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Lua 脚本用于原子性释放锁
    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";

    private final DefaultRedisScript<Long> unlockScript;

    public DistributedLock() {
        this.unlockScript = new DefaultRedisScript<>();
        this.unlockScript.setScriptText(UNLOCK_LUA_SCRIPT);
        this.unlockScript.setResultType(Long.class);
    }

    /**
     * 生成锁的名称
     *
     * @param prefix 锁前缀
     * @param params 锁参数
     * @return 锁名称
     */
    public String getLockName(String prefix, String... params) {
        if (prefix == null || prefix.isEmpty()) {
            throw new ServiceException("锁前缀不能为空");
        }
        if (params == null || params.length == 0) {
            throw new ServiceException("锁名称不能为空");
        }
        return prefix + "_" + String.join("_", params);
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey    锁的键
     * @param expireTime 过期时间（秒）
     * @return 锁的值（用于释放锁时验证）
     */
    public String tryLock(String lockKey, int expireTime) {
        String lockValue = UUID.randomUUID().toString();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(expireTime));

        if (Boolean.TRUE.equals(success)) {
            log.info("成功获取锁：{}, 值: {}", lockKey, lockValue);
            return lockValue;
        } else {
            log.warn("获取锁失败：{}", lockKey);
            return null;
        }
    }

    /**
     * 释放锁
     *
     * @param lockKey   锁的键
     * @param lockValue 锁的值
     */
    public void releaseLock(String lockKey, String lockValue) {
        try {
            Long result = stringRedisTemplate.execute(unlockScript,
                    Collections.singletonList(lockKey), lockValue);
            boolean success = result > 0;

            if (success) {
                log.info("成功释放锁: {}", lockKey);
            } else {
                log.error("释放锁失败，锁可能已过期或被其他线程释放: {}", lockKey);
            }

        } catch (Exception e) {
            log.error("释放锁异常: {}", lockKey, e);
        }
    }
}
