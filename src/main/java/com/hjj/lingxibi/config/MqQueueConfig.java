package com.hjj.lingxibi.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MqQueueConfig {
    private final String COMMON_EXCHANGE = "bi_common_exchange"; // 普通交换机名称
    private final String COMMON_QUEUE = "bi_common_queue"; // 普通队列名称
    private final String COMMON_QUEUE_ADMIN = "bi_common_queue_admin"; // 普通队列名称（管理员）
    private final String DEAD_LETTER_EXCHANGE = "bi_dead_letter_exchange"; // 死信交换机名称
    private final String DEAD_LETTER_QUEUE = "bi_dead_letter_queue"; // 死信队列名称
    private final String COMMON_ROUTINGKEY = "bi_common_routingKey"; // 普通routingKey
    private final String COMMON_ROUTINGKEY_ADMIN = "bi_common_routingKey_admin"; // 普通routingKey（管理员）
    private final String DEAD_LETTER_ROUTINGKEY = "bi_dead_letter_routingKey"; // 死信routingKey

    // 普通交换机
    @Bean("commonExchange")
    public DirectExchange commonExchange() {
        return new DirectExchange(COMMON_EXCHANGE);
    }

    // 死信交换机
    @Bean("deadLetterExchange")
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    // 普通队列
    @Bean("commonQueue")
    public Queue commonQueue() {
        Map<String, Object> map = new HashMap<>(4);
        map.put("x-message-ttl", 20000);
        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        map.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTINGKEY);
        // 设置正常队列的长度限制
        map.put("x-max-length", 30);
        return QueueBuilder.durable(COMMON_QUEUE).withArguments(map).build();
    }

    // 死信队列
    @Bean("deadLetterQueue")
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    // 普通队列（管理员专用）
    @Bean("commonQueueAdmin")
    public Queue commonQueueAdmin() {
        return QueueBuilder.durable(COMMON_QUEUE_ADMIN).build();
    }

    @Bean
    public Binding commonQueueBindingCommonExchange(@Qualifier("commonQueue") Queue commonQueue,
                                                    @Qualifier("commonExchange") DirectExchange commonExchange) {
        return BindingBuilder.bind(commonQueue).to(commonExchange).with(COMMON_ROUTINGKEY);
    }

    @Bean
    public Binding deadQueueBindingDeadExchange(@Qualifier("deadLetterQueue") Queue deadLetterQueue,
                                                @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTINGKEY);
    }

    @Bean
    public Binding commonQueueBindingCommonExchangeAdmin(@Qualifier("commonQueueAdmin") Queue commonQueueAdmin,
                                                         @Qualifier("commonExchange") DirectExchange commonExchange) {
        return BindingBuilder.bind(commonQueueAdmin).to(commonExchange).with(COMMON_ROUTINGKEY_ADMIN);
    }
}
