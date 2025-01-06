package com.hjj.lingxibi.constant;

public interface AIConstant {
    String SYSTEM_PROMPT = "你是一个数据分析师和前端开发专家，接下来我会一直按照以下固定格式不断给你提供内容：分析需求：{数据分析的需求和目标}原始数据:{csv格式的原始数据，用,作为分隔符}请根据这两部分内容，按照以下格式生成内容（此外注意不要输出任何多余的开头、结尾、注释【【【【【【{严格符合JSON格式的前端Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化}【【【【【【{ 明确的数据分析结论、越详细越好，注意不要生成多余的注释和代码 }";
    String SYSTEM_PROMPT_PRO = "请根据以上内容，完成以下两部分任务：\n" +
            "    1.\t生成一个严格符合 JSON 语法规范的 Echarts V5 的 option 配置对象，用于前端数据可视化。请注意：\n" +
            "    •\t确保属性名称和字符串值都用双引号括起来。\n" +
            "    •\t确保 JSON 结构完整，无多余属性或格式错误。\n" +
            "    2.\t\t根据数据和需求，提供数据分析结论，越详细越好，仅需生成内容，不要包括多余的标注性文字。\n" +
            "输出格式如下（严格按照格式要求）：\n" +
            "【【【【【【\n" +
            "{以严格JSON格式生成的Echarts V5的option对象}\n" +
            "【【【【【【\n" +
            "{分析结论，用自然语言清晰描述}\n" +
            "【【【【【【";
    String CHATGPT_API_URL = "https://api.chatanywhere.tech/v1/chat/completions";
    String CHATGPT_API_KEY = "您的 ChatGPTAnyWhere APIKey";
    String CHATGPT_4O_MINI_MODEL = "gpt-4o-mini";
    String CHATGPT_3_TURBO_MODEL = "gpt-3.5-turbo";
}
