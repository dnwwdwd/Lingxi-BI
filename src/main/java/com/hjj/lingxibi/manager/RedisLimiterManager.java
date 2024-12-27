package com.hjj.lingxibi.manager;

import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 专门提供 RedisLimiter 限流基础服务（提供了通用的能力）
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流操作
     *
     * @param key 区分不同的限流器，比如不同的用户 id 应该分别统计
     */
    public void doRateLimit(String key) {
        // 创建一个名称为user_limiter的限流器，每秒最多访问2次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, 1, 1, RateIntervalUnit.SECONDS); // OVERALL类型：不管有多少台服务器都是放在一起统计的，请求阈值，生成令牌的频率
        // 每当一个操作来了后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(1);// 令牌请求数，处理请求需消耗的令牌
        if (!canOp) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
