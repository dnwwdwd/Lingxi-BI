package com.hjj.lingxibi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hjj.lingxibi.model.dto.chart.ChartQueryRequest;
import com.hjj.lingxibi.model.dto.chart.ChartQueryRequestEs;
import com.hjj.lingxibi.model.dto.chart.ChartRegenRequest;
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

    /**
     * 根据 id 查询图表
     * @param id
     * @return
     */
    Long queryUserIdByChartId(Long id);

    /**
     * MQ 异步生成图表（有重试机制）
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    BIResponse genChartByAIAsyncMq(MultipartFile multipartFile, GenChartByAIRequest genChartByAIRequest, HttpServletRequest request);

    /**
     * MQ 异步重新生成图表（需要更改图表参数）
     * @param chartRegenRequest
     * @param request
     * @return
     */
    BIResponse regenChartByAsyncMq(ChartRegenRequest chartRegenRequest, HttpServletRequest request);

    /**
     * 从 ES 中查询图表
     * @param chartQueryRequestEs
     * @return
     */
    Page<Chart> searchFromEs(ChartQueryRequestEs chartQueryRequestEs);

    /**
     * 同步生成图表
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    BIResponse genChartByAI(MultipartFile multipartFile, GenChartByAIRequest genChartByAIRequest, HttpServletRequest request);
}
