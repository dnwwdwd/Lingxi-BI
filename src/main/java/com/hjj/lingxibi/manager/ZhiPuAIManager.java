package com.hjj.lingxibi.manager;

import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.constant.AIConstant;
import com.hjj.lingxibi.exception.BusinessException;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ZhiPuAIManager {

    @Resource
    private ClientV4 clientV4;

    public String doChat(ChatMessage chatMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage prompt = new ChatMessage(ChatMessageRole.SYSTEM.value(), "将我的输入的内容生成为英文，" +
                "并且不要生成多余内容");
        messages.add(prompt);
        if (chatMessage == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        messages.add(chatMessage);
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("glm-4-flash")
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
        String result = invokeModelApiResp.getData().getChoices().get(0).getMessage().getContent().toString();
        log.info("ZhiPuAI Response: {}", result);
        return result;
    }

    public String doChat(ChatMessage chatMessage, Long chartId) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage prompt = new ChatMessage(ChatMessageRole.SYSTEM.value(), AIConstant.SYSTEM_PROMPT_PRO);
        messages.add(prompt);
        if (chatMessage == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        messages.add(chatMessage);
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("glm-4-flash")
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
        String result = invokeModelApiResp.getData().getChoices().get(0).getMessage().getContent().toString();
        log.info("ZhiPuAI Response For chartId: {} {}", chartId, result);
        return result;
    }

}
