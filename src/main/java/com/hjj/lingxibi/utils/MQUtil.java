package com.hjj.lingxibi.utils;


import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class MQUtil {

    public static void rejectMsgAndRequeue(Channel channel, long deliveryTag, long charId){
        try {
            channel.basicReject(deliveryTag, true);
        } catch (IOException e) {
            log.error("图表Id：{} 消息拒绝和重入队失败了", charId);
            throw new RuntimeException(e);
        }
    }

}
