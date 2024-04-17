package com.hjj.lingxibi.utils;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class InvalidEchartsTest {
    @Test
    public void checkEchartsTest() {
        String jsCode = "<ReactECharts option={{\n" +
                "  \"title\" : {\n" +
                "    \"text\" : \"用户增长趋势分析\",\n" +
                "    \"left\" : \"center\"\n" +
                "  },\n" +
                "  \"tooltip\" : {\n" +
                "    \"trigger\" : \"axis\"\n" +
                "  },\n" +
                "  \" xAxis\" : {\n" +
                "    \"type\" : \"category\",\n" +
                "    \"data\" : [ \"1号\", \"2号\", \"3号\", \"4号\", \"5号\", \"6号\", \"7号\", \"8号\", \"9号\", \"10号\", \"11号\" ]\n" +
                "  },\n" +
                "  \" yAxis\" : {\n" +
                "    \"type\" : \"value\"\n" +
                "  },\n" +
                "  \"series\" : [ {\n" +
                "    \"data\" : [ 10, 20, 30, 23, 20, 29, 24, 31, 40, 38, 43 ],\n" +
                "    \"type\" : \"line\",\n" +
                "    \"toolbox\" : {\n" +
                "      \"feature\" : {\n" +
                "        \"saveAsImage\" : { }\n" +
                "      }\n" +
                "    }\n" +
                "  } ],\n" +
                "  \"toolbox\" : {\n" +
                "    \"feature\" : {\n" +
                "      \"saveAsImage\" : { }\n" +
                "    }\n" +
                "  }\n" +
                "}} />";

        Context context = Context.enter();
        context.setErrorReporter(new ErrorReporter() {
            @Override
            public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
                System.err.println("Warning: " + message);
            }

            @Override
            public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
                System.err.println("Error: " + message);
            }

            @Override
            public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
                System.err.println("Runtime Error: " + message);
                return new EvaluatorException(message);
            }
        });

        try {
            context.evaluateString(context.initStandardObjects(), jsCode, "JavaScriptCode", 1, null);
            System.out.println("JavaScript code is valid");
        } catch (EvaluatorException e) {
            System.out.println("JavaScript code is invalid: " + e.getMessage());
        } finally {
            Context.exit();
        }
    }
}
