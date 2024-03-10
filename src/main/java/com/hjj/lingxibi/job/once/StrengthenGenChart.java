package com.hjj.lingxibi.job.once;


import cn.hutool.json.JSONObject;

public class StrengthenGenChart {
    public static void main(String[] args) {
            String inputJson = "{\n" +
                    "  \"legend\": {\n" +
                    "    \"data\": [\"用户数\"]\n" +
                    "  },\n" +
                    "  \"tooltip\": {\n" +
                    "    \"trigger\": \"item\"\n" +
                    "  },\n" +
                    "  \"series\": [\n" +
                    "    {\n" +
                    "      \"name\": \"用户增长\",\n" +
                    "      \"type\": \"pie\",\n" +
                    "      \"radius\": \"50%\",\n" +
                    "      \"data\": [\n" +
                    "        {\"value\": 10, \"name\": \"1号\"},\n" +
                    "        {\"value\": 20, \"name\": \"2号\"},\n" +
                    "        {\"value\": 30, \"name\": \"3号\"},\n" +
                    "        {\"value\": 21, \"name\": \"4号\"},\n" +
                    "        {\"value\": 20, \"name\": \"5号\"},\n" +
                    "        {\"value\": 29, \"name\": \"6号\"},\n" +
                    "        {\"value\": 24, \"name\": \"7号\"},\n" +
                    "        {\"value\": 31, \"name\": \"8号\"},\n" +
                    "        {\"value\": 40, \"name\": \"9号\"},\n" +
                    "        {\"value\": 38, \"name\": \"10号\"},\n" +
                    "        {\"value\": 43, \"name\": \"11号\"}\n" +
                    "      ],\n" +
                    "      \"label\": {\n" +
                    "        \"show\": true,\n" +
                    "        \"formatter\": \"{b}: {c}人\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            JSONObject jsonObject = new JSONObject(inputJson);
            JSONObject seriesArray = jsonObject.getJSONArray("series").getJSONObject(0); // 获取series数组的第一个元素

            // 检查是否存在toolbox字段，如果不存在则添加
            if (!jsonObject.containsKey("toolbox")) {
                jsonObject.put("toolbox", new JSONObject().put("feature", new JSONObject().put("saveAsImage", new JSONObject())));
                jsonObject.getJSONArray("series").put(seriesArray); // 将series数组的第一个元素移动到数组末尾，以便在其后添加toolbox
            }
            // 输出转换后的JSON字符串
        System.out.println(jsonObject.toString());
    }

}
