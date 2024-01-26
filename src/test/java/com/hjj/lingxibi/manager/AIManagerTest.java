package com.hjj.lingxibi.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;

@SpringBootTest
class AIManagerTest {

    @Resource
    private AIManager aiManager;

    @Test
    void doChat() {
        String answer = aiManager.doChat(1659171950288818178L, "IU是谁");
        System.out.println(answer);
    }
}