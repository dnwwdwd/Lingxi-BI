package com.hjj.lingxibi;

import com.hjj.lingxibi.model.dto.ai.ChatGPTResponse;
import com.hjj.lingxibi.utils.AIUtil;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String message = "分析需求：\n" +
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
                "11号,43";
        String result = null;
        try {
            result = AIUtil.invokeChatGPT(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ChatGPTResponse chatGPTResponse = AIUtil.extractAIResponseFoChatGPT(result);
        System.out.println(chatGPTResponse.toString());
        String[] split = chatGPTResponse.getContent().split("【【【【【【");
        System.out.println(Arrays.toString(split));
    }
}
