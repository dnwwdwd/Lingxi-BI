package com.hjj.lingxibi.bizmq;

import com.hjj.lingxibi.constant.BIMQConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MyMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 消息生产生产者，发送消息
     * @param message
     */
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(BIMQConstant.BI_EXCHANGE_NAME, BIMQConstant.BI_QUEUE_NAME, message);
    }
}
