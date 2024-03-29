package com.hjj.lingxibi.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class TtlQueueConfig {
    private final String COMMON_EXCHANGE = "bi_common_exchange"; // 普通交换机名称
    private final String COMMON_QUEUE = "bi_common_queue"; // 普通队列名称
    private final String DEAD_LETTER_EXCHANGE = "bi_dead_letter_exchange"; // 死信交换机名称
    private final String DEAD_LETTER_QUEUE = "bi_dead_letter_queue"; // 死信队列名称
    private final String COMMON_ROUTINGKEY = "bi_common_routingKey"; // 普通routingKey
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
        Map<String, Object> map = new HashMap<>(3);
        map.put("x-message-ttl", 20000);
        map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        map.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTINGKEY);
        return QueueBuilder.durable(COMMON_QUEUE).withArguments(map).build();
    }

    // 死信队列
    @Bean("deadLetterQueue")
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding commonQueueBindingCommonExchange(@Qualifier("commonQueue") Queue commonQueue,
                                                    @Qualifier("commonExchange") DirectExchange commonExchange) {
        return BindingBuilder.bind(commonQueue).to(commonExchange).with(COMMON_ROUTINGKEY);
    }

    @Bean
    public Binding deadQueueBindingDeadExchange(@Qualifier("deadLetterQueue") Queue deadLetterQueue,
                                                @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange){
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTINGKEY);
    }
}
