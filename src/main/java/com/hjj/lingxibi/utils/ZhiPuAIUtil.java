package com.hjj.lingxibi.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZhiPuAIUtil {
    /**
     * 提取JS代码
     */
    public static String extractJSCode(String content) {
        // 匹配 ```javascript``` 和 ``` 包裹的部分
        String regex = "```javascript\\n(.*?)```";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL); // DOTALL 允许匹配换行符
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1); // 返回匹配的JS代码部分
        }
        return "未找到JS代码";
    }

    /**
     * 提取数据分析结论
     */
    public static String extractAnalysis(String content) {
        // 找到 "数据分析结论：" 后的所有内容
        String keyword = "数据分析结论：\n";
        int index = content.indexOf(keyword);
        if (index != -1) {
            return content.substring(index + keyword.length()).trim(); // 截取关键字后的部分
        }
        return "未找到数据分析结论";
    }

}
