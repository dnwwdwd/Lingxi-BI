package com.hjj.lingxibi;

import com.github.rholder.retry.*;
import com.google.common.base.Predicates;
import com.hjj.lingxibi.bizmq.BIMessageProducer;
import com.hjj.lingxibi.constant.CommonConstant;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class TestClassSecond {

    @Resource
    private YuCongMingClient yuCongMingClient;

    @Resource
    private BIMessageProducer biMessageProducer;

    public static final Logger log = LoggerFactory.getLogger(TestClassSecond.class);

    /**
     * 测试过程
     */
    @Test
    public void testProc() {
//      定义重试器并配置
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()   // 模板类型为 要重试函数的返回类型
                .retryIfExceptionOfType(Exception.class) // 重试条件 根据指定异常类型重试
                .retryIfResult(Predicates.equalTo(false)) // 重试条件 根据返回值判断重试     异常类型判断 和 返回值判断  可以2选1 或者 两个条件同时设定
                .withWaitStrategy(WaitStrategies.fixedWait(5, TimeUnit.SECONDS))
//                .withWaitStrategy(WaitStrategies.exponentialWait(100, 5, TimeUnit.MINUTES))  // 重试频率 每次叠加5分钟
                .withStopStrategy(StopStrategies.stopAfterAttempt(2))  // 最多重试2次
//                .withStopStrategy(StopStrategies.neverStop())    // 判断重试条件 一直重试下去
                .withAttemptTimeLimiter(AttemptTimeLimiters.fixedTimeLimit(1, TimeUnit.SECONDS)) // 每次重试条件为真 重试频率固定2秒后重试
                .build();
        // 定义目标对象
        TestClassSecond testClassSecond = new TestClassSecond();

        //  定义参数
        //  String params = "https://blog.csdn.net/wangxudongx";
        String params = "https://www.baidu.com";

        try {
            // 重试器调用
            retryer.call(() -> testClassSecond.func(params));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 重试的目标函数
     *
     * @param url
     * @return
     */
    private Boolean func(String url) {
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.getForObject(url, String.class);
        log.info("result：" + result);
        if (result.length() > 10000) {
            return false;
        }
        System.out.println("success");
        return true;
    }


    @Test
    public void YucongmingTest() {
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setMessage("iu是谁");
        devChatRequest.setModelId(CommonConstant.BI_MODEL_ID);
        BaseResponse<DevChatResponse> baseResponse =
                yuCongMingClient.doChat(devChatRequest);
        String content = baseResponse.getData().getContent();
        System.out.println(content);
    }
}