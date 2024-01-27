package com.hjj.lingxibi.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class RedisLimiterManagerTest {
    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Test
    void doRateLimit() {
        String userId = "1";
        for (int i=0;i<5;i++) {
            redisLimiterManager.doRateLimit(userId);
            System.out.println("success");
        }
    }
}