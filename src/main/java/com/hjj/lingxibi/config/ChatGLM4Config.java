package com.hjj.lingxibi.config;

import com.hjj.lingxibi.constant.ZhiPuAIConstant;
import com.zhipu.oapi.ClientV4;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatGLM4Config {
    @Bean
    public ClientV4 clientV4() {
        return new ClientV4.Builder(ZhiPuAIConstant.ZHIPU_AI_API_KEY).build();
    }
}
