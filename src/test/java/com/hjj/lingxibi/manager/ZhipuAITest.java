package com.hjj.lingxibi.manager;

import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.model.dto.ai.ChatGPTResponse;
import com.hjj.lingxibi.utils.AIUtil;
import com.hjj.lingxibi.utils.ChartUtil;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.Arrays;

import static com.hjj.lingxibi.utils.AIUtil.extractAnalysisForZhiPuAI;
import static com.hjj.lingxibi.utils.AIUtil.extractJSCodeForZhiPuAI;

@SpringBootTest
public class ZhipuAITest {
    @Resource
    private ZhiPuAIManager zhiPuAIManager;

    @Test
    public void test() {
        String content = "```javascript\n" +
                "{\n" +
                "  \"title\": {\n" +
                "    \"text\": \"近几日网站用户数变化趋势\"\n" +
                "  },\n" +
                "  \"tooltip\": {\n" +
                "    \"trigger\": \"axis\"\n" +
                "  },\n" +
                "  \"legend\": {\n" +
                "    \"data\": [\"用户数\"]\n" +
                "  },\n" +
                "  \"grid\": {\n" +
                "    \"left\": \"3%\",\n" +
                "    \"right\": \"4%\",\n" +
                "    \"bottom\": \"3%\",\n" +
                "    \"containLabel\": true\n" +
                "  },\n" +
                "  \"xAxis\": {\n" +
                "    \"type\": \"category\",\n" +
                "    \"boundaryGap\": false,\n" +
                "    \"data\": [\"1号\", \"2号\", \"3号\", \"4号\", \"5号\", \"6号\", \"7号\", \"8号\", \"9号\", \"10号\", \"11号\"]\n" +
                "  },\n" +
                "  \"yAxis\": {\n" +
                "    \"type\": \"value\"\n" +
                "  },\n" +
                "  \"series\": [{\n" +
                "    \"name\": \"用户数\",\n" +
                "    \"type\": \"line\",\n" +
                "    \"stack\": \"总量\",\n" +
                "    \"smooth\": true,\n" +
                "    \"data\": [10, 20, 30, 70, 20, 29, 24, 31, 40, 38, 43]\n" +
                "  }]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "数据分析结论：\n" +
                "- 在过去11天内，网站的用户数整体呈波动上升趋势。\n" +
                "- 4号时用户数达到峰值，为70人。\n" +
                "- 5号用户数有所下降，但随后在6号和7号两天又有所回升。\n" +
                "- 8号和9号用户数相对稳定，之后在10号和11号又有所增长。\n" +
                "- 整体来看，用户数在4号之前增长较慢，之后增长速度加快，但波动性较大。\n";
        String jsCode = extractJSCodeForZhiPuAI(content);
        String analysis = extractAnalysisForZhiPuAI(content);
        System.out.println(jsCode);
        System.out.println(analysis);

    }


    @Test
    public void testZhiPuAI() {
        String response = null;
        try {
            response = zhiPuAIManager.doChat(new ChatMessage(ChatMessageRole.USER.value(), "你好，你是谁？"));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.THIRD_SERVICE_ERROR);
        }
        System.out.println(response);
    }

    @Test
    public void testChatGPT() {
        String response = "";
        try {
            response = AIUtil.invokeChatGPT("分析需求：\n" +
                    "分析近几日的网站数据情况,请注意图表类型必须是折线图\n" +
                    "原始数据：\n" +
                    "日期,用户数\n" +
                    "1号,10\n" +
                    "2号,20\n" +
                    "3号,30\n" +
                    "4号,70\n" +
                    "5号,20\n" +
                    "6号,29\n" +
                    "7号,24\n" +
                    "8号,31\n" +
                    "9号,40\n" +
                    "10号,38\n" +
                    "11号,43");
        } catch (Exception e) {
            e.printStackTrace();
        }
        ChatGPTResponse chatGPTResponse = AIUtil.extractAIResponseFoChatGPT(response);
        String content = chatGPTResponse.getContent();
        System.out.println("ChatGPT返回内容：" + content);
        String jsCode = AIUtil.extractJsCode(content);
        String analysis = AIUtil.extractAnalysis(content);
        System.out.println("生成的JS代码：" + jsCode);
        System.out.println("数据结论：" + analysis);

    }

}
