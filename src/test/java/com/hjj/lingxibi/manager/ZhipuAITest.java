package com.hjj.lingxibi.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static com.hjj.lingxibi.utils.ZhiPuAIUtil.extractAnalysis;
import static com.hjj.lingxibi.utils.ZhiPuAIUtil.extractJSCode;

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
        String jsCode = extractJSCode(content);
        String analysis = extractAnalysis(content);
        System.out.println(jsCode);
        System.out.println(analysis);

    }



}
