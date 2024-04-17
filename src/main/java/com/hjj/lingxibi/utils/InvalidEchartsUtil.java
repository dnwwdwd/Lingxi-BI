package com.hjj.lingxibi.utils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

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
                System.err.println("Echarts code Warning: " + message);
            }

            @Override
            public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
                System.err.println("Echarts code Error: " + message);
            }

            @Override
            public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
                System.err.println("Echarts code Runtime Error: " + message);
                return new EvaluatorException(message);
            }
        });
        try {
            context.evaluateString(context.initStandardObjects(), jsCode, "JavaScriptCode", 1, null);
            return true;
        } catch (EvaluatorException e) {
            return false;
        } finally {
            Context.exit();
        }
    }
}
