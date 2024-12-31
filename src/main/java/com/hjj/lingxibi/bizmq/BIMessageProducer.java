package com.hjj.lingxibi.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BIMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 消息生产生产者，发送消息
     * @param message
     */
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend("bi_common_exchange", "bi_common_routingKey", message);
    }

    /**
     * 消息生产生产者，发送消息给普通队列（管理员）
     * @Param message
     */
    public void sendMessageToCommonQueue(String message) {
        rabbitTemplate.convertAndSend("bi_common_exchange", "bi_common_routingKey_admin", message);
    }

/*    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(BIMQConstant.BI_EXCHANGE_NAME, BIMQConstant.BI_ROUTING_KEY, message);
    }*/
}
