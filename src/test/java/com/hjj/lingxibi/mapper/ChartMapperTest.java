package com.hjj.lingxibi.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class ChartMapperTest {
    @Resource
    ChartMapper chartMapper;
    
    @Test
    public void queryChartData() {
        String chartId = "1750880023067357186";
        String querySql = String.format("select * from chart_%s", chartId);
        List<Map<String, Object>> resultData = chartMapper.queryChartData(querySql);
        System.out.println(resultData);
    }
}
