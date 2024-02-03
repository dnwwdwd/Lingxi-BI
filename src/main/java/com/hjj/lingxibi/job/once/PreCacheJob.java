package com.hjj.lingxibi.job.once;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.service.ChartService;
import com.hjj.lingxibi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PreCacheJob {
    @Resource
    RedissonClient redissonClient;

    @Resource
    ChartService chartService;

    @Resource
    UserService userService;

    @Resource
    RedisTemplate<String, Object> redisTemplate;

    private  List<Long> usersIdList;

    @PostConstruct
    public void initializeUsersIdList() {
        List<Long> queriedUsersIdList = userService.queryUsersId();
        if (queriedUsersIdList != null) {
            this.usersIdList = queriedUsersIdList;
        } else {
            log.error("queryUsersId 返回了 null。无法初始化 usersIdList。");
            // 根据需要处理这种情况，例如，抛出异常或设置默认值。
        }
    }

    @Scheduled(cron = "0 22 20 * * *")
    public void doPreCacheJob(){
        String doCacheLockId = String.format("%s:precachejob:docache:lock", "lingxibi");
        RLock lock = redissonClient.getLock(doCacheLockId);
        try {
            if(lock.tryLock(0, 3000, TimeUnit.MICROSECONDS)) {
                for(Long userId : usersIdList) {
                    String myChartKeyId = String.format("lingxibi:chart:list:%s", userId);
                    Page<Chart> myChartPage = chartService.page(new Page<>(1, 20), null);
                    ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();
                    try {
                        opsForValue.set(myChartKeyId, myChartPage, 24, TimeUnit.HOURS);
                    } catch (Exception e) {
                        log.error("缓存预热失败");
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if(lock.isHeldByCurrentThread()) {
                log.info("锁已经被{}释放", Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}
