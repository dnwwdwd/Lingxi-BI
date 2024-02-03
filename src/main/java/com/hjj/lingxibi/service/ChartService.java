package com.hjj.lingxibi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjj.lingxibi.model.dto.chart.ChartQueryRequest;
import com.hjj.lingxibi.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 17653
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2024-01-25 19:35:15
*/
public interface ChartService extends IService<Chart> {
    /**
     * 根据查询
     * @param chartQueryRequest
     * @return
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

}
