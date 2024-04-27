package com.hjj.lingxibi.utils;

import lombok.extern.slf4j.Slf4j;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class InvalidEchartsUtil {

    private static final String jsCodePrefix = "<ReactECharts option={";

    private static final String jsCodeSuffix ="}/>";

    public  static boolean checkEchartsTest(String echartsCode) {
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
}
