package com.hjj.lingxibi.config;

import com.github.rholder.retry.*;
import com.google.common.base.Predicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class RetryConfig {

    @Bean
    public Retryer<Boolean> retryer() {
        return RetryerBuilder.<Boolean>newBuilder()
                .retryIfExceptionOfType(Exception.class)
                .retryIfResult(Predicates.equalTo(false))
                .withWaitStrategy(WaitStrategies.fixedWait(5, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(2))
                .withAttemptTimeLimiter(AttemptTimeLimiters.fixedTimeLimit(1, TimeUnit.SECONDS))
                .build();
    }
}
