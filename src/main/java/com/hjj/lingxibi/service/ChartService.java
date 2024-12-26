package com.hjj.lingxibi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hjj.lingxibi.model.dto.chart.ChartQueryRequest;
import com.hjj.lingxibi.model.dto.chart.ChartRegenRequest;
import com.hjj.lingxibi.model.dto.chart.GenChartByAIRequest;
import com.hjj.lingxibi.model.dto.team_chart.ChartAddToTeamRequest;
import com.hjj.lingxibi.model.entity.Chart;
import com.hjj.lingxibi.model.vo.BIResponse;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

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
     * 同步生成图表
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    BIResponse genChartByAI(MultipartFile multipartFile, GenChartByAIRequest genChartByAIRequest, HttpServletRequest request);

    /**
     * 按关键词搜索我的图表（MySQL 实现）
     */
    Page<Chart> searchMyCharts(ChartQueryRequest chartQueryRequest);


    /**
     * 将图表状态改为成功
     * @param chartId
     * @param genChart
     * @param genResult
     */
    void handleChartUpdateSuccess(long chartId, String genChart, String genResult);

    void handleChartUpdateError(long chartId, String execMessage);

    void saveAndReturnFailedChart(String name, String goal, String chartType,
                                  String chartData, String genChart, String genResult, String execMessage, Long userId);

    boolean addChartToTeam(ChartAddToTeamRequest chartAddToTeamRequest, HttpServletRequest request);

    Page<Chart> pageTeamChart(ChartQueryRequest chartQueryRequest);
}
