package com.hjj.lingxibi.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.constant.AIConstant;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.model.dto.ai.ChatGPTResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class AIUtil {
    /**
     * 提取JS代码（用于智谱AI）
     */
    public static String extractJSCodeForZhiPuAI(String content) {
        // 匹配 ```javascript``` 和 ``` 包裹的部分
        String regex = "```javascript\\n(.*?)```";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL); // DOTALL 允许匹配换行符
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1); // 返回匹配的JS代码部分
        }
        return "";
    }

    /**
     * 提取数据分析结论（用于智谱AI）
     */
    public static String extractAnalysisForZhiPuAI(String content) {
        // 找到 "数据分析结论：" 后的所有内容
        String keyword = "数据分析结论：\n";
        int index = content.indexOf(keyword);
        if (index != -1) {
            return content.substring(index + keyword.length()).trim(); // 截取关键字后的部分
        }
        return "";
    }

    public static String invokeChatGPT(String message) throws Exception {
        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();

        // 创建 JSON 对象
        JsonObject json = new JsonObject();
        json.addProperty("model", "gpt-4o-mini");

        JsonArray messages = new JsonArray();
        JsonObject systemMessage = new JsonObject();
        JsonObject messageObj = new JsonObject();
        // 添加 system 消息
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", AIConstant.SYSTEM_PROMPT_PRO);
        messages.add(systemMessage);
        // 添加用户发送的消息
        messageObj.addProperty("role", "user");
        message = message.replace("\n", "\\n");
        messageObj.addProperty("content", message);
        messages.add(messageObj);
        json.add("messages", messages);
        json.addProperty("temperature", 0.7);

        // 将 JSON 对象转换为字符串
        String jsonString = gson.toJson(json);

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonString, mediaType);
        Request request = new Request.Builder()
                .url(AIConstant.CHATGPT_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + AIConstant.CHATGPT_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        String responseBody = "";
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorResponse = response.body().string();
                throw new BusinessException(ErrorCode.THIRD_SERVICE_ERROR, errorResponse);
            }
            responseBody = response.body().string();
            log.info("ChatGPT API Response for Chart: {}", responseBody);
        } catch (Exception e) {
            log.info("图表调用ChatGPT API 时抛出了异常，异常信息：{}", e.getMessage());
            throw new Exception(e.getMessage());
        }
        return responseBody;
    }

    public static String invokeChatGPT(String message, Long chartId) throws Exception {
        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();

        // 创建 JSON 对象
        JsonObject json = new JsonObject();
        json.addProperty("model", AIConstant.CHATGPT_4O_MINI_MODEL);

        JsonArray messages = new JsonArray();
        JsonObject systemMessage = new JsonObject();
        JsonObject messageObj = new JsonObject();
        // 添加 system 消息
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", AIConstant.SYSTEM_PROMPT_PRO);
        messages.add(systemMessage);
        // 添加用户发送的消息
        messageObj.addProperty("role", "user");
        message = message.replace("\n", "\\n");
        messageObj.addProperty("content", message);
        messages.add(messageObj);
        json.add("messages", messages);
        json.addProperty("temperature", 0.7);

        // 将 JSON 对象转换为字符串
        String jsonString = gson.toJson(json);

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonString, mediaType);
        Request request = new Request.Builder()
                .url(AIConstant.CHATGPT_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + AIConstant.CHATGPT_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        String responseBody = "";
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorResponse = response.body().string();
                throw new BusinessException(ErrorCode.THIRD_SERVICE_ERROR, errorResponse);
            }
            responseBody = response.body().string();
            log.info("ChatGPT API Response for ChartId: " + chartId + responseBody);
        } catch (IOException e) {
            log.info("图表Id：{} 调用ChatGPT API 时抛出了异常，异常信息：{}", chartId, e.getMessage());
            throw new Exception(e.getMessage());
        }
        return responseBody;
    }


    /**
     * 从 ChatGPT 返回的内容中提取消息
     *
     * @param responseBody
     * @return
     */
    public static ChatGPTResponse extractAIResponseFoChatGPT(String responseBody) {
        Gson gson = new Gson();
        JsonObject response = gson.fromJson(responseBody, JsonObject.class);
        // 提取 message.content
        String content = response.getAsJsonArray("choices")
                .get(0).getAsJsonObject().getAsJsonObject("message")
                .get("content").getAsString();

        // 提取 usage.total_tokens
        int totalTokens = response.getAsJsonObject("usage")
                .get("total_tokens").getAsInt();
        return ChatGPTResponse.builder().content(content).totalTokens(totalTokens).build();
    }

    public static String extractJsCode(String content) {
        String[] split = content.split("【【【【【【");
        if (split.length >= 3) {
            return split[1].trim();
        }
        return "";
    }

    public static String extractAnalysis(String content) {
        String[] split = content.split("【【【【【【");
        if (split.length >= 3) {
            return split[2].trim();
        }
        return "";
    }


}
