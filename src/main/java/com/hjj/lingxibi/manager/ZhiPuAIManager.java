package com.hjj.lingxibi.manager;

import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.exception.BusinessException;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class ZhiPuAIManager {

    @Resource
    private ClientV4 clientV4;

    public String doChat(ChatMessage chatMessage2) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage1 = new ChatMessage(ChatMessageRole.USER.value(), "你是一个数据分析刊师和前端开发专家，接下来我会一直按照以下固定格式不断给你提供内容：分析需求：{数据分析的需求和目标}原始数据:{csv格式的原始数据，用,作为分隔符}请根据这两部分内容，按照以下格式生成内容（此外注意不要输出任何多余的开头、结尾、注释）\n" +
                "【【【【【【{前端Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化}【【【【【【{ 明确的数据分析结论、越详细越好，注意不要生成多余的注释 }");
            messages.add(chatMessage1);
        if (chatMessage2 == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        messages.add(chatMessage2);
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
        String aiResponse = invokeModelApiResp.getData().getChoices().get(0).toString();
        System.out.println("model output:" + invokeModelApiResp.getMsg());
        System.out.println("model output:" + aiResponse);
        return aiResponse;
    }

}
