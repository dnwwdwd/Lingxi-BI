package com.hjj.lingxibi.bizmq;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class TtlQueueConsumer {
    @Resource
    BIMessageProducer biMessageProducer;

    @SneakyThrows
    @RabbitListener(queues = "bi_dead_letter_queue", ackMode = "MANUAL")
    public void doTTLMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("已经接受到死信消息：{}", message);
        biMessageProducer.sendMessage(message);
        channel.basicAck(deliveryTag, false);
    }
}