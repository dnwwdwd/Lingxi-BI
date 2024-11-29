package com.hjj.lingxibi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hjj.lingxibi.model.entity.Chart;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* @author hejiajun
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2024-01-25 19:35:15
* @Entity com.hjj.lingxibi.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {
    List<Map<String, Object>> queryChartData(@Param("querySql") String querySql);

    Long queryUserIdByChartId(@Param("id") Long id);
}