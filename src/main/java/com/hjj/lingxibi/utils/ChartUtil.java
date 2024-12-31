package com.hjj.lingxibi.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hjj.lingxibi.model.entity.Chart;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

@Slf4j
public class ChartUtil {

    public static final String jsCodePrefix = "<ReactECharts option={";

    public static final String jsCodeSuffix = "}/>";

    public static boolean isChartValid(String echartsCode) {
        if (StringUtils.isEmpty(echartsCode)) {
            return false;
        }
        StringBuffer stringBuffer = new StringBuffer();
        String jsCode = stringBuffer.append(jsCodePrefix).append(echartsCode).append(jsCodeSuffix).toString();
        Context context = Context.enter();
        context.setErrorReporter(new ErrorReporter() {
            @Override
            public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
                log.error("Echarts code Warning: " + message);
            }

            @Override
            public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
                log.error("Echarts code Error: " + message);
                throw new EvaluatorException(message);
            }

            @Override
            public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
                log.error("Echarts code Runtime Error: " + message);
                throw new EvaluatorException(message);
            }
        });
        try {
            // 初始化一个包含标准 js 对象的上下文，再传入 echarts 代码，源名称，起始行号，null 默认执行方式
            context.evaluateString(context.initStandardObjects(), jsCode, "JavaScriptCode", 1, null);
            return true;
        } catch (EvaluatorException e) {
            log.error(e.getMessage());
            return false;
        } finally {
            Context.exit();
        }
    }

    /**
     * 构建用户输入
     *
     * @param chart
     * @return
     */
    public static String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请注意图表类型必须是" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据

        userInput.append(csvData).append("\n");
        return userInput.toString();
    }

    // 将生成的Echarts代码进行增强，拓展下载图表功能
    public static String strengthenGenChart(String genChart) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(genChart);

            boolean toolboxExists = addToolboxToSeries(jsonNode);

            if (!toolboxExists) {
                ObjectNode toolboxNode = mapper.createObjectNode();
                ObjectNode featureNode = toolboxNode.putObject("feature");
                featureNode.putObject("saveAsImage");
                ((ObjectNode) jsonNode).set("toolbox", toolboxNode);
            }

            String outputString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            return outputString;
        } catch (Exception e) {
            e.printStackTrace();
            return genChart;
        }
    }

    // 判断生成图表中是否有 toolbox 字段
    public static boolean addToolboxToSeries(JsonNode node) {
        boolean toolboxExists = false;
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            if (objectNode.has("toolbox")) {
                toolboxExists = true;
            }
            if (objectNode.has("series")) {
                for (JsonNode seriesNode : objectNode.get("series")) {
                    if (seriesNode.isObject() && !((ObjectNode) seriesNode).has("toolbox")) {
                        ObjectNode toolboxNode = ((ObjectNode) seriesNode).putObject("toolbox");
                        ObjectNode featureNode = toolboxNode.putObject("feature");
                        featureNode.putObject("saveAsImage");
                    }
                }
            }
            for (JsonNode childNode : objectNode) {
                toolboxExists = addToolboxToSeries(childNode) || toolboxExists;
            }
        }
        return toolboxExists;
    }

    public static String optimizeGenChart(String echartsCode) {
        return echartsCode.replace("'", "\"").trim().
                replaceAll("\\s*\"\\s*(\\w+)\\s*\"\\s*:", "\"$1\":");
    }
}
