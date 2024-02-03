package com.hjj.lingxibi.bizmq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class BIMessageProducerTest {
    @Resource
    private BIMessageProducer myMessageProducer;

/*    @Test
    void sendMessage() {
        myMessageProducer.sendMessage("code_exchange", "my_routineKey", "你好呀");
    }*/
}