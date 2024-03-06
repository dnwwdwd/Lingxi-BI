package com.hjj.lingxibi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hjj.lingxibi.model.dto.chart.ChartQueryRequest;
import com.hjj.lingxibi.model.dto.chart.GenChartByAIRequest;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.model.vo.BIResponse;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

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

    Long queryUserIdByChartId(Long id);

    BIResponse genChartByAIAsyncMq(MultipartFile multipartFile, GenChartByAIRequest genChartByAIRequest, HttpServletRequest request);
}
